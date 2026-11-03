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

/** Essential-style extras on the title and pause screens: icon column + player preview. */
public final class MenuOverlay {
	private static final net.minecraft.world.item.ItemStack SHIRT = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LEATHER_CHESTPLATE);
	private static final net.minecraft.world.item.ItemStack MODS = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COMPARATOR);
	private static final net.minecraft.world.item.ItemStack COIN = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET);
	private static final net.minecraft.world.item.ItemStack EMOTE = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ARMOR_STAND);

	private MenuOverlay() {}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof TitleScreen) && !(screen instanceof PauseScreen)) return;
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

		// Right icon column: hover for a white outline, keep hovering to see where it leads
		Ui.frame();
		int size = 22, x = w - size - 8, y = h / 2 - 50;
		if (Ui.tile(g, mc.font, mx, my, x, y, size, SHIRT, "Wardrobe", true, 1f) && click) action = () -> mc.setScreen(WardrobeScreen.open(screen, WardrobeScreen.Section.COSMETICS));
		if (Ui.tile(g, mc.font, mx, my, x, y + 26, size, MODS, "Surge mods", true, 1f) && click) action = () -> mc.setScreen(new SurgeMenuScreen(screen));
		if (Ui.tile(g, mc.font, mx, my, x, y + 52, size, COIN, Wallet.get().coins + " Surge Coins", true, 1f) && click) action = () -> mc.setScreen(WardrobeScreen.open(screen, WardrobeScreen.Section.CURRENCY));
		if (mc.player != null && Ui.tile(g, mc.font, mx, my, x, y + 78, size, EMOTE, "Emotes", true, 1f) && click) action = () -> mc.setScreen(new EmoteWheelScreen());

		// Player preview on the pause screen, wardrobe button under it
		if (screen instanceof PauseScreen && mc.player != null) {
			int x2 = w / 2 - 112;
			int x1 = Math.max(8, x2 - 90);
			if (x2 - x1 >= 50) {
				int y1 = h / 2 - 80, y2 = h / 2 + 30;
				gg.surge.client.cosmetics.Preview.draw(g, x1, y1, x2, y2, 46);
			}
		}

		if (action != null) { Ui.click(); action.run(); }
	}
}
