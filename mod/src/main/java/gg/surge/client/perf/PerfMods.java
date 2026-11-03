package gg.surge.client.perf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The performance mods the launcher installed into this modpack (Sodium, Lithium, …).
 * Mods can't be loaded or unloaded while the game runs, so switching one writes
 * surge-mods.json; the launcher applies it the next time you press Play.
 */
public final class PerfMods {
	public record Mod(String file, String title, boolean loaded) {}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static List<Mod> mods;
	private static Map<String, Boolean> pending;

	private PerfMods() {}

	private static Path gameDir() { return FabricLoader.getInstance().getGameDir(); }

	public static List<Mod> list() {
		if (mods != null) return mods;
		mods = new ArrayList<>();
		try {
			JsonObject inst = JsonParser.parseString(Files.readString(gameDir().resolve("instance.json"))).getAsJsonObject();
			for (JsonElement e : inst.getAsJsonArray("content")) {
				JsonObject c = e.getAsJsonObject();
				if (!c.has("locked") || !c.get("locked").getAsBoolean() || !"mod".equals(c.get("type").getAsString())) continue;
				String file = c.get("file").getAsString();
				String title = c.has("title") ? c.get("title").getAsString() : file;
				if (title.equalsIgnoreCase("Fabric API")) continue; // required, not optional
				boolean loaded = Files.exists(gameDir().resolve("mods").resolve(file));
				mods.add(new Mod(file, title, loaded));
			}
		} catch (Exception ignored) {
			// not started from the Surge launcher
		}
		return mods;
	}

	private static Map<String, Boolean> pending() {
		if (pending != null) return pending;
		pending = new LinkedHashMap<>();
		try {
			JsonObject o = JsonParser.parseString(Files.readString(gameDir().resolve("surge-mods.json"))).getAsJsonObject();
			for (Map.Entry<String, JsonElement> e : o.entrySet()) pending.put(e.getKey(), e.getValue().getAsBoolean());
		} catch (Exception ignored) { }
		return pending;
	}

	/** What the mod will be after the next launch. */
	public static boolean wanted(Mod m) {
		return pending().getOrDefault(m.file(), m.loaded());
	}

	public static boolean needsRestart(Mod m) {
		return wanted(m) != m.loaded();
	}

	public static void toggle(Mod m) {
		boolean next = !wanted(m);
		if (next == m.loaded()) pending().remove(m.file()); else pending().put(m.file(), next);
		try {
			Files.writeString(gameDir().resolve("surge-mods.json"), GSON.toJson(pending()));
		} catch (Exception ignored) { }
	}
}
