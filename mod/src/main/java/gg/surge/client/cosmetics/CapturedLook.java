package gg.surge.client.cosmetics;

import gg.surge.client.emotes.Emotes;

public record CapturedLook(String cape, String wings, String halo, Emotes.Emote emote, float emoteTime, boolean emotePreview, String pet) {
	public CapturedLook(String cape, String wings, String halo, Emotes.Emote emote, float emoteTime, boolean emotePreview) {
		this(cape, wings, halo, emote, emoteTime, emotePreview, null);
	}
}
