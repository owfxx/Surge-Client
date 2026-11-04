package gg.surge.client.cosmetics;

import gg.surge.client.emotes.Emotes;

public record CapturedLook(String cape, String wings, String halo, Emotes.Emote emote, float emoteTime, boolean emotePreview) {}
