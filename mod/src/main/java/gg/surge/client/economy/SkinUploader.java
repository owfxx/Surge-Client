package gg.surge.client.economy;

import net.minecraft.client.Minecraft;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public final class SkinUploader {
	private SkinUploader() {}

	public static Path folder() {
		Path p = Wallet.home().resolve("skins");
		try { Files.createDirectories(p); } catch (Exception ignored) { }
		return p;
	}

	public static List<Path> list() {
		List<Path> out = new ArrayList<>();
		try (Stream<Path> s = Files.list(folder())) {
			s.filter(p -> p.toString().toLowerCase().endsWith(".png")).sorted().forEach(out::add);
		} catch (Exception ignored) { }
		return out;
	}

	public static void openFolder() {
		try { new ProcessBuilder("explorer", folder().toString()).start(); } catch (Exception ignored) { }
	}

	public static CompletableFuture<String> upload(Path png, boolean slim) {
		String token = Minecraft.getInstance().getUser().getAccessToken();
		return CompletableFuture.supplyAsync(() -> {
			try {
				String boundary = "----surge" + UUID.randomUUID();
				ByteArrayOutputStream body = new ByteArrayOutputStream();
				String head = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"variant\"\r\n\r\n" + (slim ? "slim" : "classic") + "\r\n"
					+ "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"skin.png\"\r\nContent-Type: image/png\r\n\r\n";
				body.write(head.getBytes(StandardCharsets.UTF_8));
				body.write(Files.readAllBytes(png));
				body.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
				HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile/skins"))
					.timeout(Duration.ofSeconds(20))
					.header("Authorization", "Bearer " + token)
					.header("Content-Type", "multipart/form-data; boundary=" + boundary)
					.POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
					.build();
				HttpResponse<String> res = HttpClient.newHttpClient().send(req, HttpResponse.BodyHandlers.ofString());
				return res.statusCode() / 100 == 2 ? null : "Failed (" + res.statusCode() + ")";
			} catch (Exception e) {
				return "Failed";
			}
		});
	}
}
