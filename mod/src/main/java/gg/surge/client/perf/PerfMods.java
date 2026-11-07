package gg.surge.client.perf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class PerfMods {
	public record Mod(String slug, String modId, String title, String description) {}

	public static final List<Mod> KNOWN = List.of(
		new Mod("sodium", "sodium", "Sodium", "Rewrites how the game draws chunks. Often doubles your FPS."),
		new Mod("lithium", "lithium", "Lithium", "Faster game logic, mostly helps in singleplayer."),
		new Mod("ferrite-core", "ferritecore", "FerriteCore", "Uses a lot less RAM."),
		new Mod("c2me-fabric", "c2me", "C2ME", "Loads and generates chunks on more CPU cores."),
		new Mod("badoptimizations", "badoptimizations", "BadOptimizations", "Many small speedups for lighting, sky and more."),
		new Mod("vmp-fabric", "vmp", "Very Many Players", "Smoother play on busy servers and with lots of entities."),
		new Mod("clumps", "clumps", "Clumps", "Merges XP orbs so farms don't lag."),
		new Mod("entityculling", "entityculling", "Entity Culling", "Skips mobs and chests hidden behind walls."),
		new Mod("immediatelyfast", "immediatelyfast", "ImmediatelyFast", "Faster HUD, text and item rendering."),
		new Mod("modernfix", "modernfix", "ModernFix", "Faster start-up and less RAM."),
		new Mod("dynamic-fps", "dynamic_fps", "Dynamic FPS", "Lower FPS when the game is in the background.")
	);


	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, String> status = new LinkedHashMap<>();
	private static final Map<String, Boolean> wanted = new LinkedHashMap<>();
	private static boolean hooked;

	private PerfMods() {}

	public static List<Mod> list() {
		return KNOWN;
	}

	private static Path modsDir() {
		return FabricLoader.getInstance().getGameDir().resolve("mods");
	}

	public static boolean loaded(Mod m) {
		return FabricLoader.getInstance().isModLoaded(m.modId());
	}

	private static Path loadedJar(Mod m) {
		return FabricLoader.getInstance().getModContainer(m.modId()).flatMap(c -> c.getOrigin().getPaths().stream().filter(p -> p.toString().endsWith(".jar")).findFirst()).orElse(null);
	}

	private static Path disabledJar(Mod m) {
		String key = m.slug().replace("-", "").toLowerCase(Locale.ROOT);
		try (Stream<Path> s = Files.list(modsDir())) {
			return s.filter(p -> {
				String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
				return n.endsWith(".jar.disabled") && n.replace("-", "").replace("_", "").startsWith(key);
			}).findFirst().orElse(null);
		} catch (Exception e) {
			return null;
		}
	}

	public static boolean wanted(Mod m) {
		return wanted.getOrDefault(m.slug(), loaded(m));
	}

	public static boolean needsRestart(Mod m) {
		return wanted(m) != loaded(m);
	}

	public static String status(Mod m) {
		String s = status.get(m.slug());
		if (s != null) return s;
		return needsRestart(m) ? "next start" : null;
	}

	public static void toggle(Mod m) {
		boolean next = !wanted(m);
		wanted.put(m.slug(), next);
		if (next == loaded(m)) {
			wanted.remove(m.slug());
			status.remove(m.slug());
			Path off = disabledJar(m);
			if (next && off != null) return;
			savePending();
			return;
		}
		if (next) {
			Path off = disabledJar(m);
			if (off != null) {
				try {
					String n = off.getFileName().toString();
					Files.move(off, off.resolveSibling(n.substring(0, n.length() - ".disabled".length())), StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception ignored) { }
			} else download(m);
		} else hookShutdown();
		savePending();
	}

	private static void savePending() {
		Map<String, Boolean> out = new LinkedHashMap<>();
		for (Mod m : KNOWN) {
			if (!wanted.containsKey(m.slug()) || wanted.get(m.slug())) continue;
			Path jar = loadedJar(m);
			if (jar != null) out.put(jar.getFileName().toString(), false);
		}
		try {
			Path f = FabricLoader.getInstance().getGameDir().resolve("surge-mods.json");
			if (out.isEmpty()) Files.deleteIfExists(f);
			else Files.writeString(f, GSON.toJson(out));
		} catch (Exception ignored) { }
	}

	private static void download(Mod m) {
		status.put(m.slug(), "downloading");
		String mc = FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("1.21.11");
		CompletableFuture.runAsync(() -> {
			try {
				HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
				String q = "loaders=" + URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8) + "&game_versions=" + URLEncoder.encode("[\"" + mc + "\"]", StandardCharsets.UTF_8);
				HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create("https://api.modrinth.com/v2/project/" + m.slug() + "/version?" + q))
					.header("User-Agent", "surge-client").timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
				JsonArray versions = r.statusCode() == 200 ? JsonParser.parseString(r.body()).getAsJsonArray() : new JsonArray();
				if (versions.isEmpty()) { status.put(m.slug(), "not for " + mc); wanted.remove(m.slug()); return; }
				JsonObject file = null;
				for (var f : versions.get(0).getAsJsonObject().getAsJsonArray("files")) {
					JsonObject o = f.getAsJsonObject();
					if (file == null || (o.has("primary") && o.get("primary").getAsBoolean())) file = o;
				}
				if (file == null) { status.put(m.slug(), "failed"); wanted.remove(m.slug()); return; }
				String name = file.get("filename").getAsString().replaceAll("[\\\\/]", "");
				Path tmp = modsDir().resolve(name + ".part");
				HttpResponse<Path> d = http.send(HttpRequest.newBuilder(URI.create(file.get("url").getAsString())).header("User-Agent", "surge-client").timeout(Duration.ofMinutes(2)).GET().build(), HttpResponse.BodyHandlers.ofFile(tmp));
				if (d.statusCode() != 200) { Files.deleteIfExists(tmp); status.put(m.slug(), "failed"); wanted.remove(m.slug()); return; }
				Files.move(tmp, modsDir().resolve(name), StandardCopyOption.REPLACE_EXISTING);
				status.remove(m.slug());
			} catch (Exception e) {
				status.put(m.slug(), "failed");
				wanted.remove(m.slug());
			}
		});
	}

	private static void hookShutdown() {
		if (hooked) return;
		hooked = true;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			List<String> cmds = new ArrayList<>();
			for (Mod m : KNOWN) {
				if (wanted.getOrDefault(m.slug(), true) || !loaded(m)) continue;
				Path jar = loadedJar(m);
				if (jar == null) continue;
				Path off = jar.resolveSibling(jar.getFileName() + ".disabled");
				try {
					Files.move(jar, off, StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception e) {
					cmds.add("move /y \"" + jar + "\" \"" + off + "\"");
				}
			}
			if (!cmds.isEmpty() && System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
				try {
					new ProcessBuilder("cmd", "/c", "ping -n 4 127.0.0.1 >nul & " + String.join(" & ", cmds)).start();
				} catch (Exception ignored) { }
			}
		}));
	}
}
