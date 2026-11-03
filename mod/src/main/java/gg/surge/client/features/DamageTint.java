package gg.surge.client.features;

import com.mojang.blaze3d.platform.NativeImage;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

import java.lang.reflect.Field;

public final class DamageTint {
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

	public static void tick(Minecraft mc) {
		if (Mods.DAMAGE_TINT.enabled() && Mods.TINT_COLOR.chroma()) apply(mc);
	}

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
		}
	}
}
