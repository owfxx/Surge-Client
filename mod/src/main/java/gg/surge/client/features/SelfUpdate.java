package gg.surge.client.features;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class SelfUpdate {
	public static volatile String installed;

	private SelfUpdate() {}

	private static int[] parts(String v) {
		String core = v.replaceFirst("^v", "").split("[+-]")[0];
		String[] s = core.split("\\.");
		int[] out = new int[3];
		for (int i = 0; i < 3 && i < s.length; i++) {
			try { out[i] = Integer.parseInt(s[i].replaceAll("\\D", "")); } catch (Exception ignored) { }
		}
		return out;
	}

	private static boolean newer(String a, String b) {
		int[] x = parts(a), y = parts(b);
		for (int i = 0; i < 3; i++) if (x[i] != y[i]) return x[i] > y[i];
		return false;
	}

	public static void start() {
		if (!System.getProperty("surge.home", "").isEmpty()) return;
		var mod = FabricLoader.getInstance().getModContainer("surge").orElse(null);
		if (mod == null) return;
		Path self = mod.getOrigin().getPaths().stream().filter(p -> p.toString().endsWith(".jar")).findFirst().orElse(null);
		if (self == null || !Files.isRegularFile(self)) return;
		String current = mod.getMetadata().getVersion().getFriendlyString();
		String mc = current.contains("+mc") ? current.substring(current.indexOf("+mc") + 3) : "1.21.11";
		CompletableFuture.runAsync(() -> {
			try {
				HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
				String repo = System.getProperty("surge.repo", "owfxx/Surge-Client");
				HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + repo + "/releases/latest"))
					.header("Accept", "application/vnd.github+json").header("User-Agent", "surge-client").timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
				if (r.statusCode() != 200) return;
				JsonObject rel = JsonParser.parseString(r.body()).getAsJsonObject();
				String url = null, name = null;
				for (JsonElement e : rel.getAsJsonArray("assets")) {
					JsonObject a = e.getAsJsonObject();
					String n = a.get("name").getAsString();
					if (n.startsWith("surge-client-") && n.endsWith("-mc" + mc + ".jar")) { url = a.get("browser_download_url").getAsString(); name = n; }
				}
				if (url == null) return;
				String latest = name.substring("surge-client-".length(), name.length() - 4);
				if (!newer(latest, current)) return;
				Path dir = self.getParent();
				Path tmp = dir.resolve(name + ".part");
				HttpResponse<Path> d = http.send(HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "surge-client").timeout(Duration.ofMinutes(2)).GET().build(), HttpResponse.BodyHandlers.ofFile(tmp));
				if (d.statusCode() != 200 || Files.size(tmp) < 10_000) { Files.deleteIfExists(tmp); return; }
				Path pending = dir.resolve(name + ".pending");
				Files.move(tmp, pending, StandardCopyOption.REPLACE_EXISTING);
				installed = latest;
				Path fresh = dir.resolve(name);
				Runtime.getRuntime().addShutdownHook(new Thread(() -> swap(self, pending, fresh)));
			} catch (Exception ignored) { }
		});
	}

	private static void swap(Path old, Path pending, Path fresh) {
		try {
			Files.delete(old);
			Files.move(pending, fresh, StandardCopyOption.REPLACE_EXISTING);
			return;
		} catch (Exception ignored) { }
		if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
			try {
				String cmd = "ping -n 4 127.0.0.1 >nul & del /f /q \"" + old + "\" & if not exist \"" + old + "\" move /y \"" + pending + "\" \"" + fresh + "\"";
				new ProcessBuilder("cmd", "/c", cmd).start();
			} catch (Exception ignored) { }
		}
	}
}
