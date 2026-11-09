package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class GuiScales {
	private static boolean applied;

	private GuiScales() {}

	public static int inventoryScale() {
		if (!Mods.GUI_SCALES.enabled() || Mods.GS_INVENTORY.is("Same as game")) return 0;
		try { return Integer.parseInt(Mods.GS_INVENTORY.get()); } catch (Exception e) { return 0; }
	}

	public static boolean wants(Screen s) {
		return s instanceof AbstractContainerScreen<?> && inventoryScale() > 0;
	}

	public static int adjust(int scale) {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || !wants(mc.screen)) return scale;
		try {
			return mc.getWindow().calculateScale(inventoryScale(), mc.options.forceUnicodeFont().get());
		} catch (Throwable t) {
			return scale;
		}
	}

	public static void screenChanged(Minecraft mc) {
		boolean want = wants(mc.screen);
		if (want != applied) {
			applied = want;
			mc.resizeDisplay();
		}
	}

	public static void refresh() {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.getWindow() == null) return;
		mc.execute(() -> {
			applied = wants(mc.screen);
			if (gg.surge.client.mods.Mods.GUI_SCALES.enabled()) mc.options.menuBackgroundBlurriness().set(gg.surge.client.mods.Mods.GS_BLUR.asInt());
			mc.resizeDisplay();
		});
	}

	public static float hudFactor = 1f;

	public static float compensation() {
		Minecraft mc = Minecraft.getInstance();
		if (!applied || mc == null || mc.getWindow() == null) return 1f;
		try {
			int normal = mc.getWindow().calculateScale(mc.options.guiScale().get(), mc.options.forceUnicodeFont().get());
			int cur = mc.getWindow().getGuiScale();
			return cur <= 0 || normal == cur ? 1f : normal / (float) cur;
		} catch (Throwable t) {
			return 1f;
		}
	}

	public static float scale(int pct) {
		return Mods.GUI_SCALES.enabled() ? pct / 100f : 1f;
	}

	public static boolean push(GuiGraphics g, float s, float ax, float ay) {
		if (s == 1f) return false;
		g.pose().pushMatrix();
		g.pose().translate(ax, ay);
		g.pose().scale(s, s);
		g.pose().translate(-ax, -ay);
		return true;
	}
}
