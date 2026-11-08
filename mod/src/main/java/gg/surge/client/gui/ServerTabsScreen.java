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
			List<String> heads = new ArrayList<>();
			if (m == Mode.FRIENDS) for (var f : Social.friends) if (!"offline".equals(f.status()) && heads.size() < 4) heads.add(f.uuid());
			int extra = heads.isEmpty() ? 0 : heads.size() * 7 + 6;
			int w = mc.font.width(label) + 16 + extra;
			boolean on = m == active;
			if (Ui.pill(g, mc.font, mx, my, x, 6, w, 16, "", on) && click && !on) {
				Ui.click();
				mc.setScreen(m == Mode.SERVERS ? serverList : new ServerTabsScreen(serverList, m));
				return;
			}
			g.drawString(mc.font, label, x + 8, 10, on ? 0xFFFFFF55 : Ui.TEXT, true);
			int hx = x + 8 + mc.font.width(label) + 5;
			for (int i = heads.size() - 1; i >= 0; i--) {
				int fx = hx + i * 7;
				g.fill(fx - 1, 9, fx + 9, 19, 0xFF000000);
				gg.surge.client.net.Skins.face(g, heads.get(i), fx, 10, 8);
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
				if (r.statusCode() == 404) { List<Listing> out = new ArrayList<>(); addService(out); discover = out; return; }
				if (r.statusCode() != 200) { List<Listing> out = new ArrayList<>(); addService(out); if (out.isEmpty()) discoverError = "Couldn't load servers (" + r.statusCode() + ")"; else discover = out; return; }
				List<Listing> out = new ArrayList<>();
				JsonElement root = JsonParser.parseString(r.body());
				JsonArray arr = root.isJsonArray() ? root.getAsJsonArray() : root.getAsJsonObject().getAsJsonArray("servers");
				for (JsonElement e : arr) {
					JsonObject o = e.getAsJsonObject();
					String ip = str(o, "ip");
					if (ip.isEmpty()) continue;
					out.add(new Listing(str(o, "name").isEmpty() ? ip : str(o, "name"), ip, str(o, "description"), str(o, "tag")));
				}
				addService(out);
				discover = out;
			})
			.exceptionally(t -> {
				List<Listing> out = new ArrayList<>();
				addService(out);
				if (out.isEmpty()) discoverError = "Couldn't load servers. Check your connection.";
				else discover = out;
				return null;
			});
	}

	private static void addService(List<Listing> out) {
		try {
			JsonElement root = gg.surge.client.economy.Wallet.fetchRemote();
			if (root == null || !root.isJsonObject() || !root.getAsJsonObject().has("servers")) return;
			for (JsonElement e : root.getAsJsonObject().getAsJsonArray("servers")) {
				JsonObject o = e.getAsJsonObject();
				String ip = str(o, "ip");
				if (ip.isEmpty() || out.stream().anyMatch(l -> l.ip().equalsIgnoreCase(ip))) continue;
				out.add(new Listing(str(o, "name").isEmpty() ? ip : str(o, "name"), ip, str(o, "description"), str(o, "tag")));
			}
		} catch (Exception ignored) { }
	}

	private static String str(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
	}

	private static final java.util.concurrent.ExecutorService PINGS = java.util.concurrent.Executors.newFixedThreadPool(3, r -> { Thread t = new Thread(r, "Surge server ping"); t.setDaemon(true); return t; });
	private static final net.minecraft.resources.Identifier LIST_BG = net.minecraft.resources.Identifier.withDefaultNamespace("textures/gui/menu_list_background.png");
	private static final net.minecraft.resources.Identifier HEADER = net.minecraft.resources.Identifier.withDefaultNamespace("textures/gui/header_separator.png");
	private static final net.minecraft.resources.Identifier FOOTER = net.minecraft.resources.Identifier.withDefaultNamespace("textures/gui/footer_separator.png");
	private final net.minecraft.client.multiplayer.ServerStatusPinger pinger = new net.minecraft.client.multiplayer.ServerStatusPinger();
	private final java.util.Map<String, ServerData> pings = new java.util.HashMap<>();
	private final java.util.Map<String, net.minecraft.client.gui.screens.FaviconTexture> icons = new java.util.HashMap<>();
	private final java.util.Map<String, byte[]> iconBytes = new java.util.HashMap<>();
	private String selected;
	private long lastClick;

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double sx, double sy) {
		scrollTarget = Math.max(0, Math.min(contentH, scrollTarget - (float) sy * 24f));
		return true;
	}

	private ServerData ping(String ip, String name) {
		ServerData d = pings.get(ip);
		if (d != null) return d;
		d = new ServerData(name, ip, ServerData.Type.OTHER);
		d.setState(ServerData.State.PINGING);
		pings.put(ip, d);
		ServerData data = d;
		PINGS.submit(() -> {
			try {
				pinger.pingServer(data, () -> { }, () -> { }, net.minecraft.server.network.EventLoopGroupHolder.remote(minecraft.options.useNativeTransport()));
			} catch (Exception e) {
				data.setState(ServerData.State.UNREACHABLE);
				data.motd = Component.literal("Can't reach this server").withStyle(ChatFormatting.DARK_RED);
			}
		});
		return d;
	}

	private net.minecraft.resources.Identifier icon(String ip, ServerData d) {
		byte[] bytes = d.getIconBytes();
		if (bytes == null) return null;
		net.minecraft.client.gui.screens.FaviconTexture t = icons.get(ip);
		if (t == null || iconBytes.get(ip) != bytes) {
			if (t == null) { t = net.minecraft.client.gui.screens.FaviconTexture.forServer(minecraft.getTextureManager(), ip); icons.put(ip, t); }
			try {
				t.upload(com.mojang.blaze3d.platform.NativeImage.read(bytes));
				iconBytes.put(ip, bytes);
			} catch (Exception e) {
				iconBytes.put(ip, bytes);
				return null;
			}
		}
		return t.textureLocation();
	}

	private static int bars(ServerData d) {
		if (d.ping < 0) return 0;
		if (d.ping < 150) return 5;
		if (d.ping < 300) return 4;
		if (d.ping < 600) return 3;
		if (d.ping < 1000) return 2;
		return 1;
	}

	@Override
	public void tick() {
		pinger.tick();
	}

	@Override
	public void removed() {
		pinger.removeAll();
		for (var t : icons.values()) t.close();
		icons.clear();
	}

	private void tile(GuiGraphics g, net.minecraft.resources.Identifier tex, int x1, int y1, int x2, int y2) {
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, x1, y1, 0f, 0f, x2 - x1, y2 - y1, 32, 32);
	}

	private boolean button(GuiGraphics g, int mx, int my, int x, int y, int w, String label, boolean active) {
		boolean hov = active && Ui.hover(mx, my, x, y, w, 20);
		Ui.mcButton(g, x, y, w, 20, hov, !active, 1f);
		g.drawCenteredString(font, label, x + w / 2, y + 6, active ? 0xFFFFFFFF : 0xFFA0A0A0);
		return hov;
	}

	private void join(String ip, String name) {
		ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(ip), new ServerData(name, ip, ServerData.Type.OTHER), false, null);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		boolean wasDown = down[0];
		drawTabs(minecraft, this, servers, g, mx, my, down, mode);
		if (minecraft.screen != this) return;
		boolean click = down[0] && !wasDown;

		g.drawCenteredString(font, mode == Mode.FRIENDS ? "Friends playing now" : "Discover servers", this.width / 2, 12, 0xFFFFFFFF);
		int top = 32, bottom = this.height - 64;
		tile(g, LIST_BG, 0, top, this.width, bottom);
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, HEADER, 0, top - 2, 0f, 0f, this.width, 2, 32, 2);
		g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, FOOTER, 0, bottom, 0f, 0f, this.width, 2, 32, 2);
		int w = Math.min(305, this.width - 20), x = (this.width - w) / 2;
		Runnable action = null;

		List<Row> rows = new ArrayList<>();
		String empty = null;
		if (mode == Mode.FRIENDS) {
			if (!Social.available()) empty = Social.unavailableReason();
			else {
				for (var f : Social.friends) {
					if ("offline".equals(f.status())) continue;
					boolean joinable = !f.server().isEmpty() && !f.server().equals("Singleplayer") && ServerAddress.isValidAddress(f.server());
					String where = f.server().isEmpty() ? "In the menus" : joinable ? "Playing on " + f.server() : f.server();
					rows.add(new Row(f.name(), Social.statusLabel(f.status()), where, joinable ? f.server() : null, Social.statusColor(f.status()), "", f.uuid()));
				}
				if (rows.isEmpty()) empty = Social.friends.isEmpty() ? "Add friends from the Social menu to see where they play." : "None of your friends are online right now.";
			}
		} else {
			if (discoverError != null) empty = discoverError;
			else if (discover == null) empty = "Loading…";
			else {
				for (Listing l : discover) {
					String region = ServerRegions.get(l.ip());
					String tag = (l.tag().isEmpty() ? "" : l.tag()) + (region == null || region.isEmpty() ? "" : (l.tag().isEmpty() ? "" : "  ") + region);
					rows.add(new Row(l.name(), l.description(), "", l.ip(), 0, tag, null));
				}
				if (rows.isEmpty()) empty = "No partnered servers yet. Check back soon.";
			}
		}

		if (empty != null) g.drawCenteredString(font, empty, this.width / 2, (top + bottom) / 2 - 4, 0xFFA0A0A0);
		int rowH = 36;
		contentH = Math.max(0, rows.size() * rowH + 8 - (bottom - top));
		scrollTarget = Math.min(scrollTarget, contentH);
		scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		g.enableScissor(0, top, this.width, bottom);
		int y = top + 4 - Math.round(scroll);
		Row chosen = null;
		for (Row r : rows) {
			String key = r.uuid() != null ? r.uuid() : r.ip();
			boolean sel = key != null && key.equals(selected);
			if (sel) chosen = r;
			boolean hov = Ui.hover(mx, my, x - 2, y - 2, w + 4, rowH) && my >= top && my < bottom;
			if (sel) {
				g.fill(x - 2, y - 2, x + w + 2, y + rowH - 2, 0xFFFFFFFF);
				g.fill(x - 1, y - 1, x + w + 1, y + rowH - 3, 0xFF000000);
			} else if (hov) g.fill(x - 2, y - 2, x + w + 2, y + rowH - 2, 0x30FFFFFF);
			ServerData d = r.ip() != null ? ping(r.ip(), r.title()) : null;
			if (r.uuid() != null) {
				gg.surge.client.net.Skins.face(g, r.uuid(), x, y, 32);
				g.fill(x + 25, y + 25, x + 32, y + 32, 0xFF000000);
				g.fill(x + 26, y + 26, x + 31, y + 31, r.dot());
			} else {
				net.minecraft.resources.Identifier ic = d == null ? null : icon(r.ip(), d);
				if (ic != null) g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ic, x, y, 0f, 0f, 32, 32, 32, 32);
				else g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, net.minecraft.resources.Identifier.withDefaultNamespace("textures/misc/unknown_server.png"), x, y, 0f, 0f, 32, 32, 32, 32);
			}
			if (r.ip() != null && Ui.hover(mx, my, x, y, 32, 32) && my >= top && my < bottom) {
				g.fill(x, y, x + 32, y + 32, 0xA0909090);
				g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, net.minecraft.resources.Identifier.withDefaultNamespace("server_list/join_highlighted"), x, y, 32, 32);
			}
			int tx = x + 35;
			int right = x + w;
			int countW = 0;
			if (d != null) {
				int b = bars(d);
				String sprite = d.state() == ServerData.State.PINGING || d.state() == ServerData.State.INITIAL ? "server_list/pinging_" + (1 + (int) (System.currentTimeMillis() / 100L % 5)) : b == 0 ? "server_list/unreachable" : "server_list/ping_" + b;
				g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, net.minecraft.resources.Identifier.withDefaultNamespace(sprite), right - 15, y, 10, 8);
				if (d.players != null && b > 0) {
					String count = d.players.online() + "/" + d.players.max();
					countW = font.width(count) + 4;
					g.drawString(font, count, right - 17 - font.width(count), y + 1, 0xFF808080, false);
				}
			}
			int room = right - 20 - countW - tx;
			String title = Ui.fit(font, r.title(), room);
			g.drawString(font, title, tx, y + 1, 0xFFFFFFFF, false);
			int tagRoom = room - font.width(title) - 6;
			if (!r.tag().isEmpty() && tagRoom > 20) g.drawString(font, Ui.fit(font, r.tag(), tagRoom), tx + font.width(title) + 6, y + 1, 0xFF8A8A8A, false);
			List<net.minecraft.util.FormattedCharSequence> lines = new ArrayList<>();
			if (mode == Mode.DISCOVER) {
				Component motd = d != null && d.motd != null && !d.motd.getString().isBlank() && d.state() == ServerData.State.SUCCESSFUL ? d.motd : Component.literal(r.sub().isEmpty() ? r.ip() : r.sub()).withStyle(ChatFormatting.GRAY);
				lines = font.split(motd, w - 37);
			} else {
				lines.add(Component.literal(r.sub()).withColor(r.dot() & 0xFFFFFF).getVisualOrderText());
				lines.add(Component.literal(r.extra()).withStyle(ChatFormatting.GRAY).getVisualOrderText());
			}
			for (int i = 0; i < Math.min(2, lines.size()); i++) g.drawString(font, lines.get(i), tx, y + 12 + i * 9, 0xFF808080, false);
			if (hov && click) {
				Row rr = r;
				boolean dbl = key != null && key.equals(selected) && System.currentTimeMillis() - lastClick < 300;
				action = () -> {
					if (dbl && rr.ip() != null) join(rr.ip(), rr.title());
					selected = key;
					lastClick = System.currentTimeMillis();
				};
			}
			y += rowH;
		}
		g.disableScissor();

		int bw = Math.min(100, (this.width - 40) / 4), gap = 4;
		int total = bw * 4 + gap * 3, bx = this.width / 2 - total / 2, by = this.height - 52;
		Row ch = chosen;
		if (button(g, mx, my, bx, by, bw * 2 + gap, mode == Mode.FRIENDS ? "Join Their Server" : "Join Server", ch != null && ch.ip() != null) && click) action = () -> join(ch.ip(), ch.title());
		if (mode == Mode.DISCOVER) {
			if (button(g, mx, my, bx + (bw + gap) * 2, by, bw * 2 + gap, "Add to My Servers", ch != null) && click) action = () -> {
				net.minecraft.client.multiplayer.ServerList list = new net.minecraft.client.multiplayer.ServerList(minecraft);
				list.load();
				if (list.get(ch.ip()) == null) { list.add(new ServerData(ch.title(), ch.ip(), ServerData.Type.OTHER), false); list.save(); }
				minecraft.setScreen(servers instanceof net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen ? new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(parentOf(servers)) : servers);
			};
			if (button(g, mx, my, bx, by + 24, bw * 2 + gap, "Refresh", true) && click) action = () -> { discover = null; pinger.removeAll(); pings.clear(); loadDiscover(); };
		} else {
			if (button(g, mx, my, bx + (bw + gap) * 2, by, bw * 2 + gap, "Open Social", true) && click) action = () -> minecraft.setScreen(gg.surge.client.net.Social.screen(this));
			if (button(g, mx, my, bx, by + 24, bw * 2 + gap, "Refresh", true) && click) action = () -> { Social.refresh(); pinger.removeAll(); pings.clear(); };
		}
		if (button(g, mx, my, bx + (bw + gap) * 2, by + 24, bw * 2 + gap, "Back", true) && click) action = this::onClose;
		if (action != null) { Ui.click(); action.run(); }
	}

	private static Screen parentOf(Screen s) {
		try {
			for (var f : s.getClass().getDeclaredFields()) {
				if (Screen.class.isAssignableFrom(f.getType())) { f.setAccessible(true); Object v = f.get(s); if (v instanceof Screen sc) return sc; }
			}
		} catch (Exception ignored) { }
		return null;
	}

	private record Row(String title, String sub, String extra, String ip, int dot, String tag, String uuid) {}

	@Override
	public void onClose() {
		minecraft.setScreen(servers);
	}
}
