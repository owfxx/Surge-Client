package gg.surge.client.features;

import gg.surge.client.mods.Mods;

public final class FogTweaks {

	private FogTweaks() {}

	public static float finalScale() {
		if (!Mods.FOG.enabled()) return 1f;
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (mc.player != null && (mc.player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) || mc.player.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS))) return 1f;
		net.minecraft.world.level.material.FogType type = mc.gameRenderer.getMainCamera().getFluidInCamera();
		boolean none = Mods.FOG_NONE.on();
		return switch (type) {
			case WATER -> none || !Mods.FOG_WATER.on() ? -1f : Mods.FOG_WATER_DIST.get() / 100f;
			case LAVA -> none || !Mods.FOG_LAVA.on() ? -1f : 1f;
			case POWDER_SNOW -> none || !Mods.FOG_SNOW.on() ? -1f : 1f;
			default -> none || !Mods.FOG_WORLD.on() ? -1f : Mods.FOG_WORLD_DIST.get() / 100f;
		};
	}

	public static void color(org.joml.Vector4f c) {
		if (c == null || !Mods.FOG.enabled() || !Mods.FOG_TINT.on()) return;
		int rgb = Mods.FOG_COLOR.rgb(0);
		c.x = ((rgb >> 16) & 255) / 255f;
		c.y = ((rgb >> 8) & 255) / 255f;
		c.z = (rgb & 255) / 255f;
	}
}
