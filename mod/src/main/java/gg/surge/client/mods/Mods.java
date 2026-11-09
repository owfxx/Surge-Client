package gg.surge.client.mods;

import gg.surge.client.features.DamageTint;
import gg.surge.client.features.Fullbright;
import gg.surge.client.features.OptionTweaks;
import gg.surge.client.features.Waypoints;
import gg.surge.client.hud.HudModule;
import gg.surge.client.hud.HudModules;
import gg.surge.client.mods.SurgeMod.Category;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class Mods {
	private static final List<SurgeMod> FEATURES = new ArrayList<>();

	private Mods() {}

	private static SurgeMod mod(String id, String name, Category cat, net.minecraft.world.item.Item icon, String desc, boolean def) {
		SurgeMod m = new SurgeMod(id, name, cat, icon, desc, def);
		FEATURES.add(m);
		return m;
	}

	private static Minecraft mc() {
		return Minecraft.getInstance();
	}

	public static final SurgeMod GENERAL = new SurgeMod("general", "Settings", SurgeMod.Category.UTILITY, Items.COMPARATOR,
		"Settings for the whole client", true);
	static { GENERAL.toggleable = false; }
	public static final Setting.Header H_STYLE = GENERAL.header("HUD style");
	public static final Setting.Toggle HUD_OVERRIDE = GENERAL.toggle("hudOverride", "Same style for every HUD mod", false);
	public static final Setting.Toggle HUD_BG = GENERAL.toggle("hudBg", "Background", true).showIf(() -> Mods.HUD_OVERRIDE.on());
	public static final Setting.Color HUD_BG_COLOR = GENERAL.color("hudBgColor", "Background color", "000000").showIf(() -> Mods.HUD_OVERRIDE.on() && Mods.HUD_BG.on());
	public static final Setting.Slider HUD_BG_OPACITY = GENERAL.slider("hudBgOpacity", "Background opacity", 45, 0, 100, 5, "%").showIf(() -> Mods.HUD_OVERRIDE.on() && Mods.HUD_BG.on());
	public static final Setting.Color HUD_TEXT = GENERAL.color("hudText", "Text color", "FFFFFF").showIf(() -> Mods.HUD_OVERRIDE.on());
	public static final Setting.Toggle HUD_SHADOW = GENERAL.toggle("hudShadow", "Text shadow", true).showIf(() -> Mods.HUD_OVERRIDE.on());
	public static final Setting.Choice HUD_BRACKETS = GENERAL.choice("hudBrackets", "Brackets", "None", "None", "[ ]", "( )", "< >").showIf(() -> Mods.HUD_OVERRIDE.on());
	public static final Setting.Header H_THEME = GENERAL.header("Menu colors");
	public static final Setting.Choice THEME = GENERAL.choice("theme2", "Theme", "Ash", "Ash", "Purple", "Blue", "Cyan", "Green", "Red", "Orange", "Pink", "Gold", "Mono", "Custom");
	public static final Setting.Color THEME_COLOR = GENERAL.color("themeColor", "Custom color", "6B6B73").showIf(() -> Mods.THEME.is("Custom"));
	public static final Setting.Header H_EMOTES = GENERAL.header("Emotes");
	public static final Setting.Toggle EMOTE_MUSIC = GENERAL.toggle("emoteMusic", "Dance music", true);
	public static final Setting.Toggle EMOTE_MUSIC_OTHERS = GENERAL.toggle("emoteMusicOthers", "Hear other players' dance music", true).showIf(() -> Mods.EMOTE_MUSIC.on());
	public static final Setting.Slider EMOTE_VOLUME = GENERAL.slider("emoteVolume", "Music volume", 80, 0, 100, 5, "%").showIf(() -> Mods.EMOTE_MUSIC.on());
	public static final Setting.Header H_SOCIAL = GENERAL.header("Status and friends");
	public static final Setting.Choice STATUS = GENERAL.choice("status", "Your status", "Online", "Online", "Idle", "Busy", "Offline");
	public static final Setting.Toggle AUTO_IDLE = GENERAL.toggle("autoIdle", "Go idle after 5 minutes away", true);
	public static final Setting.Toggle SHOW_SERVER = GENERAL.toggle("showServer", "Show friends which server you're on", true);
	public static final Setting.Toggle SOCIAL_TOASTS = GENERAL.toggle("toasts", "Message and request pop-ups", true);
	public static final Setting.Toggle SOCIAL_ONLINE_TOASTS = GENERAL.toggle("onlineToasts", "Pop-up when a friend comes online", true).showIf(() -> Mods.SOCIAL_TOASTS.on());
	public static final Setting.Header H_MENUS = GENERAL.header("Menus");
	public static final Setting.Toggle UI_CLICKS = GENERAL.toggle("clicks", "Click sounds", true);
	public static final Setting.Toggle UI_HOVER = GENERAL.toggle("hoverSounds", "Hover sounds", false);
	public static final Setting.Toggle UI_ANIM = GENERAL.toggle("animations", "Smooth animations", true);
	public static final Setting.Slider UI_ANIM_SPEED = GENERAL.slider("animSpeed", "Animation speed", 100, 50, 200, 10, "%").showIf(() -> Mods.UI_ANIM.on());
	public static final Setting.Choice BRAND_POS = GENERAL.choice("brandPos", "Surge Client label", "Bottom right", "Bottom right", "Under inventory");
	public static final Setting.Header H_CAMERA = GENERAL.header("Camera");
	public static final Setting.Toggle MIN_BOBBING = GENERAL.toggle("minBobbing", "Minimal view bobbing (only your hand moves)", false);
	public static final Setting.Header H_WINDOW = GENERAL.header("Window");
	public static final Setting.Choice FULLSCREEN_MODE = GENERAL.choice("fullscreenMode", "Fullscreen (F11)", "Exclusive", "Exclusive", "Borderless");
	public static final Setting.Toggle BORDERLESS_ACTIVE = GENERAL.toggle("borderlessActive", "Borderless active", false).showIf(() -> false);

	public static final SurgeMod TOGGLE_SPRINT = mod("toggle_sprint", "Toggle Sprint", Category.PVP, Items.SUGAR,
		"Sprint without holding the key. Tap your sprint key to switch it on or off.", false);
	private static boolean sprintToggled = true;

	public static boolean sprintToggled() { return sprintToggled; }

	public static void flipSprint() { sprintToggled = !sprintToggled; }

	private static boolean sneakToggled;

	public static boolean sneakToggled() { return sneakToggled; }

	public static void setSneak(boolean on) { sneakToggled = on; }

	public static final SurgeMod TOGGLE_SNEAK = mod("toggle_sneak", "Toggle Sneak", Category.PVP, Items.LEATHER_BOOTS,
		"Tap sneak to start sneaking, tap again to stop", false);

	public static final SurgeMod DAMAGE_TINT = mod("damage_tint", "Damage Tint", Category.PVP, Items.RED_DYE,
		"Recolor the flash players and mobs get when they're hit", false);
	public static final Setting.Color TINT_COLOR = DAMAGE_TINT.color("color", "Color", "FF0000");
	public static final Setting.Slider TINT_OPACITY = DAMAGE_TINT.slider("opacity", "Opacity", 30, 5, 95, 5, "%");
	public static final Setting.Toggle TINT_ARMOR = DAMAGE_TINT.toggle("armor", "Tint armor too", true);

	public static final SurgeMod APPLESKIN = mod("appleskin", "AppleSkin", Category.PVP, Items.GOLDEN_APPLE,
		"Shows saturation on your hunger bar and how much the food you hold will fill", true);
	public static final Setting.Toggle APPLE_SAT = APPLESKIN.toggle("saturation", "Saturation on the hunger bar", true);
	public static final Setting.Toggle APPLE_PREVIEW = APPLESKIN.toggle("preview", "Preview food you're holding", true);
	public static final Setting.Toggle APPLE_TEXT = APPLESKIN.toggle("numbers", "Numbers next to the bar", false);

	public static final SurgeMod HIT_SOUND = mod("hit_sound", "Hit Sounds", Category.PVP, Items.NOTE_BLOCK,
		"A satisfying sound when you hit a player or mob", false);
	public static final Setting.Choice HIT_SOUND_TYPE = HIT_SOUND.choice("sound", "Sound", "Ding", "Ding", "Pop", "Click");
	public static final Setting.Slider HIT_SOUND_VOL = HIT_SOUND.slider("volume", "Volume", 60, 10, 100, 5, "%");

	public static final SurgeMod SHIELD_FIX = mod("shield_fix", "Shield Fix", Category.PVP, Items.SHIELD,
		"Lower and shrink your shield so it doesn't block your view", false);
	public static final Setting.Slider SHIELD_LOWER = SHIELD_FIX.slider("lower", "Lower by", 60, 0, 100, 5, "%");
	public static final Setting.Slider SHIELD_BLOCKING = SHIELD_FIX.slider("blocking", "While blocking", 50, 0, 100, 5, "%");
	public static final Setting.Slider SHIELD_SCALE = SHIELD_FIX.slider("scale", "Size", 85, 40, 100, 5, "%");

	public static final SurgeMod TOTEMS = mod("totems", "Totem Tweaks", Category.PVP, Items.TOTEM_OF_UNDYING,
		"A pop counter in chat and a smaller (or no) totem animation", false);
	public static final Setting.Toggle TOTEM_CHAT = TOTEMS.toggle("chat", "Pop counter in chat", true);
	public static final Setting.Toggle TOTEM_CHAT_SELF = TOTEMS.toggle("chatSelf", "Count your own pops", true).showIf(() -> Mods.TOTEM_CHAT.on());
	public static final Setting.Toggle TOTEM_HIDE = TOTEMS.toggle("hide", "Hide the totem animation", false);
	public static final Setting.Slider TOTEM_SCALE = TOTEMS.slider("scale", "Animation size", 50, 20, 100, 5, "%").showIf(() -> !Mods.TOTEM_HIDE.on());

	public static final SurgeMod HURT_CAM = mod("hurt_cam", "Hurt Cam", Category.PVP, Items.IRON_SWORD,
		"Change how much your screen shakes when you take damage", false);
	public static final Setting.Slider HURT_STRENGTH = HURT_CAM.slider("strength", "Shake", 0, 0, 100, 5, "%");

	public static final SurgeMod LOCATOR_HEADS = mod("locator_heads", "Locator Bar Heads", Category.VISUAL, Items.PLAYER_HEAD,
		"Show each player's head on the locator bar instead of a dot", true);
	public static final Setting.Toggle LH_OUTLINE = LOCATOR_HEADS.toggle("outline", "Colored outline", true);

	public static final SurgeMod COLOR_GRADE = mod("color_grade", "Color Customizer", Category.VISUAL, Items.CYAN_DYE,
		"Change the colors of the whole game, like saturation, brightness, contrast and hue", false);
	public static final Setting.Choice CG_PRESET = COLOR_GRADE.choice("preset", "Preset", "Custom", "Custom", "Grayscale", "Vivid", "Warm", "Cold", "Sepia", "Noir", "Cinematic");
	public static final Setting.Slider CG_SATURATION = COLOR_GRADE.slider("saturation", "Saturation", 100, 0, 250, 5, "%").showIf(() -> CG_PRESET.is("Custom"));
	public static final Setting.Slider CG_BRIGHTNESS = COLOR_GRADE.slider("brightness", "Brightness", 100, 40, 180, 5, "%").showIf(() -> CG_PRESET.is("Custom"));
	public static final Setting.Slider CG_CONTRAST = COLOR_GRADE.slider("contrast", "Contrast", 100, 40, 200, 5, "%").showIf(() -> CG_PRESET.is("Custom"));
	public static final Setting.Slider CG_GAMMA = COLOR_GRADE.slider("gamma", "Gamma", 100, 40, 200, 5, "%").showIf(() -> CG_PRESET.is("Custom"));
	public static final Setting.Slider CG_HUE = COLOR_GRADE.slider("hue", "Hue shift", 0, -180, 180, 5, "\u00b0").showIf(() -> CG_PRESET.is("Custom"));
	public static final Setting.Color CG_TINT = COLOR_GRADE.color("tint", "Tint color", "FFB070").showIf(() -> CG_PRESET.is("Custom"));
	public static final Setting.Slider CG_TINT_AMOUNT = COLOR_GRADE.slider("tintAmount", "Tint strength", 0, 0, 100, 5, "%").showIf(() -> CG_PRESET.is("Custom"));
	public static final Setting.Slider CG_VIGNETTE = COLOR_GRADE.slider("vignette", "Vignette", 0, 0, 100, 5, "%").showIf(() -> CG_PRESET.is("Custom"));

	public static final SurgeMod CALCULATOR = mod("calculator", "Calculator", Category.UTILITY, Items.COMPARATOR,
		"A calculator you can open anywhere, set a key for it in Controls", false);
	public static final Setting.Button CALC_OPEN = CALCULATOR.button("Calculator", "Open", () -> net.minecraft.client.Minecraft.getInstance().setScreen(new gg.surge.client.gui.CalculatorScreen(net.minecraft.client.Minecraft.getInstance().screen)));

	public static final SurgeMod GUI_SCALES = mod("gui_scales", "GUI Modifier", Category.VISUAL, Items.SPYGLASS,
		"Resize your hotbar, chat, tab list, titles and inventories, and remove the dark inventory background", false);
	public static final Setting.Toggle GS_NO_TINT = GUI_SCALES.toggle("noTint", "Remove inventory background tint", false);
	public static final Setting.Slider GS_BLUR = GUI_SCALES.slider("blur", "Menu background blur", 5, 0, 10, 1, "");
	public static final Setting.Choice GS_BACKDROP = GUI_SCALES.choice("backdrop", "Inventory background", "None", "None", "Rain and thunder", "Snow", "Fireflies", "Starry night", "Aurora", "Sakura", "Embers");
	public static final Setting.Slider GS_BACKDROP_AMOUNT = GUI_SCALES.slider("backdropAmount", "Background amount", 100, 20, 200, 10, "%").showIf(() -> !GS_BACKDROP.get().equals("None"));
	public static final Setting.Slider GS_HOTBAR = GUI_SCALES.slider("hotbar", "Hotbar, health and hunger", 100, 50, 150, 5, "%");
	public static final Setting.Slider GS_CHAT = GUI_SCALES.slider("chat", "Chat", 100, 50, 150, 5, "%");
	public static final Setting.Slider GS_TAB = GUI_SCALES.slider("tab", "Tab list", 100, 50, 150, 5, "%");
	public static final Setting.Slider GS_TITLE = GUI_SCALES.slider("title", "Titles", 100, 40, 150, 5, "%");
	public static final Setting.Slider GS_SIDEBAR = GUI_SCALES.slider("sidebar", "Scoreboard", 100, 50, 150, 5, "%");
	public static final Setting.Choice GS_INVENTORY = GUI_SCALES.choice("inventory", "Inventories and chests", "Same as game", "Same as game", "1", "2", "3", "4", "5", "6");

	public static final SurgeMod CROSSHAIR = mod("crosshair", "Crosshair", Category.VISUAL, Items.TRIPWIRE_HOOK,
		"A clean custom crosshair with your own style and color", false);
	public static final Setting.Choice CROSS_STYLE = CROSSHAIR.choice("style", "Style", "Cross", "Cross", "Gap", "Dot", "Circle", "T");
	public static final Setting.Color CROSS_COLOR = CROSSHAIR.color("color", "Color", "FFFFFF");
	public static final Setting.Slider CROSS_SIZE = CROSSHAIR.slider("size", "Length", 4, 1, 10, 1, "px");
	public static final Setting.Slider CROSS_GAP = CROSSHAIR.slider("gap", "Gap", 2, 0, 8, 1, "px");
	public static final Setting.Slider CROSS_THICK = CROSSHAIR.slider("thickness", "Thickness", 1, 1, 4, 1, "px");
	public static final Setting.Toggle CROSS_OUTLINE = CROSSHAIR.toggle("outline", "Outline", true);
	public static final Setting.Toggle CROSS_DOT = CROSSHAIR.toggle("dot", "Center dot", false);
	public static final Setting.Toggle CROSS_THIRD = CROSSHAIR.toggle("thirdPerson", "Show in third person", false);

	public static final SurgeMod FULLBRIGHT = mod("fullbright", "Gamma", Category.VISUAL, Items.GLOWSTONE,
		"Turn brightness past the normal maximum to see in caves and at night", false);
	public static final Setting.Slider GAMMA_LEVEL = FULLBRIGHT.slider("level", "Brightness", 1000, 100, 1500, 50, "%");

	public static final SurgeMod LOW_FIRE = mod("low_fire", "Low Fire", Category.VISUAL, Items.FIRE_CHARGE,
		"Lower the fire overlay when you're burning", true);
	public static final Setting.Slider FIRE_HEIGHT = LOW_FIRE.slider("lower", "Lower by", 32, 0, 60, 2, "%");

	public static final SurgeMod VIEW_MODEL = mod("view_model", "View Model", Category.VISUAL, Items.DIAMOND_PICKAXE,
		"Move, turn and resize the items in your hands", false);
	public static final Setting.Header VM_H1 = VIEW_MODEL.header("Main hand");
	public static final Setting.Slider VM_MAIN_X = VIEW_MODEL.slider("mainX", "Left / right", 0, -1, 1, 0.02f, "");
	public static final Setting.Slider VM_MAIN_Y = VIEW_MODEL.slider("mainY", "Up / down", 0, -1, 1, 0.02f, "");
	public static final Setting.Slider VM_MAIN_Z = VIEW_MODEL.slider("mainZ", "Forward / back", 0, -1, 1, 0.02f, "");
	public static final Setting.Slider VM_MAIN_RX = VIEW_MODEL.slider("mainRX", "Tilt", 0, -180, 180, 5, "°");
	public static final Setting.Slider VM_MAIN_RY = VIEW_MODEL.slider("mainRY", "Turn", 0, -180, 180, 5, "°");
	public static final Setting.Slider VM_MAIN_RZ = VIEW_MODEL.slider("mainRZ", "Roll", 0, -180, 180, 5, "°");
	public static final Setting.Slider VM_MAIN_SCALE = VIEW_MODEL.slider("mainScale", "Size", 100, 30, 150, 5, "%");
	public static final Setting.Header VM_H2 = VIEW_MODEL.header("Off hand");
	public static final Setting.Slider VM_OFF_X = VIEW_MODEL.slider("offX", "Left / right", 0, -1, 1, 0.02f, "");
	public static final Setting.Slider VM_OFF_Y = VIEW_MODEL.slider("offY", "Up / down", 0, -1, 1, 0.02f, "");
	public static final Setting.Slider VM_OFF_Z = VIEW_MODEL.slider("offZ", "Forward / back", 0, -1, 1, 0.02f, "");
	public static final Setting.Slider VM_OFF_RX = VIEW_MODEL.slider("offRX", "Tilt", 0, -180, 180, 5, "°");
	public static final Setting.Slider VM_OFF_RY = VIEW_MODEL.slider("offRY", "Turn", 0, -180, 180, 5, "°");
	public static final Setting.Slider VM_OFF_RZ = VIEW_MODEL.slider("offRZ", "Roll", 0, -180, 180, 5, "°");
	public static final Setting.Slider VM_OFF_SCALE = VIEW_MODEL.slider("offScale", "Size", 100, 30, 150, 5, "%");

	public static final SurgeMod NAMETAGS = mod("nametags", "Nametags", Category.VISUAL, Items.NAME_TAG,
		"Show your own nametag in third person, and your country's flag if you want", false);
	public static final Setting.Toggle TAG_OWN = NAMETAGS.toggle("own", "Show your own nametag", true);
	public static final Setting.Toggle TAG_FLAG = NAMETAGS.toggle("flag", "Show your country flag (from where you connect)", false);
	public static final Setting.Toggle TAG_OTHERS_FLAGS = NAMETAGS.toggle("othersFlags", "Show other players' flags", true);

	public static final SurgeMod TNT_TIMER = mod("tnt_timer", "TNT Timer", Category.VISUAL, Items.TNT,
		"Shows how long until lit TNT explodes", false);

	public static final SurgeMod TAB_LIST = mod("tab_list", "Tab List", Category.VISUAL, Items.BOOK,
		"Ping as a number instead of bars, and an optional see-through tab list", false);
	public static final Setting.Toggle TAB_PING_NUMBER = TAB_LIST.toggle("pingNumber", "Ping as a number", true);
	public static final Setting.Toggle TAB_PING_COLOR = TAB_LIST.toggle("pingColor", "Color by ping", true).showIf(() -> Mods.TAB_PING_NUMBER.on());
	public static final Setting.Toggle TAB_NO_BG = TAB_LIST.toggle("noBackground", "Remove the background", false);

	public static final SurgeMod SERVER_REGION = mod("server_region", "Server Region", Category.UTILITY, Items.FILLED_MAP,
		"Shows where each server is (continent and country) in the server list", true);

	public static final SurgeMod MOTION_BLUR = mod("motion_blur", "Motion Blur", Category.VISUAL, Items.PHANTOM_MEMBRANE,
		"Smooth, cinematic blur when you move and turn", false);
	public static final Setting.Slider BLUR_STRENGTH = MOTION_BLUR.slider("strength", "Strength", 50, 20, 80, 15, "%");

	public static final SurgeMod HIT_PARTICLES = mod("hit_particles", "Hit Particles", Category.PVP, Items.NETHERITE_SWORD,
		"More hit particles every time you land a hit", false);
	public static final Setting.Slider HIT_PARTICLE_COUNT = HIT_PARTICLES.slider("amount", "Amount", 2, 1, 5, 1, "x");
	public static final Setting.Toggle HIT_PARTICLE_ALWAYS = HIT_PARTICLES.toggle("always", "Always show crits", true);

	public static final SurgeMod NO_BOBBING = mod("no_bobbing", "No View Bobbing", Category.VISUAL, Items.FEATHER,
		"Stop the camera bouncing while you walk", false);

	public static final SurgeMod FOV_EFFECTS = mod("fov_effects", "Static FOV", Category.VISUAL, Items.ENDER_PEARL,
		"Stop sprinting and speed effects from zooming your view", false);
	public static final Setting.Slider FOV_AMOUNT = FOV_EFFECTS.slider("amount", "FOV effects", 0, 0, 100, 5, "%");

	public static final SurgeMod WEATHER = mod("weather", "Weather Changer", Category.VISUAL, Items.LIGHTNING_ROD,
		"Pick the weather you see. Only changes it for you.", false);
	public static final Setting.Choice WEATHER_TYPE = WEATHER.choice("type", "Weather", "Clear", "Clear", "Rain", "Thunder");

	public static final SurgeMod OVERLAYS = mod("overlays", "Clean Overlays", Category.VISUAL, Items.CARVED_PUMPKIN,
		"Hide the pumpkin blur and the dark screen edges", false);
	public static final Setting.Toggle HIDE_PUMPKIN = OVERLAYS.toggle("pumpkin", "Hide pumpkin blur", true);
	public static final Setting.Toggle HIDE_VIGNETTE = OVERLAYS.toggle("vignette", "Hide vignette", true);

	public static final SurgeMod SCOREBOARD = mod("scoreboard", "Scoreboard", Category.VISUAL, Items.OAK_SIGN,
		"Resize or hide the sidebar scoreboard", false);
	public static final Setting.Toggle SCORE_HIDE = SCOREBOARD.toggle("hide", "Hide scoreboard", false);
	public static final Setting.Slider SCORE_SCALE = SCOREBOARD.slider("scale", "Scale", 100, 50, 150, 5, "%").showIf(() -> !Mods.SCORE_HIDE.on());

	public static final SurgeMod BOSSBAR = mod("bossbar", "Boss Bar", Category.VISUAL, Items.DRAGON_HEAD,
		"Resize or hide boss bars", false);
	public static final Setting.Toggle BOSS_HIDE = BOSSBAR.toggle("hide", "Hide boss bars", false);
	public static final Setting.Slider BOSS_SCALE = BOSSBAR.slider("scale", "Scale", 100, 50, 150, 5, "%").showIf(() -> !Mods.BOSS_HIDE.on());

	public static final SurgeMod TITLES = mod("titles", "Title Scale", Category.VISUAL, Items.NAME_TAG,
		"Shrink the big titles servers put on your screen", false);
	public static final Setting.Slider TITLE_SCALE = TITLES.slider("scale", "Scale", 60, 30, 150, 5, "%");

	public static final SurgeMod COSMETICS = mod("cosmetics", "Cosmetics", Category.VISUAL, Items.ELYTRA,
		"Wings, halos and capes from the wardrobe", true);
	public static final Setting.Toggle COS_OTHERS = COSMETICS.toggle("others", "Show other players' cosmetics", true);
	public static final Setting.Toggle COS_WINGS_ELYTRA = COSMETICS.toggle("hideWithElytra", "Hide wings while wearing an elytra", true);
	public static final Setting.Toggle COS_CAPE_ELYTRA = COSMETICS.toggle("capeElytra", "Your cape also changes your elytra", true);

	public static final SurgeMod MAIN_MENU = mod("main_menu", "Surge Main Menu", Category.VISUAL, Items.PAINTING,
		"Use Surge's main menu instead of the vanilla one", true);

	public static final SurgeMod ZOOM = mod("zoom", "Zoom", Category.UTILITY, Items.SPYGLASS,
		"Hold your zoom key (C) to zoom in. Scroll while zooming to adjust.", true);
	public static final Setting.Slider ZOOM_LEVEL = ZOOM.slider("level", "Zoom", 4, 2, 16, 0.5f, "x");
	public static final Setting.Toggle ZOOM_SCROLL = ZOOM.toggle("scroll", "Scroll to adjust", true);
	public static final Setting.Toggle ZOOM_SMOOTH_CAM = ZOOM.toggle("smoothCam", "Smooth camera", true);
	public static final Setting.Toggle ZOOM_ANIMATE = ZOOM.toggle("animate", "Smooth zoom animation", true);

	public static final SurgeMod FREELOOK = mod("freelook", "Freelook", Category.UTILITY, Items.ENDER_EYE,
		"Hold your freelook key (Left Alt) to look around without turning", true);
	public static final Setting.Choice FREELOOK_MODE = FREELOOK.choice("mode", "Key mode", "Hold", "Hold", "Toggle");
	public static final Setting.Choice FREELOOK_VIEW = FREELOOK.choice("view", "Camera", "Behind", "Behind", "Front");
	public static final Setting.Toggle FREELOOK_INVERT = FREELOOK.toggle("invert", "Invert up and down", false);

	public static final SurgeMod CHAT = mod("chat", "Chat", Category.UTILITY, Items.WRITABLE_BOOK,
		"Timestamps, stacked repeat messages, mentions and a longer chat history", false);
	public static final Setting.Toggle CHAT_STACK = CHAT.toggle("stack", "Stack repeated messages, like [x14]", true);
	public static final Setting.Toggle CHAT_MENTION = CHAT.toggle("mention", "Play a ding when someone says your name", true);
	public static final Setting.Toggle CHAT_HIDE_JOINS = CHAT.toggle("hideJoins", "Hide join and leave messages", false);
	public static final Setting.Toggle CHAT_LONG = CHAT.toggle("longHistory", "Keep 1000 messages instead of 100", true);
	public static final Setting.Toggle CHAT_TIME = CHAT.toggle("timestamps", "Timestamps", true);
	public static final Setting.Toggle CHAT_24H = CHAT.toggle("24h", "24-hour time", true).showIf(() -> Mods.CHAT_TIME.on());
	public static final Setting.Toggle CHAT_SECONDS = CHAT.toggle("seconds", "Show seconds", false).showIf(() -> Mods.CHAT_TIME.on());

	public static final SurgeMod WAYPOINTS = mod("waypoints", "Waypoints", Category.UTILITY, Items.LODESTONE,
		"Mark places and see them through walls with their distance. Add one with the Waypoint key.", true);
	public static final Setting.Toggle WP_WORLD = WAYPOINTS.toggle("world", "Show markers in the world", true);
	public static final Setting.Choice WP_MARKER = WAYPOINTS.choice("marker", "Marker", "Diamond", "Diamond", "Dot", "Pin", "None").showIf(() -> Mods.WP_WORLD.on());
	public static final Setting.Choice WP_LABEL = WAYPOINTS.choice("label", "Name and distance", "When near", "Always", "When near", "When looking", "Never").showIf(() -> Mods.WP_WORLD.on());
	public static final Setting.Slider WP_NEAR = WAYPOINTS.slider("near", "Near means within", 75, 10, 500, 5, " blocks").showIf(() -> Mods.WP_WORLD.on() && Mods.WP_LABEL.is("When near"));
	public static final Setting.Toggle WP_DISTANCE = WAYPOINTS.toggle("distance", "Show distance", true).showIf(() -> Mods.WP_WORLD.on() && !Mods.WP_LABEL.is("Never"));
	public static final Setting.Toggle WP_GLOW = WAYPOINTS.toggle("glow", "Glowing block outline", true).showIf(() -> Mods.WP_WORLD.on());
	public static final Setting.Slider WP_GLOW_RANGE = WAYPOINTS.slider("glowRange", "Outline within", 75, 10, 300, 5, " blocks").showIf(() -> Mods.WP_WORLD.on() && Mods.WP_GLOW.on());
	public static final Setting.Toggle WP_BEAM = WAYPOINTS.toggle("beam", "Light beam", true).showIf(() -> Mods.WP_WORLD.on());
	public static final Setting.Slider WP_SCALE = WAYPOINTS.slider("scale", "Label size", 100, 50, 200, 10, "%").showIf(() -> Mods.WP_WORLD.on());
	public static final Setting.Slider WP_RANGE = WAYPOINTS.slider("range", "Hide farther than", 5000, 100, 30000, 100, " blocks").showIf(() -> Mods.WP_WORLD.on());
	public static final Setting.Toggle WP_DEATH = WAYPOINTS.toggle("death", "Waypoint where you die", true);

	public static final SurgeMod DISCORD = mod("discord", "Discord Status", Category.UTILITY, Items.AMETHYST_CLUSTER,
		"Show what you're playing on your Discord profile", true);
	public static final Setting.Toggle DISCORD_SERVER = DISCORD.toggle("showIp", "Show the server address", false);
	public static final Setting.Toggle DISCORD_PACK = DISCORD.toggle("pack", "Show the modpack name", true);

	public static final SurgeMod AUTO_GG = mod("auto_gg", "AutoGG", Category.UTILITY, Items.CAKE,
		"Says gg in chat when you kill someone or a game ends", false);
	public static final Setting.Choice GG_TEXT = AUTO_GG.choice("text", "Message", "gg", "gg", "GG", "gg wp", "Good game");
	public static final Setting.Slider GG_DELAY = AUTO_GG.slider("delay", "Delay", 1, 0, 5, 0.5f, "s");
	public static final Setting.Toggle GG_KILLS = AUTO_GG.toggle("kills", "Also when you kill someone", true);

	public static final SurgeMod COMMAND_KEYS = mod("command_keys", "Command Keys", Category.UTILITY, Items.COMMAND_BLOCK,
		"Bind keys that send a chat message or command, like /home or gg", true);

	public static final SurgeMod CLIPS = mod("clips", "Clips", Category.UTILITY, Items.SPYGLASS,
		"Keeps the last few seconds of gameplay. Press the Save clip key (F8) to save them as a video in your clips folder", false);
	public static final Setting.Slider CLIP_LENGTH = CLIPS.slider("length", "Clip length", 15, 10, 30, 5, "s");
	public static final Setting.Choice CLIP_FPS = CLIPS.choice("fps", "Frame rate", "30", "20", "30", "60", "90", "120");
	public static final Setting.Choice CLIP_RES = CLIPS.choice("res", "Resolution", "720p", "480p", "720p", "1080p", "Full");
	public static final Setting.Toggle CLIP_SMART = CLIPS.toggle("smart", "Record less when your FPS drops", true);
	public static final Setting.Slider CLIP_QUALITY = CLIPS.slider("quality", "Quality", 70, 30, 95, 5, "%");
	public static final Setting.Button CLIP_FOLDER = CLIPS.button("Clips folder", "Open", () -> {
		try {
			java.nio.file.Path dir = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("clips");
			java.nio.file.Files.createDirectories(dir);
			net.minecraft.util.Util.getPlatform().openPath(dir);
		} catch (Exception ignored) { }
	});

	public static final SurgeMod CUSTOM_SOUNDS = mod("custom_sounds", "Custom Sounds", Category.UTILITY, Items.NOTE_BLOCK,
		"Replace any game sound with a built in sound or your own mp3, wav or ogg files", false);
	public static final Setting.Button CS_OPEN = CUSTOM_SOUNDS.button("Pick sounds", "Open", () -> {
		net.minecraft.client.Minecraft mc = mc();
		mc.setScreen(new gg.surge.client.gui.CustomSoundsScreen(mc.screen));
	});
	public static final Setting.Button CS_FOLDER = CUSTOM_SOUNDS.button("Your sounds folder", "Open", () -> {
		try { net.minecraft.util.Util.getPlatform().openPath(gg.surge.client.features.CustomSounds.folder()); } catch (Exception ignored) { }
	});

	public static final SurgeMod LOW_HEALTH = mod("low_health", "Low Health Warning", Category.PVP, Items.RED_DYE,
		"Your screen edges pulse red and you hear a heartbeat when your health gets low", false);
	public static final Setting.Slider LH_HEALTH = LOW_HEALTH.slider("health", "Warn at or below", 6, 2, 14, 1, " HP");
	public static final Setting.Toggle LH_VIGNETTE = LOW_HEALTH.toggle("vignette", "Red screen edges", true);
	public static final Setting.Toggle LH_SOUND = LOW_HEALTH.toggle("sound", "Heartbeat sound", true);

	public static final SurgeMod ITEM_PICKUPS = mod("item_pickups", "Item Pickups", Category.UTILITY, Items.HOPPER,
		"Shows what you just picked up next to your hotbar", false);

	public static final SurgeMod DEATH_COORDS = mod("death_coords", "Death Coordinates", Category.UTILITY, Items.SKELETON_SKULL,
		"Tells you in chat where you died, click the coordinates to copy them", true);

	public static final SurgeMod TOOLTIPS = mod("tooltips", "Better Tooltips", Category.UTILITY, Items.BOOK,
		"Adds durability, food values and stack size to item tooltips", true);
	public static final Setting.Toggle TT_DURABILITY = TOOLTIPS.toggle("durability", "Durability numbers", true);
	public static final Setting.Toggle TT_FOOD = TOOLTIPS.toggle("food", "Food and saturation", true);
	public static final Setting.Toggle TT_STACK = TOOLTIPS.toggle("stack", "Stack size", false);

	public static final SurgeMod NICK_HIDER = mod("nick_hider", "Nick Hider", Category.VISUAL, Items.LEATHER_HELMET,
		"Hide your name in chat, the tab list and your nametag, great for recording", false);
	public static final Setting.Choice NICK_NAME = NICK_HIDER.choice("nick", "Show me as", "You", "You", "Player", "Surge", "Hidden", "Me");

	public static final SurgeMod TRAILS = mod("trails", "Projectile Trails", Category.PVP, Items.SPECTRAL_ARROW,
		"Particle trails behind arrows, pearls, tridents and snowballs", false);
	public static final Setting.Choice TRAIL_TYPE = TRAILS.choice("type", "Particle", "Flame", "Flame", "Soul", "Crit", "Magic", "Hearts", "Notes", "End rod", "Cherry", "Smoke");
	public static final Setting.Toggle TRAIL_OWN = TRAILS.toggle("own", "Only my projectiles", true);
	public static final Setting.Slider TRAIL_AMOUNT = TRAILS.slider("amount", "Amount", 2, 1, 5, 1, "x");

	public static final SurgeMod TIME_CHANGER = mod("time_changer", "Time Changer", Category.VISUAL, Items.CLOCK,
		"Pick the time of day you see. Only changes it for you.", false);
	public static final Setting.Choice TIME_PRESET = TIME_CHANGER.choice("preset", "Time", "Day", "Sunrise", "Day", "Noon", "Sunset", "Night", "Midnight", "Custom");
	public static final Setting.Slider TIME_CUSTOM = TIME_CHANGER.slider("custom", "Custom time", 6000, 0, 23999, 250, "").showIf(() -> Mods.TIME_PRESET.is("Custom"));
	public static final Setting.Toggle TIME_CYCLE = TIME_CHANGER.toggle("cycle", "Let the time move", false);

	public static final SurgeMod DROP_PROTECT = mod("drop_protect", "Drop Protection", Category.UTILITY, Items.CHEST,
		"Stops you dropping valuable items by accident: press drop twice to really drop them", true);
	public static final Setting.Toggle DROP_ENCHANTED = DROP_PROTECT.toggle("enchanted", "Enchanted items", true);
	public static final Setting.Toggle DROP_RARE = DROP_PROTECT.toggle("rare", "Netherite, diamond gear, totems and elytras", true);
	public static final Setting.Toggle DROP_NAMED = DROP_PROTECT.toggle("named", "Renamed items", true);
	public static final Setting.Toggle DROP_ALL = DROP_PROTECT.toggle("all", "Every item", false);

	public static final SurgeMod LOD = mod("distant_terrain", "Distant Terrain (Beta)", Category.VISUAL, Items.FILLED_MAP,
		"See land far past your render distance, up to 1024 chunks. It shows the places you have already been to", true);
	public static final Setting.Slider LOD_DISTANCE = LOD.slider("distance", "Distance", 256, 64, 1024, 32, " chunks");
	public static final Setting.Choice LOD_QUALITY = LOD.choice("quality", "Detail", "High", "Low", "Medium", "High", "Ultra", "Full");
	public static final Setting.Toggle LOD_FOG = LOD.toggle("fog", "Keep normal fog", false);
	public static final Setting.Toggle LOD_SAVE = LOD.toggle("save", "Remember terrain between sessions", true);
	public static final Setting.Button LOD_FORGET = LOD.button("Saved terrain for this world", "Forget", gg.surge.client.lod.Lod::forget);

	public static final SurgeMod BG_FPS = mod("background_fps", "Background FPS", Category.UTILITY, Items.REDSTONE_LAMP,
		"Lowers FPS when you tab out so your PC stays cool and other apps run smoothly", true);
	public static final Setting.Slider BG_FPS_LIMIT = BG_FPS.slider("limit", "FPS when tabbed out", 15, 5, 60, 5, "");

	public static final SurgeMod HITBOXES = mod("hitboxes", "Hitboxes", Category.PVP, Items.IRON_SWORD,
		"Custom colors for F3 + B hitboxes, with combat colors when someone gets hit or is in your crosshair", true);
	public static final Setting.Color HB_COLOR = HITBOXES.color("color", "Hitbox color", "FFFFFF");
	public static final Setting.Toggle HB_COMBAT = HITBOXES.toggle("combat", "Combat colors", true);
	public static final Setting.Color HB_HIT = HITBOXES.color("hit", "When hurt", "FF4040").showIf(() -> Mods.HB_COMBAT.on());
	public static final Setting.Color HB_TARGET = HITBOXES.color("target", "In your crosshair", "40FF7A").showIf(() -> Mods.HB_COMBAT.on());
	public static final Setting.Toggle HB_EYES = HITBOXES.toggle("eyes", "Show eye line", true);
	public static final Setting.Toggle HB_PLAYERS = HITBOXES.toggle("players", "Only players", false);

	public static final SurgeMod GLINT = mod("glint", "Glint Customizer", Category.VISUAL, Items.ENCHANTED_BOOK,
		"Change the color, speed and strength of the enchantment shine", false);
	public static final Setting.Toggle GLINT_TINT = GLINT.toggle("tint", "Custom color", true);
	public static final Setting.Color GLINT_COLOR = GLINT.color("color", "Color", "FF55FF").showIf(() -> Mods.GLINT_TINT.on());
	public static final Setting.Slider GLINT_SPEED = GLINT.slider("speed", "Speed", 50, 0, 100, 5, "%");
	public static final Setting.Slider GLINT_STRENGTH = GLINT.slider("strength", "Strength", 75, 0, 100, 5, "%");

	public static final SurgeMod FOG = mod("fog", "Fog Customizer", Category.VISUAL, Items.WHITE_STAINED_GLASS,
		"Pick which fog you see, how thick it is and what color it has", false);
	public static final Setting.Toggle FOG_NONE = FOG.toggle("none", "Remove all fog", false);
	public static final Setting.Header H_FOG_TYPES = FOG.header("Fog types");
	public static final Setting.Toggle FOG_WORLD = FOG.toggle("world", "Distance fog", true).showIf(() -> !Mods.FOG_NONE.on());
	public static final Setting.Slider FOG_WORLD_DIST = FOG.slider("worldDist", "Distance fog range", 100, 25, 400, 5, "%").showIf(() -> Mods.FOG_WORLD.on() && !Mods.FOG_NONE.on());
	public static final Setting.Toggle FOG_WATER = FOG.toggle("water", "Water fog", true).showIf(() -> !Mods.FOG_NONE.on());
	public static final Setting.Slider FOG_WATER_DIST = FOG.slider("waterDist", "Water fog range", 100, 25, 400, 5, "%").showIf(() -> Mods.FOG_WATER.on() && !Mods.FOG_NONE.on());
	public static final Setting.Toggle FOG_LAVA = FOG.toggle("lava", "Lava fog", true).showIf(() -> !Mods.FOG_NONE.on());
	public static final Setting.Toggle FOG_SNOW = FOG.toggle("snow", "Powder snow fog", true).showIf(() -> !Mods.FOG_NONE.on());
	public static final Setting.Header H_FOG_COLOR = FOG.header("Color");
	public static final Setting.Toggle FOG_TINT = FOG.toggle("tint", "Custom fog color", false);
	public static final Setting.Color FOG_COLOR = FOG.color("color", "Fog color", "B483FF").showIf(() -> Mods.FOG_TINT.on());

	public static final SurgeMod DURABILITY = mod("durability", "Durability Warning", Category.UTILITY, Items.DAMAGED_ANVIL,
		"Warns you when your armor or the item in your hand is about to break", true);
	public static final Setting.Slider DURA_PERCENT = DURABILITY.slider("percent", "Warn below", 10, 2, 50, 1, "%");
	public static final Setting.Toggle DURA_SOUND = DURABILITY.toggle("sound", "Play a sound", true);
	public static final Setting.Toggle DURA_ARMOR = DURABILITY.toggle("armor", "Check armor", true);
	public static final Setting.Toggle DURA_HANDS = DURABILITY.toggle("hands", "Check held items", true);

	public static final SurgeMod BLOCK_OUTLINE = mod("block_outline", "Block Outline", Category.VISUAL, Items.GLASS,
		"Change the color and thickness of the outline around the block you look at", false);
	public static final Setting.Color BO_COLOR = BLOCK_OUTLINE.color("color", "Color", "FFFFFF");
	public static final Setting.Slider BO_ALPHA = BLOCK_OUTLINE.slider("alpha", "Opacity", 70, 10, 100, 5, "%");
	public static final Setting.Slider BO_WIDTH = BLOCK_OUTLINE.slider("width", "Thickness", 3, 1, 10, 0.5f, "");

	public static final SurgeMod SNAPLOOK = mod("snaplook", "Snaplook", Category.VISUAL, Items.ENDER_EYE,
		"Hold X to quickly look at yourself, let go to go back", false);
	public static final Setting.Choice SL_VIEW = SNAPLOOK.choice("view", "View", "Front", "Front", "Back");

	public static final SurgeMod AUTO_PERSPECTIVE = mod("auto_perspective", "Auto Perspective", Category.VISUAL, Items.ELYTRA,
		"Switches to third person while you fly with an elytra or ride something", false);
	public static final Setting.Toggle AP_ELYTRA = AUTO_PERSPECTIVE.toggle("elytra", "When flying with an elytra", true);
	public static final Setting.Toggle AP_RIDING = AUTO_PERSPECTIVE.toggle("riding", "When riding a horse, boat or minecart", true);

	public static final SurgeMod ITEM_PHYSICS = mod("item_physics", "Item Physics", Category.VISUAL, Items.APPLE,
		"Dropped items lie flat on the ground instead of floating and spinning", false);

	public static final SurgeMod SHINY_POTS = mod("shiny_pots", "Shiny Pots", Category.VISUAL, Items.POTION,
		"Potions shine with the enchantment glint like they used to", false);

	public static final SurgeMod MOB_HEALTH = mod("mob_health", "Mob Health", Category.VISUAL, Items.GLISTERING_MELON_SLICE,
		"Shows the health of mobs you can see above their heads", false);
	public static final Setting.Choice MH_STYLE = MOB_HEALTH.choice("style", "Style", "Hearts", "Hearts", "Number", "Bar");
	public static final Setting.Slider MH_RANGE = MOB_HEALTH.slider("range", "Range", 16, 6, 48, 2, " blocks");
	public static final Setting.Toggle MH_HURT = MOB_HEALTH.toggle("hurt", "Only hurt mobs", false);


	public static final SurgeMod LOOT_BEAMS = mod("loot_beams", "Loot Beams", Category.VISUAL, Items.BEACON,
		"Beams of light above dropped items you can see, colored by rarity", false);
	public static final Setting.Toggle LB_RARE = LOOT_BEAMS.toggle("rare", "Only rare and enchanted items", false);
	public static final Setting.Slider LB_RANGE = LOOT_BEAMS.slider("range", "Range", 24, 8, 64, 4, " blocks");

	public static final SurgeMod DESPAWN_TIMER = mod("despawn_timer", "Item Despawn Timer", Category.UTILITY, Items.CLOCK,
		"Shows how long dropped items have left before they disappear", false);
	public static final Setting.Slider DT_RANGE = DESPAWN_TIMER.slider("range", "Range", 10, 4, 32, 2, " blocks");

	public static final SurgeMod LIGHT_OVERLAY = mod("light_overlay", "Light Level Overlay", Category.UTILITY, Items.TORCH,
		"Marks the blocks around you where mobs can spawn at night", false);
	public static final Setting.Slider LO_RANGE = LIGHT_OVERLAY.slider("range", "Range", 8, 4, 16, 1, " blocks");
	public static final Setting.Toggle LO_NUMBERS = LIGHT_OVERLAY.toggle("numbers", "Show light numbers", false);
	public static final Setting.Toggle LO_DAY = LIGHT_OVERLAY.toggle("day", "Count sunlight", false);

	public static final SurgeMod TOASTS = mod("toasts", "Toast Control", Category.UTILITY, Items.BREAD,
		"Hide the popups in the top right corner of the screen", false);
	public static final Setting.Toggle TOAST_ADV = TOASTS.toggle("advancements", "Hide advancements", true);
	public static final Setting.Toggle TOAST_RECIPE = TOASTS.toggle("recipes", "Hide new recipes", true);
	public static final Setting.Toggle TOAST_TUTORIAL = TOASTS.toggle("tutorial", "Hide tutorial hints", true);
	public static final Setting.Toggle TOAST_SYSTEM = TOASTS.toggle("system", "Hide system messages", false);

	public static final SurgeMod RECONNECT = mod("reconnect", "Reconnect", Category.UTILITY, Items.RECOVERY_COMPASS,
		"Adds a Reconnect button when you get disconnected from a server", true);
	public static final Setting.Toggle RC_AUTO = RECONNECT.toggle("auto", "Reconnect by itself", false);
	public static final Setting.Slider RC_DELAY = RECONNECT.slider("delay", "Wait before reconnecting", 10, 5, 60, 5, "s").showIf(() -> RC_AUTO.on());

	public static final SurgeMod PROFILES = mod("profiles", "Profiles", Category.UTILITY, Items.BOOKSHELF,
		"Save your whole setup (mods, HUD layout, settings) and switch between them, or per server", true);
	static { PROFILES.toggleable = false; }
	public static final Setting.Toggle PROFILE_PER_SERVER = PROFILES.toggle("perServer", "Remember a setup for each server", false);

	static {
		DAMAGE_TINT.onToggle(on -> DamageTint.apply(mc())).onChange(() -> DamageTint.apply(mc()));
		FULLBRIGHT.onToggle(on -> Fullbright.apply(mc())).onChange(() -> Fullbright.apply(mc()));
		TOGGLE_SNEAK.onToggle(on -> OptionTweaks.apply(mc()));
		HURT_CAM.onToggle(on -> OptionTweaks.apply(mc())).onChange(() -> OptionTweaks.apply(mc()));
		NO_BOBBING.onToggle(on -> OptionTweaks.apply(mc()));
		FOV_EFFECTS.onToggle(on -> OptionTweaks.apply(mc())).onChange(() -> OptionTweaks.apply(mc()));
		OVERLAYS.onToggle(on -> OptionTweaks.apply(mc())).onChange(() -> OptionTweaks.apply(mc()));
		CHAT.onToggle(on -> OptionTweaks.apply(mc()));
		WAYPOINTS.button("Add waypoint", "Add here", () -> Waypoints.addHere(mc()));
		GLINT.onToggle(on -> gg.surge.client.features.Extras.applyGlint(mc())).onChange(() -> gg.surge.client.features.Extras.applyGlint(mc()));
		GUI_SCALES.onToggle(on -> gg.surge.client.features.GuiScales.refresh()).onChange(gg.surge.client.features.GuiScales::refresh);
		var cfg = gg.surge.client.config.SurgeConfig.get();
		if (cfg.defaultsVersion < 2) {
			for (SurgeMod m : new SurgeMod[]{SERVER_REGION, CLIPS, DISCORD, COMMAND_KEYS, COSMETICS}) m.state().enabled = true;
			cfg.defaultsVersion = 2;
			gg.surge.client.config.SurgeConfig.save();
		}
	}

	public static List<SurgeMod> all() {
		List<SurgeMod> out = new ArrayList<>(HudModules.ALL);
		out.addAll(FEATURES);
		return out;
	}

	public static SurgeMod byId(String id) {
		for (SurgeMod m : all()) if (m.id.equals(id)) return m;
		return null;
	}

	public static HudModule hud(String id) {
		return HudModules.byId(id);
	}
}
