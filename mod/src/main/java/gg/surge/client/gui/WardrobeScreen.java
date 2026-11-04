package gg.surge.client.gui;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.cosmetics.CapturedLook;
import gg.surge.client.cosmetics.Cosmetics;
import gg.surge.client.cosmetics.Preview;
import gg.surge.client.economy.Locks;
import gg.surge.client.economy.Sales;
import gg.surge.client.economy.Shop;
import gg.surge.client.economy.SkinUploader;
import gg.surge.client.economy.Wallet;
import gg.surge.client.emotes.Emotes;
import gg.surge.client.mods.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class WardrobeScreen extends Screen {
	public enum Section {
		FEATURED("Featured", Items.NETHER_STAR), COSMETICS("Cosmetics", Items.ELYTRA), EMOTES("Emotes", Items.ARMOR_STAND), OUTFITS("Outfits", Items.LEATHER_CHESTPLATE),
		SKINS("Skins", Items.PLAYER_HEAD), CURRENCY("Coins", Items.GOLD_NUGGET);
		final String label;
		final Item icon;

		Section(String label, Item icon) {
			this.label = label;
			this.icon = icon;
		}
	}

	private static final String[] COSMETIC_SUBS = {"All", "Capes", "Wings", "Halos"};
	private static final String[] EMOTE_SUBS = {"All", "Dances", "With music", "Basic", "Expressive"};
	private static final String[] SORTS = {"Featured", "Price", "Owned first", "Name"};
	private static final Set<String> DANCES = Set.of("FLOSS", "GROOVE", "SALSA", "ROBOT", "CHICKEN", "HEADBANG", "GUITAR", "DRUMS", "TWIRL", "SWIM", "ZOMBIE", "BOXING");
	private static final Set<String> EXPRESSIVE = Set.of("FLUSTERED", "FACEPALM", "SHRUG", "THINKING", "LAUGH", "CRY", "HEART", "SHY", "YAWN", "BOW", "TPOSE");
	private static final int[] WHEEL_COL = {1, 2, 2, 2, 1, 0, 0, 0};
	private static final int[] WHEEL_ROW = {0, 0, 1, 2, 2, 2, 1, 0};

	private static Section section = Section.FEATURED;
	private static String cosmeticSub = "All", emoteSub = "All";
	private static int sortIdx;
	private static int outfitIdx, wheelPage;
	private static boolean slim;

	private final Screen parent;
	private EditBox codeBox, searchBox;
	private boolean searching;
	private String pendingBuy;
	private long pendingUntil;
	private String message = "";
	private int messageColor = Ui.ACCENT_2;
	private long messageAt;
	private boolean wasL = true, wasR = true, redeeming;
	private int m, top, stageX, stageW, gx, gw, gridTop, gridH, mx0, mw, wy, wh;
	private float scroll, scrollTarget;
	private int contentH;
	private long openedAt;
	private Shop.Item hoveredItem;
	private Emotes.Emote selectedEmote;
	private Shop.Item selected;
	private String lastClicked;
	private long lastClickAt;
	private List<Path> skins = new ArrayList<>();
	private int menuX, menuY;
	private List<String> menuLabels;
	private List<Runnable> menuActions;

	public WardrobeScreen(Screen parent) {
		super(Component.literal("Wardrobe"));
		this.parent = parent;
	}

	public static void autotestSub(String sub) {
		cosmeticSub = sub;
	}

	public static WardrobeScreen open(Screen parent, Section s) {
		section = s;
		return new WardrobeScreen(parent);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		float p = openProgress();
		g.fill(0, 0, this.width, this.height, Colors.alpha(0xE8141416, p));
		g.fillGradient(0, this.height / 2, this.width, this.height, 0x00000000, Colors.alpha(0x60000000, p));
	}

	private float openProgress() {
		float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f);
		return 1f - (1f - t) * (1f - t) * (1f - t);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	@Override
	protected void init() {
		if (openedAt == 0) openedAt = System.currentTimeMillis();
		m = this.width < 480 ? 6 : 12;
		top = m + 24;
		stageX = m;
		stageW = Math.max(140, Math.min(280, Math.round(this.width * 0.36f)));
		gx = stageX + stageW + 10;
		gw = this.width - m - gx;
		mx0 = gx;
		mw = gw;
		wy = top;
		wh = this.height - m - top;
		gridTop = top + 22;
		gridH = this.height - m - gridTop;
		clearWidgets();
		codeBox = null;
		searchBox = new EditBox(font, 0, 0, 80, 10, Component.literal("Search"));
		searchBox.setBordered(false);
		searchBox.setMaxLength(32);
		searchBox.setHint(Component.literal("Search").withStyle(ChatFormatting.DARK_GRAY));
		searchBox.setResponder(v -> scroll = scrollTarget = 0);
		addWidget(searchBox);
		if (section == Section.CURRENCY) {
			codeBox = new EditBox(font, 0, 0, 100, 12, Component.literal("Code"));
			codeBox.setBordered(false);
			codeBox.setMaxLength(40);
			codeBox.setHint(Component.literal("Type or paste a code").withStyle(ChatFormatting.DARK_GRAY));
			addWidget(codeBox);
			setFocused(codeBox);
			codeBox.setFocused(true);
		}
		if (section == Section.SKINS) skins = SkinUploader.list();
	}

	private void toast(String s, int color) {
		message = s;
		messageColor = color;
		messageAt = System.currentTimeMillis();
	}

	private void switchTo(Section s) {
		section = s;
		scroll = scrollTarget = 0;
		pendingBuy = null;
		menuLabels = null;
		init();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0) {
			scrollTarget = Math.max(0, Math.min(Math.max(0, contentH - gridH), scrollTarget - (float) scrollY * 30f));
			return true;
		}
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		if (menuLabels != null) { menuLabels = null; return false; }
		return true;
	}

	private static String equippedKey(String type) {
		SurgeConfig c = SurgeConfig.get();
		return switch (type) { case "wings" -> c.wings; case "halo" -> c.halo; default -> c.cape; };
	}

	private static void setEquipped(String type, String key) {
		SurgeConfig c = SurgeConfig.get();
		switch (type) {
			case "wings" -> c.wings = key;
			case "halo" -> c.halo = key;
			default -> c.cape = key;
		}
		SurgeConfig.save();
	}

	private static String typeOf(Shop.Item item) {
		return item.id().substring(0, item.id().indexOf(':'));
	}

	private void buy(Shop.Item item, boolean confirmNow) {
		Wallet w = Wallet.get();
		int price = Shop.price(item);
		if (Locks.locked(item)) { toast(item.id().equals("cape:beta") ? "This cape is only for beta testers" : "This can't be bought right now", Ui.MUTED); return; }
		if (w.coins < price) { toast("You need " + (price - w.coins) + " more coins", 0xFFFF8C8C); return; }
		if (confirmNow || (item.id().equals(pendingBuy) && System.currentTimeMillis() < pendingUntil)) {
			w.buy(item);
			pendingBuy = null;
			toast("Unlocked " + item.name(), 0xFF8CFFA0);
			if (item.category().equals("cosmetic")) setEquipped(typeOf(item), item.key());
		} else {
			pendingBuy = item.id();
			pendingUntil = System.currentTimeMillis() + 3000;
			toast("Click again to buy " + item.name() + " for " + price + " coins", Ui.YELLOW);
		}
	}

	private void redeem() {
		if (codeBox == null || codeBox.getValue().isBlank() || redeeming) return;
		redeeming = true;
		toast("Checking…", Ui.MUTED);
		Wallet.get().redeem(codeBox.getValue()).thenAccept(r -> minecraft.execute(() -> {
			redeeming = false;
			if (r.ok()) {
				List<String> parts = new ArrayList<>();
				if (r.coins() > 0) parts.add("+" + r.coins() + " coins");
				for (String id : r.items()) { Shop.Item i = Shop.byId(id); parts.add(i != null ? i.name() : id); }
				toast("Redeemed: " + String.join(", ", parts), 0xFF8CFFA0);
				codeBox.setValue("");
			} else {
				toast(switch (r.error()) {
					case 2 -> "You already redeemed this code";
					case 3 -> "You already have everything this code gives";
					default -> "That code doesn't exist. Check the spelling, or ask for a new one.";
				}, 0xFFFF8C8C);
			}
		}));
	}

	private static String emoteGroup(Emotes.Emote e) {
		if (e.music != null) return "With music";
		if (DANCES.contains(e.name())) return "Dances";
		if (EXPRESSIVE.contains(e.name())) return "Expressive";
		return "Basic";
	}

	private List<Shop.Item> featured() {
		List<Shop.Item> out = new ArrayList<>();
		String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase();
		for (Shop.Item i : Shop.ITEMS) if (Sales.discount(i) > 0 && !Locks.locked(i)) out.add(i);
		for (Emotes.Emote e : Emotes.Emote.values()) {
			Shop.Item i = Shop.byId("emote:" + e.name());
			if (e.music != null && i != null && !out.contains(i)) out.add(i);
		}
		Shop.Item beta = Shop.byId("cape:beta");
		if (beta != null && Wallet.get().owns(beta.id()) && !out.contains(beta)) out.add(0, beta);
		for (Shop.Item i : Shop.bySub("cosmetic", "cape")) if (i.key().startsWith("classic_") && !out.contains(i) && out.size() < 24) out.add(i);
		if (!q.isEmpty()) out.removeIf(i -> !i.name().toLowerCase().contains(q));
		return out;
	}

	private List<Shop.Item> items() {
		if (section == Section.FEATURED) return featured();
		List<Shop.Item> out = new ArrayList<>();
		String q = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase();
		boolean emotes = section == Section.EMOTES;
		for (Shop.Item i : Shop.bySub(emotes ? "emote" : "cosmetic", null)) {
			if (!q.isEmpty() && !i.name().toLowerCase().contains(q)) continue;
			if (emotes) {
				Emotes.Emote e = Emotes.Emote.byName(i.key());
				if (e == null) continue;
				if (!emoteSub.equals("All")) {
					String grp = emoteGroup(e);
					if (emoteSub.equals("Dances") ? !(grp.equals("Dances") || grp.equals("With music")) : !grp.equals(emoteSub)) continue;
				}
			} else {
				String t = typeOf(i);
				if (cosmeticSub.equals("Capes") && !t.equals("cape")) continue;
				if (cosmeticSub.equals("Wings") && !t.equals("wings")) continue;
				if (cosmeticSub.equals("Halos") && !t.equals("halo")) continue;
				if (Locks.locked(i) && !Wallet.get().owns(i.id()) && !i.id().equals("cape:beta")) continue;
			}
			out.add(i);
		}
		Wallet w = Wallet.get();
		switch (SORTS[sortIdx]) {
			case "Price" -> out.sort(Comparator.comparingInt(Shop::price));
			case "Owned first" -> out.sort(Comparator.comparing((Shop.Item i) -> !w.owns(i.id())));
			case "Name" -> out.sort(Comparator.comparing(Shop.Item::name));
			default -> { }
		}
		return out;
	}

	private static int rarity(Shop.Item item) {
		int p = item.price();
		if (p == 0) return 0xFFE11D48;
		if (p < 700) return 0xFF8A8A92;
		if (p < 950) return 0xFF3B82F6;
		if (p < 1150) return 0xFFA855F7;
		return 0xFFF59E0B;
	}

	private CapturedLook lookFor(Shop.Item item, int index) {
		if (item.category().equals("emote")) {
			Emotes.Emote e = Emotes.Emote.byName(item.key());
			float t = (System.currentTimeMillis() - openedAt) / 1000f + index * 0.41f;
			if (e != null && !e.loops) t = t % (e.seconds + 0.6f);
			return new CapturedLook(null, null, null, e, t, true);
		}
		String t = typeOf(item);
		return new CapturedLook(t.equals("cape") ? item.key() : null, t.equals("wings") ? item.key() : null, t.equals("halo") ? item.key() : null, null, 0f, false);
	}

	private static float cardTurn(Shop.Item item) {
		if (item.category().equals("emote")) return 18f;
		return switch (typeOf(item)) { case "cape" -> 200f; case "wings" -> 180f; case "halo" -> 20f; default -> 0f; };
	}

	private void openMenu(int x, int y, List<String> labels, List<Runnable> actions) {
		int w = 0;
		for (String l : labels) w = Math.max(w, font.width(l));
		menuX = Math.min(x, this.width - w - 24);
		menuY = Math.min(y, this.height - labels.size() * 16 - 8);
		menuLabels = labels;
		menuActions = actions;
	}

	private void itemMenu(int x, int y, Shop.Item item) {
		Wallet w = Wallet.get();
		boolean owned = w.owns(item.id());
		List<String> l = new ArrayList<>();
		List<Runnable> a = new ArrayList<>();
		if (!owned && !Locks.locked(item)) { l.add("Purchase (" + Shop.price(item) + " coins)"); a.add(() -> buy(item, true)); }
		if (item.category().equals("emote")) {
			Emotes.Emote e = Emotes.Emote.byName(item.key());
			if (owned && e != null) {
				l.add("Play"); a.add(() -> { if (minecraft.player == null) { toast("Join a world to play emotes", Ui.MUTED); return; } minecraft.setScreen(null); Emotes.play(minecraft, e); });
				l.add("Put on the wheel"); a.add(() -> { selectedEmote = e; toast("Now click a slot on the wheel", Ui.ACCENT_2); });
			}
		} else if (owned) {
			boolean eq = item.key().equals(equippedKey(typeOf(item)));
			l.add(eq ? "Take off" : "Equip"); a.add(() -> setEquipped(typeOf(item), eq ? "none" : item.key()));
			if (item.id().equals("cape:beta")) { l.add("Customize"); a.add(() -> minecraft.setScreen(new BetaCapeScreen(this))); }
		}
		if (l.isEmpty()) { l.add(item.id().equals("cape:beta") ? "Only for beta testers" : "Not available"); a.add(() -> { }); }
		openMenu(x, y, l, a);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		Ui.frame();
		long win = minecraft.getWindow().handle();
		boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		boolean click = l && !wasL, rclick = r && !wasR;
		wasL = l;
		wasR = r;
		Runnable action = null;
		hoveredItem = null;

		if (menuLabels != null && (click || rclick)) {
			int idx = -1, w = menuWidth();
			for (int i = 0; i < menuLabels.size(); i++) if (Ui.hover(mx, my, menuX, menuY + 3 + i * 16, w, 16)) idx = i;
			Runnable run = idx >= 0 ? menuActions.get(idx) : null;
			menuLabels = null;
			if (run != null) { Ui.click(); run.run(); }
			click = rclick = false;
		}

		action = topBar(g, mx, my, click, action);
		scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		switch (section) {
			case FEATURED, COSMETICS, EMOTES -> action = grid(g, mx, my, click, rclick, delta, action);
			case OUTFITS -> action = outfits(g, mx, my, click, rclick, action);
			case SKINS -> action = skinsPage(g, mx, my, click, action);
			case CURRENCY -> action = coinsPage(g, mx, my, click, delta, action);
		}
		action = section == Section.EMOTES ? wheelStage(g, mx, my, click, rclick, action) : stage(g, mx, my, click, action);

		long age = System.currentTimeMillis() - messageAt;
		if (!message.isEmpty() && age < 4000 && section != Section.CURRENCY) {
			float a = Math.min(1f, Math.min(age / 150f, (4000 - age) / 400f));
			String msg = Ui.fit(font, message, gw - 20);
			int w = font.width(msg) + 16;
			int tx = gx + (gw - w) / 2, ty = this.height - m - 22 + Math.round((1 - a) * 6);
			g.nextStratum();
			Ui.cutBox(g, tx, ty, w, 17, 3, Colors.alpha(0xF5101012, a), Colors.alpha(messageColor, a * 0.8f));
			g.drawString(font, msg, tx + 8, ty + 5, Colors.alpha(messageColor, a), false);
		}

		if (menuLabels != null) {
			g.nextStratum();
			int w = menuWidth(), h = menuLabels.size() * 16 + 6;
			Ui.cutBox(g, menuX, menuY, w, h, 3, 0xFA1B1B1E, 0xFF45454C);
			for (int i = 0; i < menuLabels.size(); i++) {
				int y = menuY + 3 + i * 16;
				boolean hov = Ui.hover(mx, my, menuX, y, w, 16);
				if (hov) { g.fill(menuX + 2, y, menuX + w - 2, y + 16, 0xFF2E2E33); g.fill(menuX + 2, y, menuX + 3, y + 16, Ui.ACCENT_2); }
				g.drawString(font, menuLabels.get(i), menuX + 8, y + 4, Ui.TEXT, false);
			}
		}
		if (action != null) { Ui.click(); action.run(); }
	}

	private int menuWidth() {
		int w = 60;
		for (String s : menuLabels) w = Math.max(w, font.width(s) + 16);
		return w;
	}

	private static final String[] LEFT = {"....X...", "...XX...", "..XXX...", ".XXXX...", "..XXX...", "...XX...", "....X...", "........"};
	private static final String[] RIGHT = {"...X....", "...XX...", "...XXX..", "...XXXX.", "...XXX..", "...XX...", "...X....", "........"};
	private static final String[] NOTE = {"...XXXX.", "...X..X.", "...X..X.", "...X..X.", ".XXX.XX.", "XXXXXXX.", ".XX.XX..", "........"};
	private static final String[] CHECK = {"........", ".......X", "......XX", "X....XX.", "XX..XX..", ".XXXX...", "..XX....", "........"};
	private static final String[] LOCK = {"..XXXX..", ".X....X.", ".X....X.", "XXXXXXXX", "XXX..XXX", "XXX..XXX", "XXXXXXXX", "........"};

	private Runnable topBar(GuiGraphics g, int mx, int my, boolean click, Runnable action) {
		int y = m;
		boolean bh = Ui.hover(mx, my, m, y, 18, 18);
		Ui.cutBox(g, m, y, 18, 18, 3, bh ? 0xFF34343A : 0xFF232327, 0xFF3A3A40);
		Ui.icon(g, LEFT, m + 5, y + 5, 1, Ui.TEXT);
		if (bh && click) action = this::onClose;
		g.drawString(font, "Wardrobe", m + 26, y + 5, Ui.TEXT, true);

		Section[] tabs = {Section.FEATURED, Section.COSMETICS, Section.EMOTES, Section.OUTFITS, Section.SKINS};
		String bal = String.format("%,d", Wallet.get().coins);
		int pillW = font.width(bal) + 26, px = this.width - m - pillW;
		int tx = Math.max(m + 26 + font.width("Wardrobe") + 18, gx);
		int gap = 14, need = 0;
		for (Section s : tabs) need += font.width(s.label) + gap;
		if (tx + need > px - 8) gap = Math.max(6, gap - (tx + need - (px - 8)) / tabs.length - 1);
		int selX = -1, selW = 0;
		for (Section s : tabs) {
			int w = font.width(s.label);
			boolean sel = section == s;
			boolean hov = Ui.hover(mx, my, tx - 3, y, w + 6, 18);
			g.drawString(font, s.label, tx, y + 5, sel ? Ui.TEXT : hov ? 0xFFD4D4D8 : Ui.FAINT, false);
			if (sel) { selX = tx; selW = w; }
			if (hov && click && !sel) action = () -> switchTo(s);
			tx += w + gap;
		}
		g.fill(gx, y + 19, this.width - m, y + 20, 0xFF2A2A2E);
		if (selX >= 0) {
			float ux = Ui.approach("wtab:x", selX, 18f), uw = Ui.approach("wtab:w", selW, 18f);
			g.fill(Math.round(ux), y + 18, Math.round(ux + uw), y + 20, Ui.ACCENT_2);
		}

		boolean pillHov = Ui.hover(mx, my, px, y, pillW, 18);
		boolean coins = section == Section.CURRENCY;
		Ui.cutBox(g, px, y, pillW, 18, 3, coins ? 0xFF3A3A40 : pillHov ? 0xFF303035 : 0xFF232327, coins ? Ui.ACCENT_2 : 0xFF3A3A40);
		Ui.coin(g, px + 5, y + 4, 10);
		g.drawString(font, bal, px + 19, y + 5, Ui.TEXT, false);
		if (pillHov && click && !coins) action = () -> switchTo(Section.CURRENCY);
		return action;
	}

	private void pedestal(GuiGraphics g, int cx, int fy, int rx) {
		int ry = Math.max(4, rx * 22 / 100);
		ellipse(g, cx, fy + 4, rx, ry, 0xFF151517);
		ellipse(g, cx, fy, rx, ry, 0xFF26262A);
		ellipse(g, cx, fy, rx - 4, ry - 1, 0xFF2D2D32);
		float t = (System.currentTimeMillis() % 100000L) / 1000f;
		int n = Math.max(18, rx / 2);
		for (int k = 0; k < n; k++) {
			double ang = t * 0.7 + k * Math.PI * 2 / n;
			float depth = (float) (Math.sin(ang) + 1) / 2f;
			int px = cx + (int) Math.round(Math.cos(ang) * (rx - 2));
			int py = fy + (int) Math.round(Math.sin(ang) * (ry - 0.5));
			int col = Colors.alpha(Ui.ACCENT_2, 0.15f + 0.85f * depth);
			g.fill(px, py, px + (depth > 0.5f ? 2 : 1), py + 1, col);
		}
	}

	private static void ellipse(GuiGraphics g, int cx, int cy, int rx, int ry, int color) {
		for (int dy = -ry; dy <= ry; dy++) {
			int hw = (int) Math.round(rx * Math.sqrt(Math.max(0, 1 - (dy / (double) ry) * (dy / (double) ry))));
			if (hw > 0) g.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, color);
		}
	}

	private record Primary(String label, boolean enabled, Runnable run) {}

	private Primary primary(Shop.Item item) {
		Wallet w = Wallet.get();
		boolean owned = w.owns(item.id());
		if (!owned && Locks.locked(item)) return new Primary(item.id().equals("cape:beta") ? "Beta testers only" : "Not available", false, () -> { });
		if (!owned) {
			boolean confirm = item.id().equals(pendingBuy) && System.currentTimeMillis() < pendingUntil;
			return new Primary((confirm ? "Confirm " : "Buy  ") + String.format("%,d", Shop.price(item)), w.coins >= Shop.price(item), () -> buy(item, false));
		}
		if (item.category().equals("emote")) {
			Emotes.Emote e = Emotes.Emote.byName(item.key());
			if (section == Section.EMOTES) return new Primary(selectedEmote == e ? "Pick a slot" : "Add to wheel", true, () -> { selectedEmote = selectedEmote == e ? null : e; if (selectedEmote != null) toast("Click a slot on the wheel", Ui.ACCENT_2); });
			return new Primary("Play", true, () -> { if (minecraft.player == null || e == null) { toast("Join a world to play emotes", Ui.MUTED); return; } minecraft.setScreen(null); Emotes.play(minecraft, e); });
		}
		boolean eq = item.key().equals(equippedKey(typeOf(item)));
		return new Primary(eq ? "Take off" : "Equip", true, () -> setEquipped(typeOf(item), eq ? "none" : item.key()));
	}

	private static String rarityName(Shop.Item item) {
		int p = item.price();
		if (p == 0) return "Exclusive";
		if (p < 700) return "Common";
		if (p < 950) return "Rare";
		if (p < 1150) return "Epic";
		return "Legendary";
	}

	private static String kind(Shop.Item item) {
		if (item.category().equals("emote")) {
			Emotes.Emote e = Emotes.Emote.byName(item.key());
			return e != null && e.music != null ? "Dance with music" : "Emote";
		}
		return switch (typeOf(item)) { case "wings" -> "Wings"; case "halo" -> "Halo"; default -> "Cape"; };
	}

	private boolean bigButton(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, boolean primary, boolean enabled, boolean coin) {
		boolean hov = enabled && Ui.hover(mx, my, x, y, w, h);
		float a = Ui.anim("wbb:" + label + x, hov);
		int fill = !enabled ? 0xFF222225 : primary ? Colors.lerp(0xFFE4E4E7, 0xFFFFFFFF, a) : Colors.lerp(0xFF2A2A2E, 0xFF36363B, a);
		int text = !enabled ? Ui.FAINT : primary ? 0xFF141416 : Ui.TEXT;
		Ui.cutBox(g, x, y, w, h, 4, fill, primary && enabled ? 0xFFFFFFFF : 0xFF3A3A40);
		int tw = font.width(label) + (coin ? 12 : 0);
		int tx = x + (w - tw) / 2;
		g.drawString(font, label, tx, y + (h - 8) / 2, text, false);
		if (coin) Ui.coin(g, tx + font.width(label) + 3, y + (h - 10) / 2, 10);
		return hov;
	}

	private Runnable stage(GuiGraphics g, int mx, int my, boolean click, Runnable action) {
		int x1 = stageX, x2 = stageX + stageW, y1 = top + 4, y2 = this.height - m;
		int cx = (x1 + x2) / 2;
		Shop.Item focus = hoveredItem != null ? hoveredItem : selected;
		if (section == Section.OUTFITS || section == Section.SKINS || section == Section.CURRENCY) focus = hoveredItem;
		applyPreview(focus);
		int infoH = focus != null ? 58 : section == Section.OUTFITS ? 38 : 20;
		int floorY0 = y2 - infoH - 10;
		int size = Math.max(30, Math.min(80, Math.min((floorY0 - y1 - 30) * 100 / 220, stageW * 30 / 100)));
		int rx = Math.min(stageW / 2 - 14, size + 14);
		int floorY = floorY0 - Math.max(4, rx * 22 / 100) - 2;
		pedestal(g, cx, floorY, rx);
		int boxTop = floorY - size * 2 - 22, boxBot = floorY + 6;
		if (minecraft.player != null) {
			int cy = floorY - Math.round(size * 0.97f);
			int half = Math.round(size * 1.15f);
			Preview.draw(g, x1 + 18, cy - half, x2 - 18, cy + half, size);
		} else Ui.centered(g, font, "Join a world to see yourself", cx, (boxTop + floorY) / 2, Ui.FAINT);

		int ay = floorY - 26;
		if (Ui.hover(mx, my, cx - rx - 12, ay, 12, 14) || Ui.hover(mx, my, cx + rx, ay, 12, 14)) {
			boolean lh = Ui.hover(mx, my, cx - rx - 12, ay, 12, 14);
			if (click) action = () -> Preview.forced = (Preview.forced == null ? 0f : Preview.forced) + (lh ? -90f : 90f);
		}
		Ui.icon(g, LEFT, cx - rx - 10, ay + 3, 1, Ui.hover(mx, my, cx - rx - 12, ay, 12, 14) ? Ui.TEXT : 0xFF55555C);
		Ui.icon(g, RIGHT, cx + rx + 2, ay + 3, 1, Ui.hover(mx, my, cx + rx, ay, 12, 14) ? Ui.TEXT : 0xFF55555C);

		int iy = y2 - infoH;
		if (focus != null) {
			int rc = rarity(focus);
			g.drawString(font, Ui.fit(font, focus.name(), stageW - 8), x1 + 4, iy, Ui.TEXT, true);
			String sub = rarityName(focus) + "  " + kind(focus);
			g.fill(x1 + 4, iy + 13, x1 + 8, iy + 17, rc);
			g.drawString(font, Ui.fit(font, sub, stageW - 16), x1 + 12, iy + 12, Ui.MUTED, false);
			if (focus == selected || hoveredItem == null) {
				Primary p = primary(focus);
				boolean beta = focus.id().equals("cape:beta") && Wallet.get().owns(focus.id());
				int bw = beta ? stageW - 8 - 74 : stageW - 8;
				boolean coin = p.label().startsWith("Buy") || p.label().startsWith("Confirm");
				if (bigButton(g, mx, my, x1 + 4, iy + 30, bw, 22, p.label(), true, p.enabled(), coin) && click) action = p.run();
				if (beta && bigButton(g, mx, my, x1 + 8 + bw, iy + 30, 70, 22, "Customize", false, true, false) && click) action = () -> minecraft.setScreen(new BetaCapeScreen(this));
				if (!p.enabled() && p.label().startsWith("Buy")) g.drawString(font, "Need " + (Shop.price(focus) - Wallet.get().coins) + " more", x1 + 4 + bw - font.width("Need " + (Shop.price(focus) - Wallet.get().coins) + " more"), iy + 12, 0xFFFF8C8C, false);
			} else g.drawString(font, "Click to select", x1 + 4, iy + 36, Ui.FAINT, false);
		} else if (section == Section.OUTFITS) {
			g.drawString(font, "Outfit " + (outfitIdx + 1), x1 + 4, iy, Ui.TEXT, true);
			if (bigButton(g, mx, my, x1 + 4, iy + 14, stageW - 8, 20, "Save what I'm wearing", false, true, false) && click) action = this::saveOutfit;
		} else {
			String name = minecraft.getUser().getName();
			Ui.centered(g, font, name, cx, iy + 4, Ui.MUTED);
		}
		return action;
	}

	private Runnable wheelStage(GuiGraphics g, int mx, int my, boolean click, boolean rclick, Runnable action) {
		int x1 = stageX, x2 = stageX + stageW, y1 = top + 4, y2 = this.height - m;
		int cx = (x1 + x2) / 2;
		List<List<String>> pages = EmoteWheelScreen.layout();
		wheelPage = Math.min(wheelPage, pages.size());
		String label = wheelPage >= pages.size() ? "New wheel" : "Wheel " + (wheelPage + 1);
		int lw = font.width(label);
		Ui.centered(g, font, label, cx, y1 + 4, Ui.TEXT);
		boolean ph = Ui.hover(mx, my, cx - lw / 2 - 18, y1, 14, 16), nh = Ui.hover(mx, my, cx + lw / 2 + 4, y1, 14, 16);
		Ui.icon(g, LEFT, cx - lw / 2 - 14, y1 + 4, 1, ph ? Ui.TEXT : 0xFF6E6F74);
		Ui.icon(g, RIGHT, cx + lw / 2 + 6, y1 + 4, 1, nh ? Ui.TEXT : 0xFF6E6F74);
		int count = pages.size();
		if (ph && click) action = () -> wheelPage = (wheelPage + count) % (count + 1);
		if (nh && click) action = () -> wheelPage = (wheelPage + 1) % (count + 1);

		boolean fresh = wheelPage >= pages.size();
		List<String> slots = fresh ? List.of("", "", "", "", "", "", "", "") : pages.get(wheelPage);
		int areaTop = y1 + 18, areaBot = y2 - 30;
		int cy = (areaTop + areaBot) / 2;
		int R = Math.max(36, Math.min(stageW / 2 - 4, (areaBot - areaTop) / 2) - 18);
		int cell = Math.max(24, Math.min(46, Math.round(R * 0.62f)));
		R = Math.min(R, Math.min(stageW / 2 - cell / 2 - 2, (areaBot - areaTop) / 2 - cell / 2));
		for (int k = 0; k < 64; k++) {
			double a = k * Math.PI * 2 / 64;
			int px = cx + (int) Math.round(Math.cos(a) * R), py = cy + (int) Math.round(Math.sin(a) * R);
			g.fill(px, py, px + 1, py + 1, 0xFF2E2E33);
		}
		Emotes.Emote center = selectedEmote;
		Shop.Item hi = hoveredItem;
		if (hi != null && hi.category().equals("emote")) center = Emotes.Emote.byName(hi.key());
		Cosmetics.preview(null, null);
		Emotes.setPreview(null);
		int cs = Math.max(20, Math.round(R * 0.42f));
		Ui.cut(g, cx - R + cell / 2 + 4, cy - cs - 6, (R - cell / 2 - 4) * 2, cs * 2 + 14, 6, 0xFF1C1C1F);
		if (center != null) {
			float t = (System.currentTimeMillis() - openedAt) / 1000f;
			if (!center.loops) t = t % (center.seconds + 0.6f);
			Preview.drawCard(g, cx - R + cell / 2, cy - cs - 4, cx + R - cell / 2, cy + cs + 6, Math.round(cs * 0.62f), 18f, new CapturedLook(null, null, null, center, t, true));
		}
		int hovered = -1;
		for (int i = 0; i < 8; i++) {
			double a = -Math.PI / 2 + i * Math.PI / 4;
			int sx = cx + (int) Math.round(Math.cos(a) * R) - cell / 2, sy = cy + (int) Math.round(Math.sin(a) * R) - cell / 2;
			boolean hov = Ui.hover(mx, my, sx, sy, cell, cell);
			if (hov) hovered = i;
			Emotes.Emote e = i < slots.size() ? Emotes.Emote.byName(slots.get(i)) : null;
			if (e != null && !Emotes.owned(e)) e = null;
			int edge = hov && selectedEmote != null ? Ui.ACCENT_2 : hov ? 0xFF55555C : 0xFF34343A;
			Ui.cutBox(g, sx, sy, cell, cell, 4, hov ? 0xFF2E2E33 : 0xFF232327, edge);
			if (e != null) {
				float t = (System.currentTimeMillis() - openedAt) / 1000f + i * 0.5f;
				if (!e.loops) t = t % (e.seconds + 0.6f);
				Preview.drawCard(g, sx + 1, sy + 3, sx + cell - 1, sy + cell - 1, Math.round(cell * 0.3f), 18f, new CapturedLook(null, null, null, e, t, true));
			} else Ui.centered(g, font, "+", sx + cell / 2, sy + cell / 2 - 4, 0xFF4A4A52);
			g.drawString(font, String.valueOf(i + 1), sx + 3, sy + 3, 0x70FFFFFF, false);
			final int slot = i;
			final Emotes.Emote here = e;
			if (hov && click) action = () -> {
				if (selectedEmote != null) {
					List<List<String>> ed = EmoteWheelScreen.editable();
					while (ed.size() <= wheelPage && ed.size() < 9) { List<String> p = new ArrayList<>(); for (int k = 0; k < 8; k++) p.add(""); ed.add(p); }
					if (wheelPage >= ed.size()) { toast("You have 9 wheels already", Ui.MUTED); return; }
					List<String> p = ed.get(wheelPage);
					while (p.size() < 8) p.add("");
					p.set(slot, selectedEmote.name());
					SurgeConfig.save();
					toast("Put " + selectedEmote.label + " in slot " + (slot + 1), 0xFF8CFFA0);
					selectedEmote = null;
				} else if (here != null) selectedEmote = here;
			};
			if (hov && rclick && here != null) action = () -> {
				List<String> p = EmoteWheelScreen.editable().get(wheelPage);
				p.set(slot, "");
				SurgeConfig.save();
			};
		}
		String info;
		if (selectedEmote != null) info = "Placing " + selectedEmote.label + ", pick a slot";
		else if (hovered >= 0 && hovered < slots.size() && Emotes.Emote.byName(slots.get(hovered)) != null) info = Emotes.Emote.byName(slots.get(hovered)).label + "  (right click to remove)";
		else info = "Pick an emote, then a slot";
		Ui.centered(g, font, Ui.fit(font, info, stageW - 8), cx, y2 - 22, selectedEmote != null ? Ui.ACCENT_2 : Ui.FAINT);
		Ui.centered(g, font, "Press B in game to open it", cx, y2 - 10, 0xFF4A4A52);
		return action;
	}

	private Runnable grid(GuiGraphics g, int mx, int my, boolean click, boolean rclick, float delta, Runnable action) {
		String[] subs = section == Section.EMOTES ? EMOTE_SUBS : section == Section.COSMETICS ? COSMETIC_SUBS : null;
		int cy = top + 2, cxp = gx;
		if (subs != null) {
			for (String sub : subs) {
				boolean on = sub.equals(section == Section.EMOTES ? emoteSub : cosmeticSub);
				int w = font.width(sub) + 12;
				boolean hov = Ui.hover(mx, my, cxp, cy, w, 15);
				Ui.cutBox(g, cxp, cy, w, 15, 3, on ? 0xFFE4E4E7 : hov ? 0xFF303035 : 0xFF202023, on ? 0xFFFFFFFF : 0xFF34343A);
				g.drawString(font, sub, cxp + 6, cy + 4, on ? 0xFF141416 : Ui.MUTED, false);
				if (hov && click) action = () -> { if (section == Section.EMOTES) emoteSub = sub; else cosmeticSub = sub; scroll = scrollTarget = 0; };
				cxp += w + 4;
			}
		} else g.drawString(font, "New, on sale and dances with music", gx, cy + 4, Ui.FAINT, false);
		int bx = gx + gw - 15;
		if (squareButton(g, mx, my, bx, cy, Ui.SEARCH, searching) && click) action = () -> { searching = !searching; if (!searching) searchBox.setValue(""); setFocused(searching ? searchBox : null); searchBox.setFocused(searching); };
		bx -= 18;
		if (squareButton(g, mx, my, bx, cy, new String[]{"XXXXXXX.", "........", "XXXXX...", "........", "XXX.....", "........", "X.......", "........"}, false) && click) action = () -> { sortIdx = (sortIdx + 1) % SORTS.length; toast("Sorted by " + SORTS[sortIdx].toLowerCase(), Ui.MUTED); };
		searchBox.visible = searching;
		if (searching) {
			int sw = Math.min(120, Math.max(50, bx - cxp - 8)), sx = bx - sw - 3;
			Ui.cutBox(g, sx, cy, sw, 15, 3, 0xFF141416, searchBox.isFocused() ? Ui.ACCENT_2 : 0xFF34343A);
			searchBox.setX(sx + 5);
			searchBox.setY(cy + 4);
			searchBox.setWidth(sw - 10);
			searchBox.render(g, mx, my, delta);
		}

		List<Shop.Item> list = items();
		int gap = 5, cols = Math.max(2, (gw + gap) / (68 + gap));
		int tw = (gw - 4 - (cols - 1) * gap) / cols, th = Math.round(tw * 1.18f);
		contentH = ((list.size() + cols - 1) / cols) * (th + gap) + 4;
		scrollTarget = Math.max(0, Math.min(Math.max(0, contentH - gridH), scrollTarget));
		g.enableScissor(gx - 2, gridTop, gx + gw, gridTop + gridH);
		boolean inView = Ui.hover(mx, my, gx, gridTop, gw, gridH);
		Wallet w = Wallet.get();
		for (int i = 0; i < list.size(); i++) {
			Shop.Item item = list.get(i);
			int x = gx + (i % cols) * (tw + gap);
			int y = gridTop + 2 + (i / cols) * (th + gap) - Math.round(scroll);
			if (y + th < gridTop || y > gridTop + gridH) continue;
			boolean hov = inView && Ui.hover(mx, my, x, y, tw, th);
			boolean owned = w.owns(item.id());
			boolean locked = !owned && Locks.locked(item);
			boolean isEmote = item.category().equals("emote");
			boolean equipped = !isEmote && owned && item.key().equals(equippedKey(typeOf(item)));
			boolean sel = selected != null && selected.id().equals(item.id());
			int rc = rarity(item);
			float a = Ui.anim("wc:" + item.id(), hov);
			int edge = sel ? 0xFFFFFFFF : equipped ? Ui.ACCENT_2 : Colors.lerp(0xFF2E2E33, 0xFF6A6A72, a);
			Ui.cutBox(g, x, y, tw, th, 5, Colors.lerp(0xFF1E1E21, 0xFF27272B, a), edge);
			g.fillGradient(x + 1, y + th / 2, x + tw - 1, y + th - 14, 0x00000000, Colors.alpha(rc, 0.12f + 0.1f * a));
			for (int k = 0; k < 7; k++) g.fill(x + 1, y + 1 + k, x + 8 - k, y + 2 + k, k < 4 ? rc : Colors.alpha(rc, 0.5f));
			Preview.drawCard(g, x + 1, y + 4, x + tw - 1, y + th - 14, Math.round((th - 18) * 0.34f), cardTurn(item), lookFor(item, i));
			g.fill(x + 1, y + th - 14, x + tw - 1, y + th - 13, 0xFF2A2A2E);
			g.drawString(font, Ui.fit(font, item.name(), tw - 8), x + 5, y + th - 11, hov || sel ? Ui.TEXT : 0xFFC8C8CD, false);
			if (owned) {
				if (equipped) { String eq = "ON"; g.fill(x + tw - font.width(eq) - 9, y + 4, x + tw - 4, y + 14, Ui.ACCENT_2); g.drawString(font, eq, x + tw - font.width(eq) - 6, y + 5, 0xFF141416, false); }
				else Ui.icon(g, CHECK, x + tw - 12, y + 5, 1, 0xFF6EE07A);
			} else if (locked) Ui.icon(g, LOCK, x + tw - 12, y + 5, 1, 0xFFFF8C8C);
			else {
				String price = String.format("%,d", Shop.price(item));
				int pw = font.width(price) + 15;
				int sale = Sales.discount(item);
				g.fill(x + tw - pw - 4, y + 4, x + tw - 4, y + 15, sale > 0 ? 0xE0E0A21A : 0xC0101012);
				g.drawString(font, price, x + tw - pw - 1, y + 6, sale > 0 ? 0xFF1A1200 : Ui.TEXT, false);
				Ui.coin(g, x + tw - 15, y + 5, 9);
			}
			Emotes.Emote em = isEmote ? Emotes.Emote.byName(item.key()) : null;
			if (em != null && em.music != null) Ui.icon(g, NOTE, x + 5, y + th - 25, 1, 0xC0FFFFFF);
			if (hov) hoveredItem = item;
			if (hov && rclick) itemMenu(mx, my, item);
			if (hov && click) {
				boolean dbl = item.id().equals(lastClicked) && System.currentTimeMillis() - lastClickAt < 350;
				lastClicked = item.id();
				lastClickAt = System.currentTimeMillis();
				action = () -> {
					if (dbl && sel) { Primary p = primary(item); if (p.enabled()) p.run().run(); return; }
					selected = item;
					if (!owned) pendingBuy = null;
				};
			}
		}
		g.disableScissor();
		if (list.isEmpty()) Ui.centered(g, font, searching ? "Nothing matches that search" : "Nothing here yet", gx + gw / 2, gridTop + 40, Ui.FAINT);
		scrollbar(g, gx + gw - 2, gridTop, gridH);
		return action;
	}

	private boolean squareButton(GuiGraphics g, int mx, int my, int x, int y, String[] icon, boolean on) {
		boolean hov = Ui.hover(mx, my, x, y, 15, 15);
		Ui.cutBox(g, x, y, 15, 15, 3, on ? 0xFF3A3A40 : hov ? 0xFF303035 : 0xFF202023, on ? Ui.ACCENT_2 : 0xFF34343A);
		Ui.icon(g, icon, x + 4, y + 4, 1, Ui.TEXT);
		return hov;
	}

	private void scrollbar(GuiGraphics g, int x, int top, int h) {
		if (contentH <= h) return;
		int barH = Math.max(16, h * h / contentH);
		int barY = top + Math.round((h - barH) * (scroll / Math.max(1, contentH - h)));
		g.fill(x, barY, x + 2, barY + barH, 0xFF5A5A62);
	}

	private Runnable outfits(GuiGraphics g, int mx, int my, boolean click, boolean rclick, Runnable action) {
		SurgeConfig c = SurgeConfig.get();
		g.drawString(font, "Save up to three looks and switch with one click", gx, top + 6, Ui.FAINT, false);
		int gap = 8, cw = (gw - gap * 2) / 3, ch = Math.min(gridH - 8, Math.round(cw * 1.5f));
		contentH = 0;
		for (int i = 0; i < 3; i++) {
			int x = gx + i * (cw + gap), y = gridTop + 2;
			SurgeConfig.Outfit o = c.outfits[i];
			boolean hov = Ui.hover(mx, my, x, y, cw, ch);
			boolean cur = i == outfitIdx;
			float a = Ui.anim("wo:" + i, hov);
			Ui.cutBox(g, x, y, cw, ch, 6, Colors.lerp(0xFF1E1E21, 0xFF27272B, a), cur ? 0xFFFFFFFF : Colors.lerp(0xFF2E2E33, 0xFF6A6A72, a));
			g.drawString(font, String.valueOf(i + 1), x + 8, y + 7, cur ? Ui.TEXT : Ui.FAINT, true);
			if (o != null) {
				CapturedLook look = new CapturedLook(Cosmetics.equipped("cape", o.cape).equals("none") ? null : o.cape, Cosmetics.equipped("wings", o.wings).equals("none") ? null : o.wings,
					Cosmetics.equipped("halo", o.halo).equals("none") ? null : o.halo, null, 0f, false);
				Preview.drawCard(g, x + 2, y + 16, x + cw - 2, y + ch - 20, Math.round((ch - 40) * 0.3f), 160f, look);
				Ui.centered(g, font, cur ? "Wearing" : "Click to wear", x + cw / 2, y + ch - 14, cur ? Ui.ACCENT_2 : Ui.MUTED);
			} else {
				Ui.centered(g, font, "+", x + cw / 2, y + ch / 2 - 8, 0xFF55555C);
				Ui.centered(g, font, "Click to save", x + cw / 2, y + ch / 2 + 4, Ui.FAINT);
			}
			final int idx = i;
			if (hov && click) action = () -> { if (c.outfits[idx] == null) { outfitIdx = idx; saveOutfit(); } else { wearOutfit(idx); toast("Wearing outfit " + (idx + 1), 0xFF8CFFA0); } };
			if (hov && rclick) {
				List<String> l = new ArrayList<>(List.of("Wear", "Save what I'm wearing here"));
				List<Runnable> a2 = new ArrayList<>(List.of(() -> wearOutfit(idx), () -> { outfitIdx = idx; saveOutfit(); }));
				if (o != null) { l.add("Clear"); a2.add(() -> { c.outfits[idx] = null; SurgeConfig.save(); }); }
				openMenu(mx, my, l, a2);
			}
		}
		return action;
	}

	private Runnable skinsPage(GuiGraphics g, int mx, int my, boolean click, Runnable action) {
		int bx = gx, y0 = top + 2;
		if (bigButton(g, mx, my, bx, y0, 80, 16, "Open folder", false, true, false) && click) action = SkinUploader::openFolder;
		if (bigButton(g, mx, my, bx + 84, y0, 60, 16, slim ? "Slim arms" : "Wide arms", false, true, false) && click) action = () -> slim = !slim;
		if (bigButton(g, mx, my, bx + 148, y0, 56, 16, "Refresh", false, true, false) && click) action = () -> skins = SkinUploader.list();
		int y = gridTop + 4;
		if (skins.isEmpty()) {
			for (String line : wrap("Drop .png skin files into the folder, then press Refresh. They show up here to apply with one click.", gw - 8)) { g.drawString(font, line, bx, y, Ui.MUTED, false); y += 11; }
			return action;
		}
		for (int i = 0; i < skins.size() && y + 22 < this.height - m; i++, y += 24) {
			Path p = skins.get(i);
			String name = p.getFileName().toString().replaceAll("(?i)\\.png$", "");
			boolean hov = Ui.hover(mx, my, bx, y, gw, 22);
			Ui.cutBox(g, bx, y, gw, 22, 4, hov ? 0xFF27272B : 0xFF1E1E21, hov ? 0xFF55555C : 0xFF2E2E33);
			g.drawString(font, Ui.fit(font, name, gw - 80), bx + 8, y + 7, Ui.TEXT, false);
			if (bigButton(g, mx, my, bx + gw - 58, y + 3, 54, 16, "Apply", true, true, false) && click) action = () -> {
				toast("Uploading…", Ui.MUTED);
				SkinUploader.upload(p, slim).thenAccept(err -> minecraft.execute(() -> toast(err == null ? "Skin applied. Rejoin to see it." : err, err == null ? 0xFF8CFFA0 : 0xFFFF8C8C)));
			};
		}
		return action;
	}

	private Runnable coinsPage(GuiGraphics g, int mx, int my, boolean click, float delta, Runnable action) {
		int x = gx, y = top + 4, cw = gw;
		Ui.cutBox(g, x, y, cw, 52, 6, 0xFF1E1E21, 0xFF2E2E33);
		Ui.coin(g, x + 10, y + 10, 32);
		g.drawString(font, Component.literal(String.format("%,d", Wallet.get().coins)).withStyle(ChatFormatting.BOLD), x + 50, y + 15, Ui.TEXT, true);
		g.drawString(font, "Surge Coins", x + 50, y + 28, Ui.MUTED, false);

		Wallet w = Wallet.get();
		int ry = y + 60, half = (cw - 6) / 2;
		Ui.cutBox(g, x, ry, half, 50, 5, 0xFF1E1E21, 0xFF2E2E33);
		g.drawString(font, "Daily reward", x + 8, ry + 7, Ui.TEXT, false);
		g.drawString(font, Ui.fit(font, "Tomorrow +" + gg.surge.client.economy.Rewards.dailyAmount(w.streak + 1), half - 16), x + 8, ry + 20, Ui.MUTED, false);
		for (int d = 0; d < 7; d++) {
			int dx = x + 8 + d * Math.max(6, (half - 16) / 7);
			g.fill(dx, ry + 36, dx + Math.max(4, (half - 16) / 7 - 2), ry + 40, d < Math.min(7, w.streak) ? Ui.ACCENT_2 : 0xFF2E2E33);
		}
		int px2 = x + half + 6;
		Ui.cutBox(g, px2, ry, cw - half - 6, 50, 5, 0xFF1E1E21, 0xFF2E2E33);
		g.drawString(font, "Playtime", px2 + 8, ry + 7, Ui.TEXT, false);
		int left = gg.surge.client.economy.Rewards.PLAY_MINUTES * 60 - w.playSeconds;
		g.drawString(font, Ui.fit(font, "+" + gg.surge.client.economy.Rewards.PLAY_REWARD + " in " + Math.max(1, left / 60) + " min", cw - half - 22), px2 + 8, ry + 20, Ui.MUTED, false);
		int bw2 = cw - half - 22;
		g.fill(px2 + 8, ry + 36, px2 + 8 + bw2, ry + 40, 0xFF2E2E33);
		g.fill(px2 + 8, ry + 36, px2 + 8 + Math.round(bw2 * (w.playSeconds / (float) (gg.surge.client.economy.Rewards.PLAY_MINUTES * 60))), ry + 40, Ui.ACCENT_2);

		int cy = ry + 58;
		g.drawString(font, "Redeem a code", x, cy, Ui.TEXT, false);
		int bw = Math.max(60, cw - 76);
		Ui.cutBox(g, x, cy + 12, bw, 20, 4, 0xFF141416, codeBox != null && codeBox.isFocused() ? Ui.ACCENT_2 : 0xFF34343A);
		if (codeBox != null) {
			codeBox.setX(x + 7);
			codeBox.setY(cy + 18);
			codeBox.setWidth(bw - 14);
			codeBox.render(g, mx, my, delta);
			if (GLFW.glfwGetKey(minecraft.getWindow().handle(), GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS && codeBox.isFocused() && !redeeming) action = this::redeem;
		}
		if (bigButton(g, mx, my, x + bw + 6, cy + 12, 70, 20, redeeming ? "…" : "Redeem", true, true, false) && click) action = this::redeem;
		if (!message.isEmpty() && System.currentTimeMillis() - messageAt < 8000) {
			List<String> lines = wrap(message, cw);
			for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x, cy + 38 + i * 10, messageColor, false);
		}
		g.drawString(font, Ui.fit(font, "Codes can give coins, cosmetics or emotes.", cw), x, this.height - m - 10, Ui.FAINT, false);
		return action;
	}

	private void wearOutfit(int idx) {
		outfitIdx = idx;
		SurgeConfig c = SurgeConfig.get();
		SurgeConfig.Outfit o = c.outfits[idx];
		if (o == null) return;
		c.wings = o.wings;
		c.halo = o.halo;
		c.cape = o.cape;
		SurgeConfig.save();
	}

	private void saveOutfit() {
		SurgeConfig c = SurgeConfig.get();
		SurgeConfig.Outfit n = new SurgeConfig.Outfit();
		n.wings = c.wings;
		n.halo = c.halo;
		n.cape = c.cape;
		c.outfits[outfitIdx] = n;
		SurgeConfig.save();
		toast("Saved outfit " + (outfitIdx + 1), 0xFF8CFFA0);
	}

	private List<String> wrap(String text, int w) {
		List<String> out = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			String next = line.isEmpty() ? word : line + " " + word;
			if (font.width(next) > w && !line.isEmpty()) { out.add(line.toString()); line = new StringBuilder(word); }
			else line = new StringBuilder(next);
		}
		if (!line.isEmpty()) out.add(line.toString());
		return out;
	}

	private void applyPreview(Shop.Item item) {
		if (item == null || item.category().equals("emote")) {
			Cosmetics.preview(null, null);
			Emotes.setPreview(item == null ? null : Emotes.Emote.byName(item.key()));
			return;
		}
		Cosmetics.preview(typeOf(item), item.key());
		Emotes.setPreview(null);
	}

	@Override
	public void removed() {
		Cosmetics.preview(null, null);
		Emotes.setPreview(null);
		Preview.forced = null;
		super.removed();
	}

	@Override
	public void onClose() {
		SurgeConfig.save();
		minecraft.setScreen(parent);
	}
}
