package gg.surge.client.net;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Consumer;

public final class AdminApi {
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
	private static final Path TOKEN = Path.of(System.getProperty("user.home"), ".surgeclient", "admin-session");
	private static String token;

	private AdminApi() {}

	public static boolean signedIn() {
		if (token == null) {
			try { if (Files.exists(TOKEN)) token = Files.readString(TOKEN).trim(); } catch (Exception ignored) { }
			if (token != null && token.isEmpty()) token = null;
		}
		return token != null;
	}

	public static void signOut() {
		token = null;
		try { Files.deleteIfExists(TOKEN); } catch (Exception ignored) { }
	}

	public static void call(String path, JsonObject body, Consumer<JsonObject> ok, Consumer<String> err) {
		HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(SurgeNet.publicUrl() + "/admin/api/" + path)).timeout(Duration.ofSeconds(12)).header("content-type", "application/json");
		if (token != null) b.header("authorization", "Bearer " + token);
		b = body == null ? b.GET() : b.POST(HttpRequest.BodyPublishers.ofString(body.toString()));
		HTTP.sendAsync(b.build(), HttpResponse.BodyHandlers.ofString()).whenComplete((r, e) -> Minecraft.getInstance().execute(() -> {
			if (e != null || r == null) { err.accept("Couldn't reach the Surge service"); return; }
			JsonObject o;
			try {
				JsonElement el = JsonParser.parseString(r.body());
				o = el.isJsonObject() ? el.getAsJsonObject() : new JsonObject();
			} catch (Exception ex) {
				o = new JsonObject();
			}
			if (r.statusCode() == 401 && !path.equals("login")) { signOut(); err.accept("Please sign in again"); return; }
			if (r.statusCode() != 200) { err.accept(o.has("error") ? o.get("error").getAsString() : "Something went wrong (" + r.statusCode() + ")"); return; }
			ok.accept(o);
		}));
	}

	public static void login(String password, Runnable ok, Consumer<String> err) {
		JsonObject b = new JsonObject();
		b.addProperty("password", password);
		call("login", b, o -> {
			token = o.has("token") ? o.get("token").getAsString() : null;
			if (token == null) { err.accept("Sign in failed"); return; }
			try {
				Files.createDirectories(TOKEN.getParent());
				Files.writeString(TOKEN, token);
			} catch (Exception ignored) { }
			ok.run();
		}, err);
	}
}
