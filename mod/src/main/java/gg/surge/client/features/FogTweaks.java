package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.minecraft.client.renderer.fog.FogData;

public final class FogTweaks {
	private static final float FAR = 1_000_000f;

	private FogTweaks() {}

	public static void none(FogData d) {
		d.environmentalStart = FAR;
		d.environmentalEnd = FAR;
		d.renderDistanceStart = FAR;
		d.renderDistanceEnd = FAR;
	}

	public static void scale(FogData d, float f) {
		d.environmentalStart *= f;
		d.environmentalEnd *= f;
		d.renderDistanceStart *= f;
		d.renderDistanceEnd *= f;
	}

	public static void world(FogData d) {
		if (!Mods.FOG.enabled()) return;
		if (!Mods.FOG_WORLD.on()) none(d);
		else scale(d, Mods.FOG_WORLD_DIST.get() / 100f);
	}

	public static void water(FogData d) {
		if (!Mods.FOG.enabled()) return;
		if (!Mods.FOG_WATER.on()) none(d);
		else scale(d, Mods.FOG_WATER_DIST.get() / 100f);
	}

	public static void simple(FogData d, boolean keep) {
		if (Mods.FOG.enabled() && !keep) none(d);
	}

	public static void color(org.joml.Vector4f c) {
		if (c == null || !Mods.FOG.enabled() || !Mods.FOG_TINT.on()) return;
		int rgb = Mods.FOG_COLOR.rgb(0);
		c.x = ((rgb >> 16) & 255) / 255f;
		c.y = ((rgb >> 8) & 255) / 255f;
		c.z = (rgb & 255) / 255f;
	}
}
