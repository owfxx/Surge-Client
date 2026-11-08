package gg.surge.legacy.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.surge.legacy.Config;
import gg.surge.legacy.mod.Category;
import gg.surge.legacy.mod.Mod;
import gg.surge.legacy.mod.Mods;
import gg.surge.legacy.mod.Setting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.util.Identifier;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MenuScreen extends Screen {
	private static final String[] PALETTE = {"FFFFFF", "A1A1AA", "000000", "8F50F2", "B483FF", "EC68B0", "F43F5E", "F0842A", "F2C430", "78CE46", "22BED4", "3468E8"};
	private static Category lastTab = Category.ALL;

	private final Screen parent;
	private Category tab = lastTab;
	private Mod open;
	private TextFieldWidget search;
	private int wx, wy, ww, wh, sideW, cx, cy, cw, ch;
	private float scroll, scrollTarget;
	private int contentH;
	private long openedAt;
	private Setting.Slider dragging;
	private int dragX, dragW;
	private final Map<Object, Float> anims = new HashMap<Object, Float>();
	private long lastFrame = System.currentTimeMillis();
	private float dt;

	public MenuScreen(Screen parent) {
		this.parent = parent;
	}

	public MenuScreen openMod(Mod m) {
		this.open = m;
		return this;
	}

	@Override
	public void init() {
		if (openedAt == 0) openedAt = System.currentTimeMillis();
		Keyboard.enableRepeatEvents(true);
		ww = Math.min(width - 12, Math.max(380, Math.min(560, Math.round(width * 0.72f))));
		wh = Math.min(height - 12, Math.max(220, Math.min(340, Math.round(height * 0.78f))));
		wx = (width - ww) / 2;
		wy = (height - wh) / 2;
		sideW = ww < 440 ? 84 : 100;
		cx = wx + sideW + 10;
		cy = wy + 34;
		cw = wx + ww - 8 - cx;
		ch = wy + wh - 8 - cy;
		String q = search == null ? "" : search.getText();
		search = new TextFieldWidget(0, textRenderer, 0, 0, 120, 12);
		search.setHasBorder(false);
		search.setMaxLength(32);
		search.setText(q);
	}

	@Override
	public void removed() {
		Keyboard.enableRepeatEvents(false);
		lastTab = tab;
		Config.save();
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}

	private float anim(Object key, boolean on) {
		Float v = anims.get(key);
		float cur = v == null ? (on ? 1f : 0f) : v;
		cur += ((on ? 1f : 0f) - cur) * Math.min(1f, dt * 16f);
		anims.put(key, cur);
		return cur;
	}

	static void click() {
		net.minecraft.client.MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(new Identifier("gui.button.press"), 1.0f));
	}

	private List<Mod> visibleMods() {
		List<Mod> out = new ArrayList<Mod>();
		String q = search.getText().trim().toLowerCase(Locale.ROOT);
		for (Mod m : Mods.all()) {
			if (!q.isEmpty()) {
				if (m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
			} else if (tab == Category.ALL || m.category == tab) out.add(m);
		}
		return out;
	}

	@Override
	public void render(int mx, int my, float delta) {
		long now = System.currentTimeMillis();
		dt = Math.min(0.1f, (now - lastFrame) / 1000f);
		lastFrame = now;
		float k = Math.min(1f, (now - openedAt) / 160f);
		k = 1f - (1f - k) * (1f - k) * (1f - k);
		Draw.rect(0, 0, width, height, Draw.alpha(0x88000000, k));
		GlStateManager.pushMatrix();
		GlStateManager.translate(0f, (1f - k) * 8f, 0f);
		Draw.roundBox(wx, wy, ww, wh, 6, Draw.BG, Draw.EDGE);
		header(mx, my);
		sidebar(mx, my);
		Draw.rect(wx + sideW, wy + 30, 1, wh - 36, 0xFF26262C);
		scroll += (scrollTarget - scroll) * Math.min(1f, dt * 18f);
		if (dragging != null) {
			if (Mouse.isButtonDown(0)) dragging.setFraction((mx - dragX) / (float) dragW);
			else dragging = null;
		}
		Draw.scissor(cx - 2, cy, cw + 4, ch);
		if (open != null) settings(mx, my);
		else grid(mx, my);
		Draw.endScissor();
		if (contentH > ch) {
			int barH = Math.max(20, ch * ch / contentH);
			int barY = cy + Math.round((ch - barH) * (scroll / Math.max(1, contentH - ch)));
			Draw.round(wx + ww - 5, barY, 2, barH, 1, 0xFF3A3A42);
		}
		GlStateManager.popMatrix();
	}

	private void header(int mx, int my) {
		int y = wy + 8;
		Draw.text("SURGE", wx + 12, y + 2, Draw.TEXT, true);
		Draw.text(net.minecraft.client.MinecraftClient.getCurrentFps() + " fps", wx + 16 + Draw.width("SURGE"), y + 2, Draw.FAINT, false);
		String eh = "Edit HUD";
		int ew = Draw.width(eh) + 14, ex = wx + ww - 8 - ew;
		boolean eHov = Draw.hover(mx, my, ex, y - 3, ew, 16);
		Draw.round(ex, y - 3, ew, 16, 4, eHov ? Draw.ACCENT_2 : Draw.ACCENT);
		Draw.text(eh, ex + 7, y + 1, 0xFFFFFFFF, false);
		int sx = cx, sw = Math.max(80, Math.min(170, ex - 8 - sx));
		boolean f = search.isFocused();
		Draw.roundBox(sx, y - 3, sw, 16, 4, 0xFF141418, f ? Draw.ACCENT_2 : 0xFF2E2E35);
		search.x = sx + 6;
		search.y = y + 1;
		if (search.getText().isEmpty() && !f) Draw.text("Search mods", sx + 6, y + 1, 0xFF55555C, false);
		search.render();
	}

	private void sidebar(int mx, int my) {
		int y = wy + 36;
		for (Category c : Category.values()) {
			boolean sel = c == tab && open == null && search.getText().isEmpty();
			boolean hov = Draw.hover(mx, my, wx + 6, y, sideW - 10, 18);
			float a = anim("tab" + c, sel || hov);
			if (a > 0.01f) Draw.round(wx + 6, y, sideW - 10, 18, 4, Draw.alpha(sel ? 0xFF2C2440 : 0xFF222228, a));
			if (sel) Draw.round(wx + 6, y + 4, 2, 10, 1, Draw.ACCENT_2);
			Draw.text(c.label, wx + 14, y + 5, sel ? Draw.TEXT : hov ? 0xFFD4D4D8 : Draw.MUTED, false);
			y += 20;
		}
		int on = 0, total = 0;
		for (Mod m : Mods.all()) if (m.toggleable) { total++; if (m.enabled()) on++; }
		Draw.text(Draw.fit(on + " of " + total + " on", sideW - 14), wx + 10, wy + wh - 14, 0xFF4A4A52, false);
	}

	private int cols() {
		return Math.max(2, cw / 104);
	}

	private void grid(int mx, int my) {
		List<Mod> list = visibleMods();
		int cols = cols(), gap = 6;
		int cardW = (cw - gap * (cols - 1)) / cols, cardH = 70;
		int rows = (list.size() + cols - 1) / cols;
		contentH = rows * (cardH + gap);
		clampScroll();
		boolean inside = Draw.hover(mx, my, cx, cy, cw, ch);
		for (int i = 0; i < list.size(); i++) {
			Mod m = list.get(i);
			int x = cx + (i % cols) * (cardW + gap), y = cy + (i / cols) * (cardH + gap) - Math.round(scroll);
			if (y + cardH < cy || y > cy + ch) continue;
			boolean hov = inside && Draw.hover(mx, my, x, y, cardW, cardH);
			float h = anim(m, hov);
			Draw.roundBox(x, y, cardW, cardH, 5, Draw.lerp(Draw.CARD, Draw.CARD_HOVER, h), Draw.lerp(0xFF2A2A31, 0xFF3A3A44, h));
			Draw.item(m.icon, x + (cardW - 20) / 2, y + 8, 20);
			Draw.center(Draw.fit(m.name, cardW - 8), x + cardW / 2, y + 33, Draw.TEXT, false);
			boolean on = m.enabled();
			float t = anim("on" + m.id, on);
			int bx = x + 6, by = y + cardH - 20, bw = cardW - 12;
			boolean bh = inside && Draw.hover(mx, my, bx, by, bw, 14);
			Draw.round(bx, by, bw, 14, 4, Draw.lerp(bh ? 0xFF3A2A2E : 0xFF2C2224, bh ? 0xFF2F5F43 : 0xFF234A35, t));
			String label = on ? "Enabled" : "Disabled";
			Draw.center(label, bx + bw / 2, by + 3, Draw.lerp(Draw.RED, Draw.GREEN, t), false);
		}
		if (list.isEmpty()) Draw.center("No mods match your search", cx + cw / 2, cy + 30, Draw.FAINT, false);
	}

	private void clampScroll() {
		float max = Math.max(0, contentH - ch);
		if (scrollTarget > max) scrollTarget = max;
		if (scrollTarget < 0) scrollTarget = 0;
	}

	private void settings(int mx, int my) {
		int y = cy - Math.round(scroll);
		boolean inside = Draw.hover(mx, my, cx, cy, cw, ch);
		boolean bh = inside && Draw.hover(mx, my, cx, y, 16, 16);
		Draw.round(cx, y, 16, 16, 4, bh ? 0xFF33333A : 0xFF26262C);
		Draw.text("<", cx + 5, y + 4, Draw.TEXT, false);
		Draw.item(open.icon, cx + 22, y, 16);
		Draw.text(open.name, cx + 42, y + 4, Draw.TEXT, true);
		if (open.toggleable) Draw.toggle(cx + cw - 26, y + 2, open.enabled(), anim("on" + open.id, open.enabled()));
		y += 20;
		for (String line : textRenderer.wrapLines(open.description, cw - 4)) {
			Draw.text(line, cx, y, Draw.MUTED, false);
			y += 10;
		}
		y += 6;
		int start = y + Math.round(scroll);
		for (Setting<?> s : open.settings) {
			if (!s.visible()) continue;
			int h = rowHeight(s);
			boolean hov = inside && Draw.hover(mx, my, cx, y, cw, h);
			Draw.round(cx, y, cw, h - 2, 4, hov ? 0xFF222228 : 0xFF1C1C21);
			row(s, cx, y, cw, h - 2, mx, my);
			y += h;
		}
		if (open instanceof gg.surge.legacy.hud.HudMod) {
			gg.surge.legacy.hud.HudMod hm = (gg.surge.legacy.hud.HudMod) open;
			Draw.round(cx, y, cw, 20, 4, 0xFF1C1C21);
			Draw.text("Size", cx + 8, y + 6, Draw.TEXT, false);
			String v = String.format("%.2fx", hm.scale());
			Draw.text(v, cx + cw - 8 - Draw.width(v), y + 6, Draw.ACCENT_2, false);
			int tx = cx + cw / 2, tw = cw / 2 - 50;
			Draw.round(tx, y + 9, tw, 3, 1, 0xFF33333A);
			float f = (hm.scale() - 0.5f) / 2f;
			Draw.round(tx, y + 9, Math.round(tw * f), 3, 1, Draw.ACCENT);
			Draw.round(tx + Math.round(tw * f) - 3, y + 7, 7, 7, 3, 0xFFFFFFFF);
			y += 22;
		}
		contentH = y + Math.round(scroll) - cy;
		clampScroll();
	}

	private int rowHeight(Setting<?> s) {
		return s instanceof Setting.Color ? 36 : 22;
	}

	private void row(Setting<?> s, int x, int y, int w, int h, int mx, int my) {
		Draw.text(s.name, x + 8, y + 6, Draw.TEXT, false);
		if (s instanceof Setting.Toggle) {
			Setting.Toggle t = (Setting.Toggle) s;
			Draw.toggle(x + w - 30, y + 4, t.on(), anim(s, t.on()));
		} else if (s instanceof Setting.Slider) {
			Setting.Slider sl = (Setting.Slider) s;
			String v = sl.format();
			Draw.text(v, x + w - 8 - Draw.width(v), y + 6, Draw.ACCENT_2, false);
			int tx = x + w / 2, tw = w / 2 - 14 - Draw.width(v);
			Draw.round(tx, y + 9, tw, 3, 1, 0xFF33333A);
			Draw.round(tx, y + 9, Math.round(tw * sl.fraction()), 3, 1, Draw.ACCENT);
			Draw.round(tx + Math.round(tw * sl.fraction()) - 3, y + 7, 7, 7, 3, 0xFFFFFFFF);
		} else if (s instanceof Setting.Choice) {
			Setting.Choice c = (Setting.Choice) s;
			String v = c.get();
			int vw = Math.max(50, Draw.width(v) + 24), vx = x + w - 6 - vw;
			Draw.round(vx, y + 3, vw, 14, 4, 0xFF2A2A31);
			Draw.text("<", vx + 4, y + 6, Draw.FAINT, false);
			Draw.text(">", vx + vw - 9, y + 6, Draw.FAINT, false);
			Draw.center(v, vx + vw / 2, y + 6, Draw.TEXT, false);
		} else if (s instanceof Setting.Color) {
			Setting.Color c = (Setting.Color) s;
			int sx = x + 8, sy = y + 19;
			for (int i = 0; i <= PALETTE.length; i++) {
				boolean chroma = i == PALETTE.length;
				int col = chroma ? java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 3000) / 3000f, 0.6f, 1f) | 0xFF000000 : 0xFF000000 | Integer.parseInt(PALETTE[i], 16);
				boolean sel = chroma ? c.chroma() : PALETTE[i].equalsIgnoreCase(c.get());
				if (sel) Draw.round(sx - 1, sy - 1, 13, 13, 3, 0xFFFFFFFF);
				Draw.round(sx, sy, 11, 11, 3, col);
				sx += 15;
			}
			Draw.round(x + w - 22, y + 4, 14, 10, 3, c.rgb(0));
		} else if (s instanceof Setting.Button) {
			Setting.Button b = (Setting.Button) s;
			int bw = Draw.width(b.label) + 16, bx = x + w - 6 - bw;
			boolean hov = Draw.hover(mx, my, bx, y + 3, bw, 14);
			Draw.round(bx, y + 3, bw, 14, 4, hov ? Draw.ACCENT_2 : Draw.ACCENT);
			Draw.center(b.label, bx + bw / 2, y + 6, 0xFFFFFFFF, false);
		}
	}

	@Override
	protected void mouseClicked(int mx, int my, int button) {
		search.mouseClicked(mx, my, button);
		int oy = Math.round((1f - Math.min(1f, (System.currentTimeMillis() - openedAt) / 160f)) * 8f);
		my -= oy;
		int y = wy + 8;
		String eh = "Edit HUD";
		int ew = Draw.width(eh) + 14, ex = wx + ww - 8 - ew;
		if (Draw.hover(mx, my, ex, y - 3, ew, 16)) { click(); client.setScreen(new HudEditorScreen(this)); return; }
		if (!Draw.hover(mx, my, wx, wy, ww, wh)) return;
		int sy = wy + 36;
		for (Category c : Category.values()) {
			if (Draw.hover(mx, my, wx + 6, sy, sideW - 10, 18)) {
				click();
				tab = c;
				open = null;
				search.setText("");
				scroll = scrollTarget = 0;
				return;
			}
			sy += 20;
		}
		if (!Draw.hover(mx, my, cx, cy, cw, ch)) return;
		if (open != null) clickSettings(mx, my, button);
		else clickGrid(mx, my, button);
	}

	private void clickGrid(int mx, int my, int button) {
		List<Mod> list = visibleMods();
		int cols = cols(), gap = 6;
		int cardW = (cw - gap * (cols - 1)) / cols, cardH = 70;
		for (int i = 0; i < list.size(); i++) {
			Mod m = list.get(i);
			int x = cx + (i % cols) * (cardW + gap), y = cy + (i / cols) * (cardH + gap) - Math.round(scroll);
			if (!Draw.hover(mx, my, x, y, cardW, cardH)) continue;
			click();
			if (Draw.hover(mx, my, x + 6, y + cardH - 20, cardW - 12, 14) || button == 1 && m.settings.isEmpty()) m.toggle();
			else { open = m; scroll = scrollTarget = 0; }
			return;
		}
	}

	private void clickSettings(int mx, int my, int button) {
		int y = cy - Math.round(scroll);
		if (Draw.hover(mx, my, cx, y, 16, 16)) { click(); open = null; scroll = scrollTarget = 0; return; }
		if (open.toggleable && Draw.hover(mx, my, cx + cw - 30, y, 30, 18)) { click(); open.toggle(); return; }
		y += 20 + textRenderer.wrapLines(open.description, cw - 4).size() * 10 + 6;
		for (Setting<?> s : open.settings) {
			if (!s.visible()) continue;
			int h = rowHeight(s);
			if (Draw.hover(mx, my, cx, y, cw, h - 2)) {
				clickRow(s, cx, y, cw, mx, my, button);
				return;
			}
			y += h;
		}
		if (open instanceof gg.surge.legacy.hud.HudMod && Draw.hover(mx, my, cx, y, cw, 20)) {
			int tx = cx + cw / 2, tw = cw / 2 - 50;
			float f = Math.max(0, Math.min(1, (mx - tx) / (float) tw));
			((gg.surge.legacy.hud.HudMod) open).setScale(Math.round((0.5f + f * 2f) * 20f) / 20f);
		}
	}

	private void clickRow(Setting<?> s, int x, int y, int w, int mx, int my, int button) {
		if (s instanceof Setting.Toggle) { click(); ((Setting.Toggle) s).flip(); }
		else if (s instanceof Setting.Choice) { click(); ((Setting.Choice) s).cycle(button == 1 ? -1 : 1); }
		else if (s instanceof Setting.Slider) {
			Setting.Slider sl = (Setting.Slider) s;
			int tx = x + w / 2, tw = w / 2 - 14 - Draw.width(sl.format());
			if (mx >= tx - 4) { dragging = sl; dragX = tx; dragW = tw; sl.setFraction((mx - tx) / (float) tw); }
		} else if (s instanceof Setting.Color) {
			Setting.Color c = (Setting.Color) s;
			int sx = x + 8, sy = y + 19;
			for (int i = 0; i <= PALETTE.length; i++) {
				if (Draw.hover(mx, my, sx - 1, sy - 1, 13, 13)) { click(); c.set(i == PALETTE.length ? "chroma" : PALETTE[i]); return; }
				sx += 15;
			}
		} else if (s instanceof Setting.Button) { click(); ((Setting.Button) s).action.run(); }
	}

	@Override
	public void handleMouse() {
		super.handleMouse();
		int d = Mouse.getEventDWheel();
		if (d != 0) {
			scrollTarget -= Math.signum(d) * 30;
			clampScroll();
		}
	}

	@Override
	protected void keyPressed(char c, int key) {
		if (key == Keyboard.KEY_ESCAPE) {
			if (search.isFocused() && !search.getText().isEmpty()) { search.setText(""); return; }
			if (open != null) { open = null; scroll = scrollTarget = 0; return; }
			client.setScreen(parent);
			return;
		}
		if (search.keyPressed(c, key)) { open = null; scroll = scrollTarget = 0; return; }
		if (gg.surge.legacy.Keys.MENU.getCode() == key && !search.isFocused()) { client.setScreen(parent); return; }
		if (Character.isLetterOrDigit(c) && !search.isFocused()) {
			search.setFocused(true);
			search.keyPressed(c, key);
			open = null;
		}
	}

	@Override
	public void tick() {
		search.tick();
	}
}
