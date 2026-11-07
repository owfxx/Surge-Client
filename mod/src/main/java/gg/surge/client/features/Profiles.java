package gg.surge.client.features;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import gg.surge.client.config.SurgeConfig;
import gg.surge.client.mods.Mods;
import gg.surge.client.mods.SurgeMod;
import gg.surge.client.net.Social;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Profiles {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type MAP = new TypeToken<LinkedHashMap<String, SurgeConfig.ModuleState>>() {}.getType();
	private static final String PREFIX = "SURGE1:";
	private static String lastServer;

	private Profiles() {}

	private static boolean migrated;

	private static Path dir() {
		Path d = Path.of(System.getProperty("user.home"), ".surgeclient", "profiles");
		if (!migrated) {
			migrated = true;
			try {
				Files.createDirectories(d.resolve("servers"));
				Path old = FabricLoader.getInstance().getConfigDir().resolve("surge").resolve("profiles");
				for (int i = 1; i <= 3; i++) {
					Path f = old.resolve("setup" + i + ".json");
					Path to = d.resolve("Setup " + i + ".json");
					if (Files.exists(f) && !Files.exists(to)) Files.copy(f, to);
				}
			} catch (Exception ignored) { }
		}
		return d;
	}

	private static String snapshot() {
		return GSON.toJson(SurgeConfig.get().modules, MAP);
	}

	private static void restore(String json) {
		Map<String, SurgeConfig.ModuleState> loaded = GSON.fromJson(json, MAP);
		if (loaded == null) throw new IllegalArgumentException("empty");
		SurgeConfig c = SurgeConfig.get();
		SurgeConfig.ModuleState keep = c.modules.get(Mods.PROFILES.id);
		for (SurgeConfig.ModuleState s : loaded.values()) if (s != null && s.opts == null) s.opts = new LinkedHashMap<>();
		c.modules = loaded;
		if (keep != null) c.modules.put(Mods.PROFILES.id, keep);
		refresh();
	}

	private static void refresh() {
		Minecraft mc = Minecraft.getInstance();
		SurgeConfig.save();
		try {
			OptionTweaks.apply(mc);
			DamageTint.apply(mc);
			Fullbright.apply(mc);
			Extras.applyGlint(mc);
		} catch (Exception ignored) { }
	}

	private static void save(String name, String json) {
		try {
			Files.createDirectories(dir().resolve("servers"));
			Files.writeString(dir().resolve(name + ".json"), json);
		} catch (Exception ignored) { }
	}

	private static String read(String name) {
		try {
			Path p = dir().resolve(name + ".json");
			return Files.exists(p) ? Files.readString(p) : null;
		} catch (Exception e) {
			return null;
		}
	}

	public static String clean(String name) {
		String n = name == null ? "" : name.replaceAll("[^A-Za-z0-9 _\\-]", "").trim();
		if (n.length() > 32) n = n.substring(0, 32).trim();
		return n;
	}

	public static java.util.List<String> names() {
		java.util.List<String> out = new java.util.ArrayList<>();
		try (var st = Files.list(dir())) {
			st.filter(p -> p.getFileName().toString().endsWith(".json") && !p.getFileName().toString().startsWith("_"))
				.sorted(java.util.Comparator.comparing(p -> p.getFileName().toString().toLowerCase()))
				.forEach(p -> { String f = p.getFileName().toString(); out.add(f.substring(0, f.length() - 5)); });
		} catch (Exception ignored) { }
		return out;
	}

	public static String active() {
		try {
			Path f = dir().resolve("_active.txt");
			return Files.exists(f) ? Files.readString(f).trim() : "";
		} catch (Exception e) {
			return "";
		}
	}

	private static void setActive(String name) {
		try { Files.writeString(dir().resolve("_active.txt"), name); } catch (Exception ignored) { }
	}

	public static boolean saveAs(String raw) {
		String name = clean(raw);
		if (name.isEmpty()) { Social.notice("Name your profile", "Type a name first"); return false; }
		save(name, snapshot());
		setActive(name);
		Social.notice("Saved " + name, "Your mods, settings and HUD are in this profile");
		return true;
	}

	public static void load(String name) {
		String json = read(name);
		if (json == null) return;
		try {
			restore(json);
			setActive(name);
			Social.notice("Switched to " + name, "Your mods and HUD switched over");
		} catch (Exception e) {
			Social.notice("Couldn't load " + name, "The file is damaged");
		}
	}

	public static void delete(String name) {
		try { Files.deleteIfExists(dir().resolve(name + ".json")); } catch (Exception ignored) { }
		if (name.equals(active())) setActive("");
	}

	public static boolean rename(String from, String raw) {
		String to = clean(raw);
		if (to.isEmpty() || to.equals(from)) return false;
		try {
			Files.move(dir().resolve(from + ".json"), dir().resolve(to + ".json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			if (from.equals(active())) setActive(to);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static void export() {
		String code = PREFIX + Base64.getEncoder().encodeToString(snapshot().getBytes(java.nio.charset.StandardCharsets.UTF_8));
		Minecraft.getInstance().keyboardHandler.setClipboard(code);
		Social.notice("Setup copied", "Paste it to a friend so they can import it");
	}

	public static void importClipboard() {
		String clip = Minecraft.getInstance().keyboardHandler.getClipboard().trim();
		try {
			String json = clip.startsWith(PREFIX) ? new String(Base64.getDecoder().decode(clip.substring(PREFIX.length())), java.nio.charset.StandardCharsets.UTF_8) : clip;
			save("_before-import", snapshot());
			restore(json);
			Social.notice("Setup imported", "Your old setup was saved as a backup");
		} catch (Exception e) {
			Social.notice("Nothing to import", "Copy a Surge setup first, then press Import");
		}
	}

	public static void allMods(boolean on) {
		for (SurgeMod m : Mods.all()) if (m.toggleable) m.setEnabled(on);
		refresh();
		Social.notice(on ? "Every mod turned on" : "Every mod turned off", "Use a saved profile to go back");
	}

	public static void reset() {
		save("_before-reset", snapshot());
		SurgeConfig c = SurgeConfig.get();
		SurgeConfig.ModuleState keep = c.modules.get(Mods.PROFILES.id);
		c.modules.clear();
		if (keep != null) c.modules.put(Mods.PROFILES.id, keep);
		refresh();
		Social.notice("Everything reset", "Your old setup was saved as a backup");
	}

	private static String key(String server) {
		return "servers/" + server.toLowerCase().replaceAll("[^a-z0-9.-]", "_");
	}

	public static void tick(Minecraft mc) {
		if (!Mods.PROFILE_PER_SERVER.on()) { lastServer = null; return; }
		String now = mc.getCurrentServer() != null && mc.level != null ? mc.getCurrentServer().ip : null;
		if (java.util.Objects.equals(now, lastServer)) return;
		if (lastServer != null) save(key(lastServer), snapshot());
		if (now != null) {
			String json = read(key(now));
			if (json != null) {
				try { restore(json); Social.notice("Setup for " + now, "Loaded your setup for this server"); } catch (Exception ignored) { }
			}
		}
		lastServer = now;
	}
}
