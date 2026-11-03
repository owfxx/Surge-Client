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

public final class SurgeNet {
	public static final class Remote {
		public volatile String wings = "none", halo = "none", cape = "none", emote = "none", flag = "none";
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
		while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
		if (!u.isEmpty() && !u.contains("://")) u = "https://" + u;
		return u;
	}

	public static boolean online() {
		String u = url();
		if (u.isEmpty()) return false;
		try {
			URI uri = URI.create(u);
			return uri.getScheme() != null && uri.getHost() != null;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean isSurge(UUID id) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.getUUID().equals(id)) return true;
		return PLAYERS.containsKey(id);
	}

	public static Remote remote(UUID id) {
		return PLAYERS.get(id);
	}

	public static String myFlag() {
		return gg.surge.client.mods.Mods.NAMETAGS.enabled() && gg.surge.client.mods.Mods.TAG_FLAG.on() ? gg.surge.client.mods.Mods.TAG_COUNTRY.get() : "none";
	}

	public static String flagGlyph(UUID id) {
		if (!gg.surge.client.mods.Mods.NAMETAGS.enabled()) return "";
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null && mc.player.getUUID().equals(id)) return gg.surge.client.features.Flags.glyph(myFlag());
		if (!gg.surge.client.mods.Mods.TAG_OTHERS_FLAGS.on()) return "";
		Remote r = PLAYERS.get(id);
		return r == null ? "" : gg.surge.client.features.Flags.glyph(r.flag);
	}

	public static void playedEmote(String name) {
		emote = name;
		emoteAt = System.currentTimeMillis();
		lastPush = 0;
	}

	public static void tick(Minecraft mc) {
		if (!online()) return;
		if (mc.player == null && Social.token().isEmpty()) return;
		long now = System.currentTimeMillis();
		SurgeConfig c = SurgeConfig.get();
		String wear = Cosmetics.equipped("wings", c.wings) + "|" + Cosmetics.equipped("halo", c.halo) + "|" + Cosmetics.wire("cape", c.cape) + "|" + emote + emoteAt
			+ "|" + Social.status() + "|" + Social.server(mc) + "|" + myFlag();
		if (!busyPush && (now - lastPush > 60_000 || !wear.equals(lastSent))) {
			lastPush = now;
			lastSent = wear;
			push(mc);
		}
		if (!busyPull && mc.level != null && now - lastPull > 6_000) {
			lastPull = now;
			pull(mc);
		}
	}

	private static void push(Minecraft mc) {
		SurgeConfig c = SurgeConfig.get();
		JsonObject o = new JsonObject();
		o.addProperty("uuid", mc.player != null ? mc.player.getUUID().toString() : "");
		o.addProperty("name", mc.getUser().getName());
		o.addProperty("wings", Cosmetics.equipped("wings", c.wings));
		o.addProperty("halo", Cosmetics.equipped("halo", c.halo));
		o.addProperty("cape", Cosmetics.wire("cape", c.cape));
		o.addProperty("emote", emote);
		o.addProperty("emoteAt", emoteAt);
		o.addProperty("flag", myFlag());
		o.addProperty("status", Social.status());
		o.addProperty("server", Social.server(mc));
		HttpRequest req;
		try {
			HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url() + "/v1/me")).timeout(Duration.ofSeconds(8))
				.header("content-type", "application/json").PUT(HttpRequest.BodyPublishers.ofString(o.toString()));
			if (!Social.token().isEmpty()) b.header("Authorization", "Bearer " + Social.token());
			req = b.build();
		} catch (Exception e) {
			return;
		}
		busyPush = true;
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
		HttpRequest req;
		try {
			req = HttpRequest.newBuilder(URI.create(url() + "/v1/players?ids=" + ids)).timeout(Duration.ofSeconds(8)).GET().build();
		} catch (Exception e) {
			return;
		}
		busyPull = true;
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
					rem.flag = str(d, "flag");
					String em = str(d, "emote");
					long at = d.has("emoteAt") ? d.get("emoteAt").getAsLong() : 0;
					if (at != rem.emoteAt) {
						boolean firstSight = rem.emoteAt == 0 && !PLAYERS.containsKey(id);
						rem.emoteAt = at;
						rem.emote = em;
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
