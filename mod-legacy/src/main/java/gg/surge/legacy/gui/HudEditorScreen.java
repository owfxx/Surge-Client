package gg.surge.legacy.gui;

import gg.surge.legacy.hud.Hud;
import gg.surge.legacy.hud.HudMod;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

public class HudEditorScreen extends Screen {
	private final Screen parent;
	private HudMod drag;
	private int offX, offY;
	private boolean snapX, snapY;

	public HudEditorScreen(Screen parent) {
		this.parent = parent;
	}

	@Override
	public boolean shouldPauseGame() {
		return false;
	}

	private HudMod at(int mx, int my) {
		for (int i = Hud.ALL.size() - 1; i >= 0; i--) {
			HudMod m = Hud.ALL.get(i);
			if (!m.enabled()) continue;
			if (Draw.hover(mx, my, m.screenX(width), m.screenY(height), m.sw(), m.sh())) return m;
		}
		return null;
	}

	@Override
	public void render(int mx, int my, float delta) {
		Draw.rect(0, 0, width, height, 0x55000000);
		if (drag != null) {
			if (!Mouse.isButtonDown(0)) drag = null;
			else {
				int x = mx - offX, y = my - offY;
				int w = drag.sw(), h = drag.sh();
				snapX = Math.abs(x + w / 2 - width / 2) < 4;
				snapY = Math.abs(y + h / 2 - height / 2) < 4;
				if (snapX) x = width / 2 - w / 2;
				if (snapY) y = height / 2 - h / 2;
				x = Math.max(0, Math.min(width - w, x));
				y = Math.max(0, Math.min(height - h, y));
				drag.setPos(x / (float) width, y / (float) height);
			}
		}
		if (drag != null && snapX) Draw.rect(width / 2, 0, 1, height, 0x88B483FF);
		if (drag != null && snapY) Draw.rect(0, height / 2, width, 1, 0x88B483FF);
		HudMod hov = drag != null ? drag : at(mx, my);
		for (HudMod m : Hud.ALL) {
			if (!m.enabled()) continue;
			try { m.draw(width, height); } catch (Throwable ignored) { }
			int x = m.screenX(width), y = m.screenY(height);
			Draw.outline(x - 1, y - 1, m.sw() + 2, m.sh() + 2, m == hov ? 0xFFB483FF : 0x55FFFFFF);
		}
		if (hov != null) {
			String t = hov.name + "  " + String.format("%.2fx", hov.scale()) + "  Scroll to resize";
			int tw = Draw.width(t) + 12;
			Draw.round(width / 2 - tw / 2, height - 52, tw, 14, 4, 0xDD141418);
			Draw.center(t, width / 2, height - 49, Draw.TEXT, false);
		}
		int bw = 90, bx = width / 2 - bw / 2, by = height - 32;
		boolean bh = Draw.hover(mx, my, bx, by, bw, 18);
		Draw.round(bx, by, bw, 18, 5, bh ? Draw.ACCENT_2 : Draw.ACCENT);
		Draw.center("Done", width / 2, by + 5, 0xFFFFFFFF, false);
		int mw = 90, mxb = width / 2 - mw / 2;
		boolean mh = Draw.hover(mx, my, mxb, 10, mw, 16);
		Draw.round(mxb, 10, mw, 16, 5, mh ? 0xEE2A2A31 : 0xEE1C1C21);
		Draw.center("Mods", width / 2, 14, Draw.TEXT, false);
	}

	@Override
	protected void mouseClicked(int mx, int my, int button) {
		int bw = 90, bx = width / 2 - bw / 2, by = height - 32;
		if (Draw.hover(mx, my, bx, by, bw, 18)) { MenuScreen.click(); client.setScreen(parent); return; }
		if (Draw.hover(mx, my, width / 2 - 45, 10, 90, 16)) { MenuScreen.click(); client.setScreen(new MenuScreen(null).openMod(null)); return; }
		HudMod m = at(mx, my);
		if (m == null) return;
		if (button == 1) { MenuScreen.click(); client.setScreen(new MenuScreen(this).openMod(m)); return; }
		drag = m;
		offX = mx - m.screenX(width);
		offY = my - m.screenY(height);
	}

	@Override
	public void handleMouse() {
		super.handleMouse();
		int d = Mouse.getEventDWheel();
		if (d == 0) return;
		int mx = Mouse.getEventX() * width / client.width, my = height - Mouse.getEventY() * height / client.height - 1;
		HudMod m = at(mx, my);
		if (m != null) m.setScale(Math.round((m.scale() + (d > 0 ? 0.05f : -0.05f)) * 20f) / 20f);
	}

	@Override
	protected void keyPressed(char c, int key) {
		if (key == Keyboard.KEY_ESCAPE) client.setScreen(parent);
	}

	@Override
	public void removed() {
		gg.surge.legacy.Config.save();
	}
}
