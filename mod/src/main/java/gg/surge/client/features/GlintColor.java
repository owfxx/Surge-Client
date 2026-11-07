package gg.surge.client.features;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public final class GlintColor {
	private static final Map<Identifier, NativeImage> ORIGINAL = new HashMap<>();
	private static String applied = "";
	private static int ticks;

	private GlintColor() {}

	public static void tick(Minecraft mc) {
		if (++ticks % 100 == 0 && !applied.isEmpty()) { applied = "?"; apply(mc); }
	}

	private static NativeImage original(Minecraft mc, Identifier id) {
		NativeImage img = ORIGINAL.get(id);
		if (img != null) return img;
		try (InputStream in = mc.getResourceManager().getResource(id).orElseThrow().open()) {
			img = NativeImage.read(in);
			ORIGINAL.put(id, img);
			return img;
		} catch (Exception e) {
			return null;
		}
	}

	public static void apply(Minecraft mc) {
		if (mc == null) return;
		boolean on = Mods.GLINT.enabled() && Mods.GLINT_TINT.on();
		String want = on ? Mods.GLINT_COLOR.get() : "";
		if (want.equals(applied)) return;
		boolean chroma = on && Mods.GLINT_COLOR.chroma();
		int rgb = on ? (chroma ? 0xFFFFFF : Mods.GLINT_COLOR.rgb(0) & 0xFFFFFF) : 0;
		try {
			for (Identifier id : new Identifier[]{ItemRenderer.ENCHANTED_GLINT_ITEM, ItemRenderer.ENCHANTED_GLINT_ARMOR}) {
				NativeImage src = original(mc, id);
				AbstractTexture tex = mc.getTextureManager().getTexture(id);
				if (src == null || tex == null) continue;
				GpuTexture gpu = tex.getTexture();
				if (gpu == null || gpu.getWidth(0) != src.getWidth() || gpu.getHeight(0) != src.getHeight()) continue;
				NativeImage out = new NativeImage(src.getWidth(), src.getHeight(), false);
				int tr = (rgb >> 16) & 255, tg = (rgb >> 8) & 255, tb = rgb & 255;
				for (int y = 0; y < src.getHeight(); y++) for (int x = 0; x < src.getWidth(); x++) {
					int c = src.getPixel(x, y);
					if (!on) { out.setPixel(x, y, c); continue; }
					int a = (c >>> 24) & 255, l = Math.max((c >> 16) & 255, Math.max((c >> 8) & 255, c & 255));
					int r = tr * l / 255, g = tg * l / 255, b = tb * l / 255;
					if (chroma) {
						int h = java.awt.Color.HSBtoRGB((x + y) / (float) (src.getWidth() * 2), 0.8f, l / 255f);
						r = (h >> 16) & 255; g = (h >> 8) & 255; b = h & 255;
					}
					out.setPixel(x, y, (a << 24) | (r << 16) | (g << 8) | b);
				}
				RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpu, out);
				out.close();
			}
			applied = want;
		} catch (Throwable ignored) { }
	}
}
