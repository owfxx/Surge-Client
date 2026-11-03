package gg.surge.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SurgeConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("surge.json");
	private static SurgeConfig instance;

	public static class ModuleState {
		public boolean enabled;
		public float x;
		public float y;
		public float scale = 1f;
		public Map<String, Object> opts = new LinkedHashMap<>();

		public ModuleState() {}

		public ModuleState(boolean enabled, float x, float y) {
			this.enabled = enabled;
			this.x = x;
			this.y = y;
		}
	}

	public static class CommandKey {
		public int key = -1;
		public String text = "";
	}

	public static class Waypoint {
		public String name = "Waypoint";
		public int x, y, z;
		public String world = "";
		public String color = "B483FF";
		public boolean visible = true;
	}

	public Map<String, ModuleState> modules = new LinkedHashMap<>();
	public List<CommandKey> commandKeys = new ArrayList<>();
	public List<Waypoint> waypoints = new ArrayList<>();

	public Map<String, Boolean> boost = new LinkedHashMap<>();
	public int boostVersion = 0;
	public int cullItems = 40, cullBlockEntities = 64, cullParticles = 32, nametagDistance = 48;
	public Map<String, Object> saved = new LinkedHashMap<>();

	public String halo = "none";
	public String wings = "none";
	public String cape = "none";

	public static class Outfit {
		public String halo = "none", wings = "none", cape = "none";
	}

	public Outfit[] outfits = {null, null, null};

	public static class BetaCape {
		public String design = "logo";
		public String top = "8F50F2", bottom = "EC68B0";
		public boolean shimmer = true, glow = true;
	}

	public BetaCape betaCape = new BetaCape();

	public List<List<String>> emotePages = null;

	public static SurgeConfig get() {
		if (instance == null) load();
		return instance;
	}

	public ModuleState module(String id, boolean defEnabled, float defX, float defY) {
		ModuleState s = modules.computeIfAbsent(id, k -> new ModuleState(defEnabled, defX, defY));
		if (s.opts == null) s.opts = new LinkedHashMap<>();
		return s;
	}

	public static void load() {
		if (Files.exists(FILE)) {
			try (Reader r = Files.newBufferedReader(FILE)) {
				instance = GSON.fromJson(r, SurgeConfig.class);
			} catch (Exception e) {
				instance = null;
			}
		}
		if (instance == null) instance = new SurgeConfig();
		if (instance.modules == null) instance.modules = new LinkedHashMap<>();
		if (instance.commandKeys == null) instance.commandKeys = new ArrayList<>();
		if (instance.waypoints == null) instance.waypoints = new ArrayList<>();
		if (instance.boost == null) instance.boost = new LinkedHashMap<>();
		if (instance.saved == null) instance.saved = new LinkedHashMap<>();
		if (instance.outfits == null || instance.outfits.length != 3) instance.outfits = new Outfit[3];
		if (instance.halo == null) instance.halo = "none";
		if (instance.wings == null) instance.wings = "none";
		if (instance.cape == null) instance.cape = "none";
		if (instance.betaCape == null) instance.betaCape = new BetaCape();
	}

	private static boolean dirty;
	private static long dirtyAt;

	public static void markDirty() {
		dirty = true;
		dirtyAt = System.currentTimeMillis();
	}

	public static void flush() {
		if (dirty && System.currentTimeMillis() - dirtyAt > 400) save();
	}

	public static void save() {
		dirty = false;
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer w = Files.newBufferedWriter(FILE)) {
				GSON.toJson(get(), w);
			}
		} catch (Exception ignored) {
		}
	}
}
