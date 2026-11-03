package gg.surge.client.mods;

/** Small color helpers. */
public final class Colors {
	private Colors() {}

	public static int parse(String hex) {
		try {
			return 0xFF000000 | Integer.parseInt(hex, 16);
		} catch (Exception e) {
			return 0xFFFFFFFF;
		}
	}

	/** HSV (0..1) to opaque ARGB. */
	public static int hsv(float h, float s, float v) {
		h = (h % 1f + 1f) % 1f;
		int i = (int) (h * 6f);
		float f = h * 6f - i, p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
		float r, g, b;
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
	}

	/** Soft rainbow that cycles every 4 seconds. */
	public static int chroma(float offset) {
		return hsv((System.currentTimeMillis() % 4000L) / 4000f + offset, 0.55f, 1f);
	}

	public static int lerp(int a, int b, float t) {
		t = Math.max(0, Math.min(1, t));
		int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
		int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
		return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
			| (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
	}

	public static int alpha(int argb, float mul) {
		int a = Math.round((argb >>> 24) * Math.max(0, Math.min(1, mul)));
		return (a << 24) | (argb & 0xFFFFFF);
	}
}
