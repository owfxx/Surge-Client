package gg.surge.client.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class Sales {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static final class Sale {
		public String target = "all";
		public int percent = 20;
		public long until;

		public boolean active() { return percent > 0 && (until == 0 || System.currentTimeMillis() < until); }
	}

	private static List<Sale> local = new ArrayList<>();
	private static List<Sale> remote = new ArrayList<>();
	private static boolean loaded;

	private Sales() {}

	private static Path file() { return Wallet.home().resolve("sales.json"); }

	private static List<Sale> parse(JsonElement root) {
		List<Sale> out = new ArrayList<>();
		if (root == null || !root.isJsonObject()) return out;
		JsonArray arr = root.getAsJsonObject().getAsJsonArray("sales");
		if (arr == null) return out;
		for (JsonElement e : arr) {
			try { out.add(GSON.fromJson(e, Sale.class)); } catch (Exception ignored) { }
		}
		return out;
	}

	public static void load() {
		loaded = true;
		try {
			if (Files.exists(file())) local = parse(JsonParser.parseString(Files.readString(file())));
		} catch (Exception e) {
			local = new ArrayList<>();
		}
		CompletableFuture.runAsync(() -> {
			List<Sale> r = parse(Wallet.fetchRemote());
			remote = r;
		});
	}

	public static void applyRemote(JsonElement root) {
		List<Sale> r = parse(root);
		if (root != null) remote = r;
	}

	public static List<Sale> local() {
		if (!loaded) load();
		return local;
	}

	public static void save() {
		try {
			JsonObject o = new JsonObject();
			o.add("sales", GSON.toJsonTree(local));
			Files.writeString(file(), GSON.toJson(o));
		} catch (Exception ignored) { }
	}

	private static boolean matches(Sale s, Shop.Item item) {
		return switch (s.target) {
			case "all" -> true;
			case "cosmetic", "emote" -> item.category().equals(s.target);
			default -> s.target.startsWith("sub:") ? item.sub().equals(s.target.substring(4)) : item.id().equals(s.target);
		};
	}

	public static int discount(Shop.Item item) {
		if (!loaded) load();
		int best = 0;
		for (List<Sale> list : List.of(local, remote)) {
			for (Sale s : list) if (s.active() && matches(s, item)) best = Math.max(best, Math.min(90, s.percent));
		}
		return best;
	}

	public static String describe(Sale s) {
		String what = switch (s.target) {
			case "all" -> "Everything";
			case "cosmetic" -> "All cosmetics";
			case "emote" -> "All emotes";
			default -> {
				if (s.target.startsWith("sub:")) yield "All " + s.target.substring(4);
				Shop.Item i = Shop.byId(s.target);
				yield i == null ? s.target : i.name();
			}
		};
		String end = s.until == 0 ? "" : "  ·  ends in " + Math.max(1, (s.until - System.currentTimeMillis()) / 3600000L) + "h";
		return s.percent + "% off " + what + end;
	}
}
