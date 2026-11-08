package gg.surge.client.lod;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public final class LodStore {
	public static final int REGION_CHUNKS = 32, CELLS_PER_CHUNK = 4, CELL = 16 / CELLS_PER_CHUNK, SIDE = REGION_CHUNKS * CELLS_PER_CHUNK;
	public static final short EMPTY = Short.MIN_VALUE;

	public static final class Region {
		public final int rx, rz;
		public final short[] height = new short[SIDE * SIDE];
		public final int[] color = new int[SIDE * SIDE];
		public volatile int version;
		volatile boolean dirty;
		volatile long touched = System.currentTimeMillis();

		Region(int rx, int rz) {
			this.rx = rx;
			this.rz = rz;
			java.util.Arrays.fill(height, EMPTY);
		}
	}

	static final Map<Long, Region> REGIONS = new ConcurrentHashMap<>();
	private static final Map<Long, Long> SAMPLED = new HashMap<>();
	private static final Map<Long, Boolean> LOADING = new ConcurrentHashMap<>();
	private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "Surge LOD files"); t.setDaemon(true); t.setPriority(Thread.MIN_PRIORITY); return t; });
	private static String worldKey = "";
	private static Object lastLevel;
	private static long lastSave;
	private static int spiral;

	private LodStore() {}

	static long key(int rx, int rz) {
		return ((long) rx << 32) ^ (rz & 0xFFFFFFFFL);
	}

	private static String sanitize(String s) {
		return s.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9._-]", "_");
	}

	private static String keyFor(Minecraft mc, ClientLevel level) {
		String world;
		if (mc.getSingleplayerServer() != null) world = "sp_" + mc.getSingleplayerServer().getWorldData().getLevelName();
		else if (mc.getCurrentServer() != null) world = "mp_" + mc.getCurrentServer().ip;
		else world = "unknown";
		return sanitize(world) + "/" + sanitize(level.dimension().identifier().toString());
	}

	private static Path dir() {
		return Path.of(System.getProperty("user.home"), ".surgeclient", "lod", worldKey);
	}

	public static void clear() {
		REGIONS.clear();
		synchronized (SAMPLED) { SAMPLED.clear(); }
		LodRenderer.dropAll();
	}

	public static void deleteFiles() {
		Path d = Path.of(System.getProperty("user.home"), ".surgeclient", "lod");
		IO.execute(() -> {
			try (var walk = Files.walk(d)) {
				walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) { } });
			} catch (IOException ignored) { }
		});
		clear();
	}

	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level != lastLevel) {
			saveAll();
			clear();
			lastLevel = level;
			worldKey = level == null ? "" : keyFor(mc, level);
		}
		if (level == null || mc.player == null || !Mods.LOD.enabled()) return;
		long now = System.currentTimeMillis();
		int pcx = mc.player.chunkPosition().x, pcz = mc.player.chunkPosition().z;
		int rd = mc.options.getEffectiveRenderDistance();
		int budget = Math.max(2, Mods.LOD_SPEED.asInt());
		int area = (2 * rd + 1) * (2 * rd + 1);
		for (int n = 0; n < area && budget > 0; n++) {
			int i = spiral++ % area;
			int dx = i % (2 * rd + 1) - rd, dz = i / (2 * rd + 1) - rd;
			int cx = pcx + dx, cz = pcz + dz;
			long ck = ChunkPos.asLong(cx, cz);
			Long last;
			synchronized (SAMPLED) { last = SAMPLED.get(ck); }
			int age = Math.max(Math.abs(dx), Math.abs(dz)) <= 2 ? 8_000 : 45_000;
			if (last != null && now - last < age) continue;
			LevelChunk chunk = level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
			if (chunk == null) continue;
			sample(level, chunk);
			synchronized (SAMPLED) { SAMPLED.put(ck, now); }
			budget--;
		}
		if (now - lastSave > 30_000) {
			lastSave = now;
			saveAll();
			evict(pcx, pcz);
		}
	}

	private static void sample(ClientLevel level, LevelChunk chunk) {
		ChunkPos cp = chunk.getPos();
		int rx = Math.floorDiv(cp.x, REGION_CHUNKS), rz = Math.floorDiv(cp.z, REGION_CHUNKS);
		Region r = region(rx, rz, true);
		int ox = Math.floorMod(cp.x, REGION_CHUNKS) * CELLS_PER_CHUNK, oz = Math.floorMod(cp.z, REGION_CHUNKS) * CELLS_PER_CHUNK;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		boolean changed = false;
		for (int cz = 0; cz < CELLS_PER_CHUNK; cz++) for (int cx = 0; cx < CELLS_PER_CHUNK; cx++) {
			int best = Integer.MIN_VALUE, rs = 0, gs = 0, bs = 0, n = 0;
			for (int sz = 0; sz < CELL; sz += 2) for (int sx = 0; sx < CELL; sx += 2) {
				int lx = cx * CELL + sx + 1, lz = cz * CELL + sz + 1;
				int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, lx, lz);
				if (top <= chunk.getMinY()) continue;
				pos.set(cp.getMinBlockX() + lx, top, cp.getMinBlockZ() + lz);
				BlockState st = chunk.getBlockState(pos);
				int y = top;
				for (int k = 0; k < 6 && (st.isAir() || st.getBlock() == Blocks.SNOW || st.is(BlockTags.FLOWERS) || st.getBlock() == Blocks.SHORT_GRASS || st.getBlock() == Blocks.TALL_GRASS); k++) {
					y--;
					pos.setY(y);
					st = chunk.getBlockState(pos);
				}
				int col = colorOf(level, st, pos);
				if (col == 0) continue;
				best = Math.max(best, y + 1);
				rs += (col >> 16) & 255;
				gs += (col >> 8) & 255;
				bs += col & 255;
				n++;
			}
			int idx = (oz + cz) * SIDE + ox + cx;
			short h = n == 0 ? EMPTY : (short) best;
			int c = n == 0 ? 0 : ((rs / n) << 16) | ((gs / n) << 8) | (bs / n);
			if (r.height[idx] != h || r.color[idx] != c) {
				r.height[idx] = h;
				r.color[idx] = c;
				changed = true;
			}
		}
		if (changed) {
			r.dirty = true;
			r.version++;
		}
		r.touched = System.currentTimeMillis();
	}

	private static int colorOf(ClientLevel level, BlockState st, BlockPos pos) {
		if (st.isAir()) return 0;
		if (st.getFluidState().is(Fluids.WATER) || st.getFluidState().is(Fluids.FLOWING_WATER)) {
			int w = level.getBiome(pos).value().getWaterColor();
			return mix(w, 0x1E3A6E, 0.35f);
		}
		Biome b = level.getBiome(pos).value();
		if (st.getBlock() == Blocks.GRASS_BLOCK) return mix(b.getGrassColor(pos.getX(), pos.getZ()), 0x000000, 0.12f);
		if (st.is(BlockTags.LEAVES)) {
			if (st.getBlock() == Blocks.SPRUCE_LEAVES) return 0x3B5A3B;
			if (st.getBlock() == Blocks.BIRCH_LEAVES) return 0x6E9A4C;
			if (st.getBlock() == Blocks.CHERRY_LEAVES) return 0xE8A8C8;
			return mix(b.getFoliageColor(), 0x000000, 0.25f);
		}
		int c = st.getMapColor(level, pos).col;
		return c == 0 ? 0x7F7F7F : c;
	}

	private static int mix(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
		return (r << 16) | (g << 8) | bl;
	}

	public static Region region(int rx, int rz, boolean create) {
		long k = key(rx, rz);
		Region r = REGIONS.get(k);
		if (r != null) { r.touched = System.currentTimeMillis(); return r; }
		if (!create) {
			if (!worldKey.isEmpty() && Mods.LOD_SAVE.on() && LOADING.putIfAbsent(k, true) == null) {
				String wk = worldKey;
				IO.execute(() -> {
					try {
						Region loaded = load(wk, rx, rz);
						if (loaded != null && wk.equals(worldKey)) REGIONS.putIfAbsent(k, loaded);
					} finally {
						LOADING.remove(k);
					}
				});
			}
			return null;
		}
		r = new Region(rx, rz);
		Region prev = REGIONS.putIfAbsent(k, r);
		if (prev != null) return prev;
		if (Mods.LOD_SAVE.on() && !worldKey.isEmpty()) {
			Region loaded = load(worldKey, rx, rz);
			if (loaded != null) {
				System.arraycopy(loaded.height, 0, r.height, 0, r.height.length);
				System.arraycopy(loaded.color, 0, r.color, 0, r.color.length);
				r.version++;
			}
		}
		return r;
	}

	private static Path file(String wk, int rx, int rz) {
		return Path.of(System.getProperty("user.home"), ".surgeclient", "lod", wk, "r." + rx + "." + rz + ".lod");
	}

	private static Region load(String wk, int rx, int rz) {
		Path f = file(wk, rx, rz);
		if (!Files.exists(f)) return null;
		try (DataInputStream in = new DataInputStream(new InflaterInputStream(Files.newInputStream(f)))) {
			if (in.readInt() != 0x53524C31) return null;
			Region r = new Region(rx, rz);
			for (int i = 0; i < r.height.length; i++) r.height[i] = in.readShort();
			for (int i = 0; i < r.color.length; i++) r.color[i] = in.readInt();
			r.version = 1;
			return r;
		} catch (IOException e) {
			return null;
		}
	}

	public static void saveAll() {
		if (worldKey.isEmpty() || !Mods.LOD_SAVE.on()) return;
		List<Region> dirty = new ArrayList<>();
		for (Region r : REGIONS.values()) if (r.dirty) { r.dirty = false; dirty.add(r); }
		if (dirty.isEmpty()) return;
		String wk = worldKey;
		List<short[]> hs = new ArrayList<>();
		List<int[]> cs = new ArrayList<>();
		for (Region r : dirty) { hs.add(r.height.clone()); cs.add(r.color.clone()); }
		IO.execute(() -> {
			for (int i = 0; i < dirty.size(); i++) {
				Region r = dirty.get(i);
				Path f = file(wk, r.rx, r.rz);
				try {
					Files.createDirectories(f.getParent());
					Path tmp = f.resolveSibling(f.getFileName() + ".tmp");
					try (DataOutputStream out = new DataOutputStream(new DeflaterOutputStream(Files.newOutputStream(tmp)))) {
						out.writeInt(0x53524C31);
						for (short h : hs.get(i)) out.writeShort(h);
						for (int c : cs.get(i)) out.writeInt(c);
					}
					Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException ignored) { }
			}
		});
	}

	private static void evict(int pcx, int pcz) {
		int limit = Mods.LOD_DISTANCE.asInt() / REGION_CHUNKS + 3;
		int prx = Math.floorDiv(pcx, REGION_CHUNKS), prz = Math.floorDiv(pcz, REGION_CHUNKS);
		REGIONS.entrySet().removeIf(e -> {
			Region r = e.getValue();
			boolean far = Math.max(Math.abs(r.rx - prx), Math.abs(r.rz - prz)) > limit;
			if (far && !r.dirty) { LodRenderer.drop(e.getKey()); return true; }
			return false;
		});
	}

	public static int regionCount() {
		return REGIONS.size();
	}
}
