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

	private static Path dir() {
		return FabricLoader.getInstance().getConfigDir().resolve("surge").resolve("profiles");
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

	private static void saveSlot(int n) {
		save("setup" + n, snapshot());
		Social.notice("Setup " + n + " saved", "Load it any time from Profiles");
	}

	private static void loadSlot(int n) {
		String json = read("setup" + n);
		if (json == null) { Social.notice("Setup " + n + " is empty", "Save your current setup there first"); return; }
		try { restore(json); Social.notice("Setup " + n + " loaded", "Your mods and HUD switched over"); }
		catch (Exception e) { Social.notice("Couldn't load setup " + n, "The file is damaged"); }
	}

	private static void export() {
		String code = PREFIX + Base64.getEncoder().encodeToString(snapshot().getBytes(java.nio.charset.StandardCharsets.UTF_8));
		Minecraft.getInstance().keyboardHandler.setClipboard(code);
		Social.notice("Setup copied", "Paste it to a friend so they can import it");
	}

	private static void importClipboard() {
		String clip = Minecraft.getInstance().keyboardHandler.getClipboard().trim();
		try {
			String json = clip.startsWith(PREFIX) ? new String(Base64.getDecoder().decode(clip.substring(PREFIX.length())), java.nio.charset.StandardCharsets.UTF_8) : clip;
			save("before-import", snapshot());
			restore(json);
			Social.notice("Setup imported", "Your old setup was saved as a backup");
		} catch (Exception e) {
			Social.notice("Nothing to import", "Copy a Surge setup first, then press Import");
		}
	}

	private static void allMods(boolean on) {
		for (SurgeMod m : Mods.all()) if (m.toggleable) m.setEnabled(on);
		refresh();
		Social.notice(on ? "Every mod turned on" : "Every mod turned off", "Use a saved setup to go back");
	}

	private static void reset() {
		save("before-reset", snapshot());
		SurgeConfig c = SurgeConfig.get();
		SurgeConfig.ModuleState keep = c.modules.get(Mods.PROFILES.id);
		c.modules.clear();
		if (keep != null) c.modules.put(Mods.PROFILES.id, keep);
		refresh();
		Social.notice("Everything reset", "Your old setup was saved as a backup");
	}

	public static void buttons(SurgeMod m) {
		m.header("Saved setups");
		for (int i = 1; i <= 3; i++) {
			final int n = i;
			m.button("Save what I have now as setup " + n, "Save", () -> saveSlot(n));
			m.button("Switch to setup " + n, "Load", () -> loadSlot(n));
		}
		m.header("Share");
		m.button("Copy my setup", "Export", Profiles::export);
		m.button("Paste a setup", "Import", Profiles::importClipboard);
		m.header("Everything");
		m.button("Turn every mod off", "All off", () -> allMods(false));
		m.button("Turn every mod on", "All on", () -> allMods(true));
		m.button("Reset all settings", "Reset", Profiles::reset);
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
