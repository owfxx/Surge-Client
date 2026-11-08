package gg.surge.client.economy;

import java.util.ArrayList;
import java.util.List;

public final class Shop {
	public record Item(String id, String category, String sub, String name, int price) {
		public String key() { return id.substring(id.indexOf(':') + 1); }
	}

	public static final List<Item> ITEMS = new ArrayList<>();

	private Shop() {}

	private static void add(String id, String cat, String sub, String name, int price) {
		ITEMS.add(new Item(id, cat, sub, name, price));
	}

	static {
		String[][] cos = {
			{"wings:angel", "Angel Wings", "1200"}, {"wings:surge", "Surge Wings", "1500"}, {"wings:soul", "Soul Wings", "1100"},
			{"wings:flame", "Flame Wings", "1100"}, {"wings:void", "Void Wings", "1300"}, {"wings:crystal", "Crystal Wings", "1400"},
			{"wings:gold", "Golden Wings", "1600"}, {"wings:bat", "Bat Wings", "1300"}, {"wings:dragon", "Dragon Wings", "1800"}, {"wings:butterfly", "Butterfly Wings", "1500"}, {"wings:fairy", "Fairy Wings", "1400"},
			{"halo:angel", "Angel Halo", "900"}, {"halo:surge", "Surge Halo", "1000"}, {"halo:fire", "Fire Halo", "750"},
			{"halo:enchanted", "Enchanted Halo", "800"}, {"halo:void", "Void Halo", "850"}, {"halo:rose", "Rose Halo", "700"}, {"halo:crown", "Royal Crown", "1500"}, {"halo:orbit", "Orbit Halo", "1300"}, {"halo:rainbow", "Rainbow Halo", "1600"},
			{"cape:surge", "Surge Cape", "1000"}, {"cape:void", "Void Cape", "800"}, {"cape:flame", "Flame Cape", "1200"},
			{"cape:ocean", "Ocean Cape", "700"}, {"cape:sakura", "Sakura Cape", "700"},
			{"cape:midnight", "Midnight Cape", "750"}, {"cape:royal", "Royal Cape", "950"}, {"cape:emerald", "Emerald Cape", "700"},
						{"cape:lava", "Lava Cape", "1200"}, {"cape:matrix", "Matrix Cape", "1300"}, {"cape:storm", "Storm Cape", "1500"}, {"cape:phoenix", "Phoenix Cape", "1600"}, {"cape:koi", "Koi Cape", "1400"}, {"cape:bloodmoon", "Blood Moon Cape", "1300"}, {"cape:obsidian", "Obsidian Cape", "1200"}, {"cape:frost", "Frost Cape", "1000"}, {"cape:galaxy", "Galaxy Cape", "1100"}, {"cape:aurora", "Aurora Cape", "900"}, 			{"cape:custom", "Color Cape", "300"},
			{"cape:blinkcat", "Blink Cat Cape", "1500"}, {"cape:enchanted", "Enchanted Cape", "1800"}, {"cape:runes", "Rune Cape", "1600"},
			{"cape:beta", "Beta Tester Cape", "0"}, {"cape:image", "Picture Cape", "0"}};
		for (String[] c : cos) add(c[0], "cosmetic", c[0].substring(0, c[0].indexOf(':')), c[1], Integer.parseInt(c[2]));

		String[][] emotes = {
			{"WAVE", "600"}, {"CLAP", "600"}, {"FACEPALM", "650"}, {"DAB", "750"},
			{"SHRUG", "600"}, {"SALUTE", "600"}, {"CHEER", "800"}, {"LAUGH", "700"},
			{"BOW", "650"}, {"HEART", "850"}, {"FLOSS", "1000"}, {"TPOSE", "600"}, {"CRY", "750"},
			{"SIT", "600"}, {"GRIDDY", "1200"}, {"SHUFFLE", "1100"},
			{"DISCO", "1000"}, {"ZOMBIE", "700"}, {"ROBOT", "900"}, {"AIRGUITAR", "1000"}, {"JUMPJACKS", "600"}, {"FLEX", "750"}, {"KISS", "700"}, {"SPIN", "800"}, {"FACEPLANT", "850"}};
		String[][] idles = {{"IDLE_LOOK", "400"}, {"IDLE_STRETCH", "500"}, {"IDLE_YAWN", "500"}, {"IDLE_TAP", "450"}, {"IDLE_CROSS", "450"}, {"IDLE_VIBE", "600"}, {"IDLE_WATCH", "450"}, {"IDLE_SCRATCH", "450"}, {"IDLE_BOX", "600"}, {"IDLE_PHONE", "500"}, {"IDLE_WHISTLE", "550"}};
		for (String[] e : idles) add("idle:" + e[0], "emote", "idle", gg.surge.client.emotes.Emotes.Emote.valueOf(e[0]).label, Integer.parseInt(e[1]));
		for (String[] e : emotes) {
			gg.surge.client.emotes.Emotes.Emote em = gg.surge.client.emotes.Emotes.Emote.valueOf(e[0]);
			add("emote:" + e[0], "emote", "", em.label, Integer.parseInt(e[1]));
		}
	}

	public static final java.util.Map<String, Integer> OVERRIDES = new java.util.concurrent.ConcurrentHashMap<>();

	public static int basePrice(Item item) {
		Integer o = OVERRIDES.get(item.id());
		return o != null ? o : item.price();
	}

	public static int price(Item item) {
		int base = basePrice(item);
		int pct = Sales.discount(item);
		return pct <= 0 ? base : Math.max(0, Math.round(base * (100 - pct) / 100f));
	}

	public static void applyRemote(com.google.gson.JsonElement root) {
		if (root == null || !root.isJsonObject() || !root.getAsJsonObject().has("prices")) return;
		java.util.Map<String, Integer> m = new java.util.HashMap<>();
		try {
			for (var en : root.getAsJsonObject().getAsJsonObject("prices").entrySet()) m.put(en.getKey(), Math.max(0, en.getValue().getAsInt()));
		} catch (Exception ignored) { return; }
		OVERRIDES.clear();
		OVERRIDES.putAll(m);
	}

	public static Item byId(String id) {
		for (Item i : ITEMS) if (i.id().equals(id)) return i;
		return null;
	}

	public static List<Item> bySub(String category, String sub) {
		List<Item> out = new ArrayList<>();
		for (Item i : ITEMS) if (i.category().equals(category) && (sub == null || i.sub().equals(sub))) out.add(i);
		return out;
	}
}
