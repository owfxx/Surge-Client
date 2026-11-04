package gg.surge.client.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class Wallet {
	public static final int STARTING_COINS = 1000;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Wallet instance;

	public int coins = STARTING_COINS;
	public Set<String> owned = new LinkedHashSet<>();
	public Set<String> redeemed = new LinkedHashSet<>();
	public String lastDaily = "";
	public int streak = 0;
	public int playSeconds = 0;

	public static Path home() {
		String prop = System.getProperty("surge.home");
		Path p = prop != null && !prop.isBlank() ? Path.of(prop) : FabricLoader.getInstance().getConfigDir().resolve("surge");
		try { Files.createDirectories(p); } catch (Exception ignored) { }
		return p;
	}

	private static Path file() { return home().resolve("wallet.json"); }

	public static Wallet get() {
		if (instance == null) {
			try (Reader r = Files.newBufferedReader(file())) {
				instance = GSON.fromJson(r, Wallet.class);
			} catch (Exception e) {
				instance = null;
			}
			if (instance == null) instance = new Wallet();
			if (instance.owned == null) instance.owned = new LinkedHashSet<>();
			if (instance.redeemed == null) instance.redeemed = new LinkedHashSet<>();
		}
		return instance;
	}

	public void save() {
		try (Writer w = Files.newBufferedWriter(file())) {
			GSON.toJson(this, w);
		} catch (Exception ignored) { }
	}

	public boolean owns(String itemId) { return owned.contains(itemId); }

	public String buy(Shop.Item item) {
		if (owns(item.id())) return null;
		if (Locks.locked(item)) return "You can't buy this";
		int price = Shop.price(item);
		if (coins < price) return "Not enough coins";
		coins -= price;
		owned.add(item.id());
		save();
		return null;
	}

	public static String normalize(String code) {
		return code.toUpperCase().replaceAll("[^A-Z0-9]", "");
	}

	static String sha256(String s) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static volatile JsonObject remoteCache;
	private static volatile long remoteAt;

	private static JsonObject fetchJson(String url) {
		if (url == null || url.isBlank()) return null;
		try {
			HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
			HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString());
			if (res.statusCode() == 200) {
				JsonElement e = JsonParser.parseString(res.body());
				if (e.isJsonObject()) return e.getAsJsonObject();
			}
		} catch (Exception ignored) { }
		return null;
	}

	private static void addAll(JsonObject into, JsonObject from, String key) {
		if (from == null || !from.has(key) || !from.get(key).isJsonArray()) return;
		if (!into.has(key)) into.add(key, new JsonArray());
		into.getAsJsonArray(key).addAll(from.getAsJsonArray(key));
	}

	public static JsonElement fetchRemote() {
		if (remoteCache != null && System.currentTimeMillis() - remoteAt < 60_000) return remoteCache;
		String repo = System.getProperty("surge.repo", "owfxx/Surge-Client");
		if (repo.isBlank()) repo = "owfxx/Surge-Client";
		String codesUrl = System.getProperty("surge.codesUrl", "");
		if (codesUrl.isBlank()) codesUrl = "https://github.com/" + repo + "/releases/download/data/codes.json";
		JsonObject gh = fetchJson(codesUrl);
		JsonObject svc = fetchJson(gg.surge.client.net.SurgeNet.publicUrl() + "/v1/public");
		if (gh == null && svc == null) return remoteCache;
		JsonObject out = new JsonObject();
		for (String k : new String[]{"codes", "sales", "servers"}) { addAll(out, gh, k); addAll(out, svc, k); }
		if (svc != null && svc.has("locked")) out.add("locked", svc.get("locked"));
		else if (gh != null && gh.has("locked")) out.add("locked", gh.get("locked"));
		remoteCache = out;
		remoteAt = System.currentTimeMillis();
		return out;
	}

	public record Reward(int coins, java.util.List<String> items, int error) {
		public boolean ok() { return error == 0; }
	}

	private record Found(int coins, java.util.List<String> items) {}

	private static java.util.List<Path> codeDirs() {
		java.util.List<Path> dirs = new java.util.ArrayList<>();
		dirs.add(home());
		String appdata = System.getenv("APPDATA");
		if (appdata != null) dirs.add(Path.of(appdata, ".surgeclient"));
		dirs.add(Path.of(System.getProperty("user.home"), ".surgeclient"));
		dirs.add(FabricLoader.getInstance().getConfigDir().resolve("surge"));
		return dirs;
	}

	private static Found entry(JsonObject o) {
		int c = o.has("coins") && !o.get("coins").isJsonNull() ? o.get("coins").getAsInt() : 0;
		java.util.List<String> items = new java.util.ArrayList<>();
		if (o.has("items") && o.get("items").isJsonArray()) for (JsonElement i : o.getAsJsonArray("items")) items.add(i.getAsString());
		return new Found(c, items);
	}

	private static Found find(JsonElement root, String hash, String code) {
		if (root == null || !root.isJsonObject()) return null;
		JsonArray codes = root.getAsJsonObject().getAsJsonArray("codes");
		if (codes == null) return null;
		for (JsonElement e : codes) {
			if (!e.isJsonObject()) continue;
			JsonObject o = e.getAsJsonObject();
			if (o.has("hash") && hash.equalsIgnoreCase(o.get("hash").getAsString())) return entry(o);
			if (o.has("code") && code.equals(normalize(o.get("code").getAsString()))) return entry(o);
		}
		return null;
	}

	public CompletableFuture<Reward> redeem(String rawCode) {
		String code = normalize(rawCode);
		if (code.isEmpty()) return CompletableFuture.completedFuture(new Reward(0, java.util.List.of(), 1));
		String hash = sha256(code);
		if (redeemed.contains(hash)) return CompletableFuture.completedFuture(new Reward(0, java.util.List.of(), 2));
		return CompletableFuture.supplyAsync(() -> {
			Found f = null;
			for (Path dir : codeDirs()) {
				for (String name : new String[]{"codes.json", "admin.json"}) {
					if (f != null) break;
					try {
						Path file = dir.resolve(name);
						if (Files.exists(file)) f = find(JsonParser.parseString(Files.readString(file)), hash, code);
					} catch (Exception ignored) { }
				}
			}
			if (f == null) {
				try (var in = Wallet.class.getResourceAsStream("/assets/surge/codes.json")) {
					if (in != null) f = find(JsonParser.parseString(new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)), hash, code);
				} catch (Exception ignored) { }
			}
			if (f == null) { remoteAt = 0; f = find(fetchRemote(), hash, code); }
			return f;
		}).thenApply(f -> {
			if (f == null) return new Reward(0, java.util.List.of(), 1);
			synchronized (this) {
				if (redeemed.contains(hash)) return new Reward(0, java.util.List.of(), 2);
				java.util.List<String> fresh = new java.util.ArrayList<>();
				for (String i : f.items()) if (!owned.contains(i)) fresh.add(i);
				if (f.coins() <= 0 && fresh.isEmpty()) return new Reward(0, java.util.List.of(), 3);
				redeemed.add(hash);
				coins += Math.max(0, f.coins());
				owned.addAll(fresh);
				save();
				return new Reward(Math.max(0, f.coins()), fresh, 0);
			}
		});
	}
}
