package gg.surge.client.gui;

import gg.surge.client.mods.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class Ui {
	public static final int BG = 0xF2242427;
	public static final int SIDEBAR = 0xFF1C1C1F;
	public static final int SURFACE = 0xFF3A3A3D;
	public static final int SURFACE_HOVER = 0xFF48484C;
	public static final int EDGE = 0xFF0C0C0D;
	public static int ACCENT = 0xFF6B6B73;
	public static int ACCENT_HOVER = 0xFF7E7E86;
	public static int ACCENT_DIM = 0xFF404045;
	public static int ACCENT_2 = 0xFFC4C4C9;
	private static String themeKey = "";

	private static void theme() {
		String t = gg.surge.client.mods.Mods.THEME.get();
		String key = t + gg.surge.client.mods.Mods.THEME_COLOR.get();
		if (key.equals(themeKey)) return;
		themeKey = key;
		int base = switch (t) {
			case "Blue" -> 0x3B82F6;
			case "Cyan" -> 0x06B6D4;
			case "Green" -> 0x22C55E;
			case "Red" -> 0xEF4444;
			case "Orange" -> 0xF97316;
			case "Pink" -> 0xEC4899;
			case "Gold" -> 0xEAB308;
			case "Purple" -> 0x8F50F2;
			case "Mono" -> 0x8A8A92;
			case "Custom" -> gg.surge.client.mods.Mods.THEME_COLOR.rgb(0) & 0xFFFFFF;
			default -> 0x6B6B73;
		};
		ACCENT = 0xFF000000 | base;
		ACCENT_HOVER = Colors.lerp(ACCENT, 0xFFFFFFFF, 0.15f);
		ACCENT_DIM = Colors.lerp(ACCENT, 0xFF000000, 0.4f);
		ACCENT_2 = Colors.lerp(ACCENT, 0xFFFFFFFF, t.equals("Ash") || t.equals("Mono") ? 0.6f : 0.35f);
	}
	public static final int TEXT = 0xFFFFFFFF;
	public static final int MUTED = 0xFFA9AAAE;
	public static final int FAINT = 0xFF6E6F74;
	public static final int YELLOW = 0xFFFFE07A;
	public static final int ON = 0xFF3C8527;
	public static final int ON_LIP = 0xFF1D4D13;
	public static final int OFF = 0xFFA02C2C;
	public static final int OFF_LIP = 0xFF5E1717;
	public static final int NEUTRAL = 0xFF4C4C50;
	public static final int BUTTON = 0xFF4C4C50, BUTTON_HOVER = 0xFF5E5E63, BUTTON_LIP = 0xFF2E2E31;
	public static final ItemStack COIN_ITEM = new ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET);
	private static final Identifier[] COIN_TEX = {Identifier.fromNamespaceAndPath("surge", "textures/gui/coin_10.png"),
		Identifier.fromNamespaceAndPath("surge", "textures/gui/coin_16.png"), Identifier.fromNamespaceAndPath("surge", "textures/gui/coin_32.png")};

	public static void coin(GuiGraphics g, int x, int y, int size) {
		int idx = size < 14 ? 0 : size < 28 ? 1 : 2;
		int s = idx == 0 ? 10 : idx == 1 ? 16 : 32;
		g.blit(RenderPipelines.GUI_TEXTURED, COIN_TEX[idx], x + (size - s) / 2, y + (size - s) / 2, 0f, 0f, s, s, s, s, s, s);
	}

	public static final Identifier LOGO = Identifier.fromNamespaceAndPath("surge", "textures/gui/logo.png");

	private static final Map<Object, Float> ANIM = new HashMap<>();
	private static long lastFrame = System.nanoTime();
	private static float dt = 0.016f;

	private Ui() {}

	public static void frame() {
		theme();
		long now = System.nanoTime();
		long diff = now - lastFrame;
		if (diff < 1_000_000L) return;
		dt = Math.min(0.1f, diff / 1e9f);
		lastFrame = now;
	}

	public static float dt() {
		return dt;
	}

	public static float approach(Object key, float target, float speed) {
		if (!gg.surge.client.mods.Mods.UI_ANIM.on()) { ANIM.put(key, target); return target; }
		speed *= gg.surge.client.mods.Mods.UI_ANIM_SPEED.get() / 100f;
		float cur = ANIM.getOrDefault(key, target);
		cur += (target - cur) * (1f - (float) Math.exp(-dt * speed));
		if (Math.abs(cur - target) < 0.001f) cur = target;
		ANIM.put(key, cur);
		return cur;
	}

	public static float anim(Object key, boolean on) {
		return approach(key, on ? 1f : 0f, 16f);
	}

	public static void resetAnim(Object key, float value) {
		ANIM.put(key, value);
	}

	public static void click() {
		if (!gg.surge.client.mods.Mods.UI_CLICKS.on()) return;
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.55F));
	}

	public static void toggleSound(boolean on) {
		if (!gg.surge.client.mods.Mods.UI_CLICKS.on()) return;
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), on ? 1.25F : 0.85F, 0.5F));
	}

	private static final java.util.Set<Object> HOVERED = new java.util.HashSet<>();
	private static final Map<Object, Long> HOVER_SINCE = new HashMap<>();

	public static long hoverTick(Object key, boolean hov) {
		if (hov) {
			if (HOVERED.add(key)) {
				HOVER_SINCE.put(key, System.currentTimeMillis());
				if (gg.surge.client.mods.Mods.UI_HOVER.on())
					Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.9F, 0.12F));
			}
			return System.currentTimeMillis() - HOVER_SINCE.getOrDefault(key, System.currentTimeMillis());
		}
		HOVERED.remove(key);
		HOVER_SINCE.remove(key);
		return 0;
	}

	public static boolean tile(GuiGraphics g, Font font, int mx, int my, int x, int y, int size, ItemStack icon, String label, boolean labelLeft, float alpha) {
		Object key = "tile:" + label;
		boolean hov = alpha > 0.95f && hover(mx, my, x, y, size, size);
		long held = hoverTick(key, hov);
		float a = anim(key, hov);
		float pop = 1f + 0.08f * a;
		g.pose().pushMatrix();
		g.pose().translate(x + size / 2f, y + size / 2f);
		g.pose().scale(pop, pop);
		g.pose().translate(-(x + size / 2f), -(y + size / 2f));
		g.fill(x, y, x + size, y + size, Colors.alpha(Colors.lerp(0xA00E0E12, 0xD0202026, a), alpha));
		Ui.outline(g, x, y, size, size, Colors.alpha(Colors.lerp(0x28FFFFFF, 0xFFFFFFFF, a), alpha));
		if (a > 0.02f) Ui.outline(g, x - 1, y - 1, size + 2, size + 2, Colors.alpha(0x50FFFFFF, a * alpha));
		int s = Math.min(16, size - 6);
		if (icon == COIN_ITEM) coin(g, x + (size - s) / 2, y + (size - s) / 2, s);
		else item(g, icon, x + (size - s) / 2, y + (size - s) / 2, s);
		g.pose().popMatrix();
		float l = approach(key + ":label", hov && held > 1200 ? 1f : 0f, 12f);
		if (l > 0.01f) {
			int tw = font.width(label) + 10;
			int lx = labelLeft ? x - tw - 6 + Math.round((1 - l) * 6) : x + size + 6 - Math.round((1 - l) * 6);
			int ly = y + (size - 14) / 2;
			g.nextStratum();
			g.fill(lx, ly, lx + tw, ly + 14, Colors.alpha(0xE0101014, l));
			Ui.outline(g, lx, ly, tw, 14, Colors.alpha(0x60FFFFFF, l));
			g.drawString(font, label, lx + 5, ly + 3, Colors.alpha(0xFFFFFFFF, l), false);
		}
		return hov;
	}

	public static boolean hover(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	public static void box(GuiGraphics g, int x, int y, int w, int h, int fill, int edge) {
		g.fill(x, y, x + w, y + h, edge);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
		g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x12FFFFFF);
	}

	public static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y + 1, x + 1, y + h - 1, color);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
	}

	public static void shadow(GuiGraphics g, int x, int y, int w, int h) {
		for (int i = 1; i <= 6; i++) {
			int a = Math.max(0, 0x30 - i * 7);
			outline(g, x - i, y - i + 2, w + i * 2, h + i * 2, a << 24);
		}
	}

	public static void chunky(GuiGraphics g, int x, int y, int w, int h, int fill, int lip) {
		g.fill(x, y, x + w, y + h, EDGE);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, lip);
		g.fill(x + 1, y + 1, x + w - 1, y + h - 3, fill);
		g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x38FFFFFF);
	}

	public static boolean pill(GuiGraphics g, Font font, int mx, int my, int x, int y, int w, int h, String label, boolean active) {
		boolean hov = hover(mx, my, x, y, w, h);
		hoverTick("pill:" + label + "@" + x + "," + y, hov);
		float a = anim("pill:" + label + "@" + x + "," + y, hov);
		int fill = active ? Colors.lerp(ACCENT, ACCENT_HOVER, a) : Colors.lerp(BUTTON, BUTTON_HOVER, a);
		chunky(g, x, y, w, h, fill, active ? ACCENT_DIM : BUTTON_LIP);
		String text = fit(font, label, w - 6);
		g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 9) / 2, TEXT, true);
		return hov;
	}

	public static boolean flat(GuiGraphics g, int mx, int my, int x, int y, int w, int h, boolean selected, Object key) {
		boolean hov = hover(mx, my, x, y, w, h);
		hoverTick(key, hov);
		float a = anim(key, hov || selected);
		if (a > 0.01f) g.fill(x, y, x + w, y + h, Colors.alpha(selected ? Colors.lerp(0xFF2E2E32, ACCENT, 0.35f) : 0xFF2E2E32, a));
		if (selected) g.fill(x, y, x + 2, y + h, ACCENT);
		return hov;
	}

	public static void toggleSwitch(GuiGraphics g, int x, int y, boolean on, Object key) {
		float a = anim(key, on);
		int track = Colors.lerp(0xFF55555A, ACCENT, a);
		g.fill(x, y, x + 22, y + 12, EDGE);
		g.fill(x + 1, y + 1, x + 21, y + 11, track);
		int kx = x + 1 + Math.round(a * 10);
		g.fill(kx, y + 1, kx + 10, y + 11, 0xFFE9E9EC);
		g.fill(kx, y + 9, kx + 10, y + 11, 0xFFB9B9BE);
	}

	public static void slider(GuiGraphics g, int x, int y, int w, float fraction, boolean active) {
		int cy = y + 5;
		g.fill(x, cy - 1, x + w, cy + 2, EDGE);
		g.fill(x + 1, cy, x + w - 1, cy + 1, 0xFF55555A);
		int fx = x + Math.round((w - 1) * Math.max(0, Math.min(1, fraction)));
		g.fill(x + 1, cy, fx, cy + 1, ACCENT_2);
		g.fill(fx - 3, y, fx + 4, y + 11, EDGE);
		g.fill(fx - 2, y + 1, fx + 3, y + 10, active ? 0xFFFFFFFF : 0xFFDDDDE2);
		g.fill(fx - 2, y + 8, fx + 3, y + 10, 0xFFA9A9AE);
	}

	public static String fit(Font font, String s, int maxW) {
		if (font.width(s) <= maxW) return s;
		while (s.length() > 1 && font.width(s + "…") > maxW) s = s.substring(0, s.length() - 1);
		return s + "…";
	}

	public static void centered(GuiGraphics g, Font font, String s, int cx, int y, int color) {
		g.drawString(font, s, cx - font.width(s) / 2, y, color, false);
	}

	public static void centeredShadow(GuiGraphics g, Font font, String s, int cx, int y, int color) {
		g.drawString(font, s, cx - font.width(s) / 2, y, color, true);
	}

	public static void item(GuiGraphics g, ItemStack stack, int x, int y, float size) {
		if (Math.abs(size - 16f) < 0.01f) { g.renderItem(stack, x, y); return; }
		float s = size / 16f;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(s, s);
		g.renderItem(stack, 0, 0);
		g.pose().popMatrix();
	}

	public static void logo(GuiGraphics g, int x, int y, int size) {
		float s = size / 128f;
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(s, s);
		g.blit(RenderPipelines.GUI_TEXTURED, LOGO, 0, 0, 0f, 0f, 128, 128, 128, 128);
		g.pose().popMatrix();
	}

	public static final String[] SHIRT = {"XXX..XXX", "XXXXXXXX", "XXXXXXXX", ".XXXXXX.", ".XXXXXX.", ".XXXXXX.", ".XXXXXX.", ".XXXXXX."};
	public static final String[] GEAR = {"...XX...", ".X.XX.X.", "..XXXX..", "XXX..XXX", "XXX..XXX", "..XXXX..", ".X.XX.X.", "...XX..."};
	public static final String[] COIN = {"..XXXX..", ".XXXXXX.", "XXX..XXX", "XX.XX.XX", "XX.XX.XX", "XXX..XXX", ".XXXXXX.", "..XXXX.."};
	public static final String[] BACK = {"...X....", "..XX....", ".XXXXXXX", "XXXXXXXX", ".XXXXXXX", "..XX....", "...X....", "........"};
	public static final String[] CROSS = {"XX....XX", ".XX..XX.", "..XXXX..", "...XX...", "..XXXX..", ".XX..XX.", "XX....XX", "........"};
	public static final String[] SEARCH = {".XXXX...", "X....X..", "X....X..", "X....X..", ".XXXX...", ".....XX.", "......XX", "........"};
	public static final String[] PLUS = {"...XX...", "...XX...", "...XX...", "XXXXXXXX", "XXXXXXXX", "...XX...", "...XX...", "...XX..."};

	public static void icon(GuiGraphics g, String[] px, int x, int y, int scale, int color) {
		for (int r = 0; r < px.length; r++) {
			for (int col = 0; col < px[r].length(); col++) {
				if (px[r].charAt(col) == 'X') g.fill(x + col * scale, y + r * scale, x + (col + 1) * scale, y + (r + 1) * scale, color);
			}
		}
	}

	public static boolean iconButton(GuiGraphics g, int mx, int my, int x, int y, int size, String[] px) {
		boolean hov = hover(mx, my, x, y, size, size);
		float a = anim("ib@" + x + "," + y, hov);
		chunky(g, x, y, size, size, Colors.lerp(BUTTON, BUTTON_HOVER, a), BUTTON_LIP);
		icon(g, px, x + (size - 8) / 2 + 1, y + (size - 10) / 2 + 1, 1, 0xFF2A2A2A);
		icon(g, px, x + (size - 8) / 2, y + (size - 10) / 2, 1, TEXT);
		return hov;
	}

	public static boolean itemButton(GuiGraphics g, int mx, int my, int x, int y, int size, ItemStack stack) {
		boolean hov = hover(mx, my, x, y, size, size);
		float a = anim("itb@" + x + "," + y, hov);
		chunky(g, x, y, size, size, Colors.lerp(BUTTON, BUTTON_HOVER, a), BUTTON_LIP);
		int s = Math.min(16, size - 6);
		item(g, stack, x + (size - s) / 2, y + (size - s) / 2 - 1, s);
		return hov;
	}

	public static void tooltip(GuiGraphics g, Font font, String text, int mx, int my, int screenW, int screenH) {
		int maxW = 170;
		java.util.List<String> lines = new java.util.ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			String next = line.isEmpty() ? word : line + " " + word;
			if (font.width(next) > maxW && !line.isEmpty()) { lines.add(line.toString()); line = new StringBuilder(word); }
			else line = new StringBuilder(next);
		}
		if (!line.isEmpty()) lines.add(line.toString());
		int w = 0;
		for (String l : lines) w = Math.max(w, font.width(l));
		int h = lines.size() * 10 + 6;
		int x = Math.min(mx + 10, screenW - w - 12);
		int y = Math.min(my + 12, screenH - h - 4);
		g.nextStratum();
		box(g, x, y, w + 10, h, 0xF8141418, ACCENT_DIM);
		for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x + 5, y + 4 + i * 10, TEXT, false);
	}
}
