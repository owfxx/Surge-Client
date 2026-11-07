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
	public static final List<String> TEXTS = List.of("beta_tester", "beta", "early", "surge", "og", "founder", "none");
	public static final List<String> TEXT_NAMES = List.of("Beta Tester", "Beta", "Early Access", "Surge", "OG", "Founder", "No text");
	public static final List<String> PALETTE = List.of(
		"141418", "FFFFFF", "A1A1AA", "52525B", "8F50F2", "B483FF", "EC68B0", "F43F5E",
		"E2363F", "F0842A", "F2C430", "78CE46", "1C965A", "22BED4", "3468E8", "1E3A8A");

	private static final Map<String, Identifier> CACHE = new HashMap<>();
	private static final Map<String, NativeImage> SOURCES = new HashMap<>();

	private CapeTextures() {}

	public static String resolve(String style) {
		if (style == null) return null;
		if (style.equals("custom")) return "custom_" + SurgeConfig.get().capeColor.toLowerCase(Locale.ROOT);
		if (style.equals("beta")) {
			var b = SurgeConfig.get().betaCape;
			return "beta2_" + b.color.toLowerCase(Locale.ROOT) + "_" + b.logo.toLowerCase(Locale.ROOT) + "_" + b.text;
		}
		if (style.startsWith("beta_")) {
			String[] p = style.split("_");
			return "beta2_" + (p.length > 2 ? p[2] : "141418") + "_ffffff_beta_tester";
		}
		if (style.startsWith("classic_")) return "custom_8f50f2";
		return style;
	}

	public static Identifier get(String style) {
		String key = resolve(style);
		if (key == null) return null;
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
		if (CACHE.size() > 64) {
			for (Identifier old : CACHE.values()) Minecraft.getInstance().getTextureManager().release(old);
			CACHE.clear();
		}
		CACHE.put(key, id);
		return id;
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

	private static void fill(NativeImage img, int color) {
		int w = img.getWidth(), h = img.getHeight();
		for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) img.setPixel(x, y, color);
	}

	private static NativeImage build(String key) {
		int w = 512, h = 256;
		if (key.startsWith("custom_")) {
			NativeImage img = new NativeImage(w, h, true);
			fill(img, hex(key.substring(7), 0xFF8F50F2));
			return img;
		}
		if (key.startsWith("beta2_")) {
			String[] p = key.split("_", 4);
			NativeImage img = new NativeImage(w, h, true);
			int base = hex(p.length > 1 ? p[1] : "141418", 0xFF141418);
			int tint = hex(p.length > 2 ? p[2] : "ffffff", 0xFFFFFFFF);
			String text = p.length > 3 ? p[3] : "beta_tester";
			fill(img, base);
			overlay(img, source("textures/cosmetic/cape_overlay_logo.png"), tint);
			if (TEXTS.contains(text) && !text.equals("none")) overlay(img, source("textures/cosmetic/cape_text_" + text + ".png"), tint);
			return img;
		}
		NativeImage src = source("textures/cosmetic/cape_" + key + ".png");
		if (src == null) return null;
		int sw = src.getWidth(), sh = src.getHeight(), s = sw / 64;
		NativeImage img = new NativeImage(sw, sh, true);
		for (int y = 0; y < sh; y++) for (int x = 0; x < sw; x++) img.setPixel(x, y, src.getPixel(x, y));
		for (int y = 0; y < 22 * s; y++) for (int x = 22 * s; x < 46 * s; x++) {
			int c = img.getPixel(x, y);
			if (((c >>> 24) & 255) > 8) continue;
			int fx = s + ((x - 22 * s) % (10 * s)), fy = s + Math.min(16 * s - 1, y * 16 / 22);
			img.setPixel(x, y, src.getPixel(Math.min(sw - 1, fx), Math.min(sh - 1, fy)));
		}
		return img;
	}
}
