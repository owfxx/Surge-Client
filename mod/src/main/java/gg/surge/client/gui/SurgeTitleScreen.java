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
	private static final ItemStack COIN = new ItemStack(Items.GOLD_NUGGET);
	private static final ItemStack SHIRT = new ItemStack(Items.LEATHER_CHESTPLATE);
	private static final ItemStack GEAR = new ItemStack(Items.REPEATER);
	private static final ItemStack SOCIAL = new ItemStack(Items.WRITABLE_BOOK);
	private static final ItemStack MODS = new ItemStack(Items.COMPARATOR);
	private static boolean playedIntro;
	private long openedAt;
	private boolean wasDown = true;

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
		int fill = primary ? Colors.lerp(0xE07A3FE0, 0xF0935BF5, a) : Colors.lerp(0x9A0E0E12, 0xC0222228, a);
		int border = primary ? Colors.lerp(0x60FFFFFF, 0xC0FFFFFF, a) : Colors.lerp(0x30FFFFFF, 0xC0B483FF, a);
		g.fill(x, y, x + w, y + h, Colors.alpha(fill, alpha));
		Ui.outline(g, x, y, w, h, Colors.alpha(border, alpha));
		g.fill(x + 1, y + 1, x + w - 1, y + 2, Colors.alpha(0x22FFFFFF, alpha));
		int tc = Colors.alpha(0xFFFFFFFF, alpha);
		g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, tc, true);
		return hov;
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
		String upd = gg.surge.client.features.SelfUpdate.installed;
		if (upd != null) {
			String msg = "Surge Client " + upd.split("[+-]")[0] + " is ready. Restart the game to use it.";
			g.drawString(font, msg, this.width - 8 - font.width(msg), this.height - 26, Colors.alpha(0xFFB483FF, kf), true);
		}
		String brand = "Surge Client";
		g.drawString(font, brand, this.width - 8 - font.width(brand), this.height - 14, Colors.alpha(0xA0FFFFFF, kf), true);

		if (!playedIntro) {
			float f = since(0, 700);
			if (f < 1) g.fill(0, 0, this.width, this.height, Colors.alpha(0xFF000000, 1 - f));
			else playedIntro = true;
		}

		SocialToasts.render(g, minecraft, this.width);
		if (action != null) { Ui.click(); action.run(); }
	}
}
