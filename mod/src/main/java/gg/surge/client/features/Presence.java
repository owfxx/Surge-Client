package gg.surge.client.features;

import gg.surge.client.economy.Wallet;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

public final class Presence {
	private static String last = "";
	private static long since = System.currentTimeMillis();
	private static int tick;
	private static final long START = System.currentTimeMillis();

	private Presence() {}

	public static void tick(Minecraft mc) {
		if (++tick % 20 != 0) return;
		String state, server = "";
		boolean picking = mc.screen instanceof net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen || mc.screen instanceof gg.surge.client.gui.ServerTabsScreen
			|| mc.screen instanceof net.minecraft.client.gui.screens.ConnectScreen;
		if (mc.level == null) state = picking ? "servers" : "menu";
		else if (mc.getCurrentServer() != null) { state = "server"; server = mc.getCurrentServer().ip; }
		else state = "singleplayer";
		boolean on = gg.surge.client.mods.Mods.DISCORD.enabled();
		boolean showServer = gg.surge.client.mods.Mods.DISCORD_SERVER.on(), showPack = gg.surge.client.mods.Mods.DISCORD_PACK.on();
		String key = state + "|" + server + "|" + on + showServer + showPack;
		String pack = System.getProperty("surge.pack", "").replace('_', ' ').trim();
		String details = switch (state) {
			case "server" -> showServer && !server.isEmpty() ? "Playing on " + server : "Playing multiplayer";
			case "singleplayer" -> "Playing singleplayer";
			case "servers" -> "Picking a server";
			default -> "In the menus";
		};
		String line = showPack && !pack.isEmpty() ? "Modpack: " + pack : "Surge Client";
		DiscordRpc.update(on, new DiscordRpc.Activity(details, line, state.equals("menu") || state.equals("servers") ? START : since));
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
