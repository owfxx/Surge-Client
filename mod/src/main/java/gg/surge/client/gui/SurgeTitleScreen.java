package gg.surge.client.gui;

import gg.surge.client.economy.Wallet;
import gg.surge.client.mods.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class SurgeTitleScreen extends Screen {
	private static final ItemStack COIN = Ui.COIN_ITEM;
	private static final ItemStack SHIRT = new ItemStack(Items.LEATHER_CHESTPLATE);
	private static final ItemStack GEAR = new ItemStack(Items.REPEATER);
	private static final ItemStack SOCIAL = new ItemStack(Items.WRITABLE_BOOK);
	private static final ItemStack MODS = new ItemStack(Items.COMPARATOR);
	private static boolean playedIntro, logOpen, logChecked;
	private long openedAt;
	private boolean wasDown = true;

	public static void autotestLog() {
		logOpen = true;
		logChecked = true;
	}

	public SurgeTitleScreen() {
		super(Component.literal("Surge"));
	}

	@Override
	protected void init() {
		openedAt = System.currentTimeMillis();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		renderPanorama(g, delta);
		g.fill(0, 0, this.width, this.height, 0x38000000);
		g.fillGradient(0, this.height / 2, this.width, this.height, 0x00000000, 0xB0000000);
		g.fillGradient(0, 0, this.width, 40, 0x60000000, 0x00000000);
	}

	private float since(float delayMs, float durMs) {
		float t = (System.currentTimeMillis() - openedAt - delayMs) / durMs;
		t = Math.max(0f, Math.min(1f, t));
		return 1f - (1f - t) * (1f - t) * (1f - t);
	}

	private boolean glass(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, boolean primary, float alpha) {
		boolean hov = Ui.hover(mx, my, x, y, w, h) && alpha > 0.95f;
		float a = Ui.anim("title:" + label, hov);
		int fill = primary ? Colors.lerp(0xF0E4E4E7, 0xFFFFFFFF, a) : Colors.lerp(0xB0141416, 0xD8232327, a);
		int border = primary ? 0xFFFFFFFF : Colors.lerp(0x40FFFFFF, 0xD0FFFFFF, a);
		Ui.cutBox(g, x, y, w, h, 5, Colors.alpha(fill, alpha), Colors.alpha(border, alpha));
		int tc = Colors.alpha(primary ? 0xFF141416 : 0xFFFFFFFF, alpha);
		g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, tc, !primary);
		return hov;
	}

	private static String current() {
		return net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("surge").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("").split("[+-]")[0];
	}

	private java.util.List<String> wrap(String text, int w) {
		java.util.List<String> out = new java.util.ArrayList<>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			String next = line.isEmpty() ? word : line + " " + word;
			if (font.width(next) > w && !line.isEmpty()) { out.add(line.toString()); line = new StringBuilder(word); }
			else line = new StringBuilder(next);
		}
		if (!line.isEmpty()) out.add(line.toString());
		return out;
	}

	private Runnable changelog(GuiGraphics g, int mx, int my, boolean click, float alpha) {
		String cur = current();
		String upd = gg.surge.client.features.SelfUpdate.installed;
		boolean ready = upd != null;
		String shown = ready ? upd.split("[+-]")[0] : cur;
		java.util.List<String> log = ready && !gg.surge.client.features.SelfUpdate.notes.isEmpty() ? gg.surge.client.features.SelfUpdate.notes : gg.surge.client.features.Changelog.forVersion(shown);
		var cfg = gg.surge.client.config.SurgeConfig.get();
		boolean fresh = !cur.isEmpty() && !cur.equals(cfg.seenVersion);
		if (!logChecked) {
			logChecked = true;
			if (fresh && !cfg.seenVersion.isEmpty() && !log.isEmpty()) logOpen = true;
			else if (fresh) { cfg.seenVersion = cur; gg.surge.client.config.SurgeConfig.save(); }
		}
		String label = ready ? "Surge Client " + shown + " is ready, restart to use it" : fresh && !cfg.seenVersion.isEmpty() ? "Updated to Surge Client " + cur : "Surge Client " + cur;
		int right = this.width - 8, y = this.height - 14;
		String toggle = log.isEmpty() ? "" : logOpen ? "Hide" : "What's new";
		int tw = toggle.isEmpty() ? 0 : font.width(toggle) + 14;
		int lx = right - tw - (tw > 0 ? 8 : 0) - font.width(label);
		g.drawString(font, label, lx, y, Colors.alpha(ready || fresh ? Ui.ACCENT_2 : 0xA0FFFFFF, alpha), true);
		Runnable action = null;
		if (tw > 0) {
			int bx = right - tw, by = y - 4;
			boolean hov = Ui.hover(mx, my, bx, by, tw, 15);
			Ui.cutBox(g, bx, by, tw, 15, 3, Colors.alpha(logOpen ? 0xF0E4E4E7 : hov ? 0xD8232327 : 0xB0141416, alpha), Colors.alpha(logOpen ? 0xFFFFFFFF : hov ? 0xD0FFFFFF : 0x40FFFFFF, alpha));
			Ui.icon(g, logOpen ? new String[]{"........", "........", "XXXXXXX.", ".XXXXX..", "..XXX...", "...X....", "........", "........"} : new String[]{"..X.....", "..XX....", "..XXX...", "..XXXX..", "..XXX...", "..XX....", "..X.....", "........"}, bx + 3, by + 4, 1, Colors.alpha(logOpen ? 0xFF141416 : 0xFFFFFFFF, alpha));
			g.drawString(font, toggle, bx + 11, by + 4, Colors.alpha(logOpen ? 0xFF141416 : 0xFFFFFFFF, alpha), false);
			if (hov && click) action = () -> {
				logOpen = !logOpen;
				if (!logOpen && !cur.equals(cfg.seenVersion)) { cfg.seenVersion = cur; gg.surge.client.config.SurgeConfig.save(); }
			};
		}
		if (logOpen && !log.isEmpty()) {
			int pw = Math.min(240, this.width / 2 - 16);
			java.util.List<String[]> rows = new java.util.ArrayList<>();
			for (String item : log) {
				java.util.List<String> ls = wrap(item, pw - 26);
				for (int i = 0; i < ls.size(); i++) rows.add(new String[]{i == 0 ? "1" : "0", ls.get(i)});
			}
			int ph = 22 + rows.size() * 10 + 6;
			int px = right - pw, py = y - 10 - ph;
			g.nextStratum();
			Ui.cutBox(g, px, py, pw, ph, 5, Colors.alpha(0xF0141416, alpha), Colors.alpha(0xFF3A3A40, alpha));
			g.drawString(font, "What's new in " + shown, px + 10, py + 8, Colors.alpha(0xFFFFFFFF, alpha), false);
			int ry = py + 22;
			for (String[] r : rows) {
				if (r[0].equals("1")) g.fill(px + 11, ry + 3, px + 14, ry + 6, Colors.alpha(Ui.ACCENT_2, alpha));
				g.drawString(font, r[1], px + 19, ry, Colors.alpha(0xFFC8C8CD, alpha), false);
				ry += 10;
			}
		}
		return action;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		Ui.frame();
		boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasDown;
		wasDown = down;
		Runnable action = null;
		int cx = this.width / 2;

		float logoIn = since(100, 600);
		int size = this.height >= 320 ? 64 : 48;
		float bob = (float) Math.sin((System.currentTimeMillis() - openedAt) / 900.0) * 2f;
		int bw = 204, bh = 22, gap = 5;
		int blockH = size + 18 + bh * 3 + gap * 2;
		int top = Math.max(16, (this.height - blockH) / 2 - 10);
		int logoY = top + Math.round((1 - logoIn) * 10 + bob);
		if (logoIn > 0) {
			g.pose().pushMatrix();
			Ui.logo(g, cx - size / 2, logoY, size);
			g.pose().popMatrix();
			if (logoIn < 1) g.fill(cx - size / 2, logoY, cx + size / 2, logoY + size, Colors.alpha(0xFF000000, 1 - logoIn));
		}

		int by = top + size + 18;
		String[] labels = {"Singleplayer", "Multiplayer"};
		for (int i = 0; i < labels.length; i++) {
			float k = since(250 + i * 70, 420);
			int y = by + i * (bh + gap) + Math.round((1 - k) * 12);
			if (glass(g, mx, my, cx - bw / 2, y, bw, bh, labels[i], i == 0, k) && click) {
				action = i == 0 ? () -> minecraft.setScreen(new SelectWorldScreen(this)) : () -> minecraft.setScreen(new JoinMultiplayerScreen(this));
			}
		}
		float k3 = since(390, 420);
		int y3 = by + 2 * (bh + gap) + Math.round((1 - k3) * 12);
		int half = (bw - gap) / 2;
		if (glass(g, mx, my, cx - bw / 2, y3, half, bh, "Options", false, k3) && click) action = () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options));
		if (glass(g, mx, my, cx - bw / 2 + half + gap, y3, half, bh, "Quit", false, k3) && click) action = minecraft::stop;

		float kc = since(450, 400);
		int ty = 8 - Math.round((1 - kc) * 8);
		int x = this.width - 8 - 24;
		if (Ui.tile(g, font, mx, my, x, ty, 24, COIN, Wallet.get().coins + " Surge Coins", true, kc) && click) action = () -> minecraft.setScreen(WardrobeScreen.open(this, WardrobeScreen.Section.CURRENCY));
		x -= 30;
		if (Ui.tile(g, font, mx, my, x, ty, 24, MODS, "Surge mods", true, kc) && click) action = () -> minecraft.setScreen(new SurgeMenuScreen(this));
		x -= 30;
		if (Ui.tile(g, font, mx, my, x, ty, 24, SHIRT, "Wardrobe", true, kc) && click) action = () -> minecraft.setScreen(new WardrobeScreen(this));
		x -= 30;
		if (Ui.tile(g, font, mx, my, x, ty, 24, GEAR, "Settings", true, kc) && click) action = () -> minecraft.setScreen(SurgeMenuScreen.general(this));
		x -= 30;
		if (Ui.tile(g, font, mx, my, x, ty, 24, SOCIAL, MenuOverlay.socialLabel(), true, kc) && click) action = () -> minecraft.setScreen(gg.surge.client.net.Social.screen(this));
		MenuOverlay.unreadBadge(g, minecraft, x, ty, 24);

		String name = minecraft.getUser().getName();
		g.drawString(font, name, 10, ty + 3, Colors.alpha(0xFFFFFFFF, kc), true);
		String st = gg.surge.client.net.Social.status();
		int sc = gg.surge.client.net.Social.statusColor(st.equals("invisible") ? "offline" : st);
		g.fill(10, ty + 15, 15, ty + 20, Colors.alpha(sc, kc));
		int on = 0;
		for (var f : gg.surge.client.net.Social.friends) if (!"offline".equals(f.status())) on++;
		String sub = gg.surge.client.net.Social.statusLabel(st) + (gg.surge.client.net.Social.available() ? "  " + on + " friend" + (on == 1 ? "" : "s") + " online" : "");
		g.drawString(font, sub, 19, ty + 14, Colors.alpha(0xFFA9AAAE, kc), true);

		float kf = since(500, 400);
		g.drawString(font, "Minecraft 1.21.11", 8, this.height - 14, Colors.alpha(0xA0FFFFFF, kf), true);
		Runnable logAction = changelog(g, mx, my, click, kf);
		if (logAction != null) action = logAction;

		if (!playedIntro) {
			float f = since(0, 700);
			if (f < 1) g.fill(0, 0, this.width, this.height, Colors.alpha(0xFF000000, 1 - f));
			else playedIntro = true;
		}

		Ui.flushLabels();
		SocialToasts.render(g, minecraft, this.width);
		if (action != null) { Ui.click(); action.run(); }
	}
}
