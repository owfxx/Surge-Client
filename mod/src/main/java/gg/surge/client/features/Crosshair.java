package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Custom crosshair: style, color, length, gap, thickness, outline and a center dot. */
public final class Crosshair {
	private static final int OUTLINE = 0xC0000000;

	private Crosshair() {}

	private static void bar(GuiGraphics g, int x1, int y1, int x2, int y2, int col, boolean outline) {
		if (outline) g.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, OUTLINE);
		g.fill(x1, y1, x2, y2, col);
	}

	public static void render(GuiGraphics g, Minecraft mc) {
		if (mc.options.hideGui) return;
		if (!mc.options.getCameraType().isFirstPerson() && !Mods.CROSS_THIRD.on()) return;
		int cx = g.guiWidth() / 2, cy = g.guiHeight() / 2;
		int col = Mods.CROSS_COLOR.rgb(0);
		int len = Mods.CROSS_SIZE.asInt(), gap = Mods.CROSS_GAP.asInt(), t = Mods.CROSS_THICK.asInt();
		boolean ol = Mods.CROSS_OUTLINE.on();
		int h0 = -(t / 2), h1 = h0 + t; // thickness span around the center pixel
		switch (Mods.CROSS_STYLE.get()) {
			case "Dot" -> bar(g, cx + h0 - 1, cy + h0 - 1, cx + h1 + 1, cy + h1 + 1, col, ol);
			case "Circle" -> {
				int r = Math.max(3, gap + 2);
				for (int a = 0; a < 360; a += 6) {
					int px = cx + (int) Math.round(Math.cos(Math.toRadians(a)) * r);
					int py = cy + (int) Math.round(Math.sin(Math.toRadians(a)) * r);
					if (ol) g.fill(px - 1, py - 1, px + 2, py + 2, OUTLINE);
				}
				for (int a = 0; a < 360; a += 6) {
					int px = cx + (int) Math.round(Math.cos(Math.toRadians(a)) * r);
					int py = cy + (int) Math.round(Math.sin(Math.toRadians(a)) * r);
					g.fill(px, py, px + 1, py + 1, col);
				}
			}
			case "Cross" -> {
				// joined plus: gap ignored
				if (ol) {
					g.fill(cx - len - 1, cy + h0 - 1, cx + len + 2, cy + h1 + 1, OUTLINE);
					g.fill(cx + h0 - 1, cy - len - 1, cx + h1 + 1, cy + len + 2, OUTLINE);
				}
				g.fill(cx - len, cy + h0, cx + len + 1, cy + h1, col);
				g.fill(cx + h0, cy - len, cx + h1, cy + len + 1, col);
			}
			default -> { // Gap and T
				int g1 = gap + 1;
				bar(g, cx - g1 - len + 1, cy + h0, cx - g1 + 1, cy + h1, col, ol);      // left
				bar(g, cx + g1, cy + h0, cx + g1 + len, cy + h1, col, ol);              // right
				bar(g, cx + h0, cy + g1, cx + h1, cy + g1 + len, col, ol);              // down
				if (!Mods.CROSS_STYLE.is("T")) bar(g, cx + h0, cy - g1 - len + 1, cx + h1, cy - g1 + 1, col, ol); // up
			}
		}
		if (Mods.CROSS_DOT.on() && !Mods.CROSS_STYLE.is("Dot") && !Mods.CROSS_STYLE.is("Cross")) bar(g, cx + h0, cy + h0, cx + h1, cy + h1, col, ol);
	}
}
