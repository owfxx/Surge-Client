package gg.surge.client.cosmetics;

import gg.surge.client.economy.Wallet;
import gg.surge.client.mods.Mods;
import gg.surge.client.net.SurgeNet;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Which cosmetics a player shows: yours from the wardrobe (only owned ones), other
 * Surge players' from the online service. Wings, halos and capes are 3D models (CosmeticLayer).
 */
public final class Cosmetics {
	/** Wardrobe hover preview: "wings", "halo" or "cape" plus the style key, or null. */
	public static volatile String previewSub, previewKey;

	private Cosmetics() {}

	public static void preview(String sub, String key) {
		previewSub = sub;
		previewKey = key;
	}

	/** Your equipped style if you own it, else "none". */
	public static String equipped(String type, String key) {
		return key != null && !"none".equals(key) && Wallet.get().owns(type + ":" + key) ? key : "none";
	}

	/** The style a player shows for this type, or null for none. */
	public static String forPlayer(Player p, String type) {
		Minecraft mc = Minecraft.getInstance();
		if (p == mc.player) {
			if (previewSub != null && previewSub.equals(type)) return previewKey;
			if (!Mods.COSMETICS.enabled()) return null;
			var c = gg.surge.client.config.SurgeConfig.get();
			String key = switch (type) { case "wings" -> c.wings; case "halo" -> c.halo; default -> c.cape; };
			String eq = equipped(type, key);
			return "none".equals(eq) ? null : eq;
		}
		if (!Mods.COSMETICS.enabled() || !Mods.COS_OTHERS.on()) return null;
		UUID id = p.getUUID();
		SurgeNet.Remote r = SurgeNet.remote(id);
		if (r == null) return null;
		String v = switch (type) { case "wings" -> r.wings; case "halo" -> r.halo; default -> r.cape; };
		return v == null || "none".equals(v) ? null : v;
	}
}
