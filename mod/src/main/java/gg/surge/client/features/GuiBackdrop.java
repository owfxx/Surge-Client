package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class GuiBackdrop {
	private GuiBackdrop() {}

	private static final long START = System.nanoTime();
	private static Object lastScreen;
	private static long openedAt;
	private static long forcedStrike;

	public static void strikeNow() {
		forcedStrike = System.nanoTime();
	}

	public static void draw(GuiGraphics g) {
		Minecraft mc = Minecraft.getInstance();
		if (!Mods.GUI_SCALES.enabled() || !(mc.screen instanceof AbstractContainerScreen<?>)) return;
		String type = Mods.GS_BACKDROP.get();
		if (type.equals("None")) return;
		long now = System.nanoTime();
		if (lastScreen != mc.screen) { lastScreen = mc.screen; openedAt = now; }
		float t = (now - START) / 1e9f;
		float fade = Math.min(1f, (now - openedAt) / 1e9f / 0.35f);
		float amount = Mods.GS_BACKDROP_AMOUNT.get() / 100f;
		float s = (float) mc.getWindow().getGuiScale();
		int w = Math.round(g.guiWidth() * s), h = Math.round(g.guiHeight() * s);
		g.pose().pushMatrix();
		g.pose().scale(1f / s, 1f / s);
		switch (type) {
			case "Rain and thunder" -> rain(g, w, h, t, fade, amount, s);
			case "Snow" -> snow(g, w, h, t, fade, amount, s);
			case "Fireflies" -> fireflies(g, w, h, t, fade, amount, s);
			case "Starry night" -> stars(g, w, h, t, fade, amount, s);
			case "Aurora" -> aurora(g, w, h, t, fade, amount, s);
			case "Sakura" -> sakura(g, w, h, t, fade, amount, s);
			case "Embers" -> embers(g, w, h, t, fade, amount, s);
			default -> { }
		}
		g.pose().popMatrix();
		g.nextStratum();
	}

	private static float hash(int i, int k) {
		int x = i * 374761393 + k * 668265263;
		x = (x ^ (x >>> 13)) * 1274126177;
		x ^= x >>> 16;
		return (x & 0xFFFFFF) / (float) 0xFFFFFF;
	}

	private static int argb(float a, int rgb) {
		return (Math.max(0, Math.min(255, Math.round(a * 255))) << 24) | (rgb & 0xFFFFFF);
	}

	private static float wrap(float v, float m) {
		v %= m;
		return v < 0 ? v + m : v;
	}

	private static int count(int base, int w, int h, float amount, float s) {
		return Math.max(4, Math.round(base * amount * (w * h) / (1920f * 1080f) * Math.min(1.6f, 2.2f / Math.max(1f, s) + 0.5f)));
	}

	private static void streak(GuiGraphics g, float x, float y, float len, float thick, float angle, int c0, int c1) {
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().rotate(angle);
		int ht = Math.max(1, Math.round(thick / 2));
		g.fillGradient(-ht, 0, Math.max(ht, 1), Math.max(1, Math.round(len)), c0, c1);
		g.pose().popMatrix();
	}

	private static void dot(GuiGraphics g, float x, float y, float r, int color) {
		int ir = Math.max(1, Math.round(r));
		int cx = Math.round(x), cy = Math.round(y);
		if (ir <= 1) { g.fill(cx, cy, cx + 1, cy + 1, color); return; }
		for (int dy = -ir; dy < ir; dy++) {
			float yy = dy + 0.5f;
			int dx = (int) Math.round(Math.sqrt(Math.max(0, r * r - yy * yy)));
			if (dx > 0) g.fill(cx - dx, cy + dy, cx + dx, cy + dy + 1, color);
		}
	}

	private static void glow(GuiGraphics g, float x, float y, float r, float a, int rgb) {
		dot(g, x, y, r * 3.2f, argb(a * 0.07f, rgb));
		dot(g, x, y, r * 2f, argb(a * 0.16f, rgb));
		dot(g, x, y, r, argb(a, rgb));
	}

	private static void rain(GuiGraphics g, int w, int h, float t, float fade, float amount, float s) {
		float angle = 0.2f;
		float slant = (float) Math.tan(angle);
		int n = count(260, w, h, amount, s);
		for (int i = 0; i < n; i++) {
			float depth = 0.35f + hash(i, 1) * 0.65f;
			float speed = h * (1.1f + depth * 1.3f);
			float len = (18 + 30 * depth) * Math.max(1f, s / 2f);
			float y = wrap(hash(i, 2) * (h + len) + t * speed, h + len * 2) - len;
			float x = wrap(hash(i, 3) * (w + h * slant) - y * slant, w + h * slant);
			int c = 0xBFD8FF;
			streak(g, x, y, len, Math.max(1f, depth * 1.6f * s / 2f), angle, argb(0, c), argb(fade * (0.12f + depth * 0.32f), c));
		}
		g.fillGradient(0, h - h / 5, w, h, 0x00000000, argb(fade * 0.25f, 0x101828));
		float period = 9f;
		int strike = (int) Math.floor(t / period);
		float since = t - strike * period - hash(strike, 7) * 4f;
		float forced = (System.nanoTime() - forcedStrike) / 1e9f;
		if (forced < 1.2f) { since = forced; strike = (int) (forcedStrike >>> 20); }
		if (since < 0 || since > 1.2f) return;
		float flash = since < 0.08f ? 1f : since < 0.16f ? 0.3f : since < 0.24f ? 0.85f : (float) Math.exp(-(since - 0.24f) * 5f) * 0.85f;
		g.fill(0, 0, w, h, argb(fade * flash * 0.22f, 0xDCE6FF));
		if (since > 0.5f) return;
		float bx = w * (0.15f + hash(strike, 8) * 0.7f), by = 0;
		float boltA = fade * Math.min(1f, flash * 1.2f);
		int segs = 14;
		float seg = h * 0.62f / segs;
		for (int k = 0; k < segs; k++) {
			float nx = bx + (hash(strike * 31 + k, 9) - 0.5f) * seg * 1.3f, ny = by + seg;
			float dx = nx - bx, dy = ny - by;
			float a = (float) (Math.atan2(dy, dx) - Math.PI / 2);
			float len = (float) Math.hypot(dx, dy);
			streak(g, bx, by, len, 6 * s / 2f, a, argb(boltA * 0.25f, 0xAFC4FF), argb(boltA * 0.25f, 0xAFC4FF));
			streak(g, bx, by, len, 2 * s / 2f, a, argb(boltA, 0xFFFFFF), argb(boltA, 0xFFFFFF));
			if (k > 3 && hash(strike * 17 + k, 10) > 0.75f) {
				float fx = bx, fy = by;
				for (int f = 0; f < 4; f++) {
					float gx = fx + (hash(k * 7 + f, strike) - 0.3f) * seg * 1.2f * (hash(k, strike) > 0.5f ? 1 : -1), gy = fy + seg * 0.7f;
					float fa = (float) (Math.atan2(gy - fy, gx - fx) - Math.PI / 2);
					streak(g, fx, fy, (float) Math.hypot(gx - fx, gy - fy), Math.max(1f, s / 2f), fa, argb(boltA * 0.6f, 0xFFFFFF), argb(boltA * 0.6f, 0xFFFFFF));
					fx = gx;
					fy = gy;
				}
			}
			bx = nx;
			by = ny;
		}
	}

	private static void snow(GuiGraphics g, int w, int h, float t, float fade, float amount, float s) {
		int n = count(170, w, h, amount, s);
		for (int i = 0; i < n; i++) {
			float depth = 0.25f + hash(i, 1) * 0.75f;
			float r = (1.2f + depth * 3.2f) * s / 2f;
			float speed = h * (0.05f + depth * 0.09f);
			float y = wrap(hash(i, 2) * (h + 20) + t * speed, h + 20) - 10;
			float sway = (float) Math.sin(t * (0.6f + hash(i, 4)) + hash(i, 5) * 6.28f) * 18 * depth * s / 2f;
			float x = wrap(hash(i, 3) * w + sway + t * 6 * depth, w);
			dot(g, x, y, r * 2.2f, argb(fade * 0.06f * depth, 0xFFFFFF));
			dot(g, x, y, r, argb(fade * (0.35f + depth * 0.5f), 0xF4F8FF));
		}
		g.fillGradient(0, h - h / 7, w, h, 0x00FFFFFF, argb(fade * 0.08f, 0xEAF2FF));
	}

	private static void fireflies(GuiGraphics g, int w, int h, float t, float fade, float amount, float s) {
		g.fillGradient(0, h / 2, w, h, 0x00000000, argb(fade * 0.25f, 0x0A1A10));
		int n = count(46, w, h, amount, s);
		for (int i = 0; i < n; i++) {
			float sp = 0.05f + hash(i, 1) * 0.08f;
			float x = (0.5f + 0.48f * (float) Math.sin(t * sp + hash(i, 2) * 40) * (float) Math.cos(t * sp * 0.7f + hash(i, 3) * 20)) * w;
			float y = (0.55f + 0.42f * (float) Math.sin(t * sp * 1.3f + hash(i, 4) * 30)) * h;
			float pulse = 0.5f + 0.5f * (float) Math.sin(t * (1.2f + hash(i, 5) * 1.6f) + hash(i, 6) * 6.28f);
			pulse = pulse * pulse;
			float r = (1.6f + hash(i, 7) * 1.4f) * s / 2f;
			glow(g, x, y, r, fade * (0.15f + 0.85f * pulse), hash(i, 8) > 0.3f ? 0xD8FF7A : 0xFFE27A);
		}
	}

	private static void stars(GuiGraphics g, int w, int h, float t, float fade, float amount, float s) {
		g.fillGradient(0, 0, w, h, argb(fade * 0.3f, 0x0B1030), argb(fade * 0.15f, 0x1A1238));
		int n = count(240, w, h, amount, s);
		for (int i = 0; i < n; i++) {
			float x = hash(i, 1) * w, y = hash(i, 2) * h;
			float tw = 0.55f + 0.45f * (float) Math.sin(t * (0.8f + hash(i, 3) * 2.5f) + hash(i, 4) * 6.28f);
			float big = hash(i, 5);
			float r = (big > 0.93f ? 1.8f : big > 0.7f ? 1.1f : 0.7f) * s / 2f;
			int c = hash(i, 6) > 0.8f ? 0xFFE9C4 : hash(i, 6) > 0.6f ? 0xC4D8FF : 0xFFFFFF;
			if (big > 0.93f) glow(g, x, y, r, fade * tw, c);
			else dot(g, x, y, r, argb(fade * tw * (0.35f + big * 0.6f), c));
		}
		float period = 6f;
		int k = (int) Math.floor(t / period);
		float since = t - k * period - hash(k, 3) * 2f;
		if (since >= 0 && since < 0.9f) {
			float p = since / 0.9f;
			float sx = w * (0.2f + hash(k, 1) * 0.7f), sy = h * (0.05f + hash(k, 2) * 0.3f);
			float dist = w * 0.35f;
			float hx = sx - dist * p, hy = sy + dist * 0.45f * p;
			float a = fade * (float) Math.sin(p * Math.PI);
			float ang = (float) (Math.atan2(-0.45f, 1f) - Math.PI / 2);
			streak(g, hx, hy, w * 0.12f, 2 * s / 2f, ang, argb(a, 0xFFFFFF), argb(0, 0xFFFFFF));
			glow(g, hx, hy, 1.6f * s / 2f, a, 0xFFFFFF);
		}
	}

	private static void aurora(GuiGraphics g, int w, int h, float t, float fade, float amount, float s) {
		g.fillGradient(0, 0, w, h, argb(fade * 0.25f, 0x061018), argb(fade * 0.1f, 0x0A0A20));
		int[] cols = {0x3CFFB0, 0x46D6FF, 0xA070FF};
		int step = Math.max(2, Math.round(3 * s / 2f));
		for (int band = 0; band < 3; band++) {
			float base = h * (0.22f + band * 0.09f);
			for (int x = 0; x < w; x += step) {
				float u = x / (float) w;
				float wave = (float) (Math.sin(u * 5.5f + t * 0.35f + band * 1.7f) * 0.6f + Math.sin(u * 13f - t * 0.6f + band) * 0.25f + Math.sin(u * 2.1f + t * 0.15f) * 0.5f);
				float top = base + wave * h * 0.07f;
				float height = h * (0.12f + 0.1f * (0.5f + 0.5f * (float) Math.sin(u * 9f + t * 0.8f + band * 2.3f)));
				float shimmer = 0.6f + 0.4f * (float) Math.sin(u * 40f + t * 2.2f + band);
				float a = fade * amount * (0.12f + 0.1f * shimmer) * (1f - Math.abs(u - 0.5f) * 0.6f);
				int c = cols[band];
				g.fillGradient(x, Math.round(top), x + step, Math.round(top + height * 0.25f), argb(0, c), argb(a, c));
				g.fillGradient(x, Math.round(top + height * 0.25f), x + step, Math.round(top + height), argb(a, c), argb(0, c));
			}
		}
		int n = count(90, w, h, 1f, s);
		for (int i = 0; i < n; i++) {
			float tw = 0.5f + 0.5f * (float) Math.sin(t * (1 + hash(i, 3) * 2) + hash(i, 4) * 6.28f);
			dot(g, hash(i, 1) * w, hash(i, 2) * h, 0.7f * s / 2f, argb(fade * tw * 0.6f, 0xFFFFFF));
		}
	}

	private static void sakura(GuiGraphics g, int w, int h, float t, float fade, float amount, float s) {
		int n = count(90, w, h, amount, s);
		for (int i = 0; i < n; i++) {
			float depth = 0.35f + hash(i, 1) * 0.65f;
			float speed = h * (0.06f + depth * 0.08f);
			float y = wrap(hash(i, 2) * (h + 30) + t * speed, h + 30) - 15;
			float x = wrap(hash(i, 3) * w + t * w * 0.03f * depth + (float) Math.sin(t * 0.9f + hash(i, 4) * 6.28f) * 30 * s / 2f, w);
			float spin = t * (0.8f + hash(i, 5) * 2f) + hash(i, 6) * 6.28f;
			float pw = (5 + depth * 6) * s / 2f, ph = pw * (0.35f + 0.65f * Math.abs((float) Math.cos(spin)));
			int c = hash(i, 7) > 0.5f ? 0xFFB7D0 : 0xFFD4E2;
			g.pose().pushMatrix();
			g.pose().translate(x, y);
			g.pose().rotate(spin * 0.5f);
			float a = fade * (0.45f + depth * 0.45f);
			int hw = Math.max(1, Math.round(pw / 2)), hh = Math.max(1, Math.round(ph / 2));
			if (hh > 1) g.fill(-hw, -hh + 1, hw, hh - 1, argb(a, c));
			g.fill(-hw + 1, -hh, hw - 1, hh, argb(a, c));
			g.fill(-hw / 3, -hh, hw / 3, 0, argb(a * 0.5f, 0xFF8FB4));
			g.pose().popMatrix();
		}
	}

	private static void embers(GuiGraphics g, int w, int h, float t, float fade, float amount, float s) {
		g.fillGradient(0, h / 2, w, h, 0x00000000, argb(fade * 0.28f, 0x3A1004));
		int n = count(120, w, h, amount, s);
		for (int i = 0; i < n; i++) {
			float depth = 0.3f + hash(i, 1) * 0.7f;
			float life = 4f + hash(i, 2) * 4f;
			float p = wrap(t / life + hash(i, 3), 1f);
			float y = h + 10 - p * h * (0.6f + depth * 0.5f);
			float x = hash(i, 4) * w + (float) Math.sin(t * (1 + hash(i, 5)) + hash(i, 6) * 6.28f) * 20 * s / 2f + p * 40 * (hash(i, 7) - 0.5f) * s;
			float a = fade * (float) Math.sin(p * Math.PI) * (0.4f + depth * 0.6f);
			float flicker = 0.75f + 0.25f * (float) Math.sin(t * 12 + i);
			int c = p < 0.4f ? 0xFFD27A : p < 0.75f ? 0xFF9A3C : 0xFF5A28;
			glow(g, x, y, (0.8f + depth * 1.4f) * s / 2f, a * flicker, c);
		}
	}
}
