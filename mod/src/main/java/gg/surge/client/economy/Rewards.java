package gg.surge.client.economy;

import gg.surge.client.net.Social;
import net.minecraft.client.Minecraft;

import java.time.LocalDate;

public final class Rewards {
	public static final int PLAY_REWARD = 20, PLAY_MINUTES = 30;
	private static int tickCounter;

	private Rewards() {}

	public static int dailyAmount(int streak) {
		return 100 + 25 * (Math.min(7, Math.max(1, streak)) - 1);
	}

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;
		Wallet w = Wallet.get();
		if (mc.player.tickCount >= 100 && mc.player.tickCount % 40 == 0 && gg.surge.client.net.SurgeNet.hasServerTime()) daily(w);
		if (mc.isPaused()) return;
		if (++tickCounter < 20) return;
		tickCounter = 0;
		w.playSeconds++;
		if (w.playSeconds >= PLAY_MINUTES * 60) {
			w.playSeconds = 0;
			w.coins += PLAY_REWARD;
			Social.notice("+" + PLAY_REWARD + " Surge Coins", "Thanks for playing for " + PLAY_MINUTES + " minutes");
			w.save();
		} else if (w.playSeconds % 60 == 0) w.save();
	}

	private static void daily(Wallet w) {
		LocalDate now = java.time.Instant.ofEpochMilli(gg.surge.client.net.SurgeNet.serverTime()).atZone(java.time.ZoneOffset.UTC).toLocalDate();
		String today = now.toString();
		if (today.equals(w.lastDaily)) return;
		if (!w.lastDaily.isEmpty() && today.compareTo(w.lastDaily) < 0) return;
		String yesterday = now.minusDays(1).toString();
		w.streak = yesterday.equals(w.lastDaily) ? w.streak + 1 : 1;
		w.lastDaily = today;
		int amount = dailyAmount(w.streak);
		w.coins += amount;
		w.save();
		Social.notice("Daily reward: +" + amount + " coins", w.streak > 1 ? w.streak + " days in a row!" : "Come back tomorrow for more");
	}
}
