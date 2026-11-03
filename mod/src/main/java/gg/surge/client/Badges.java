package gg.surge.client;

import gg.surge.client.mods.Mods;
import net.minecraft.network.chat.Component;

/**
 * The Surge badge shown before player names in the tab list and on nametags.
 * The glyph  is mapped to textures/font/badge.png by assets/minecraft/font/default.json.
 */
public final class Badges {
	public static final String GLYPH = "";

	private Badges() {}

	/** Only on Surge players: you, and others the online service knows are using Surge right now. */
	public static boolean show(java.util.UUID id) {
		if (!Mods.BADGE.enabled() || id == null) return false;
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		boolean you = mc.player != null && mc.player.getUUID().equals(id);
		if (you) return true;
		return Mods.BADGE_WHO.is("Everyone") && gg.surge.client.net.SurgeNet.isSurge(id);
	}

	/** Badge + space + name. The badge is forced white so team colors don't tint it. */
	public static Component prefix(Component name) {
		if (name.getString().contains(GLYPH)) return name;
		return Component.empty()
			.append(Component.literal(GLYPH).withStyle(s -> s.withColor(0xFFFFFF)))
			.append(Component.literal(" "))
			.append(name);
	}
}
