package gg.surge.legacy.cosmetic;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

public class SurgeFeature implements FeatureRenderer<AbstractClientPlayerEntity> {
	private static final Map<Integer, float[]> FLAP = new HashMap<Integer, float[]>();
	private final PlayerEntityRenderer renderer;

	public SurgeFeature(PlayerEntityRenderer renderer) {
		this.renderer = renderer;
	}

	@Override
	public void render(AbstractClientPlayerEntity p, float limbAngle, float limbDistance, float tickDelta, float age, float headYaw, float headPitch, float scale) {
		if (p.isInvisible()) return;
		String wings = Cosmetics.forPlayer(p, "wings");
		String halo = Cosmetics.forPlayer(p, "halo");
		if (wings == null && halo == null) return;
		restore = p.getLightmapCoordinates(tickDelta);
		GlStateManager.pushMatrix();
		GlStateManager.disableCull();
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(770, 771);
		try {
			if (wings != null) wings(p, wings, age, Math.min(1f, limbDistance), p.abilities.flying);
			if (halo != null) halo(halo, age, p.getArmorSlot(3) != null);
		} finally {
			GlStateManager.enableCull();
			GlStateManager.disableBlend();
			GlStateManager.enableTexture();
			GlStateManager.enableLighting();
			GlStateManager.color(1f, 1f, 1f, 1f);
			GlStateManager.popMatrix();
		}
	}

	private void wings(AbstractClientPlayerEntity p, String style, float age, float moving, boolean flying) {
		boolean glows = !("angel".equals(style) || "gold".equals(style) || "bat".equals(style));
		float open = Math.min(1f, (flying ? 1f : moving) * 1.2f);
		float flapSpeed = 0.07f + open * 0.18f;
		float[] st = FLAP.get(p.getEntityId());
		if (st == null) { st = new float[]{0f, age, 0f}; FLAP.put(p.getEntityId(), st); }
		float dAge = age - st[1];
		if (dAge < 0f || dAge > 5f) dAge = 0f;
		st[0] += dAge * flapSpeed;
		st[1] = age;
		st[2] += (open - st[2]) * Math.min(1f, dAge * 0.15f);
		open = st[2];
		if (FLAP.size() > 256) FLAP.clear();
		float flap = (float) Math.sin(st[0]) * (4f + open * 22f);
		float spread = 62f - open * 34f;
		float lift = 10f + open * 6f;
		MinecraftClient.getInstance().getTextureManager().bindTexture(new Identifier("surge", "textures/cosmetic/wings_" + style + ".png"));
		if (glows) {
			GlStateManager.disableLighting();
			lightmap(true);
		}
		GlStateManager.color(1f, 1f, 1f, 1f);
		for (int side = -1; side <= 1; side += 2) {
			GlStateManager.pushMatrix();
			renderer.getModel().body.preRender(0.0625f);
			GlStateManager.translate(side * 0.06f, 0.14f, 0.15f);
			GlStateManager.scale((float) side, 1f, 1f);
			GlStateManager.rotate(-(spread + flap), 0f, 1f, 0f);
			GlStateManager.rotate(-lift - flap * 0.3f, 0f, 0f, 1f);
			wingQuad(0.02f, -0.46f, 1.32f, 0.30f, 0f);
			GlStateManager.popMatrix();
		}
		if (glows) lightmap(false);
	}

	private static int restore;

	private static void lightmap(boolean full) {
		if (full) com.mojang.blaze3d.platform.GLX.gl13MultiTexCoord2f(com.mojang.blaze3d.platform.GLX.lightmapTextureUnit, 240f, 240f);
		else com.mojang.blaze3d.platform.GLX.gl13MultiTexCoord2f(com.mojang.blaze3d.platform.GLX.lightmapTextureUnit, restore % 65536, restore / 65536);
	}

	private static void wingQuad(float x0, float y0, float x1, float y1, float z) {
		Tessellator t = Tessellator.getInstance();
		BufferBuilder b = t.getBuffer();
		b.begin(7, VertexFormats.POSITION_TEXTURE);
		int seg = 8;
		float bend = 0.16f;
		for (int i = 0; i < seg; i++) {
			float ua = i / (float) seg, ub = (i + 1) / (float) seg;
			float xa = x0 + (x1 - x0) * ua, xb = x0 + (x1 - x0) * ub;
			float za = z + bend * ua * ua, zb = z + bend * ub * ub;
			b.vertex(xa, y0, za).texture(ua, 0).next();
			b.vertex(xa, y1, za).texture(ua, 1).next();
			b.vertex(xb, y1, zb).texture(ub, 1).next();
			b.vertex(xb, y0, zb).texture(ub, 0).next();
		}
		t.draw();
	}

	private static int haloColor(String style) {
		if ("surge".equals(style)) return 0xC66BFF;
		if ("fire".equals(style)) return 0xFF8C28;
		if ("enchanted".equals(style)) return 0x78C8FF;
		if ("void".equals(style)) return 0x6A2BD9;
		if ("rose".equals(style)) return 0xFF6FAE;
		if ("crown".equals(style)) return 0xFFD54A;
		if ("orbit".equals(style)) return 0x7AF0FF;
		if ("rainbow".equals(style)) return java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 4000) / 4000f, 0.65f, 1f) & 0xFFFFFF;
		return 0xFFEC9A;
	}

	private void halo(String style, float age, boolean helmet) {
		int rgb = haloColor(style);
		GlStateManager.pushMatrix();
		renderer.getModel().head.preRender(0.0625f);
		GlStateManager.translate(0f, (helmet ? -0.74f : -0.66f) + 0.03f * (float) Math.sin(age * 0.07), 0f);
		GlStateManager.rotate(-10f, 1f, 0f, 0f);
		GlStateManager.rotate(age * 1.6f, 0f, 1f, 0f);
		GlStateManager.disableTexture();
		GlStateManager.disableLighting();
		lightmap(true);
		boolean crown = "crown".equals(style);
		Tessellator t = Tessellator.getInstance();
		BufferBuilder b = t.getBuffer();
		b.begin(7, VertexFormats.POSITION_COLOR);
		torus(b, crown ? 0.22f : 0.26f, crown ? 0.025f : 0.032f, 40, 10, rgb, 255);
		if (crown) {
			for (int i = 0; i < 8; i++) {
				double a0 = i * Math.PI / 4 - 0.22, a1 = i * Math.PI / 4 + 0.22, am = i * Math.PI / 4;
				float h = i % 2 == 0 ? 0.17f : 0.11f;
				float x0 = (float) Math.cos(a0) * 0.22f, z0 = (float) Math.sin(a0) * 0.22f, x1 = (float) Math.cos(a1) * 0.22f, z1 = (float) Math.sin(a1) * 0.22f;
				float xm = (float) Math.cos(am) * 0.22f, zm = (float) Math.sin(am) * 0.22f;
				v(b, x0, 0, z0, rgb, 255); v(b, x1, 0, z1, rgb, 255); v(b, xm, -h, zm, rgb, 255); v(b, xm, -h, zm, rgb, 255);
				cube(b, xm * 1.02f, -h - 0.015f, zm * 1.02f, 0.018f, i % 2 == 0 ? 0xFF4D6A : 0x4DB8FF);
			}
		}
		if ("orbit".equals(style)) {
			for (int i = 0; i < 3; i++) {
				double a = age * 0.09 + i * Math.PI * 2 / 3;
				cube(b, (float) Math.cos(a) * 0.36f, (float) Math.sin(age * 0.12 + i) * 0.05f, (float) Math.sin(a) * 0.36f, 0.03f, i == 1 ? 0xFFFFFF : rgb);
			}
		}
		ring(b, 0.29f, 0.36f, rgb, 34, 40);
		ring(b, 0.22f, 0.29f, rgb, 70, 40);
		ring(b, 0.16f, 0.23f, rgb, 34, 40);
		t.draw();
		lightmap(false);
		GlStateManager.popMatrix();
	}

	private static void v(BufferBuilder b, float x, float y, float z, int rgb, int a) {
		b.vertex(x, y, z).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, a).next();
	}

	private static void torus(BufferBuilder b, float R, float r, int seg, int tube, int rgb, int a) {
		for (int i = 0; i < seg; i++) {
			double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
			for (int j = 0; j < tube; j++) {
				double b0 = j * Math.PI * 2 / tube, b1 = (j + 1) * Math.PI * 2 / tube;
				float shade = 0.75f + 0.25f * (float) Math.sin(b0);
				int c = shadeColor(rgb, shade);
				tp(b, R, r, a0, b0, c, a);
				tp(b, R, r, a0, b1, c, a);
				tp(b, R, r, a1, b1, c, a);
				tp(b, R, r, a1, b0, c, a);
			}
		}
	}

	private static int shadeColor(int rgb, float k) {
		int r = Math.min(255, Math.round(((rgb >> 16) & 255) * k)), g = Math.min(255, Math.round(((rgb >> 8) & 255) * k)), bl = Math.min(255, Math.round((rgb & 255) * k));
		return (r << 16) | (g << 8) | bl;
	}

	private static void tp(BufferBuilder b, float R, float r, double a, double bb, int rgb, int al) {
		double x = (R + r * Math.cos(bb)) * Math.cos(a), z = (R + r * Math.cos(bb)) * Math.sin(a), y = r * Math.sin(bb);
		v(b, (float) x, (float) y, (float) z, rgb, al);
	}

	private static void cube(BufferBuilder b, float cx, float cy, float cz, float h, int rgb) {
		float[][] n = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
		for (float[] f : n) {
			float[] u = f[0] != 0 ? new float[]{0, 1, 0} : new float[]{1, 0, 0};
			float[] w = {f[1] * u[2] - f[2] * u[1], f[2] * u[0] - f[0] * u[2], f[0] * u[1] - f[1] * u[0]};
			for (int k = 0; k < 4; k++) {
				float su = (k == 0 || k == 3) ? -1 : 1, sv = (k < 2) ? -1 : 1;
				v(b, cx + (f[0] + u[0] * su + w[0] * sv) * h, cy + (f[1] + u[1] * su + w[1] * sv) * h, cz + (f[2] + u[2] * su + w[2] * sv) * h, rgb, 255);
			}
		}
	}

	private static void ring(BufferBuilder b, float rIn, float rOut, int rgb, int alpha, int seg) {
		for (int i = 0; i < seg; i++) {
			double a0 = i * Math.PI * 2 / seg, a1 = (i + 1) * Math.PI * 2 / seg;
			float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
			v(b, c0 * rIn, 0.001f, s0 * rIn, rgb, alpha);
			v(b, c0 * rOut, 0.001f, s0 * rOut, rgb, alpha);
			v(b, c1 * rOut, 0.001f, s1 * rOut, rgb, alpha);
			v(b, c1 * rIn, 0.001f, s1 * rIn, rgb, alpha);
		}
	}

	@Override
	public boolean combineTextures() {
		return false;
	}
}
