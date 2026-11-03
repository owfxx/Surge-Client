package gg.surge.client.economy;

import java.util.ArrayList;
import java.util.List;

/** Everything sold in the wardrobe. Prices are in Surge Coins (all ≥ 600). */
public final class Shop {
	public record Item(String id, String category, String sub, String name, int price) {
		/** The value stored in the config when equipped, e.g. "flame" for "trail:flame". */
		public String key() { return id.substring(id.indexOf(':') + 1); }
	}

	public static final List<Item> ITEMS = new ArrayList<>();


	private Shop() {}

	private static void add(String id, String cat, String sub, String name, int price) {
		ITEMS.add(new Item(id, cat, sub, name, price));
	}

	static {
		// Cosmetics: one list, no sub-categories. Wings, halos and capes.
		String[][] cos = {
			{"wings:angel", "Angel Wings", "1200"}, {"wings:surge", "Surge Wings", "1500"}, {"wings:soul", "Soul Wings", "1100"},
			{"wings:flame", "Flame Wings", "1100"}, {"wings:void", "Void Wings", "1300"}, {"wings:crystal", "Crystal Wings", "1400"},
			{"wings:gold", "Golden Wings", "1600"},
			{"halo:angel", "Angel Halo", "900"}, {"halo:surge", "Surge Halo", "1000"}, {"halo:fire", "Fire Halo", "750"},
			{"halo:enchanted", "Enchanted Halo", "800"}, {"halo:void", "Void Halo", "850"}, {"halo:rose", "Rose Halo", "700"},
			{"cape:surge", "Surge Cape", "1000"}, {"cape:void", "Void Cape", "800"}, {"cape:flame", "Flame Cape", "750"},
			{"cape:ocean", "Ocean Cape", "700"}, {"cape:aurora", "Aurora Cape", "900"}, {"cape:sakura", "Sakura Cape", "700"},
			{"cape:midnight", "Midnight Cape", "750"}, {"cape:royal", "Royal Cape", "950"}, {"cape:emerald", "Emerald Cape", "700"},
			{"cape:galaxy", "Galaxy Cape", "1100"}};
		for (String[] c : cos) add(c[0], "cosmetic", c[0].substring(0, c[0].indexOf(':')), c[1], Integer.parseInt(c[2]));

		String[][] emotes = {
			{"WAVE", "600"}, {"CLAP", "600"}, {"FLUSTERED", "900"}, {"FACEPALM", "650"}, {"DAB", "750"},
			{"SHRUG", "600"}, {"SALUTE", "600"}, {"CHEER", "800"}, {"THINKING", "650"}, {"LAUGH", "700"},
			{"BOW", "650"}, {"HEART", "850"}, {"BOXING", "900"}, {"FLOSS", "1000"}, {"GROOVE", "950"},
			{"ZOMBIE", "700"}, {"TPOSE", "600"}, {"CRY", "750"}, {"SIT", "600"}, {"POINT", "600"}, {"PEACE", "650"},
			{"STRETCH", "600"}, {"TWIRL", "800"}, {"JUMP", "750"}, {"MEDITATE", "850"}, {"HEADBANG", "700"}, {"YAWN", "600"}};
		for (String[] e : emotes) {
			gg.surge.client.emotes.Emotes.Emote em = gg.surge.client.emotes.Emotes.Emote.valueOf(e[0]);
			add("emote:" + e[0], "emote", "", em.label, Integer.parseInt(e[1]));
		}
	}

	/** Price after the best running sale. */
	public static int price(Item item) {
		int pct = Sales.discount(item);
		return pct <= 0 ? item.price() : Math.max(0, Math.round(item.price() * (100 - pct) / 100f));
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
