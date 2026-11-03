package gg.surge.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.surge.client.features.ServerRegions;
import gg.surge.client.mods.Colors;
import gg.surge.client.net.Social;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ServerTabsScreen extends Screen {
	public enum Mode { SERVERS, FRIENDS, DISCOVER }

	public record Listing(String name, String ip, String description, String tag) {}

	private static volatile List<Listing> discover;
	private static volatile String discoverError;
	private static volatile long discoverAt;

	private final Screen servers;
	private final Mode mode;
	private final boolean[] down = {true};
	private float scroll, scrollTarget;
	private int contentH;
	private long openedAt;

	public ServerTabsScreen(Screen servers, Mode mode) {
		super(Component.literal(mode == Mode.FRIENDS ? "Friends" : "Discover"));
		this.servers = servers;
		this.mode = mode;
	}

	@Override
	protected void init() {
		openedAt = System.currentTimeMillis();
		if (mode == Mode.FRIENDS) Social.refresh();
		if (mode == Mode.DISCOVER && (discover == null || System.currentTimeMillis() - discoverAt > 5 * 60_000)) loadDiscover();
	}

	public static void drawTabs(Minecraft mc, Screen screen, Screen serverList, GuiGraphics g, int mx, int my, boolean[] wasDown, Mode active) {
		boolean d = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean click = d && !wasDown[0];
		wasDown[0] = d;
		Ui.frame();
		int x = 6;
		for (Mode m : Mode.values()) {
			String label = m == Mode.SERVERS ? "Servers" : m == Mode.FRIENDS ? "Friends" : "Discover";
			int online = 0;
			if (m == Mode.FRIENDS) for (var f : Social.friends) if (!"offline".equals(f.status())) online++;
			if (online > 0) label += " " + online;
			int w = mc.font.width(label) + 16;
			if (Ui.pill(g, mc.font, mx, my, x, 6, w, 16, label, m == active) && click && m != active) {
				Ui.click();
				mc.setScreen(m == Mode.SERVERS ? serverList : new ServerTabsScreen(serverList, m));
				return;
			}
			x += w + 4;
		}
	}

	private static void loadDiscover() {
		discoverAt = System.currentTimeMillis();
		discoverError = null;
		String repo = System.getProperty("surge.repo", "owfxx/Surge-Client");
		if (repo == null || repo.isBlank()) repo = "owfxx/Surge-Client";
		String url = System.getProperty("surge.discoverUrl", "https://github.com/" + repo + "/releases/download/data/discover.json");
		HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
		HttpRequest req;
		try {
			req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET().build();
		} catch (Exception e) {
			discoverError = "Couldn't load servers.";
			return;
		}
		http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
			.thenAccept(r -> {
				if (r.statusCode() == 404) { discover = List.of(); return; }
				if (r.statusCode() != 200) { discoverError = "Couldn't load servers (" + r.statusCode() + ")"; return; }
				List<Listing> out = new ArrayList<>();
				JsonElement root = JsonParser.parseString(r.body());
				JsonArray arr = root.isJsonArray() ? root.getAsJsonArray() : root.getAsJsonObject().getAsJsonArray("servers");
				for (JsonElement e : arr) {
					JsonObject o = e.getAsJsonObject();
					String ip = str(o, "ip");
					if (ip.isEmpty()) continue;
					out.add(new Listing(str(o, "name").isEmpty() ? ip : str(o, "name"), ip, str(o, "description"), str(o, "tag")));
				}
				discover = out;
			})
			.exceptionally(t -> { discoverError = "Couldn't load servers. Check your connection."; return null; });
	}

	private static String str(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double sx, double sy) {
		scrollTarget = Math.max(0, Math.min(contentH, scrollTarget - (float) sy * 24f));
		return true;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		boolean wasDown = down[0];
		drawTabs(minecraft, this, servers, g, mx, my, down, mode);
		if (minecraft.screen != this) return;
		boolean click = down[0] && !wasDown;

		Ui.centered(g, font, mode == Mode.FRIENDS ? "Friends playing now" : "Discover servers", this.width / 2, 12, 0xFFFFFFFF);
		int w = Math.min(420, this.width - 24), x = (this.width - w) / 2, top = 32, bottom = this.height - 40;
		Runnable action = null;

		List<Row> rows = new ArrayList<>();
		String empty = null;
		if (mode == Mode.FRIENDS) {
			if (!Social.available()) empty = Social.unavailableReason();
			else {
				for (var f : Social.friends) {
					if ("offline".equals(f.status())) continue;
					String where = f.server().isEmpty() ? "In the menus" : f.server();
					boolean joinable = !f.server().isEmpty() && !f.server().equals("Singleplayer") && ServerAddress.isValidAddress(f.server());
					rows.add(new Row(f.name(), Social.statusLabel(f.status()) + "  " + where, joinable ? f.server() : null, Social.statusColor(f.status()), ""));
				}
				if (rows.isEmpty()) empty = Social.friends.isEmpty() ? "Add friends from the Social menu to see where they play." : "None of your friends are online right now.";
			}
		} else {
			if (discoverError != null) empty = discoverError;
			else if (discover == null) empty = "Loading…";
			else {
				for (Listing l : discover) {
					String region = ServerRegions.get(l.ip());
					rows.add(new Row(l.name(), l.description().isEmpty() ? l.ip() : l.description(), l.ip(), 0, (l.tag().isEmpty() ? "" : l.tag()) + (region == null || region.isEmpty() ? "" : (l.tag().isEmpty() ? "" : "  ") + region)));
				}
				if (rows.isEmpty()) empty = "No partnered servers yet. Check back soon.";
			}
		}

		if (empty != null) Ui.centered(g, font, empty, this.width / 2, (top + bottom) / 2 - 4, Ui.MUTED);
		int rowH = 36;
		contentH = Math.max(0, rows.size() * (rowH + 4) - (bottom - top));
		scrollTarget = Math.min(scrollTarget, contentH);
		scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		g.enableScissor(0, top, this.width, bottom);
		int y = top - Math.round(scroll);
		float appear = Math.min(1f, (System.currentTimeMillis() - openedAt) / 250f);
		for (Row r : rows) {
			boolean hov = Ui.hover(mx, my, x, y, w, rowH) && my >= top && my < bottom;
			Ui.box(g, x, y, w, rowH, Colors.alpha(hov ? 0xE0303036 : 0xC0202024, appear), hov ? 0xFF55555C : Ui.EDGE);
			int tx = x + 10;
			if (r.dot() != 0) { g.fill(x + 8, y + 9, x + 13, y + 14, r.dot()); tx = x + 18; }
			g.drawString(font, Component.literal(r.title()).withStyle(ChatFormatting.BOLD), tx, y + 8, 0xFFFFFFFF, false);
			if (!r.tag().isEmpty()) g.drawString(font, r.tag(), tx + font.width(Component.literal(r.title()).withStyle(ChatFormatting.BOLD)) + 8, y + 8, Ui.ACCENT_2, false);
			g.drawString(font, Ui.fit(font, r.sub(), w - 90), x + 10, y + 21, Ui.MUTED, false);
			if (r.ip() != null) {
				if (Ui.pill(g, font, mx, my, x + w - 58, y + 10, 50, 16, "Join", true) && hov && click) {
					String ip = r.ip(), name = r.title();
					action = () -> ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(ip), new ServerData(name, ip, ServerData.Type.OTHER), false, null);
				}
			}
			y += rowH + 4;
		}
		g.disableScissor();

		int bw = 120;
		if (Ui.pill(g, font, mx, my, this.width / 2 - bw / 2, this.height - 30, bw, 20, "Back", false) && click) action = this::onClose;
		if (mode == Mode.DISCOVER && Ui.pill(g, font, mx, my, this.width / 2 + bw / 2 + 6, this.height - 30, 70, 20, "Refresh", false) && click) action = () -> { discover = null; loadDiscover(); };
		if (action != null) { Ui.click(); action.run(); }
	}

	private record Row(String title, String sub, String ip, int dot, String tag) {}

	@Override
	public void onClose() {
		minecraft.setScreen(servers);
	}
}
