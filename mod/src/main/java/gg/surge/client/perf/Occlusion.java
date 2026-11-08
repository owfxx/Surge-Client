package gg.surge.client.perf;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

public final class Occlusion {
	private static final Long2LongOpenHashMap ENTITIES = new Long2LongOpenHashMap();
	private static final Long2LongOpenHashMap BLOCKS = new Long2LongOpenHashMap();
	private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();
	private static long frame = -1, lastClean;
	private static int budget;
	private static ClientLevel lastLevel;

	private Occlusion() {}

	private static void newFrame(Minecraft mc) {
		long f = mc.getFrameTimeNs();
		if (f != frame) {
			frame = f;
			budget = 96;
		}
		if (mc.level != lastLevel) {
			lastLevel = mc.level;
			ENTITIES.clear();
			BLOCKS.clear();
		}
	}

	public static boolean hidden(Entity e, double camX, double camY, double camZ) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || e instanceof net.minecraft.world.entity.player.Player || e.isCurrentlyGlowing() || e.hasCustomName()) return false;
		double dx = e.getX() - camX, dy = e.getY() - camY, dz = e.getZ() - camZ;
		if (dx * dx + dy * dy + dz * dz < 100) return false;
		newFrame(mc);
		long now = mc.level.getGameTime();
		long key = e.getId();
		long c = ENTITIES.getOrDefault(key, Long.MIN_VALUE);
		int wait = 4 + (int) (key & 3);
		if (c != Long.MIN_VALUE && now - (c >> 1) < wait) return (c & 1) == 1;
		if (budget <= 0) return c != Long.MIN_VALUE && (c & 1) == 1;
		budget--;
		AABB b = e.getBoundingBox().inflate(0.15);
		boolean blocked = boxHidden(mc.level, camX, camY, camZ, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
		ENTITIES.put(key, (now << 1) | (blocked ? 1 : 0));
		clean(now);
		return blocked;
	}

	public static boolean hidden(BlockPos pos, double camX, double camY, double camZ) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return false;
		double dx = pos.getX() + 0.5 - camX, dy = pos.getY() + 0.5 - camY, dz = pos.getZ() + 0.5 - camZ;
		if (dx * dx + dy * dy + dz * dz < 64) return false;
		newFrame(mc);
		long now = mc.level.getGameTime();
		long key = pos.asLong();
		long c = BLOCKS.getOrDefault(key, Long.MIN_VALUE);
		int wait = 6 + (int) (key & 3);
		if (c != Long.MIN_VALUE && now - (c >> 1) < wait) return (c & 1) == 1;
		if (budget <= 0) return c != Long.MIN_VALUE && (c & 1) == 1;
		budget--;
		boolean blocked = boxHidden(mc.level, camX, camY, camZ, pos.getX() - 0.05, pos.getY() - 0.05, pos.getZ() - 0.05, pos.getX() + 1.05, pos.getY() + 1.05, pos.getZ() + 1.05);
		BLOCKS.put(key, (now << 1) | (blocked ? 1 : 0));
		clean(now);
		return blocked;
	}

	private static void clean(long now) {
		if (now - lastClean < 200) return;
		lastClean = now;
		ENTITIES.long2LongEntrySet().removeIf(en -> now - (en.getLongValue() >> 1) > 100);
		BLOCKS.long2LongEntrySet().removeIf(en -> now - (en.getLongValue() >> 1) > 100);
	}

	private static boolean boxHidden(ClientLevel level, double cx, double cy, double cz, double x0, double y0, double z0, double x1, double y1, double z1) {
		double mx = (x0 + x1) / 2, my = (y0 + y1) / 2, mz = (z0 + z1) / 2;
		if (clear(level, cx, cy, cz, mx, my, mz)) return false;
		double[] xs = {x0, x1}, ys = {y0, y1}, zs = {z0, z1};
		for (double x : xs) for (double y : ys) for (double z : zs) if (clear(level, cx, cy, cz, x, y, z)) return false;
		return true;
	}

	private static boolean clear(ClientLevel level, double sx, double sy, double sz, double ex, double ey, double ez) {
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
			if (level.getBlockState(POS.set(x, y, z)).isSolidRender()) return false;
		}
		return true;
	}
}
