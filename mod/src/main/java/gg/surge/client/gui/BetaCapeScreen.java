package gg.surge.client.gui;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.cosmetics.BetaCapes;
import gg.surge.client.mods.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class BetaCapeScreen extends Screen {
	private static final Identifier SHINE = Identifier.fromNamespaceAndPath("surge", "textures/cosmetic/cape_shine.png");
	private final Screen parent;
	private boolean wasDown = true;
	private boolean editingTop = true;
	private long openedAt;

	public BetaCapeScreen(Screen parent) {
		super(Component.literal("Beta Tester Cape"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		openedAt = System.currentTimeMillis();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, Colors.alpha(0xA0000000, Math.min(1f, (System.currentTimeMillis() - openedAt) / 160f)));
	}

	private static int hex(String s) {
		try { return 0xFF000000 | Integer.parseInt(s, 16); } catch (Exception e) { return 0xFF8F50F2; }
	}

	private void drawCape(GuiGraphics g, SurgeConfig.BetaCape b, int x, int y, int scale) {
		Identifier tex = Identifier.fromNamespaceAndPath("surge", "textures/cosmetic/cape_beta_" + b.design + ".png");
		int top = hex(b.top), bot = hex(b.bottom);
		int slices = 32, w = 10 * scale, sliceH = 16 * scale / slices;
		Ui.shadow(g, x, y, w, sliceH * slices);
		for (int i = 0; i < slices; i++) {
			int col = Colors.lerp(top, bot, (i + 0.5f) / slices);
			g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y + i * sliceH, 8f, 8f + i * 4, w, sliceH, 80, 4, 512, 256, col);
		}
		if (b.shimmer) {
			float phase = (System.currentTimeMillis() % 3500) / 3500f;
			int h = sliceH * slices;
			int bandY = y + Math.round((phase * 1.9f - 0.5f) * h);
			int bandH = h / 3;
			int from = Math.max(y, bandY), to = Math.min(y + h, bandY + bandH);
			if (to > from) {
				g.enableScissor(x, from, x + w, to);
				g.blit(RenderPipelines.GUI_TEXTURED, SHINE, x, bandY, 0f, 0f, w, bandH, 8, 128, 8, 128, 0xB0FFFFFF);
				g.disableScissor();
			}
		}
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		Ui.frame();
		boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasDown;
		wasDown = down;
		Runnable action = null;
		SurgeConfig c = SurgeConfig.get();
		SurgeConfig.BetaCape b = c.betaCape;

		int pw = Math.min(380, this.width - 16), ph = Math.min(250, this.height - 16);
		int px = (this.width - pw) / 2, py = (this.height - ph) / 2;
		Ui.shadow(g, px, py, pw, ph);
		Ui.box(g, px, py, pw, ph, Ui.BG, Ui.EDGE);
		g.drawString(font, Component.literal("Beta Tester Cape").withStyle(ChatFormatting.BOLD), px + 10, py + 10, Ui.TEXT, true);
		g.drawString(font, "Only for beta testers. Make it yours.", px + 10, py + 22, Ui.MUTED, false);
		if (Ui.iconButton(g, mx, my, px + pw - 26, py + 7, 18, Ui.CROSS) && click) action = this::onClose;

		int scale = Math.max(6, Math.min(10, (ph - 70) / 16)) & ~1;
		int capeX = px + 22, capeY = py + 44;
		g.fillGradient(px + 10, py + 38, px + 32 + 10 * scale, py + ph - 10, 0x20FFFFFF, 0x10000000);
		drawCape(g, b, capeX, capeY, scale);

		int ox = capeX + 10 * scale + 24, ow = px + pw - 12 - ox, y = py + 42;
		g.drawString(font, "Design", ox, y, Ui.ACCENT_2, false);
		y += 12;
		int dw = (ow - 3 * 4) / 4;
		for (int i = 0; i < BetaCapes.DESIGNS.size(); i++) {
			String d = BetaCapes.DESIGNS.get(i);
			if (Ui.pill(g, font, mx, my, ox + i * (dw + 4), y, dw, 16, BetaCapes.DESIGN_NAMES.get(i), d.equals(b.design)) && click) action = () -> b.design = d;
		}
		y += 26;

		g.drawString(font, "Colors", ox, y, Ui.ACCENT_2, false);
		y += 12;
		int half = (ow - 4) / 2;
		if (Ui.pill(g, font, mx, my, ox, y, half, 16, "Top", editingTop) && click) action = () -> editingTop = true;
		if (Ui.pill(g, font, mx, my, ox + half + 4, y, half, 16, "Bottom", !editingTop) && click) action = () -> editingTop = false;
		g.fill(ox + 4, y + 5, ox + 10, y + 11, hex(b.top));
		g.fill(ox + half + 8, y + 5, ox + half + 14, y + 11, hex(b.bottom));
		y += 22;
		int cols = 8, sw = Math.min(18, (ow - (cols - 1) * 3) / cols);
		String current = editingTop ? b.top : b.bottom;
		for (int i = 0; i < BetaCapes.PALETTE.size(); i++) {
			String hx = BetaCapes.PALETTE.get(i);
			int sx = ox + (i % cols) * (sw + 3), sy = y + (i / cols) * (sw + 3);
			boolean hov = Ui.hover(mx, my, sx, sy, sw, sw);
			g.fill(sx, sy, sx + sw, sy + sw, hex(hx));
			if (hx.equalsIgnoreCase(current)) Ui.outline(g, sx - 1, sy - 1, sw + 2, sw + 2, 0xFFFFFFFF);
			else if (hov) Ui.outline(g, sx - 1, sy - 1, sw + 2, sw + 2, 0x90FFFFFF);
			if (hov && click) action = () -> { if (editingTop) b.top = hx; else b.bottom = hx; };
		}
		y += 2 * (sw + 3) + 4;
		if (Ui.pill(g, font, mx, my, ox, y, half, 16, "Swap colors", false) && click) action = () -> { String t = b.top; b.top = b.bottom; b.bottom = t; };
		if (Ui.pill(g, font, mx, my, ox + half + 4, y, half, 16, "Same color", false) && click) action = () -> { if (editingTop) b.bottom = b.top; else b.top = b.bottom; };
		y += 26;

		g.drawString(font, "Effects", ox, y, Ui.ACCENT_2, false);
		y += 12;
		if (Ui.pill(g, font, mx, my, ox, y, half, 16, "Shimmer: " + (b.shimmer ? "On" : "Off"), b.shimmer) && click) action = () -> b.shimmer = !b.shimmer;
		if (Ui.pill(g, font, mx, my, ox + half + 4, y, half, 16, "Glow: " + (b.glow ? "On" : "Off"), b.glow) && click) action = () -> b.glow = !b.glow;

		int by = py + ph - 26;
		boolean wearing = "beta".equals(c.cape);
		if (Ui.pill(g, font, mx, my, ox, by, half, 18, wearing ? "Take off" : "Wear", !wearing) && click) action = () -> c.cape = wearing ? "none" : "beta";
		if (Ui.pill(g, font, mx, my, ox + half + 4, by, half, 18, "Done", true) && click) action = this::onClose;

		if (action != null) { Ui.click(); action.run(); SurgeConfig.save(); }
	}

	@Override
	public void onClose() {
		SurgeConfig.save();
		minecraft.setScreen(parent);
	}
}
