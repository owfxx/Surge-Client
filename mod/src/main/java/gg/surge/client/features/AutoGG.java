package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;

public final class AutoGG {
	private static final String[] TRIGGERS = {"1st Killer -", "Winner:", "Winners:", "WINNER!", "VICTORY!", "Victory!", "won the game", "You won",
		"Top Survivors", "1st Place -", "Game Over!", "has won the", "won the match", "Winning Team"};
	private static long lastGg;

	private AutoGG() {}

	private static final String[] KILL_WORDS = {"slain by", "killed by", "shot by", "by ", "eliminated", "knocked", "fell", "void"};
	private static long lastKill;

	public static void onKill() {
		if (!Mods.AUTO_GG.enabled() || !Mods.GG_KILLS.on()) return;
		if (System.currentTimeMillis() - lastKill < 3000) return;
		lastKill = System.currentTimeMillis();
		say();
	}

	public static void check(String text) {
		if (!Mods.AUTO_GG.enabled() || text == null) return;
		Minecraft mc = Minecraft.getInstance();
		if (Mods.GG_KILLS.on() && mc.player != null) {
			String me = mc.getUser().getName();
			int at = text.indexOf(me);
			if (at > 0 && !text.startsWith("<") && !text.contains(":")) {
				String lower = text.toLowerCase();
				for (String w : KILL_WORDS) if (lower.contains(w)) { onKill(); return; }
			}
		}
		boolean hit = false;
		for (String t : TRIGGERS) if (text.contains(t)) { hit = true; break; }
		if (!hit || System.currentTimeMillis() - lastGg < 10_000) return;
		lastGg = System.currentTimeMillis();
		say();
	}

	private static void say() {
		long delay = Math.round(Mods.GG_DELAY.get() * 1000);
		String msg = Mods.GG_TEXT.get();
		new Thread(() -> {
			try { Thread.sleep(delay); } catch (InterruptedException ignored) { }
			Minecraft mc = Minecraft.getInstance();
			mc.execute(() -> { if (mc.getConnection() != null) mc.getConnection().sendChat(msg); });
		}, "Surge AutoGG").start();
	}
}
