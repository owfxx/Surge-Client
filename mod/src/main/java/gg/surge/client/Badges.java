package gg.surge.client;

import gg.surge.client.mods.Mods;
import net.minecraft.network.chat.Component;

public final class Badges {
	public static final String GLYPH = "";

	private Badges() {}

	public static boolean show(java.util.UUID id) {
		if (!Mods.BADGE.enabled() || id == null) return false;
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		boolean you = mc.player != null && mc.player.getUUID().equals(id);
		if (you) return true;
		return Mods.BADGE_WHO.is("Everyone") && gg.surge.client.net.SurgeNet.isSurge(id);
	}

	public static Component prefix(Component name) {
		if (name.getString().contains(GLYPH)) return name;
		return Component.empty()
			.append(gg.surge.client.features.Flags.icon(GLYPH))
			.append(Component.literal(" "))
			.append(name);
	}
}
