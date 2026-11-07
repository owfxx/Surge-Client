package gg.surge.client.emotes;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import gg.surge.client.emotes.Emotes.Emote;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class EmoteFx {
	private static final int BRIGHT = 0xF000F0;
	private static final Identifier WHITE = Identifier.fromNamespaceAndPath("surge", "textures/cosmetic/white.png");
	private static final float PX = 1f / 16f;

	private static final String[] HEART = {".XX.XX.", "XXXXXXX", "XXXXXXX", ".XXXXX.", "..XXX..", "...X..."};
	private static final String[] NOTE = {"..XXXX", "..X..X", "..X..X", "..X..X", "XXX.XX", "XXX.XX", ".X...."};
	private static final String[] ZED = {"XXXXX", "...X.", "..X..", ".X...", "XXXXX"};
	private static final String[] SPARK = {"..X..", ".XXX.", "XXXXX", ".XXX.", "..X.."};
	private static final String[] DROP = {".X.", "XXX", "XXX", ".X."};
	private static final String[] DOT = {"XX", "XX"};
	private static final String[] BANG = {"XX", "XX", "XX", "XX", "..", "XX"};

	private EmoteFx() {}

	private interface Draw {
		void go(PoseStack.Pose pose, VertexConsumer vc);
	}

	private static float frac(float v) {
		return v - (float) Math.floor(v);
	}

	private static float hash(int i) {
		int h = i * 0x27d4eb2d;
		h ^= h >>> 15;
		h *= 0x165667b1;
		h ^= h >>> 13;
		return (h & 0xFFFF) / 65535f;
	}

	private static void quad(PoseStack.Pose pose, VertexConsumer vc, float x, float y, float z, float w, float h, int argb) {
		vc.addVertex(pose, x, y, z).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(BRIGHT).setNormal(pose, 0, 0, -1);
		vc.addVertex(pose, x, y + h, z).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(BRIGHT).setNormal(pose, 0, 0, -1);
		vc.addVertex(pose, x + w, y + h, z).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(BRIGHT).setNormal(pose, 0, 0, -1);
		vc.addVertex(pose, x + w, y, z).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(BRIGHT).setNormal(pose, 0, 0, -1);
		vc.addVertex(pose, x + w, y, z).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(BRIGHT).setNormal(pose, 0, 0, 1);
		vc.addVertex(pose, x + w, y + h, z).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(BRIGHT).setNormal(pose, 0, 0, 1);
		vc.addVertex(pose, x, y + h, z).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(BRIGHT).setNormal(pose, 0, 0, 1);
		vc.addVertex(pose, x, y, z).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(BRIGHT).setNormal(pose, 0, 0, 1);
	}

	private static void sprite(PoseStack.Pose pose, VertexConsumer vc, String[] px, float cx, float cy, float z, float size, int argb) {
		int w = px[0].length(), h = px.length;
		float x0 = cx - w * size / 2f, y0 = cy - h * size / 2f;
		int edge = (argb & 0xFF000000) | (darker(argb) & 0xFFFFFF);
		for (int r = 0; r < h; r++) {
			for (int c = 0; c < w; c++) {
				if (px[r].charAt(c) != 'X') continue;
				boolean border = r == 0 || c == 0 || r == h - 1 || c == w - 1 || px[r - 1].charAt(c) != 'X' || px[r + 1].charAt(c) != 'X' || px[r].charAt(c - 1) != 'X' || px[r].charAt(c + 1) != 'X';
				quad(pose, vc, x0 + c * size, y0 + r * size, z, size, size, border ? edge : argb);
			}
		}
	}

	private static int darker(int argb) {
		int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
		return ((r * 3 / 4) << 16) | ((g * 3 / 4) << 8) | (b * 3 / 4);
	}

	private static int alpha(int rgb, float a) {
		return (Math.max(0, Math.min(255, Math.round(a * 255))) << 24) | (rgb & 0xFFFFFF);
	}

	public static void submit(PlayerModel model, PoseStack ps, SubmitNodeCollector q, Emote e, float t, boolean previewing) {
		if (e == null) return;
		if (previewing && !e.loops) t = t % (e.seconds + 0.6f);
		float fade = Math.min(1f, t / 0.3f);
		if (!e.loops && !previewing) fade *= Math.max(0f, Math.min(1f, (e.seconds - t) / 0.4f));
		if (fade <= 0.01f) return;
		final float tt = t, k = fade;
		switch (e) {
			case FLUSTERED -> {
				head(model, ps, q, (pose, vc) -> blush(pose, vc, tt, k));
				if (e == Emote.FLUSTERED) body(model, ps, q, (pose, vc) -> floaters(pose, vc, HEART, 0xFF6FA0, tt, k, 3, 1.4f, 0.55f));
			}
			case HEART -> {
				head(model, ps, q, (pose, vc) -> blush(pose, vc, tt, k * 0.6f));
				body(model, ps, q, (pose, vc) -> {
					float beat = 1f + 0.18f * (float) Math.max(0, Math.sin(tt * 7f));
					sprite(pose, vc, HEART, 0, -0.5f, -0.6f, PX * 0.9f * beat, alpha(0xFF4D7A, k));
					floaters(pose, vc, HEART, 0xFF8FB4, tt, k, 4, 1.1f, 0.7f);
				});
			}
			case CRY -> head(model, ps, q, (pose, vc) -> tears(pose, vc, tt, k));
			case FACEPALM -> head(model, ps, q, (pose, vc) -> {
				float d = frac(tt * 0.6f);
				sprite(pose, vc, DROP, 0.29f, -0.42f + d * 0.18f, -0.1f, PX * 0.55f, alpha(0x7FC8FF, k * (1f - d * 0.6f)));
			});
			case YAWN -> body(model, ps, q, (pose, vc) -> {
				for (int i = 0; i < 3; i++) {
					float p = frac(tt * 0.45f + i / 3f);
					sprite(pose, vc, ZED, 0.22f + p * 0.25f + (float) Math.sin(p * 6f) * 0.04f, -0.75f - p * 0.6f, 0f, PX * (0.35f + p * 0.35f), alpha(0xDDE6FF, k * (1f - p)));
				}
			});
			case THINKING -> body(model, ps, q, (pose, vc) -> {
				float cycle = tt % 2.4f;
				int n = Math.min(3, (int) (cycle / 0.45f));
				for (int i = 0; i < n; i++) sprite(pose, vc, DOT, 0.22f + i * 0.09f, -0.82f - i * 0.08f, 0f, PX * (0.45f + i * 0.15f), alpha(0xFFFFFF, k));
				if (cycle > 1.6f) sprite(pose, vc, BANG, 0.52f, -1.1f, 0f, PX * 0.55f, alpha(0xFFE066, k));
			});
			case CHEER -> body(model, ps, q, (pose, vc) -> confetti(pose, vc, tt, k));
			case DAB -> body(model, ps, q, (pose, vc) -> {
				float hit = Math.max(0f, 1f - Math.abs(tt - 0.35f) * 2.5f);
				if (hit > 0) for (int i = 0; i < 6; i++) {
					double a = i * Math.PI / 3;
					float r = 0.25f + (1 - hit) * 0.35f;
					sprite(pose, vc, SPARK, -0.45f + (float) Math.cos(a) * r, -0.85f + (float) Math.sin(a) * r, -0.15f, PX * 0.6f, alpha(0xFFF3B0, k * hit));
				}
			});
			case LAUGH -> head(model, ps, q, (pose, vc) -> {
				for (int side = -1; side <= 1; side += 2) {
					float p = frac(tt * 1.4f + (side > 0 ? 0.5f : 0f));
					sprite(pose, vc, DROP, side * (0.31f + p * 0.12f), -0.28f + p * p * 0.25f, -0.2f, PX * 0.6f, alpha(0x9AD8FF, k * (1 - p)));
				}
			});
			default -> {
				if (e.music != null || e == Emote.FLOSS)
					body(model, ps, q, (pose, vc) -> floaters(pose, vc, NOTE, 0xFFFFFF, tt, k, 3, 0.9f, 0.6f));
			}
		}
	}

	private static void head(PlayerModel model, PoseStack ps, SubmitNodeCollector q, Draw d) {
		ps.pushPose();
		model.head.translateAndRotate(ps);
		q.submitCustomGeometry(ps, RenderTypes.entityTranslucent(WHITE), d::go);
		ps.popPose();
	}

	private static void body(PlayerModel model, PoseStack ps, SubmitNodeCollector q, Draw d) {
		ps.pushPose();
		model.body.translateAndRotate(ps);
		q.submitCustomGeometry(ps, RenderTypes.entityTranslucent(WHITE), d::go);
		ps.popPose();
	}

	private static void blush(PoseStack.Pose pose, VertexConsumer vc, float t, float k) {
		float pulse = 0.55f + 0.25f * (float) Math.sin(t * 4f);
		int c = alpha(0xFF6B8E, pulse * k);
		float z = -4.02f * PX;
		quad(pose, vc, -3.2f * PX, -2.6f * PX, z, 2f * PX, PX, c);
		quad(pose, vc, 1.2f * PX, -2.6f * PX, z, 2f * PX, PX, c);
		int c2 = alpha(0xFF9DB4, pulse * 0.6f * k);
		quad(pose, vc, -2.8f * PX, -3.1f * PX, z - 0.001f, 1.2f * PX, 0.5f * PX, c2);
		quad(pose, vc, 1.6f * PX, -3.1f * PX, z - 0.001f, 1.2f * PX, 0.5f * PX, c2);
	}

	private static void tears(PoseStack.Pose pose, VertexConsumer vc, float t, float k) {
		float z = -4.04f * PX;
		for (int side = -1; side <= 1; side += 2) {
			float ex = side * 4.6f * PX;
			quad(pose, vc, side < 0 ? -4.2f * PX : 3.4f * PX, -3.4f * PX, z, 0.8f * PX, 3.4f * PX, alpha(0x5FB4FF, 0.7f * k));
			for (int i = 0; i < 4; i++) {
				float p = frac(t * 1.1f + i / 4f + (side > 0 ? 0.13f : 0f));
				float x = ex + side * p * 0.1f;
				float y = -2.5f * PX + p * p * 0.75f;
				float zz = z - 0.05f - p * 0.12f;
				sprite(pose, vc, DROP, x, y, zz, PX * 0.6f, alpha(0x7FC8FF, k * (1f - Math.max(0, p - 0.75f) / 0.25f)));
			}
		}
	}

	private static void floaters(PoseStack.Pose pose, VertexConsumer vc, String[] px, int rgb, float t, float k, int n, float rate, float spread) {
		for (int i = 0; i < n; i++) {
			float p = frac(t * rate * 0.5f + i / (float) n);
			float sx = (hash(i * 7 + (int) (t * rate * 0.5f + i / (float) n)) - 0.5f) * spread;
			float x = sx + (float) Math.sin(p * 7f + i) * 0.05f;
			float y = -0.7f - p * 0.65f;
			float a = Math.min(1f, p * 5f) * (1f - p);
			sprite(pose, vc, px, x, y, -0.2f, PX * (0.8f + 0.3f * (1 - p)), alpha(rgb, Math.min(1f, a * 1.6f) * k));
		}
	}

	private static final int[] CONFETTI = {0xFF5C5C, 0xFFD54F, 0x5CE1E6, 0x8AFF80, 0xC58CFF, 0xFFFFFF};

	private static void confetti(PoseStack.Pose pose, VertexConsumer vc, float t, float k) {
		for (int i = 0; i < 28; i++) {
			float p = frac(t * 0.55f + hash(i) );
			int cycle = (int) Math.floor(t * 0.55f + hash(i));
			float x = (hash(i * 31 + cycle) - 0.5f) * 1.1f + (float) Math.sin(p * 9f + i) * 0.06f;
			float y = -1.45f + p * 1.6f;
			float z = (hash(i * 13 + cycle) - 0.5f) * 0.6f;
			int c = CONFETTI[i % CONFETTI.length];
			float w = PX * (1.2f + 0.8f * (float) Math.abs(Math.sin(p * 12f + i)));
			quad(pose, vc, x, y, z - 0.15f, w, PX * 1.2f, alpha(c, k * (1f - p * 0.5f)));
		}
	}
}
