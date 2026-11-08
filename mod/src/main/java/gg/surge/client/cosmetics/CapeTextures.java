package gg.surge.client.cosmetics;

import com.mojang.blaze3d.platform.NativeImage;
import gg.surge.client.config.SurgeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CapeTextures {
	public static final List<String> TEXTS = List.of("beta_tester", "beta", "early", "surge", "og", "none");
	public static final List<String> TEXT_NAMES = List.of("Beta Tester", "Beta", "Early Access", "Surge", "OG", "No text");
	public static final List<String> PALETTE = List.of(
		"141418", "FFFFFF", "A1A1AA", "52525B", "8F50F2", "B483FF", "EC68B0", "F43F5E",
		"E2363F", "F0842A", "F2C430", "78CE46", "1C965A", "22BED4", "3468E8", "1E3A8A");

	private static final Map<String, Identifier> CACHE = new HashMap<>();
	private static final Map<String, NativeImage> SOURCES = new HashMap<>();

	private CapeTextures() {}

	public static String resolve(String style) {
		if (style == null) return null;
		if (style.equals("custom")) {
			var c = SurgeConfig.get();
			return "custom_" + c.capeColor.toLowerCase(Locale.ROOT) + (c.capeGradient ? "-" + c.capeColor2.toLowerCase(Locale.ROOT) : "");
		}
		if (style.equals("beta")) {
			var b = SurgeConfig.get().betaCape;
			return "beta2_" + b.color.toLowerCase(Locale.ROOT) + (b.gradient ? "-" + b.color2.toLowerCase(Locale.ROOT) : "") + "_" + b.logo.toLowerCase(Locale.ROOT) + "_" + b.text;
		}
		if (style.startsWith("beta_")) {
			String[] p = style.split("_");
			return "beta2_" + (p.length > 2 ? p[2] : "141418") + "_ffffff_beta_tester";
		}
		if (style.startsWith("classic_")) return "custom_8f50f2";
		if (style.equals("image")) return "image_" + gg.surge.client.net.Social.myId();
		return style;
	}

	private static final Map<String, int[]> ANIM = Map.ofEntries(
		Map.entry("flame", new int[]{16, 70}),
		Map.entry("enchanted", new int[]{16, 90}),
		Map.entry("runes", new int[]{24, 90}),
		Map.entry("blinkcat", new int[]{2, 0}),
		Map.entry("aurora", new int[]{16, 140}),
		Map.entry("emerald", new int[]{16, 110}),
		Map.entry("galaxy", new int[]{24, 120}),
		Map.entry("midnight", new int[]{20, 160}),
		Map.entry("ocean", new int[]{16, 120}),
		Map.entry("royal", new int[]{16, 110}),
		Map.entry("sakura", new int[]{20, 130}),
		Map.entry("surge", new int[]{16, 110}),
		Map.entry("void", new int[]{16, 120}),
		Map.entry("lava", new int[]{20, 110}),
		Map.entry("matrix", new int[]{20, 80}),
		Map.entry("storm", new int[]{24, 110}),
		Map.entry("phoenix", new int[]{20, 90}),
		Map.entry("koi", new int[]{24, 120}),
		Map.entry("bloodmoon", new int[]{20, 120}),
		Map.entry("obsidian", new int[]{16, 110}),
		Map.entry("frost", new int[]{16, 130}),);

	public static boolean plainElytra(String style) {
		String k = resolve(style);
		return k != null && (k.equals("blinkcat") || k.startsWith("image_"));
	}

	private static int blink(long now) {
		long cycle = now / 11000, in = now % 11000;
		long h = cycle * 0x9E3779B97F4A7C15L;
		h ^= (h >>> 29);
		long at = 1500 + Math.floorMod(h, 7500);
		long d = in - at;
		if (d >= 0 && d < 70) return 1;
		if (d >= 70 && d < 200) return 2;
		if (d >= 200 && d < 270) return 1;
		if ((h & 3) == 0 && d >= 420 && d < 640) return d < 470 || d >= 590 ? 1 : 2;
		return 0;
	}

	private static int frame(int[] spec) {
		long now = System.currentTimeMillis();
		if (spec[1] == 0) return Math.min(spec[0] - 1, blink(now));
		if (spec.length == 2) return (int) ((now / spec[1]) % spec[0]);
		int total = 0;
		for (int i = 1; i < spec.length; i++) total += spec[i];
		long t = now % total;
		for (int i = 1; i < spec.length; i++) { t -= spec[i]; if (t < 0) return i - 1; }
		return 0;
	}

	private static final Map<String, Anim> ANIMS = new HashMap<>();

	private static final class Anim {
		final Identifier id;
		final DynamicTexture tex;
		final NativeImage[] frames;
		int shown = -1;

		Anim(Identifier id, DynamicTexture tex, int n) {
			this.id = id;
			this.tex = tex;
			this.frames = new NativeImage[n];
		}
	}

	public static Identifier get(String style) {
		String key = resolve(style);
		if (key == null) return null;
		if (key.startsWith("image_")) return ImageCapes.get(key.substring(6));
		int[] spec = ANIM.get(key);
		if (spec != null) return animated(key, spec);
		Identifier id = CACHE.get(key);
		if (id != null) return id;
		NativeImage img;
		try {
			img = build(key);
		} catch (Throwable t) {
			img = null;
		}
		if (img == null) return Identifier.fromNamespaceAndPath("surge", "textures/cosmetic/cape_surge.png");
		id = Identifier.fromNamespaceAndPath("surge", "capes/" + Integer.toHexString(key.hashCode()));
		Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "surge cape", img));
		if (CACHE.size() > 160) {
			for (Identifier old : CACHE.values()) Minecraft.getInstance().getTextureManager().release(old);
			CACHE.clear();
		}
		CACHE.put(key, id);
		return id;
	}

	private static Identifier animated(String key, int[] spec) {
		int f = frame(spec);
		Anim a = ANIMS.get(key);
		try {
			if (a == null) {
				NativeImage first = build(key + "#" + f);
				if (first == null) return Identifier.fromNamespaceAndPath("surge", "textures/cosmetic/cape_surge.png");
				Identifier id = Identifier.fromNamespaceAndPath("surge", "capes/anim_" + key);
				NativeImage pixels = new NativeImage(first.getWidth(), first.getHeight(), true);
				pixels.copyFrom(first);
				DynamicTexture tex = new DynamicTexture(() -> "surge cape " + key, pixels);
				Minecraft.getInstance().getTextureManager().register(id, tex);
				a = new Anim(id, tex, spec[0]);
				a.frames[f] = first;
				a.shown = f;
				ANIMS.put(key, a);
				return id;
			}
			if (a.shown != f) {
				if (a.frames[f] == null) a.frames[f] = build(key + "#" + f);
				if (a.frames[f] != null && a.tex.getPixels() != null) {
					a.tex.getPixels().copyFrom(a.frames[f]);
					a.tex.upload();
				}
				a.shown = f;
			}
		} catch (Throwable ignored) { }
		return a == null ? Identifier.fromNamespaceAndPath("surge", "textures/cosmetic/cape_surge.png") : a.id;
	}

	private static int hex(String s, int def) {
		try { return 0xFF000000 | Integer.parseInt(s, 16); } catch (Exception e) { return def; }
	}

	private static NativeImage source(String path) {
		NativeImage n = SOURCES.get(path);
		if (n != null) return n;
		try (InputStream in = Minecraft.getInstance().getResourceManager().getResource(Identifier.fromNamespaceAndPath("surge", path)).orElseThrow().open()) {
			n = NativeImage.read(in);
			SOURCES.put(path, n);
			return n;
		} catch (Exception e) {
			return null;
		}
	}

	private static void overlay(NativeImage dst, NativeImage src, int tint) {
		if (src == null) return;
		int sw = src.getWidth(), sh = src.getHeight(), w = dst.getWidth(), h = dst.getHeight();
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
			int s = src.getPixel(x * sw / w, y * sh / h);
			int a = (s >>> 24) & 255;
			if (a == 0) continue;
			int d = dst.getPixel(x, y);
			int r = (((tint >> 16) & 255) * a + ((d >> 16) & 255) * (255 - a)) / 255;
			int g = (((tint >> 8) & 255) * a + ((d >> 8) & 255) * (255 - a)) / 255;
			int b = ((tint & 255) * a + (d & 255) * (255 - a)) / 255;
			dst.setPixel(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
		}
	}

	private static int lerp(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
		int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
		int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
		return 0xFF000000 | (r << 16) | (g << 8) | bl;
	}

	private static void fill(NativeImage img, String spec) {
		String[] parts = spec.split("-");
		int top = hex(parts[0], 0xFF8F50F2), bottom = parts.length > 1 ? hex(parts[1], top) : top;
		int w = img.getWidth(), h = img.getHeight(), s = w / 64;
		for (int y = 0; y < h; y++) {
			float t = parts.length > 1 ? Math.max(0f, Math.min(1f, (y / (float) s - 1f) / 16f)) : 0f;
			if (y >= 22 * s) t = 0.5f;
			int c = lerp(top, bottom, t);
			for (int x = 0; x < w; x++) {
				if (x >= 22 * s && x < 46 * s && y < 22 * s) {
					float te = parts.length > 1 ? Math.max(0f, Math.min(1f, (y / (float) s - 2f) / 20f)) : 0f;
					img.setPixel(x, y, lerp(top, bottom, te));
				} else img.setPixel(x, y, c);
			}
		}
	}

	private static NativeImage sga;

	private static NativeImage sga() {
		if (sga != null) return sga;
		try (InputStream in = Minecraft.getInstance().getResourceManager().getResource(Identifier.withDefaultNamespace("textures/font/ascii_sga.png")).orElseThrow().open()) {
			sga = NativeImage.read(in);
		} catch (Exception e) {
			sga = null;
		}
		return sga;
	}

	private static void glyph(NativeImage dst, NativeImage font, char c, int x, int y, int px, int color, float alpha) {
		int cell = font.getWidth() / 16;
		int gx = (c % 16) * cell, gy = (c / 16) * cell;
		for (int j = 0; j < cell; j++) for (int i = 0; i < cell; i++) {
			int a = (font.getPixel(gx + i, gy + j) >>> 24) & 255;
			if (a < 128) continue;
			for (int yy = 0; yy < px; yy++) for (int xx = 0; xx < px; xx++) {
				int dx = x + i * px + xx, dy = y + j * px + yy;
				if (dx < 0 || dy < 0 || dx >= dst.getWidth() || dy >= dst.getHeight()) continue;
				int d = dst.getPixel(dx, dy);
				int r = Math.round(((d >> 16) & 255) * (1 - alpha) + ((color >> 16) & 255) * alpha);
				int g = Math.round(((d >> 8) & 255) * (1 - alpha) + ((color >> 8) & 255) * alpha);
				int b = Math.round((d & 255) * (1 - alpha) + (color & 255) * alpha);
				dst.setPixel(dx, dy, 0xFF000000 | (r << 16) | (g << 8) | b);
			}
		}
	}

	private static void runeFace(NativeImage img, NativeImage font, int fx, int fy, int fw, int fh, int cols, int rows, int px, int frame, int seed, float dim) {
		int cw = fw / cols, ch = fh / rows, cell = font.getWidth() / 16;
		for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
			int id = seed + r * 31 + c * 7;
			int change = (frame + (id * 5) % 24) / 6;
			long h = (id * 0x9E3779B97F4A7C15L) ^ (change * 0xBF58476D1CE4E5B9L);
			h ^= h >>> 31;
			char ch2 = (char) ('a' + Math.floorMod(h, 26));
			float wave = 0.5f + 0.5f * (float) Math.cos(2 * Math.PI * (frame / 24.0 - r / (double) rows));
			int x = fx + c * cw + (cw - cell * px) / 2 + px, y = fy + r * ch + (ch - cell * px) / 2 + px / 2;
			glyph(img, font, ch2, x + 1, y + 1, px, 0xFF000000, 0.5f * dim);
			glyph(img, font, ch2, x, y, px, 0xFFFFFFFF, (0.45f + 0.55f * wave) * dim);
		}
	}

	private static NativeImage runes(int frame) {
		NativeImage font = sga();
		if (font == null) return null;
		int w = 512, h = 256, s = 8;
		NativeImage img = new NativeImage(w, h, true);
		fill(img, "111114");
		runeFace(img, font, s, s, 10 * s, 16 * s, 5, 8, 2, frame, 0, 1f);
		runeFace(img, font, 12 * s, s, 10 * s, 16 * s, 5, 8, 2, frame, 97, 0.35f);
		runeFace(img, font, 24 * s, 2 * s, 10 * s, 20 * s, 5, 10, 2, frame, 13, 1f);
		runeFace(img, font, 36 * s, 2 * s, 10 * s, 20 * s, 5, 10, 2, frame, 13, 0.5f);
		return img;
	}

	private static int sample(NativeImage src, float x, float y) {
		int w = src.getWidth(), h = src.getHeight();
		x = Math.max(0, Math.min(w - 1.001f, x - 0.5f));
		y = Math.max(0, Math.min(h - 1.001f, y - 0.5f));
		int x0 = (int) x, y0 = (int) y;
		float fx = x - x0, fy = y - y0;
		int a = src.getPixel(x0, y0), b = src.getPixel(x0 + 1, y0), c = src.getPixel(x0, y0 + 1), d = src.getPixel(x0 + 1, y0 + 1);
		return lerp(lerp(a, b, fx), lerp(c, d, fx), fy);
	}

	private static int shade(int c, float k) {
		int r = Math.round(((c >> 16) & 255) * k), g = Math.round(((c >> 8) & 255) * k), b = Math.round((c & 255) * k);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private static NativeImage build(String key) {
		int w = 512, h = 256;
		if (key.startsWith("custom_")) {
			NativeImage img = new NativeImage(w, h, true);
			fill(img, key.substring(7));
			return img;
		}
		if (key.startsWith("beta2_")) {
			String[] p = key.split("_", 4);
			NativeImage img = new NativeImage(w, h, true);
			String base = p.length > 1 ? p[1] : "141418";
			int tint = hex(p.length > 2 ? p[2] : "ffffff", 0xFFFFFFFF);
			String text = p.length > 3 ? p[3] : "beta_tester";
			fill(img, base);
			boolean hasText = TEXTS.contains(text) && !text.equals("none");
			overlay(img, source(hasText ? "textures/cosmetic/cape_overlay_logo.png" : "textures/cosmetic/cape_overlay_logo_center.png"), tint);
			if (hasText) overlay(img, source("textures/cosmetic/cape_text_" + text + ".png"), tint);
			return img;
		}
		if (key.startsWith("runes#")) return runes(Integer.parseInt(key.substring(6)));
		int fr = 0;
		if (key.contains("#")) { fr = Integer.parseInt(key.substring(key.indexOf('#') + 1)); key = key.substring(0, key.indexOf('#')); }
		NativeImage src = source("textures/cosmetic/cape_" + key + ".png");
		if (src == null) return null;
		int sw = src.getWidth(), sh = sw / 2, s = sw / 64, oy = fr * sh;
		if (oy + sh > src.getHeight()) oy = 0;
		NativeImage img = new NativeImage(sw, sh, true);
		for (int y = 0; y < sh; y++) for (int x = 0; x < sw; x++) img.setPixel(x, y, src.getPixel(x, y + oy));
		for (int y = 0; y < 22 * s; y++) for (int x = 22 * s; x < 46 * s; x++) {
			int c = img.getPixel(x, y);
			if (((c >>> 24) & 255) > 8) continue;
			float u = x / (float) s, v = y / (float) s;
			float fu;
			if (u >= 24 && u < 34) fu = 1 + (u - 24);
			else if (u >= 36) fu = 1 + (u - 36);
			else fu = u < 24 ? 1 : 10.9f;
			float fv = v < 2 ? 1 : 1 + Math.min(15.99f, (v - 2) * 16f / 20f);
			int col = sample(src, fu * s, fv * s + oy);
			float edge = Math.min(Math.min(u - 22, 46 - u), Math.min(v, 22 - v));
			if (u >= 24 && u < 34) edge = Math.min(edge, Math.min(u - 24, 34 - u) + 0.5f);
			float dark = edge < 0.6f ? 0.7f : 1f;
			img.setPixel(x, y, shade(col, dark));
		}
		return img;
	}
}
