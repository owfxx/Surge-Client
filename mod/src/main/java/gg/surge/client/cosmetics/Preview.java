package gg.surge.client.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public final class Preview {
	public static volatile boolean clean;

	private Preview() {}

	public static void draw(GuiGraphics g, int x1, int y1, int x2, int y2, int size) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) return;
		double t = System.currentTimeMillis() / 1000.0;
		int cx = (x1 + x2) / 2, cy = (y1 + y2) / 2;
		int lookX = cx + (int) Math.round(Math.sin(t * 0.6) * 70);
		int lookY = cy - size / 2;
		clean = true;
		try {
			InventoryScreen.renderEntityInInventoryFollowsMouse(g, x1, y1, x2, y2, size, 0.0625F, lookX, lookY, mc.player);
		} finally {
			clean = false;
		}
	}
}
