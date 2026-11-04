package gg.surge.client.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class Social {
	public record Friend(String uuid, String name, String status, String server) {}

	public record Person(String uuid, String name) {}

	public record Message(long id, String from, String to, String text, long ts) {}

	public record Toast(String title, String text, long at) {}

	public static volatile List<Friend> friends = new ArrayList<>();
	public static volatile List<Person> incoming = new ArrayList<>(), outgoing = new ArrayList<>(), blocked = new ArrayList<>();
	public static volatile Set<String> muted = new HashSet<>();
	public static volatile String myUuid = "", myName = "";
	public static final Map<String, List<Message>> CHATS = new HashMap<>();
	public static final Map<String, Integer> UNREAD = new HashMap<>();
	public static final List<Toast> TOASTS = new ArrayList<>();
	public static volatile String openChat;
	public static volatile boolean screenOpen;
	public static volatile String lastError = "";

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
	private static long lastSocial, lastMessages, lastId = -1, lastInput = System.currentTimeMillis();
	private static double lastMx, lastMy;
	private static boolean busySocial, busyMsg, loadedOnce;
	private static Object lastScreen;
	private static final Set<String> knownIncoming = new HashSet<>();
	private static final Map<String, String> lastStatus = new HashMap<>();

	private Social() {}

	private static volatile String session;
	private static volatile boolean signingIn;
	private static volatile long lastAttempt;
	public static volatile String signInError = "";

	private static java.nio.file.Path sessionFile() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("surge").resolve("session.json");
	}

	private static String myId() {
		try { return Minecraft.getInstance().getUser().getProfileId().toString().replace("-", "").toLowerCase(); } catch (Exception e) { return ""; }
	}

	public static String token() {
		if (session == null) {
			session = System.getProperty("surge.token", "").trim();
			if (session.isEmpty()) {
				try {
					JsonObject o = JsonParser.parseString(java.nio.file.Files.readString(sessionFile())).getAsJsonObject();
					if (s(o, "uuid").equals(myId())) session = s(o, "token");
				} catch (Exception ignored) { }
			}
		}
		return session;
	}

	public static void useSession(String t) {
		session = t;
	}

	private static void forget() {
		session = "";
		try { java.nio.file.Files.deleteIfExists(sessionFile()); } catch (Exception ignored) { }
	}

	public static void ensureSignedIn() {
		if (!SurgeNet.online() || !token().isEmpty() || signingIn) return;
		if (System.currentTimeMillis() - lastAttempt < 60_000) return;
		lastAttempt = System.currentTimeMillis();
		signingIn = true;
		Minecraft mc = Minecraft.getInstance();
		String access, uuid, name;
		try {
			access = mc.getUser().getAccessToken();
			uuid = myId();
			name = mc.getUser().getName();
		} catch (Exception e) {
			signingIn = false;
			signInError = "Couldn't read your Minecraft account";
			return;
		}
		CompletableFuture.runAsync(() -> {
			try {
				if (access == null || access.length() < 20 || uuid.isEmpty()) { signInError = "Friends and chat need a Microsoft account"; return; }
				byte[] rnd = new byte[16];
				new java.security.SecureRandom().nextBytes(rnd);
				StringBuilder sid = new StringBuilder();
				for (byte b : rnd) sid.append(String.format("%02x", b));
				JsonObject join = new JsonObject();
				join.addProperty("accessToken", access);
				join.addProperty("selectedProfile", uuid);
				join.addProperty("serverId", sid.toString());
				HttpResponse<String> j = HTTP.send(HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/join")).timeout(Duration.ofSeconds(10))
					.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(join.toString())).build(), HttpResponse.BodyHandlers.ofString());
				if (j.statusCode() >= 300) { signInError = "Minecraft couldn't confirm your account (" + j.statusCode() + "). Restart the game and try again."; return; }
				JsonObject body = new JsonObject();
				body.addProperty("name", name);
				body.addProperty("serverId", sid.toString());
				HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(SurgeNet.url() + "/v1/auth")).timeout(Duration.ofSeconds(10))
					.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofString());
				JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
				if (r.statusCode() != 200 || s(o, "token").isEmpty()) { signInError = o.has("error") ? s(o, "error") : "Surge sign-in failed (" + r.statusCode() + ")"; return; }
				session = s(o, "token");
				signInError = "";
				JsonObject save = new JsonObject();
				save.addProperty("uuid", uuid);
				save.addProperty("token", session);
				java.nio.file.Files.createDirectories(sessionFile().getParent());
				java.nio.file.Files.writeString(sessionFile(), save.toString());
				lastSocial = 0;
				lastMessages = 0;
			} catch (Exception e) {
				signInError = "Can't reach the Surge service right now";
			} finally {
				signingIn = false;
			}
		});
	}

	public static boolean available() {
		return SurgeNet.online() && !token().isEmpty();
	}

	public static String unavailableReason() {
		if (!SurgeNet.online()) return "Friends and chat are offline right now.";
		if (signingIn || signInError.isEmpty()) return "Signing you in…";
		return signInError;
	}

	public static String status() {
		String s = Mods.STATUS.get();
		if (s.equals("Busy")) return "busy";
		if (s.equals("Offline")) return "invisible";
		if (s.equals("Idle")) return "idle";
		if (Mods.AUTO_IDLE.on() && System.currentTimeMillis() - lastInput > 5 * 60_000L) return "idle";
		return "online";
	}

	public static int statusColor(String s) {
		return switch (s) {
			case "online" -> 0xFF4ADE80;
			case "idle" -> 0xFFFACC15;
			case "busy" -> 0xFFF87171;
			default -> 0xFF71717A;
		};
	}

	public static String statusLabel(String s) {
		return switch (s) {
			case "online" -> "Online";
			case "idle" -> "Idle";
			case "busy" -> "Busy";
			case "invisible" -> "Appearing offline";
			default -> "Offline";
		};
	}

	public static String server(Minecraft mc) {
		if (!Mods.SHOW_SERVER.on() || mc.level == null) return "";
		if (mc.getCurrentServer() != null) return mc.getCurrentServer().ip;
		return "Singleplayer";
	}

	public static void tick(Minecraft mc) {
		double mx = mc.mouseHandler.xpos(), my = mc.mouseHandler.ypos();
		var o = mc.options;
		if (mx != lastMx || my != lastMy || mc.screen != lastScreen || o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown()
			|| o.keyRight.isDown() || o.keyJump.isDown() || o.keyAttack.isDown() || o.keyUse.isDown()) lastInput = System.currentTimeMillis();
		lastMx = mx;
		lastMy = my;
		lastScreen = mc.screen;
		synchronized (TOASTS) { TOASTS.removeIf(t -> System.currentTimeMillis() - t.at() > 6000); }
		ensureSignedIn();
		if (!available()) return;
		long now = System.currentTimeMillis();
		if (!busySocial && now - lastSocial > (screenOpen ? 5_000 : 20_000)) { lastSocial = now; refresh(); }
		if (!busyMsg && now - lastMessages > (openChat != null ? 1_500 : 5_000)) { lastMessages = now; pollMessages(); }
	}

	private static HttpRequest.Builder req(String path) {
		return HttpRequest.newBuilder(URI.create(SurgeNet.url() + path)).timeout(Duration.ofSeconds(8))
			.header("Authorization", "Bearer " + token()).header("Content-Type", "application/json");
	}

	private static CompletableFuture<JsonObject> failed(String msg) {
		JsonObject o = new JsonObject();
		o.addProperty("error", msg);
		return CompletableFuture.completedFuture(o);
	}

	private static CompletableFuture<JsonObject> call(java.util.function.Supplier<HttpRequest> make) {
		if (!available()) return failed(unavailableReason());
		HttpRequest r;
		try {
			r = make.get();
		} catch (Exception e) {
			return failed("The Surge service link in the launcher settings isn't valid");
		}
		return HTTP.sendAsync(r, HttpResponse.BodyHandlers.ofString()).thenApply(res -> {
			JsonObject o;
			try { o = JsonParser.parseString(res.body()).getAsJsonObject(); } catch (Exception e) { o = new JsonObject(); }
			if (res.statusCode() == 401) { forget(); lastAttempt = 0; o.addProperty("error", "Signing you in again…"); }
			if (res.statusCode() >= 400 && !o.has("error")) o.addProperty("error", "Something went wrong (" + res.statusCode() + ")");
			return o;
		}).exceptionally(e -> {
			JsonObject o = new JsonObject();
			o.addProperty("error", "Can't reach the Surge service");
			return o;
		});
	}

	private static String s(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
	}

	private static List<Person> people(JsonObject o, String key) {
		List<Person> out = new ArrayList<>();
		JsonArray a = o.getAsJsonArray(key);
		if (a != null) for (JsonElement e : a) { JsonObject p = e.getAsJsonObject(); out.add(new Person(s(p, "uuid"), s(p, "name"))); }
		return out;
	}

	public static void refresh() {
		busySocial = true;
		call(() -> req("/v1/social").GET().build()).thenAccept(o -> Minecraft.getInstance().execute(() -> {
			busySocial = false;
			if (o.has("error")) { lastError = s(o, "error"); return; }
			lastError = "";
			JsonObject me = o.getAsJsonObject("me");
			if (me != null) { myUuid = s(me, "uuid"); myName = s(me, "name"); }
			List<Friend> fr = new ArrayList<>();
			JsonArray a = o.getAsJsonArray("friends");
			if (a != null) for (JsonElement e : a) {
				JsonObject f = e.getAsJsonObject();
				fr.add(new Friend(s(f, "uuid"), s(f, "name"), s(f, "status"), s(f, "server")));
			}
			fr.sort((x, y) -> {
				int ox = "offline".equals(x.status()) ? 1 : 0, oy = "offline".equals(y.status()) ? 1 : 0;
				return ox != oy ? ox - oy : x.name().compareToIgnoreCase(y.name());
			});
			List<Person> in = people(o, "incoming");
			Set<String> mu = new HashSet<>();
			JsonArray m = o.getAsJsonArray("muted");
			if (m != null) for (JsonElement e : m) mu.add(e.getAsString());
			if (loadedOnce) {
				for (Person p : in) if (!knownIncoming.contains(p.uuid())) toast("Friend request", p.name() + " wants to be friends");
				for (Friend f : fr) {
					String before = lastStatus.get(f.uuid());
					if ("offline".equals(before) && !"offline".equals(f.status()) && !mu.contains(f.uuid()) && Mods.SOCIAL_ONLINE_TOASTS.on()) toast(f.name(), "is now online");
				}
			}
			knownIncoming.clear();
			for (Person p : in) knownIncoming.add(p.uuid());
			lastStatus.clear();
			for (Friend f : fr) lastStatus.put(f.uuid(), f.status());
			friends = fr;
			incoming = in;
			outgoing = people(o, "outgoing");
			blocked = people(o, "blocked");
			muted = mu;
			loadedOnce = true;
		}));
	}

	private static void pollMessages() {
		busyMsg = true;
		long since = Math.max(0, lastId);
		call(() -> req("/v1/messages?since=" + since).GET().build()).thenAccept(o -> Minecraft.getInstance().execute(() -> {
			busyMsg = false;
			if (o.has("error")) return;
			JsonArray a = o.getAsJsonArray("messages");
			if (a == null) return;
			boolean first = lastId < 0;
			for (JsonElement e : a) {
				JsonObject m = e.getAsJsonObject();
				Message msg = new Message(m.get("id").getAsLong(), s(m, "sender"), s(m, "receiver"), s(m, "text"), m.get("ts").getAsLong());
				lastId = Math.max(lastId, msg.id());
				boolean mine = msg.from().equals(myUuid);
				String other = mine ? msg.to() : msg.from();
				CHATS.computeIfAbsent(other, k -> new ArrayList<>()).add(msg);
				if (!mine && !first && !other.equals(openChat)) {
					UNREAD.merge(other, 1, Integer::sum);
					if (!muted.contains(other) && !Social.status().equals("busy")) toast(nameOf(other), msg.text());
				}
			}
			if (first && lastId < 0) lastId = 0;
		}));
	}

	public static String nameOf(String uuid) {
		for (Friend f : friends) if (f.uuid().equals(uuid)) return f.name();
		for (Person p : incoming) if (p.uuid().equals(uuid)) return p.name();
		return "Friend";
	}

	public static Friend friend(String uuid) {
		for (Friend f : friends) if (f.uuid().equals(uuid)) return f;
		return null;
	}

	public static void notice(String title, String text) {
		synchronized (TOASTS) {
			TOASTS.add(new Toast(title, text, System.currentTimeMillis()));
			while (TOASTS.size() > 4) TOASTS.remove(0);
		}
	}

	public static void toast(String title, String text) {
		if (!Mods.SOCIAL_TOASTS.on()) return;
		synchronized (TOASTS) {
			TOASTS.add(new Toast(title, text, System.currentTimeMillis()));
			while (TOASTS.size() > 4) TOASTS.remove(0);
		}
		gg.surge.client.gui.Ui.toggleSound(true);
	}

	private static void post(String path, JsonObject body, Consumer<JsonObject> done) {
		call(() -> req(path).POST(HttpRequest.BodyPublishers.ofString(body.toString())).build())
			.thenAccept(o -> Minecraft.getInstance().execute(() -> { if (done != null) done.accept(o); refresh(); }));
	}

	private static JsonObject uuidBody(String uuid) {
		JsonObject b = new JsonObject();
		b.addProperty("uuid", uuid);
		return b;
	}

	public static void addFriend(String name, Consumer<String> result) {
		JsonObject b = new JsonObject();
		b.addProperty("name", name.trim());
		post("/v1/friends/add", b, o -> result.accept(o.has("error") ? s(o, "error")
			: o.has("friends") ? "You're now friends" : "Request sent to " + s(o, "name")));
	}

	public static void accept(String uuid) { post("/v1/friends/accept", uuidBody(uuid), null); }

	public static void decline(String uuid) { post("/v1/friends/decline", uuidBody(uuid), null); }

	public static void cancel(String uuid) { post("/v1/friends/cancel", uuidBody(uuid), null); }

	public static void remove(String uuid) { post("/v1/friends/remove", uuidBody(uuid), null); }

	public static void block(String uuid) { post("/v1/block", uuidBody(uuid), null); }

	public static void unblock(String uuid) { post("/v1/unblock", uuidBody(uuid), null); }

	public static void mute(String uuid) { post("/v1/mute", uuidBody(uuid), null); }

	public static void unmute(String uuid) { post("/v1/unmute", uuidBody(uuid), null); }

	public static void message(String uuid, String text, Consumer<String> error) {
		JsonObject b = uuidBody(uuid);
		b.addProperty("text", text);
		call(() -> req("/v1/messages").POST(HttpRequest.BodyPublishers.ofString(b.toString())).build()).thenAccept(o -> Minecraft.getInstance().execute(() -> {
			if (o.has("error")) error.accept(s(o, "error"));
			else { lastMessages = 0; }
		}));
	}

	public static int totalUnread() {
		int n = 0;
		for (int v : UNREAD.values()) n += v;
		return n + incoming.size();
	}

	public static Screen screen(Screen parent) {
		return new gg.surge.client.gui.SocialScreen(parent);
	}
}
