package gg.surge.client.emotes;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.EntityBoundSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class EmoteMusic {
	private record Playing(SoundInstance sound, Emotes.Emote emote, long start) {}

	private static final Map<UUID, Playing> PLAYING = new HashMap<>();

	private EmoteMusic() {}

	private static SoundEvent event(Emotes.Emote e) {
		return SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("surge", "emote." + e.music));
	}

	public static void start(Entity who, Emotes.Emote e, long start) {
		stop(who.getUUID());
		if (e == null || e.music == null || !Mods.EMOTE_MUSIC.on()) return;
		float vol = Mods.EMOTE_VOLUME.get() / 100f;
		if (vol <= 0) return;
		SoundInstance s = new EntityBoundSoundInstance(event(e), SoundSource.RECORDS, vol, 1f, who, start);
		Minecraft.getInstance().getSoundManager().play(s);
		PLAYING.put(who.getUUID(), new Playing(s, e, start));
	}

	public static void stop(UUID id) {
		Playing p = PLAYING.remove(id);
		if (p != null) Minecraft.getInstance().getSoundManager().stop(p.sound());
	}

	public static void tick(Minecraft mc) {
		if (mc.level == null) {
			if (!PLAYING.isEmpty()) { PLAYING.values().forEach(p -> mc.getSoundManager().stop(p.sound())); PLAYING.clear(); }
			return;
		}
		if (!Mods.EMOTE_MUSIC_OTHERS.on()) {
			for (UUID id : new java.util.ArrayList<>(PLAYING.keySet())) if (mc.player == null || !id.equals(mc.player.getUUID())) stop(id);
			return;
		}
		for (Player p : mc.level.players()) {
			if (p == mc.player || p.distanceToSqr(mc.player) > 48 * 48) continue;
			long[] start = new long[1];
			Emotes.Emote e = gg.surge.client.net.SurgeNet.remoteEmote(p.getUUID(), start);
			Playing cur = PLAYING.get(p.getUUID());
			if (e == null || e.music == null) { if (cur != null) stop(p.getUUID()); continue; }
			if (cur == null || cur.emote() != e || cur.start() != start[0]) start(p, e, start[0]);
		}
		for (UUID id : new java.util.ArrayList<>(PLAYING.keySet())) {
			if (mc.player != null && id.equals(mc.player.getUUID())) continue;
			if (mc.level.getPlayerByUUID(id) == null) stop(id);
		}
	}
}
