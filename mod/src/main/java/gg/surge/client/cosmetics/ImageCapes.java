package gg.surge.client.cosmetics;

import com.mojang.blaze3d.platform.NativeImage;
import gg.surge.client.net.SurgeNet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ImageCapes {
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
	private static final Map<String, Identifier> READY = new ConcurrentHashMap<>();
	private static final Map<String, Long> LOADING = new ConcurrentHashMap<>();
	private static final Map<String, Long> VERSIONS = new ConcurrentHashMap<>();

	private ImageCapes() {}

	public static void version(String uuidHex, long v) {
		if (uuidHex == null || v <= 0) return;
		Long old = VERSIONS.put(uuidHex, v);
		if (old != null && old != v) {
			Identifier id = READY.remove(uuidHex);
			if (id != null) Minecraft.getInstance().execute(() -> Minecraft.getInstance().getTextureManager().release(id));
		}
	}

	public static Identifier get(String uuidHex) {
		Identifier id = READY.get(uuidHex);
		if (id != null) return id;
		Long started = LOADING.get(uuidHex);
		if (started != null && System.currentTimeMillis() - started < 60_000) return null;
		LOADING.put(uuidHex, System.currentTimeMillis());
		long v = VERSIONS.getOrDefault(uuidHex, 0L);
		HttpRequest req;
		try {
			req = HttpRequest.newBuilder(URI.create(SurgeNet.publicUrl() + "/v1/cape/" + uuidHex + "?v=" + v)).timeout(Duration.ofSeconds(15)).GET().build();
		} catch (Exception e) {
			return null;
		}
		HTTP.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray()).thenAccept(r -> {
			if (r.statusCode() != 200) return;
			try {
				NativeImage src = NativeImage.read(new ByteArrayInputStream(r.body()));
				NativeImage tex = toCape(src);
				src.close();
				Minecraft.getInstance().execute(() -> {
					Identifier nid = Identifier.fromNamespaceAndPath("surge", "capes/image_" + uuidHex + "_" + v);
					Minecraft.getInstance().getTextureManager().register(nid, new DynamicTexture(() -> "surge image cape", tex));
					READY.put(uuidHex, nid);
					LOADING.remove(uuidHex);
				});
			} catch (Exception ignored) { }
		});
		return null;
	}

	static NativeImage toCape(NativeImage src) {
		int sw = src.getWidth(), sh = src.getHeight();
		int w = 512, h = 256, s = 8;
		NativeImage out = new NativeImage(w, h, true);
		if (sw == sh * 2 && sw >= 64) {
			for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) out.setPixel(x, y, src.getPixel(x * sw / w, y * sh / h));
			return out;
		}
		int fw = 10 * s, fh = 16 * s;
		float scale = Math.max(fw / (float) sw, fh / (float) sh);
		float ox = (sw - fw / scale) / 2f, oy = (sh - fh / scale) / 2f;
		long sr = 0, sg = 0, sb = 0, n = 0;
		int[] face = new int[fw * fh];
		for (int y = 0; y < fh; y++) for (int x = 0; x < fw; x++) {
			int c = 0, cnt = 0, rr = 0, gg = 0, bb = 0;
			float x0 = ox + x / scale, y0 = oy + y / scale, x1 = ox + (x + 1) / scale, y1 = oy + (y + 1) / scale;
			for (int yy = (int) y0; yy < Math.max((int) y0 + 1, (int) Math.ceil(y1)); yy++) for (int xx = (int) x0; xx < Math.max((int) x0 + 1, (int) Math.ceil(x1)); xx++) {
				int p = src.getPixel(Math.min(sw - 1, Math.max(0, xx)), Math.min(sh - 1, Math.max(0, yy)));
				rr += (p >> 16) & 255; gg += (p >> 8) & 255; bb += p & 255; cnt++;
			}
			c = 0xFF000000 | ((rr / cnt) << 16) | ((gg / cnt) << 8) | (bb / cnt);
			face[y * fw + x] = c;
			sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; n++;
		}
		int avg = 0xFF000000 | ((int) (sr / n * 0.35) << 16) | ((int) (sg / n * 0.35) << 8) | (int) (sb / n * 0.35);
		for (int y = 0; y < 17 * s; y++) for (int x = 0; x < 22 * s; x++) out.setPixel(x, y, avg);
		for (int y = 0; y < fh; y++) for (int x = 0; x < fw; x++) {
			out.setPixel(s + x, s + y, face[y * fw + x]);
			int c = face[y * fw + (fw - 1 - x)];
			int r = ((c >> 16) & 255) / 3, g = ((c >> 8) & 255) / 3, b = (c & 255) / 3;
			out.setPixel(12 * s + x, s + y, 0xFF000000 | (r << 16) | (g << 8) | b);
		}
		return out;
	}
}
