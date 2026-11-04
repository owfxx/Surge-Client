package gg.surge.client.gui;

import gg.surge.client.economy.Wallet;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.lwjgl.glfw.GLFW;

public final class MenuOverlay {
	private static final net.minecraft.world.item.ItemStack SHIRT = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_CHESTPLATE);
	private static final net.minecraft.world.item.ItemStack MODS = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPARATOR);
	private static final net.minecraft.world.item.ItemStack COIN = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET);
	private static final net.minecraft.world.item.ItemStack GEAR = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.REPEATER);
	private static final net.minecraft.world.item.ItemStack SOCIAL = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
	private static final net.minecraft.world.item.ItemStack EMOTE = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARMOR_STAND);

	private MenuOverlay() {}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (screen instanceof net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen) {
				boolean[] down = {true};
				ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> {
					ServerTabsScreen.drawTabs(client, s, s, g, mx, my, down, ServerTabsScreen.Mode.SERVERS);
					SocialToasts.render(g, client, s.width);
				});
				return;
			}
			if (!(screen instanceof TitleScreen) && !(screen instanceof PauseScreen)) {
				if (!(screen instanceof SocialScreen)) ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> SocialToasts.render(g, client, s.width));
				return;
			}
			boolean[] wasDown = {true};
			ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> draw(client, s, g, mx, my, wasDown));
		});
	}

	private static void draw(Minecraft mc, Screen screen, GuiGraphics g, int mx, int my, boolean[] wasDown) {
		boolean down = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasDown[0];
		wasDown[0] = down;
		Runnable action = null;
		int w = screen.width, h = screen.height;

		Ui.frame();
		g.nextStratum();
		int size = 22, x = w - size - 8, y = h / 2 - 76;
		if (Ui.tile(g, mc.font, mx, my, x, y - 26, size, SOCIAL, socialLabel(), true, 1f) && click) action = () -> mc.setScreen(gg.surge.client.net.Social.screen(screen));
		unreadBadge(g, mc, x, y - 26, size);
		if (Ui.tile(g, mc.font, mx, my, x, y, size, GEAR, "Settings", true, 1f) && click) action = () -> mc.setScreen(SurgeMenuScreen.general(screen));
		y += 26;
		if (Ui.tile(g, mc.font, mx, my, x, y, size, SHIRT, "Wardrobe", true, 1f) && click) action = () -> mc.setScreen(WardrobeScreen.open(screen, WardrobeScreen.Section.COSMETICS));
		if (Ui.tile(g, mc.font, mx, my, x, y + 26, size, MODS, "Surge mods", true, 1f) && click) action = () -> mc.setScreen(new SurgeMenuScreen(screen));
		if (Ui.tile(g, mc.font, mx, my, x, y + 52, size, COIN, Wallet.get().coins + " Surge Coins", true, 1f) && click) action = () -> mc.setScreen(WardrobeScreen.open(screen, WardrobeScreen.Section.CURRENCY));
		if (mc.player != null && Ui.tile(g, mc.font, mx, my, x, y + 78, size, EMOTE, "Emotes", true, 1f) && click) action = () -> mc.setScreen(new EmoteWheelScreen());

		if (screen instanceof PauseScreen && mc.player != null) {
			int x2 = w / 2 - 112;
			int x1 = Math.max(8, x2 - 90);
			if (x2 - x1 >= 50) {
				int y1 = h / 2 - 80, y2 = h / 2 + 30;
				gg.surge.client.cosmetics.Preview.draw(g, x1, y1, x2, y2, 46);
			}
		}

		SocialToasts.render(g, mc, w);
		if (action != null) { Ui.click(); action.run(); }
	}

	static String socialLabel() {
		int n = gg.surge.client.net.Social.totalUnread();
		return n > 0 ? "Social (" + n + " new)" : "Social";
	}

	static void unreadBadge(GuiGraphics g, Minecraft mc, int x, int y, int size) {
		int n = gg.surge.client.net.Social.totalUnread();
		if (n <= 0) return;
		String s = n > 9 ? "9+" : String.valueOf(n);
		int bw = Math.max(9, mc.font.width(s) + 4), bx = x + size - bw + 3, by = y - 3;
		g.fill(bx, by, bx + bw, by + 9, 0xFFE11D48);
		g.drawString(mc.font, s, bx + (bw - mc.font.width(s)) / 2 + 1, by + 1, 0xFFFFFFFF, false);
	}
}
