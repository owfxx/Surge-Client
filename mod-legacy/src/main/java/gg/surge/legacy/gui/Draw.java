package gg.surge.legacy.gui;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.item.ItemStack;
import org.lwjgl.opengl.GL11;

public final class Draw {
	public static final int BG = 0xF2141418, PANEL = 0xFF1B1B20, CARD = 0xFF202026, CARD_HOVER = 0xFF2A2A31, EDGE = 0xFF2E2E35;
	public static final int TEXT = 0xFFF4F4F5, MUTED = 0xFFA1A1AA, FAINT = 0xFF6B6B74, ACCENT = 0xFF8F50F2, ACCENT_2 = 0xFFB483FF, GREEN = 0xFF4ADE80, RED = 0xFFF2545B;

	private Draw() {}

	public static TextRenderer font() {
		return MinecraftClient.getInstance().textRenderer;
	}

	public static void rect(int x, int y, int w, int h, int color) {
		if (w <= 0 || h <= 0) return;
		DrawableHelper.fill(x, y, x + w, y + h, color);
	}

	public static void round(int x, int y, int w, int h, int r, int color) {
		if (w <= 0 || h <= 0) return;
		r = Math.min(r, Math.min(w, h) / 2);
		if (r <= 0) { rect(x, y, w, h, color); return; }
		rect(x + r, y, w - 2 * r, h, color);
		rect(x, y + r, r, h - 2 * r, color);
		rect(x + w - r, y + r, r, h - 2 * r, color);
		for (int i = 0; i < r; i++) {
			double dy = r - i - 0.5;
			int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
			rect(x + inset, y + i, r - inset, 1, color);
			rect(x + w - r, y + i, r - inset, 1, color);
			rect(x + inset, y + h - 1 - i, r - inset, 1, color);
			rect(x + w - r, y + h - 1 - i, r - inset, 1, color);
		}
	}

	public static void roundBox(int x, int y, int w, int h, int r, int fill, int edge) {
		round(x, y, w, h, r, edge);
		round(x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
	}

	public static void outline(int x, int y, int w, int h, int color) {
		rect(x, y, w, 1, color);
		rect(x, y + h - 1, w, 1, color);
		rect(x, y + 1, 1, h - 2, color);
		rect(x + w - 1, y + 1, 1, h - 2, color);
	}

	public static void gradient(int x, int y, int w, int h, int top, int bottom) {
		GlStateManager.disableTexture();
		GlStateManager.enableBlend();
		GlStateManager.disableAlphaTest();
		GlStateManager.blendFuncSeparate(770, 771, 1, 0);
		GlStateManager.shadeModel(7425);
		Tessellator t = Tessellator.getInstance();
		BufferBuilder b = t.getBuffer();
		b.begin(7, VertexFormats.POSITION_COLOR);
		b.vertex(x + w, y, 0).color(r(top), g(top), bl(top), a(top)).next();
		b.vertex(x, y, 0).color(r(top), g(top), bl(top), a(top)).next();
		b.vertex(x, y + h, 0).color(r(bottom), g(bottom), bl(bottom), a(bottom)).next();
		b.vertex(x + w, y + h, 0).color(r(bottom), g(bottom), bl(bottom), a(bottom)).next();
		t.draw();
		GlStateManager.shadeModel(7424);
		GlStateManager.disableBlend();
		GlStateManager.enableAlphaTest();
		GlStateManager.enableTexture();
	}

	private static int a(int c) { return (c >>> 24) & 255; }
	private static int r(int c) { return (c >> 16) & 255; }
	private static int g(int c) { return (c >> 8) & 255; }
	private static int bl(int c) { return c & 255; }

	public static int alpha(int color, float k) {
		int a = Math.round(((color >>> 24) & 255) * Math.max(0, Math.min(1, k)));
		return (a << 24) | (color & 0xFFFFFF);
	}

	public static int lerp(int c1, int c2, float t) {
		t = Math.max(0, Math.min(1, t));
		int a = Math.round(a(c1) + (a(c2) - a(c1)) * t), r = Math.round(r(c1) + (r(c2) - r(c1)) * t);
		int g = Math.round(g(c1) + (g(c2) - g(c1)) * t), b = Math.round(bl(c1) + (bl(c2) - bl(c1)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public static void text(String s, int x, int y, int color, boolean shadow) {
		font().draw(s, (float) x, (float) y, color, shadow);
	}

	public static void center(String s, int cx, int y, int color, boolean shadow) {
		text(s, cx - font().getStringWidth(s) / 2, y, color, shadow);
	}

	public static int width(String s) {
		return font().getStringWidth(s);
	}

	public static String fit(String s, int w) {
		if (width(s) <= w) return s;
		while (s.length() > 1 && width(s + "...") > w) s = s.substring(0, s.length() - 1);
		return s + "...";
	}

	public static void item(ItemStack stack, int x, int y, float size) {
		if (stack == null) return;
		GlStateManager.pushMatrix();
		GlStateManager.translate((float) x, (float) y, 0f);
		GlStateManager.scale(size / 16f, size / 16f, 1f);
		GlStateManager.enableRescaleNormal();
		DiffuseLighting.enable();
		MinecraftClient.getInstance().getItemRenderer().renderInGuiWithOverrides(stack, 0, 0);
		DiffuseLighting.disable();
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableLighting();
		GlStateManager.disableDepthTest();
		GlStateManager.enableBlend();
		GlStateManager.popMatrix();
		GlStateManager.color(1f, 1f, 1f, 1f);
	}

	public static boolean hover(int mx, int my, int x, int y, int w, int h) {
		return mx >= x && my >= y && mx < x + w && my < y + h;
	}

	public static void scissor(int x, int y, int w, int h) {
		MinecraftClient mc = MinecraftClient.getInstance();
		net.minecraft.client.util.Window win = new net.minecraft.client.util.Window(mc);
		int s = win.getScaleFactor();
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor(x * s, mc.height - (y + h) * s, Math.max(0, w * s), Math.max(0, h * s));
	}

	public static void endScissor() {
		GL11.glDisable(GL11.GL_SCISSOR_TEST);
	}

	public static void toggle(int x, int y, boolean on, float anim) {
		int w = 22, h = 12;
		round(x, y, w, h, 6, lerp(0xFF3A3A42, ACCENT, anim));
		int kx = x + 2 + Math.round((w - 12) * anim);
		round(kx, y + 2, 8, 8, 4, 0xFFFFFFFF);
	}
}
