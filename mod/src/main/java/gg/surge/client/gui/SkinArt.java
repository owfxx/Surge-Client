package gg.surge.client.gui;

import gg.surge.client.cosmetics.MenuAvatar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

public final class SkinArt {
	private static float rot = -25f, target = -25f;
	private static boolean dragging, wasDown;
	private static double lastX;
	private static long idleSince, frameAt = System.nanoTime();

	private SkinArt() {}

	public static void turn(float deg) {
		target += deg;
		idleSince = System.currentTimeMillis();
	}

	public static void drag(double dx) {
		rot += (float) dx * 2.2f;
		target = rot;
		idleSince = System.currentTimeMillis();
	}

	public static int width(int scale) {
		return 20 * scale;
	}

	public static int height(int scale) {
		return 36 * scale;
	}

	public static boolean draw(GuiGraphics g, int cx, int top, int scale, int mx, int my, float delta) {
		Minecraft mc = Minecraft.getInstance();
		int w = width(scale), h = height(scale);
		int x = cx - w / 2;
		boolean down = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean over = mx >= x - 10 && mx < x + w + 10 && my >= top && my < top + h;
		if (down && !wasDown && over) { dragging = true; lastX = mx; }
		if (!down) dragging = false;
		wasDown = down;
		long nowN = System.nanoTime();
		float dt = Math.min(0.1f, (nowN - frameAt) / 1e9f);
		frameAt = nowN;
		long now = System.currentTimeMillis();
		if (dragging) {
			if (mx != lastX) drag(mx - lastX);
			lastX = mx;
		} else {
			if (now - idleSince > 5000) target += dt * 20f;
			rot += (target - rot) * (1f - (float) Math.exp(-dt * 12f));
		}
		g.fill(cx - 5 * scale, top + h - scale, cx + 5 * scale, top + h, 0x30000000);
		MenuAvatar.render(g, x - 10 * scale, top, x + w + 10 * scale, top + h, h / 2.1f, rot, MenuAvatar.ownLook());
		return true;
	}

	public static boolean dragging() {
		return dragging;
	}
}
