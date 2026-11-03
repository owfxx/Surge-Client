package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public final class Freelook {
	private static boolean active;
	private static float yaw, pitch;
	private static CameraType previous;
	private static boolean keyWasDown;

	private Freelook() {}

	public static boolean active() { return active; }

	public static float yaw() { return yaw; }

	public static float pitch() { return pitch; }

	public static void update(Minecraft mc, boolean keyDown) {
		boolean allowed = Mods.FREELOOK.enabled() && mc.player != null && mc.screen == null;
		boolean pressed = keyDown && !keyWasDown;
		keyWasDown = keyDown;
		if (!allowed) { if (active && mc.player == null) stop(mc); return; }
		if (Mods.FREELOOK_MODE.is("Toggle")) {
			if (pressed) { if (active) stop(mc); else start(mc); }
		} else {
			if (keyDown && !active) start(mc);
			else if (!keyDown && active) stop(mc);
		}
	}

	private static void start(Minecraft mc) {
		active = true;
		yaw = mc.player.getYRot();
		pitch = mc.player.getXRot();
		previous = mc.options.getCameraType();
		mc.options.setCameraType(Mods.FREELOOK_VIEW.is("Front") ? CameraType.THIRD_PERSON_FRONT : CameraType.THIRD_PERSON_BACK);
	}

	public static void stop(Minecraft mc) {
		if (!active) return;
		active = false;
		if (previous != null) mc.options.setCameraType(previous);
	}

	public static void turn(double dx, double dy) {
		yaw += (float) dx * 0.15f;
		float dp = (float) dy * 0.15f * (Mods.FREELOOK_INVERT.on() ? -1 : 1);
		pitch = Math.max(-90f, Math.min(90f, pitch + dp));
	}

	public static float cameraYaw() {
		return Mods.FREELOOK_VIEW.is("Front") ? yaw + 180f : yaw;
	}

	public static float cameraPitch() {
		return Mods.FREELOOK_VIEW.is("Front") ? -pitch : pitch;
	}
}
