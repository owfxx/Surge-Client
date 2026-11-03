package gg.surge.client.gui;

import gg.surge.client.economy.AdminCodes;
import gg.surge.client.economy.Sales;
import gg.surge.client.economy.Shop;
import gg.surge.client.mods.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AdminScreen extends Screen {
	public static final String OWNER = "chroynic";
	private static final int[] AMOUNTS = {600, 1000, 1500, 2500, 5000, 10000};
	private static final int[] DURATIONS_H = {0, 24, 72, 168};
	private static final String[] DURATION_NAMES = {"No end", "1 day", "3 days", "1 week"};

	private final Screen parent;
	private int amountIdx = 1, count = 1, percent = 20, durationIdx = 1, targetIdx;
	private final List<String> targets = new ArrayList<>();
	private final List<String> targetNames = new ArrayList<>();
	private boolean wasDown = true;
	private String message = "";
	private long messageUntil;
	private int px, py, pw, ph;
	private net.minecraft.client.gui.components.EditBox customBox;
	private int rewardIdx;
	private final List<String> rewards = new ArrayList<>();
	private boolean wasEnter;

	public AdminScreen(Screen parent) {
		super(Component.literal("Admin"));
		this.parent = parent;
		String[][] groups = {{"all", "Everything"}, {"cosmetic", "All cosmetics"}, {"emote", "All emotes"},
			{"sub:wings", "All wings"}, {"sub:halo", "All halos"}, {"sub:cape", "All capes"}};
		for (String[] gr : groups) { targets.add(gr[0]); targetNames.add(gr[1]); }
		for (Shop.Item i : Shop.ITEMS) { targets.add(i.id()); targetNames.add(i.name()); }
	}

	public static boolean allowed(net.minecraft.client.Minecraft mc) {
		return mc.getUser() != null && OWNER.equalsIgnoreCase(mc.getUser().getName());
	}

	@Override
	protected void init() {
		pw = Math.min(440, this.width - 16);
		ph = Math.min(300, this.height - 16);
		px = (this.width - pw) / 2;
		py = (this.height - ph) / 2;
		clearWidgets();
		String keep = customBox == null ? "" : customBox.getValue();
		customBox = new net.minecraft.client.gui.components.EditBox(font, 0, 0, 100, 10, Component.literal("Custom code"));
		customBox.setBordered(false);
		customBox.setMaxLength(24);
		customBox.setValue(keep);
		customBox.setHint(Component.literal("Random").withStyle(ChatFormatting.DARK_GRAY));
		addWidget(customBox);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, 0xA0000000);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void toast(String s) {
		message = s;
		messageUntil = System.currentTimeMillis() + 3500;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		Ui.frame();
		boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasDown;
		wasDown = down;
		Runnable action = null;

		Ui.shadow(g, px, py, pw, ph);
		Ui.box(g, px, py, pw, ph, Ui.BG, Ui.EDGE);
		Ui.logo(g, px + 9, py + 7, 16);
		g.drawString(font, Component.literal("Admin").withStyle(ChatFormatting.BOLD), px + 30, py + 11, Ui.TEXT, true);
		if (Ui.iconButton(g, mx, my, px + pw - 26, py + 6, 18, Ui.CROSS) && click) action = this::onClose;
		g.fill(px + 1, py + 30, px + pw - 1, py + 31, Ui.EDGE);

		int colW = (pw - 30) / 2;
		int lx = px + 10, rx = px + 20 + colW, top = py + 40;

		g.drawString(font, "Coin codes", lx, top, Ui.ACCENT_2, false);
		g.drawString(font, "Coins per code", lx, top + 16, Ui.MUTED, false);
		int abw = (colW - 10) / 3;
		for (int i = 0; i < AMOUNTS.length; i++) {
			int bx = lx + (i % 3) * (abw + 5), by = top + 28 + (i / 3) * 20;
			final int idx = i;
			if (Ui.pill(g, font, mx, my, bx, by, abw, 16, String.valueOf(AMOUNTS[i]), i == amountIdx) && click) action = () -> amountIdx = idx;
		}
		g.drawString(font, "Code", lx, top + 72, Ui.MUTED, false);
		int cbx = lx + 36, cbw = colW - 36;
		Ui.box(g, cbx, top + 68, cbw, 16, 0xFF161619, customBox.isFocused() ? Ui.ACCENT : Ui.EDGE);
		customBox.setX(cbx + 5);
		customBox.setY(top + 72);
		customBox.setWidth(cbw - 10);
		customBox.render(g, mx, my, delta);
		boolean custom = !customBox.getValue().isBlank();
		if (click) {
			boolean over = Ui.hover(mx, my, cbx, top + 68, cbw, 16);
			customBox.setFocused(over);
			setFocused(over ? customBox : null);
		}
		g.drawString(font, "Unlocks", lx, top + 92, Ui.MUTED, false);
		int rbx = lx + 46, rbw = colW - 46 - 34;
		boolean rHov = Ui.hover(mx, my, rbx, top + 88, rbw, 16);
		Ui.chunky(g, rbx, top + 88, rbw, 16, rHov ? Ui.BUTTON_HOVER : Ui.BUTTON, Ui.BUTTON_LIP);
		String rname = Ui.fit(font, Shop.ITEMS.get(rewardIdx).name(), rbw - 20);
		g.drawString(font, "<", rbx + 4, top + 92, Ui.FAINT, false);
		g.drawString(font, ">", rbx + rbw - 9, top + 92, Ui.FAINT, false);
		g.drawString(font, rname, rbx + (rbw - font.width(rname)) / 2, top + 92, Ui.TEXT, true);
		if (rHov && click) { boolean left = mx < rbx + rbw / 2; action = () -> rewardIdx = (rewardIdx + (left ? -1 : 1) + Shop.ITEMS.size()) % Shop.ITEMS.size(); }
		if (Ui.pill(g, font, mx, my, lx + colW - 30, top + 88, 30, 16, "Add", false) && click) action = () -> {
			String id = Shop.ITEMS.get(rewardIdx).id();
			if (!rewards.contains(id)) rewards.add(id);
		};
		String gives = rewards.isEmpty() ? "Only coins" : String.join(", ", rewards.stream().map(id -> Shop.byId(id).name()).toList());
		g.drawString(font, Ui.fit(font, gives, colW - 14), lx, top + 108, rewards.isEmpty() ? Ui.FAINT : Ui.ACCENT_2, false);
		if (!rewards.isEmpty()) {
			boolean clr = Ui.hover(mx, my, lx + colW - 10, top + 107, 10, 10);
			Ui.icon(g, Ui.CROSS, lx + colW - 9, top + 108, 1, clr ? 0xFFFF6B6B : Ui.FAINT);
			if (clr && click) action = rewards::clear;
		}
		if (!custom) {
			if (Ui.pill(g, font, mx, my, lx, top + 120, 16, 16, "-", false) && click) action = () -> count = Math.max(1, count - 1);
			Ui.centered(g, font, "x" + count, lx + 30, top + 124, Ui.TEXT);
			if (Ui.pill(g, font, mx, my, lx + 44, top + 120, 16, 16, "+", false) && click) action = () -> count = Math.min(20, count + 1);
		}
		int crx = custom ? lx : lx + 64, crw = custom ? colW : colW - 64;
		if (Ui.pill(g, font, mx, my, crx, top + 120, crw, 16, custom ? "Create code" : "Create " + (count == 1 ? "code" : count + " codes"), true) && click) {
			action = () -> {
				List<String> made = AdminCodes.create(custom ? customBox.getValue() : null, AMOUNTS[amountIdx], new ArrayList<>(rewards), count);
				if (made.isEmpty()) { toast("That code already exists"); return; }
				minecraft.keyboardHandler.setClipboard(String.join("\n", made));
				toast(made.size() == 1 ? "Copied " + made.get(0) : "Copied " + made.size() + " codes");
				customBox.setValue("");
			};
		}
		g.drawString(font, "Latest (click to copy)", lx, top + 142, Ui.MUTED, false);
		List<AdminCodes.Code> codes = AdminCodes.list();
		int listY = top + 153;
		int maxRows = Math.max(0, (py + ph - 26 - listY) / 14);
		for (int i = 0; i < Math.min(maxRows, codes.size()); i++) {
			AdminCodes.Code c = codes.get(i);
			int y = listY + i * 14;
			boolean hov = Ui.hover(mx, my, lx, y, colW - 16, 13);
			if (hov) g.fill(lx, y, lx + colW - 16, y + 13, 0x30FFFFFF);
			g.drawString(font, c.code(), lx + 3, y + 3, hov ? Ui.TEXT : 0xFFD8D8DC, false);
			String coins = c.coins() + (c.items().isEmpty() ? "" : " +" + c.items().size());
			g.drawString(font, coins, lx + colW - 20 - font.width(coins), y + 3, Ui.YELLOW, false);
			if (hov && click) action = () -> { minecraft.keyboardHandler.setClipboard(c.code()); toast("Copied " + c.code()); };
			boolean delHov = Ui.hover(mx, my, lx + colW - 13, y + 2, 10, 10);
			Ui.icon(g, Ui.CROSS, lx + colW - 12, y + 3, 1, delHov ? 0xFFFF6B6B : Ui.FAINT);
			if (delHov && click) action = () -> { AdminCodes.remove(c.code()); toast("Deleted " + c.code()); };
		}

		g.drawString(font, "Sales", rx, top, Ui.ACCENT_2, false);
		g.drawString(font, "On", rx, top + 16, Ui.MUTED, false);
		int tbx = rx + 22, tbw = colW - 22;
		boolean tHov = Ui.hover(mx, my, tbx, top + 12, tbw, 16);
		Ui.chunky(g, tbx, top + 12, tbw, 16, tHov ? Ui.BUTTON_HOVER : Ui.BUTTON, Ui.BUTTON_LIP);
		g.drawString(font, "<", tbx + 4, top + 16, Ui.FAINT, false);
		g.drawString(font, ">", tbx + tbw - 9, top + 16, Ui.FAINT, false);
		String tname = Ui.fit(font, targetNames.get(targetIdx), tbw - 24);
		g.drawString(font, tname, tbx + (tbw - font.width(tname)) / 2, top + 16, Ui.TEXT, true);
		if (tHov && click) {
			boolean left = mx < tbx + tbw / 2;
			action = () -> targetIdx = (targetIdx + (left ? -1 : 1) + targets.size()) % targets.size();
		}
		g.drawString(font, "Discount", rx, top + 38, Ui.MUTED, false);
		int sx = rx + 52, sw = colW - 52 - 30;
		Ui.slider(g, sx, top + 36, sw, (percent - 5) / 85f, false);
		g.drawString(font, percent + "%", rx + colW - 24, top + 38, Ui.ACCENT_2, false);
		if (down && Ui.hover(mx, my, sx - 4, top + 33, sw + 8, 16)) percent = Math.max(5, Math.min(90, Math.round((5 + (mx - sx) / (float) sw * 85) / 5f) * 5));
		g.drawString(font, "Length", rx, top + 58, Ui.MUTED, false);
		int dbw = (colW - 52 - 9) / 4;
		for (int i = 0; i < DURATION_NAMES.length; i++) {
			final int idx = i;
			if (Ui.pill(g, font, mx, my, rx + 52 + i * (dbw + 3), top + 54, dbw, 16, DURATION_NAMES[i], i == durationIdx) && click) action = () -> durationIdx = idx;
		}
		if (Ui.pill(g, font, mx, my, rx, top + 76, colW, 18, "Start sale", true) && click) {
			action = () -> {
				Sales.Sale s = new Sales.Sale();
				s.target = targets.get(targetIdx);
				s.percent = percent;
				s.until = DURATIONS_H[durationIdx] == 0 ? 0 : System.currentTimeMillis() + DURATIONS_H[durationIdx] * 3600_000L;
				Sales.local().add(s);
				Sales.save();
				AdminCodes.publish();
				toast("Sale started: " + Sales.describe(s));
			};
		}
		g.drawString(font, "Running", rx, top + 104, Ui.MUTED, false);
		List<Sales.Sale> sales = Sales.local();
		sales.removeIf(s -> !s.active());
		int sy = top + 116;
		if (sales.isEmpty()) g.drawString(font, "No sales right now", rx + 3, sy + 3, Ui.FAINT, false);
		for (int i = 0; i < sales.size() && sy + i * 16 < py + ph - 30; i++) {
			Sales.Sale s = sales.get(i);
			int y = sy + i * 16;
			g.fill(rx, y, rx + colW, y + 14, 0x30FFFFFF);
			g.drawString(font, Ui.fit(font, Sales.describe(s), colW - 40), rx + 3, y + 3, Ui.TEXT, false);
			boolean endHov = Ui.hover(mx, my, rx + colW - 30, y, 30, 14);
			g.drawString(font, "End", rx + colW - 22, y + 3, endHov ? 0xFFFF6B6B : Ui.MUTED, false);
			if (endHov && click) action = () -> { Sales.local().remove(s); Sales.save(); AdminCodes.publish(); toast("Sale ended"); };
		}

		String foot = System.currentTimeMillis() < messageUntil ? message : "Upload codes.json from your Surge folder to your codes URL to share codes and sales.";
		g.drawString(font, Ui.fit(font, foot, pw - 20), px + 10, py + ph - 15, System.currentTimeMillis() < messageUntil ? Ui.ACCENT_2 : Ui.FAINT, false);
		g.fill(px + 1, py + ph - 20, px + pw - 1, py + ph - 19, Colors.alpha(Ui.EDGE, 0.8f));

		if (action != null) { Ui.click(); action.run(); }
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return customBox == null || !customBox.isFocused();
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
