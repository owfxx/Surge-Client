package gg.surge.client;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

public final class Badges {
	public static final String GLYPH = "";
	public static final List<String> KINDS = List.of("staff", "booster", "beta", "media", "vip", "early", "dev");
	public static final List<String> NAMES = List.of("Staff", "Server Booster", "Beta Tester", "Creator", "VIP", "Early Supporter", "Developer");

	private Badges() {}

	public static boolean show(UUID id) {
		if (id == null) return false;
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		boolean you = mc.player != null && mc.player.getUUID().equals(id);
		if (you) return true;
		return gg.surge.client.net.SurgeNet.isSurge(id);
	}

	public static String kind(UUID id) {
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
		if (id == null) return "";
		if (mc.player != null && mc.player.getUUID().equals(id)) return gg.surge.client.net.Inbox.badge;
		var r = gg.surge.client.net.SurgeNet.remote(id);
		return r == null ? "" : r.badge;
	}

	public static String glyph(String kind) {
		if ("owner".equals(kind)) {
			long t = System.currentTimeMillis() % 3200;
			return String.valueOf((char) (0xE010 + (t < 880 ? (int) (t / 110) : 0)));
		}
		int i = kind == null ? -1 : KINDS.indexOf(kind);
		return i < 0 ? GLYPH : String.valueOf((char) (0xE001 + i));
	}

	private static boolean hasBadge(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == 0xE000 || (c >= 0xE001 && c <= 0xE017)) return true;
		}
		return false;
	}

	public static Component prefix(Component name) {
		return prefix(null, name);
	}

	public static Component prefix(UUID id, Component name) {
		if (hasBadge(name.getString())) return name;
		return Component.empty()
			.append(gg.surge.client.features.Flags.icon(glyph(kind(id))))
			.append(Component.literal(" "))
			.append(name);
	}
}
