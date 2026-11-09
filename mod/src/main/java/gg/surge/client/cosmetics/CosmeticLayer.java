package gg.surge.client.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

public class CosmeticLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
	private static final int FULL_BRIGHT = 0xF000F0;
	private static final Identifier WHITE = Identifier.fromNamespaceAndPath("surge", "textures/cosmetic/white.png");

	public CosmeticLayer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) {
		super(parent);
	}

	public static Player playerOf(AvatarRenderState s) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return null;
		if (mc.player != null && matches(s, mc.player)) return mc.player;
		for (Player p : mc.level.players()) if (matches(s, p)) return p;
		return null;
	}

	public static boolean isLocal(AvatarRenderState s) {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && matches(s, mc.player);
	}

	private static boolean matches(AvatarRenderState s, Player p) {
		return within(s.x, p.xo, p.getX()) && within(s.y, p.yo, p.getY()) && within(s.z, p.zo, p.getZ());
	}

	private static boolean within(double v, double a, double b) {
		return v >= Math.min(a, b) - 0.05 && v <= Math.max(a, b) + 0.05;
	}

	@Override
	public void submit(PoseStack ps, SubmitNodeCollector q, int light, AvatarRenderState state, float limbAngle, float limbDistance) {
		if (state.isInvisible) return;
		CapturedLook fxLook = ((SurgeState) state).surge$look();
		if (fxLook != null && fxLook.emote() != null) gg.surge.client.emotes.EmoteFx.submit(getParentModel(), ps, q, fxLook.emote(), fxLook.emoteTime(), fxLook.emotePreview());
		Player p = playerOf(state);
		if (p == null) {
			CapturedLook lk = ((SurgeState) state).surge$look();
			if (lk == null || Minecraft.getInstance().level != null && !Preview.clean) return;
			if (lk.cape() != null) cape(ps, q, light, lk.cape(), state.ageInTicks, state);
			if (lk.wings() != null) wings(ps, q, light, lk.wings(), state.ageInTicks, 0f, state.id * 2 + 1);
			if (lk.halo() != null) halo(ps, q, lk.halo(), state.ageInTicks, false);
			return;
		}
		float age = state.ageInTicks;
		boolean elytra = p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA) && !(Preview.clean || Preview.card != null);
		float speed = Math.min(1f, state.walkAnimationSpeed);
		boolean flying = p.getAbilities().flying || p.isFallFlying();

		CapturedLook look = ((SurgeState) state).surge$look();
		String cape = look != null ? look.cape() : Cosmetics.forPlayer(p, "cape");
		if (cape != null && !elytra) cape(ps, q, light, cape, age, state);
		String wings = look != null ? look.wings() : Cosmetics.forPlayer(p, "wings");
		if (wings != null && !(elytra && Mods.COS_WINGS_ELYTRA.on())) wings(ps, q, light, wings, age, flying ? 1f : speed, p.getId() * 2 + (Preview.clean || Preview.card != null ? 1 : 0));
		String halo = look != null ? look.halo() : Cosmetics.forPlayer(p, "halo");
		if (halo != null) halo(ps, q, halo, age, !p.getItemBySlot(EquipmentSlot.HEAD).isEmpty());
	}

	private void cape(PoseStack ps, SubmitNodeCollector q, int light, String style, float age, AvatarRenderState state) {
		Identifier tex = CapeTextures.get(style);
		if (tex == null) return;
		int lit = light;
		boolean preview = Preview.clean || Preview.card != null;
		float flap = preview ? 0f : state.capeFlap, lean = preview ? 0f : state.capeLean, lean2 = preview ? 0f : state.capeLean2;
		float swing = 6f + lean / 2f + flap + (preview ? (float) Math.sin(age * 0.06f) * 2.5f : 0f);
		float side = lean2 / 2f;
		ps.pushPose();
		getParentModel().body.translateAndRotate(ps);
		ps.translate(0f, 0f, 0.125f);
		ps.mulPose(Axis.XP.rotationDegrees(swing));
		ps.mulPose(Axis.ZP.rotationDegrees(side));
		ps.mulPose(Axis.YP.rotationDegrees(-side));
		int t = -1, b = -1;
		q.submitCustomGeometry(ps, RenderTypes.entityTranslucent(tex), (pose, vc) -> {
			float x0 = -5f / 16, x1 = 5f / 16, y0 = 0f, y1 = 1f, z0 = 0f, z1 = 1f / 16;
			face(pose, vc, lit, t, b, 0, 0, 1, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 1, 1, 11, 17);
			face(pose, vc, lit, t, b, 0, 0, -1, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 12, 1, 22, 17);
			face(pose, vc, lit, t, b, -1, 0, 0, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, 0, 1, 1, 17);
			face(pose, vc, lit, t, b, 1, 0, 0, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 11, 1, 12, 17);
			face(pose, vc, lit, t, t, 0, -1, 0, x0, y0, z0, x0, y0, z1, x1, y0, z1, x1, y0, z0, 1, 0, 11, 1);
			face(pose, vc, lit, b, b, 0, 1, 0, x0, y1, z1, x0, y1, z0, x1, y1, z0, x1, y1, z1, 11, 0, 21, 1);
		});
		ps.popPose();
	}

	private static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	private static void face(PoseStack.Pose pose, VertexConsumer vc, int light, int top, int bottom, float nx, float ny, float nz,
							 float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
							 float u0, float v0, float u1, float v1) {
		u0 /= 64f; u1 /= 64f; v0 /= 32f; v1 /= 32f;
		vc.addVertex(pose, ax, ay, az).setColor(ay < 0.5f ? top : bottom).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
		vc.addVertex(pose, dx, dy, dz).setColor(dy < 0.5f ? top : bottom).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
		vc.addVertex(pose, cx, cy, cz).setColor(cy < 0.5f ? top : bottom).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
		vc.addVertex(pose, bx, by, bz).setColor(by < 0.5f ? top : bottom).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
	}

	private static final java.util.Map<Integer, float[]> FLAP = new java.util.HashMap<>();

	private void wings(PoseStack ps, SubmitNodeCollector q, int light, String style, float age, float moving, int key) {
		Identifier tex = Identifier.fromNamespaceAndPath("surge", "textures/cosmetic/wings_" + style + ".png");
		boolean glows = !("angel".equals(style) || "gold".equals(style) || "bat".equals(style));
		int lit = glows ? FULL_BRIGHT : (light | (11 << 4));
		float open = Math.min(1f, moving * 1.2f);
		float flapSpeed = 0.07f + open * 0.18f;
		float[] st = FLAP.computeIfAbsent(key, k -> new float[]{0f, age, 0f});
		float dAge = age - st[1];
		if (dAge < 0f || dAge > 5f) dAge = 0f;
		st[0] += dAge * flapSpeed;
		st[1] = age;
		st[2] += (open - st[2]) * Math.min(1f, dAge * 0.15f);
		open = st[2];
		if (FLAP.size() > 256) FLAP.clear();
		float flap = (float) Math.sin(st[0]) * (3f + open * 15f);
		float spread = 68f - open * 34f;
		float lift = 10f + open * 6f;

		for (int side = -1; side <= 1; side += 2) {
			ps.pushPose();
			getParentModel().body.translateAndRotate(ps);
			ps.translate(side * 0.05f, 0.18f, 0.16f);
			ps.scale(side, 1f, 1f);
			ps.mulPose(Axis.YP.rotationDegrees(-(spread + flap)));
			ps.mulPose(Axis.ZP.rotationDegrees(-lift - flap * 0.3f));
			final float ph = st[0], op = open;
			q.submitCustomGeometry(ps, RenderTypes.entityTranslucent(tex), (pose, vc) -> {
				wingMesh(pose, vc, ph, op, lit, 0xFFFFFFFF);
			});
			ps.popPose();
		}
	}

	public static float[][] wingShape(float phase, float open) {
		int n = 12;
		float len = 1.30f / n;
		float[][] pts = new float[n + 1][4];
		float x = 0.02f, y = 0f, z = 0f;
		float flex = 10f + open * 26f, lift = 6f + open * 10f;
		for (int k = 0; k <= n; k++) {
			float u = k / (float) n;
			float wave = (float) Math.sin(phase - 0.9f - u * 1.7f);
			float ripple = (float) Math.sin(phase * 1.6f - u * 5.2f) * (0.012f + open * 0.03f) * u;
			pts[k][0] = x;
			pts[k][1] = y;
			pts[k][2] = z;
			pts[k][3] = ripple;
			float yaw = (float) Math.toRadians(14f * u + flex * u * wave);
			float pitch = (float) Math.toRadians(-lift * u * (float) Math.sin(phase - 1.3f - u * 1.6f) + 9f * u * u);
			float c = (float) Math.cos(pitch);
			x += (float) Math.cos(yaw) * c * len;
			z += (float) Math.sin(yaw) * c * len;
			y += (float) Math.sin(pitch) * len;
		}
		return pts;
	}

	private static void wingMesh(PoseStack.Pose pose, VertexConsumer vc, float phase, float open, int light, int color) {
		float[][] p = wingShape(phase, open);
		int n = p.length - 1;
		float top = -0.50f, bot = 0.20f;
		for (int i = 0; i < n; i++) {
			float ua = i / (float) n, ub = (i + 1) / (float) n;
			float[] a = p[i], b = p[i + 1];
			float dx = b[0] - a[0], dz = b[2] - a[2], len = Math.max(1e-4f, (float) Math.sqrt(dx * dx + dz * dz));
			float nx = -dz / len, nz = dx / len;
			float ta = a[1] + top, tb = b[1] + top, ba = a[1] + bot + a[3], bb = b[1] + bot + b[3];
			vc.addVertex(pose, a[0], ta, a[2]).setColor(color).setUv(ua, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, 0f, nz);
			vc.addVertex(pose, a[0], ba, a[2]).setColor(color).setUv(ua, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, 0f, nz);
			vc.addVertex(pose, b[0], bb, b[2]).setColor(color).setUv(ub, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, 0f, nz);
			vc.addVertex(pose, b[0], tb, b[2]).setColor(color).setUv(ub, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, 0f, nz);
			vc.addVertex(pose, b[0], tb, b[2]).setColor(color).setUv(ub, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -nx, 0f, -nz);
			vc.addVertex(pose, b[0], bb, b[2]).setColor(color).setUv(ub, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -nx, 0f, -nz);
			vc.addVertex(pose, a[0], ba, a[2]).setColor(color).setUv(ua, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -nx, 0f, -nz);
			vc.addVertex(pose, a[0], ta, a[2]).setColor(color).setUv(ua, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, -nx, 0f, -nz);
		}
	}

	private static int haloColor(String style) {
		return switch (style) {
			case "surge" -> 0xC66BFF;
			case "fire" -> 0xFF8C28;
			case "enchanted" -> 0x78C8FF;
			case "void" -> 0x6A2BD9;
			case "rose" -> 0xFF6FAE;
			case "crown" -> 0xFFD54A;
			case "orbit" -> 0x7AF0FF;
			case "rainbow" -> java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 4000) / 4000f, 0.65f, 1f) & 0xFFFFFF;
			default -> 0xFFEC9A;
		};
	}

	private void halo(PoseStack ps, SubmitNodeCollector q, String style, float age, boolean helmet) {
		int rgb = haloColor(style);
		ps.pushPose();
		getParentModel().head.translateAndRotate(ps);
		ps.translate(0f, (helmet ? -0.74f : -0.66f) + 0.03f * (float) Math.sin(age * 0.07), 0f);
		ps.mulPose(Axis.XP.rotationDegrees(-10f));
		ps.mulPose(Axis.YP.rotationDegrees(age * 1.6f));
		boolean crown = "crown".equals(style);
		q.submitCustomGeometry(ps, RenderTypes.entityTranslucent(WHITE), (pose, vc) -> torus(pose, vc, crown ? 0.22f : 0.26f, crown ? 0.025f : 0.032f, 40, 10, 0xFF000000 | rgb));
		if (crown) {
			q.submitCustomGeometry(ps, RenderTypes.entityTranslucent(WHITE), (pose, vc) -> {
				for (int i = 0; i < 8; i++) {
					double a0 = i * Math.PI / 4 - 0.22, a1 = i * Math.PI / 4 + 0.22, am = i * Math.PI / 4;
					float h = i % 2 == 0 ? 0.17f : 0.11f;
					float x0 = (float) Math.cos(a0) * 0.22f, z0 = (float) Math.sin(a0) * 0.22f, x1 = (float) Math.cos(a1) * 0.22f, z1 = (float) Math.sin(a1) * 0.22f;
					float xm = (float) Math.cos(am) * 0.22f, zm = (float) Math.sin(am) * 0.22f;
					int c = 0xFF000000 | rgb;
					for (int side = 0; side < 2; side++) {
						float ny = 0.3f, nx = (float) Math.cos(am) * (side == 0 ? 1 : -1), nz = (float) Math.sin(am) * (side == 0 ? 1 : -1);
						vc.addVertex(pose, x0, 0f, z0).setColor(c).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
						vc.addVertex(pose, side == 0 ? x1 : xm, side == 0 ? 0f : -h, side == 0 ? z1 : zm).setColor(c).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
						vc.addVertex(pose, side == 0 ? xm : x1, side == 0 ? -h : 0f, side == 0 ? zm : z1).setColor(c).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
						vc.addVertex(pose, xm, -h, zm).setColor(c).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, nx, ny, nz);
					}
					int gem = i % 2 == 0 ? 0xFFFF4D6A : 0xFF4DB8FF;
					cube(pose, vc, xm * 1.02f, -h - 0.015f, zm * 1.02f, 0.018f, gem);
				}
			});
		}
		if ("orbit".equals(style)) {
			q.submitCustomGeometry(ps, RenderTypes.entityTranslucent(WHITE), (pose, vc) -> {
				for (int i = 0; i < 3; i++) {
					double a = age * 0.09 + i * Math.PI * 2 / 3;
					float x = (float) Math.cos(a) * 0.36f, z = (float) Math.sin(a) * 0.36f, y = (float) Math.sin(age * 0.12 + i) * 0.05f;
					cube(pose, vc, x, y, z, 0.03f, i == 1 ? 0xFFFFFFFF : 0xFF000000 | rgb);
				}
			});
		}
		q.submitCustomGeometry(ps, RenderTypes.entityTranslucent(WHITE), (pose, vc) -> {
			ring(pose, vc, 0.29f, 0.36f, 0.001f, rgb, 34, 40);
			ring(pose, vc, 0.22f, 0.29f, 0.001f, rgb, 70, 40);
			ring(pose, vc, 0.16f, 0.23f, 0.001f, rgb, 34, 40);
		});
		ps.popPose();
	}

	private static void torus(PoseStack.Pose pose, VertexConsumer vc, float R, float r, int seg, int tube, int argb) {
		for (int i = 0; i < seg; i++) {
			double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
			for (int j = 0; j < tube; j++) {
				double b0 = j * Math.PI * 2 / tube, b1 = (j + 1) * Math.PI * 2 / tube;
				float[] p00 = tp(R, r, a0, b0), p10 = tp(R, r, a1, b0), p11 = tp(R, r, a1, b1), p01 = tp(R, r, a0, b1);
				float[] n00 = tn(a0, b0), n10 = tn(a1, b0), n11 = tn(a1, b1), n01 = tn(a0, b1);
				tv(pose, vc, p00, n00, argb);
				tv(pose, vc, p01, n01, argb);
				tv(pose, vc, p11, n11, argb);
				tv(pose, vc, p10, n10, argb);
			}
		}
	}

	private static void cube(PoseStack.Pose pose, VertexConsumer vc, float cx, float cy, float cz, float h, int argb) {
		float[][] n = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		for (float[] f : n) {
			float[] u = f[0] != 0 ? new float[]{0, 1, 0} : new float[]{1, 0, 0};
			float[] v = {f[1] * u[2] - f[2] * u[1], f[2] * u[0] - f[0] * u[2], f[0] * u[1] - f[1] * u[0]};
			for (int k = 0; k < 4; k++) {
				float su = (k == 0 || k == 3) ? -1 : 1, sv = (k < 2) ? -1 : 1;
				vc.addVertex(pose, cx + (f[0] + u[0] * su + v[0] * sv) * h, cy + (f[1] + u[1] * su + v[1] * sv) * h, cz + (f[2] + u[2] * su + v[2] * sv) * h)
					.setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, f[0], f[1], f[2]);
			}
		}
	}

	private static float[] tp(float R, float r, double a, double b) {
		double x = (R + r * Math.cos(b)) * Math.cos(a), z = (R + r * Math.cos(b)) * Math.sin(a), y = r * Math.sin(b);
		return new float[]{(float) x, (float) y, (float) z};
	}

	private static float[] tn(double a, double b) {
		return new float[]{(float) (Math.cos(b) * Math.cos(a)), (float) Math.sin(b), (float) (Math.cos(b) * Math.sin(a))};
	}

	private static void tv(PoseStack.Pose pose, VertexConsumer vc, float[] p, float[] n, int argb) {
		vc.addVertex(pose, p[0], p[1], p[2]).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, n[0], n[1], n[2]);
	}

	private static void ring(PoseStack.Pose pose, VertexConsumer vc, float rIn, float rOut, float y, int rgb, int alpha, int seg) {
		int argb = (Math.min(255, Math.max(0, alpha)) << 24) | (rgb & 0xFFFFFF);
		for (int i = 0; i < seg; i++) {
			double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
			float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
			float[][] v = {{c0 * rIn, s0 * rIn}, {c0 * rOut, s0 * rOut}, {c1 * rOut, s1 * rOut}, {c1 * rIn, s1 * rIn}};
			for (float[] p : v) vc.addVertex(pose, p[0], y, p[1]).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0f, 1f, 0f);
			for (int k = 3; k >= 0; k--) vc.addVertex(pose, v[k][0], y, v[k][1]).setColor(argb).setUv(0.5f, 0.5f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT).setNormal(pose, 0f, -1f, 0f);
		}
	}
}
