package gg.surge.legacy.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.surge.legacy.mod.Category;
import gg.surge.legacy.mod.Mod;
import gg.surge.legacy.mod.Setting;
import gg.surge.legacy.net.SurgeNet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class Cosmetics {
	public static final Mod MOD = new Mod("cosmetics", "Cosmetics", Category.COSMETICS, new ItemStack(Items.LEATHER_CHESTPLATE), "Shows Surge capes, wings and halos. Pick them in the launcher wardrobe", true);
	public static final Setting.Toggle OTHERS = MOD.toggle("others", "Show other players' cosmetics", true);
	public static final Setting.Toggle MINE = MOD.toggle("mine", "Show my cosmetics", true);
	public static final Setting.Toggle WINGS = MOD.toggle("wings", "Show wings", true);
	public static final Setting.Toggle HALOS = MOD.toggle("halos", "Show halos", true);

	private static long readAt;
	private static String cape = "none", wings = "none", halo = "none";

	private Cosmetics() {}

	private static JsonObject read(File f) {
		try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
			JsonElement e = new JsonParser().parse(r);
			return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
		} catch (Exception e) {
			return null;
		}
	}

	private static String str(JsonObject o, String k, String def) {
		return o != null && o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : def;
	}

	private static File home() {
		String prop = System.getProperty("surge.home");
		return prop != null && !prop.trim().isEmpty() ? new File(prop) : new File(System.getProperty("user.home"), ".surgeclient");
	}

	private static void refresh() {
		if (System.currentTimeMillis() - readAt < 3000) return;
		readAt = System.currentTimeMillis();
		JsonObject cfg = read(new File(new File(System.getProperty("user.home"), ".surgeclient"), "surge.json"));
		JsonObject wallet = read(new File(home(), "wallet.json"));
		Set<String> owned = new HashSet<String>();
		if (wallet != null && wallet.has("owned") && wallet.get("owned").isJsonArray()) {
			JsonArray a = wallet.getAsJsonArray("owned");
			for (JsonElement e : a) owned.add(e.getAsString());
		}
		wings = equipped(owned, "wings", str(cfg, "wings", "none"));
		halo = equipped(owned, "halo", str(cfg, "halo", "none"));
		String c = equipped(owned, "cape", str(cfg, "cape", "none"));
		cape = c.equals("none") || c.equals("image") ? c : resolve(c, cfg);
		if (gg.surge.legacy.AutoTest.active()) {
			cape = gg.surge.legacy.AutoTest.cosmetic("cape");
			wings = gg.surge.legacy.AutoTest.cosmetic("wings");
			halo = gg.surge.legacy.AutoTest.cosmetic("halo");
		}
	}

	private static String equipped(Set<String> owned, String type, String key) {
		if (key == null || key.equals("none")) return "none";
		if (owned.contains(type + ":" + key)) return key;
		if (type.equals("cape") && key.equals("custom")) for (String o : owned) if (o.startsWith("cape:classic_")) return key;
		return "none";
	}

	private static String resolve(String style, JsonObject cfg) {
		if (style.equals("custom")) {
			boolean grad = cfg != null && cfg.has("capeGradient") && cfg.get("capeGradient").getAsBoolean();
			return "custom_" + str(cfg, "capeColor", "8F50F2").toLowerCase(Locale.ROOT) + (grad ? "-" + str(cfg, "capeColor2", "EC68B0").toLowerCase(Locale.ROOT) : "");
		}
		if (style.equals("beta")) {
			JsonObject b = cfg != null && cfg.has("betaCape") && cfg.get("betaCape").isJsonObject() ? cfg.getAsJsonObject("betaCape") : null;
			boolean grad = b != null && b.has("gradient") && b.get("gradient").getAsBoolean();
			return "beta2_" + str(b, "color", "141418").toLowerCase(Locale.ROOT) + (grad ? "-" + str(b, "color2", "8F50F2").toLowerCase(Locale.ROOT) : "") + "_" + str(b, "logo", "FFFFFF").toLowerCase(Locale.ROOT) + "_" + str(b, "text", "beta_tester");
		}
		if (style.startsWith("beta_")) {
			String[] p = style.split("_");
			return "beta2_" + (p.length > 2 ? p[2] : "141418") + "_ffffff_beta_tester";
		}
		if (style.startsWith("classic_")) return "custom_8f50f2";
		return style;
	}

	public static String myCape() { refresh(); return cape; }

	public static String myWings() { refresh(); return wings; }

	public static String myHalo() { refresh(); return halo; }

	public static String forPlayer(PlayerEntity p, String type) {
		if (!MOD.enabled()) return null;
		MinecraftClient mc = MinecraftClient.getInstance();
		String v;
		if (p == mc.player) {
			if (!MINE.on()) return null;
			v = type.equals("cape") ? myCape() : type.equals("wings") ? myWings() : myHalo();
		} else {
			if (!OTHERS.on()) return null;
			v = SurgeNet.remote(p.getUuid(), type);
		}
		if (v == null || v.equals("none") || v.isEmpty()) return null;
		if (type.equals("wings") && !WINGS.on()) return null;
		if (type.equals("halo") && !HALOS.on()) return null;
		return v;
	}
}
