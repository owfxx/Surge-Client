package gg.surge.legacy.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.surge.legacy.cosmetic.Cosmetics;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SurgeNet {
	private static final String DEFAULT_URL = "https://surge-client.remvuxd.workers.dev";
	private static final ExecutorService POOL = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "Surge network");
		t.setDaemon(true);
		return t;
	});
	private static volatile Map<UUID, String[]> players = new ConcurrentHashMap<UUID, String[]>();
	private static volatile boolean busyPull, busyPush;
	private static long lastPull, lastPush;
	private static String lastSent = "";

	private SurgeNet() {}

	public static String url() {
		String u = System.getProperty("surge.apiUrl", "").trim();
		if (u.isEmpty()) u = DEFAULT_URL;
		while (u.endsWith("/")) u = u.substring(0, u.length() - 1);
		if (!u.contains("://")) u = "https://" + u;
		return u;
	}

	private static String token() {
		return System.getProperty("surge.token", "").trim();
	}

	private static String call(String method, String path, String body) throws Exception {
		HttpURLConnection c = (HttpURLConnection) new URL(url() + path).openConnection();
		c.setConnectTimeout(6000);
		c.setReadTimeout(6000);
		c.setRequestMethod(method);
		c.setRequestProperty("User-Agent", "SurgeClient/legacy");
		c.setRequestProperty("cache-control", "no-cache");
		if (!token().isEmpty()) c.setRequestProperty("Authorization", "Bearer " + token());
		if (body != null) {
			c.setDoOutput(true);
			c.setRequestProperty("content-type", "application/json");
			OutputStream o = c.getOutputStream();
			o.write(body.getBytes(StandardCharsets.UTF_8));
			o.close();
		}
		if (c.getResponseCode() != 200) return null;
		InputStream in = c.getInputStream();
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int n;
		while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
		in.close();
		return new String(bo.toByteArray(), StandardCharsets.UTF_8);
	}

	public static void tick(MinecraftClient mc) {
		if (mc.player == null || mc.world == null) return;
		long now = System.currentTimeMillis();
		String server = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "";
		final String wear = Cosmetics.myWings() + "|" + Cosmetics.myHalo() + "|" + Cosmetics.myCape() + "|" + server;
		if (!busyPush && (now - lastPush > 60000 || !wear.equals(lastSent))) {
			lastPush = now;
			lastSent = wear;
			final JsonObject o = new JsonObject();
			o.addProperty("uuid", mc.player.getUuid().toString());
			o.addProperty("name", mc.getSession().getUsername());
			o.addProperty("wings", Cosmetics.myWings());
			o.addProperty("halo", Cosmetics.myHalo());
			o.addProperty("cape", Cosmetics.myCape());
			o.addProperty("emote", "none");
			o.addProperty("emoteAt", 0);
			o.addProperty("flag", "show");
			o.addProperty("status", "online");
			o.addProperty("server", server);
			busyPush = true;
			POOL.execute(() -> {
				try { if (call("PUT", "/v1/me", o.toString()) == null) lastPush = System.currentTimeMillis() - 50000; } catch (Exception e) { lastPush = System.currentTimeMillis() - 50000; }
				busyPush = false;
			});
		}
		if (!busyPull && now - lastPull > (players.isEmpty() ? 4000 : 2000)) {
			lastPull = now;
			Set<UUID> want = new LinkedHashSet<UUID>();
			for (PlayerEntity p : mc.world.playerEntities) if (p != mc.player && want.size() < 150 && p.getUuid().version() == 4) want.add(p.getUuid());
			if (want.isEmpty()) { players = new ConcurrentHashMap<UUID, String[]>(); return; }
			final StringBuilder ids = new StringBuilder();
			for (UUID u : want) {
				if (ids.length() > 0) ids.append(',');
				ids.append(u.toString().replace("-", ""));
			}
			busyPull = true;
			POOL.execute(() -> {
				try {
					String body = call("GET", "/v1/players?ids=" + ids + "&t=" + System.currentTimeMillis(), null);
					if (body != null) {
						JsonObject root = new JsonParser().parse(body).getAsJsonObject();
						JsonObject ps = root.getAsJsonObject("players");
						Map<UUID, String[]> fresh = new ConcurrentHashMap<UUID, String[]>();
						for (Map.Entry<String, JsonElement> en : ps.entrySet()) {
							UUID id = parse(en.getKey());
							if (id == null) continue;
							JsonObject d = en.getValue().getAsJsonObject();
							fresh.put(id, new String[]{str(d, "cape"), str(d, "wings"), str(d, "halo")});
						}
						players = fresh;
					}
				} catch (Exception ignored) { }
				busyPull = false;
			});
		}
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

	public static String remote(UUID id, String type) {
		String[] r = players.get(id);
		if (r == null) return null;
		return type.equals("cape") ? r[0] : type.equals("wings") ? r[1] : r[2];
	}

}
