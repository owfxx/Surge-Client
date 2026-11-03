package gg.surge.client.gui;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.cosmetics.Cosmetics;
import gg.surge.client.cosmetics.Preview;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WardrobeScreen extends Screen {
	public enum Section {
		COSMETICS("Cosmetics", Items.ELYTRA), EMOTES("Emotes", Items.ARMOR_STAND), OUTFITS("Outfits", Items.LEATHER_CHESTPLATE),
		SKINS("Skins", Items.PLAYER_HEAD), CURRENCY("Coins", Items.GOLD_NUGGET);
		final String label;
		final Item icon;
		ItemStack stack;

		Section(String label, Item icon) {
			this.label = label;
			this.icon = icon;
		}

		ItemStack stack() {
			if (stack == null) stack = new ItemStack(icon);
			return stack;
		}
	}

	private static final Map<String, ItemStack> ICONS = new HashMap<>();
	private static Section section = Section.COSMETICS;
	private static boolean slim = false;
	private static final int SIDEBAR_W = 96, CARD_H = 52, GAP = 6;

	private final Screen parent;
	private EditBox codeBox;
	private String pendingBuy;
	private long pendingUntil;
	private String message = "";
	private int messageColor = Ui.ACCENT_2;
	private long messageAt;
	private boolean wasDown = true, redeeming;
	private int px, py, pw, ph, cx, cy, cw, ch;
	private float scroll, scrollTarget;
	private int contentH;
	private long openedAt;
	private Shop.Item hoveredItem;
	private List<Path> skins = new ArrayList<>();

	public WardrobeScreen(Screen parent) {
		super(Component.literal("Wardrobe"));
		this.parent = parent;
	}

	public static WardrobeScreen open(Screen parent, Section s) {
		section = s;
		return new WardrobeScreen(parent);
	}

	private static ItemStack iconFor(Shop.Item item) {
		String prefix = item.id().substring(0, item.id().indexOf(':'));
		return ICONS.computeIfAbsent(prefix, p -> new ItemStack(switch (p) {
			case "wings" -> Items.ELYTRA;
			case "halo" -> Items.GOLD_INGOT;
			case "cape" -> Items.WHITE_BANNER;
			default -> Items.ARMOR_STAND;
		}));
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, Colors.alpha(0x90000000, openProgress()));
	}

	private float openProgress() {
		float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f);
		return 1f - (1f - t) * (1f - t) * (1f - t);
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	private boolean preview() {
		return minecraft != null && minecraft.player != null && pw >= 360;
	}

	@Override
	protected void init() {
		if (openedAt == 0) openedAt = System.currentTimeMillis();
		pw = Math.min(500, this.width - 16);
		ph = Math.min(290, this.height - 16);
		px = (this.width - pw) / 2;
		py = (this.height - ph) / 2;
		cx = px + SIDEBAR_W + 8;
		cy = py + 36;
		cw = px + pw - 10 - cx - (preview() ? 128 : 0);
		ch = py + ph - 10 - cy;
		clearWidgets();
		codeBox = null;
		if (section == Section.CURRENCY) {
			codeBox = new EditBox(font, cx + 10, cy + 70, Math.min(180, cw - 90), 12, Component.literal("Code"));
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
		init();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0) {
			scrollTarget = Math.max(0, Math.min(Math.max(0, contentH - ch), scrollTarget - (float) scrollY * 28f));
			return true;
		}
		return false;
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

	private void buyFlow(Shop.Item item) {
		Wallet w = Wallet.get();
		int price = Shop.price(item);
		if (w.coins < price) { toast("You need " + (price - w.coins) + " more coins", 0xFFFF8C8C); return; }
		if (item.id().equals(pendingBuy) && System.currentTimeMillis() < pendingUntil) {
			w.buy(item);
			pendingBuy = null;
			toast("Unlocked " + item.name(), 0xFF8CFFA0);
			if (item.category().equals("cosmetic")) setEquipped(typeOf(item), item.key());
		} else {
			pendingBuy = item.id();
			pendingUntil = System.currentTimeMillis() + 3000;
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

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		Ui.frame();
		boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasDown;
		wasDown = down;
		Runnable action = null;
		hoveredItem = null;
		String tip = null;

		float k = openProgress();
		if (k < 1f) {
			float s = 0.95f + 0.05f * k;
			g.pose().pushMatrix();
			g.pose().translate(this.width / 2f, this.height / 2f);
			g.pose().scale(s, s);
			g.pose().translate(-this.width / 2f, -this.height / 2f);
		}

		Ui.shadow(g, px, py, pw, ph);
		Ui.box(g, px, py, pw, ph, Ui.BG, Ui.EDGE);
		g.fill(px + 1, py + 1, px + SIDEBAR_W, py + ph - 1, Ui.SIDEBAR);
		g.fill(px + SIDEBAR_W, py + 1, px + SIDEBAR_W + 1, py + ph - 1, Ui.EDGE);
		Ui.logo(g, px + 9, py + 8, 16);
		g.drawString(font, Component.literal("Wardrobe").withStyle(ChatFormatting.BOLD), px + 30, py + 12, Ui.TEXT, true);

		String bal = String.valueOf(Wallet.get().coins);
		int bw = font.width(bal) + 28, bx = px + pw - bw - 30;
		boolean balHover = Ui.hover(mx, my, bx, py + 7, bw, 18);
		float ba = Ui.anim("ward:coins", balHover);
		Ui.box(g, bx, py + 7, bw, 18, Colors.lerp(0xFF1C1C1F, 0xFF2C2C31, ba), Colors.lerp(Ui.EDGE, Ui.YELLOW, ba));
		Ui.item(g, new ItemStack(Items.GOLD_NUGGET), bx + 3, py + 8, 16);
		g.drawString(font, bal, bx + 22, py + 12, Ui.YELLOW, true);
		if (balHover && click) action = () -> switchTo(Section.CURRENCY);
		if (Ui.iconButton(g, mx, my, px + pw - 26, py + 7, 18, Ui.CROSS) && click) action = this::onClose;

		int sy = py + 36;
		for (Section s : Section.values()) {
			boolean sel = s == section;
			if (Ui.flat(g, mx, my, px + 4, sy, SIDEBAR_W - 8, 20, sel, "ward:" + s) && click && !sel) action = () -> switchTo(s);
			Ui.item(g, s.stack(), px + 9, sy + 3, 14);
			g.drawString(font, s.label, px + 28, sy + 6, sel ? Ui.TEXT : Ui.MUTED, false);
			sy += 22;
		}

		scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));

		switch (section) {
			case COSMETICS, EMOTES -> {
				List<Shop.Item> items = Shop.bySub(section == Section.COSMETICS ? "cosmetic" : "emote", null);
				int cols = Math.max(2, (cw + GAP) / 104);
				int cardW = (cw - (cols - 1) * GAP) / cols;
				contentH = ((items.size() + cols - 1) / cols) * (CARD_H + GAP);
				scrollTarget = Math.max(0, Math.min(Math.max(0, contentH - ch), scrollTarget));
				g.enableScissor(cx - 2, cy, cx + cw + 2, cy + ch);
				boolean inView = Ui.hover(mx, my, cx, cy, cw, ch);
				for (int i = 0; i < items.size(); i++) {
					Shop.Item item = items.get(i);
					int x = cx + (i % cols) * (cardW + GAP);
					int y = cy + (i / cols) * (CARD_H + GAP) - Math.round(scroll);
					if (y + CARD_H < cy || y > cy + ch) continue;
					boolean hov = inView && Ui.hover(mx, my, x, y, cardW, CARD_H);
					Runnable a = card(g, item, x, y, cardW, hov);
					if (hov) hoveredItem = item;
					if (hov && click) action = a;
				}
				g.disableScissor();
				scrollbar(g);
			}
			case OUTFITS -> action = outfits(g, mx, my, click, action);
			case SKINS -> action = skinsPage(g, mx, my, click, action);
			case CURRENCY -> action = coinsPage(g, mx, my, click, delta, action);
		}

		applyPreview(hoveredItem);
		if (preview()) {
			int x1 = px + pw - 128, y1 = py + 36, x2 = px + pw - 10, y2 = py + ph - 10;
			Ui.box(g, x1, y1, x2 - x1, y2 - y1, 0xFF18181B, Ui.EDGE);
			g.fillGradient(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0x00000000, 0x508F50F2);
			Preview.draw(g, x1 + 1, y1 + 4, x2 - 1, y2 - 18, 60);
			String hint = hoveredItem != null ? hoveredItem.name() : "Hover something to try it on";
			int hc = hoveredItem != null ? Ui.TEXT : Ui.FAINT;
			Ui.centered(g, font, Ui.fit(font, hint, x2 - x1 - 8), (x1 + x2) / 2, y2 - 13, hc);
		}

		long age = System.currentTimeMillis() - messageAt;
		if (!message.isEmpty() && age < 4500 && section != Section.CURRENCY) {
			float a = Math.min(1f, Math.min(age / 150f, (4500 - age) / 400f));
			int mw = font.width(message) + 12;
			int mxp = cx + (cw - mw) / 2, myp = py + ph - 30;
			Ui.box(g, mxp, myp, mw, 16, Colors.alpha(0xF0141418, a), Colors.alpha(messageColor, a));
			g.drawString(font, message, mxp + 6, myp + 4, Colors.alpha(messageColor, a), false);
		}

		if (k < 1f) g.pose().popMatrix();
		if (action != null) { Ui.click(); action.run(); }
	}

	private Runnable card(GuiGraphics g, Shop.Item item, int x, int y, int w, boolean hov) {
		Wallet wal = Wallet.get();
		boolean owned = wal.owns(item.id());
		boolean isEmote = item.category().equals("emote");
		boolean equipped = !isEmote && owned && item.key().equals(equippedKey(typeOf(item)));
		boolean asking = item.id().equals(pendingBuy) && System.currentTimeMillis() < pendingUntil;
		float a = Ui.anim("wcard:" + item.id(), hov);
		float eq = Ui.anim("weq:" + item.id(), equipped);
		int yy = y - Math.round(a);
		Ui.chunky(g, x, yy, w, CARD_H, Colors.lerp(Ui.SURFACE, Ui.SURFACE_HOVER, a), 0xFF26262A);
		if (eq > 0.01f) Ui.outline(g, x, yy, w, CARD_H, Colors.alpha(0xFF6BDB5A, eq));
		else if (a > 0.02f) Ui.outline(g, x, yy, w, CARD_H, Colors.alpha(0xFFFFFFFF, a * 0.85f));
		Ui.item(g, iconFor(item), x + 6, yy + 6, 16);
		g.drawString(font, Ui.fit(font, item.name(), w - 30), x + 26, yy + 11, Ui.TEXT, true);

		int sale = Sales.discount(item);
		if (sale > 0 && !owned) {
			String tag = "-" + sale + "%";
			int tw = font.width(tag) + 6;
			g.fill(x + w - tw - 3, yy + 3, x + w - 3, yy + 13, 0xFFE0A21A);
			g.drawString(font, tag, x + w - tw, yy + 4, 0xFF1A1200, false);
		}

		int bx = x + 4, by = yy + CARD_H - 19, bw = w - 8, bh = 15;
		int fill, lip;
		String label;
		boolean coin = false;
		if (owned) {
			if (isEmote) { label = "Play"; fill = Ui.ACCENT; lip = Ui.ACCENT_DIM; }
			else if (equipped) { label = "Equipped"; fill = Ui.ON; lip = Ui.ON_LIP; }
			else { label = "Equip"; fill = Ui.BUTTON; lip = Ui.BUTTON_LIP; }
		} else if (asking) {
			label = "Buy for " + Shop.price(item) + "?";
			fill = 0xFFE0A21A;
			lip = 0xFF7A5608;
		} else {
			label = String.valueOf(Shop.price(item));
			coin = true;
			boolean afford = wal.coins >= Shop.price(item);
			fill = afford ? Ui.ACCENT : Ui.NEUTRAL;
			lip = afford ? Ui.ACCENT_DIM : Ui.BUTTON_LIP;
		}
		Ui.chunky(g, bx, by, bw, bh, fill, lip);
		int tw = font.width(label) + (coin ? 11 : 0);
		int tx = bx + (bw - tw) / 2;
		if (coin) { Ui.icon(g, Ui.COIN, tx, by + 3, 1, Ui.YELLOW); tx += 11; }
		g.drawString(font, label, tx, by + 3, Ui.TEXT, true);
		if (sale > 0 && !owned && !asking) {
			String old = String.valueOf(item.price());
			int ox = bx + bw - font.width(old) - 4;
			g.drawString(font, old, ox, by + 3, 0xA0FFFFFF, false);
			g.fill(ox - 1, by + 7, ox + font.width(old) + 1, by + 8, 0xC0FFFFFF);
		}

		return () -> {
			if (!owned) { buyFlow(item); return; }
			if (isEmote) {
				Emotes.Emote e = Emotes.Emote.byName(item.key());
				if (minecraft.player == null || e == null) { toast("Join a world to play emotes", Ui.MUTED); return; }
				minecraft.setScreen(null);
				Emotes.play(minecraft, e);
				return;
			}
			setEquipped(typeOf(item), equipped ? "none" : item.key());
		};
	}

	private void scrollbar(GuiGraphics g) {
		if (contentH <= ch) return;
		int x = cx + cw + 3;
		g.fill(x, cy, x + 2, cy + ch, 0x30FFFFFF);
		int barH = Math.max(16, ch * ch / contentH);
		int barY = cy + Math.round((ch - barH) * (scroll / Math.max(1, contentH - ch)));
		g.fill(x, barY, x + 2, barY + barH, Ui.ACCENT_2);
	}

	private static String nameOf(String type, String key) {
		if (key == null || "none".equals(key)) return "None";
		Shop.Item i = Shop.byId(type + ":" + key);
		return i == null ? key : i.name();
	}

	private Runnable outfits(GuiGraphics g, int mx, int my, boolean click, Runnable action) {
		SurgeConfig c = SurgeConfig.get();
		int rowH = 58;
		for (int i = 0; i < 3; i++) {
			int y = cy + i * (rowH + GAP);
			SurgeConfig.Outfit o = c.outfits[i];
			boolean hov = Ui.hover(mx, my, cx, y, cw, rowH);
			float a = Ui.anim("outfit:" + i, hov);
			Ui.chunky(g, cx, y, cw, rowH, Colors.lerp(Ui.SURFACE, Ui.SURFACE_HOVER, a), 0xFF26262A);
			g.drawString(font, Component.literal("Outfit " + (i + 1)).withStyle(ChatFormatting.BOLD), cx + 8, y + 7, Ui.TEXT, true);
			if (o == null) g.drawString(font, "Empty. Save what you're wearing now.", cx + 8, y + 21, Ui.FAINT, false);
			else {
				g.drawString(font, Ui.fit(font, "Wings: " + nameOf("wings", o.wings) + "   Halo: " + nameOf("halo", o.halo), cw - 16), cx + 8, y + 20, Ui.MUTED, false);
				g.drawString(font, Ui.fit(font, "Cape: " + nameOf("cape", o.cape), cw - 16), cx + 8, y + 31, Ui.MUTED, false);
			}
			final int slot = i;
			int bw = 70;
			if (Ui.pill(g, font, mx, my, cx + cw - bw * 2 - 12, y + rowH - 22, bw, 16, "Save", false) && click) action = () -> {
				SurgeConfig.Outfit n = new SurgeConfig.Outfit();
				n.wings = c.wings; n.halo = c.halo; n.cape = c.cape;
				c.outfits[slot] = n;
				SurgeConfig.save();
				toast("Saved outfit " + (slot + 1), 0xFF8CFFA0);
			};
			if (o != null && Ui.pill(g, font, mx, my, cx + cw - bw - 6, y + rowH - 22, bw, 16, "Wear", true) && click) action = () -> {
				c.wings = o.wings; c.halo = o.halo; c.cape = o.cape;
				SurgeConfig.save();
				toast("Wearing outfit " + (slot + 1), 0xFF8CFFA0);
			};
		}
		return action;
	}

	private Runnable skinsPage(GuiGraphics g, int mx, int my, boolean click, Runnable action) {
		int bx = cx;
		if (Ui.pill(g, font, mx, my, bx, cy, 80, 16, "Open folder", false) && click) action = SkinUploader::openFolder;
		if (Ui.pill(g, font, mx, my, bx + 84, cy, 60, 16, slim ? "Slim" : "Classic", false) && click) action = () -> slim = !slim;
		if (Ui.pill(g, font, mx, my, bx + 148, cy, 60, 16, "Refresh", false) && click) action = () -> skins = SkinUploader.list();
		int top = cy + 24;
		if (skins.isEmpty()) {
			g.drawString(font, "Put .png skins in the folder, then press Refresh.", cx, top + 4, Ui.MUTED, false);
			return action;
		}
		for (int i = 0; i < skins.size() && top + i * 24 < cy + ch - 20; i++) {
			Path p = skins.get(i);
			String name = p.getFileName().toString().replaceAll("(?i)\\.png$", "");
			int y = top + i * 24;
			boolean hov = Ui.hover(mx, my, cx, y, cw, 22);
			if (Ui.flat(g, mx, my, cx, y, cw, 22, false, "skin:" + name)) { }
			g.drawString(font, Ui.fit(font, name, cw - 90), cx + 6, y + 7, Ui.TEXT, false);
			if (Ui.pill(g, font, mx, my, cx + cw - 72, y + 3, 68, 16, "Apply", true) && click && hov) action = () -> {
				toast("Uploading…", Ui.MUTED);
				SkinUploader.upload(p, slim).thenAccept(err -> minecraft.execute(() -> toast(err == null ? "Skin applied. Rejoin to see it." : err, err == null ? 0xFF8CFFA0 : 0xFFFF8C8C)));
			};
		}
		return action;
	}

	private Runnable coinsPage(GuiGraphics g, int mx, int my, boolean click, float delta, Runnable action) {
		Ui.item(g, new ItemStack(Items.GOLD_NUGGET), cx, cy + 2, 32);
		g.drawString(font, Component.literal(String.valueOf(Wallet.get().coins)).withStyle(ChatFormatting.BOLD), cx + 40, cy + 8, Ui.YELLOW, true);
		g.drawString(font, "Surge Coins", cx + 40, cy + 20, Ui.MUTED, false);

		g.drawString(font, "Redeem a code", cx, cy + 54, Ui.ACCENT_2, false);
		int bw = Math.min(190, cw - 86);
		Ui.box(g, cx, cy + 64, bw, 22, 0xFF161619, codeBox != null && codeBox.isFocused() ? Ui.ACCENT : Ui.EDGE);
		if (codeBox != null) {
			codeBox.setX(cx + 7);
			codeBox.setY(cy + 71);
			codeBox.setWidth(bw - 14);
			codeBox.render(g, mx, my, delta);
			if (GLFW.glfwGetKey(minecraft.getWindow().handle(), GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS && codeBox.isFocused() && !redeeming) action = this::redeem;
		}
		if (Ui.pill(g, font, mx, my, cx + bw + 6, cy + 64, 72, 22, redeeming ? "…" : "Redeem", true) && click) action = this::redeem;
		if (!message.isEmpty() && System.currentTimeMillis() - messageAt < 8000) {
			List<String> lines = wrap(message, cw);
			for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), cx, cy + 94 + i * 10, messageColor, false);
		}
		g.drawString(font, "Codes can give coins, cosmetics or emotes.", cx, cy + ch - 24, Ui.FAINT, false);
		g.drawString(font, "Spaces, dashes and capitals don't matter.", cx, cy + ch - 13, Ui.FAINT, false);
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
		if (item == null) {
			Cosmetics.preview(null, null);
			Emotes.setPreview(null);
			return;
		}
		if (item.category().equals("emote")) {
			Emotes.setPreview(Emotes.Emote.byName(item.key()));
			Cosmetics.preview(null, null);
		} else {
			Cosmetics.preview(typeOf(item), item.key());
			Emotes.setPreview(null);
		}
	}

	@Override
	public void removed() {
		applyPreview(null);
		super.removed();
	}

	@Override
	public void onClose() {
		SurgeConfig.save();
		minecraft.setScreen(parent);
	}
}
