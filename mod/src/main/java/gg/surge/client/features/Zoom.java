package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;

/** Hold-to-zoom (C by default). Smoothly eases in and out; scroll while zoomed to change the zoom level. */
public final class Zoom {
	private static boolean active;
	private static float current = 1f;
	private static long lastNanos = System.nanoTime();
	private static boolean changedSmoothCamera;
	private static float scrollLevel = -1;
	private static float lastFov = 70f;

	private Zoom() {}

	public static boolean active() { return active; }

	public static void setActive(Minecraft mc, boolean on) {
		if (on && !Mods.ZOOM.enabled()) on = false;
		if (on == active) return;
		active = on;
		if (on) {
			scrollLevel = Mods.ZOOM_LEVEL.get();
			if (Mods.ZOOM_SMOOTH_CAM.on() && !mc.options.smoothCamera) { mc.options.smoothCamera = true; changedSmoothCamera = true; }
		} else if (changedSmoothCamera) {
			mc.options.smoothCamera = false;
			changedSmoothCamera = false;
		}
	}

	/** Mouse wheel while zoomed: zoom further in or out (between 1.5x and 50x). Returns true if used. */
	public static boolean scroll(int steps) {
		if (!active || !Mods.ZOOM_SCROLL.on()) return false;
		scrollLevel = Math.max(1.5f, Math.min(50f, scrollLevel * (float) Math.pow(1.25, steps)));
		return true;
	}

	/** How much to divide the FOV by this frame. */
	public static float divisor() {
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - lastNanos) / 1e9f);
		lastNanos = now;
		float target = active ? (scrollLevel > 0 ? scrollLevel : Mods.ZOOM_LEVEL.get()) : 1f;
		if (!Mods.ZOOM_ANIMATE.on()) current = target;
		else current += (target - current) * (1f - (float) Math.exp(-dt * 14f));
		if (Math.abs(current - target) < 0.002f) current = target;
		return current;
	}

	/** The world FOV used for the last frame (after zoom), for projecting waypoints. */
	public static float lastFov() { return lastFov; }

	public static void recordFov(float fov) { lastFov = fov; }
}
