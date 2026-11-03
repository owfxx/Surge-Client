package gg.surge.client.features;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerRegions {
	private static final Map<String, String> CACHE = new ConcurrentHashMap<>();
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

	private ServerRegions() {}

	public static String get(String address) {
		if (address == null || address.isBlank()) return "";
		String host = address.trim();
		if (host.startsWith("[")) host = host.substring(1, Math.max(1, host.indexOf(']')));
		else if (host.indexOf(':') == host.lastIndexOf(':') && host.contains(":")) host = host.substring(0, host.indexOf(':'));
		String key = host.toLowerCase();
		String v = CACHE.get(key);
		if (v != null) return v;
		CACHE.put(key, "");
		final String h = key;
		CompletableFuture.runAsync(() -> {
			try {
				String ip = InetAddress.getByName(h).getHostAddress();
				if (ip.startsWith("127.") || ip.startsWith("192.168.") || ip.startsWith("10.")) { CACHE.put(h, "Local network"); return; }
				HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create("https://ipwho.is/" + ip + "?fields=success,continent,country_code,region"))
					.timeout(Duration.ofSeconds(6)).GET().build(), HttpResponse.BodyHandlers.ofString());
				JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
				if (o.has("success") && o.get("success").getAsBoolean()) {
					String cont = o.get("continent").getAsString();
					String cc = o.get("country_code").getAsString();
					CACHE.put(h, cont + ", " + cc);
				}
			} catch (Exception ignored) { }
		});
		return "";
	}
}
