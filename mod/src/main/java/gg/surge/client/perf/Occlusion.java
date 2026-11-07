package gg.surge.client.perf;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class Occlusion {
	private static final Map<Integer, long[]> CACHE = new HashMap<>();
	private static long lastClean;

	private Occlusion() {}

	public static boolean hidden(Entity e, double camX, double camY, double camZ) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null || e instanceof net.minecraft.world.entity.player.Player || e.isCurrentlyGlowing()) return false;
		double dx = e.getX() - camX, dy = e.getY() - camY, dz = e.getZ() - camZ;
		if (dx * dx + dy * dy + dz * dz < 144) return false;
		long now = mc.level.getGameTime();
		long[] c = CACHE.get(e.getId());
		if (c != null && now - c[0] < 6 + (e.getId() & 3)) return c[1] == 1;
		Vec3 cam = new Vec3(camX, camY, camZ);
		AABB box = e.getBoundingBox();
		Vec3[] pts = {box.getCenter(), new Vec3(box.getCenter().x, box.maxY, box.getCenter().z), new Vec3(box.minX, box.minY + 0.1, box.minZ), new Vec3(box.maxX, box.minY + 0.1, box.maxZ)};
		boolean blocked = true;
		for (Vec3 p : pts) {
			HitResult r = mc.level.clip(new ClipContext(cam, p, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
			if (r.getType() == HitResult.Type.MISS) { blocked = false; break; }
		}
		CACHE.put(e.getId(), new long[]{now, blocked ? 1 : 0});
		if (now - lastClean > 200) {
			lastClean = now;
			CACHE.entrySet().removeIf(en -> now - en.getValue()[0] > 100);
		}
		return blocked;
	}
}
