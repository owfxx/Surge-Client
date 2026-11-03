package gg.surge.client.features;

import gg.surge.client.economy.Wallet;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

/**
 * Tells the launcher what you're doing (menus, singleplayer, which server) so it can
 * show it on Discord. Written to presence.json in the Surge folder when it changes.
 */
public final class Presence {
	private static String last = "";
	private static long since = System.currentTimeMillis();
	private static int tick;

	private Presence() {}

	public static void tick(Minecraft mc) {
		if (++tick % 40 != 0) return;
		String state, server = "";
		if (mc.level == null) state = "menu";
		else if (mc.getCurrentServer() != null) { state = "server"; server = mc.getCurrentServer().ip; }
		else state = "singleplayer";
		boolean on = gg.surge.client.mods.Mods.DISCORD.enabled();
		boolean showServer = gg.surge.client.mods.Mods.DISCORD_SERVER.on(), showPack = gg.surge.client.mods.Mods.DISCORD_PACK.on();
		String key = state + "|" + server + "|" + on + showServer + showPack;
		if (key.equals(last)) return;
		if (!last.startsWith(state + "|" + server + "|")) since = System.currentTimeMillis();
		last = key;
		String json = "{\"state\":\"" + state + "\",\"server\":\"" + server.replace("\\", "").replace("\"", "") + "\",\"since\":" + since
			+ ",\"discord\":" + on + ",\"showServer\":" + showServer + ",\"showPack\":" + showPack + "}";
		CompletableFuture.runAsync(() -> {
			try { Files.writeString(Wallet.home().resolve("presence.json"), json); } catch (Exception ignored) { }
		});
	}
}
