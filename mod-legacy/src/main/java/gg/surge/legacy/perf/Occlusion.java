package gg.surge.legacy.perf;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class Occlusion {
	private static final Map<Long, Long> ENTITIES = new HashMap<Long, Long>();
	private static final Map<Long, Long> BLOCKS = new HashMap<Long, Long>();
	private static long frame = -1, lastClean;
	private static int budget;
	private static World lastWorld;

	private Occlusion() {}

	private static void newFrame(MinecraftClient mc) {
		long f = Perf.frame();
		if (f != frame) {
			frame = f;
			budget = 96;
		}
		if (mc.world != lastWorld) {
			lastWorld = mc.world;
			ENTITIES.clear();
			BLOCKS.clear();
		}
	}

	public static boolean hidden(Entity e, double camX, double camY, double camZ) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null || e instanceof PlayerEntity || e.hasCustomName()) return false;
		double dx = e.x - camX, dy = e.y - camY, dz = e.z - camZ;
		if (dx * dx + dy * dy + dz * dz < 100) return false;
		newFrame(mc);
		long now = mc.world.getLastUpdateTime();
		long key = e.getEntityId();
		Long c = ENTITIES.get(key);
		int wait = 4 + (int) (key & 3);
		if (c != null && now - (c >> 1) < wait) return (c & 1) == 1;
		if (budget <= 0) return c != null && (c & 1) == 1;
		budget--;
		Box b = e.getBoundingBox();
		boolean blocked = boxHidden(mc.world, camX, camY, camZ, b.minX - 0.15, b.minY - 0.15, b.minZ - 0.15, b.maxX + 0.15, b.maxY + 0.15, b.maxZ + 0.15);
		ENTITIES.put(key, (now << 1) | (blocked ? 1 : 0));
		clean(now);
		return blocked;
	}

	public static boolean hidden(BlockPos pos, double camX, double camY, double camZ) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null) return false;
		double dx = pos.getX() + 0.5 - camX, dy = pos.getY() + 0.5 - camY, dz = pos.getZ() + 0.5 - camZ;
		if (dx * dx + dy * dy + dz * dz < 64) return false;
		newFrame(mc);
		long now = mc.world.getLastUpdateTime();
		long key = pos.asLong();
		Long c = BLOCKS.get(key);
		int wait = 6 + (int) (key & 3);
		if (c != null && now - (c >> 1) < wait) return (c & 1) == 1;
		if (budget <= 0) return c != null && (c & 1) == 1;
		budget--;
		boolean blocked = boxHidden(mc.world, camX, camY, camZ, pos.getX() - 0.05, pos.getY() - 0.05, pos.getZ() - 0.05, pos.getX() + 1.05, pos.getY() + 1.05, pos.getZ() + 1.05);
		BLOCKS.put(key, (now << 1) | (blocked ? 1 : 0));
		clean(now);
		return blocked;
	}

	private static void clean(long now) {
		if (now - lastClean < 200) return;
		lastClean = now;
		for (Map<Long, Long> m : new Map[]{ENTITIES, BLOCKS}) {
			for (Iterator<Map.Entry<Long, Long>> it = m.entrySet().iterator(); it.hasNext(); ) if (now - (it.next().getValue() >> 1) > 100) it.remove();
		}
	}

	private static boolean boxHidden(World w, double cx, double cy, double cz, double x0, double y0, double z0, double x1, double y1, double z1) {
		if (clear(w, cx, cy, cz, (x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2)) return false;
		double[] xs = {x0, x1}, ys = {y0, y1}, zs = {z0, z1};
		for (double x : xs) for (double y : ys) for (double z : zs) if (clear(w, cx, cy, cz, x, y, z)) return false;
		return true;
	}

	private static boolean solid(World w, int x, int y, int z) {
		Block b = w.getBlockState(new BlockPos(x, y, z)).getBlock();
		return b.isFullCube() && !b.hasTransparency() && b.isNormalBlock();
	}

	private static boolean clear(World w, double sx, double sy, double sz, double ex, double ey, double ez) {
		double dx = ex - sx, dy = ey - sy, dz = ez - sz;
		double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1e-6) return true;
		dx /= len; dy /= len; dz /= len;
		int x = (int) Math.floor(sx), y = (int) Math.floor(sy), z = (int) Math.floor(sz);
		int tx = (int) Math.floor(ex), ty = (int) Math.floor(ey), tz = (int) Math.floor(ez);
		int stepX = dx > 0 ? 1 : -1, stepY = dy > 0 ? 1 : -1, stepZ = dz > 0 ? 1 : -1;
		double tMaxX = dx != 0 ? ((dx > 0 ? x + 1 - sx : sx - x) / Math.abs(dx)) : Double.POSITIVE_INFINITY;
		double tMaxY = dy != 0 ? ((dy > 0 ? y + 1 - sy : sy - y) / Math.abs(dy)) : Double.POSITIVE_INFINITY;
		double tMaxZ = dz != 0 ? ((dz > 0 ? z + 1 - sz : sz - z) / Math.abs(dz)) : Double.POSITIVE_INFINITY;
		double tDX = dx != 0 ? 1 / Math.abs(dx) : Double.POSITIVE_INFINITY, tDY = dy != 0 ? 1 / Math.abs(dy) : Double.POSITIVE_INFINITY, tDZ = dz != 0 ? 1 / Math.abs(dz) : Double.POSITIVE_INFINITY;
		for (int i = 0; i < 256; i++) {
			if (x == tx && y == ty && z == tz) return true;
			if (tMaxX < tMaxY && tMaxX < tMaxZ) { x += stepX; tMaxX += tDX; }
			else if (tMaxY < tMaxZ) { y += stepY; tMaxY += tDY; }
			else { z += stepZ; tMaxZ += tDZ; }
			if (x == tx && y == ty && z == tz) return true;
			if (solid(w, x, y, z)) return false;
		}
		return true;
	}
}
