package gg.surge.legacy;

import gg.surge.legacy.feature.Features;
import gg.surge.legacy.gui.HudEditorScreen;
import gg.surge.legacy.gui.MenuScreen;
import gg.surge.legacy.hud.Hud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotUtils;
import net.minecraft.world.level.LevelGeneratorType;
import net.minecraft.world.level.LevelInfo;

public final class AutoTest {
	private static int ticks, step;
	private static boolean worldStarted;

	private AutoTest() {}

	public static boolean active() {
		return Boolean.getBoolean("surge.autotest");
	}

	private static void shot(MinecraftClient mc, String name) {
		try {
			ScreenshotUtils.saveScreenshot(mc.runDirectory, name + ".png", mc.width, mc.height, mc.getFramebuffer());
			System.out.println("[Surge autotest] shot " + name);
		} catch (Throwable t) {
			System.out.println("[Surge autotest] shot failed " + name + " " + t);
		}
	}

	private static void set(String mod, String key, String value) {
		Config.mod(mod).values.put(key, value);
	}

	public static void tick(MinecraftClient mc) {
		ticks++;
		try {
			run(mc);
		} catch (Throwable t) {
			System.out.println("[Surge autotest] error at step " + step + ": " + t);
			t.printStackTrace();
			step++;
			ticks = 0;
		}
	}

	private static void next() {
		step++;
		ticks = 0;
	}

	private static void run(MinecraftClient mc) {
		switch (step) {
			case 0:
				if (ticks == 80) { shot(mc, "l01-title"); next(); }
				break;
			case 1:
				if (!worldStarted) {
					worldStarted = true;
					mc.startIntegratedServer("surgetest", "surgetest", new LevelInfo(4242L, LevelInfo.GameMode.CREATIVE, true, false, LevelGeneratorType.DEFAULT).enableCommands());
				}
				if (mc.player != null && mc.world != null) next();
				break;
			case 2:
				if (ticks == 200) {
					mc.options.hudHidden = false;
					shot(mc, "l02-hud-default");
					set("coords", "biome", "true");
					set("coords", "chunk", "true");
					set("coords", "otherDim", "true");
					set("coords", "customLabels", "true");
					set("coords", "dirFormat", "Letter and axis");
					for (String id : new String[]{"armor", "ping", "biome", "reach", "clock", "memory"}) Config.mod(id).enabled = true;
					next();
				}
				break;
			case 3:
				if (ticks == 20) { shot(mc, "l03-hud-full"); mc.setScreen(new MenuScreen(null)); next(); }
				break;
			case 4:
				if (ticks == 30) { shot(mc, "l04-menu"); mc.setScreen(new MenuScreen(null).openMod(Hud.byId("coords"))); next(); }
				break;
			case 5:
				if (ticks == 20) { shot(mc, "l05-menu-coords"); mc.setScreen(new HudEditorScreen(null)); next(); }
				break;
			case 6:
				if (ticks == 20) { shot(mc, "l06-hud-editor"); mc.setScreen(null); mc.options.perspective = 2; next(); }
				break;
			case 7:
				if (ticks == 60) { shot(mc, "l07-cosmetics-front"); mc.options.perspective = 1; next(); }
				break;
			case 8:
				if (ticks == 40) { shot(mc, "l08-cosmetics-back"); mc.options.perspective = 0; next(); }
				break;
			case 9:
				if (ticks == 10) {
					mc.player.sendChatMessage("/time set 18000");
					Features.FULLBRIGHT.setEnabled(true);
				}
				if (ticks == 60) { shot(mc, "l09-fullbright-night"); Features.FULLBRIGHT.setEnabled(false); next(); }
				break;
			case 10:
				if (ticks == 20) {
					System.out.println("[Surge autotest] fps " + MinecraftClient.getCurrentFps());
					System.out.println("[Surge autotest] done");
					mc.scheduleStop();
					next();
				}
				break;
			default:
				break;
		}
	}

	public static String cosmetic(String type) {
		if (!active()) return null;
		if (type.equals("cape")) return "flame";
		if (type.equals("wings")) return "angel";
		return "crown";
	}

}
