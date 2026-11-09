package gg.surge.client.economy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.surge.client.config.SurgeConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class WardrobeSync {
	private WardrobeSync() {}

	private static long checkedAt;

	public static void tick() {
		if (System.currentTimeMillis() - checkedAt < 1000) return;
		checkedAt = System.currentTimeMillis();
		Wallet.get();
		Path p = Wallet.home().resolve("wardrobe-pending.json");
		if (!Files.exists(p)) return;
		try {
			JsonObject o = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
			Files.deleteIfExists(p);
			SurgeConfig c = SurgeConfig.get();
			if (o.has("cape")) c.cape = o.get("cape").getAsString();
			if (o.has("wings")) c.wings = o.get("wings").getAsString();
			if (o.has("halo")) c.halo = o.get("halo").getAsString();
			if (o.has("idle")) c.idle = o.get("idle").getAsString();
			if (o.has("emotePages") && o.get("emotePages").isJsonArray()) {
				List<List<String>> pages = new ArrayList<>();
				for (JsonElement page : o.getAsJsonArray("emotePages")) {
					List<String> keys = new ArrayList<>();
					for (JsonElement k : page.getAsJsonArray()) keys.add(k.getAsString());
					pages.add(keys);
				}
				c.emotePages = pages;
			}
			SurgeConfig.save();
		} catch (Exception e) {
			try { Files.deleteIfExists(p); } catch (Exception ignored) { }
		}
	}
}
