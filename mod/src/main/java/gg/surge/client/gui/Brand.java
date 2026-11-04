package gg.surge.client.gui;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

public final class Brand {
	private static String version;

	private Brand() {}

	public static String version() {
		if (version == null) version = net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("surge")
			.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("").split("[+-]")[0];
		return version;
	}

	public static void draw(Minecraft mc, Screen s, GuiGraphics g) {
		if (s instanceof SurgeMenuScreen || s instanceof WardrobeScreen || s instanceof SocialScreen || s instanceof EmoteWheelScreen
			|| s instanceof HudEditorScreen || s instanceof SurgeTitleScreen || s instanceof ChatScreen || s instanceof BetaCapeScreen) return;
		Component title = Ui.ten("SURGE CLIENT");
		String ver = "v" + version();
		int tw = mc.font.width(title);
		int w = 14 + tw;
		int x, y;
		boolean under = Mods.BRAND_POS.is("Under inventory") && s instanceof AbstractContainerScreen<?>;
		if (under) {
			gg.surge.client.mixin.ContainerScreenAccessor a = (gg.surge.client.mixin.ContainerScreenAccessor) s;
			x = a.surge$left() + (a.surge$width() - w) / 2;
			y = a.surge$top() + a.surge$height() + 6;
			if (y + 18 > s.height) { x = s.width - w - 6; y = s.height - 22; }
		} else {
			x = s.width - w - 6;
			y = s.height - 22;
		}
		g.nextStratum();
		Ui.logo(g, x, y + 1, 11);
		g.drawString(mc.font, title, x + 14, y + 2, 0xE6FFFFFF, true);
		g.pose().pushMatrix();
		float sc = 0.75f;
		int vw = Math.round(mc.font.width(ver) * sc);
		g.pose().translate(x + 14 + tw - vw, y + 12);
		g.pose().scale(sc, sc);
		g.drawString(mc.font, ver, 0, 0, 0xA0C8C8CD, false);
		g.pose().popMatrix();
	}
}
