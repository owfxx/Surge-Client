package gg.surge.client.features;

import com.mojang.blaze3d.platform.NativeImage;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.lang.reflect.Field;

/**
 * Damage Tint: changes the color and strength of the flash entities get when hit.
 * The game draws that flash from a small "overlay" texture; its top half is the hurt
 * color. We repaint those pixels. Fields are found by type, so name changes can't break it.
 */
public final class DamageTint {
	/** Overlay coordinates the game uses for "hurt" (u = 0, v = 3). */
	public static final int HURT_OVERLAY = 3 << 16;

	private static DynamicTexture texture;
	private static int vanillaPixel;
	private static boolean painted;

	private DamageTint() {}

	public static boolean armor() {
		return Mods.DAMAGE_TINT.enabled() && Mods.TINT_ARMOR.on();
	}

	private static Object fieldOfType(Object owner, Class<?> type) throws IllegalAccessException {
		for (Class<?> k = owner.getClass(); k != null; k = k.getSuperclass()) {
			for (Field f : k.getDeclaredFields()) {
				if (type.isAssignableFrom(f.getType())) { f.setAccessible(true); return f.get(owner); }
			}
		}
		return null;
	}

	private static int lastColor;

	/** Chroma tint needs repainting as the color moves. */
	public static void tick(Minecraft mc) {
		if (Mods.DAMAGE_TINT.enabled() && Mods.TINT_COLOR.chroma()) apply(mc);
	}

	/** Repaints the hurt color from the settings (or back to vanilla). Safe to call any time on the render thread. */
	public static void apply(Minecraft mc) {
		try {
			if (texture == null) {
				Object overlay = fieldOfType(mc.gameRenderer, OverlayTexture.class);
				if (overlay == null) return;
				texture = (DynamicTexture) fieldOfType(overlay, DynamicTexture.class);
				if (texture == null) return;
				vanillaPixel = texture.getPixels().getPixel(0, 0);
			}
			boolean on = Mods.DAMAGE_TINT.enabled();
			if (!on && !painted) return;
			int color = vanillaPixel;
			if (on) {
				int rgb = Mods.TINT_COLOR.rgb(0) & 0xFFFFFF;
				// the shader keeps "alpha" of the original color, so strength = 255 - alpha
				int alpha = 255 - Math.round(255 * Math.max(5, Math.min(95, Mods.TINT_OPACITY.get())) / 100f);
				color = (alpha << 24) | rgb;
			}
			if (color == lastColor && painted == on) return;
			NativeImage px = texture.getPixels();
			for (int y = 0; y < 8; y++) for (int x = 0; x < 16; x++) px.setPixel(x, y, color);
			texture.upload();
			lastColor = color;
			painted = on;
		} catch (Throwable ignored) {
			// leave the vanilla tint alone if anything about the texture changed
		}
	}
}
