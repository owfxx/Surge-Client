package gg.surge.client.lod;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public final class Lod {
	static final int CELLS = 4;
	static final int REGION_CHUNKS = 32;
	static final int RC = REGION_CHUNKS * CELLS;
	static final short EMPTY = Short.MIN_VALUE;

	private static final RenderPipeline PIPELINE = RenderPipeline.builder()
		.withLocation(Identifier.fromNamespaceAndPath("surge", "pipeline/lod"))
		.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
		.withUniform("Projection", UniformType.UNIFORM_BUFFER)
		.withVertexShader("core/position_color")
		.withFragmentShader("core/position_color")
		.withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
		.withCull(false)
		.withDepthWrite(true)
		.withoutBlend()
		.build();

	private static final ExecutorService BUILDER = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "Surge distant terrain"); t.setDaemon(true); t.setPriority(Thread.MIN_PRIORITY); return t; });
	private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "Surge terrain save"); t.setDaemon(true); return t; });

	static final class Region {
		final int rx, rz;
		final short[] height = new short[RC * RC];
		final int[] color = new int[RC * RC];
		volatile boolean dirty, loaded;
		volatile int version;
		int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

		Region(int rx, int rz) {
			this.rx = rx;
			this.rz = rz;
			java.util.Arrays.fill(height, EMPTY);
		}
	}

	static final class Mesh {
		GpuBuffer buffer;
		int indexCount;
		int level, version, holeX, holeZ, holeR;
		boolean hasHole;
		float minY, maxY;
	}

	private record Built(long key, MeshData data, ByteBufferBuilder bytes, int level, int version, boolean hole, int hx, int hz, int hr, float minY, float maxY) {}

	private static final Map<Long, Region> REGIONS = new ConcurrentHashMap<>();
	private static final Map<Long, Mesh> MESHES = new HashMap<>();
	private static final Set<Long> BUILDING = ConcurrentHashMap.newKeySet();
	private static final java.util.concurrent.ConcurrentLinkedQueue<Built> READY = new java.util.concurrent.ConcurrentLinkedQueue<>();
	private static final Map<Long, Long> SAMPLED = new HashMap<>();
	private static String worldKey = "";
	private static ClientLevel lastLevel;
	private static long lastSave;
	private static int scanIndex;
	private static Frustum frustum;

	private Lod() {}

	static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
	}

	public static boolean active() {
		Minecraft mc = Minecraft.getInstance();
		return Mods.LOD.enabled() && mc.level != null;
	}

	public static int distanceChunks() {
		return Math.max(32, Math.round(Mods.LOD_DISTANCE.get()));
	}

	public static float farBlocks() {
		return distanceChunks() * 16f + 256f;
	}

	public static void setFrustum(Frustum f) {
		frustum = f;
	}

	private static Path dir() {
		return Path.of(System.getProperty("user.home"), ".surgeclient", "lod", worldKey);
	}

	private static String computeKey(Minecraft mc, ClientLevel level) {
		String base;
		if (mc.getSingleplayerServer() != null) base = "sp_" + mc.getSingleplayerServer().getWorldData().getLevelName();
		else if (mc.getCurrentServer() != null) base = "mp_" + mc.getCurrentServer().ip;
		else base = "unknown";
		String dim = level.dimension().identifier().toString();
		return (base + "_" + dim).toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
	}

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level != lastLevel) {
			saveAll();
			clear();
			lastLevel = level;
			if (level != null) worldKey = computeKey(mc, level);
		}
		if (level == null || mc.player == null || !Mods.LOD.enabled()) return;
		sample(mc, level);
		long now = System.currentTimeMillis();
		if (now - lastSave > 30_000) {
			lastSave = now;
			saveAll();
		}
	}

	private static void sample(Minecraft mc, ClientLevel level) {
		int rd = mc.options.getEffectiveRenderDistance();
		int pcx = mc.player.blockPosition().getX() >> 4, pcz = mc.player.blockPosition().getZ() >> 4;
		int side = rd * 2 + 1, total = side * side;
		long now = System.currentTimeMillis();
		int budget = 12;
		for (int n = 0; n < total && budget > 0; n++) {
			int idx = (scanIndex++) % total;
			int cx = pcx - rd + idx % side, cz = pcz - rd + idx / side;
			long k = key(cx, cz);
			Long at = SAMPLED.get(k);
			if (at != null && now - at < 45_000) continue;
			LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
			if (chunk == null || chunk.isEmpty()) continue;
			SAMPLED.put(k, now);
			sampleChunk(level, chunk, cx, cz);
			budget--;
		}
		if (SAMPLED.size() > 20_000) SAMPLED.clear();
	}

	private static void sampleChunk(ClientLevel level, LevelChunk chunk, int cx, int cz) {
		int rx = Math.floorDiv(cx, REGION_CHUNKS), rz = Math.floorDiv(cz, REGION_CHUNKS);
		Region r = region(rx, rz, true);
		int ox = (cx - rx * REGION_CHUNKS) * CELLS, oz = (cz - rz * REGION_CHUNKS) * CELLS;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean changed = false;
		for (int i = 0; i < CELLS; i++) for (int j = 0; j < CELLS; j++) {
			int lx = i * 4 + 2, lz = j * 4 + 2;
			int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
			short h = EMPTY;
			int col = 0;
			if (top >= level.getMinY()) {
				pos.set((cx << 4) + lx, top, (cz << 4) + lz);
				BlockState st = chunk.getBlockState(pos);
				int y = top;
				while (st.isAir() && y > level.getMinY()) {
					y--;
					pos.setY(y);
					st = chunk.getBlockState(pos);
				}
				if (!st.isAir()) {
					h = (short) (y + 1);
					col = colorOf(level, st, pos);
				}
			}
			int idx = (oz + j) * RC + ox + i;
			if (r.height[idx] != h || r.color[idx] != col) {
				r.height[idx] = h;
				r.color[idx] = col;
				changed = true;
			}
		}
		if (changed) {
			r.dirty = true;
			r.version++;
		}
	}

	private record Tex(int color, int tint) {}

	private static final Map<BlockState, Tex> TEX = new ConcurrentHashMap<>();
	private static final Tex NONE = new Tex(-1, -1);

	private static Tex tex(BlockState st) {
		Tex t = TEX.get(st);
		if (t != null) return t;
		t = NONE;
		try {
			var model = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(st);
			net.minecraft.client.renderer.block.model.BakedQuad pick = null;
			for (var part : model.collectParts(net.minecraft.util.RandomSource.create(42L))) {
				for (var q : part.getQuads(net.minecraft.core.Direction.UP)) { pick = q; break; }
				if (pick == null) for (var q : part.getQuads(null)) if (q.direction() == net.minecraft.core.Direction.UP) { pick = q; break; }
				if (pick != null) break;
			}
			var sprite = pick != null ? pick.sprite() : model.particleIcon();
			int avg = average(sprite);
			if (avg != -1) t = new Tex(avg, pick != null && pick.isTinted() ? pick.tintIndex() : -1);
		} catch (Throwable ignored) { }
		TEX.put(st, t);
		return t;
	}

	private static int average(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		var img = ((gg.surge.client.mixin.SpriteContentsAccessor) sprite.contents()).surge$image();
		if (img == null) return -1;
		int w = sprite.contents().width(), h = sprite.contents().height();
		long r = 0, g = 0, b = 0, n = 0;
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
			int c = img.getPixel(x, y);
			if (((c >>> 24) & 255) < 128) continue;
			r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255; n++;
		}
		if (n == 0) return -1;
		return (int) (((r / n) << 16) | ((g / n) << 8) | (b / n));
	}

	private static int multiply(int a, int b) {
		int r = ((a >> 16) & 255) * ((b >> 16) & 255) / 255;
		int g = ((a >> 8) & 255) * ((b >> 8) & 255) / 255;
		int bl = (a & 255) * (b & 255) / 255;
		return (r << 16) | (g << 8) | bl;
	}

	private static int colorOf(ClientLevel level, BlockState st, BlockPos pos) {
		try {
			if (st.getFluidState().is(FluidTags.WATER)) {
				return mix(level.getBiome(pos).value().getWaterColor(), 0x10204A, 0.35f);
			}
			if (st.getFluidState().is(FluidTags.LAVA)) return 0xD9661A;
			Tex t = tex(st);
			if (t != NONE) {
				int c = t.color();
				if (t.tint() >= 0) c = multiply(c, Minecraft.getInstance().getBlockColors().getColor(st, level, pos, t.tint()));
				return c;
			}
			int c = st.getMapColor(level, pos).col;
			return c == 0 ? 0x7F7F7F : c;
		} catch (Throwable t) {
			return 0x7F7F7F;
		}
	}

	private static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
		return (r << 16) | (g << 8) | bl;
	}

	private static Region region(int rx, int rz, boolean create) {
		long k = key(rx, rz);
		Region r = REGIONS.get(k);
		if (r == null && create) {
			r = new Region(rx, rz);
			Region prev = REGIONS.putIfAbsent(k, r);
			if (prev != null) return prev;
			Region fresh = r;
			IO.execute(() -> load(fresh));
		}
		return r;
	}

	private static void load(Region r) {
		if (!Mods.LOD_SAVE.on()) { r.loaded = true; return; }
		Path f = dir().resolve("r." + r.rx + "." + r.rz + ".lod");
		if (Files.exists(f)) {
			try (DataInputStream in = new DataInputStream(new InflaterInputStream(new java.io.BufferedInputStream(Files.newInputStream(f))))) {
				if (in.readInt() == 1) {
					for (int i = 0; i < RC * RC; i++) {
						short h = in.readShort();
						int c = in.readInt();
						if (r.height[i] == EMPTY) { r.height[i] = h; r.color[i] = c; }
					}
					r.version++;
				}
			} catch (Exception ignored) { }
		}
		r.loaded = true;
	}

	private static void saveAll() {
		if (worldKey.isEmpty() || !Mods.LOD_SAVE.on()) return;
		List<Region> dirty = new ArrayList<>();
		for (Region r : REGIONS.values()) if (r.dirty && r.loaded) { r.dirty = false; dirty.add(r); }
		if (dirty.isEmpty()) return;
		Path d = dir();
		IO.execute(() -> {
			try {
				Files.createDirectories(d);
				for (Region r : dirty) {
					Path tmp = d.resolve("r." + r.rx + "." + r.rz + ".tmp");
					try (DataOutputStream out = new DataOutputStream(new DeflaterOutputStream(new java.io.BufferedOutputStream(Files.newOutputStream(tmp))))) {
						out.writeInt(1);
						for (int i = 0; i < RC * RC; i++) { out.writeShort(r.height[i]); out.writeInt(r.color[i]); }
					}
					Files.move(tmp, d.resolve("r." + r.rx + "." + r.rz + ".lod"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (Exception ignored) { }
		});
	}

	private static void ensureRegionsLoaded(int pcx, int pcz) {
		if (!Mods.LOD_SAVE.on()) return;
		int dist = distanceChunks();
		int r0x = Math.floorDiv(pcx - dist, REGION_CHUNKS), r1x = Math.floorDiv(pcx + dist, REGION_CHUNKS);
		int r0z = Math.floorDiv(pcz - dist, REGION_CHUNKS), r1z = Math.floorDiv(pcz + dist, REGION_CHUNKS);
		Path d = dir();
		if (!Files.isDirectory(d)) return;
		for (int rx = r0x; rx <= r1x; rx++) for (int rz = r0z; rz <= r1z; rz++) {
			if (REGIONS.containsKey(key(rx, rz))) continue;
			if (Files.exists(d.resolve("r." + rx + "." + rz + ".lod"))) region(rx, rz, true);
		}
	}

	public static void clear() {
		TEX.clear();
		for (Mesh m : MESHES.values()) if (m.buffer != null) m.buffer.close();
		MESHES.clear();
		REGIONS.clear();
		SAMPLED.clear();
		Built b;
		while ((b = READY.poll()) != null) { b.data().close(); b.bytes().close(); }
	}

	public static void forget() {
		Path d = dir();
		clear();
		IO.execute(() -> {
			try (var s = Files.walk(d)) {
				s.sorted(java.util.Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) { } });
			} catch (Exception ignored) { }
		});
	}

	private static int levelFor(int regionDist) {
		int q = switch (Mods.LOD_QUALITY.get()) { case "Low" -> 0; case "High" -> 2; default -> 1; };
		int near = 1 + q;
		if (regionDist <= near) return 0;
		if (regionDist <= near * 2 + 1) return 1;
		if (regionDist <= near * 4 + 3) return 2;
		return 3;
	}

	private static int lastCam = Integer.MIN_VALUE;
	private static long lastLoadScan;

	public static void render(Matrix4fc modelView, Vec3 cam) {
		if (!active()) return;
		Minecraft mc = Minecraft.getInstance();
		int pcx = (int) Math.floor(cam.x) >> 4, pcz = (int) Math.floor(cam.z) >> 4;
		long now = System.currentTimeMillis();
		if (now - lastLoadScan > 3000) {
			lastLoadScan = now;
			ensureRegionsLoaded(pcx, pcz);
		}
		Built b;
		int uploads = 0;
		while (uploads < 6 && (b = READY.poll()) != null) {
			BUILDING.remove(b.key());
			Mesh m = MESHES.computeIfAbsent(b.key(), k -> new Mesh());
			if (m.buffer != null) { m.buffer.close(); m.buffer = null; }
			if (b.data().drawState().indexCount() > 0) {
				m.buffer = RenderSystem.getDevice().createBuffer(() -> "surge lod", GpuBuffer.USAGE_VERTEX, b.data().vertexBuffer());
				m.indexCount = b.data().drawState().indexCount();
			} else m.indexCount = 0;
			m.level = b.level();
			m.version = b.version();
			m.hasHole = b.hole();
			m.holeX = b.hx();
			m.holeZ = b.hz();
			m.holeR = b.hr();
			m.minY = b.minY();
			m.maxY = b.maxY();
			b.data().close();
			b.bytes().close();
			uploads++;
		}

		int dist = distanceChunks();
		int rd = mc.options.getEffectiveRenderDistance();
		int hole = Math.max(0, rd - 1);
		int regionRange = dist / REGION_CHUNKS + 1;
		int prx = Math.floorDiv(pcx, REGION_CHUNKS), prz = Math.floorDiv(pcz, REGION_CHUNKS);

		Iterator<Map.Entry<Long, Mesh>> it = MESHES.entrySet().iterator();
		while (it.hasNext()) {
			var e = it.next();
			int rx = (int) (e.getKey() >> 32), rz = (int) (long) e.getKey();
			if (Math.max(Math.abs(rx - prx), Math.abs(rz - prz)) > regionRange + 1) {
				if (e.getValue().buffer != null) e.getValue().buffer.close();
				it.remove();
			}
		}

		for (Region r : REGIONS.values()) {
			int rdist = Math.max(Math.abs(r.rx - prx), Math.abs(r.rz - prz));
			if (rdist > regionRange) continue;
			int lvl = levelFor(rdist);
			int x0 = r.rx * REGION_CHUNKS, z0 = r.rz * REGION_CHUNKS;
			boolean touches = pcx + hole >= x0 && pcx - hole < x0 + REGION_CHUNKS && pcz + hole >= z0 && pcz - hole < z0 + REGION_CHUNKS;
			Mesh m = MESHES.get(key(r.rx, r.rz));
			boolean stale = m == null || m.level != lvl || m.version != r.version
				|| (touches != m.hasHole) || (touches && (Math.abs(m.holeX - pcx) > 1 || Math.abs(m.holeZ - pcz) > 1 || m.holeR != hole));
			if (stale && r.loaded && BUILDING.size() < 3 && BUILDING.add(key(r.rx, r.rz))) {
				int version = r.version, hx = pcx, hz = pcz;
				BUILDER.execute(() -> build(r, lvl, version, touches, hx, hz, hole));
			}
		}

		drawn = 0;
		renders++;
		if (MESHES.isEmpty()) return;
		RenderTarget target = mc.getMainRenderTarget();
		var idx = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		int maxIndex = 0;
		for (Mesh m : MESHES.values()) maxIndex = Math.max(maxIndex, m.indexCount);
		if (maxIndex == 0) return;
		GpuBuffer indexBuffer = idx.getBuffer(maxIndex);
		Vector4f white = new Vector4f(1f, 1f, 1f, 1f);
		Matrix4f identity = new Matrix4f();
		List<Mesh> draw = new ArrayList<>();
		List<GpuBufferSlice> slices = new ArrayList<>();
		try {
			for (Map.Entry<Long, Mesh> e : MESHES.entrySet()) {
				Mesh m = e.getValue();
				if (m.buffer == null || m.indexCount == 0) continue;
				int rx = (int) (e.getKey() >> 32), rz = (int) (long) e.getKey();
				double bx = rx * REGION_CHUNKS * 16.0, bz = rz * REGION_CHUNKS * 16.0;
				if (frustum != null && !frustum.isVisible(new AABB(bx, m.minY - 1, bz, bx + REGION_CHUNKS * 16, m.maxY + 1, bz + REGION_CHUNKS * 16))) continue;
				Matrix4f mv = new Matrix4f(modelView).translate((float) (bx - cam.x), (float) -cam.y, (float) (bz - cam.z));
				slices.add(RenderSystem.getDynamicUniforms().writeTransform(mv, white, new Vector3f(), identity));
				draw.add(m);
			}
			if (draw.isEmpty()) return;
			try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "surge distant terrain", target.getColorTextureView(), OptionalInt.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
				pass.setPipeline(PIPELINE);
				RenderSystem.bindDefaultUniforms(pass);
				pass.setIndexBuffer(indexBuffer, idx.type());
				for (int i = 0; i < draw.size(); i++) {
					Mesh m = draw.get(i);
					pass.setUniform("DynamicTransforms", slices.get(i));
					pass.setVertexBuffer(0, m.buffer);
					pass.drawIndexed(0, 0, m.indexCount, 1);
					drawn++;
				}
			}
		} catch (Throwable t) {
			fail("render", t);
		}
	}

	private static int argb(int c, float k) {
		int r = Math.min(255, Math.round(((c >> 16) & 255) * k));
		int g = Math.min(255, Math.round(((c >> 8) & 255) * k));
		int b = Math.min(255, Math.round((c & 255) * k));
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private static void build(Region r, int lvl, int version, boolean useHole, int hx, int hz, int hole) {
		try {
			int step = 1 << lvl;
			int n = RC / step;
			float cell = 4f * step;
			short[] hs = new short[n * n];
			int[] cs = new int[n * n];
			int x0c = r.rx * REGION_CHUNKS, z0c = r.rz * REGION_CHUNKS;
			for (int gz = 0; gz < n; gz++) for (int gx = 0; gx < n; gx++) {
				int best = EMPTY;
				long sr = 0, sg = 0, sb = 0, cnt = 0;
				for (int dz = 0; dz < step; dz++) for (int dx = 0; dx < step; dx++) {
					int i = (gz * step + dz) * RC + gx * step + dx;
					short h = r.height[i];
					if (h == EMPTY) continue;
					if (h > best) best = h;
					int c = r.color[i];
					sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; cnt++;
				}
				int cx = x0c + (gx * step) / CELLS, cz = z0c + (gz * step) / CELLS;
				if (useHole && Math.abs(cx - hx) <= hole && Math.abs(cz - hz) <= hole) best = EMPTY;
				hs[gz * n + gx] = (short) best;
				cs[gz * n + gx] = cnt == 0 ? 0 : (int) (((sr / cnt) << 16) | ((sg / cnt) << 8) | (sb / cnt));
			}
			ByteBufferBuilder bytes = new ByteBufferBuilder(1 << 16);
			BufferBuilder bb = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
			float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
			int quads = 0;
			for (int gz = 0; gz < n; gz++) for (int gx = 0; gx < n; gx++) {
				int h = hs[gz * n + gx];
				if (h == EMPTY) continue;
				int c = cs[gz * n + gx];
				float y = h - 0.15f;
				float x = gx * cell, z = gz * cell;
				int west = gx > 0 ? hs[gz * n + gx - 1] : EMPTY, north = gz > 0 ? hs[(gz - 1) * n + gx] : EMPTY;
				float slope = 1f;
				if (west != EMPTY) slope += Math.max(-0.18f, Math.min(0.18f, (h - west) * 0.025f));
				if (north != EMPTY) slope += Math.max(-0.12f, Math.min(0.12f, (h - north) * 0.02f));
				int top = argb(c, slope);
				bb.addVertex(x, y, z).setColor(top);
				bb.addVertex(x, y, z + cell).setColor(top);
				bb.addVertex(x + cell, y, z + cell).setColor(top);
				bb.addVertex(x + cell, y, z).setColor(top);
				quads++;
				minY = Math.min(minY, y);
				maxY = Math.max(maxY, y);
				int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
				for (int[] dv : dirs) {
					int nx = gx + dv[0], nz = gz + dv[1];
					int nh = nx >= 0 && nz >= 0 && nx < n && nz < n ? hs[nz * n + nx] : EMPTY;
					float bottom;
					if (nh == EMPTY) {
						if (nx >= 0 && nz >= 0 && nx < n && nz < n) bottom = y - 6f;
						else continue;
					} else if (nh < h) bottom = nh - 0.15f;
					else continue;
					int sc = argb(c, dv[0] != 0 ? 0.78f : 0.66f);
					minY = Math.min(minY, bottom);
					if (dv[0] == 1) {
						float xx = x + cell;
						bb.addVertex(xx, y, z).setColor(sc); bb.addVertex(xx, y, z + cell).setColor(sc);
						bb.addVertex(xx, bottom, z + cell).setColor(sc); bb.addVertex(xx, bottom, z).setColor(sc);
					} else if (dv[0] == -1) {
						bb.addVertex(x, y, z + cell).setColor(sc); bb.addVertex(x, y, z).setColor(sc);
						bb.addVertex(x, bottom, z).setColor(sc); bb.addVertex(x, bottom, z + cell).setColor(sc);
					} else if (dv[1] == 1) {
						float zz = z + cell;
						bb.addVertex(x + cell, y, zz).setColor(sc); bb.addVertex(x, y, zz).setColor(sc);
						bb.addVertex(x, bottom, zz).setColor(sc); bb.addVertex(x + cell, bottom, zz).setColor(sc);
					} else {
						bb.addVertex(x, y, z).setColor(sc); bb.addVertex(x + cell, y, z).setColor(sc);
						bb.addVertex(x + cell, bottom, z).setColor(sc); bb.addVertex(x, bottom, z).setColor(sc);
					}
					quads++;
				}
			}
			MeshData data = quads == 0 ? null : bb.build();
			if (data == null) {
				bytes.close();
				BUILDING.remove(key(r.rx, r.rz));
				Minecraft.getInstance().execute(() -> {
					Mesh m = MESHES.computeIfAbsent(key(r.rx, r.rz), k -> new Mesh());
					if (m.buffer != null) { m.buffer.close(); m.buffer = null; }
					m.indexCount = 0; m.level = lvl; m.version = version; m.hasHole = useHole; m.holeX = hx; m.holeZ = hz; m.holeR = hole;
				});
				return;
			}
			READY.add(new Built(key(r.rx, r.rz), data, bytes, lvl, version, useHole, hx, hz, hole, minY, maxY));
		} catch (Throwable t) {
			BUILDING.remove(key(r.rx, r.rz));
			fail("build", t);
		}
	}

	private static int drawn, renders;
	private static boolean warned;

	private static void fail(String where, Throwable t) {
		if (warned) return;
		warned = true;
		org.slf4j.LoggerFactory.getLogger("surge").warn("Distant terrain " + where + " failed", t);
	}

	public static String stats() {
		return "regions=" + REGIONS.size() + " meshes=" + MESHES.size() + " drawn=" + drawn + " renders=" + renders + " sampled=" + SAMPLED.size();
	}
}
