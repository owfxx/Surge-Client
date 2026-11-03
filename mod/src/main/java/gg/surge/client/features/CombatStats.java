package gg.surge.client.features;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CombatStats {
	private static double lastReach = -1;
	private static long lastHitAt;
	private static int combo;
	private static Entity target;

	private CombatStats() {}

	public static void onAttack(Player player, Entity hit) {
		if (player != Minecraft.getInstance().player) return;
		Vec3 eye = player.getEyePosition();
		AABB box = hit.getBoundingBox();
		double x = Math.max(box.minX, Math.min(eye.x, box.maxX));
		double y = Math.max(box.minY, Math.min(eye.y, box.maxY));
		double z = Math.max(box.minZ, Math.min(eye.z, box.maxZ));
		lastReach = eye.distanceTo(new Vec3(x, y, z));
		long now = System.currentTimeMillis();
		combo = now - lastHitAt < 2000 ? combo + 1 : 1;
		lastHitAt = now;
		target = hit;
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null) { target = null; return; }
		if (mc.player.hurtTime > 0 || System.currentTimeMillis() - lastHitAt > 2000) combo = 0;
	}

	public static String reach() {
		return lastReach < 0 ? "--" : String.format("%.2f", lastReach);
	}

	public static int combo() { return combo; }

	public static Entity target() {
		if (target == null || target.isRemoved() || System.currentTimeMillis() - lastHitAt > 8000) return null;
		return target;
	}
}
