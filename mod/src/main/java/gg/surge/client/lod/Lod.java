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
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
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
	static final int REGION_CHUNKS = 32;
	static final int RC = REGION_CHUNKS * 16;
	static final int MAX_LEVEL = 4;
	static final short EMPTY = Short.MIN_VALUE;
	private static final int FORMAT = 3;
	private static final float SINK = 0.08f;

	private static final RenderPipeline COLOR = RenderPipeline.builder()
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

	private static final RenderPipeline TEXTURED = RenderPipeline.builder()
		.withLocation(Identifier.fromNamespaceAndPath("surge", "pipeline/lod_textured"))
		.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
		.withUniform("Projection", UniformType.UNIFORM_BUFFER)
		.withVertexShader("core/position_tex_color")
		.withFragmentShader("core/position_tex_color")
		.withSampler("Sampler0")
		.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
		.withCull(false)
		.withDepthWrite(true)
		.withoutBlend()
		.build();

	private static final ExecutorService BUILDER = Executors.newFixedThreadPool(2, r -> { Thread t = new Thread(r, "Surge distant terrain"); t.setDaemon(true); t.setPriority(Thread.MIN_PRIORITY); return t; });
	private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "Surge terrain save"); t.setDaemon(true); return t; });

	static final class Region {
		final int rx, rz;
		volatile short[] h, st, tn;
		volatile BlockState[] pal = new BlockState[16];
		volatile int[] tints = new int[16];
		int palSize = 1, tintSize = 1;
		final Map<BlockState, Integer> palIdx = new HashMap<>();
		final Map<Integer, Integer> tintIdx = new HashMap<>();
		final short[][] mh = new short[MAX_LEVEL + 1][];
		final int[][] mc = new int[MAX_LEVEL + 1][];
		final int[] mipVersion = new int[MAX_LEVEL + 1];
		volatile boolean dirty, loaded, loading, dropping;
		volatile int version = 1;

		Region(int rx, int rz) {
			this.rx = rx;
			this.rz = rz;
			tints[0] = 0xFFFFFF;
			java.util.Arrays.fill(mipVersion, -1);
		}

		void allocate() {
			short[] a = new short[RC * RC];
			java.util.Arrays.fill(a, EMPTY);
			st = new short[RC * RC];
			tn = new short[RC * RC];
			h = a;
		}

		int state(BlockState s) {
			Integer i = palIdx.get(s);
			if (i != null) return i;
			if (palSize >= 65000) return 0;
			if (palSize == pal.length) pal = java.util.Arrays.copyOf(pal, pal.length * 2);
			pal[palSize] = s;
			palIdx.put(s, palSize);
			return palSize++;
		}

		int tint(int c) {
			Integer i = tintIdx.get(c);
			if (i != null) return i;
			if (tintSize >= 65000) return 0;
			if (tintSize == tints.length) tints = java.util.Arrays.copyOf(tints, tints.length * 2);
			tints[tintSize] = c;
			tintIdx.put(c, tintSize);
			return tintSize++;
		}
	}

	static final class Mesh {
		GpuBuffer col, tex;
		int colCount, texCount;
		int level, version, holeX, holeZ, holeR;
		boolean hasHole;
		float minY, maxY;

		void close() {
			if (col != null) col.close();
			if (tex != null) tex.close();
			col = tex = null;
			colCount = texCount = 0;
		}
	}

	private record Part(MeshData data, ByteBufferBuilder bytes) {
		void close() {
			if (data != null) data.close();
			bytes.close();
		}
	}

	private record Built(long key, Part col, Part tex, int level, int version, boolean hole, int hx, int hz, int hr, float minY, float maxY) {}

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

	private static int fingerprint() {
		return Block.BLOCK_STATE_REGISTRY.size() * 31 + FORMAT;
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
		int budget = 6;
		for (int n = 0; n < total && budget > 0; n++) {
			int idx = (scanIndex++) % total;
			int cx = pcx - rd + idx % side, cz = pcz - rd + idx / side;
			long k = key(cx, cz);
			Long at = SAMPLED.get(k);
			if (at != null && now - at < 45_000) continue;
			LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
			if (chunk == null || chunk.isEmpty()) continue;
			Region r = region(Math.floorDiv(cx, REGION_CHUNKS), Math.floorDiv(cz, REGION_CHUNKS), true);
			if (r.h == null || !r.loaded) continue;
			SAMPLED.put(k, now);
			sampleChunk(level, chunk, r, cx, cz);
			budget--;
		}
		if (SAMPLED.size() > 20_000) SAMPLED.clear();
	}

	private static boolean see(BlockState st, LevelChunk chunk, BlockPos pos) {
		if (st.isAir()) return false;
		if (!st.getFluidState().isEmpty()) return true;
		if (st.is(Blocks.SNOW)) return true;
		return !st.getCollisionShape(chunk, pos).isEmpty();
	}

	private static void sampleChunk(ClientLevel level, LevelChunk chunk, Region r, int cx, int cz) {
		short[] hs = r.h, ss = r.st, ts = r.tn;
		if (hs == null) return;
		int ox = (cx - r.rx * REGION_CHUNKS) * 16, oz = (cz - r.rz * REGION_CHUNKS) * 16;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean changed = false;
		int minY = level.getMinY();
		for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) {
			int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
			short h = EMPTY;
			int s = 0, t = 0;
			if (top >= minY) {
				int y = top;
				pos.set((cx << 4) + lx, y, (cz << 4) + lz);
				BlockState st = chunk.getBlockState(pos);
				int guard = 0;
				while (!see(st, chunk, pos) && y > minY && guard++ < 64) {
					y--;
					pos.setY(y);
					st = chunk.getBlockState(pos);
				}
				if (!st.isAir()) {
					h = (short) (y + 1);
					BlockState shown = st.getFluidState().is(FluidTags.WATER) ? Blocks.WATER.defaultBlockState() : st;
					s = r.state(shown);
					t = r.tint(tintOf(level, shown, pos));
				}
			}
			int idx = (oz + lz) * RC + ox + lx;
			if (hs[idx] != h || ss[idx] != s || ts[idx] != t) {
				hs[idx] = h;
				ss[idx] = (short) s;
				ts[idx] = (short) t;
				changed = true;
			}
		}
		if (changed) {
			r.dirty = true;
			r.version++;
		}
	}

	private static int tintOf(ClientLevel level, BlockState st, BlockPos pos) {
		try {
			if (st.is(Blocks.WATER)) return level.getBiome(pos).value().getWaterColor() & 0xFFFFFF;
			Info info = info(st);
			if (info.tintIndex >= 0) return Minecraft.getInstance().getBlockColors().getColor(st, level, pos, info.tintIndex) & 0xFFFFFF;
		} catch (Throwable ignored) { }
		return 0xFFFFFF;
	}

	static final class Info {
		TextureAtlasSprite top, side, under;
		boolean topClear, sideTinted;
		int tintIndex = -1;
		int color = 0x7F7F7F;
	}

	private static final Map<BlockState, Info> INFO = new ConcurrentHashMap<>();

	private static Info info(BlockState st) {
		Info i = INFO.get(st);
		if (i != null) return i;
		i = new Info();
		try {
			var model = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(st);
			BakedQuad up = null, side = null;
			for (var part : model.collectParts(net.minecraft.util.RandomSource.create(42L))) {
				if (up == null) for (var q : part.getQuads(Direction.UP)) { up = q; break; }
				if (side == null) for (var q : part.getQuads(Direction.NORTH)) { side = q; break; }
				if (up == null || side == null) for (var q : part.getQuads(null)) {
					if (up == null && q.direction() == Direction.UP) up = q;
					if (side == null && q.direction() == Direction.NORTH) side = q;
				}
			}
			TextureAtlasSprite particle = model.particleIcon();
			i.top = up != null ? up.sprite() : particle;
			i.side = side != null ? side.sprite() : i.top;
			i.under = particle;
			if (up != null && up.isTinted()) i.tintIndex = up.tintIndex();
			i.sideTinted = side != null && side.isTinted();
			int[] avg = average(i.top);
			if (avg != null) { i.color = avg[0]; i.topClear = avg[1] != 0; }
			else i.color = st.getMapColor(Minecraft.getInstance().level, BlockPos.ZERO).col;
		} catch (Throwable ignored) { }
		INFO.put(st, i);
		return i;
	}

	private static int[] average(TextureAtlasSprite sprite) {
		var img = ((gg.surge.client.mixin.SpriteContentsAccessor) sprite.contents()).surge$image();
		if (img == null) return null;
		int w = sprite.contents().width(), h = Math.min(sprite.contents().height(), w);
		long r = 0, g = 0, b = 0, n = 0;
		int clear = 0;
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
			int c = img.getPixel(x, y);
			if (((c >>> 24) & 255) < 128) { clear++; continue; }
			r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255; n++;
		}
		if (n == 0) return null;
		return new int[]{(int) (((r / n) << 16) | ((g / n) << 8) | (b / n)), clear > 0 ? 1 : 0};
	}

	private static int multiply(int a, int b) {
		int r = ((a >> 16) & 255) * ((b >> 16) & 255) / 255;
		int g = ((a >> 8) & 255) * ((b >> 8) & 255) / 255;
		int bl = (a & 255) * (b & 255) / 255;
		return (r << 16) | (g << 8) | bl;
	}

	private static Region region(int rx, int rz, boolean create) {
		long k = key(rx, rz);
		Region r = REGIONS.get(k);
		if (r == null && create) {
			r = new Region(rx, rz);
			Region prev = REGIONS.putIfAbsent(k, r);
			if (prev != null) return prev;
			r.allocate();
			r.loading = true;
			Region fresh = r;
			IO.execute(() -> load(fresh));
		}
		return r;
	}

	private static void load(Region r) {
		try {
			if (!Mods.LOD_SAVE.on()) return;
			Path f = dir().resolve("r." + r.rx + "." + r.rz + ".lod");
			if (!Files.exists(f)) return;
			try (DataInputStream in = new DataInputStream(new InflaterInputStream(new java.io.BufferedInputStream(Files.newInputStream(f), 1 << 16)))) {
				if (in.readInt() != fingerprint()) return;
				int ns = in.readInt();
				BlockState[] states = new BlockState[ns];
				for (int i = 0; i < ns; i++) states[i] = Block.stateById(in.readInt());
				int nt = in.readInt();
				int[] tints = new int[nt];
				for (int i = 0; i < nt; i++) tints[i] = in.readInt();
				short[] fh = new short[RC * RC], fs = new short[RC * RC], ft = new short[RC * RC];
				for (int i = 0; i < RC * RC; i++) fh[i] = in.readShort();
				for (int i = 0; i < RC * RC; i++) fs[i] = in.readShort();
				for (int i = 0; i < RC * RC; i++) ft[i] = in.readShort();
				Minecraft.getInstance().execute(() -> merge(r, states, tints, fh, fs, ft));
				return;
			}
		} catch (Exception ignored) {
		} finally {
			if (!r.loaded) Minecraft.getInstance().execute(() -> { r.loaded = true; r.loading = false; });
		}
	}

	private static void merge(Region r, BlockState[] states, int[] tints, short[] fh, short[] fs, short[] ft) {
		if (r.h == null) r.allocate();
		short[] hs = r.h, ss = r.st, ts = r.tn;
		int[] smap = new int[states.length], tmap = new int[tints.length];
		for (int i = 1; i < states.length; i++) smap[i] = states[i] == null ? 0 : r.state(states[i]);
		for (int i = 1; i < tints.length; i++) tmap[i] = r.tint(tints[i]);
		for (int i = 0; i < RC * RC; i++) {
			if (hs[i] != EMPTY || fh[i] == EMPTY) continue;
			hs[i] = fh[i];
			int s = fs[i] & 0xFFFF, t = ft[i] & 0xFFFF;
			ss[i] = (short) (s < smap.length ? smap[s] : 0);
			ts[i] = (short) (t < tmap.length ? tmap[t] : 0);
		}
		r.version++;
		r.loaded = true;
		r.loading = false;
	}

	private static void saveAll() {
		if (worldKey.isEmpty() || !Mods.LOD_SAVE.on()) return;
		List<Region> dirty = new ArrayList<>();
		for (Region r : REGIONS.values()) if (r.dirty && r.loaded && r.h != null) { r.dirty = false; dirty.add(r); }
		if (dirty.isEmpty()) return;
		Path d = dir();
		int fp = fingerprint();
		List<Object[]> snaps = new ArrayList<>();
		for (Region r : dirty) {
			int[] ids = new int[r.palSize];
			for (int i = 1; i < r.palSize; i++) ids[i] = Block.getId(r.pal[i]);
			snaps.add(new Object[]{r, ids, java.util.Arrays.copyOf(r.tints, r.tintSize), r.h.clone(), r.st.clone(), r.tn.clone()});
		}
		IO.execute(() -> {
			try {
				Files.createDirectories(d);
				for (Object[] s : snaps) {
					Region r = (Region) s[0];
					int[] ids = (int[]) s[1], tints = (int[]) s[2];
					short[] fh = (short[]) s[3], fs = (short[]) s[4], ft = (short[]) s[5];
					Path tmp = d.resolve("r." + r.rx + "." + r.rz + ".tmp");
					try (DataOutputStream out = new DataOutputStream(new java.io.BufferedOutputStream(new DeflaterOutputStream(Files.newOutputStream(tmp)), 1 << 16))) {
						out.writeInt(fp);
						out.writeInt(ids.length);
						for (int id : ids) out.writeInt(id);
						out.writeInt(tints.length);
						for (int t : tints) out.writeInt(t);
						for (short v : fh) out.writeShort(v);
						for (short v : fs) out.writeShort(v);
						for (short v : ft) out.writeShort(v);
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
		for (Mesh m : MESHES.values()) m.close();
		MESHES.clear();
		REGIONS.clear();
		SAMPLED.clear();
		INFO.clear();
		Built b;
		while ((b = READY.poll()) != null) { if (b.col() != null) b.col().close(); if (b.tex() != null) b.tex().close(); }
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
		int base;
		int[] steps;
		switch (Mods.LOD_QUALITY.get()) {
			case "Low" -> { base = 3; steps = new int[]{2, 6}; }
			case "Medium" -> { base = 2; steps = new int[]{2, 5, 11}; }
			case "Ultra" -> { base = 0; steps = new int[]{0, 2, 6, 12}; }
			case "Full" -> { base = 0; steps = new int[]{1, 3, 8, 16}; }
			default -> { base = 1; steps = new int[]{1, 4, 9}; }
		}
		int lvl = base;
		for (int t : steps) if (regionDist > t) lvl++;
		return Math.min(MAX_LEVEL, lvl);
	}

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
			m.close();
			if (b.col() != null && b.col().data() != null) {
				m.col = RenderSystem.getDevice().createBuffer(() -> "surge lod", GpuBuffer.USAGE_VERTEX, b.col().data().vertexBuffer());
				m.colCount = b.col().data().drawState().indexCount();
			}
			if (b.tex() != null && b.tex().data() != null) {
				m.tex = RenderSystem.getDevice().createBuffer(() -> "surge lod textured", GpuBuffer.USAGE_VERTEX, b.tex().data().vertexBuffer());
				m.texCount = b.tex().data().drawState().indexCount();
			}
			m.level = b.level();
			m.version = b.version();
			m.hasHole = b.hole();
			m.holeX = b.hx();
			m.holeZ = b.hz();
			m.holeR = b.hr();
			m.minY = b.minY();
			m.maxY = b.maxY();
			if (b.col() != null) b.col().close();
			if (b.tex() != null) b.tex().close();
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
				e.getValue().close();
				it.remove();
			}
		}

		REGIONS.values().removeIf(r -> Math.max(Math.abs(r.rx - prx), Math.abs(r.rz - prz)) > regionRange + 2 && r.loaded && (!r.dirty || !Mods.LOD_SAVE.on()));
		for (Region r : REGIONS.values()) {
			int rdist = Math.max(Math.abs(r.rx - prx), Math.abs(r.rz - prz));
			if (rdist > regionRange) continue;
			int lvl = levelFor(rdist);
			if (r.h == null && lvl < 2) lvl = 2;
			if (r.h == null && rdist <= 4 && !r.loading && Mods.LOD_SAVE.on()) {
				r.loading = true;
				r.loaded = false;
				Region rr = r;
				IO.execute(() -> load(rr));
			}
			if (r.h == null && r.mh[lvl] == null) continue;
			if (r.h != null && rdist >= 6 && !r.dirty && r.loaded && !r.dropping && !BUILDING.contains(key(r.rx, r.rz))) {
				r.dropping = true;
				Region rr = r;
				BUILDER.execute(() -> drop(rr));
			}
			int x0 = r.rx * REGION_CHUNKS, z0 = r.rz * REGION_CHUNKS;
			boolean touches = pcx + hole >= x0 && pcx - hole < x0 + REGION_CHUNKS && pcz + hole >= z0 && pcz - hole < z0 + REGION_CHUNKS;
			Mesh m = MESHES.get(key(r.rx, r.rz));
			boolean stale = m == null || m.level != lvl || m.version != r.version
				|| (touches != m.hasHole) || (touches && (Math.abs(m.holeX - pcx) > 1 || Math.abs(m.holeZ - pcz) > 1 || m.holeR != hole));
			if (stale && r.loaded && !r.dropping && BUILDING.size() < 3 && BUILDING.add(key(r.rx, r.rz))) {
				int version = r.version, hx = pcx, hz = pcz, l = lvl;
				BUILDER.execute(() -> build(r, l, version, touches, hx, hz, hole));
			}
		}

		drawn = 0;
		renders++;
		if (MESHES.isEmpty()) return;
		RenderTarget target = mc.getMainRenderTarget();
		var idx = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		int maxIndex = 0;
		for (Mesh m : MESHES.values()) maxIndex = Math.max(maxIndex, Math.max(m.colCount, m.texCount));
		if (maxIndex == 0) return;
		GpuBuffer indexBuffer = idx.getBuffer(maxIndex);
		Vector4f white = new Vector4f(1f, 1f, 1f, 1f);
		Matrix4f identity = new Matrix4f();
		List<Mesh> draw = new ArrayList<>();
		List<GpuBufferSlice> slices = new ArrayList<>();
		try {
			for (Map.Entry<Long, Mesh> e : MESHES.entrySet()) {
				Mesh m = e.getValue();
				if (m.colCount == 0 && m.texCount == 0) continue;
				int rx = (int) (e.getKey() >> 32), rz = (int) (long) e.getKey();
				double bx = rx * (double) RC, bz = rz * (double) RC;
				if (frustum != null && !frustum.isVisible(new AABB(bx, m.minY - 1, bz, bx + RC, m.maxY + 1, bz + RC))) continue;
				Matrix4f mv = new Matrix4f(modelView).translate((float) (bx - cam.x), (float) -cam.y, (float) (bz - cam.z));
				slices.add(RenderSystem.getDynamicUniforms().writeTransform(mv, white, new Vector3f(), identity));
				draw.add(m);
			}
			if (draw.isEmpty()) return;
			var atlas = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
			try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "surge distant terrain", target.getColorTextureView(), OptionalInt.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
				pass.setPipeline(COLOR);
				RenderSystem.bindDefaultUniforms(pass);
				pass.setIndexBuffer(indexBuffer, idx.type());
				for (int i = 0; i < draw.size(); i++) {
					Mesh m = draw.get(i);
					if (m.col == null || m.colCount == 0) continue;
					pass.setUniform("DynamicTransforms", slices.get(i));
					pass.setVertexBuffer(0, m.col);
					pass.drawIndexed(0, 0, m.colCount, 1);
					drawn++;
				}
				boolean bound = false;
				for (int i = 0; i < draw.size(); i++) {
					Mesh m = draw.get(i);
					if (m.tex == null || m.texCount == 0) continue;
					if (!bound) {
						pass.setPipeline(TEXTURED);
						RenderSystem.bindDefaultUniforms(pass);
						pass.setIndexBuffer(indexBuffer, idx.type());
						pass.bindTexture("Sampler0", atlas.getTextureView(), atlas.getSampler());
						bound = true;
					}
					pass.setUniform("DynamicTransforms", slices.get(i));
					pass.setVertexBuffer(0, m.tex);
					pass.drawIndexed(0, 0, m.texCount, 1);
					drawn++;
				}
			}
		} catch (Throwable t) {
			fail("render", t);
		}
	}

	private static void drop(Region r) {
		try {
			short[] hs = r.h;
			if (hs == null) return;
			for (int l = 2; l <= MAX_LEVEL; l++) mip(r, l);
			if (!r.dirty) {
				r.h = null;
				r.st = null;
				r.tn = null;
				r.mh[0] = r.mh[1] = null;
				r.mc[0] = r.mc[1] = null;
			}
		} catch (Throwable t) {
			fail("drop", t);
		} finally {
			r.dropping = false;
		}
	}

	private static int cellColor(Region r, int i, short[] ss, short[] ts) {
		BlockState[] pal = r.pal;
		int[] tints = r.tints;
		int s = ss[i] & 0xFFFF, t = ts[i] & 0xFFFF;
		BlockState st = s < pal.length ? pal[s] : null;
		if (st == null) return 0x7F7F7F;
		int c = info(st).color;
		int tint = t < tints.length ? tints[t] : 0xFFFFFF;
		return tint == 0xFFFFFF ? c : multiply(c, tint);
	}

	private static void mip(Region r, int l) {
		if (r.mipVersion[l] == r.version && r.mh[l] != null) return;
		short[] hs = r.h, ss = r.st, ts = r.tn;
		if (hs == null) return;
		int version = r.version;
		int step = 1 << l, n = RC >> l;
		short[] oh = new short[n * n];
		int[] oc = new int[n * n];
		for (int gz = 0; gz < n; gz++) for (int gx = 0; gx < n; gx++) {
			int best = EMPTY;
			long sr = 0, sg = 0, sb = 0, cnt = 0;
			for (int dz = 0; dz < step; dz++) for (int dx = 0; dx < step; dx++) {
				int i = (gz * step + dz) * RC + gx * step + dx;
				short h = hs[i];
				if (h == EMPTY) continue;
				if (h > best) best = h;
				int c = cellColor(r, i, ss, ts);
				sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; cnt++;
			}
			oh[gz * n + gx] = (short) best;
			oc[gz * n + gx] = cnt == 0 ? 0 : (int) (((sr / cnt) << 16) | ((sg / cnt) << 8) | (sb / cnt));
		}
		r.mh[l] = oh;
		r.mc[l] = oc;
		r.mipVersion[l] = version;
	}

	private static int argb(int c, float k) {
		int r = Math.min(255, Math.round(((c >> 16) & 255) * k));
		int g = Math.min(255, Math.round(((c >> 8) & 255) * k));
		int b = Math.min(255, Math.round((c & 255) * k));
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private static short neighborHeight(Region r, int level, int gx, int gz, int n) {
		int rx = r.rx, rz = r.rz;
		if (gx < 0) { rx--; gx += n; } else if (gx >= n) { rx++; gx -= n; }
		if (gz < 0) { rz--; gz += n; } else if (gz >= n) { rz++; gz -= n; }
		if (rx == r.rx && rz == r.rz) return EMPTY;
		Region o = REGIONS.get(key(rx, rz));
		if (o == null) return EMPTY;
		short[] a = level == 0 ? o.h : o.mh[level];
		if (a == null || a.length != n * n) return EMPTY;
		return a[gz * n + gx];
	}

	private static final int[][] DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private static void build(Region r, int lvl, int version, boolean useHole, int hx, int hz, int hole) {
		try {
			if (lvl == 0 && r.h != null) buildFull(r, version, useHole, hx, hz, hole);
			else buildColored(r, lvl, version, useHole, hx, hz, hole);
		} catch (Throwable t) {
			BUILDING.remove(key(r.rx, r.rz));
			fail("build", t);
		}
	}

	private static Part finish(BufferBuilder bb, ByteBufferBuilder bytes, int quads) {
		if (quads == 0) { bytes.close(); return null; }
		return new Part(bb.build(), bytes);
	}

	private static void buildColored(Region r, int lvl, int version, boolean useHole, int hx, int hz, int hole) {
		if (r.h != null) mip(r, lvl);
		short[] hsrc = r.mh[lvl];
		int[] csrc = r.mc[lvl];
		if (hsrc == null) { BUILDING.remove(key(r.rx, r.rz)); return; }
		int step = 1 << lvl;
		int n = RC >> lvl;
		float cell = step;
		short[] hs = hsrc.clone();
		int x0b = r.rx * RC, z0b = r.rz * RC;
		if (useHole) for (int gz = 0; gz < n; gz++) for (int gx = 0; gx < n; gx++) {
			int cx = (x0b + gx * step) >> 4, cz = (z0b + gz * step) >> 4;
			if (Math.abs(cx - hx) <= hole && Math.abs(cz - hz) <= hole) hs[gz * n + gx] = EMPTY;
		}
		ByteBufferBuilder bytes = new ByteBufferBuilder(1 << 16);
		BufferBuilder bb = new BufferBuilder(bytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		int quads = 0;
		for (int gz = 0; gz < n; gz++) for (int gx = 0; gx < n; gx++) {
			int h = hs[gz * n + gx];
			if (h == EMPTY) continue;
			int c = csrc[gz * n + gx];
			float y = h - SINK;
			float x = gx * cell, z = gz * cell;
			int west = gx > 0 ? hs[gz * n + gx - 1] : EMPTY, north = gz > 0 ? hs[(gz - 1) * n + gx] : EMPTY;
			float slope = 1f;
			if (west != EMPTY) slope += Math.max(-0.15f, Math.min(0.15f, (h - west) * 0.03f / step));
			if (north != EMPTY) slope += Math.max(-0.1f, Math.min(0.1f, (h - north) * 0.025f / step));
			int top = argb(c, slope);
			bb.addVertex(x, y, z).setColor(top);
			bb.addVertex(x, y, z + cell).setColor(top);
			bb.addVertex(x + cell, y, z + cell).setColor(top);
			bb.addVertex(x + cell, y, z).setColor(top);
			quads++;
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
			for (int[] dv : DIRS) {
				int nx = gx + dv[0], nz = gz + dv[1];
				boolean inside = nx >= 0 && nz >= 0 && nx < n && nz < n;
				int nh = inside ? hs[nz * n + nx] : neighborHeight(r, lvl, nx, nz, n);
				float bottom;
				if (nh == EMPTY) {
					if (inside && hsrc[nz * n + nx] == EMPTY) bottom = y - 4f * step;
					else if (inside) bottom = y - 2f;
					else continue;
				} else if (nh < h) bottom = nh - SINK;
				else continue;
				int sc = argb(c, dv[0] != 0 ? 0.6f : 0.8f);
				minY = Math.min(minY, bottom);
				side(bb, null, x, z, cell, dv, y, bottom, sc);
				quads++;
			}
		}
		Part col = finish(bb, bytes, quads);
		READY.add(new Built(key(r.rx, r.rz), col, null, lvl, version, useHole, hx, hz, hole, minY == Float.MAX_VALUE ? 0 : minY, maxY == -Float.MAX_VALUE ? 0 : maxY));
	}

	private static void side(BufferBuilder bb, float[] uv, float x, float z, float cell, int[] dv, float y, float bottom, int color) {
		float u0 = 0, u1 = 0, v0 = 0, v1 = 0;
		boolean t = uv != null;
		if (t) { u0 = uv[0]; u1 = uv[1]; v0 = uv[2]; v1 = uv[3]; }
		float vb = t ? v0 + (v1 - v0) * Math.min(1f, y - bottom) : 0;
		if (dv[0] == 1) {
			float xx = x + cell;
			vert(bb, t, xx, y, z, u0, v0, color); vert(bb, t, xx, y, z + cell, u1, v0, color);
			vert(bb, t, xx, bottom, z + cell, u1, vb, color); vert(bb, t, xx, bottom, z, u0, vb, color);
		} else if (dv[0] == -1) {
			vert(bb, t, x, y, z + cell, u0, v0, color); vert(bb, t, x, y, z, u1, v0, color);
			vert(bb, t, x, bottom, z, u1, vb, color); vert(bb, t, x, bottom, z + cell, u0, vb, color);
		} else if (dv[1] == 1) {
			float zz = z + cell;
			vert(bb, t, x + cell, y, zz, u0, v0, color); vert(bb, t, x, y, zz, u1, v0, color);
			vert(bb, t, x, bottom, zz, u1, vb, color); vert(bb, t, x + cell, bottom, zz, u0, vb, color);
		} else {
			vert(bb, t, x, y, z, u0, v0, color); vert(bb, t, x + cell, y, z, u1, v0, color);
			vert(bb, t, x + cell, bottom, z, u1, vb, color); vert(bb, t, x, bottom, z, u0, vb, color);
		}
	}

	private static void vert(BufferBuilder bb, boolean tex, float x, float y, float z, float u, float v, int color) {
		if (tex) bb.addVertex(x, y, z).setUv(u, v).setColor(color);
		else bb.addVertex(x, y, z).setColor(color);
	}

	private static float[] uv(TextureAtlasSprite s) {
		return new float[]{s.getU0(), s.getU1(), s.getV0(), s.getV1()};
	}

	private static void buildFull(Region r, int version, boolean useHole, int hx, int hz, int hole) {
		short[] src = r.h, ss = r.st, ts = r.tn;
		if (src == null) { BUILDING.remove(key(r.rx, r.rz)); return; }
		BlockState[] pal = r.pal;
		int[] tints = r.tints;
		int n = RC;
		short[] hs = src.clone();
		int x0b = r.rx * RC, z0b = r.rz * RC;
		if (useHole) for (int gz = 0; gz < n; gz++) for (int gx = 0; gx < n; gx++) {
			int cx = (x0b + gx) >> 4, cz = (z0b + gz) >> 4;
			if (Math.abs(cx - hx) <= hole && Math.abs(cz - hz) <= hole) hs[gz * n + gx] = EMPTY;
		}
		ByteBufferBuilder tbytes = new ByteBufferBuilder(1 << 20);
		BufferBuilder tb = new BufferBuilder(tbytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		ByteBufferBuilder cbytes = new ByteBufferBuilder(1 << 16);
		BufferBuilder cb = new BufferBuilder(cbytes, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		int tq = 0, cq = 0;
		float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
		Map<TextureAtlasSprite, float[]> uvs = new HashMap<>();
		for (int gz = 0; gz < n; gz++) for (int gx = 0; gx < n; gx++) {
			int i = gz * n + gx;
			int h = hs[i];
			if (h == EMPTY) continue;
			int s = ss[i] & 0xFFFF, t = ts[i] & 0xFFFF;
			BlockState st = s < pal.length ? pal[s] : null;
			if (st == null) continue;
			Info info = info(st);
			int tint = t < tints.length ? tints[t] : 0xFFFFFF;
			int topColor = info.tintIndex >= 0 || st.is(Blocks.WATER) ? 0xFF000000 | tint : 0xFFFFFFFF;
			float y = h - SINK;
			float x = gx, z = gz;
			float[] tu = uvs.computeIfAbsent(info.top, Lod::uv);
			if (info.topClear) {
				int c = argb(multiply(info.color, tint), 0.85f);
				float yy = y - 0.04f;
				cb.addVertex(x, yy, z).setColor(c); cb.addVertex(x, yy, z + 1).setColor(c);
				cb.addVertex(x + 1, yy, z + 1).setColor(c); cb.addVertex(x + 1, yy, z).setColor(c);
				cq++;
			}
			tb.addVertex(x, y, z).setUv(tu[0], tu[2]).setColor(topColor);
			tb.addVertex(x, y, z + 1).setUv(tu[0], tu[3]).setColor(topColor);
			tb.addVertex(x + 1, y, z + 1).setUv(tu[1], tu[3]).setColor(topColor);
			tb.addVertex(x + 1, y, z).setUv(tu[1], tu[2]).setColor(topColor);
			tq++;
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
			int sideTint = info.sideTinted ? tint : 0xFFFFFF;
			for (int[] dv : DIRS) {
				int nx = gx + dv[0], nz = gz + dv[1];
				boolean inside = nx >= 0 && nz >= 0 && nx < n && nz < n;
				int nh = inside ? hs[nz * n + nx] : neighborHeight(r, 0, nx, nz, n);
				int bottom;
				if (nh == EMPTY) {
					if (inside && src[nz * n + nx] == EMPTY) bottom = h - 4;
					else if (inside) bottom = h - 2;
					else continue;
				} else if (nh < h) bottom = nh;
				else continue;
				bottom = Math.max(bottom, h - 32);
				float shade = dv[0] != 0 ? 0.6f : 0.8f;
				for (int yb = h - 1; yb >= bottom; yb--) {
					boolean first = yb == h - 1;
					TextureAtlasSprite sp = first ? info.side : info.under;
					int c = argb(first ? sideTint : 0xFFFFFF, shade);
					if (st.is(Blocks.WATER)) c = argb(tint, shade);
					side(tb, uvs.computeIfAbsent(sp, Lod::uv), x, z, 1f, dv, yb + 1 - SINK, yb - SINK, c);
					tq++;
				}
				minY = Math.min(minY, bottom);
			}
		}
		Part tex = finish(tb, tbytes, tq);
		Part col = finish(cb, cbytes, cq);
		READY.add(new Built(key(r.rx, r.rz), col, tex, 0, version, useHole, hx, hz, hole, minY == Float.MAX_VALUE ? 0 : minY, maxY == -Float.MAX_VALUE ? 0 : maxY));
	}

	private static int drawn, renders;
	private static boolean warned;

	private static void fail(String where, Throwable t) {
		if (warned) return;
		warned = true;
		org.slf4j.LoggerFactory.getLogger("surge").warn("Distant terrain " + where + " failed", t);
	}

	public static String stats() {
		int full = 0;
		for (Region r : REGIONS.values()) if (r.h != null) full++;
		return "regions=" + REGIONS.size() + " full=" + full + " meshes=" + MESHES.size() + " drawn=" + drawn + " renders=" + renders + " sampled=" + SAMPLED.size();
	}
}
