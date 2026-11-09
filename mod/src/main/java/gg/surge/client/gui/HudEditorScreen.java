package gg.surge.client.gui;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.hud.HudModule;
import gg.surge.client.hud.HudModules;
import gg.surge.client.mods.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class HudEditorScreen extends Screen {
	private static final int GRID = 16, SNAP = 5;
	private final Screen parent;
	private HudModule dragging, resizing, hovered;
	private int offX, offY;
	private float resizeStartScale;
	private int resizeStartX, resizeStartW;
	private boolean wasL = true, wasR = true;
	private int guideX = -1, guideY = -1;

	public HudEditorScreen(Screen parent) {
		super(Component.literal("Edit HUD"));
		this.parent = parent;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, 0x38000000);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (hovered != null && scrollY != 0) {
			setScale(hovered, hovered.scale() + (scrollY > 0 ? 0.05f : -0.05f));
			return true;
		}
		return false;
	}

	private void setScale(HudModule m, float s) {
		m.state().scale = Math.round(Math.max(0.5f, Math.min(2.5f, s)) * 20f) / 20f;
		SurgeConfig.markDirty();
	}

	private boolean key(int k) {
		return GLFW.glfwGetKey(minecraft.getWindow().handle(), k) == GLFW.GLFW_PRESS;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		Ui.frame();
		long win = minecraft.getWindow().handle();
		boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean rdown = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasL, rclick = rdown && !wasR;
		wasL = down;
		wasR = rdown;
		boolean alt = key(GLFW.GLFW_KEY_LEFT_ALT) || key(GLFW.GLFW_KEY_RIGHT_ALT);
		boolean ctrl = key(GLFW.GLFW_KEY_LEFT_CONTROL) || key(GLFW.GLFW_KEY_RIGHT_CONTROL);
		int W = g.guiWidth(), H = g.guiHeight();

		boolean busy = dragging != null || resizing != null;
		float ui = Ui.approach("hudedit:ui", busy ? 0f : 1f, 12f);
		int tw = 200, tx = (W - tw) / 2, ty = H - 34 + Math.round((1 - ui) * 46);
		int hubW = 5 * 60 + 4 * 4, hubX = (W - hubW) / 2, hubY = H / 2 - 36;
		boolean onToolbar = Ui.hover(mx, my, tx, ty, tw, 22) || (ui > 0.5f && Ui.hover(mx, my, hubX - 4, hubY - 4, hubW + 8, 84));
		Runnable action = null;

		if (alt) {
			for (int x = 0; x < W; x += GRID) g.fill(x, 0, x + 1, H, 0x14FFFFFF);
			for (int y = 0; y < H; y += GRID) g.fill(0, y, W, y + 1, 0x14FFFFFF);
			g.fill(W / 2, 0, W / 2 + 1, H, Colors.alpha(Ui.ACCENT_2, 0.25f));
			g.fill(0, H / 2, W, H / 2 + 1, Colors.alpha(Ui.ACCENT_2, 0.25f));
		}

		hovered = null;
		boolean onCorner = false;
		if (dragging == null && resizing == null && !onToolbar) {
			for (int i = HudModules.ALL.size() - 1; i >= 0; i--) {
				HudModule m = HudModules.ALL.get(i);
				if (!m.enabled()) continue;
				int x = m.screenX(g, minecraft), y = m.screenY(g, minecraft);
				int w = m.scaledWidth(minecraft), h = m.scaledHeight(minecraft);
				if (Ui.hover(mx, my, x + w - 4, y + h - 4, 8, 8)) { hovered = m; onCorner = true; break; }
				if (Ui.hover(mx, my, x, y, w, h)) { hovered = m; break; }
			}
		}
		if (click && hovered != null) {
			if (onCorner) {
				resizing = hovered;
				resizeStartScale = hovered.scale();
				resizeStartX = mx;
				resizeStartW = hovered.scaledWidth(minecraft);
			} else {
				dragging = hovered;
				offX = mx - dragging.screenX(g, minecraft);
				offY = my - dragging.screenY(g, minecraft);
			}
		}
		if (rclick && hovered != null) {
			HudModule target = hovered;
			action = () -> minecraft.setScreen(SurgeMenuScreen.settings(target, this));
		}

		guideX = guideY = -1;
		if (dragging != null) {
			if (down) {
				int w = dragging.scaledWidth(minecraft), h = dragging.scaledHeight(minecraft);
				int nx = mx - offX, ny = my - offY;
				if (alt) { nx = snapGridX(nx, w, W); ny = snapGridY(ny, h, H); }
				else if (ctrl) { nx = snapModX(g, nx, w); ny = snapModY(g, ny, h); }
				nx = Math.max(0, Math.min(W - w, nx));
				ny = Math.max(0, Math.min(H - h, ny));
				dragging.state().x = nx / (float) W;
				dragging.state().y = ny / (float) H;
			} else {
				dragging = null;
				SurgeConfig.save();
			}
		}
		if (resizing != null) {
			if (down) {
				float base = resizeStartW / Math.max(0.01f, resizeStartScale);
				float s = (resizeStartW + (mx - resizeStartX)) / base;
				if (alt) s = Math.round(s * 4f) / 4f;
				setScale(resizing, s);
			} else {
				resizing = null;
				SurgeConfig.save();
			}
		}

		if (guideX >= 0) g.fill(guideX, 0, guideX + 1, H, Ui.ACCENT_2);
		if (guideY >= 0) g.fill(0, guideY, W, guideY + 1, Ui.ACCENT_2);

		for (HudModule m : HudModules.ALL) {
			if (!m.enabled()) continue;
			int x = m.screenX(g, minecraft), y = m.screenY(g, minecraft);
			int w = m.scaledWidth(minecraft), h = m.scaledHeight(minecraft);
			boolean active = m == dragging || m == resizing;
			float a = Ui.anim("hud:" + m.id, active || m == hovered);
			g.fill(x - 1, y - 1, x + w + 1, y + h + 1, Colors.alpha(0x30FFFFFF, 0.4f + a * 0.6f));
			Ui.outline(g, x - 1, y - 1, w + 2, h + 2, Colors.lerp(0x60FFFFFF, active ? 0xFFFFFFFF : Ui.ACCENT_2, a));
			m.draw(g, minecraft);
			if (m == hovered || m == resizing) {
				g.fill(x + w - 3, y + h - 3, x + w + 2, y + h + 2, Ui.EDGE);
				g.fill(x + w - 2, y + h - 2, x + w + 1, y + h + 1, 0xFFFFFFFF);
			}
		}

		HudModule label = resizing != null ? resizing : hovered;
		if (label != null) {
			String text = label.name + "  " + Math.round(label.scale() * 100) + "%";
			int lx = Math.min(mx + 12, W - font.width(text) - 8), ly = Math.max(4, my - 16);
			Ui.box(g, lx - 4, ly - 3, font.width(text) + 8, 14, 0xF0141418, Ui.EDGE);
			g.drawString(font, text, lx, ly, Ui.TEXT, false);
		}

		if (ui > 0.05f) {
			Runnable hub = hub(g, mx, my, click, hubX, hubY, hubW, ui);
			if (hub != null) action = hub;
		}

		Ui.shadow(g, tx, ty, tw, 22);
		Ui.box(g, tx, ty, tw, 22, Ui.BG, Ui.EDGE);
		int bw = (tw - 16) / 3;
		if (Ui.pill(g, font, mx, my, tx + 4, ty + 3, bw, 16, "Done", true) && click) action = this::onClose;
		if (Ui.pill(g, font, mx, my, tx + 8 + bw, ty + 3, bw, 16, "Mods", false) && click) action = () -> minecraft.setScreen(new SurgeMenuScreen(this));
		if (Ui.pill(g, font, mx, my, tx + 12 + bw * 2, ty + 3, bw, 16, "Reset", false) && click) action = this::resetPositions;
		if (ui > 0.05f) {
			Ui.centeredShadow(g, font, "Hold Alt to snap to the grid, Ctrl to line up with other mods", W / 2, ty - 24, Colors.alpha(0xC0FFFFFF, ui));
			Ui.centeredShadow(g, font, "Drag the corner or scroll to resize, right click for settings", W / 2, ty - 13, Colors.alpha(0x90FFFFFF, ui));
		}

		if (action != null) { Ui.click(); action.run(); }
	}

	private static net.minecraft.resources.Identifier hubIcon(String n) {
		return net.minecraft.resources.Identifier.fromNamespaceAndPath("surge", "textures/gui/hub/" + n + ".png");
	}

	private boolean hubButton(GuiGraphics g, int mx, int my, int x, int y, int w, int h, float ui, String key) {
		boolean hov = Ui.hover(mx, my, x, y, w, h);
		float a = Ui.anim("hub:" + key, hov);
		if (Ui.glass()) Ui.glassRect(g, x, y, w, h, 6, 0.8f + a * 0.6f);
		else {
			g.fill(x, y, x + w, y + h, Colors.alpha(Colors.lerp(0x8C0E0E10, 0xB0202024, a), ui));
			Ui.outline(g, x, y, w, h, Colors.alpha(Colors.lerp(0x24FFFFFF, 0x60FFFFFF, a), ui));
		}
		return hov;
	}

	private Runnable hub(GuiGraphics g, int mx, int my, boolean click, int x, int y, int w, float ui) {
		Runnable out = null;
		int mainW = 150, mainH = 28, mainX = (width - mainW) / 2;
		int lift = Math.round((1 - ui) * 10);
		y += lift;
		boolean hov = hubButton(g, mx, my, mainX, y, mainW, mainH, ui, "mods");
		int textW = font.width("Mod Menu") + 20;
		int ix = mainX + (mainW - textW) / 2;
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, hubIcon("mods"), ix, y + 8, 0f, 0f, 12, 12, 64, 64, 64, 64, Colors.alpha(0xFFFFFFFF, ui));
		g.drawString(font, Component.literal("Mod Menu").withStyle(net.minecraft.ChatFormatting.BOLD), ix + 18, y + 10, Colors.alpha(0xFFFFFFFF, ui), false);
		if (hov && click) out = () -> minecraft.setScreen(new SurgeMenuScreen(this));
		String[][] items = {{"wardrobe", "Wardrobe"}, {"emotes", "Emotes"}, {"friends", "Friends"}, {"settings", "Settings"}, {"calc", "Calculator"}};
		int by = y + mainH + 6, bw = 60, bh = 42;
		for (int i = 0; i < items.length; i++) {
			int bx = x + i * (bw + 4);
			String id = items[i][0];
			boolean disabled = id.equals("emotes") && minecraft.player == null;
			boolean h = hubButton(g, mx, my, bx, by, bw, bh, disabled ? ui * 0.5f : ui, id);
			float ia = (disabled ? 0.35f : 1f) * ui;
			g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, hubIcon(id), bx + (bw - 16) / 2, by + 7, 0f, 0f, 16, 16, 64, 64, 64, 64, Colors.alpha(0xFFFFFFFF, ia));
			String label = items[i][1];
			if (font.width(label) > bw - 4) label = Ui.fit(font, label, bw - 4);
			Ui.centered(g, font, label, bx + bw / 2, by + 28, Colors.alpha(h ? 0xFFFFFFFF : 0xFFC8C8CE, ia));
			if (h && click && !disabled) {
				out = switch (id) {
					case "wardrobe" -> () -> minecraft.setScreen(new WardrobeScreen(this));
					case "emotes" -> () -> minecraft.setScreen(new EmoteWheelScreen());
					case "friends" -> () -> minecraft.setScreen(new SocialScreen(this));
					case "settings" -> () -> minecraft.setScreen(SurgeMenuScreen.general(this));
					default -> () -> minecraft.setScreen(new CalculatorScreen(this));
				};
			}
		}
		return out;
	}

	private void resetPositions() {
		for (HudModule m : HudModules.ALL) {
			SurgeConfig.ModuleState s = m.state();
			boolean on = s.enabled;
			var opts = s.opts;
			SurgeConfig.get().modules.remove(m.id);
			SurgeConfig.ModuleState fresh = m.state();
			fresh.enabled = on;
			fresh.opts = opts;
		}
		SurgeConfig.save();
	}

	private int snapGridX(int x, int w, int W) {
		if (Math.abs(x + w / 2 - W / 2) <= SNAP) { guideX = W / 2; return W / 2 - w / 2; }
		if (x <= SNAP) { guideX = 0; return 0; }
		if (Math.abs(x + w - W) <= SNAP) { guideX = W - 1; return W - w; }
		int g = Math.round(x / (float) GRID) * GRID;
		guideX = g;
		return g;
	}

	private int snapGridY(int y, int h, int H) {
		if (Math.abs(y + h / 2 - H / 2) <= SNAP) { guideY = H / 2; return H / 2 - h / 2; }
		if (y <= SNAP) { guideY = 0; return 0; }
		if (Math.abs(y + h - H) <= SNAP) { guideY = H - 1; return H - h; }
		int g = Math.round(y / (float) GRID) * GRID;
		guideY = g;
		return g;
	}

	private int snapModX(GuiGraphics g, int x, int w) {
		int best = x, bestD = SNAP + 1;
		for (HudModule o : HudModules.ALL) {
			if (o == dragging || !o.enabled()) continue;
			int ox = o.screenX(g, minecraft), ow = o.scaledWidth(minecraft);
			int[][] pairs = {{ox, ox}, {ox + ow - w, ox + ow}, {ox + ow + 2, ox + ow + 1}, {ox - w - 2, ox - 1}, {ox + ow / 2 - w / 2, ox + ow / 2}};
			for (int[] p : pairs) {
				int d = Math.abs(x - p[0]);
				if (d < bestD) { bestD = d; best = p[0]; guideX = p[1]; }
			}
		}
		return best;
	}

	private int snapModY(GuiGraphics g, int y, int h) {
		int best = y, bestD = SNAP + 1;
		for (HudModule o : HudModules.ALL) {
			if (o == dragging || !o.enabled()) continue;
			int oy = o.screenY(g, minecraft), oh = o.scaledHeight(minecraft);
			int[][] pairs = {{oy, oy}, {oy + oh - h, oy + oh}, {oy + oh + 2, oy + oh + 1}, {oy - h - 2, oy - 1}, {oy + oh / 2 - h / 2, oy + oh / 2}};
			for (int[] p : pairs) {
				int d = Math.abs(y - p[0]);
				if (d < bestD) { bestD = d; best = p[0]; guideY = p[1]; }
			}
		}
		return best;
	}

	@Override
	public void onClose() {
		SurgeConfig.save();
		minecraft.setScreen(parent);
	}
}
