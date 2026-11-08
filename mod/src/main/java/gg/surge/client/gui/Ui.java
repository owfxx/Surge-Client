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
import java.util.List;
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

	public static final net.minecraft.network.chat.FontDescription TEN = new net.minecraft.network.chat.FontDescription.Resource(Identifier.fromNamespaceAndPath("surge", "ten"));

	public static net.minecraft.network.chat.MutableComponent ten(String text) {
		return net.minecraft.network.chat.Component.literal(text).withStyle(st -> st.withFont(TEN));
	}

	public static void heading(GuiGraphics g, Font font, String text, int x, int y, int color, float scale, boolean shadow) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(scale, scale);
		g.drawString(font, ten(text), 0, 0, color, shadow);
		g.pose().popMatrix();
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

	private static final java.util.List<Runnable> LABELS = new java.util.ArrayList<>();

	public static void flushLabels() {
		if (LABELS.isEmpty()) return;
		List<Runnable> copy = new java.util.ArrayList<>(LABELS);
		LABELS.clear();
		for (Runnable r : copy) r.run();
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

	public static final ItemStack IT_SOCIAL = new ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
	public static final ItemStack IT_SETTINGS = new ItemStack(net.minecraft.world.item.Items.COMPARATOR);
	public static final ItemStack IT_WARDROBE = new ItemStack(net.minecraft.world.item.Items.LEATHER_CHESTPLATE);
	public static final ItemStack IT_MODS = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
	public static final ItemStack IT_COINS = new ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET);
	public static final String[] I_SOCIAL = {"XXXXXXXX", "XXXXXXXX", "XX.XX.XX", "XXXXXXXX", "XXXXXXXX", ".XX.....", ".X......", "........"};
	public static final String[] I_MODS = {"..X.....", "XXXXXXXX", "..X.....", "......X.", "XXXXXXXX", "......X.", "...X....", "XXXXXXXX"};
	public static final String[] I_GEAR = {"..X..X..", ".XXXXXX.", "XXX..XXX", ".X....X.", ".X....X.", "XXX..XXX", ".XXXXXX.", "..X..X.."};
	public static final String[] I_SHIRT = {"XXX..XXX", "XXXXXXXX", "XXXXXXXX", ".XXXXXX.", ".XXXXXX.", ".XXXXXX.", ".XXXXXX.", ".XXXXXX."};

	public static boolean tile(GuiGraphics g, Font font, int mx, int my, int x, int y, int size, String[] px, String label, boolean labelLeft, float alpha) {
		return tileCore(g, font, mx, my, x, y, size, () -> {
			int sc = size >= 22 ? 2 : 1;
			int iw = px[0].length() * sc, ih = px.length * sc;
			icon(g, px, x + (size - iw) / 2, y + (size - ih) / 2, sc, Colors.alpha(0xFFFFFFFF, alpha));
		}, label, labelLeft, alpha);
	}

	public static final Identifier IC_SOCIAL = Identifier.fromNamespaceAndPath("surge", "textures/gui/icon_social_32.png");
	public static final Identifier IC_SETTINGS = Identifier.fromNamespaceAndPath("surge", "textures/gui/icon_settings_32.png");
	public static final Identifier IC_WARDROBE = Identifier.fromNamespaceAndPath("surge", "textures/gui/icon_wardrobe_32.png");
	public static final Identifier IC_MODS = Identifier.fromNamespaceAndPath("surge", "textures/gui/icon_mods_32.png");
	public static final Identifier IC_DISCORD = Identifier.fromNamespaceAndPath("surge", "textures/gui/icon_discord_32.png");
	public static final String DISCORD = "https://discord.gg/7wGxqGN86R";

	public static void iconTex(GuiGraphics g, Identifier tex, int x, int y, int size, float alpha) {
		g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, size, size, 32, 32, 32, 32, Colors.alpha(0xFFFFFFFF, alpha));
	}

	public static boolean tile(GuiGraphics g, Font font, int mx, int my, int x, int y, int size, Identifier icon, String label, boolean labelLeft, float alpha) {
		return tileCore(g, font, mx, my, x, y, size, () -> iconTex(g, icon, x + (size - 16) / 2, y + (size - 16) / 2, 16, alpha), label, labelLeft, alpha);
	}

	public static boolean tile(GuiGraphics g, Font font, int mx, int my, int x, int y, int size, ItemStack icon, String label, boolean labelLeft, float alpha) {
		return tileCore(g, font, mx, my, x, y, size, () -> {
			int s = Math.min(16, size - 6);
			if (icon == COIN_ITEM) coin(g, x + (size - s) / 2, y + (size - s) / 2, s);
			else item(g, icon, x + (size - s) / 2, y + (size - s) / 2, s);
		}, label, labelLeft, alpha);
	}

	private static boolean tileCore(GuiGraphics g, Font font, int mx, int my, int x, int y, int size, Runnable drawIcon, String label, boolean labelLeft, float alpha) {
		Object key = "tile:" + label;
		boolean hov = alpha > 0.95f && hover(mx, my, x, y, size, size);
		long held = hoverTick(key, hov);
		float a = anim(key, hov);
		float pop = 1f + 0.08f * a;
		g.pose().pushMatrix();
		g.pose().translate(x + size / 2f, y + size / 2f);
		g.pose().scale(pop, pop);
		g.pose().translate(-(x + size / 2f), -(y + size / 2f));
		mcButton(g, x, y, size, size, hov, false, alpha);
		drawIcon.run();
		g.pose().popMatrix();
		float l = approach(key + ":label", hov && held > 350 ? 1f : 0f, 14f);
		if (l > 0.01f) LABELS.add(() -> {
			int tw = font.width(label) + 10;
			boolean below = labelLeft && y < 40;
			int lx = below ? Math.min(x + size - tw, x + (size - tw) / 2) : labelLeft ? x - tw - 6 + Math.round((1 - l) * 6) : x + size + 6 - Math.round((1 - l) * 6);
			int ly = below ? y + size + 5 - Math.round((1 - l) * 4) : y + (size - 14) / 2;
			g.nextStratum();
			vanillaTooltip(g, lx, ly, tw, 14);
			g.drawString(font, label, lx + 5, ly + 3, Colors.alpha(0xFFFFFFFF, l), true);
		});
		return hov;
	}

	public static boolean hover(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	public static void box(GuiGraphics g, int x, int y, int w, int h, int fill, int edge) {
		cutBox(g, x, y, w, h, 3, fill, edge == EDGE ? 0xFF2E2E33 : edge);
	}

	public static void cut(GuiGraphics g, int x, int y, int w, int h, int c, int color) {
		c = Math.max(0, Math.min(c, Math.min(w, h) / 2));
		for (int i = 0; i < c; i++) {
			g.fill(x + c - i, y + i, x + w - c + i, y + i + 1, color);
			g.fill(x + c - i, y + h - i - 1, x + w - c + i, y + h - i, color);
		}
		g.fill(x, y + c, x + w, y + h - c, color);
	}

	public static void cutBox(GuiGraphics g, int x, int y, int w, int h, int c, int fill, int edge) {
		int a = (fill >>> 24) & 0xFF;
		int ea = Math.max(a, (edge >>> 24) & 0xFF);
		g.fill(x, y, x + w, y + h, (ea << 24));
		g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
		if (w >= 6 && h >= 6 && a > 0x40) {
			g.fill(x + 1, y + 1, x + w - 1, y + 2, ((a * 0x30 / 255) << 24) | 0xFFFFFF);
			g.fill(x + 1, y + 2, x + 2, y + h - 1, ((a * 0x30 / 255) << 24) | 0xFFFFFF);
			g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, ((a * 0x60 / 255) << 24));
			g.fill(x + w - 2, y + 1, x + w - 1, y + h - 2, ((a * 0x60 / 255) << 24));
		}
		if (((edge >>> 24) & 0xFF) > 0x60 && (edge & 0xFFFFFF) > 0x606060) {
			g.fill(x, y, x + w, y + 1, edge);
			g.fill(x, y + h - 1, x + w, y + h, edge);
			g.fill(x, y + 1, x + 1, y + h - 1, edge);
			g.fill(x + w - 1, y + 1, x + w, y + h - 1, edge);
		}
	}

	public static final Identifier BUTTON_SPRITE = Identifier.withDefaultNamespace("widget/button");
	public static final Identifier BUTTON_HOVER_SPRITE = Identifier.withDefaultNamespace("widget/button_highlighted");
	public static final Identifier BUTTON_OFF_SPRITE = Identifier.withDefaultNamespace("widget/button_disabled");
	public static final Identifier SLIDER_SPRITE = Identifier.withDefaultNamespace("widget/slider");
	public static final Identifier HANDLE_SPRITE = Identifier.withDefaultNamespace("widget/slider_handle");
	public static final Identifier HANDLE_HOVER_SPRITE = Identifier.withDefaultNamespace("widget/slider_handle_highlighted");

	public static void mcButton(GuiGraphics g, int x, int y, int w, int h, boolean hov, boolean disabled, float alpha) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, disabled ? BUTTON_OFF_SPRITE : hov ? BUTTON_HOVER_SPRITE : BUTTON_SPRITE, x, y, w, h, alpha);
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
		cutBox(g, x, y, w, h, 3, fill, Colors.lerp(fill, 0xFFFFFFFF, 0.12f));
	}

	public static boolean pill(GuiGraphics g, Font font, int mx, int my, int x, int y, int w, int h, String label, boolean active) {
		boolean hov = hover(mx, my, x, y, w, h);
		hoverTick("pill:" + label + "@" + x + "," + y, hov);
		mcButton(g, x, y, w, h, hov || active, false, 1f);
		if (active) g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, 0xFFFFFF55);
		String text = fit(font, label, w - 6);
		g.drawString(font, text, x + (w - font.width(text)) / 2, y + (h - 8) / 2, active ? 0xFFFFFF55 : hov ? 0xFFFFFFA0 : TEXT, true);
		return hov;
	}

	public static boolean flat(GuiGraphics g, int mx, int my, int x, int y, int w, int h, boolean selected, Object key) {
		boolean hov = hover(mx, my, x, y, w, h);
		hoverTick(key, hov);
		float a = anim(key, hov || selected);
		if (a > 0.01f) cut(g, x, y, w, h, 3, Colors.alpha(selected ? 0xFF2A2A2F : 0xFF222226, a));
		if (selected) g.fill(x, y + 4, x + 2, y + h - 4, ACCENT_2);
		return hov;
	}

	public static void toggleSwitch(GuiGraphics g, int x, int y, boolean on, Object key) {
		float a = anim(key, on);
		g.fill(x, y, x + 22, y + 12, 0xFF000000);
		g.fill(x + 1, y + 1, x + 21, y + 11, Colors.lerp(0xFF8B2B2B, 0xFF3C8527, a));
		g.fill(x + 1, y + 1, x + 21, y + 2, 0x40000000);
		int kx = x + Math.round(a * 14);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, HANDLE_SPRITE, kx, y, 8, 12);
	}

	public static void slider(GuiGraphics g, int x, int y, int w, float fraction, boolean active) {
		g.blitSprite(RenderPipelines.GUI_TEXTURED, SLIDER_SPRITE, x, y + 2, w, 8);
		int fx = x + Math.round((w - 6) * Math.max(0, Math.min(1, fraction)));
		g.blitSprite(RenderPipelines.GUI_TEXTURED, active ? HANDLE_HOVER_SPRITE : HANDLE_SPRITE, fx, y, 6, 12);
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
		mcButton(g, x, y, size, size, hov, false, 1f);
		icon(g, px, x + (size - 8) / 2 + 1, y + (size - 8) / 2 + 1, 1, 0xFF3F3F3F);
		icon(g, px, x + (size - 8) / 2, y + (size - 8) / 2, 1, hov ? 0xFFFFFFA0 : TEXT);
		return hov;
	}

	public static boolean itemButton(GuiGraphics g, int mx, int my, int x, int y, int size, ItemStack stack) {
		boolean hov = hover(mx, my, x, y, size, size);
		mcButton(g, x, y, size, size, hov, false, 1f);
		int s = Math.min(16, size - 6);
		item(g, stack, x + (size - s) / 2, y + (size - s) / 2, s);
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
		vanillaTooltip(g, x, y, w + 10, h);
		for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x + 5, y + 4 + i * 10, TEXT, true);
	}

	public static void vanillaTooltip(GuiGraphics g, int x, int y, int w, int h) {
		int bg = 0xF0100010;
		g.fill(x + 1, y, x + w - 1, y + h, bg);
		g.fill(x, y + 1, x + 1, y + h - 1, bg);
		g.fill(x + w - 1, y + 1, x + w, y + h - 1, bg);
		g.fillGradient(x + 1, y + 1, x + 2, y + h - 1, 0x505000FF, 0x5028007F);
		g.fillGradient(x + w - 2, y + 1, x + w - 1, y + h - 1, 0x505000FF, 0x5028007F);
		g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x505000FF);
		g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x5028007F);
	}
}
