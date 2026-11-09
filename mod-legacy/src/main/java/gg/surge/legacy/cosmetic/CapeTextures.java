package gg.surge.legacy.cosmetic;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CapeTextures {
	public static final List<String> TEXTS = Arrays.asList("beta_tester", "beta", "early", "surge", "og", "none");
	private static final Identifier FALLBACK = new Identifier("surge", "textures/cosmetic/cape_surge.png");
	private static final Map<String, Identifier> CACHE = new HashMap<String, Identifier>();
	private static final Map<String, BufferedImage> SOURCES = new HashMap<String, BufferedImage>();
	private static final Map<String, int[]> ANIM = new HashMap<String, int[]>();
	private static final Map<String, Anim> ANIMS = new HashMap<String, Anim>();

	static {
		Object[][] a = {{"flame", 16, 70}, {"enchanted", 16, 90}, {"runes", 24, 90}, {"blinkcat", 2, 0}, {"aurora", 16, 140}, {"emerald", 16, 110}, {"galaxy", 24, 120},
			{"midnight", 20, 160}, {"ocean", 16, 120}, {"royal", 16, 110}, {"sakura", 20, 130}, {"surge", 16, 110}, {"void", 16, 120}, {"lava", 20, 110}, {"matrix", 20, 80},
			{"storm", 24, 110}, {"phoenix", 20, 90}, {"koi", 24, 120}, {"bloodmoon", 20, 120}, {"obsidian", 16, 110}, {"frost", 16, 130}, {"sunset", 18, 110}, {"ender", 24, 110}, {"diamond", 20, 100}, {"toxic", 20, 100}};
		for (Object[] r : a) ANIM.put((String) r[0], new int[]{(Integer) r[1], (Integer) r[2]});
	}

	private CapeTextures() {}

	private static final class Anim {
		final Identifier id;
		final NativeImageBackedTexture tex;
		final int[][] frames;
		int shown = -1;

		Anim(Identifier id, NativeImageBackedTexture tex, int n) {
			this.id = id;
			this.tex = tex;
			this.frames = new int[n][];
		}
	}

	private static int blink(long now) {
		long cycle = now / 11000, in = now % 11000;
		long h = cycle * 0x9E3779B97F4A7C15L;
		h ^= (h >>> 29);
		long at = 1500 + Math.floorMod(h, 7500L);
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
		return (int) ((now / spec[1]) % spec[0]);
	}

	public static Identifier get(String key) {
		if (key == null || key.equals("none")) return null;
		if (key.startsWith("image_") || key.equals("image")) return FALLBACK;
		int[] spec = ANIM.get(key);
		if (spec != null) return animated(key, spec);
		Identifier id = CACHE.get(key);
		if (id != null) return id;
		BufferedImage img;
		try { img = build(key); } catch (Throwable t) { img = null; }
		if (img == null) return FALLBACK;
		id = new Identifier("surge", "capes/" + Integer.toHexString(key.hashCode()));
		MinecraftClient.getInstance().getTextureManager().loadTexture(id, new NativeImageBackedTexture(img));
		CACHE.put(key, id);
		return id;
	}

	private static Identifier animated(String key, int[] spec) {
		int f = frame(spec);
		Anim a = ANIMS.get(key);
		try {
			if (a == null) {
				BufferedImage first = build(key + "#" + f);
				if (first == null) return FALLBACK;
				Identifier id = new Identifier("surge", "capes/anim_" + key);
				NativeImageBackedTexture tex = new NativeImageBackedTexture(first);
				MinecraftClient.getInstance().getTextureManager().loadTexture(id, tex);
				a = new Anim(id, tex, spec[0]);
				a.frames[f] = pixels(first);
				a.shown = f;
				ANIMS.put(key, a);
				return id;
			}
			if (a.shown != f) {
				if (a.frames[f] == null) {
					BufferedImage img = build(key + "#" + f);
					if (img != null) a.frames[f] = pixels(img);
				}
				if (a.frames[f] != null && a.frames[f].length == a.tex.getPixels().length) {
					System.arraycopy(a.frames[f], 0, a.tex.getPixels(), 0, a.frames[f].length);
					a.tex.upload();
				}
				a.shown = f;
			}
		} catch (Throwable ignored) { }
		return a == null ? FALLBACK : a.id;
	}

	private static int[] pixels(BufferedImage img) {
		return img.getRGB(0, 0, img.getWidth(), img.getHeight(), null, 0, img.getWidth());
	}

	private static int hex(String s, int def) {
		try { return 0xFF000000 | Integer.parseInt(s, 16); } catch (Exception e) { return def; }
	}

	private static BufferedImage source(String path) {
		if (SOURCES.containsKey(path)) return SOURCES.get(path);
		BufferedImage n = null;
		try (InputStream in = MinecraftClient.getInstance().getResourceManager().getResource(new Identifier(path.startsWith("textures/font") ? "minecraft" : "surge", path)).getInputStream()) {
			n = ImageIO.read(in);
		} catch (Exception e) {
			n = null;
		}
		SOURCES.put(path, n);
		return n;
	}

	private static BufferedImage blank(int w, int h) {
		return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	}

	private static void overlay(BufferedImage dst, BufferedImage src, int tint) {
		if (src == null) return;
		int sw = src.getWidth(), sh = src.getHeight(), w = dst.getWidth(), h = dst.getHeight();
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
			int s = src.getRGB(x * sw / w, y * sh / h);
			int a = (s >>> 24) & 255;
			if (a == 0) continue;
			int d = dst.getRGB(x, y);
			int r = (((tint >> 16) & 255) * a + ((d >> 16) & 255) * (255 - a)) / 255;
			int g = (((tint >> 8) & 255) * a + ((d >> 8) & 255) * (255 - a)) / 255;
			int b = ((tint & 255) * a + (d & 255) * (255 - a)) / 255;
			dst.setRGB(x, y, 0xFF000000 | (r << 16) | (g << 8) | b);
		}
	}

	private static int lerp(int a, int b, float t) {
		int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
		int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
		int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
		return 0xFF000000 | (r << 16) | (g << 8) | bl;
	}

	private static void fill(BufferedImage img, String spec) {
		String[] parts = spec.split("-");
		int top = hex(parts[0], 0xFF8F50F2), bottom = parts.length > 1 ? hex(parts[1], top) : top;
		int w = img.getWidth(), h = img.getHeight(), s = w / 64;
		for (int y = 0; y < h; y++) {
			float t = parts.length > 1 ? Math.max(0f, Math.min(1f, (y / (float) s - 1f) / 16f)) : 0f;
			if (y >= 22 * s) t = 0.5f;
			int c = lerp(top, bottom, t);
			for (int x = 0; x < w; x++) img.setRGB(x, y, c);
		}
	}

	private static void glyph(BufferedImage dst, BufferedImage font, char c, int x, int y, int px, int color, float alpha) {
		int cell = font.getWidth() / 16;
		int gx = (c % 16) * cell, gy = (c / 16) * cell;
		for (int j = 0; j < cell; j++) for (int i = 0; i < cell; i++) {
			int a = (font.getRGB(gx + i, gy + j) >>> 24) & 255;
			if (a < 128) continue;
			for (int yy = 0; yy < px; yy++) for (int xx = 0; xx < px; xx++) {
				int dx = x + i * px + xx, dy = y + j * px + yy;
				if (dx < 0 || dy < 0 || dx >= dst.getWidth() || dy >= dst.getHeight()) continue;
				int d = dst.getRGB(dx, dy);
				int r = Math.round(((d >> 16) & 255) * (1 - alpha) + ((color >> 16) & 255) * alpha);
				int g = Math.round(((d >> 8) & 255) * (1 - alpha) + ((color >> 8) & 255) * alpha);
				int b = Math.round((d & 255) * (1 - alpha) + (color & 255) * alpha);
				dst.setRGB(dx, dy, 0xFF000000 | (r << 16) | (g << 8) | b);
			}
		}
	}

	private static void runeFace(BufferedImage img, BufferedImage font, int fx, int fy, int fw, int fh, int cols, int rows, int px, int frame, int seed, float dim) {
		int cw = fw / cols, ch = fh / rows, cell = font.getWidth() / 16;
		for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
			int id = seed + r * 31 + c * 7;
			int change = (frame + (id * 5) % 24) / 6;
			long h = (id * 0x9E3779B97F4A7C15L) ^ (change * 0xBF58476D1CE4E5B9L);
			h ^= h >>> 31;
			char ch2 = (char) ('a' + Math.floorMod(h, 26L));
			float wave = 0.5f + 0.5f * (float) Math.cos(2 * Math.PI * (frame / 24.0 - r / (double) rows));
			int x = fx + c * cw + (cw - cell * px) / 2 + px, y = fy + r * ch + (ch - cell * px) / 2 + px / 2;
			glyph(img, font, ch2, x + 1, y + 1, px, 0xFF000000, 0.5f * dim);
			glyph(img, font, ch2, x, y, px, 0xFFFFFFFF, (0.45f + 0.55f * wave) * dim);
		}
	}

	private static BufferedImage runes(int frame) {
		BufferedImage font = source("textures/font/ascii_sga.png");
		if (font == null) return null;
		int s = 8;
		BufferedImage img = blank(512, 256);
		fill(img, "111114");
		runeFace(img, font, s, s, 10 * s, 16 * s, 5, 8, 2, frame, 0, 1f);
		runeFace(img, font, 12 * s, s, 10 * s, 16 * s, 5, 8, 2, frame, 97, 0.35f);
		return img;
	}

	private static BufferedImage build(String key) {
		if (key.startsWith("custom_")) {
			BufferedImage img = blank(512, 256);
			fill(img, key.substring(7));
			return img;
		}
		if (key.startsWith("beta2_")) {
			String[] p = key.split("_", 4);
			BufferedImage img = blank(512, 256);
			int tint = hex(p.length > 2 ? p[2] : "ffffff", 0xFFFFFFFF);
			String text = p.length > 3 ? p[3] : "beta_tester";
			fill(img, p.length > 1 ? p[1] : "141418");
			boolean hasText = TEXTS.contains(text) && !text.equals("none");
			overlay(img, source(hasText ? "textures/cosmetic/cape_overlay_logo.png" : "textures/cosmetic/cape_overlay_logo_center.png"), tint);
			if (hasText) overlay(img, source("textures/cosmetic/cape_text_" + text + ".png"), tint);
			return img;
		}
		if (key.startsWith("runes#")) return runes(Integer.parseInt(key.substring(6)));
		int fr = 0;
		if (key.contains("#")) { fr = Integer.parseInt(key.substring(key.indexOf('#') + 1)); key = key.substring(0, key.indexOf('#')); }
		BufferedImage src = source("textures/cosmetic/cape_" + key + ".png");
		if (src == null) return null;
		int sw = src.getWidth(), sh = sw / 2, oy = fr * sh;
		if (oy + sh > src.getHeight()) oy = 0;
		BufferedImage img = blank(sw, sh);
		img.setRGB(0, 0, sw, sh, src.getRGB(0, oy, sw, sh, null, 0, sw), 0, sw);
		return img;
	}
}
