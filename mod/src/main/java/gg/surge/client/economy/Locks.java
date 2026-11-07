package gg.surge.client.economy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class Locks {
	public static final Set<String> DEFAULT = Set.of("cape:beta");

	private static Set<String> local;
	private static volatile Set<String> remote;
	private static boolean loaded;

	private Locks() {}

	private static Path file() { return Wallet.home().resolve("locks.json"); }

	private static Set<String> parse(JsonElement root) {
		if (root == null || !root.isJsonObject() || !root.getAsJsonObject().has("locked")) return null;
		Set<String> out = new LinkedHashSet<>();
		for (JsonElement e : root.getAsJsonObject().getAsJsonArray("locked")) out.add(e.getAsString());
		return out;
	}

	public static void load() {
		loaded = true;
		try {
			local = Files.exists(file()) ? parse(JsonParser.parseString(Files.readString(file()))) : null;
		} catch (Exception e) {
			local = null;
		}
		CompletableFuture.runAsync(() -> remote = parse(Wallet.fetchRemote()));
	}

	private static Set<String> active() {
		if (!loaded) load();
		if (local != null) return local;
		if (remote != null) return remote;
		return DEFAULT;
	}

	public static void applyRemote(com.google.gson.JsonElement root) {
		if (root != null) remote = parse(root);
	}

	public static boolean locked(Shop.Item item) {
		return active().contains(item.id());
	}

	public static void toggle(String id) {
		Set<String> s = new LinkedHashSet<>(active());
		if (!s.remove(id)) s.add(id);
		local = s;
		try {
			JsonObject o = new JsonObject();
			JsonArray a = new JsonArray();
			s.forEach(a::add);
			o.add("locked", a);
			Files.writeString(file(), o.toString());
		} catch (Exception ignored) { }
	}
}
