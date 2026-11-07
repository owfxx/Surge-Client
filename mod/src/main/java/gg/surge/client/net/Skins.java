package gg.surge.client.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Skins {
	public record Skin(Identifier texture, int height, boolean slim) {}

	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build();
	private static final Map<String, Skin> LOADED = new ConcurrentHashMap<>();
	private static final Map<String, Long> TRIED = new ConcurrentHashMap<>();
	private static final Deque<String> QUEUE = new ArrayDeque<>();
	private static Thread worker;

	private Skins() {}

	private static String clean(String uuid) {
		return uuid == null ? "" : uuid.replace("-", "").toLowerCase();
	}

	public static Skin get(String rawUuid) {
		String id = clean(rawUuid);
		if (id.length() != 32) return null;
		Skin s = LOADED.get(id);
		if (s != null) return s;
		Long at = TRIED.get(id);
		if (at == null || System.currentTimeMillis() - at > 10 * 60_000) {
			TRIED.put(id, System.currentTimeMillis());
			synchronized (QUEUE) {
				if (!QUEUE.contains(id)) QUEUE.addLast(id);
				if (worker == null) {
					worker = new Thread(Skins::loop, "Surge Skins");
					worker.setDaemon(true);
					worker.start();
				}
				QUEUE.notifyAll();
			}
		}
		return null;
	}

	private static void loop() {
		while (true) {
			String id;
			synchronized (QUEUE) {
				while (QUEUE.isEmpty()) {
					try { QUEUE.wait(); } catch (InterruptedException e) { return; }
				}
				id = QUEUE.pollFirst();
			}
			try {
				load(id);
			} catch (Exception ignored) { }
			try { Thread.sleep(350); } catch (InterruptedException e) { return; }
		}
	}

	private static void load(String id) throws Exception {
		HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + id)).timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString());
		if (r.statusCode() != 200) return;
		JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
		String url = null;
		boolean slim = false;
		for (var p : o.getAsJsonArray("properties")) {
			JsonObject prop = p.getAsJsonObject();
			if (!"textures".equals(prop.get("name").getAsString())) continue;
			JsonObject tex = JsonParser.parseString(new String(Base64.getDecoder().decode(prop.get("value").getAsString()), StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject t = tex.getAsJsonObject("textures");
			if (t != null && t.has("SKIN")) {
				JsonObject skin = t.getAsJsonObject("SKIN");
				url = skin.get("url").getAsString();
				if (skin.has("metadata") && skin.getAsJsonObject("metadata").has("model")) slim = "slim".equals(skin.getAsJsonObject("metadata").get("model").getAsString());
			}
		}
		if (url == null) return;
		if (url.startsWith("http://")) url = "https://" + url.substring(7);
		HttpResponse<byte[]> img = HTTP.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
		if (img.statusCode() != 200) return;
		byte[] bytes = img.body();
		boolean slimF = slim;
		Minecraft.getInstance().execute(() -> {
			try {
				NativeImage ni = NativeImage.read(bytes);
				int h = ni.getHeight();
				Identifier tid = Identifier.fromNamespaceAndPath("surge", "skins/" + id);
				Minecraft.getInstance().getTextureManager().register(tid, new DynamicTexture(() -> "surge skin " + id, ni));
				LOADED.put(id, new Skin(tid, h, slimF));
			} catch (Exception ignored) { }
		});
	}

	public static void face(GuiGraphics g, String rawUuid, int x, int y, int size) {
		Minecraft mc = Minecraft.getInstance();
		UUID uid = null;
		try { String c = clean(rawUuid); uid = UUID.fromString(c.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5")); } catch (Exception ignored) { }
		if (uid != null && mc.getConnection() != null) {
			PlayerInfo info = mc.getConnection().getPlayerInfo(uid);
			if (info != null) { PlayerFaceRenderer.draw(g, info.getSkin(), x, y, size); return; }
		}
		Skin s = get(rawUuid);
		if (s != null) {
			g.blit(RenderPipelines.GUI_TEXTURED, s.texture(), x, y, 8f, 8f, size, size, 8, 8, 64, s.height());
			g.blit(RenderPipelines.GUI_TEXTURED, s.texture(), x, y, 40f, 8f, size, size, 8, 8, 64, s.height());
			return;
		}
		PlayerFaceRenderer.draw(g, uid != null ? DefaultPlayerSkin.get(uid) : DefaultPlayerSkin.getDefaultSkin(), x, y, size);
	}
}
