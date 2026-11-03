package gg.surge.client.features;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public final class CommandKeys {
	private static final Map<Integer, Boolean> WAS_DOWN = new HashMap<>();
	private static long lastSent;

	private CommandKeys() {}

	public static void tick(Minecraft mc) {
		if (!Mods.COMMAND_KEYS.enabled() || mc.player == null || mc.getConnection() == null) return;
		long window = mc.getWindow().handle();
		for (SurgeConfig.CommandKey ck : SurgeConfig.get().commandKeys) {
			if (ck.key < 0 || ck.text == null || ck.text.isBlank()) continue;
			boolean down = GLFW.glfwGetKey(window, ck.key) == GLFW.GLFW_PRESS;
			boolean was = WAS_DOWN.getOrDefault(ck.key, false);
			if (down && !was && mc.screen == null && System.currentTimeMillis() - lastSent > 250) send(mc, ck.text);
		}
		WAS_DOWN.clear();
		for (SurgeConfig.CommandKey ck : SurgeConfig.get().commandKeys) {
			if (ck.key >= 0) WAS_DOWN.put(ck.key, GLFW.glfwGetKey(window, ck.key) == GLFW.GLFW_PRESS);
		}
	}

	public static String fill(Minecraft mc, String text) {
		LocalPlayer p = mc.player;
		if (p == null) return text;
		return text.replace("{x}", String.valueOf(p.getBlockX()))
			.replace("{y}", String.valueOf(p.getBlockY()))
			.replace("{z}", String.valueOf(p.getBlockZ()))
			.replace("{name}", p.getName().getString())
			.replace("{server}", mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "singleplayer");
	}

	public static void send(Minecraft mc, String raw) {
		lastSent = System.currentTimeMillis();
		String text = fill(mc, raw.trim());
		if (text.isEmpty()) return;
		if (text.startsWith("/")) mc.getConnection().sendCommand(text.substring(1));
		else mc.getConnection().sendChat(text);
	}

	public static String keyName(int key) {
		if (key < 0) return "None";
		String n = GLFW.glfwGetKeyName(key, 0);
		if (n != null && !n.isBlank()) return n.toUpperCase();
		if (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25) return "F" + (key - GLFW.GLFW_KEY_F1 + 1);
		if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) return "Numpad " + (key - GLFW.GLFW_KEY_KP_0);
		return switch (key) {
			case GLFW.GLFW_KEY_SPACE -> "Space";
			case GLFW.GLFW_KEY_LEFT_SHIFT -> "L Shift";
			case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R Shift";
			case GLFW.GLFW_KEY_LEFT_CONTROL -> "L Ctrl";
			case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R Ctrl";
			case GLFW.GLFW_KEY_LEFT_ALT -> "L Alt";
			case GLFW.GLFW_KEY_RIGHT_ALT -> "R Alt";
			case GLFW.GLFW_KEY_TAB -> "Tab";
			case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps";
			case GLFW.GLFW_KEY_ENTER -> "Enter";
			case GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
			case GLFW.GLFW_KEY_INSERT -> "Insert";
			case GLFW.GLFW_KEY_DELETE -> "Delete";
			case GLFW.GLFW_KEY_HOME -> "Home";
			case GLFW.GLFW_KEY_END -> "End";
			case GLFW.GLFW_KEY_PAGE_UP -> "Page Up";
			case GLFW.GLFW_KEY_PAGE_DOWN -> "Page Down";
			case GLFW.GLFW_KEY_UP -> "Up";
			case GLFW.GLFW_KEY_DOWN -> "Down";
			case GLFW.GLFW_KEY_LEFT -> "Left";
			case GLFW.GLFW_KEY_RIGHT -> "Right";
			default -> "Key " + key;
		};
	}

	public static int pollPressed(Minecraft mc, Map<Integer, Boolean> prev) {
		long window = mc.getWindow().handle();
		int found = -1;
		for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
			boolean d = GLFW.glfwGetKey(window, k) == GLFW.GLFW_PRESS;
			if (d && !prev.getOrDefault(k, false) && found < 0) found = k;
			prev.put(k, d);
		}
		return found;
	}
}
