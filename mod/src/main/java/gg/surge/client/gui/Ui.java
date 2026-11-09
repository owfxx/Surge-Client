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
	public static final int BG = 0xB81A1A1D;
	public static final int SIDEBAR = 0xFF1C1C1F;
	public static final int SURFACE = 0xFF3A3A3D;
	public static final int SURFACE_HOVER = 0xFF48484C;
	public static final int EDGE = 0xFF0C0C0D;
	public static int ACCENT = 0xFF6B6B73;
	public static int ACCENT_HOVER = 0xFF7E7E86;
	public static int ACCENT_DIM = 0xFF404045;
	public static int ACCENT_2 = 0xFFC4C4C9;
	private static String themeKey = "";
	private static boolean glass;
	private static long themeAt;

	private static String style = "Surge Dark";
	private static int screenDepth;
	private static Font uiFont;

	public static String style() {
		glass();
		return style;
	}

	public static boolean surgeStyle() {
		return style().startsWith("Surge");
	}

	public static boolean mcStyle() {
		return style().startsWith("Minecraft");
	}

	public static boolean light() {
		return style().endsWith("Light") && (screenDepth == 0 || lightAllowed);
	}

	private static boolean lightAllowed = true;

	public static void beginScreen(net.minecraft.client.gui.screens.Screen s) {
		if (!s.getClass().getName().startsWith("gg.surge.client.gui.")) return;
		screenDepth++;
		lightAllowed = !(s instanceof HudEditorScreen);
	}

	public static int suspendTheme() {
		int d = screenDepth;
		screenDepth = 0;
		return d;
	}

	public static void resumeTheme(int d) {
		screenDepth = d;
	}

	public static void endScreen() {
		if (screenDepth > 0) screenDepth--;
	}

	public static boolean themed() {
		return screenDepth > 0 && !glass();
	}

	public static Font uiFont() {
		if (uiFont == null) {
			Font.Provider p = new Font.Provider() {
				private Font.Provider base() {
					return ((gg.surge.client.mixin.FontAccessor) Minecraft.getInstance().font).surge$provider();
				}

				@Override
				public net.minecraft.client.gui.GlyphSource glyphs(net.minecraft.network.chat.FontDescription d) {
					if (d == null || d.equals(net.minecraft.network.chat.FontDescription.DEFAULT)) d = UI_FONT;
					return base().glyphs(d);
				}

				@Override
				public net.minecraft.client.gui.font.glyphs.EffectGlyph effect() {
					return base().effect();
				}
			};
			uiFont = new Font(p);
		}
		return uiFont;
	}

	private static final net.minecraft.network.chat.FontDescription UI_FONT = new net.minecraft.network.chat.FontDescription.Resource(Identifier.fromNamespaceAndPath("surge", "ui"));

	public static Font swapFont(Font f) {
		if (!themed() || !surgeStyle()) return null;
		Minecraft mc = Minecraft.getInstance();
		if (f != mc.font) return null;
		return uiFont();
	}

	public static boolean textShadow(boolean shadow) {
		if (!themed()) return shadow;
		if (surgeStyle() || light()) return false;
		return shadow;
	}

	public static int remapText(int c) {
		return themed() && light() ? remapLight(c, true) : c;
	}

	private static boolean keep;

	public static int remap(int c) {
		return !keep && themed() && light() ? remapLight(c, false) : c;
	}

	private static void knob(GuiGraphics g, int x, int y, int d, int color) {
		keep = true;
		try { cut(g, x, y, d, d, d / 2, color); } finally { keep = false; }
	}

	private static int remapLight(int c, boolean text) {
		int a = c >>> 24, r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
		int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
		float sat = mx == 0 ? 0 : (mx - mn) / (float) mx;
		if (sat > 0.3f && mx > 0x50) return c;
		float l = (mx + mn) / 510f;
		if (!text && l < 0.1f && a < 0x60) return c;
		float inv = 1f - l;
		float v = text ? (mcStyle() ? 0.24f + inv * 0.34f : 0.08f + inv * 0.5f) : (mcStyle() ? 0.25f + inv * 0.55f : 0.09f + inv * 0.88f);
		int k = Math.max(0, Math.min(255, Math.round(v * 255)));
		int tb = Math.min(255, k + (mcStyle() ? 0 : 3));
		return (a << 24) | (k << 16) | (k << 8) | tb;
	}

	public static boolean glass() {
		long now = System.currentTimeMillis();
		if (now - themeAt > 200) { themeAt = now; theme(); }
		return glass;
	}

	private static void theme() {
		String t = gg.surge.client.mods.Mods.THEME.get();
		style = gg.surge.client.mods.Mods.UI_STYLE.get();
		String key = t + gg.surge.client.mods.Mods.THEME_COLOR.get();
		if (key.equals(themeKey)) return;
		themeKey = key;
		glass = t.equals("Custom") && "EDEADE".equalsIgnoreCase(gg.surge.client.mods.Mods.THEME_COLOR.get());
		MUTED = glass ? 0xFFDCDCE0 : 0xFFA9AAAE;
		FAINT = glass ? 0xFFB4B4BA : 0xFF6E6F74;
		if (glass) {
			ACCENT = 0xFFEDEADE;
			ACCENT_HOVER = 0xFFFFFFFF;
			ACCENT_DIM = 0xFF8E8C86;
			ACCENT_2 = 0xFFFFFFFF;
			return;
		}
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
	public static int MUTED = 0xFFA9AAAE;
	public static int FAINT = 0xFF6E6F74;
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
		themeAt = System.currentTimeMillis();
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
		if (glass()) { gg.surge.client.features.CustomSounds.preview("preset:glass_tap", 1f, 0.6f); return; }
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F, 0.55F));
	}

	public static void toggleSound(boolean on) {
		if (!gg.surge.client.mods.Mods.UI_CLICKS.on()) return;
		if (glass()) { gg.surge.client.features.CustomSounds.preview(on ? "preset:glass_on" : "preset:glass_off", 1f, 0.6f); return; }
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), on ? 1.25F : 0.85F, 0.5F));
	}

	public static void closeSound() {
		if (!gg.surge.client.mods.Mods.UI_CLICKS.on()) return;
		if (glass()) gg.surge.client.features.CustomSounds.preview("preset:glass_off", 1.1f, 0.4f);
		else gg.surge.client.features.CustomSounds.preview("preset:blip", 0.75f, 0.3f);
	}

	private static long tickAt;

	public static void tickSound(float fraction) {
		if (!gg.surge.client.mods.Mods.UI_CLICKS.on() || System.currentTimeMillis() - tickAt < 45) return;
		tickAt = System.currentTimeMillis();
		if (glass()) gg.surge.client.features.CustomSounds.preview("preset:glass_tap", 0.8f + fraction * 0.6f, 0.25f);
		else Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.4F + fraction * 0.6F, 0.12F));
	}

	public static void openSound(String normal) {
		if (glass()) gg.surge.client.features.CustomSounds.preview("preset:glass_open", 1f, 0.55f);
		else gg.surge.client.features.CustomSounds.preview(normal);
	}

	private static long blurFrame = -1;

	public static boolean glassBackdrop(GuiGraphics g, Runnable blur, int w, int h) {
		if (!glass()) return false;
		long f = Minecraft.getInstance().getFrameTimeNs();
		if (f != blurFrame) {
			blurFrame = f;
			try { blur.run(); } catch (Throwable ignored) { }
		}
		g.fill(0, 0, w, h, 0x4A0E0E12);
		g.fillGradient(0, 0, w, h / 2, 0x10FFFFFF, 0x00FFFFFF);
		return true;
	}

	public static void glassRect(GuiGraphics g, int x, int y, int w, int h, int c, float strength) {
		if (w <= 0 || h <= 0) return;
		c = Math.max(0, Math.min(c, Math.min(w, h) / 2));
		int body = Math.round(0x16 * strength);
		boolean was = rawCut;
		rawCut = true;
		try {
			cut(g, x, y, w, h, c, (Math.min(255, Math.round(0x22 * strength)) << 24) | 0x08080C);
			cut(g, x, y, w, h, c, (Math.min(255, body) << 24) | 0xFFFFFF);
		} finally { rawCut = was; }
		if (h > 6 && w > 2 * c + 2) {
			int sheen = Math.round(0x1C * strength);
			int sy = Math.max(1, c / 2);
			g.fillGradient(x + c, y + sy, x + w - c, y + Math.max(sy + 1, h * 2 / 5), (Math.min(255, sheen) << 24) | 0xFFFFFF, 0x00FFFFFF);
		}
		glassRim(g, x, y, w, h, c, strength);
	}

	private static void glassRim(GuiGraphics g, int x, int y, int w, int h, int c, float strength) {
		c = Math.max(0, Math.min(c, Math.min(w, h) / 2));
		int top = Math.min(255, Math.round(0x8C * strength)), mid = Math.min(255, Math.round(0x3A * strength)), bot = Math.min(255, Math.round(0x22 * strength));
		int it = c > 0 ? inset(c, 0) : 0;
		g.fill(x + it, y, x + w - it, y + 1, (top << 24) | 0xFFFFFF);
		g.fill(x + it, y + h - 1, x + w - it, y + h, (bot << 24) | 0xFFFFFF);
		for (int i = 1; i < c; i++) {
			int in = inset(c, i), prev = inset(c, i - 1);
			int run = Math.max(1, prev - in);
			int at = Math.round(top + (mid - top) * (i / (float) c));
			int ab = Math.round(bot + (mid - bot) * (i / (float) c));
			g.fill(x + in, y + i, x + in + run, y + i + 1, (at << 24) | 0xFFFFFF);
			g.fill(x + w - in - run, y + i, x + w - in, y + i + 1, (at << 24) | 0xFFFFFF);
			g.fill(x + in, y + h - i - 1, x + in + run, y + h - i, (ab << 24) | 0xFFFFFF);
			g.fill(x + w - in - run, y + h - i - 1, x + w - in, y + h - i, (ab << 24) | 0xFFFFFF);
		}
		if (h - 2 * c > 0) {
			g.fillGradient(x, y + c, x + 1, y + h - c, (mid << 24) | 0xFFFFFF, (bot << 24) | 0xFFFFFF);
			g.fillGradient(x + w - 1, y + c, x + w, y + h - c, (mid << 24) | 0xFFFFFF, (bot << 24) | 0xFFFFFF);
		}
	}

	private static int glassTint(int color) {
		int a = (color >>> 24) & 255;
		int r = (color >> 16) & 255, gr = (color >> 8) & 255, b = color & 255;
		float lum = (0.3f * r + 0.59f * gr + 0.11f * b) / 255f;
		int mx = Math.max(r, Math.max(gr, b)), mn = Math.min(r, Math.min(gr, b));
		if (mx > 0x30 && (mx - mn) / (float) mx > 0.35f) return (Math.round(a * 0.6f) << 24) | (Colors.lerp(color | 0xFF000000, 0xFFFFFFFF, 0.25f) & 0xFFFFFF);
		if (lum < 0.4f) return (Math.round(a * (0.07f + lum * 0.25f)) << 24) | 0xFFFFFF;
		return (Math.round(a * 0.7f) << 24) | (color & 0xFFFFFF);
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
				if (gg.surge.client.mods.Mods.UI_HOVER.on()) {
					if (glass()) gg.surge.client.features.CustomSounds.preview("preset:glass_hover", 1f, 0.35f);
					else Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.9F, 0.12F));
				}
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

	private static final Map<String, Identifier> MOD_ICONS = new HashMap<>();

	public static void modIcon(GuiGraphics g, gg.surge.client.mods.SurgeMod m, int x, int y, int size, int color) {
		if (!MOD_ICONS.containsKey(m.id)) {
			Identifier t = Identifier.fromNamespaceAndPath("surge", "textures/gui/modicons/" + m.id + ".png");
			boolean ok;
			try { ok = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(t).isPresent(); } catch (Exception e) { ok = false; }
			MOD_ICONS.put(m.id, ok ? t : null);
		}
		Identifier tex = MOD_ICONS.get(m.id);
		if (tex != null) g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, size, size, 64, 64, 64, 64, remapText(color));
		else item(g, m.icon(), x, y, size);
	}

	public static void iconTex(GuiGraphics g, Identifier tex, int x, int y, int size, float alpha) {
		g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0f, 0f, size, size, 32, 32, 32, 32, remapText(Colors.alpha(0xFFFFFFFF, alpha)));
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

	private static int inset(int c, int i) {
		double dy = c - i - 0.5;
		return (int) Math.round(c - Math.sqrt(Math.max(0, c * c - dy * dy)));
	}

	public static void cut(GuiGraphics g, int x, int y, int w, int h, int c, int color) {
		if (glass && !rawCut) color = glassTint(color);
		if (!glass && mcStyle()) c = 0;
		else if (!glass && c > 0) c = c + 1;
		c = Math.max(0, Math.min(c, Math.min(w, h) / 2));
		if (glass && c > 0) {
			for (int i = 0; i < c; i++) {
				int in = inset(c, i);
				g.fill(x + in, y + i, x + w - in, y + i + 1, color);
				g.fill(x + in, y + h - i - 1, x + w - in, y + h - i, color);
			}
			g.fill(x, y + c, x + w, y + h - c, color);
			return;
		}
		for (int i = 0; i < c; i++) {
			g.fill(x + c - i, y + i, x + w - c + i, y + i + 1, color);
			g.fill(x + c - i, y + h - i - 1, x + w - c + i, y + h - i, color);
		}
		g.fill(x, y + c, x + w, y + h - c, color);
	}

	private static boolean rawCut;

	public static void cutBox(GuiGraphics g, int x, int y, int w, int h, int c, int fill, int edge) {
		if (glass()) {
			int fa = (fill >>> 24) & 0xFF;
			rawCut = true;
			try { glassRect(g, x, y, w, h, Math.max(c, Math.min(6, Math.min(w, h) / 3)), fa < 0x40 ? 0.6f : 1f); } finally { rawCut = false; }
			int r = (fill >> 16) & 255, gr = (fill >> 8) & 255, b = fill & 255;
			int mx = Math.max(r, Math.max(gr, b)), mn = Math.min(r, Math.min(gr, b));
			boolean colored = mx > 0x30 && (mx - mn) / (float) mx > 0.35f;
			if (fa > 0x40 && ((r + gr + b) / 3 > 0x70 || colored)) { rawCut = true; try { cut(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, Math.max(c, Math.min(6, Math.min(w, h) / 3)) - 1), ((colored ? 0x78 : 0x50) << 24) | (fill & 0xFFFFFF)); } finally { rawCut = false; } }
			return;
		}
		int a = (fill >>> 24) & 0xFF;
		int ea = Math.max(a, (edge >>> 24) & 0xFF);
		if (mcStyle()) {
			g.fill(x, y, x + w, y + h, (ea << 24));
			g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
			if (w >= 4 && h >= 4) {
				int hi = ((a * 0x50 / 255) << 24) | 0xFFFFFF, lo = ((a * 0x70 / 255) << 24);
				g.fill(x + 1, y + 1, x + w - 1, y + 2, hi);
				g.fill(x + 1, y + 2, x + 2, y + h - 1, hi);
				g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, lo);
				g.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, lo);
			}
			return;
		}
		if (surgeStyle() && !glass) {
			boolean bright = ((edge >>> 24) & 0xFF) > 0x60 && (edge & 0xFFFFFF) > 0x606060;
			int border = bright ? edge : (a << 24) | (Colors.lerp(fill | 0xFF000000, 0xFFFFFFFF, 0.07f) & 0xFFFFFF);
			cut(g, x, y, w, h, c, border);
			cut(g, x + 1, y + 1, w - 2, h - 2, Math.max(0, c - 1), fill);
			return;
		}
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
		if (glass()) {
			glassRect(g, x, y, w, h, Math.min(6, Math.min(w, h) / 2), (disabled ? 0.5f : hov ? 1.6f : 1.1f) * alpha);
			return;
		}
		if (surgeStyle()) {
			int fill = disabled ? 0xFF26262A : hov ? 0xFF4A4A50 : 0xFF333338;
			cutBox(g, x, y, w, h, 3, Colors.alpha(fill, alpha), Colors.alpha(hov ? 0xFF6A6A72 : EDGE, alpha));
			return;
		}
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
		if (glass()) {
			if (a > 0.01f) glassRect(g, x, y, w, h, 4, (selected ? 1.2f : 0.7f) * a);
			return hov;
		}
		if (a > 0.01f) cut(g, x, y, w, h, 3, Colors.alpha(selected ? 0xFF2A2A2F : 0xFF222226, a));
		if (selected) g.fill(x, y + 4, x + 2, y + h - 4, ACCENT_2);
		return hov;
	}

	public static void toggleSwitch(GuiGraphics g, int x, int y, boolean on, Object key) {
		float a = anim(key, on);
		if (glass()) {
			rawCut = true;
			try {
				cut(g, x, y, 22, 12, 6, Colors.lerp(0x40FFFFFF, 0xE634C759, a));
				glassRim(g, x, y, 22, 12, 6, 0.9f);
				int kx = x + 1 + Math.round(a * 10);
				cut(g, kx, y + 1, 10, 10, 5, 0xFFFFFFFF);
				g.fill(kx + 2, y + 10, kx + 8, y + 11, 0x22000000);
			} finally { rawCut = false; }
			return;
		}
		if (surgeStyle()) {
			rawCut = true;
			try {
				cut(g, x, y, 22, 12, 6, Colors.lerp(0xFF3A3A3F, 0xFF34C759, a));
				knob(g, x + 1 + Math.round(a * 10), y + 1, 10, 0xFFFFFFFF);
			} finally { rawCut = false; }
			return;
		}
		g.fill(x, y, x + 22, y + 12, 0xFF000000);
		g.fill(x + 1, y + 1, x + 21, y + 11, Colors.lerp(0xFF8B2B2B, 0xFF3C8527, a));
		g.fill(x + 1, y + 1, x + 21, y + 2, 0x40000000);
		int kx = x + Math.round(a * 14);
		g.blitSprite(RenderPipelines.GUI_TEXTURED, HANDLE_SPRITE, kx, y, 8, 12);
	}

	public static void slider(GuiGraphics g, int x, int y, int w, float fraction, boolean active) {
		if (glass()) {
			float f = Math.max(0, Math.min(1, fraction));
			rawCut = true;
			try {
				cut(g, x, y + 4, w, 4, 2, 0x38FFFFFF);
				cut(g, x, y + 4, Math.max(4, Math.round(w * f)), 4, 2, 0xE6FFFFFF);
				int kx = x + Math.round((w - 10) * f);
				cut(g, kx, y + 1, 10, 10, 5, active ? 0xFFFFFFFF : 0xFFF4F4F6);
				g.fill(kx + 2, y + 10, kx + 8, y + 11, 0x26000000);
			} finally { rawCut = false; }
			return;
		}
		if (surgeStyle()) {
			float f = Math.max(0, Math.min(1, fraction));
			rawCut = true;
			try {
				cut(g, x, y + 4, w, 4, 2, 0xFF3A3A3F);
				cut(g, x, y + 4, Math.max(4, Math.round(w * f)), 4, 2, 0xFF34C759);
				knob(g, x + Math.round((w - 10) * f), y + 1, 10, active ? 0xFFFFFFFF : 0xFFE8E8EC);
			} finally { rawCut = false; }
			return;
		}
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
		if (glass()) {
			rawCut = true;
			try { cut(g, x, y, w, h, 4, 0xB4141418); } finally { rawCut = false; }
			glassRect(g, x, y, w, h, 4, 1f);
			return;
		}
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
