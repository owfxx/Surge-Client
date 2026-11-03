package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;

public final class AutoGG {
	private static final String[] TRIGGERS = {"1st Killer -", "Winner:", "Winners:", "WINNER!", "VICTORY!", "Victory!", "won the game", "You won",
		"Top Survivors", "1st Place -", "Game Over!", "has won the", "won the match", "Winning Team"};
	private static long lastGg;

	private AutoGG() {}

	public static void check(String text) {
		if (!Mods.AUTO_GG.enabled() || text == null) return;
		boolean hit = false;
		for (String t : TRIGGERS) if (text.contains(t)) { hit = true; break; }
		if (!hit || System.currentTimeMillis() - lastGg < 10_000) return;
		lastGg = System.currentTimeMillis();
		long delay = Math.round(Mods.GG_DELAY.get() * 1000);
		String msg = Mods.GG_TEXT.get();
		new Thread(() -> {
			try { Thread.sleep(delay); } catch (InterruptedException ignored) { }
			Minecraft mc = Minecraft.getInstance();
			mc.execute(() -> { if (mc.getConnection() != null) mc.getConnection().sendChat(msg); });
		}, "Surge AutoGG").start();
	}
}
