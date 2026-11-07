package gg.surge.client.gui;

import gg.surge.client.economy.Wallet;
import gg.surge.client.mods.Colors;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.lwjgl.glfw.GLFW;

public final class MenuOverlay {
	private static final net.minecraft.world.item.ItemStack COIN = Ui.COIN_ITEM;

	private MenuOverlay() {}

	public static void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> Brand.draw(client, s, g));
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
			java.util.List<net.minecraft.client.gui.components.Button> buttons = new java.util.ArrayList<>();
			if (screen instanceof PauseScreen) {
				for (var w2 : net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen)) {
					if (w2 instanceof net.minecraft.client.gui.components.Button b) { b.setAlpha(1f); buttons.add(b); }
				}
			}
			ScreenEvents.afterRender(screen).register((s, g, mx, my, delta) -> {
				draw(client, s, g, mx, my, wasDown);
			});
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
		int size = 22, x = w - size - 8, y = h / 2 - 66;
		if (Ui.tile(g, mc.font, mx, my, x, y - 26, size, Ui.IC_SOCIAL, socialLabel(), true, 1f) && click) action = () -> mc.setScreen(gg.surge.client.net.Social.screen(screen));
		unreadBadge(g, mc, x, y - 26, size);
		if (Ui.tile(g, mc.font, mx, my, x, y, size, Ui.IC_SETTINGS, "Settings", true, 1f) && click) action = () -> mc.setScreen(SurgeMenuScreen.general(screen));
		y += 26;
		boolean underPerson = screen instanceof PauseScreen && mc.player != null && (w / 2 - 112) - Math.max(8, w / 2 - 202) >= 50;
		if (!underPerson && Ui.tile(g, mc.font, mx, my, x, y, size, Ui.IC_WARDROBE, "Wardrobe", true, 1f) && click) action = () -> mc.setScreen(WardrobeScreen.open(screen, WardrobeScreen.Section.FEATURED));
		if (underPerson) y -= 26;
		if (Ui.tile(g, mc.font, mx, my, x, y + 26, size, Ui.IC_MODS, "Surge mods", true, 1f) && click) action = () -> mc.setScreen(new SurgeMenuScreen(screen));
		if (Ui.tile(g, mc.font, mx, my, x, y + 52, size, Ui.COIN_ITEM, Wallet.get().coins + " Surge Coins", true, 1f) && click) action = () -> mc.setScreen(WardrobeScreen.open(screen, WardrobeScreen.Section.CURRENCY));

		if (screen instanceof PauseScreen && mc.player != null) {
			int x2 = w / 2 - 112;
			int x1 = Math.max(8, x2 - 90);
			if (x2 - x1 >= 50) {
				int y1 = h / 2 - 80, y2 = h / 2 + 30;
				gg.surge.client.cosmetics.Preview.draw(g, x1, y1, x2, y2, 46);
				int bw = Math.min(84, x2 - x1), bx = (x1 + x2) / 2 - bw / 2, by = y2 + 6;
				boolean hov = Ui.hover(mx, my, bx, by, bw, 18);
				Ui.mcButton(g, bx, by, bw, 18, hov, false, 1f);
				Ui.iconTex(g, Ui.IC_WARDROBE, bx + 4, by + 1, 16, 1f);
				g.drawString(mc.font, "Wardrobe", bx + 21, by + 5, hov ? 0xFFFFFFA0 : 0xFFFFFFFF, true);
				if (hov && click) action = () -> mc.setScreen(WardrobeScreen.open(screen, WardrobeScreen.Section.FEATURED));
			}
		}

		Ui.flushLabels();
		SocialToasts.render(g, mc, w);
		if (action != null) { Ui.click(); action.run(); }
	}

	private static void restyle(Minecraft mc, GuiGraphics g, int mx, int my, java.util.List<net.minecraft.client.gui.components.Button> buttons) {
		Ui.frame();
		g.nextStratum();
		boolean first = true;
		for (var b : buttons) {
			if (!b.visible) continue;
			int x = b.getX(), y = b.getY(), w = b.getWidth(), h = b.getHeight();
			boolean hov = b.active && (Ui.hover(mx, my, x, y, w, h) || b.isFocused());
			float a = Ui.anim("pause:" + x + "," + y, hov);
			boolean primary = first;
			first = false;
			int fill = !b.active ? 0xD0141416 : primary ? Colors.lerp(0xF0E4E4E7, 0xFFFFFFFF, a) : Colors.lerp(0xE01A1A1D, 0xF02A2A2F, a);
			int edge = !b.active ? 0xFF2A2A2E : primary ? 0xFFFFFFFF : Colors.lerp(0x50FFFFFF, 0xE0FFFFFF, a);
			Ui.cutBox(g, x, y, w, h, 4, fill, edge);
			String label = Ui.fit(mc.font, b.getMessage().getString(), w - 8);
			g.drawString(mc.font, label, x + (w - mc.font.width(label)) / 2, y + (h - 8) / 2, !b.active ? 0xFF6E6F74 : primary ? 0xFF141416 : 0xFFFFFFFF, false);
		}
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
