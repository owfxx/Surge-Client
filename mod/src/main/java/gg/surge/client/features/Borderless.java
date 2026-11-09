package gg.surge.client.features;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class Borderless {
	private Borderless() {}

	public static boolean bypass;
	private static boolean active;
	private static boolean started;
	private static int px = 100, py = 100, pw = 1280, ph = 720;

	public static boolean wanted() {
		return Mods.FULLSCREEN_MODE.is("Borderless");
	}

	public static boolean active() {
		return active;
	}

	public static void toggle(Window w) {
		if (active) leave(w);
		else enter(w);
		Mods.BORDERLESS_ACTIVE.set(active);
	}

	public static void tick(Minecraft mc) {
		Window w = mc.getWindow();
		if (!started) {
			started = true;
			if (wanted() && Mods.BORDERLESS_ACTIVE.on() && !w.isFullscreen()) enter(w);
		}
		if (wanted() && w.isFullscreen()) {
			bypass = true;
			try { w.toggleFullScreen(); } finally { bypass = false; }
			enter(w);
			Mods.BORDERLESS_ACTIVE.set(true);
		}
		if (!wanted() && active) {
			leave(w);
			Mods.BORDERLESS_ACTIVE.set(false);
		}
	}

	private static void enter(Window w) {
		long h = w.handle();
		Monitor m = w.findBestMonitor();
		if (m == null) return;
		VideoMode mode = m.getCurrentMode();
		if (!active) {
			int[] x = new int[1], y = new int[1], ww = new int[1], hh = new int[1];
			GLFW.glfwGetWindowPos(h, x, y);
			GLFW.glfwGetWindowSize(h, ww, hh);
			if (ww[0] > 0 && hh[0] > 0) { px = x[0]; py = y[0]; pw = ww[0]; ph = hh[0]; }
		}
		GLFW.glfwSetWindowAttrib(h, GLFW.GLFW_DECORATED, GLFW.GLFW_FALSE);
		GLFW.glfwSetWindowMonitor(h, 0L, m.getX(), m.getY(), mode.getWidth(), mode.getHeight(), GLFW.GLFW_DONT_CARE);
		active = true;
	}

	private static void leave(Window w) {
		long h = w.handle();
		GLFW.glfwSetWindowAttrib(h, GLFW.GLFW_DECORATED, GLFW.GLFW_TRUE);
		GLFW.glfwSetWindowMonitor(h, 0L, px, py, pw, ph, GLFW.GLFW_DONT_CARE);
		active = false;
	}
}
