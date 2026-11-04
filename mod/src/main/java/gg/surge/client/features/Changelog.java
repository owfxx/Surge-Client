package gg.surge.client.features;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Changelog {
	private static String text;

	private Changelog() {}

	private static String text() {
		if (text != null) return text;
		try (InputStream in = Changelog.class.getResourceAsStream("/changelog.md")) {
			text = in == null ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (Exception e) {
			text = "";
		}
		return text;
	}

	public static List<String> forVersion(String version) {
		String v = version.split("[+-]")[0];
		List<String> out = new ArrayList<>();
		boolean in = false;
		for (String line : text().split("\\r?\\n")) {
			String t = line.trim();
			if (t.startsWith("## ")) { if (in) break; in = t.substring(3).trim().equals(v); continue; }
			if (in && !t.isEmpty()) out.add(clean(t));
		}
		return out;
	}

	public static List<String> parse(String body) {
		List<String> out = new ArrayList<>();
		if (body == null) return out;
		for (String line : body.split("\\r?\\n")) {
			String t = line.trim();
			if (t.isEmpty() || t.startsWith("#") || t.matches("(?i).*(https?://|full changelog|what's changed|new contributors| by @).*")) continue;
			out.add(clean(t));
			if (out.size() >= 20) break;
		}
		return out;
	}

	private static String clean(String t) {
		return t.replaceFirst("^[-*•]\\s*", "").replace("**", "").replace("`", "");
	}
}
