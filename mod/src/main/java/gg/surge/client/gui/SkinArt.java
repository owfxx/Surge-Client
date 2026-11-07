package gg.surge.client.gui;

import gg.surge.client.mixin.PlayerSkinWidgetAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
import org.lwjgl.glfw.GLFW;

import java.util.function.Supplier;

public final class SkinArt {
	private static PlayerSkinWidget widget;
	private static int ww, wh;
	private static Supplier<PlayerSkin> lookup;
	private static float rot = -25f, target = -25f;
	private static boolean dragging, wasDown;
	private static double lastX;
	private static long idleSince;

	private SkinArt() {}

	private static Supplier<PlayerSkin> lookup(Minecraft mc) {
		if (lookup == null) {
			try {
				lookup = mc.getSkinManager().createLookup(mc.getGameProfile(), false);
			} catch (Throwable e) {
				PlayerSkin d = DefaultPlayerSkin.getDefaultSkin();
				lookup = () -> d;
			}
		}
		return lookup;
	}

	public static void turn(float deg) {
		target += deg;
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
		if (widget == null || ww != w || wh != h) {
			try {
				widget = new PlayerSkinWidget(w, h, mc.getEntityModels(), lookup(mc));
			} catch (Throwable e) {
				return false;
			}
			ww = w;
			wh = h;
		}
		int x = cx - w / 2;
		widget.setX(x);
		widget.setY(top);
		boolean down = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean over = mx >= x && mx < x + w && my >= top && my < top + h;
		if (down && !wasDown && over) { dragging = true; lastX = mx; }
		if (!down) dragging = false;
		wasDown = down;
		long now = System.currentTimeMillis();
		if (dragging) {
			float d = (float) (mx - lastX) * 2.5f;
			lastX = mx;
			rot += d;
			target = rot;
			idleSince = now;
		} else {
			if (now - idleSince > 4000) target += delta * 0.6f;
			rot += (target - rot) * Math.min(1f, 0.25f * Math.max(1f, delta));
		}
		try {
			PlayerSkinWidgetAccessor a = (PlayerSkinWidgetAccessor) widget;
			a.surge$setRotY(rot);
			a.surge$setRotX(-5f);
		} catch (Throwable ignored) { }
		g.fill(cx - 5 * scale, top + h - scale, cx + 5 * scale, top + h, 0x30000000);
		widget.render(g, mx, my, delta);
		return true;
	}

	public static boolean dragging() {
		return dragging;
	}
}
