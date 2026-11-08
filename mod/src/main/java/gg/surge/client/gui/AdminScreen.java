package gg.surge.client.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import gg.surge.client.cosmetics.CapturedLook;
import gg.surge.client.cosmetics.Preview;
import gg.surge.client.economy.Shop;
import gg.surge.client.emotes.Emotes;
import gg.surge.client.net.AdminApi;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AdminScreen extends Screen {
	public static final String OWNER = "chroynic";
	private static final int[] COIN_PRESETS = {0, 600, 1000, 2500, 5000, 10000};
	private static final int[] DURATIONS_H = {0, 24, 72, 168};
	private static final String[] DURATION_NAMES = {"No end", "1 day", "3 days", "1 week"};
	private static final String[][] GROUPS = {{"all", "Everything"}, {"cosmetic", "All cosmetics"}, {"emote", "All emotes"},
		{"sub:wings", "All wings"}, {"sub:halo", "All halos"}, {"sub:cape", "All capes"}};

	private enum Tab { CODES("Codes"), GIFTS("Gifts"), SALES("Sales"), PRICES("Prices"), PLAYERS("Icons"), LOCKS("Locks"), SERVERS("Servers");
		final String label;
		Tab(String l) { label = l; }
	}

	private final Screen parent;
	private Tab tab = Tab.CODES;
	private boolean wasDown = true, loading;
	private String message = "";
	private int messageColor = Ui.MUTED;
	private long messageUntil;
	private int px, py, pw, ph;
	private EditBox password, coinsBox, codeBox, countBox, usesBox, srvName, srvIp, srvDesc, giftWho, giftCoins, giftNote, saleWho, priceBox, pricePct, badgeWho, capeWho;
	private final Set<String> giftPicked = new LinkedHashSet<>();
	private JsonArray gifts = new JsonArray(), badges = new JsonArray(), capes = new JsonArray();
	private JsonObject prices = new JsonObject();
	private String priceItem = "cape:surge", badgeKind = "booster", capeFile = "";
	private byte[] capePng;
	private final Set<String> picked = new LinkedHashSet<>();
	private String saleTarget = "all";
	private int percent = 20, durationIdx = 1;
	private float gridScroll, listScroll;
	private JsonArray codes = new JsonArray(), sales = new JsonArray(), servers = new JsonArray();
	private final Set<String> locked = new LinkedHashSet<>();
	private boolean loaded;
	private List<gg.surge.client.economy.AdminCodes.Code> oldCodes = List.of();

	public AdminScreen(Screen parent) {
		super(Component.literal("Admin"));
		this.parent = parent;
	}

	public static boolean allowed(net.minecraft.client.Minecraft mc) {
		return mc.getUser() != null && OWNER.equalsIgnoreCase(mc.getUser().getName());
	}

	private EditBox box(String hint, int max, String keep) {
		EditBox b = new EditBox(font, 0, 0, 80, 10, Component.literal(hint));
		b.setBordered(false);
		b.setMaxLength(max);
		b.setValue(keep == null ? "" : keep);
		b.setHint(Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY));
		b.visible = false;
		addRenderableWidget(b);
		return b;
	}

	@Override
	protected void init() {
		pw = Math.min(520, this.width - 12);
		ph = Math.min(330, this.height - 12);
		px = (this.width - pw) / 2;
		py = (this.height - ph) / 2;
		clearWidgets();
		password = box("Admin password", 128, password == null ? "" : password.getValue());
		coinsBox = box("0", 7, coinsBox == null ? "1000" : coinsBox.getValue());
		codeBox = box("Random", 24, codeBox == null ? "" : codeBox.getValue());
		countBox = box("1", 3, countBox == null ? "1" : countBox.getValue());
		usesBox = box("Unlimited", 6, usesBox == null ? "" : usesBox.getValue());
		srvName = box("Name", 32, srvName == null ? "" : srvName.getValue());
		srvIp = box("play.example.net", 64, srvIp == null ? "" : srvIp.getValue());
		srvDesc = box("Short description", 80, srvDesc == null ? "" : srvDesc.getValue());
		giftWho = box("Player name", 36, giftWho == null ? "" : giftWho.getValue());
		giftCoins = box("0", 7, giftCoins == null ? "0" : giftCoins.getValue());
		giftNote = box("Message (optional)", 140, giftNote == null ? "" : giftNote.getValue());
		saleWho = box("Everyone", 36, saleWho == null ? "" : saleWho.getValue());
		priceBox = box("Price", 7, priceBox == null ? "" : priceBox.getValue());
		pricePct = box("100", 4, pricePct == null ? "100" : pricePct.getValue());
		badgeWho = box("Player name", 36, badgeWho == null ? "" : badgeWho.getValue());
		capeWho = box("Player name", 36, capeWho == null ? "" : capeWho.getValue());
		oldCodes = gg.surge.client.economy.AdminCodes.list();
		if (AdminApi.signedIn() && !loaded) refresh();
	}

	private void toast(String s, int color) {
		message = s;
		messageColor = color;
		messageUntil = System.currentTimeMillis() + 4000;
	}

	private void refresh() {
		loading = true;
		AdminApi.call("state", null, o -> {
			loading = false;
			loaded = true;
			codes = o.has("codes") ? o.getAsJsonArray("codes") : new JsonArray();
			sales = o.has("sales") ? o.getAsJsonArray("sales") : new JsonArray();
			servers = o.has("servers") ? o.getAsJsonArray("servers") : new JsonArray();
			locked.clear();
			if (o.has("locked")) for (JsonElement e : o.getAsJsonArray("locked")) locked.add(e.getAsString());
			gifts = o.has("gifts") ? o.getAsJsonArray("gifts") : new JsonArray();
			badges = o.has("badges") ? o.getAsJsonArray("badges") : new JsonArray();
			capes = o.has("capes") ? o.getAsJsonArray("capes") : new JsonArray();
			prices = o.has("prices") && o.get("prices").isJsonObject() ? o.getAsJsonObject("prices") : new JsonObject();
		}, e -> { loading = false; toast(e, 0xFFFF7A7A); });
	}

	private void post(String path, JsonObject body, String done) {
		loading = true;
		AdminApi.call(path, body, o -> {
			loading = false;
			if (o.has("made")) {
				List<String> made = new ArrayList<>();
				for (JsonElement e : o.getAsJsonArray("made")) made.add(e.getAsString());
				minecraft.keyboardHandler.setClipboard(String.join("\n", made));
				toast((made.size() == 1 ? "Code " + made.get(0) : made.size() + " codes") + " made and copied", Ui.ON);
			} else if (done != null) toast(done, Ui.ON);
			refresh();
			gg.surge.client.economy.Wallet.refreshRemote(true);
		}, e -> { loading = false; toast(e, 0xFFFF7A7A); });
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, 0xB0000000);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		if (mx > px + pw * 0.45) gridScroll = Math.max(0, gridScroll - (float) sy * 24);
		else listScroll = Math.max(0, listScroll - (float) sy * 20);
		return true;
	}

	private static String itemLabel(String id) {
		for (String[] g : GROUPS) if (g[0].equals(id)) return g[1];
		Shop.Item i = Shop.byId(id);
		return i == null ? id : i.name();
	}

	private static int parse(EditBox b, int def) {
		try { return Integer.parseInt(b.getValue().trim()); } catch (Exception e) { return def; }
	}

	private void field(GuiGraphics g, EditBox b, int x, int y, int w, int mx, int my, float delta) {
		Ui.box(g, x, y, w, 16, 0xFF161619, b.isFocused() ? Ui.ACCENT : Ui.EDGE);
		b.setX(x + 5);
		b.setY(y + 4);
		b.setWidth(w - 10);
		b.visible = true;
	}

	private interface CellAction { void run(Shop.Item item); }

	private Runnable grid(GuiGraphics g, int x, int y, int w, int h, int mx, int my, boolean click, java.util.function.Predicate<Shop.Item> selected, java.util.function.Predicate<Shop.Item> dim, CellAction act) {
		Runnable action = null;
		int cell = 46, cols = Math.max(1, w / cell), cw = w / cols, chh = 56;
		List<Shop.Item> items = Shop.ITEMS;
		int rows = (items.size() + cols - 1) / cols;
		gridScroll = Math.min(gridScroll, Math.max(0, rows * chh - h));
		g.enableScissor(x, y, x + w, y + h);
		for (int i = 0; i < items.size(); i++) {
			Shop.Item it = items.get(i);
			int cx = x + (i % cols) * cw, cy = y + (i / cols) * chh - Math.round(gridScroll);
			if (cy + chh < y || cy > y + h) continue;
			boolean sel = selected.test(it), hov = Ui.hover(mx, my, cx + 1, cy + 1, cw - 2, chh - 2) && my >= y && my < y + h;
			Ui.cutBox(g, cx + 1, cy + 1, cw - 2, chh - 2, 2, sel ? 0xFF2F4A2A : hov ? 0xFF2E2E33 : 0xFF202024, sel ? 0xFF6BD35A : hov ? 0xFF8A8A90 : 0xFF2E2E33);
			CapturedLook look;
			float turn = 20f;
			if (it.category().equals("emote")) {
				Emotes.Emote e = Emotes.Emote.byName(it.key());
				look = new CapturedLook(null, null, null, e, (System.currentTimeMillis() % 100000) / 1000f + i * 0.3f, true);
			} else {
				String sub = it.sub();
				look = new CapturedLook(sub.equals("cape") ? it.key() : null, sub.equals("wings") ? it.key() : null, sub.equals("halo") ? it.key() : null, null, 0f, false);
				if (sub.equals("cape") || sub.equals("wings")) turn = 200f;
			}
			if (it.sub().equals("halo")) {
				g.enableScissor(cx + 2, Math.max(y, cy + 2), cx + cw - 2, Math.min(y + h, cy + chh - 12));
				Preview.drawCard(g, cx + 2, cy + 14, cx + cw - 2, cy + chh + 14, 24, 25f, look);
				g.disableScissor();
			} else Preview.drawCard(g, cx + 2, cy + 2, cx + cw - 2, cy + chh - 12, 17, turn, look);
			if (dim.test(it)) {
				g.fill(cx + 2, cy + 2, cx + cw - 2, cy + chh - 2, 0x90200000);
				Ui.icon(g, new String[]{"..XXX...", ".X...X..", ".X...X..", "XXXXXXX.", "XXX.XXX.", "XXX.XXX.", "XXXXXXX.", "........"}, cx + cw - 12, cy + 4, 1, 0xFFFF6B6B);
			}
			String name = Ui.fit(font, it.name().replace(" (emote)", ""), cw - 4);
			g.pose().pushMatrix();
			g.pose().translate(cx + cw / 2f, cy + chh - 10);
			g.pose().scale(0.75f, 0.75f);
			g.drawString(font, name, -font.width(name) / 2, 0, sel ? 0xFFB6F5A8 : Ui.TEXT, true);
			g.pose().popMatrix();
			if (hov && click) action = () -> act.run(it);
		}
		g.disableScissor();
		return action;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		renderBackground(g, mx, my, delta);
		Ui.frame();
		boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasDown;
		wasDown = down;
		Runnable action = null;
		for (EditBox b : new EditBox[]{password, coinsBox, codeBox, countBox, usesBox, srvName, srvIp, srvDesc, giftWho, giftCoins, giftNote, saleWho, priceBox, pricePct, badgeWho, capeWho}) b.visible = false;

		Ui.shadow(g, px, py, pw, ph);
		Ui.box(g, px, py, pw, ph, Ui.BG, Ui.EDGE);
		Ui.logo(g, px + 9, py + 7, 16);
		g.drawString(font, Component.literal("Admin").withStyle(ChatFormatting.BOLD), px + 30, py + 11, Ui.TEXT, true);
		if (Ui.iconButton(g, mx, my, px + pw - 26, py + 6, 18, Ui.CROSS) && click) action = this::onClose;

		if (!AdminApi.signedIn()) {
			int cx = px + pw / 2, y = py + ph / 2 - 40;
			Ui.centered(g, font, "Sign in with the admin password", cx, y, Ui.TEXT);
			Ui.centered(g, font, "It's the ADMIN_PASSWORD secret of the Surge service", cx, y + 12, Ui.MUTED);
			field(g, password, cx - 90, y + 30, 180, mx, my, delta);
			if (Ui.pill(g, font, mx, my, cx - 45, y + 54, 90, 18, loading ? "Signing in…" : "Sign in", false) && click && !loading) action = this::signIn;
		} else {
			int tx = px + 34 + font.width(Component.literal("Admin").withStyle(ChatFormatting.BOLD)) + 8;
			for (Tab t : Tab.values()) {
				int tw = font.width(t.label) + 10;
				if (Ui.pill(g, font, mx, my, tx, py + 7, tw, 16, t.label, tab == t) && click) { Tab nt = t; action = () -> { tab = nt; gridScroll = listScroll = 0; }; }
				tx += tw + 4;
			}
			if (tx + 60 < px + pw - 30 && Ui.pill(g, font, mx, my, px + pw - 88, py + 7, 58, 16, loading ? "…" : "Refresh", false) && click) action = this::refresh;
			g.fill(px + 1, py + 29, px + pw - 1, py + 30, Ui.EDGE);
			Runnable a = switch (tab) {
				case CODES -> codesTab(g, mx, my, click, delta);
				case SALES -> salesTab(g, mx, my, click, delta);
				case GIFTS -> giftsTab(g, mx, my, click, delta);
				case PRICES -> pricesTab(g, mx, my, click, delta);
				case PLAYERS -> playersTab(g, mx, my, click, delta);
				case LOCKS -> locksTab(g, mx, my, click);
				case SERVERS -> serversTab(g, mx, my, click, delta);
			};
			if (a != null) action = a;
			if (Ui.pill(g, font, mx, my, px + 8, py + ph - 22, 60, 16, "Sign out", false) && click) action = () -> { AdminApi.signOut(); loaded = false; };
		}
		if (System.currentTimeMillis() < messageUntil) Ui.centered(g, font, message, px + pw / 2, py + ph - 17, messageColor);
		super.render(g, mx, my, delta);
		Ui.flushLabels();
		if (action != null) { Ui.click(); action.run(); }
	}

	private void signIn() {
		loading = true;
		AdminApi.login(password.getValue(), () -> { loading = false; password.setValue(""); toast("Signed in", Ui.ON); refresh(); }, e -> { loading = false; toast(e, 0xFFFF7A7A); });
	}

	private Runnable codesTab(GuiGraphics g, int mx, int my, boolean click, float delta) {
		Runnable action = null;
		int lx = px + 10, top = py + 38, lw = (int) (pw * 0.45f) - 16;
		g.drawString(font, "Coins", lx, top + 4, Ui.MUTED, false);
		field(g, coinsBox, lx + 60, top, 60, mx, my, delta);
		int bx = lx + 124, bw = (lw - 124) / 3;
		for (int i = 1; i < 4; i++) {
			int v = COIN_PRESETS[i];
			if (Ui.pill(g, font, mx, my, bx + (i - 1) * bw, top, bw - 2, 16, String.valueOf(v), parse(coinsBox, -1) == v) && click) action = () -> coinsBox.setValue(String.valueOf(v));
		}
		g.drawString(font, "Code", lx, top + 24, Ui.MUTED, false);
		field(g, codeBox, lx + 60, top + 20, lw - 60, mx, my, delta);
		g.drawString(font, "How many", lx, top + 44, Ui.MUTED, false);
		field(g, countBox, lx + 60, top + 40, 40, mx, my, delta);
		g.drawString(font, "Uses each", lx + 108, top + 44, Ui.MUTED, false);
		field(g, usesBox, lx + 168, top + 40, lw - 168, mx, my, delta);
		String unlocks = picked.isEmpty() ? "No items, pick on the right" : picked.size() == 1 ? "Unlocks " + itemLabel(picked.iterator().next()) : "Unlocks " + picked.size() + " items";
		g.drawString(font, Ui.fit(font, unlocks, lw - 50), lx, top + 64, picked.isEmpty() ? Ui.FAINT : Ui.TEXT, false);
		if (!picked.isEmpty() && Ui.pill(g, font, mx, my, lx + lw - 44, top + 60, 44, 16, "Clear", false) && click) action = picked::clear;
		if (Ui.pill(g, font, mx, my, lx, top + 80, lw, 18, loading ? "Working…" : "Create", false) && click && !loading) action = () -> {
			JsonObject b = new JsonObject();
			b.addProperty("coins", Math.max(0, parse(coinsBox, 0)));
			b.addProperty("code", codeBox.getValue().trim());
			b.addProperty("count", Math.max(1, parse(countBox, 1)));
			b.addProperty("uses", Math.max(0, parse(usesBox, 0)));
			JsonArray it = new JsonArray();
			for (String p : picked) it.add(p);
			b.add("items", it);
			post("codes", b, null);
		};
		g.drawString(font, "Codes (click to copy)", lx, top + 106, Ui.ACCENT_2, false);
		List<gg.surge.client.economy.AdminCodes.Code> old = oldCodes;
		if (!old.isEmpty() && Ui.pill(g, font, mx, my, lx + lw - 120, top + 102, 120, 16, "Put " + old.size() + " old code" + (old.size() == 1 ? "" : "s") + " online", false) && click && !loading) action = () -> moveOld(old);
		int ly = top + 118, lh = py + ph - 28 - ly;
		listScroll = Math.min(listScroll, Math.max(0, codes.size() * 22 - lh));
		g.enableScissor(lx, ly, lx + lw, ly + lh);
		for (int i = 0; i < codes.size(); i++) {
			JsonObject c = codes.get(i).getAsJsonObject();
			int y = ly + i * 22 - Math.round(listScroll);
			if (y + 22 < ly || y > ly + lh) continue;
			boolean hov = Ui.hover(mx, my, lx, y, lw - 20, 20) && my >= ly && my < ly + lh;
			g.fill(lx, y, lx + lw, y + 20, hov ? 0xFF2E2E33 : 0xFF1C1C20);
			String code = c.get("code").getAsString();
			int coins = c.has("coins") ? c.get("coins").getAsInt() : 0;
			int items = c.has("items") ? c.getAsJsonArray("items").size() : 0;
			int used = c.has("used") ? c.get("used").getAsInt() : 0;
			int max = c.has("max_uses") && !c.get("max_uses").isJsonNull() ? c.get("max_uses").getAsInt() : 0;
			g.drawString(font, code, lx + 4, y + 2, Ui.TEXT, false);
			String sub = (coins > 0 ? coins + " coins" : "") + (items > 0 ? (coins > 0 ? " + " : "") + items + " item" + (items == 1 ? "" : "s") : "") + "  used " + used + (max > 0 ? "/" + max : "");
			g.pose().pushMatrix();
			g.pose().translate(lx + 4, y + 12);
			g.pose().scale(0.75f, 0.75f);
			g.drawString(font, sub, 0, 0, Ui.MUTED, false);
			g.pose().popMatrix();
			if (hov && click) action = () -> { minecraft.keyboardHandler.setClipboard(code); toast("Copied " + code, Ui.ON); };
			if (Ui.iconButton(g, mx, my, lx + lw - 18, y + 2, 16, Ui.CROSS) && click && my >= ly && my < ly + lh) {
				String hash = c.get("hash").getAsString();
				action = () -> { JsonObject b = new JsonObject(); b.addProperty("hash", hash); post("codes/delete", b, "Code deleted"); };
			}
		}
		if (codes.isEmpty()) g.drawString(font, loaded ? "No codes yet" : "Loading…", lx + 4, ly + 4, Ui.FAINT, false);
		g.disableScissor();

		int gx = px + (int) (pw * 0.45f), gw = px + pw - 10 - gx;
		g.drawString(font, "Items the code unlocks", gx, top + 4 - 4, Ui.ACCENT_2, false);
		Runnable ga = grid(g, gx, top + 10, gw, py + ph - 28 - top - 10, mx, my, click, it -> picked.contains(it.id()), it -> locked.contains(it.id()),
			it -> { if (!picked.remove(it.id())) picked.add(it.id()); });
		return ga != null ? ga : action;
	}

	private void moveOld(List<gg.surge.client.economy.AdminCodes.Code> old) {
		int[] left = {old.size()};
		loading = true;
		for (gg.surge.client.economy.AdminCodes.Code c : old) {
			JsonObject b = new JsonObject();
			b.addProperty("code", c.code());
			b.addProperty("coins", c.coins());
			b.addProperty("count", 1);
			b.addProperty("uses", 0);
			JsonArray it = new JsonArray();
			for (String i : c.items()) it.add(i);
			b.add("items", it);
			AdminApi.call("codes", b, o -> {
				gg.surge.client.economy.AdminCodes.remove(c.code());
				if (--left[0] == 0) { loading = false; oldCodes = gg.surge.client.economy.AdminCodes.list(); toast("Old codes are online now and work for everyone", Ui.ON); refresh(); }
			}, e -> {
				if (e.contains("already exists")) gg.surge.client.economy.AdminCodes.remove(c.code());
				if (--left[0] == 0) { loading = false; oldCodes = gg.surge.client.economy.AdminCodes.list(); toast(e.contains("already exists") ? "Old codes are online now" : e, e.contains("already exists") ? Ui.ON : 0xFFFF7A7A); refresh(); }
			});
		}
	}

	private Runnable salesTab(GuiGraphics g, int mx, int my, boolean click, float delta) {
		Runnable action = null;
		int lx = px + 10, top = py + 38, lw = (int) (pw * 0.45f) - 16;
		g.drawString(font, "On", lx, top + 4, Ui.MUTED, false);
		g.drawString(font, Ui.fit(font, itemLabel(saleTarget), lw - 30), lx + 30, top + 4, Ui.TEXT, false);
		int gbw = (lw - 4) / 3;
		for (int i = 0; i < GROUPS.length; i++) {
			String id = GROUPS[i][0];
			if (Ui.pill(g, font, mx, my, lx + (i % 3) * (gbw + 2), top + 18 + (i / 3) * 18, gbw, 16, GROUPS[i][1], saleTarget.equals(id)) && click) action = () -> saleTarget = id;
		}
		g.drawString(font, "Discount", lx, top + 62, Ui.MUTED, false);
		if (Ui.pill(g, font, mx, my, lx + 60, top + 58, 18, 16, "-", false) && click) action = () -> percent = Math.max(5, percent - 5);
		Ui.centered(g, font, percent + "%", lx + 98, top + 62, Ui.TEXT);
		if (Ui.pill(g, font, mx, my, lx + 118, top + 58, 18, 16, "+", false) && click) action = () -> percent = Math.min(100, percent + 5);
		g.drawString(font, "For", lx + 144, top + 62, Ui.MUTED, false);
		field(g, saleWho, lx + 164, top + 58, lw - 164, mx, my, delta);
		g.drawString(font, "Length", lx, top + 82, Ui.MUTED, false);
		int dbw = (lw - 50) / 4;
		for (int i = 0; i < DURATION_NAMES.length; i++) {
			int idx = i;
			if (Ui.pill(g, font, mx, my, lx + 50 + i * dbw, top + 78, dbw - 2, 16, DURATION_NAMES[i], i == durationIdx) && click) action = () -> durationIdx = idx;
		}
		if (Ui.pill(g, font, mx, my, lx, top + 100, lw, 18, "Start sale", false) && click && !loading) action = () -> {
			JsonObject b = new JsonObject();
			b.addProperty("target", saleTarget);
			b.addProperty("percent", percent);
			b.addProperty("hours", DURATIONS_H[durationIdx]);
			b.addProperty("player", saleWho.getValue().trim());
			post("sales", b, saleWho.getValue().isBlank() ? "Sale started" : "Sale started for " + saleWho.getValue().trim());
		};
		g.drawString(font, "Running", lx, top + 126, Ui.ACCENT_2, false);
		int ly = top + 138;
		for (int i = 0; i < sales.size(); i++) {
			JsonObject s = sales.get(i).getAsJsonObject();
			int y = ly + i * 20;
			if (y > py + ph - 44) break;
			g.fill(lx, y, lx + lw, y + 18, 0xFF1C1C20);
			long until = s.has("until") ? s.get("until").getAsLong() : 0;
			String left = until == 0 ? "no end" : Math.max(0, (until - System.currentTimeMillis()) / 3600000) + "h left";
			String who = s.has("name") && !s.get("name").isJsonNull() && !s.get("name").getAsString().isEmpty() ? " for " + s.get("name").getAsString() : "";
			g.drawString(font, Ui.fit(font, s.get("percent").getAsInt() + "% off " + itemLabel(s.get("target").getAsString()) + who + "  " + left, lw - 26), lx + 4, y + 5, Ui.TEXT, false);
			if (Ui.iconButton(g, mx, my, lx + lw - 17, y + 1, 16, Ui.CROSS) && click) {
				int id = s.get("id").getAsInt();
				action = () -> { JsonObject b = new JsonObject(); b.addProperty("id", id); post("sales/delete", b, "Sale ended"); };
			}
		}
		if (sales.isEmpty()) g.drawString(font, "No sales right now", lx + 4, ly + 4, Ui.FAINT, false);
		int gx = px + (int) (pw * 0.45f), gw = px + pw - 10 - gx;
		g.drawString(font, "Or pick one item", gx, top, Ui.ACCENT_2, false);
		Runnable ga = grid(g, gx, top + 10, gw, py + ph - 28 - top - 10, mx, my, click, it -> it.id().equals(saleTarget), it -> locked.contains(it.id()), it -> saleTarget = it.id());
		return ga != null ? ga : action;
	}

	private Runnable locksTab(GuiGraphics g, int mx, int my, boolean click) {
		int top = py + 38;
		g.drawString(font, "Locked items can't be bought, only unlocked with a code. Click to lock or unlock.", px + 10, top, Ui.MUTED, false);
		return grid(g, px + 10, top + 12, pw - 20, py + ph - 28 - top - 12, mx, my, click, it -> false, it -> locked.contains(it.id()), it -> {
			JsonObject b = new JsonObject();
			b.addProperty("item", it.id());
			post("locks/toggle", b, (locked.contains(it.id()) ? "Unlocked " : "Locked ") + it.name());
		});
	}

	private Runnable serversTab(GuiGraphics g, int mx, int my, boolean click, float delta) {
		Runnable action = null;
		int lx = px + 10, top = py + 38, w = pw - 20;
		g.drawString(font, "Shown in the Discover tab of the server list", lx, top, Ui.MUTED, false);
		field(g, srvName, lx, top + 14, 110, mx, my, delta);
		field(g, srvIp, lx + 114, top + 14, 140, mx, my, delta);
		field(g, srvDesc, lx + 258, top + 14, w - 258 - 54, mx, my, delta);
		if (Ui.pill(g, font, mx, my, lx + w - 50, top + 14, 50, 16, "Add", false) && click) action = () -> {
			JsonObject b = new JsonObject();
			b.addProperty("name", srvName.getValue().trim());
			b.addProperty("ip", srvIp.getValue().trim());
			b.addProperty("description", srvDesc.getValue().trim());
			post("servers", b, "Server added");
			srvName.setValue(""); srvIp.setValue(""); srvDesc.setValue("");
		};
		int ly = top + 40;
		for (int i = 0; i < servers.size(); i++) {
			JsonObject s = servers.get(i).getAsJsonObject();
			int y = ly + i * 22;
			if (y > py + ph - 48) break;
			g.fill(lx, y, lx + w, y + 20, 0xFF1C1C20);
			g.drawString(font, s.get("name").getAsString(), lx + 4, y + 2, Ui.TEXT, false);
			g.pose().pushMatrix();
			g.pose().translate(lx + 4, y + 12);
			g.pose().scale(0.75f, 0.75f);
			g.drawString(font, s.get("ip").getAsString() + (s.has("description") && !s.get("description").isJsonNull() ? "  " + s.get("description").getAsString() : ""), 0, 0, Ui.MUTED, false);
			g.pose().popMatrix();
			if (Ui.iconButton(g, mx, my, lx + w - 18, y + 2, 16, Ui.CROSS) && click) {
				int id = s.get("id").getAsInt();
				action = () -> { JsonObject b = new JsonObject(); b.addProperty("id", id); post("servers/delete", b, "Server removed"); };
			}
		}
		if (servers.isEmpty()) g.drawString(font, "No featured servers", lx + 4, ly + 4, Ui.FAINT, false);
		return action;
	}


	private static String str(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
	}

	private Runnable giftsTab(GuiGraphics g, int mx, int my, boolean click, float delta) {
		Runnable action = null;
		int lx = px + 10, top = py + 38, lw = (int) (pw * 0.45f) - 16;
		g.drawString(font, "Player", lx, top + 4, Ui.MUTED, false);
		field(g, giftWho, lx + 50, top, lw - 50, mx, my, delta);
		g.drawString(font, "Coins", lx, top + 24, Ui.MUTED, false);
		field(g, giftCoins, lx + 50, top + 20, 56, mx, my, delta);
		int bx = lx + 110, bw = (lw - 110) / 3;
		for (int i = 1; i < 4; i++) {
			int v = COIN_PRESETS[i];
			if (Ui.pill(g, font, mx, my, bx + (i - 1) * bw, top + 20, bw - 2, 16, String.valueOf(v), parse(giftCoins, -1) == v) && click) action = () -> giftCoins.setValue(String.valueOf(v));
		}
		field(g, giftNote, lx, top + 40, lw, mx, my, delta);
		String what = giftPicked.isEmpty() ? "No items, pick on the right" : giftPicked.size() == 1 ? "Gives " + itemLabel(giftPicked.iterator().next()) : "Gives " + giftPicked.size() + " items";
		g.drawString(font, Ui.fit(font, what, lw - 50), lx, top + 64, giftPicked.isEmpty() ? Ui.FAINT : Ui.TEXT, false);
		if (!giftPicked.isEmpty() && Ui.pill(g, font, mx, my, lx + lw - 44, top + 60, 44, 16, "Clear", false) && click) action = giftPicked::clear;
		if (Ui.pill(g, font, mx, my, lx, top + 80, lw, 18, loading ? "Working…" : "Send gift", false) && click && !loading) action = () -> {
			JsonObject b = new JsonObject();
			b.addProperty("player", giftWho.getValue().trim());
			b.addProperty("coins", Math.max(0, parse(giftCoins, 0)));
			b.addProperty("note", giftNote.getValue().trim());
			JsonArray it = new JsonArray();
			for (String p : giftPicked) it.add(p);
			b.add("items", it);
			post("gifts", b, "Gift sent to " + giftWho.getValue().trim());
			giftPicked.clear();
		};
		g.drawString(font, "Recent gifts", lx, top + 106, Ui.ACCENT_2, false);
		int ly = top + 118, lh = py + ph - 28 - ly;
		listScroll = Math.min(listScroll, Math.max(0, gifts.size() * 22 - lh));
		g.enableScissor(lx, ly, lx + lw, ly + lh);
		for (int i = 0; i < gifts.size(); i++) {
			JsonObject c = gifts.get(i).getAsJsonObject();
			int y = ly + i * 22 - Math.round(listScroll);
			if (y + 22 < ly || y > ly + lh) continue;
			g.fill(lx, y, lx + lw, y + 20, 0xFF1C1C20);
			boolean got = c.has("claimed") && c.get("claimed").getAsLong() > 0;
			g.drawString(font, Ui.fit(font, str(c, "name"), lw - 80), lx + 4, y + 2, Ui.TEXT, false);
			g.drawString(font, got ? "Received" : "Waiting", lx + lw - 62 - (got ? 0 : 0), y + 2, got ? 0xFF8CFFA0 : Ui.YELLOW, false);
			List<String> parts = new ArrayList<>();
			int coins = c.has("coins") ? c.get("coins").getAsInt() : 0;
			if (coins > 0) parts.add(coins + " coins");
			if (c.has("items")) for (JsonElement e : c.getAsJsonArray("items")) parts.add(itemLabel(e.getAsString()));
			g.pose().pushMatrix();
			g.pose().translate(lx + 4, y + 12);
			g.pose().scale(0.75f, 0.75f);
			g.drawString(font, Ui.fit(font, String.join(", ", parts), (int) ((lw - 30) / 0.75f)), 0, 0, Ui.MUTED, false);
			g.pose().popMatrix();
			if (!got && Ui.iconButton(g, mx, my, lx + lw - 18, y + 2, 16, Ui.CROSS) && click && my >= ly && my < ly + lh) {
				int id = c.get("id").getAsInt();
				action = () -> { JsonObject b = new JsonObject(); b.addProperty("id", id); post("gifts/delete", b, "Gift cancelled"); };
			}
		}
		if (gifts.isEmpty()) g.drawString(font, loaded ? "No gifts yet" : "Loading…", lx + 4, ly + 4, Ui.FAINT, false);
		g.disableScissor();
		int gx = px + (int) (pw * 0.45f), gw = px + pw - 10 - gx;
		g.drawString(font, "Items to give", gx, top, Ui.ACCENT_2, false);
		Runnable ga = grid(g, gx, top + 10, gw, py + ph - 28 - top - 10, mx, my, click, it -> giftPicked.contains(it.id()), it -> false,
			it -> { if (!giftPicked.remove(it.id())) giftPicked.add(it.id()); });
		return ga != null ? ga : action;
	}

	private int currentPrice(Shop.Item it) {
		return prices.has(it.id()) ? prices.get(it.id()).getAsInt() : it.price();
	}

	private Runnable pricesTab(GuiGraphics g, int mx, int my, boolean click, float delta) {
		Runnable action = null;
		int lx = px + 10, top = py + 38, lw = (int) (pw * 0.45f) - 16;
		Shop.Item it = Shop.byId(priceItem);
		if (it == null) it = Shop.ITEMS.get(0);
		Shop.Item item = it;
		g.drawString(font, Ui.fit(font, item.name(), lw), lx, top, Ui.TEXT, false);
		g.drawString(font, "Normal price " + item.price() + "   Now " + currentPrice(item), lx, top + 12, Ui.MUTED, false);
		g.drawString(font, "New price", lx, top + 32, Ui.MUTED, false);
		field(g, priceBox, lx + 60, top + 28, 70, mx, my, delta);
		if (Ui.pill(g, font, mx, my, lx + 134, top + 28, 46, 16, "Save", false) && click && !loading) action = () -> {
			JsonObject b = new JsonObject();
			b.addProperty("item", item.id());
			b.addProperty("price", Math.max(0, parse(priceBox, item.price())));
			post("prices", b, "New price for " + item.name());
		};
		if (Ui.pill(g, font, mx, my, lx + 184, top + 28, lw - 184, 16, "Normal", false) && click && !loading) action = () -> {
			JsonObject b = new JsonObject();
			b.addProperty("item", item.id());
			b.add("price", com.google.gson.JsonNull.INSTANCE);
			post("prices", b, item.name() + " is back to normal");
		};
		g.drawString(font, "All prices", lx, top + 60, Ui.ACCENT_2, false);
		field(g, pricePct, lx, top + 72, 40, mx, my, delta);
		g.drawString(font, "% of normal", lx + 44, top + 76, Ui.MUTED, false);
		if (Ui.pill(g, font, mx, my, lx + 110, top + 72, lw - 110, 16, "Apply to all", false) && click && !loading) action = () -> {
			JsonObject b = new JsonObject();
			b.addProperty("percent", Math.max(1, parse(pricePct, 100)));
			JsonObject base = new JsonObject();
			for (Shop.Item i : Shop.ITEMS) base.addProperty(i.id(), i.price());
			b.add("base", base);
			post("prices/scale", b, "All prices updated");
		};
		g.drawString(font, "Changed prices", lx, top + 100, Ui.ACCENT_2, false);
		int ly = top + 112, n = 0;
		for (var en : prices.entrySet()) {
			int y = ly + n * 12;
			if (y > py + ph - 40) break;
			g.drawString(font, Ui.fit(font, itemLabel(en.getKey()) + ": " + en.getValue().getAsInt(), lw), lx + 4, y, Ui.TEXT, false);
			n++;
		}
		if (prices.size() == 0) g.drawString(font, "Every price is normal", lx + 4, ly, Ui.FAINT, false);
		int gx = px + (int) (pw * 0.45f), gw = px + pw - 10 - gx;
		g.drawString(font, "Pick an item", gx, top, Ui.ACCENT_2, false);
		Runnable ga = grid(g, gx, top + 10, gw, py + ph - 28 - top - 10, mx, my, click, x -> x.id().equals(priceItem), x -> false,
			x -> { priceItem = x.id(); priceBox.setValue(String.valueOf(currentPrice(x))); });
		return ga != null ? ga : action;
	}

	private static final String[] BADGE_KINDS = {"owner", "staff", "booster", "beta", "media", "vip", "early", "dev"};
	private static final String[] BADGE_NAMES = {"Owner", "Staff", "Booster", "Beta", "Creator", "VIP", "Early", "Dev"};

	private Runnable playersTab(GuiGraphics g, int mx, int my, boolean click, float delta) {
		Runnable action = null;
		int lx = px + 10, top = py + 38, half = (pw - 30) / 2;
		g.drawString(font, "Name icon", lx, top, Ui.ACCENT_2, false);
		field(g, badgeWho, lx, top + 12, half, mx, my, delta);
		int bw = (half - 6) / 4;
		for (int i = 0; i < BADGE_KINDS.length; i++) {
			String k = BADGE_KINDS[i];
			int bx = lx + (i % 4) * (bw + 2), by = top + 32 + (i / 4) * 18;
			if (Ui.pill(g, font, mx, my, bx, by, bw, 16, "", badgeKind.equals(k)) && click) action = () -> badgeKind = k;
			Component glyph = gg.surge.client.features.Flags.icon(gg.surge.client.Badges.glyph(k));
			int tw = font.width(glyph) + 3 + font.width(BADGE_NAMES[i]);
			g.drawString(font, glyph, bx + (bw - tw) / 2, by + 4, 0xFFFFFFFF, false);
			g.drawString(font, BADGE_NAMES[i], bx + (bw - tw) / 2 + font.width(glyph) + 3, by + 4, Ui.TEXT, false);
		}
		if (Ui.pill(g, font, mx, my, lx, top + 72, half, 18, "Give icon", false) && click && !loading) action = () -> {
			JsonObject b = new JsonObject();
			b.addProperty("player", badgeWho.getValue().trim());
			b.addProperty("badge", badgeKind);
			post("badges", b, badgeWho.getValue().trim() + " has a new icon");
		};
		int ly = top + 96;
		for (int i = 0; i < badges.size(); i++) {
			JsonObject o = badges.get(i).getAsJsonObject();
			int y = ly + i * 18;
			if (y > py + ph - 44) break;
			g.fill(lx, y, lx + half, y + 16, 0xFF1C1C20);
			Component glyph = gg.surge.client.features.Flags.icon(gg.surge.client.Badges.glyph(str(o, "badge")));
			g.drawString(font, glyph, lx + 4, y + 4, 0xFFFFFFFF, false);
			g.drawString(font, Ui.fit(font, str(o, "name"), half - 40), lx + 18, y + 4, Ui.TEXT, false);
			if (Ui.iconButton(g, mx, my, lx + half - 16, y, 16, Ui.CROSS) && click) {
				String uuid = str(o, "uuid");
				action = () -> { JsonObject b = new JsonObject(); b.addProperty("player", uuid); b.addProperty("badge", ""); post("badges", b, "Icon removed"); };
			}
		}
		if (badges.isEmpty()) g.drawString(font, "Nobody has a special icon yet", lx + 4, ly + 4, Ui.FAINT, false);

		int rx = lx + half + 10;
		g.drawString(font, "Picture cape", rx, top, Ui.ACCENT_2, false);
		field(g, capeWho, rx, top + 12, half, mx, my, delta);
		if (Ui.pill(g, font, mx, my, rx, top + 32, half, 16, capeFile.isEmpty() ? "Pick a picture…" : Ui.fit(font, capeFile, half - 10), false) && click && !loading) action = this::pickPicture;
		g.drawString(font, Ui.fit(font, "Any picture works. It goes on the back of the cape.", half), rx, top + 54, Ui.FAINT, false);
		if (Ui.pill(g, font, mx, my, rx, top + 72, half, 18, "Upload and gift", false) && click && !loading) action = () -> {
			if (capePng == null) { toast("Pick a picture first", 0xFFFF7A7A); return; }
			JsonObject b = new JsonObject();
			b.addProperty("player", capeWho.getValue().trim());
			b.addProperty("png", java.util.Base64.getEncoder().encodeToString(capePng));
			post("capes", b, "Picture cape sent to " + capeWho.getValue().trim());
		};
		for (int i = 0; i < capes.size(); i++) {
			JsonObject o = capes.get(i).getAsJsonObject();
			int y = ly + i * 18;
			if (y > py + ph - 44) break;
			g.fill(rx, y, rx + half, y + 16, 0xFF1C1C20);
			g.drawString(font, Ui.fit(font, str(o, "name"), half - 24), rx + 4, y + 4, Ui.TEXT, false);
			if (Ui.iconButton(g, mx, my, rx + half - 16, y, 16, Ui.CROSS) && click) {
				String uuid = str(o, "uuid");
				action = () -> { JsonObject b = new JsonObject(); b.addProperty("uuid", uuid); post("capes/delete", b, "Picture cape removed"); };
			}
		}
		if (capes.isEmpty()) g.drawString(font, "No picture capes yet", rx + 4, ly + 4, Ui.FAINT, false);
		return action;
	}

	private void pickPicture() {
		java.util.concurrent.CompletableFuture.runAsync(() -> {
			String path;
			try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
				org.lwjgl.PointerBuffer filters = stack.mallocPointer(4);
				filters.put(stack.UTF8("*.png")).put(stack.UTF8("*.jpg")).put(stack.UTF8("*.jpeg")).put(stack.UTF8("*.gif"));
				filters.flip();
				path = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Pick a picture", null, filters, "Pictures", false);
			} catch (Throwable t) {
				path = null;
			}
			if (path == null) return;
			try {
				byte[] png = toPng(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path)));
				String name = java.nio.file.Path.of(path).getFileName().toString();
				minecraft.execute(() -> { capePng = png; capeFile = name; });
			} catch (Exception e) {
				minecraft.execute(() -> toast("That picture couldn't be read", 0xFFFF7A7A));
			}
		});
	}

	private static byte[] toPng(byte[] raw) throws Exception {
		java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(raw));
		if (img == null) throw new Exception("not a picture");
		int w = img.getWidth(), h = img.getHeight();
		int max = w == h * 2 ? 1024 : 512;
		float k = Math.min(1f, max / (float) Math.max(w, h));
		int nw = Math.max(1, Math.round(w * k)), nh = Math.max(1, Math.round(h * k));
		java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(nw, nh, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g2 = out.createGraphics();
		g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, k < 1 ? java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC : java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g2.drawImage(img, 0, 0, nw, nh, null);
		g2.dispose();
		java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
		javax.imageio.ImageIO.write(out, "png", bo);
		return bo.toByteArray();
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent e) {
		if (!AdminApi.signedIn() && (e.key() == GLFW.GLFW_KEY_ENTER || e.key() == GLFW.GLFW_KEY_KP_ENTER) && !loading) { signIn(); return true; }
		return super.keyPressed(e);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
