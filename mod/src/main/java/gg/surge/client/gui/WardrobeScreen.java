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
		COSMETICS("Cosmetics", Items.ELYTRA), EMOTES("Emotes", Items.ARMOR_STAND), OUTFITS("Outfits", Items.LEATHER_CHESTPLATE),
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

	private static Section section = Section.COSMETICS;
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
	private int wx, wy, ww, wh, sideW, rightW, mx0, mw, gridTop, gridH;
	private float scroll, scrollTarget;
	private int contentH;
	private long openedAt;
	private Shop.Item hoveredItem;
	private Emotes.Emote selectedEmote;
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
		g.fill(0, 0, this.width, this.height, Colors.alpha(0xB0101012, openProgress()));
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
		ww = Math.min(620, this.width - 44);
		wh = Math.min(340, this.height - 24);
		wx = (this.width - ww) / 2 + 10;
		wy = (this.height - wh) / 2;
		sideW = Math.max(78, Math.min(100, ww / 6));
		rightW = Math.max(130, Math.min(230, (int) (ww * 0.36f)));
		mx0 = wx + sideW + 2;
		mw = ww - sideW - rightW - 4;
		gridTop = wy + 26;
		gridH = wy + wh - gridTop - 2;
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

	private List<Shop.Item> items() {
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

		if (Ui.iconButton(g, mx, my, wx - 24, wy, 18, new String[]{"....X...", "...X....", "..X.....", ".X......", "..X.....", "...X....", "....X...", "........"}) && click) action = this::onClose;

		g.fill(wx, wy, wx + ww, wy + wh, 0xFF1E1E21);
		Ui.outline(g, wx - 1, wy - 1, ww + 2, wh + 2, 0xFF2C2C30);
		g.fill(wx + sideW, wy, wx + sideW + 2, wy + wh, 0xFF141416);
		g.fill(wx + ww - rightW - 2, wy, wx + ww - rightW, wy + wh, 0xFF141416);

		action = sidebar(g, mx, my, click, action);
		action = middle(g, mx, my, click, rclick, delta, action);
		action = right(g, mx, my, click, rclick, action);

		long age = System.currentTimeMillis() - messageAt;
		if (!message.isEmpty() && age < 4000 && section != Section.CURRENCY) {
			float a = Math.min(1f, Math.min(age / 150f, (4000 - age) / 400f));
			int w = font.width(message) + 14;
			int tx = mx0 + (mw - w) / 2, ty = wy + wh - 24;
			g.nextStratum();
			g.fill(tx, ty, tx + w, ty + 16, Colors.alpha(0xF0101012, a));
			Ui.outline(g, tx, ty, w, 16, Colors.alpha(messageColor, a));
			g.drawString(font, message, tx + 7, ty + 4, Colors.alpha(messageColor, a), false);
		}

		if (menuLabels != null) {
			g.nextStratum();
			int w = menuWidth(), h = menuLabels.size() * 16 + 6;
			g.fill(menuX, menuY, menuX + w, menuY + h, 0xFA202023);
			Ui.outline(g, menuX, menuY, w, h, 0xFF3A3A40);
			for (int i = 0; i < menuLabels.size(); i++) {
				int y = menuY + 3 + i * 16;
				if (Ui.hover(mx, my, menuX, y, w, 16)) g.fill(menuX + 2, y, menuX + w - 2, y + 16, 0xFF34343A);
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

	private Runnable sidebar(GuiGraphics g, int mx, int my, boolean click, Runnable action) {
		g.drawString(font, "Wardrobe", wx + 8, wy + 9, Ui.TEXT, false);
		g.fill(wx, wy + 24, wx + sideW, wy + 25, 0xFF141416);
		int y = wy + 30;
		Object[][] groups = {{Section.OUTFITS, 0xFF6EE07A}, {Section.SKINS, 0xFFFF8AD8}, {Section.EMOTES, 0xFFFFB347}, {Section.COSMETICS, 0xFFFFE066}};
		for (Object[] grp : groups) {
			Section s = (Section) grp[0];
			int col = (Integer) grp[1];
			boolean sel = section == s;
			boolean hov = Ui.hover(mx, my, wx, y - 3, sideW, 16);
			if (sel || hov) g.fill(wx + 2, y - 3, wx + sideW - 2, y + 13, sel ? 0xFF2A2A2E : 0xFF242427);
			g.drawString(font, s.label, wx + 8, y + 1, col, false);
			if (hov && click && !sel) action = () -> switchTo(s);
			y += 18;
			String[] subs = s == Section.EMOTES ? EMOTE_SUBS : s == Section.COSMETICS ? COSMETIC_SUBS : null;
			if (sel && subs != null) {
				for (String sub : subs) {
					boolean on = sub.equals(s == Section.EMOTES ? emoteSub : cosmeticSub);
					boolean sh = Ui.hover(mx, my, wx, y - 2, sideW, 12);
					g.drawString(font, Ui.fit(font, sub, sideW - 20), wx + 12, y, on ? Ui.TEXT : sh ? 0xFFCFCFD4 : Ui.FAINT, false);
					if (sh && click) action = () -> { if (s == Section.EMOTES) emoteSub = sub; else cosmeticSub = sub; scroll = scrollTarget = 0; };
					y += 12;
				}
				y += 4;
			}
			g.fill(wx + 6, y - 2, wx + sideW - 6, y - 1, 0xFF2A2A2E);
			y += 4;
		}
		return action;
	}

	private Runnable middle(GuiGraphics g, int mx, int my, boolean click, boolean rclick, float delta, Runnable action) {
		String title = switch (section) {
			case EMOTES -> emoteSub.equals("All") ? "Emotes" : emoteSub;
			case COSMETICS -> cosmeticSub.equals("All") ? "Cosmetics" : cosmeticSub;
			default -> section.label;
		};
		g.drawString(font, title, mx0 + 8, wy + 9, Ui.TEXT, false);
		g.fill(mx0, wy + 24, mx0 + mw, wy + 25, 0xFF141416);

		String bal = String.format("%,d", Wallet.get().coins);
		int pillW = font.width(bal) + 24, px = mx0 + mw - pillW - 6;
		boolean pillHov = Ui.hover(mx, my, px, wy + 5, pillW, 15);
		g.fill(px, wy + 5, px + pillW, wy + 20, pillHov ? 0xFF4A4A52 : 0xFF38383E);
		g.drawString(font, bal, px + 5, wy + 9, 0xFFFFFFFF, false);
		Ui.coin(g, px + pillW - 14, wy + 7, 11);
		if (pillHov && click) action = () -> switchTo(Section.CURRENCY);

		boolean grid = section == Section.COSMETICS || section == Section.EMOTES;
		if (grid) {
			int bx = px - 19;
			if (squareButton(g, mx, my, bx, wy + 5, Ui.SEARCH, searching) && click) action = () -> { searching = !searching; if (!searching) searchBox.setValue(""); setFocused(searching ? searchBox : null); searchBox.setFocused(searching); };
			bx -= 17;
			if (squareButton(g, mx, my, bx, wy + 5, new String[]{"XXXXXXX.", "........", "XXXXX...", "........", "XXX.....", "........", "X.......", "........"}, false) && click) action = () -> { sortIdx = (sortIdx + 1) % SORTS.length; toast("Sorted by " + SORTS[sortIdx].toLowerCase(), Ui.MUTED); };
			searchBox.visible = searching;
			if (searching) {
				int sw = Math.max(40, bx - (mx0 + 8 + font.width(title) + 8) - 4), sx = bx - sw - 4;
				g.fill(sx, wy + 5, sx + sw, wy + 20, 0xFF141416);
				Ui.outline(g, sx, wy + 5, sw, 15, searchBox.isFocused() ? Ui.ACCENT_2 : 0xFF34343A);
				searchBox.setX(sx + 4);
				searchBox.setY(wy + 9);
				searchBox.setWidth(sw - 8);
				searchBox.render(g, mx, my, delta);
			}
		} else searchBox.visible = false;

		scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		switch (section) {
			case COSMETICS, EMOTES -> action = grid(g, mx, my, click, rclick, action);
			case OUTFITS -> action = outfits(g, mx, my, click, rclick, action);
			case SKINS -> action = skinsPage(g, mx, my, click, action);
			case CURRENCY -> action = coinsPage(g, mx, my, click, delta, action);
		}
		return action;
	}

	private boolean squareButton(GuiGraphics g, int mx, int my, int x, int y, String[] icon, boolean on) {
		boolean hov = Ui.hover(mx, my, x, y, 15, 15);
		g.fill(x, y, x + 15, y + 15, on ? 0xFF4A4A52 : hov ? 0xFF3A3A40 : 0xFF2C2C30);
		Ui.icon(g, icon, x + 4, y + 4, 1, Ui.TEXT);
		return hov;
	}

	private Runnable grid(GuiGraphics g, int mx, int my, boolean click, boolean rclick, Runnable action) {
		List<Shop.Item> list = items();
		int pad = 6, cols = mw >= 200 ? 3 : 2;
		int cardW = (mw - pad * 2 - (cols - 1) * pad) / cols;
		int imgH = cardW, cardH = imgH + 13;
		contentH = ((list.size() + cols - 1) / cols) * (cardH + pad) + pad;
		scrollTarget = Math.max(0, Math.min(Math.max(0, contentH - gridH), scrollTarget));
		int left = mx0 + pad;
		g.enableScissor(mx0, gridTop, mx0 + mw, gridTop + gridH);
		boolean inView = Ui.hover(mx, my, mx0, gridTop, mw, gridH);
		Wallet w = Wallet.get();
		for (int i = 0; i < list.size(); i++) {
			Shop.Item item = list.get(i);
			int x = left + (i % cols) * (cardW + pad);
			int y = gridTop + pad + (i / cols) * (cardH + pad) - Math.round(scroll);
			if (y + cardH < gridTop || y > gridTop + gridH) continue;
			boolean hov = inView && Ui.hover(mx, my, x, y, cardW, cardH);
			boolean owned = w.owns(item.id());
			boolean locked = !owned && Locks.locked(item);
			boolean isEmote = item.category().equals("emote");
			boolean equipped = !isEmote && owned && item.key().equals(equippedKey(typeOf(item)));
			boolean picked = isEmote && selectedEmote != null && selectedEmote.name().equals(item.key());
			int rc = rarity(item);
			float a = Ui.anim("wc:" + item.id(), hov);
			int base = Colors.lerp(0xFF29292D, 0xFF323237, a);
			g.fill(x, y, x + cardW, y + imgH, base);
			g.fillGradient(x, y + imgH / 2, x + cardW, y + imgH, 0x00000000, Colors.alpha(owned ? 0xFF2E7D32 : rc, 0.22f));
			Preview.drawCard(g, x, y + 2, x + cardW, y + imgH - 2, Math.round(cardW * 0.36f), cardTurn(item), lookFor(item, i));
			g.fill(x, y + imgH - 2, x + cardW, y + imgH, owned ? 0xFF3FAF4F : rc);
			if (equipped || picked) Ui.outline(g, x - 1, y - 1, cardW + 2, imgH + 2, 0xFFE4E4E7);
			else if (a > 0.05f) Ui.outline(g, x - 1, y - 1, cardW + 2, imgH + 2, Colors.alpha(0xFFFFFFFF, a * 0.35f));
			if (isEmote && Emotes.Emote.byName(item.key()) != null && Emotes.Emote.byName(item.key()).music != null) {
				g.fill(x + cardW - 12, y + 2, x + cardW - 2, y + 12, 0xC0000000);
				Ui.icon(g, new String[]{"...XXXX.", "...X..X.", "...X..X.", "...X..X.", ".XXX.XX.", "XXXXXXX.", ".XX.XX..", "........"}, x + cardW - 11, y + 3, 1, 0xFFFFFFFF);
			}
			int sale = Sales.discount(item);
			if (sale > 0 && !owned && !locked) {
				String tag = "-" + sale + "%";
				g.fill(x + 2, y + 2, x + 6 + font.width(tag), y + 12, 0xFFE0A21A);
				g.drawString(font, tag, x + 4, y + 3, 0xFF1A1200, false);
			}
			if (owned) Ui.icon(g, new String[]{"........", ".......X", "......XX", "X....XX.", "XX..XX..", ".XXXX...", "..XX....", "........"}, x + cardW - 11, y + imgH - 12, 1, 0xFF6EE07A);
			else if (locked) g.drawString(font, item.id().equals("cape:beta") ? "BETA" : "LOCKED", x + 3, y + imgH - 11, 0xFFFF8C8C, true);
			else {
				String price = String.format("%,d", Shop.price(item));
				g.drawString(font, price, x + 3, y + imgH - 11, item.id().equals(pendingBuy) && System.currentTimeMillis() < pendingUntil ? Ui.YELLOW : 0xFFE4E4E7, true);
				Ui.coin(g, x + 4 + font.width(price), y + imgH - 12, 9);
			}
			g.drawString(font, Ui.fit(font, item.name(), cardW), x, y + imgH + 3, hov ? Ui.TEXT : 0xFFD4D4D8, false);
			if (hov) hoveredItem = item;
			if (hov && rclick) itemMenu(mx, my, item);
			if (hov && click) {
				action = () -> {
					if (locked) { toast(item.id().equals("cape:beta") ? "This cape is only for beta testers" : "This can't be bought right now", Ui.MUTED); return; }
					if (!owned) { buy(item, false); return; }
					if (isEmote) {
						Emotes.Emote e = Emotes.Emote.byName(item.key());
						selectedEmote = picked ? null : e;
						if (!picked) toast("Click a slot on the wheel to put " + item.name() + " there", Ui.ACCENT_2);
						return;
					}
					if (item.id().equals("cape:beta")) { minecraft.setScreen(new BetaCapeScreen(this)); return; }
					setEquipped(typeOf(item), equipped ? "none" : item.key());
				};
			}
		}
		g.disableScissor();
		if (list.isEmpty()) Ui.centered(g, font, "Nothing here", mx0 + mw / 2, gridTop + 40, Ui.FAINT);
		scrollbar(g, mx0 + mw - 3, gridTop, gridH);
		return action;
	}

	private void scrollbar(GuiGraphics g, int x, int top, int h) {
		if (contentH <= h) return;
		int barH = Math.max(16, h * h / contentH);
		int barY = top + Math.round((h - barH) * (scroll / Math.max(1, contentH - h)));
		g.fill(x, barY, x + 2, barY + barH, 0xFF5A5A62);
	}

	private Runnable right(GuiGraphics g, int mx, int my, boolean click, boolean rclick, Runnable action) {
		int rx = wx + ww - rightW, rw = rightW, cxr = rx + rw / 2;
		boolean wheel = section == Section.EMOTES;
		String label;
		if (wheel) {
			List<List<String>> pages = EmoteWheelScreen.layout();
			wheelPage = Math.min(wheelPage, pages.size());
			label = wheelPage >= pages.size() ? "New wheel" : "Wheel #" + (wheelPage + 1);
		} else {
			SurgeConfig.Outfit o = SurgeConfig.get().outfits[outfitIdx];
			label = "Outfit " + (outfitIdx + 1) + (o == null ? " (empty)" : "");
		}
		int lw = Math.min(rw - 70, font.width(label) + 16);
		g.drawString(font, label, cxr - font.width(label) / 2, wy + 9, Ui.TEXT, false);
		boolean prev = arrow(g, mx, my, cxr - lw / 2 - 18, wy + 5, true), next = arrow(g, mx, my, cxr + lw / 2 + 3, wy + 5, false);
		if (wheel) {
			int pages = EmoteWheelScreen.layout().size();
			if (prev && click) action = () -> wheelPage = (wheelPage + pages) % (pages + 1);
			if (next && click) action = () -> wheelPage = (wheelPage + 1) % (pages + 1);
		} else {
			if (prev && click) action = () -> wearOutfit((outfitIdx + 2) % 3);
			if (next && click) action = () -> wearOutfit((outfitIdx + 1) % 3);
			int sx = rx + rw - 21;
			boolean sh = Ui.hover(mx, my, sx, wy + 5, 15, 15);
			g.fill(sx, wy + 5, sx + 15, wy + 20, sh ? 0xFF4A4A52 : 0xFF38383E);
			Ui.centered(g, font, "+", sx + 8, wy + 9, 0xFFFFFFFF);
			if (sh && click) action = this::saveOutfit;
			if (sh) Ui.tooltip(g, font, "Save what you're wearing as outfit " + (outfitIdx + 1), mx, my, this.width, this.height);
		}
		g.fill(rx, wy + 24, rx + rw, wy + 25, 0xFF141416);

		if (wheel) return wheelPanel(g, mx, my, click, rclick, rx, rw, action);

		applyPreview(hoveredItem);
		int top = wy + 30, bottom = wy + wh - 30;
		if (minecraft.player != null) Preview.draw(g, rx + 4, top, rx + rw - 4, bottom, Math.min(70, (bottom - top) * 34 / 100));
		else Ui.centered(g, font, "Join a world to see yourself", cxr, (top + bottom) / 2, Ui.FAINT);
		int bw = 38, by = wy + wh - 24;
		boolean back = Preview.forced != null && Preview.forced == 180f;
		if (toggleButton(g, mx, my, cxr - bw, by, bw, "Front", !back) && click) action = () -> Preview.forced = 0f;
		if (toggleButton(g, mx, my, cxr, by, bw, "Back", back) && click) action = () -> Preview.forced = 180f;
		return action;
	}

	private boolean arrow(GuiGraphics g, int mx, int my, int x, int y, boolean leftArrow) {
		boolean hov = Ui.hover(mx, my, x, y, 15, 15);
		g.fill(x, y, x + 15, y + 15, hov ? 0xFF3A3A40 : 0xFF2C2C30);
		Ui.centered(g, font, leftArrow ? "<" : ">", x + 8, y + 4, Ui.TEXT);
		return hov;
	}

	private boolean toggleButton(GuiGraphics g, int mx, int my, int x, int y, int w, String label, boolean on) {
		boolean hov = Ui.hover(mx, my, x, y, w, 16);
		g.fill(x, y, x + w, y + 16, on ? 0xFF4A4A52 : hov ? 0xFF34343A : 0xFF2A2A2E);
		Ui.centered(g, font, label, x + w / 2, y + 4, on ? Ui.TEXT : Ui.MUTED);
		return hov;
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

	private Runnable wheelPanel(GuiGraphics g, int mx, int my, boolean click, boolean rclick, int rx, int rw, Runnable action) {
		Cosmetics.preview(null, null);
		List<List<String>> pages = EmoteWheelScreen.layout();
		boolean fresh = wheelPage >= pages.size();
		List<String> slots = fresh ? List.of("", "", "", "", "", "", "", "") : pages.get(wheelPage);
		int avail = Math.min(rw - 16, wh - 70);
		int cell = (avail - 8) / 3, gx = rx + (rw - (cell * 3 + 8)) / 2, gy = wy + 34;
		for (int i = 0; i < 8; i++) {
			int x = gx + WHEEL_COL[i] * (cell + 4), y = gy + WHEEL_ROW[i] * (cell + 4);
			boolean hov = Ui.hover(mx, my, x, y, cell, cell);
			Emotes.Emote e = i < slots.size() ? Emotes.Emote.byName(slots.get(i)) : null;
			if (e != null && !Emotes.owned(e)) e = null;
			g.fill(x, y, x + cell, y + cell, hov ? 0xFF323237 : 0xFF29292D);
			if (selectedEmote != null && hov) Ui.outline(g, x - 1, y - 1, cell + 2, cell + 2, 0xFFE4E4E7);
			if (e != null) {
				float t = (System.currentTimeMillis() - openedAt) / 1000f + i * 0.5f;
				if (!e.loops) t = t % (e.seconds + 0.6f);
				Preview.drawCard(g, x, y + 2, x + cell, y + cell - 2, Math.round(cell * 0.33f), 18f, new CapturedLook(null, null, null, e, t, true));
			} else Ui.centered(g, font, "+", x + cell / 2, y + cell / 2 - 4, 0xFF4A4A52);
			g.drawString(font, String.valueOf(i + 1), x + 3, y + 3, 0x60FFFFFF, false);
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
		int cxg = gx + cell + 4;
		int cyg = gy + cell + 4;
		g.fill(cxg, cyg, cxg + cell, cyg + cell, 0xFF202023);
		String[] hint = selectedEmote != null ? new String[]{"Pick a", "slot"} : new String[]{"Pick an", "emote"};
		Ui.centered(g, font, hint[0], cxg + cell / 2, cyg + cell / 2 - 9, Ui.FAINT);
		Ui.centered(g, font, hint[1], cxg + cell / 2, cyg + cell / 2 + 1, Ui.FAINT);
		int infoY = gy + cell * 3 + 14;
		String info = selectedEmote != null ? "Placing " + selectedEmote.label : "Right click a slot to empty it";
		Ui.centered(g, font, Ui.fit(font, info, rw - 10), rx + rw / 2, infoY, selectedEmote != null ? Ui.ACCENT_2 : Ui.FAINT);
		return action;
	}

	private Runnable outfits(GuiGraphics g, int mx, int my, boolean click, boolean rclick, Runnable action) {
		SurgeConfig c = SurgeConfig.get();
		int pad = 6, cols = mw >= 200 ? 3 : 2;
		int cardW = (mw - pad * 2 - (cols - 1) * pad) / cols, imgH = cardW;
		contentH = 0;
		for (int i = 0; i < 3; i++) {
			int x = mx0 + pad + (i % cols) * (cardW + pad), y = gridTop + pad + (i / cols) * (imgH + 13 + pad);
			SurgeConfig.Outfit o = c.outfits[i];
			boolean hov = Ui.hover(mx, my, x, y, cardW, imgH + 13);
			g.fill(x, y, x + cardW, y + imgH, hov ? 0xFF323237 : 0xFF29292D);
			if (o != null) {
				CapturedLook look = new CapturedLook(Cosmetics.equipped("cape", o.cape).equals("none") ? null : o.cape, Cosmetics.equipped("wings", o.wings).equals("none") ? null : o.wings,
					Cosmetics.equipped("halo", o.halo).equals("none") ? null : o.halo, null, 0f, false);
				Preview.drawCard(g, x, y + 2, x + cardW, y + imgH - 2, Math.round(cardW * 0.36f), 160f, look);
			} else Ui.centered(g, font, "Empty", x + cardW / 2, y + imgH / 2 - 4, Ui.FAINT);
			if (i == outfitIdx) Ui.outline(g, x - 1, y - 1, cardW + 2, imgH + 2, 0xFFE4E4E7);
			g.fill(x, y + imgH - 2, x + cardW, y + imgH, o != null ? 0xFF3FAF4F : 0xFF3A3A40);
			g.drawString(font, "Outfit " + (i + 1), x, y + imgH + 3, Ui.TEXT, false);
			final int idx = i;
			if (hov && click) action = () -> { if (c.outfits[idx] == null) { outfitIdx = idx; saveOutfit(); } else { wearOutfit(idx); toast("Wearing outfit " + (idx + 1), 0xFF8CFFA0); } };
			if (hov && rclick) {
				List<String> l = new ArrayList<>(List.of("Wear", "Save what I'm wearing here"));
				List<Runnable> a = new ArrayList<>(List.of(() -> wearOutfit(idx), () -> { outfitIdx = idx; saveOutfit(); }));
				if (o != null) { l.add("Clear"); a.add(() -> { c.outfits[idx] = null; SurgeConfig.save(); }); }
				openMenu(mx, my, l, a);
			}
		}
		g.drawString(font, Ui.fit(font, "Click an empty outfit to save what you wear.", mw - 12), mx0 + 6, wy + wh - 14, Ui.FAINT, false);
		return action;
	}

	private Runnable skinsPage(GuiGraphics g, int mx, int my, boolean click, Runnable action) {
		int bx = mx0 + 6, top = gridTop + 6, cw = mw - 12;
		if (Ui.pill(g, font, mx, my, bx, top, 74, 16, "Open folder", false) && click) action = SkinUploader::openFolder;
		if (Ui.pill(g, font, mx, my, bx + 78, top, 54, 16, slim ? "Slim" : "Classic", false) && click) action = () -> slim = !slim;
		if (Ui.pill(g, font, mx, my, bx + 136, top, 54, 16, "Refresh", false) && click) action = () -> skins = SkinUploader.list();
		int y0 = top + 24;
		if (skins.isEmpty()) {
			for (String line : wrap("Put .png skins in the folder, then press Refresh.", cw)) { g.drawString(font, line, bx, y0 + 4, Ui.MUTED, false); y0 += 10; }
			return action;
		}
		for (int i = 0; i < skins.size() && y0 + i * 22 < wy + wh - 22; i++) {
			Path p = skins.get(i);
			String name = p.getFileName().toString().replaceAll("(?i)\\.png$", "");
			int y = y0 + i * 22;
			boolean hov = Ui.hover(mx, my, bx, y, cw, 20);
			g.fill(bx, y, bx + cw, y + 20, hov ? 0xFF323237 : 0xFF29292D);
			g.drawString(font, Ui.fit(font, name, cw - 70), bx + 6, y + 6, Ui.TEXT, false);
			if (Ui.pill(g, font, mx, my, bx + cw - 54, y + 2, 50, 16, "Apply", true) && click && hov) action = () -> {
				toast("Uploading…", Ui.MUTED);
				SkinUploader.upload(p, slim).thenAccept(err -> minecraft.execute(() -> toast(err == null ? "Skin applied. Rejoin to see it." : err, err == null ? 0xFF8CFFA0 : 0xFFFF8C8C)));
			};
		}
		return action;
	}

	private Runnable coinsPage(GuiGraphics g, int mx, int my, boolean click, float delta, Runnable action) {
		int x = mx0 + 10, y = gridTop + 10, cw = mw - 20;
		Ui.coin(g, x, y + 2, 32);
		g.drawString(font, Component.literal(String.format("%,d", Wallet.get().coins)).withStyle(ChatFormatting.BOLD), x + 40, y + 8, Ui.TEXT, true);
		g.drawString(font, "Surge Coins", x + 40, y + 20, Ui.MUTED, false);
		g.drawString(font, "Redeem a code", x, y + 54, Ui.TEXT, false);
		int bw = Math.max(60, cw - 70);
		g.fill(x, y + 66, x + bw, y + 86, 0xFF141416);
		Ui.outline(g, x, y + 66, bw, 20, codeBox != null && codeBox.isFocused() ? Ui.ACCENT_2 : 0xFF34343A);
		if (codeBox != null) {
			codeBox.setX(x + 6);
			codeBox.setY(y + 72);
			codeBox.setWidth(bw - 12);
			codeBox.render(g, mx, my, delta);
			if (GLFW.glfwGetKey(minecraft.getWindow().handle(), GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS && codeBox.isFocused() && !redeeming) action = this::redeem;
		}
		if (Ui.pill(g, font, mx, my, x + bw + 6, y + 66, 60, 20, redeeming ? "…" : "Redeem", true) && click) action = this::redeem;
		if (!message.isEmpty() && System.currentTimeMillis() - messageAt < 8000) {
			List<String> lines = wrap(message, cw);
			for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x, y + 94 + i * 10, messageColor, false);
		}
		g.drawString(font, "Codes can give coins, cosmetics or emotes.", x, wy + wh - 26, Ui.FAINT, false);
		g.drawString(font, "Spaces, dashes and capitals don't matter.", x, wy + wh - 15, Ui.FAINT, false);
		return action;
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
