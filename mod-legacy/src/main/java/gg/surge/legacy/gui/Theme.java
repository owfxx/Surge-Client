package gg.surge.legacy.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.surge.legacy.mod.Category;
import gg.surge.legacy.mod.Mod;
import gg.surge.legacy.mod.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public final class Theme {
	public static final Mod MENU = new Mod("menu", "Menu Look", Category.UTILITY, new ItemStack(Items.PAINTING), "Change the accent color of the Surge menus", true);
	public static final Setting.Color ACCENT = MENU.color("accent", "Accent color", "8F50F2");

	static {
		MENU.toggleable = false;
	}

	private static boolean glass, shared;
	private static long checkedAt, fileAt;
	private static String lastAccent = "";

	private Theme() {}

	public static boolean glass() {
		long now = System.currentTimeMillis();
		if (now - checkedAt > 250) {
			checkedAt = now;
			String a = ACCENT.chroma() ? "chroma" : ACCENT.get();
			if (!a.equals(lastAccent)) {
				lastAccent = a;
				int c = ACCENT.rgb(0);
				Draw.ACCENT = c;
				Draw.ACCENT_2 = Draw.lerp(c, 0xFFFFFFFF, 0.3f);
			}
			if (now - fileAt > 2500) {
				fileAt = now;
				shared = readShared();
			}
			boolean g = "EDEADE".equalsIgnoreCase(ACCENT.get()) || shared;
			if (g != glass) {
				glass = g;
				if (g) { Draw.ACCENT = 0xFFEDEADE; Draw.ACCENT_2 = 0xFFFFFFFF; }
				else { lastAccent = ""; }
			}
		}
		return glass;
	}

	private static boolean readShared() {
		File f = new File(new File(System.getProperty("user.home"), ".surgeclient"), "surge.json");
		if (!f.isFile()) return false;
		try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
			JsonElement e = new JsonParser().parse(r);
			JsonObject o = e.getAsJsonObject().getAsJsonObject("modules").getAsJsonObject("general").getAsJsonObject("opts");
			return "Custom".equals(o.get("theme2").getAsString()) && "EDEADE".equalsIgnoreCase(o.get("themeColor").getAsString());
		} catch (Exception ex) {
			return false;
		}
	}

	public static void sound(String glassName, float pitch) {
		MinecraftClient mc = MinecraftClient.getInstance();
		try {
			if (glass()) mc.getSoundManager().play(PositionedSoundInstance.master(new Identifier("surge", glassName), 1f));
			else mc.getSoundManager().play(PositionedSoundInstance.master(new Identifier("gui.button.press"), pitch));
		} catch (Throwable ignored) { }
	}
}
