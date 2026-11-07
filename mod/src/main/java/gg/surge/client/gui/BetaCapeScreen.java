package gg.surge.client.gui;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.cosmetics.CapeTextures;
import gg.surge.client.mods.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class BetaCapeScreen extends Screen {
	private final Screen parent;
	private final boolean beta;
	private boolean wasDown = true;
	private int editing;
	private long openedAt;
	private EditBox hexBox;

	public BetaCapeScreen(Screen parent) {
		this(parent, true);
	}

	public BetaCapeScreen(Screen parent, boolean beta) {
		super(Component.literal(beta ? "Beta Tester Cape" : "Color Cape"));
		this.parent = parent;
		this.beta = beta;
	}

	@Override
	protected void init() {
		openedAt = System.currentTimeMillis();
		hexBox = new EditBox(font, 0, 0, 44, 10, Component.literal("Hex"));
		hexBox.setBordered(false);
		hexBox.setMaxLength(7);
		hexBox.setValue(current());
		hexBox.setResponder(v -> {
			String c = v.trim().replace("#", "");
			if (c.matches("[0-9a-fA-F]{6}")) set(c.toUpperCase());
		});
		addRenderableWidget(hexBox);
	}

	private String current() {
		SurgeConfig c = SurgeConfig.get();
		if (!beta) return editing == 1 ? c.capeColor2 : c.capeColor;
		return editing == 2 ? c.betaCape.logo : editing == 1 ? c.betaCape.color2 : c.betaCape.color;
	}

	private void set(String hx) {
		SurgeConfig c = SurgeConfig.get();
		if (!beta) { if (editing == 1) c.capeColor2 = hx; else c.capeColor = hx; }
		else if (editing == 2) c.betaCape.logo = hx;
		else if (editing == 1) c.betaCape.color2 = hx;
		else c.betaCape.color = hx;
		SurgeConfig.markDirty();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, Colors.alpha(0xA0000000, Math.min(1f, (System.currentTimeMillis() - openedAt) / 160f)));
	}

	private static int hex(String s) {
		try { return 0xFF000000 | Integer.parseInt(s, 16); } catch (Exception e) { return 0xFF8F50F2; }
	}

	private void drawCape(GuiGraphics g, int x, int y, int scale) {
		Identifier tex = CapeTextures.get(beta ? "beta" : "custom");
		if (tex == null) return;
		int w = 10 * scale, h = 16 * scale;
		Ui.shadow(g, x, y, w, h);
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
		g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 8f, 8f, w, h, 80, 128, 512, 256);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		renderBackground(g, mx, my, delta);
		Ui.frame();
		boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasDown;
		wasDown = down;
		Runnable action = null;
		SurgeConfig c = SurgeConfig.get();
		SurgeConfig.BetaCape b = c.betaCape;

		int pw = Math.min(380, this.width - 16), ph = Math.min(beta ? 250 : 200, this.height - 16);
		int px = (this.width - pw) / 2, py = (this.height - ph) / 2;
		Ui.shadow(g, px, py, pw, ph);
		Ui.box(g, px, py, pw, ph, Ui.BG, Ui.EDGE);
		g.drawString(font, Component.literal(beta ? "Beta Tester Cape" : "Color Cape").withStyle(ChatFormatting.BOLD), px + 10, py + 10, Ui.TEXT, true);
		g.drawString(font, beta ? "Pick the colors and the text on your cape" : "Pick any color for your cape and elytra", px + 10, py + 22, Ui.MUTED, false);
		if (Ui.iconButton(g, mx, my, px + pw - 26, py + 7, 18, Ui.CROSS) && click) action = this::onClose;

		int scale = Math.max(6, Math.min(10, (ph - 70) / 16)) & ~1;
		int capeX = px + 22, capeY = py + 44;
		drawCape(g, capeX, capeY, scale);

		int ox = capeX + 10 * scale + 24, ow = px + pw - 12 - ox, y = py + 42;
		int half = (ow - 4) / 2;
		if (beta) {
			g.drawString(font, "Text", ox, y, Ui.ACCENT_2, false);
			y += 12;
			int tw = (ow - 2 * 4) / 3;
			for (int i = 0; i < CapeTextures.TEXTS.size(); i++) {
				String t = CapeTextures.TEXTS.get(i);
				int tx = ox + (i % 3) * (tw + 4), ty = y + (i / 3) * 20;
				if (Ui.pill(g, font, mx, my, tx, ty, tw, 16, CapeTextures.TEXT_NAMES.get(i), t.equals(b.text)) && click) action = () -> b.text = t;
			}
			y += ((CapeTextures.TEXTS.size() + 2) / 3) * 20 + 6;
		}
		boolean grad = beta ? b.gradient : c.capeGradient;
		java.util.List<String> modes = new java.util.ArrayList<>();
		modes.add(grad ? "Top" : "Color");
		if (grad) modes.add("Bottom");
		if (beta) modes.add("Logo and text");
		int mw = (ow - 4 - 70) / modes.size();
		for (int i = 0; i < modes.size(); i++) {
			int mode = i == 0 ? 0 : modes.get(i).equals("Bottom") ? 1 : 2;
			if (Ui.pill(g, font, mx, my, ox + i * (mw + 2), y, mw, 16, modes.get(i), editing == mode) && click) action = () -> { editing = mode; hexBox.setValue(current()); };
		}
		if (Ui.pill(g, font, mx, my, ox + ow - 68, y, 68, 16, "Gradient", grad) && click) action = () -> {
			if (beta) b.gradient = !b.gradient; else c.capeGradient = !c.capeGradient;
			if (editing == 1) editing = 0;
			hexBox.setValue(current());
		};
		y += 22;
		int cols = 8, sw = Math.min(18, (ow - (cols - 1) * 3) / cols);
		String cur = current();
		for (int i = 0; i < CapeTextures.PALETTE.size(); i++) {
			String hx = CapeTextures.PALETTE.get(i);
			int sx = ox + (i % cols) * (sw + 3), sy = y + (i / cols) * (sw + 3);
			boolean hov = Ui.hover(mx, my, sx, sy, sw, sw);
			g.fill(sx - 1, sy - 1, sx + sw + 1, sy + sw + 1, 0xFF000000);
			g.fill(sx, sy, sx + sw, sy + sw, hex(hx));
			if (hx.equalsIgnoreCase(cur)) Ui.outline(g, sx - 2, sy - 2, sw + 4, sw + 4, 0xFFFFFFFF);
			else if (hov) Ui.outline(g, sx - 2, sy - 2, sw + 4, sw + 4, 0x90FFFFFF);
			if (hov && click) action = () -> { set(hx); hexBox.setValue(hx); };
		}
		y += 2 * (sw + 3) + 6;
		g.drawString(font, "Hex  #", ox, y + 4, Ui.MUTED, false);
		int hx0 = ox + font.width("Hex  #") + 2;
		Ui.box(g, hx0 - 3, y, 52, 16, 0xFF161619, hexBox.isFocused() ? Ui.ACCENT : Ui.EDGE);
		hexBox.setX(hx0 + 2);
		hexBox.setY(y + 4);
		g.fill(hx0 + 56, y, hx0 + 72, y + 16, 0xFF000000);
		g.fill(hx0 + 57, y + 1, hx0 + 71, y + 15, hex(cur));

		int by = py + ph - 26;
		String key = beta ? "beta" : "custom";
		boolean owns = gg.surge.client.economy.Wallet.get().owns("cape:" + key);
		boolean wearing = key.equals(c.cape);
		if (owns && Ui.pill(g, font, mx, my, ox, by, half, 18, wearing ? "Take off" : "Wear", !wearing) && click) action = () -> c.cape = wearing ? "none" : key;
		if (Ui.pill(g, font, mx, my, ox + half + 4, by, half, 18, "Done", false) && click) action = this::onClose;

		super.render(g, mx, my, delta);
		if (action != null) { Ui.click(); action.run(); SurgeConfig.save(); }
	}

	@Override
	public void onClose() {
		SurgeConfig.save();
		minecraft.setScreen(parent);
	}
}
