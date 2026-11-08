package gg.surge.client.cosmetics;

import gg.surge.client.economy.Wallet;
import gg.surge.client.mods.Mods;
import gg.surge.client.net.SurgeNet;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class Cosmetics {
	public static volatile String previewSub, previewKey;

	private Cosmetics() {}

	public static void preview(String sub, String key) {
		previewSub = sub;
		previewKey = key;
	}

	public static String equipped(String type, String key) {
		return key != null && !"none".equals(key) && Wallet.get().owns(type + ":" + key) ? key : "none";
	}

	public static String wire(String type, String key) {
		String eq = equipped(type, key);
		if (!"cape".equals(type) || "none".equals(eq) || "image".equals(eq)) return eq;
		return CapeTextures.resolve(eq);
	}

	public static String forPlayer(Player p, String type) {
		Minecraft mc = Minecraft.getInstance();
		if (p == mc.player) {
			if (previewSub != null && previewSub.equals(type) && Preview.clean) return previewKey;
			if (!Mods.COSMETICS.enabled()) return null;
			var c = gg.surge.client.config.SurgeConfig.get();
			String key = switch (type) { case "wings" -> c.wings; case "halo" -> c.halo; default -> c.cape; };
			String eq = equipped(type, key);
			if ("cape".equals(type) && "image".equals(eq)) return "image_" + gg.surge.client.net.Social.myId();
			return "none".equals(eq) ? null : eq;
		}
		if (!Mods.COSMETICS.enabled() || !Mods.COS_OTHERS.on()) return null;
		UUID id = p.getUUID();
		SurgeNet.Remote r = SurgeNet.remote(id);
		if (r == null) return null;
		String v = switch (type) { case "wings" -> r.wings; case "halo" -> r.halo; default -> r.cape; };
		if ("image".equals(v) || (v != null && v.startsWith("image_"))) return "image_" + id.toString().replace("-", "");
		return v == null || "none".equals(v) ? null : v;
	}
}
