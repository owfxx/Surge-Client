package gg.surge.legacy.feature;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.lwjgl.input.Mouse;

import java.util.ArrayDeque;
import java.util.Deque;

public final class Stats {
	private static final Deque<Long> LEFT = new ArrayDeque<Long>(), RIGHT = new ArrayDeque<Long>();
	private static boolean l, r;
	private static String reach = "0.00";
	private static long reachAt;
	private static int combo, lastHurt;
	private static long comboAt;

	private Stats() {}

	public static void frame(MinecraftClient mc) {
		long now = System.currentTimeMillis();
		boolean nl = Mouse.isButtonDown(0), nr = Mouse.isButtonDown(1);
		if (mc.currentScreen == null) {
			if (nl && !l) LEFT.addLast(now);
			if (nr && !r) RIGHT.addLast(now);
		}
		l = nl;
		r = nr;
		while (!LEFT.isEmpty() && now - LEFT.peekFirst() > 1000) LEFT.removeFirst();
		while (!RIGHT.isEmpty() && now - RIGHT.peekFirst() > 1000) RIGHT.removeFirst();
	}

	public static void tick(MinecraftClient mc) {
		if (mc.player == null) return;
		int h = mc.player.hurtTime;
		if (h > 0 && lastHurt == 0) combo = 0;
		lastHurt = h;
		if (System.currentTimeMillis() - comboAt > 3000) combo = 0;
	}

	public static void attacked(MinecraftClient mc, Entity target) {
		if (mc.player == null || target == null) return;
		double ex = mc.player.x, ey = mc.player.y + mc.player.getEyeHeight(), ez = mc.player.z;
		net.minecraft.util.math.Box b = target.getBoundingBox();
		double cx = Math.max(b.minX, Math.min(ex, b.maxX)), cy = Math.max(b.minY, Math.min(ey, b.maxY)), cz = Math.max(b.minZ, Math.min(ez, b.maxZ));
		double d = Math.sqrt((cx - ex) * (cx - ex) + (cy - ey) * (cy - ey) + (cz - ez) * (cz - ez));
		reach = String.format("%.2f", d);
		reachAt = System.currentTimeMillis();
		combo++;
		comboAt = reachAt;
	}

	public static int leftCps() { return LEFT.size(); }

	public static int rightCps() { return RIGHT.size(); }

	public static String reach() { return System.currentTimeMillis() - reachAt > 3000 ? "0.00" : reach; }

	public static int combo() { return combo; }
}
