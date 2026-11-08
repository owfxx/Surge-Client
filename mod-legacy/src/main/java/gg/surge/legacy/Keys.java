package gg.surge.legacy;

import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;

public final class Keys {
	public static final String CATEGORY = "Surge Client";
	public static final KeyBinding MENU = new KeyBinding("Open Surge menu", Keyboard.KEY_RSHIFT, CATEGORY);
	public static final KeyBinding ZOOM = new KeyBinding("Zoom", Keyboard.KEY_C, CATEGORY);
	public static final KeyBinding FREELOOK = new KeyBinding("Freelook", Keyboard.KEY_LMENU, CATEGORY);
	public static final KeyBinding HUD = new KeyBinding("Edit HUD", Keyboard.KEY_NONE, CATEGORY);
	public static final KeyBinding[] ALL = {MENU, ZOOM, FREELOOK, HUD};

	private Keys() {}
}
