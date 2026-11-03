package gg.surge.client.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.surge.client.config.SurgeConfig;
import gg.surge.client.cosmetics.Cosmetics;
import gg.surge.client.emotes.Emotes;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Talks to the Surge online service (see /server in the source). Sends what you wear
 * and which emote you're playing, and fetches the same for players around you. Without
 * a service URL everything still works, just only for you.
 */
public final class SurgeNet {
	/** What another Surge player wears. emoteStart is local time (ms) when we first saw the emote. */
	public static final class Remote {
		public volatile String wings = "none", halo = "none", cape = "none", emote = "none";
		public volatile long emoteAt, emoteStart;
	}

	private static final Map<UUID, Remote> PLAYERS = new ConcurrentHashMap<>();
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
	private static long lastPush, lastPull;
	private static String lastSent = "";
	private static volatile boolean busyPush, busyPull;
	private static String emote = "none";
	private static long emoteAt;

	private SurgeNet() {}

	public static String url() {
		String u = System.getProperty("surge.apiUrl", "").trim();
		return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
	}

	public static boolean online() {
		return !url().isEmpty();
	}

	/** Surge player? You always are; others only when the service says so. */
	public static boolean isSurge(UUID id) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.getUUID().equals(id)) return true;
		return PLAYERS.containsKey(id);
	}

	public static Remote remote(UUID id) {
		return PLAYERS.get(id);
	}

	/** Called when you start an emote so others see it right away. */
	public static void playedEmote(String name) {
		emote = name;
		emoteAt = System.currentTimeMillis();
		lastPush = 0;
	}

	public static void tick(Minecraft mc) {
		if (!online() || mc.player == null || mc.level == null) return;
		long now = System.currentTimeMillis();
		SurgeConfig c = SurgeConfig.get();
		String wear = Cosmetics.equipped("wings", c.wings) + "|" + Cosmetics.equipped("halo", c.halo) + "|" + Cosmetics.equipped("cape", c.cape) + "|" + emote + emoteAt;
		if (!busyPush && (now - lastPush > 90_000 || !wear.equals(lastSent))) {
			lastPush = now;
			lastSent = wear;
			push(mc);
		}
		if (!busyPull && now - lastPull > 6_000) {
			lastPull = now;
			pull(mc);
		}
	}

	private static void push(Minecraft mc) {
		SurgeConfig c = SurgeConfig.get();
		JsonObject o = new JsonObject();
		o.addProperty("uuid", mc.player.getUUID().toString());
		o.addProperty("name", mc.player.getName().getString());
		o.addProperty("wings", Cosmetics.equipped("wings", c.wings));
		o.addProperty("halo", Cosmetics.equipped("halo", c.halo));
		o.addProperty("cape", Cosmetics.equipped("cape", c.cape));
		o.addProperty("emote", emote);
		o.addProperty("emoteAt", emoteAt);
		busyPush = true;
		HttpRequest req = HttpRequest.newBuilder(URI.create(url() + "/v1/me")).timeout(Duration.ofSeconds(8))
			.header("content-type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(o.toString())).build();
		HTTP.sendAsync(req, HttpResponse.BodyHandlers.discarding()).whenComplete((r, e) -> busyPush = false);
	}

	private static void pull(Minecraft mc) {
		StringBuilder ids = new StringBuilder();
		int n = 0;
		for (Player p : mc.level.players()) {
			if (p == mc.player || n++ > 150) continue;
			if (ids.length() > 0) ids.append(',');
			ids.append(p.getUUID().toString().replace("-", ""));
		}
		if (ids.length() == 0) { PLAYERS.clear(); return; }
		busyPull = true;
		HttpRequest req = HttpRequest.newBuilder(URI.create(url() + "/v1/players?ids=" + ids)).timeout(Duration.ofSeconds(8)).GET().build();
		HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((r, e) -> {
			busyPull = false;
			if (e != null || r == null || r.statusCode() != 200) return;
			try {
				JsonObject players = JsonParser.parseString(r.body()).getAsJsonObject().getAsJsonObject("players");
				Map<UUID, Remote> fresh = new ConcurrentHashMap<>();
				for (Map.Entry<String, JsonElement> en : players.entrySet()) {
					UUID id = parse(en.getKey());
					if (id == null) continue;
					JsonObject d = en.getValue().getAsJsonObject();
					Remote rem = PLAYERS.getOrDefault(id, new Remote());
					rem.wings = str(d, "wings");
					rem.halo = str(d, "halo");
					rem.cape = str(d, "cape");
					String em = str(d, "emote");
					long at = d.has("emoteAt") ? d.get("emoteAt").getAsLong() : 0;
					if (at != rem.emoteAt) {
						boolean firstSight = rem.emoteAt == 0 && !PLAYERS.containsKey(id);
						rem.emoteAt = at;
						rem.emote = em;
						// don't replay old emotes the first time we see someone
						rem.emoteStart = firstSight && System.currentTimeMillis() - at > 15_000 ? 0 : System.currentTimeMillis();
					}
					fresh.put(id, rem);
				}
				PLAYERS.clear();
				PLAYERS.putAll(fresh);
			} catch (Exception ignored) { }
		});
	}

	private static String str(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "none";
	}

	private static UUID parse(String hex) {
		try {
			if (hex.length() != 32) return UUID.fromString(hex);
			return UUID.fromString(hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-" + hex.substring(20));
		} catch (Exception e) {
			return null;
		}
	}

	/** Emote another player is doing right now, or null. */
	public static Emotes.Emote remoteEmote(UUID id, long[] startOut) {
		Remote r = PLAYERS.get(id);
		if (r == null || r.emoteStart == 0) return null;
		Emotes.Emote e = Emotes.Emote.byName(r.emote);
		if (e == null) return null;
		float t = (System.currentTimeMillis() - r.emoteStart) / 1000f;
		if (t > (e.loops ? e.seconds * 3 : e.seconds)) return null;
		startOut[0] = r.emoteStart;
		return e;
	}
}
