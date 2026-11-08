package gg.surge.legacy.feature;

import gg.surge.legacy.Keys;
import gg.surge.legacy.mod.Category;
import gg.surge.legacy.mod.Mod;
import gg.surge.legacy.mod.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.particle.ParticleType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Features {
	public static final List<Mod> ALL = new ArrayList<Mod>();

	private Features() {}

	private static <M extends Mod> M add(M m) {
		ALL.add(m);
		return m;
	}

	public static final Mod SPRINT = add(new Mod("togglesprint", "Toggle Sprint", Category.PVP, new ItemStack(Items.FEATHER), "Press sprint or sneak once to keep it on", true));
	public static final Setting.Toggle SPRINT_ON = SPRINT.toggle("sprint", "Toggle sprint", true);
	public static final Setting.Toggle SNEAK_ON = SPRINT.toggle("sneak", "Toggle sneak", false);
	public static final Setting.Toggle ALWAYS_SPRINT = SPRINT.toggle("always", "Always sprint when moving", false).showIf(() -> SPRINT_ON.on());

	public static final Mod ZOOM = add(new Mod("zoom", "Zoom", Category.VISUAL, new ItemStack(Items.ENDER_PEARL), "Hold C to zoom in. Scroll to change how far", true));
	public static final Setting.Slider ZOOM_LEVEL = ZOOM.slider("level", "Zoom amount", 4, 2, 12, 0.5f, "x");
	public static final Setting.Toggle ZOOM_SCROLL = ZOOM.toggle("scroll", "Scroll to adjust", true);
	public static final Setting.Toggle ZOOM_SMOOTH = ZOOM.toggle("smooth", "Smooth zoom", true);
	public static final Setting.Toggle ZOOM_CINEMATIC = ZOOM.toggle("cinematic", "Smooth camera while zoomed", false);

	public static final Mod FREELOOK = add(new Mod("freelook", "Freelook", Category.VISUAL, new ItemStack(Items.EYE_OF_ENDER), "Hold Left Alt to look around without turning", true));
	public static final Setting.Toggle FREELOOK_TOGGLE = FREELOOK.toggle("toggle", "Press to toggle instead of hold", false);
	public static final Setting.Toggle FREELOOK_INVERT = FREELOOK.toggle("invert", "Invert vertical look", false);

	public static final Mod FULLBRIGHT = add(new Mod("fullbright", "Fullbright", Category.VISUAL, new ItemStack(Items.GLOWSTONE_DUST), "See clearly in the dark", false));

	public static final Mod HURTCAM = add(new Mod("hurtcam", "No Hurt Cam", Category.PVP, new ItemStack(Items.GOLDEN_APPLE), "Stops the camera shaking when you take damage", false));

	public static final Mod OUTLINE = add(new Mod("outline", "Block Outline", Category.VISUAL, new ItemStack(Blocks.GLASS), "Change the color and thickness of the block outline", false));
	public static final Setting.Color OUTLINE_COLOR = OUTLINE.color("color", "Color", "FFFFFF");
	public static final Setting.Slider OUTLINE_ALPHA = OUTLINE.slider("alpha", "Opacity", 60, 10, 100, 5, "%");
	public static final Setting.Slider OUTLINE_WIDTH = OUTLINE.slider("width", "Thickness", 2.5f, 1, 8, 0.5f, "");

	public static final Mod TIME = add(new Mod("time", "Time Changer", Category.VISUAL, new ItemStack(Items.CLOCK), "Pick the time of day you see", false));
	public static final Setting.Slider TIME_VALUE = TIME.slider("time", "Time", 6000, 0, 23999, 500, "");

	public static final Mod WEATHER = add(new Mod("weather", "Clear Weather", Category.VISUAL, new ItemStack(Items.WATER_BUCKET), "Hides rain and snow on your screen", false));

	public static final Mod PARTICLES = add(new Mod("particles", "Particles", Category.PVP, new ItemStack(Items.BLAZE_POWDER), "More hit particles so every hit feels strong", false));
	public static final Setting.Slider PARTICLE_COUNT = PARTICLES.slider("count", "Extra particles", 2, 1, 10, 1, "x");
	public static final Setting.Toggle PARTICLE_SHARP = PARTICLES.toggle("sharp", "Always show sharpness sparks", true);
	public static final Setting.Toggle PARTICLE_CRIT = PARTICLES.toggle("crit", "Always show critical sparks", false);

	public static final Mod NICK = add(new Mod("nickhider", "Nick Hider", Category.UTILITY, new ItemStack(Items.NAME_TAG), "Hides your name on screen for videos and streams", false));
	public static final Setting.Choice NICK_NAME = NICK.choice("name", "Show as", "You", "You", "Player", "Surge");

	public static final Mod SCOREBOARD = add(new Mod("scoreboard", "Scoreboard", Category.VISUAL, new ItemStack(Items.PAPER), "Hide the red numbers and change the sidebar", false));
	public static final Setting.Toggle SB_NUMBERS = SCOREBOARD.toggle("numbers", "Hide red numbers", true);
	public static final Setting.Toggle SB_HIDE = SCOREBOARD.toggle("hide", "Hide the sidebar completely", false);

	public static final Mod AUTOGG = add(new Mod("autogg", "Auto GG", Category.UTILITY, new ItemStack(Items.GOLD_INGOT), "Says gg when a game ends", false));
	public static final Setting.Choice GG_MESSAGE = AUTOGG.choice("message", "Message", "gg", "gg", "GG", "Good game", "gg wp");
	public static final Setting.Toggle GG_ALL = AUTOGG.toggle("all", "Send in all chat (/ac)", true);

	public static final Mod CHAT = add(new Mod("chat", "Chat Timestamps", Category.UTILITY, new ItemStack(Items.BOOK), "Shows the time next to every chat message", false));
	public static final Setting.Toggle CHAT_SECONDS = CHAT.toggle("seconds", "Show seconds", false);
	public static final Setting.Toggle CHAT_24 = CHAT.toggle("24h", "24-hour time", true);

	public static boolean sprintToggled, sneakToggled;
	private static boolean freelookOn, freelookLatch;
	private static int savedPerspective;
	public static float camYaw, camPitch;
	private static float zoomCurrent = 1f, zoomExtra;
	private static boolean savedSmooth, smoothSet;
	private static float savedGamma = -1;
	private static long ggAt;

	public static void tick(MinecraftClient mc) {
		ClientPlayerEntity p = mc.player;
		if (p == null) return;
		if (SPRINT.enabled()) {
			while (mc.options.sprintKey.wasPressed()) if (SPRINT_ON.on()) sprintToggled = !sprintToggled;
			while (mc.options.sneakKey.wasPressed()) if (SNEAK_ON.on()) sneakToggled = !sneakToggled;
			boolean moving = mc.options.forwardKey.isPressed();
			if ((sprintToggled || ALWAYS_SPRINT.on() && SPRINT_ON.on()) && moving && !p.isSneaking() && p.getHungerManager().getFoodLevel() > 6 && !p.isUsingItem() && mc.currentScreen == null) p.setSprinting(true);
			if (SNEAK_ON.on() && sneakToggled && mc.currentScreen == null) KeyBinding.setKeyPressed(mc.options.sneakKey.getCode(), true);
		} else {
			sprintToggled = false;
			sneakToggled = false;
		}
		if (FULLBRIGHT.enabled()) {
			if (savedGamma < 0) savedGamma = mc.options.gamma;
			mc.options.gamma = 16f;
		} else if (savedGamma >= 0) {
			mc.options.gamma = savedGamma;
			savedGamma = -1;
		}
		if (TIME.enabled() && mc.world != null) mc.world.setTimeOfDay(TIME_VALUE.asInt());
		boolean flKey = FREELOOK.enabled() && Keys.FREELOOK.isPressed() && mc.currentScreen == null;
		if (FREELOOK_TOGGLE.on()) {
			if (flKey && !freelookLatch) setFreelook(mc, !freelookOn);
			freelookLatch = flKey;
		} else if (flKey != freelookOn) setFreelook(mc, flKey);
		if (freelookOn && !FREELOOK.enabled()) setFreelook(mc, false);
		if (ggAt > 0 && System.currentTimeMillis() >= ggAt) {
			ggAt = 0;
			p.sendChatMessage((GG_ALL.on() ? "/ac " : "") + GG_MESSAGE.get());
		}
	}

	private static void setFreelook(MinecraftClient mc, boolean on) {
		if (on == freelookOn) return;
		freelookOn = on;
		Entity cam = mc.getCameraEntity();
		if (on) {
			savedPerspective = mc.options.perspective;
			if (cam != null) { camYaw = cam.yaw; camPitch = cam.pitch; }
			if (mc.options.perspective == 0) mc.options.perspective = 1;
		} else {
			mc.options.perspective = savedPerspective;
		}
	}

	public static boolean freelook() {
		return freelookOn;
	}

	public static boolean look(float dx, float dy) {
		if (!freelookOn) return false;
		camYaw += dx * 0.15f;
		camPitch = MathHelper.clamp(camPitch + (FREELOOK_INVERT.on() ? dy : -dy) * 0.15f, -90f, 90f);
		return true;
	}

	public static boolean zooming() {
		MinecraftClient mc = MinecraftClient.getInstance();
		return ZOOM.enabled() && Keys.ZOOM.isPressed() && mc.currentScreen == null;
	}

	public static float fov(float fov) {
		MinecraftClient mc = MinecraftClient.getInstance();
		boolean z = zooming();
		float target = z ? Math.max(1f, ZOOM_LEVEL.get() + zoomExtra) : 1f;
		if (!z) zoomExtra = 0;
		if (ZOOM_SMOOTH.on()) zoomCurrent += (target - zoomCurrent) * 0.25f;
		else zoomCurrent = target;
		if (Math.abs(zoomCurrent - target) < 0.01f) zoomCurrent = target;
		if (z && ZOOM_CINEMATIC.on()) {
			if (!smoothSet) { savedSmooth = mc.options.smoothCameraEnabled; smoothSet = true; }
			mc.options.smoothCameraEnabled = true;
		} else if (smoothSet) {
			mc.options.smoothCameraEnabled = savedSmooth;
			smoothSet = false;
		}
		return fov / zoomCurrent;
	}

	public static boolean scroll(int dir) {
		if (!zooming() || !ZOOM_SCROLL.on()) return false;
		zoomExtra = Math.max(-ZOOM_LEVEL.get() + 1.5f, Math.min(40f, zoomExtra + (dir > 0 ? 1f : -1f) * Math.max(1f, (ZOOM_LEVEL.get() + zoomExtra) * 0.2f)));
		return true;
	}

	public static String actionStatus(MinecraftClient mc, boolean showToggled) {
		ClientPlayerEntity p = mc.player;
		if (p == null) return "Sprinting (Toggled)";
		String t = showToggled ? " (Toggled)" : "";
		if (p.abilities.flying) return "Flying";
		if (p.isSneaking()) return "Sneaking" + (sneakToggled ? t : "");
		if (p.isSprinting()) return "Sprinting" + (sprintToggled ? t : "");
		if (sprintToggled) return "Sprinting" + t;
		return "";
	}

	public static void attacked(MinecraftClient mc, Entity target) {
		Stats.attacked(mc, target);
		if (!PARTICLES.enabled() || target == null) return;
		int n = PARTICLE_COUNT.asInt();
		for (int i = 0; i < n; i++) {
			if (PARTICLE_SHARP.on()) mc.particleManager.addEmitter(target, ParticleType.CRIT_MAGIC);
			if (PARTICLE_CRIT.on()) mc.particleManager.addEmitter(target, ParticleType.CRIT);
		}
	}

	public static String nick(String text) {
		if (!NICK.enabled() || text == null) return text;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.getSession() == null) return text;
		String n = mc.getSession().getUsername();
		if (n == null || n.isEmpty() || !text.contains(n)) return text;
		return text.replace(n, NICK_NAME.get());
	}

	public static void chat(String plain) {
		if (!AUTOGG.enabled() || plain == null) return;
		String s = plain.toLowerCase(Locale.ROOT);
		if (s.contains("1st killer -") || s.contains("winner:") || s.contains("winners:") || s.contains(" won the game") || s.contains("victory!") || s.contains("1st place -") || s.contains("winning team:")) {
			if (ggAt == 0) ggAt = System.currentTimeMillis() + 900;
		}
	}

	public static net.minecraft.text.Text stamp(net.minecraft.text.Text t) {
		if (!CHAT.enabled() || t == null) return t;
		String f = CHAT_24.on() ? (CHAT_SECONDS.on() ? "HH:mm:ss" : "HH:mm") : (CHAT_SECONDS.on() ? "h:mm:ss a" : "h:mm a");
		net.minecraft.text.Text out = new net.minecraft.text.LiteralText("\u00a77[" + new java.text.SimpleDateFormat(f, Locale.ROOT).format(new java.util.Date()) + "] \u00a7r");
		out.append(t);
		return out;
	}

	public static boolean backgroundLimit() {
		return !Display.isActive();
	}

	public static boolean mouseWheel() {
		return Mouse.hasWheel();
	}
}
