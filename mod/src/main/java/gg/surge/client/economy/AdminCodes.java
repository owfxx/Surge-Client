package gg.surge.client.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public final class AdminCodes {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
	private static final SecureRandom RNG = new SecureRandom();

	public record Code(String code, int coins, java.util.List<String> items, long created) {}

	private AdminCodes() {}

	private static Path adminFile() { return Wallet.home().resolve("admin.json"); }

	private static Path publicFile() { return Wallet.home().resolve("codes.json"); }

	private static JsonObject read() {
		try {
			if (Files.exists(adminFile())) {
				JsonElement e = JsonParser.parseString(Files.readString(adminFile()));
				if (e.isJsonObject()) return e.getAsJsonObject();
			}
		} catch (Exception ignored) { }
		JsonObject o = new JsonObject();
		o.add("pass", null);
		o.add("codes", new JsonArray());
		return o;
	}

	public static List<Code> list() {
		List<Code> out = new ArrayList<>();
		JsonArray arr = read().getAsJsonArray("codes");
		if (arr == null) return out;
		for (JsonElement e : arr) {
			JsonObject o = e.getAsJsonObject();
			List<String> items = new ArrayList<>();
			if (o.has("items") && o.get("items").isJsonArray()) for (JsonElement i : o.getAsJsonArray("items")) items.add(i.getAsString());
			out.add(new Code(o.get("code").getAsString(), o.has("coins") ? o.get("coins").getAsInt() : 0, items, o.has("created") ? o.get("created").getAsLong() : 0));
		}
		return out;
	}

	public static List<String> create(int coins, int count) {
		return create(null, coins, List.of(), count);
	}

	public static List<String> create(String custom, int coins, List<String> items, int count) {
		JsonObject d = read();
		JsonArray arr = d.getAsJsonArray("codes");
		if (arr == null) { arr = new JsonArray(); d.add("codes", arr); }
		List<String> made = new ArrayList<>();
		JsonArray fresh = new JsonArray();
		boolean isCustom = custom != null && !Wallet.normalize(custom).isEmpty();
		if (isCustom) {
			for (JsonElement e : arr) if (Wallet.normalize(e.getAsJsonObject().get("code").getAsString()).equals(Wallet.normalize(custom))) return List.of();
		}
		for (int n = 0; n < (isCustom ? 1 : Math.max(1, Math.min(50, count))); n++) {
			String code;
			if (isCustom) code = custom.trim().toUpperCase();
			else {
				StringBuilder raw = new StringBuilder();
				for (int i = 0; i < 12; i++) raw.append(ALPHABET.charAt(RNG.nextInt(ALPHABET.length())));
				code = raw.substring(0, 4) + "-" + raw.substring(4, 8) + "-" + raw.substring(8, 12);
			}
			JsonObject o = new JsonObject();
			o.addProperty("code", code);
			o.addProperty("coins", coins);
			JsonArray it = new JsonArray();
			for (String i : items) it.add(i);
			o.add("items", it);
			o.addProperty("created", System.currentTimeMillis());
			fresh.add(o);
			made.add(code);
		}
		fresh.addAll(arr);
		d.add("codes", fresh);
		write(d);
		return made;
	}

	public static void remove(String code) {
		JsonObject d = read();
		JsonArray arr = d.getAsJsonArray("codes");
		JsonArray keep = new JsonArray();
		if (arr != null) for (JsonElement e : arr) if (!e.getAsJsonObject().get("code").getAsString().equals(code)) keep.add(e);
		d.add("codes", keep);
		write(d);
	}

	public static void publish() {
		write(read());
	}

	private static void write(JsonObject d) {
		try {
			Files.writeString(adminFile(), GSON.toJson(d));
			JsonObject pub = new JsonObject();
			JsonArray codes = new JsonArray();
			JsonArray arr = d.getAsJsonArray("codes");
			if (arr != null) for (JsonElement e : arr) {
				JsonObject o = e.getAsJsonObject();
				JsonObject h = new JsonObject();
				h.addProperty("hash", Wallet.sha256(Wallet.normalize(o.get("code").getAsString())));
				h.addProperty("coins", o.has("coins") ? o.get("coins").getAsInt() : 0);
				if (o.has("items")) h.add("items", o.get("items"));
				codes.add(h);
			}
			pub.add("codes", codes);
			pub.add("sales", GSON.toJsonTree(Sales.local()));
			Files.writeString(publicFile(), GSON.toJson(pub));
		} catch (Exception ignored) { }
	}
}
