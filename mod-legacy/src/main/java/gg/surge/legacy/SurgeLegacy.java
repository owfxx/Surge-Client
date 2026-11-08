package gg.surge.legacy;

import gg.surge.legacy.feature.Features;
import gg.surge.legacy.feature.Stats;
import gg.surge.legacy.gui.HudEditorScreen;
import gg.surge.legacy.gui.MenuScreen;
import gg.surge.legacy.hud.Hud;
import gg.surge.legacy.mod.Mods;
import gg.surge.legacy.net.SurgeNet;
import gg.surge.legacy.perf.Perf;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

public class SurgeLegacy implements ClientModInitializer {
	private static boolean started;

	@Override
	public void onInitializeClient() {
		Config.get();
		Mods.all();
	}

	public static void tick(MinecraftClient mc) {
		if (!started && mc.options != null) {
			started = true;
			Perf.firstRun(mc);
		}
		if (mc.currentScreen == null && mc.player != null) {
			while (Keys.MENU.wasPressed()) mc.setScreen(new MenuScreen(null));
			while (Keys.HUD.wasPressed()) mc.setScreen(new HudEditorScreen(null));
		}
		Features.tick(mc);
		Stats.tick(mc);
		try { SurgeNet.tick(mc); } catch (Throwable ignored) { }
		Config.tick();
		if (gg.surge.legacy.AutoTest.active()) gg.surge.legacy.AutoTest.tick(mc);
	}

	public static void hud(MinecraftClient mc, float tickDelta) {
		Stats.frame(mc);
		net.minecraft.client.util.Window w = new net.minecraft.client.util.Window(mc);
		Hud.render(mc, w.getWidth(), w.getHeight());
	}
}
