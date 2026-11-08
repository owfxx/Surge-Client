package gg.surge.client.net;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.surge.client.cosmetics.ImageCapes;
import gg.surge.client.economy.Sales;
import gg.surge.client.economy.Wallet;
import gg.surge.client.gui.GiftScreen;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class Inbox {
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
	private static long last;
	private static volatile boolean busy;
	public static volatile String badge = "";
	private static final List<Runnable> PENDING = new ArrayList<>();

	private Inbox() {}

	public static void refresh() {
		last = 0;
	}

	public static void tick(Minecraft mc) {
		long now = System.currentTimeMillis();
		synchronized (PENDING) {
			if (!PENDING.isEmpty() && safeToShow(mc)) PENDING.remove(0).run();
		}
		if (busy || now - last < 60_000 || Social.token().isEmpty() || !SurgeNet.online()) return;
		last = now;
		busy = true;
		HttpRequest req;
		try {
			req = HttpRequest.newBuilder(URI.create(SurgeNet.url() + "/v1/inbox")).timeout(Duration.ofSeconds(10))
				.header("Authorization", "Bearer " + Social.token()).GET().build();
		} catch (Exception e) {
			busy = false;
			return;
		}
		HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).whenComplete((r, e) -> {
			busy = false;
			if (e != null || r == null || r.statusCode() != 200) return;
			try {
				JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
				badge = o.has("badge") && !o.get("badge").isJsonNull() ? o.get("badge").getAsString() : "";
				if (o.has("capev") && !o.get("capev").isJsonNull()) ImageCapes.version(Social.myId(), o.get("capev").getAsLong());
				if (o.has("sales")) Sales.applyPersonal(o.getAsJsonArray("sales"));
				JsonArray gifts = o.has("gifts") ? o.getAsJsonArray("gifts") : new JsonArray();
				if (!gifts.isEmpty()) claim(gifts);
			} catch (Exception ignored) { }
		});
	}

	private static boolean safeToShow(Minecraft mc) {
		if (mc.screen instanceof GiftScreen) return false;
		if (mc.level != null && mc.screen == null) return mc.player != null && !mc.player.isDeadOrDying();
		return mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen) && !(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen);
	}

	private static void claim(JsonArray gifts) {
		JsonArray ids = new JsonArray();
		for (JsonElement g : gifts) ids.add(g.getAsJsonObject().get("id").getAsLong());
		JsonObject body = new JsonObject();
		body.add("ids", ids);
		HttpRequest req;
		try {
			req = HttpRequest.newBuilder(URI.create(SurgeNet.url() + "/v1/inbox/claim")).timeout(Duration.ofSeconds(10))
				.header("Authorization", "Bearer " + Social.token()).header("content-type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
		} catch (Exception e) {
			return;
		}
		HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofString()).thenAccept(r -> {
			if (r.statusCode() != 200) return;
			JsonArray claimed;
			try {
				claimed = JsonParser.parseString(r.body()).getAsJsonObject().getAsJsonArray("claimed");
			} catch (Exception ex) {
				return;
			}
			Minecraft.getInstance().execute(() -> {
				Wallet w = Wallet.get();
				for (JsonElement el : claimed) {
					JsonObject g = el.getAsJsonObject();
					int coins = g.has("coins") ? Math.max(0, g.get("coins").getAsInt()) : 0;
					List<String> items = new ArrayList<>();
					if (g.has("items")) for (JsonElement i : g.getAsJsonArray("items")) items.add(i.getAsString());
					String note = g.has("note") && !g.get("note").isJsonNull() ? g.get("note").getAsString() : "";
					w.coins += coins;
					w.owned.addAll(items);
					String heading = items.isEmpty() ? "You've been gifted coins!" : "You've been gifted " + describe(items) + "!";
					synchronized (PENDING) { PENDING.add(() -> GiftScreen.show(heading, note, coins, items)); }
				}
				w.save();
			});
		});
	}

	private static String describe(List<String> items) {
		if (items.size() > 1) return items.size() + " items";
		var it = gg.surge.client.economy.Shop.byId(items.get(0));
		return it == null ? "a surprise" : it.name();
	}
}
