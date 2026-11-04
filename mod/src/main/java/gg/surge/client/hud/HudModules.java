package gg.surge.client.hud;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.features.CombatStats;
import gg.surge.client.features.Waypoints;
import gg.surge.client.mods.Colors;
import gg.surge.client.mods.Mods;
import gg.surge.client.mods.Setting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class HudModules {
	public static final List<HudModule> ALL = new ArrayList<>();

	private HudModules() {}

	private static final Deque<Long> LEFT = new ArrayDeque<>();
	private static final Deque<Long> RIGHT = new ArrayDeque<>();
	private static boolean leftWasDown;
	private static boolean rightWasDown;

	public static void pollClicks(Minecraft mc) {
		long now = System.currentTimeMillis();
		long window = mc.getWindow().handle();
		boolean l = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean r = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		if (mc.screen == null) {
			if (l && !leftWasDown) LEFT.addLast(now);
			if (r && !rightWasDown) RIGHT.addLast(now);
		}
		leftWasDown = l;
		rightWasDown = r;
		while (!LEFT.isEmpty() && now - LEFT.peekFirst() > 1000) LEFT.removeFirst();
		while (!RIGHT.isEmpty() && now - RIGHT.peekFirst() > 1000) RIGHT.removeFirst();
	}

	public static int leftCps() { return LEFT.size(); }

	public static int rightCps() { return RIGHT.size(); }

	private static boolean swRunning;
	private static long swStart, swElapsed;
	private static long sessionStart = System.currentTimeMillis();
	private static boolean inWorld;

	public static void toggleStopwatch() {
		long now = System.currentTimeMillis();
		if (swRunning) { swElapsed += now - swStart; swRunning = false; }
		else { swStart = now; swRunning = true; }
	}

	public static void resetStopwatch() {
		swRunning = false;
		swElapsed = 0;
	}

	private static long stopwatchMillis() {
		return swElapsed + (swRunning ? System.currentTimeMillis() - swStart : 0);
	}

	public static void tick(Minecraft mc) {
		ExtraModules.tick(mc);
		boolean now = mc.level != null;
		if (now && !inWorld) sessionStart = System.currentTimeMillis();
		inWorld = now;
	}

	static String duration(long ms, boolean hundredths) {
		long s = ms / 1000;
		String base = s >= 3600 ? String.format("%d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60) : String.format("%d:%02d", s / 60, s % 60);
		return hundredths ? base + String.format(".%02d", (ms / 10) % 100) : base;
	}

	static class TextModule extends HudModule {
		private final String label;
		private final Function<Minecraft, String> value;
		private final String sample;
		final Setting.Choice labelPos;

		TextModule(String id, String name, Item icon, String desc, boolean def, float x, float y, String label, String labelDef, String sample, Function<Minecraft, String> value) {
			super(id, name, icon, desc, def, x, y);
			this.label = label;
			this.value = value;
			this.sample = sample;
			this.labelPos = label.isEmpty() ? null : choice("label", "Label", labelDef, "After", "Before", "Hidden");
		}

		String text(Minecraft mc) {
			String v;
			try {
				v = mc.player == null ? sample : value.apply(mc);
			} catch (Exception e) {
				v = sample;
			}
			if (v == null) return "";
			if (labelPos == null) return v;
			return switch (labelPos.get()) {
				case "Before" -> label + ": " + v;
				case "Hidden" -> v;
				default -> v + " " + label;
			};
		}

		@Override public int width(Minecraft mc) { return boxWidth(mc, text(mc)); }

		@Override public int height(Minecraft mc) { return 13; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			String t = text(mc);
			if (!t.isEmpty()) box(g, mc, x, y, t);
		}
	}

	private static final DateTimeFormatter H24 = DateTimeFormatter.ofPattern("HH:mm"), H24S = DateTimeFormatter.ofPattern("HH:mm:ss");
	private static final DateTimeFormatter H12 = DateTimeFormatter.ofPattern("h:mm a"), H12S = DateTimeFormatter.ofPattern("h:mm:ss a");
	private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

	static String facing(LocalPlayer p) {
		float yaw = ((p.getYRot() % 360) + 360) % 360;
		return DIRS[Math.round(yaw / 45f) % 8];
	}

	static {
		ALL.add(new TextModule("fps", "FPS", Items.REDSTONE, "Frames per second", true, 0.005f, 0.01f, "FPS", "After", "144",
			mc -> String.valueOf(mc.getFps())));

		TextModule cps = new TextModule("cps", "CPS", Items.STONE_BUTTON, "Clicks per second", false, 0.005f, 0.055f, "CPS", "After", "8 | 3", null) {
			final Setting.Toggle right = toggle("right", "Show right clicks", true);

			@Override String text(Minecraft mc) {
				String v = right.on() ? leftCps() + " | " + rightCps() : String.valueOf(leftCps());
				return switch (labelPos.get()) { case "Before" -> "CPS: " + v; case "Hidden" -> v; default -> v + " CPS"; };
			}
		};
		ALL.add(cps);

		ALL.add(new Coordinates());
		ALL.add(new Compass());
		ALL.add(new PackDisplay());

		ALL.add(new TextModule("ping", "Ping", Items.REPEATER, "Your latency to the server", false, 0.005f, 0.19f, "ms", "After", "32",
			mc -> {
				if (mc.getConnection() == null || mc.player == null) return "0";
				PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
				return String.valueOf(info == null ? 0 : info.getLatency());
			}) {
			final Setting.Toggle colored = toggle("colored", "Color by ping", true);

			@Override public int fg() {
				if (!colored.on() || Minecraft.getInstance().player == null) return super.fg();
				int ms;
				try { ms = Integer.parseInt(text(Minecraft.getInstance()).replaceAll("[^0-9]", "")); } catch (Exception e) { return super.fg(); }
				return ms < 80 ? 0xFF6BFF8A : ms < 160 ? 0xFFFFE07A : 0xFFFF6B6B;
			}
		});

		ALL.add(new TextModule("speed", "Speed", Items.RABBIT_FOOT, "How fast you're moving", false, 0.005f, 0.235f, "", "After", "5.61 b/s", null) {
			final Setting.Choice unit = choice("unit", "Unit", "b/s", "b/s", "km/h");

			@Override String text(Minecraft mc) {
				LocalPlayer p = mc.player;
				if (p == null) return unit.is("km/h") ? "20.2 km/h" : "5.61 b/s";
				double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
				double bps = Math.sqrt(dx * dx + dz * dz) * 20;
				return unit.is("km/h") ? String.format("%.1f km/h", bps * 3.6) : String.format("%.2f b/s", bps);
			}
		});

		ALL.add(new TextModule("clock", "Clock", Items.CLOCK, "Real-world time", false, 0.94f, 0.01f, "", "After", "", null) {
			final Setting.Toggle h24 = toggle("24h", "24-hour time", true);
			final Setting.Toggle seconds = toggle("seconds", "Show seconds", false);

			@Override String text(Minecraft mc) {
				DateTimeFormatter f = h24.on() ? (seconds.on() ? H24S : H24) : (seconds.on() ? H12S : H12);
				return LocalTime.now().format(f);
			}
		});

		ALL.add(new TextModule("sprint", "Action Status", Items.GOLDEN_BOOTS, "Shows what you're doing: sprinting, sneaking, flying, swimming and more", true, 0.005f, 0.96f, "", "After", "", null) {
			final Setting.Toggle showToggled = toggle("toggled", "Show (Toggled)", true);

			@Override String text(Minecraft mc) { return action(mc, showToggled.on()); }
		});

		ALL.add(new TextModule("reach", "Reach", Items.DIAMOND_SWORD, "Distance of your last hit", false, 0.005f, 0.28f, "blocks", "After", "3.04",
			mc -> CombatStats.reach()));
		ALL.add(new TextModule("combo", "Combo", Items.BLAZE_POWDER, "Hits in a row without getting hit", false, 0.005f, 0.325f, "combo", "After", "4",
			mc -> String.valueOf(CombatStats.combo())));
		ALL.add(new TextModule("memory", "Memory", Items.COMPARATOR, "How much RAM the game is using", false, 0.94f, 0.055f, "", "After", "", null) {
			final Setting.Choice format = choice("format", "Show", "Both", "Both", "Percent", "Gigabytes");

			@Override String text(Minecraft mc) {
				Runtime r = Runtime.getRuntime();
				long used = r.totalMemory() - r.freeMemory();
				String pct = (used * 100 / r.maxMemory()) + "%";
				String gb = String.format("%.1f/%.1f GB", used / 1073741824.0, r.maxMemory() / 1073741824.0);
				return "Mem " + (format.is("Percent") ? pct : format.is("Gigabytes") ? gb : pct + " " + gb);
			}
		});
		ALL.add(new TextModule("server", "Server Address", Items.BEACON, "The server you're playing on", false, 0.005f, 0.37f, "", "After", "play.example.net",
			mc -> mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "Singleplayer"));
		ALL.add(new TextModule("biome", "Biome", Items.GRASS_BLOCK, "The biome you're standing in", false, 0.005f, 0.415f, "", "After", "Plains",
			mc -> mc.level.getBiome(mc.player.blockPosition()).unwrapKey().map(k -> pretty(k.identifier().getPath())).orElse("Unknown")));
		ALL.add(new TextModule("day", "Day Counter", Items.SUNFLOWER, "In-game days passed", false, 0.005f, 0.46f, "Day", "Before", "12",
			mc -> String.valueOf(mc.level.getDayTime() / 24000L)));
		ALL.add(new TextModule("saturation", "Saturation", Items.COOKED_BEEF, "Your hidden hunger saturation", false, 0.005f, 0.505f, "Saturation", "Before", "5.0",
			mc -> String.format("%.1f", mc.player.getFoodData().getSaturationLevel())));
		ALL.add(new TextModule("light", "Light Level", Items.TORCH, "Block light where you stand (mobs spawn at 0)", false, 0.005f, 0.55f, "Light", "Before", "14",
			mc -> String.valueOf(mc.level.getBrightness(LightLayer.BLOCK, mc.player.blockPosition()))));
		ALL.add(new TextModule("players", "Player Count", Items.PLAYER_HEAD, "Players online on this server", false, 0.005f, 0.595f, "online", "After", "12",
			mc -> String.valueOf(mc.getConnection() == null ? 1 : mc.getConnection().getOnlinePlayers().size())));
		ALL.add(new TextModule("stopwatch", "Stopwatch", Items.RECOVERY_COMPASS, "Start and stop with the Stopwatch key in Controls", false, 0.94f, 0.10f, "", "After", "0:00.00", null) {
			final Setting.Toggle hundredths = toggle("hundredths", "Show hundredths", true);

			{
				button("Reset", "Reset", HudModules::resetStopwatch);
			}

			@Override String text(Minecraft mc) { return duration(stopwatchMillis(), hundredths.on()); }
		});
		ALL.add(new TextModule("session", "Session Time", Items.EXPERIENCE_BOTTLE, "How long you've been in this world", false, 0.94f, 0.145f, "Session", "Before", "12:04",
			mc -> duration(System.currentTimeMillis() - sessionStart, false)));
		ALL.add(new TextModule("rotation", "Rotation", Items.ARROW, "Your exact yaw and pitch, for building and parkour", false, 0.005f, 0.64f, "", "After", "", null) {
			@Override String text(Minecraft mc) {
				if (mc.player == null) return "Yaw 90.0  Pitch 12.5";
				float yaw = ((mc.player.getYRot() % 360) + 540) % 360 - 180;
				return String.format("Yaw %.1f  Pitch %.1f", yaw, mc.player.getXRot());
			}
		});
		ALL.add(new TextModule("gametime", "Game Time", Items.CLOCK, "The in-game time of day", false, 0.94f, 0.19f, "", "After", "", null) {
			final Setting.Toggle h24 = toggle("24h", "24-hour time", true);

			@Override String text(Minecraft mc) {
				long t = mc.level == null ? 6000 : (mc.level.getDayTime() + 6000) % 24000;
				int h = (int) (t / 1000), m = (int) ((t % 1000) * 60 / 1000);
				if (h24.on()) return String.format("%02d:%02d", h, m);
				return String.format("%d:%02d %s", h % 12 == 0 ? 12 : h % 12, m, h < 12 ? "AM" : "PM");
			}
		});
		ALL.add(new PotionEffects());
		ALL.add(new Keystrokes());
		ALL.add(new ArmorStatus());
		ALL.add(new ItemCounter());
		ALL.addAll(ExtraModules.create());
	}

	static final class Coordinates extends HudModule {
		final Setting.Choice layout = choice("layout", "Layout", "Vertical", "Vertical", "Horizontal");
		final Setting.Toggle direction = toggle("direction", "Show direction", true);
		final Setting.Toggle biome = toggle("biome", "Show biome", false);
		final Setting.Slider decimals = slider("decimals", "Decimals", 0, 0, 2, 1, "");

		Coordinates() { super("coords", "Coordinates", Items.FILLED_MAP, "Your X Y Z position", true, 0.005f, 0.10f); }

		private List<String> lines(Minecraft mc) {
			List<String> out = new ArrayList<>();
			LocalPlayer p = mc.player;
			String f = "%." + decimals.asInt() + "f";
			double x = p == null ? 128 : p.getX(), y = p == null ? 64 : p.getY(), z = p == null ? -256 : p.getZ();
			if (layout.is("Horizontal")) {
				out.add(String.format(f + "  " + f + "  " + f, x, y, z) + (direction.on() ? "  " + (p == null ? "N" : facing(p)) : ""));
			} else {
				out.add("X: " + String.format(f, x));
				out.add("Y: " + String.format(f, y));
				out.add("Z: " + String.format(f, z));
				if (direction.on()) out.add("Facing: " + (p == null ? "N" : facing(p)));
			}
			if (biome.on()) {
				String b = "Plains";
				try { if (p != null) b = mc.level.getBiome(p.blockPosition()).unwrapKey().map(k -> pretty(k.identifier().getPath())).orElse("Unknown"); } catch (Exception ignored) { }
				out.add(b);
			}
			return out;
		}

		@Override public int width(Minecraft mc) {
			int w = 40;
			for (String l : lines(mc)) w = Math.max(w, mc.font.width(bracket(l)) + 8);
			return w;
		}

		@Override public int height(Minecraft mc) { return lines(mc).size() * 10 + 4; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			List<String> ls = lines(mc);
			int w = width(mc);
			if (bgOn()) g.fill(x, y, x + w, y + height(mc), bg());
			for (int i = 0; i < ls.size(); i++) text(g, mc, bracket(ls.get(i)), x + 4, y + 3 + i * 10, fg());
		}
	}

	static final class PackDisplay extends HudModule {
		final Setting.Toggle header = toggle("header", "Show \"Packs\" title", true);
		final Setting.Slider max = slider("max", "Most packs shown", 3, 1, 8, 1, "");
		private List<String> cache = List.of();
		private long cachedAt;

		PackDisplay() { super("packs", "Pack Display", Items.PAINTING, "Shows the resource packs you're using", false, 0.005f, 0.45f); }

		private List<String> packs(Minecraft mc) {
			if (System.currentTimeMillis() - cachedAt > 2000) {
				cachedAt = System.currentTimeMillis();
				List<String> out = new ArrayList<>();
				for (var p : mc.getResourcePackRepository().getSelectedPacks()) {
					if (p.isRequired() || p.getId().equals("vanilla") || p.getId().startsWith("fabric") || p.getId().equals("surge")) continue;
					out.add(0, net.minecraft.ChatFormatting.stripFormatting(p.getTitle().getString()));
				}
				cache = out;
			}
			return cache.isEmpty() ? List.of("Default") : cache.subList(0, Math.min(cache.size(), max.asInt()));
		}

		@Override public int width(Minecraft mc) {
			int w = header.on() ? mc.font.width("Packs") : 0;
			for (String s : packs(mc)) w = Math.max(w, mc.font.width(s));
			return w + 8;
		}

		@Override public int height(Minecraft mc) { return packs(mc).size() * 10 + (header.on() ? 11 : 0) + 3; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			if (bgOn()) g.fill(x, y, x + width(mc), y + height(mc), bg());
			int yy = y + 2;
			if (header.on()) { g.drawString(mc.font, "Packs", x + 4, yy, Colors.alpha(fg(), 0.7f), shadowOn()); yy += 11; }
			for (String s : packs(mc)) { g.drawString(mc.font, s, x + 4, yy, fg(), shadowOn()); yy += 10; }
		}
	}

	static final class Compass extends HudModule {
		final Setting.Slider widthSetting = slider("width", "Width", 160, 100, 280, 10, "px");
		final Setting.Toggle degrees = toggle("degrees", "Show degrees", true);
		final Setting.Toggle markers = toggle("waypoints", "Show waypoints", true);

		Compass() { super("compass", "Compass", Items.COMPASS, "A compass bar with your waypoints on it", false, 0.5f - 0.0625f, 0.005f); }

		@Override public int width(Minecraft mc) { return widthSetting.asInt(); }

		@Override public int height(Minecraft mc) { return degrees.on() ? 24 : 14; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			int w = width(mc), cx = x + w / 2;
			float yaw = mc.player == null ? 180 : ((mc.player.getYRot() % 360) + 360) % 360;
			float ppd = w / 140f;
			if (bgOn()) g.fill(x, y, x + w, y + 14, bg());
			g.enableScissor(x, y, x + w, y + 14);
			for (int d = 0; d < 360; d += 15) {
				float rel = wrap(d - yaw);
				if (Math.abs(rel) > 72) continue;
				int px = Math.round(cx + rel * ppd);
				String label = switch (d) { case 0 -> "S"; case 45 -> "SW"; case 90 -> "W"; case 135 -> "NW"; case 180 -> "N"; case 225 -> "NE"; case 270 -> "E"; case 315 -> "SE"; default -> null; };
				if (label != null) {
					int col = d % 90 == 0 ? fg() : Colors.alpha(fg(), 0.7f);
					g.drawString(mc.font, label, px - mc.font.width(label) / 2, y + 3, col, shadowOn());
				} else {
					g.fill(px, y + 5, px + 1, y + 9, Colors.alpha(fg(), 0.45f));
				}
			}
			if (markers.on() && mc.player != null) {
				for (SurgeConfig.Waypoint wp : Waypoints.here(mc)) {
					double dx = wp.x + 0.5 - mc.player.getX(), dz = wp.z + 0.5 - mc.player.getZ();
					float bearing = (float) Math.toDegrees(Math.atan2(-dx, dz));
					float rel = wrap(bearing - yaw);
					if (Math.abs(rel) > 70) continue;
					int px = Math.round(cx + rel * ppd);
					int col = Colors.parse(wp.color);
					g.fill(px - 1, y + 1, px + 2, y + 2, col);
					g.fill(px - 2, y + 2, px + 3, y + 3, col);
					g.fill(px - 1, y + 3, px + 2, y + 4, col);
				}
			}
			g.disableScissor();
			g.fill(cx, y + 11, cx + 1, y + 14, 0xFFFFFFFF);
			if (degrees.on()) {
				String deg = Math.round(yaw) + "°";
				int dw = mc.font.width(deg) + 6;
				if (bgOn()) g.fill(cx - dw / 2, y + 15, cx - dw / 2 + dw, y + 24, bg());
				g.drawString(mc.font, deg, cx - mc.font.width(deg) / 2, y + 16, fg(), shadowOn());
			}
		}

		private static float wrap(float a) {
			a = (a + 180) % 360;
			if (a < 0) a += 360;
			return a - 180;
		}
	}

	static final class Keystrokes extends HudModule {
		private static final int GAP = 2;
		final Setting.Slider size = slider("size", "Key size", 20, 14, 30, 1, "px");
		final Setting.Toggle mouse = toggle("mouse", "Show mouse buttons", true);
		final Setting.Toggle cps = toggle("cps", "Show CPS", true).showIf(() -> mouse.on());
		final Setting.Toggle space = toggle("space", "Show space bar", true);
		final Setting.Color pressed = color("pressed", "Pressed color", "FFFFFF");
		final Setting.Toggle fade = toggle("fade", "Smooth fade", true);
		private final Map<String, Float> anim = new HashMap<>();
		private long last = System.nanoTime();

		Keystrokes() { super("keystrokes", "Keystrokes", Items.OAK_BUTTON, "WASD, mouse buttons and CPS", true, 0.88f, 0.70f); }

		@Override public int width(Minecraft mc) { return size.asInt() * 3 + GAP * 2; }

		@Override public int height(Minecraft mc) {
			int k = size.asInt();
			return k * 2 + GAP + (mouse.on() ? k + GAP : 0) + (space.on() ? 12 + GAP : 0);
		}

		private float level(String id, boolean down, float dt) {
			id = id.isEmpty() ? "?" : id;
			float cur = anim.getOrDefault(id, 0f);
			float t = down ? 1f : 0f;
			cur = fade.on() ? cur + (t - cur) * (1f - (float) Math.exp(-dt * 22f)) : t;
			anim.put(id, cur);
			return cur;
		}

		private void key(GuiGraphics g, Minecraft mc, int x, int y, int w, int h, String label, boolean down, float dt) {
			float a = level(label.split(" ")[0], down, dt);
			int base = bgOn() ? bg() : 0x00000000;
			int on = (0xD0 << 24) | (pressed.rgb(0) & 0xFFFFFF);
			g.fill(x, y, x + w, y + h, Colors.lerp(base, on, a));
			int textCol = Colors.lerp(fg(), 0xFF101010, a);
			g.drawString(mc.font, label, x + (w - mc.font.width(label)) / 2, y + (h - 8) / 2, textCol, shadowOn() && a < 0.5f);
		}

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			long now = System.nanoTime();
			float dt = Math.min(0.1f, (now - last) / 1e9f);
			last = now;
			var o = mc.options;
			int k = size.asInt();
			key(g, mc, x + k + GAP, y, k, k, label(mc, o.keyUp, k), down(o.keyUp), dt);
			key(g, mc, x, y + k + GAP, k, k, label(mc, o.keyLeft, k), down(o.keyLeft), dt);
			key(g, mc, x + k + GAP, y + k + GAP, k, k, label(mc, o.keyDown, k), down(o.keyDown), dt);
			key(g, mc, x + (k + GAP) * 2, y + k + GAP, k, k, label(mc, o.keyRight, k), down(o.keyRight), dt);
			int yy = y + (k + GAP) * 2;
			if (mouse.on()) {
				int half = (width(mc) - GAP) / 2;
				String la = label(mc, o.keyAttack, half), lu = label(mc, o.keyUse, half);
				key(g, mc, x, yy, half, k, cps.on() ? la + " " + leftCps() : la, down(o.keyAttack), dt);
				key(g, mc, x + half + GAP, yy, half, k, cps.on() ? lu + " " + rightCps() : lu, down(o.keyUse), dt);
				yy += k + GAP;
			}
			if (space.on()) key(g, mc, x, yy, width(mc), 12, label(mc, o.keyJump, width(mc)), down(o.keyJump), dt);
		}

		private static String label(Minecraft mc, KeyMapping km, int w) {
			String n = km.getTranslatedKeyMessage().getString();
			n = switch (n) {
				case "Left Button" -> "LMB";
				case "Right Button" -> "RMB";
				case "Middle Button" -> "MMB";
				case "Left Shift" -> "LShift";
				case "Right Shift" -> "RShift";
				case "Left Control" -> "LCtrl";
				case "Up Arrow" -> "\u2191";
				case "Down Arrow" -> "\u2193";
				case "Left Arrow" -> "\u2190";
				case "Right Arrow" -> "\u2192";
				default -> n;
			};
			if (mc.font.width(n) > w - 4) {
				while (n.length() > 1 && mc.font.width(n) > w - 4) n = n.substring(0, n.length() - 1);
			}
			return n;
		}

		private static boolean down(KeyMapping key) { return key.isDown(); }
	}

	static final class ArmorStatus extends HudModule {
		private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		final Setting.Choice layout = choice("layout", "Layout", "Vertical", "Vertical", "Horizontal", "Uku");
		final Setting.Choice durability = choice("durability", "Durability", "Percent", "Percent", "Number", "Off");
		final Setting.Toggle held = toggle("held", "Show held item", true);
		final Setting.Toggle offhand = toggle("offhand", "Show offhand item", false);
		final Setting.Toggle bars = toggle("bars", "Durability bars (Uku)", true).showIf(() -> layout.is("Uku"));

		ArmorStatus() { super("armor", "Armor Status", Items.DIAMOND_CHESTPLATE, "Your armor and held item with durability", false, 0.005f, 0.64f); }

		private List<ItemStack> stacks(Minecraft mc) {
			List<ItemStack> out = new ArrayList<>();
			if (mc.player == null) {
				out.add(new ItemStack(Items.DIAMOND_HELMET));
				out.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
				out.add(new ItemStack(Items.DIAMOND_LEGGINGS));
				out.add(new ItemStack(Items.DIAMOND_BOOTS));
				if (held.on()) out.add(new ItemStack(Items.DIAMOND_SWORD));
				return out;
			}
			for (EquipmentSlot s : ARMOR) out.add(mc.player.getItemBySlot(s));
			if (held.on()) out.add(mc.player.getItemBySlot(EquipmentSlot.MAINHAND));
			if (offhand.on()) out.add(mc.player.getItemBySlot(EquipmentSlot.OFFHAND));
			return out;
		}

		private String dura(ItemStack s) {
			if (s.isEmpty() || !s.isDamageableItem() || durability.is("Off")) return "";
			int left = s.getMaxDamage() - s.getDamageValue();
			return durability.is("Number") ? String.valueOf(left) : (left * 100 / s.getMaxDamage()) + "%";
		}

		private int textW(Minecraft mc) {
			return durability.is("Off") ? 0 : mc.font.width(durability.is("Number") ? "2031" : "100%") + 4;
		}

		@Override public int width(Minecraft mc) {
			int n = stacks(mc).size();
			if (layout.is("Uku")) return n * 22;
			return layout.is("Horizontal") ? n * (18 + textW(mc)) : 18 + textW(mc);
		}

		@Override public int height(Minecraft mc) {
			if (layout.is("Uku")) return 19 + (bars.on() ? 3 : 0) + (durability.is("Off") ? 0 : 9);
			return layout.is("Horizontal") ? 18 : 18 * stacks(mc).size();
		}

		private void renderUku(GuiGraphics g, Minecraft mc, int x, int y) {
			List<ItemStack> list = stacks(mc);
			if (bgOn()) g.fill(x, y, x + width(mc), y + height(mc), bg());
			for (int i = 0; i < list.size(); i++) {
				ItemStack s = list.get(i);
				int ix = x + i * 22 + 3;
				if (s.isEmpty()) {
					g.fill(ix + 2, y + 2, ix + 14, y + 14, 0x30FFFFFF);
					continue;
				}
				g.renderItem(s, ix, y + 1);
				if (!s.isDamageableItem()) {
					if (s.getCount() > 1) g.drawString(mc.font, String.valueOf(s.getCount()), ix + 17 - mc.font.width(String.valueOf(s.getCount())), y + 10, 0xFFFFFFFF, true);
					continue;
				}
				float frac = (s.getMaxDamage() - s.getDamageValue()) / (float) s.getMaxDamage();
				int col = Colors.hsv(frac / 3f, 0.75f, 1f);
				int by = y + 18;
				if (bars.on()) {
					g.fill(ix, by, ix + 16, by + 2, 0xC0000000);
					g.fill(ix, by, ix + Math.max(1, Math.round(16 * frac)), by + 2, col);
					by += 3;
				}
				String d = dura(s);
				if (!d.isEmpty()) {
					g.pose().pushMatrix();
					g.pose().translate(ix + 8, by + 1);
					g.pose().scale(0.75f, 0.75f);
					g.drawString(mc.font, d, -mc.font.width(d) / 2, 0, col, shadowOn());
					g.pose().popMatrix();
				}
			}
		}

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			if (mc.player != null && mc.screen == null && stacks(mc).stream().allMatch(ItemStack::isEmpty)) return;
			if (layout.is("Uku")) { renderUku(g, mc, x, y); return; }
			if (bgOn()) g.fill(x, y, x + width(mc), y + height(mc), bg());
			List<ItemStack> list = stacks(mc);
			int tw = textW(mc);
			for (int i = 0; i < list.size(); i++) {
				ItemStack s = list.get(i);
				int ix = layout.is("Horizontal") ? x + i * (18 + tw) : x;
				int iy = layout.is("Horizontal") ? y : y + i * 18;
				if (s.isEmpty()) continue;
				g.renderItem(s, ix + 1, iy + 1);
				if (durability.is("Off")) g.renderItemDecorations(mc.font, s, ix + 1, iy + 1);
				String d = dura(s);
				if (!d.isEmpty()) {
					float frac = (s.getMaxDamage() - s.getDamageValue()) / (float) s.getMaxDamage();
					int col = frac > 0.5f ? fg() : frac > 0.2f ? 0xFFFFE07A : 0xFFFF6B6B;
					g.drawString(mc.font, d, ix + 19, iy + 5, col, shadowOn());
				}
			}
		}
	}

	static final class ItemCounter extends HudModule {
		final Setting.Toggle arrows = toggle("arrows", "Arrows", true);
		final Setting.Toggle totems = toggle("totems", "Totems", true);
		final Setting.Toggle gapples = toggle("gapples", "Golden apples", true);
		final Setting.Toggle pearls = toggle("pearls", "Ender pearls", true);
		final Setting.Toggle xp = toggle("xp", "Bottles o' enchanting", false);
		final Setting.Toggle hideZero = toggle("hideZero", "Hide items you don't have", true);

		ItemCounter() { super("items", "Item Counter", Items.ARROW, "Counts arrows, totems, golden apples and pearls in your inventory", false, 0.005f, 0.80f); }

		private record Entry(ItemStack icon, int count) {}

		private List<Entry> entries(Minecraft mc) {
			List<Entry> out = new ArrayList<>();
			add(mc, out, arrows, Items.ARROW, 64);
			add(mc, out, totems, Items.TOTEM_OF_UNDYING, 2);
			add(mc, out, gapples, Items.GOLDEN_APPLE, 12);
			add(mc, out, pearls, Items.ENDER_PEARL, 16);
			add(mc, out, xp, Items.EXPERIENCE_BOTTLE, 32);
			return out;
		}

		private void add(Minecraft mc, List<Entry> out, Setting.Toggle on, Item item, int sample) {
			if (!on.on()) return;
			int n = 0;
			if (mc.player == null) n = sample;
			else {
				var inv = mc.player.getInventory();
				for (int i = 0; i < inv.getContainerSize(); i++) {
					ItemStack s = inv.getItem(i);
					if (s.is(item)) n += s.getCount();
				}
			}
			if (n == 0 && hideZero.on() && mc.player != null) return;
			out.add(new Entry(new ItemStack(item), n));
		}

		@Override public int width(Minecraft mc) {
			int w = 0;
			for (Entry e : entries(mc)) w += 20 + mc.font.width(String.valueOf(e.count())) + 4;
			return Math.max(20, w);
		}

		@Override public int height(Minecraft mc) { return 18; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			if (bgOn()) g.fill(x, y, x + width(mc), y + 18, bg());
			int cx = x + 1;
			for (Entry e : entries(mc)) {
				g.renderItem(e.icon(), cx, y + 1);
				String n = String.valueOf(e.count());
				g.drawString(mc.font, n, cx + 18, y + 5, fg(), shadowOn());
				cx += 20 + mc.font.width(n) + 4;
			}
		}
	}

	static final class Ui2 {
		static String fit(Minecraft mc, String s, int maxW) {
			if (mc.font.width(s) <= maxW) return s;
			while (s.length() > 1 && mc.font.width(s + "…") > maxW) s = s.substring(0, s.length() - 1);
			return s + "…";
		}
	}

	static final class PotionEffects extends HudModule implements HideVanillaEffects {
		private static final String[] ROMAN = {"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
		final Setting.Choice style = choice("style", "Style", "Modern", "Modern", "Vanilla", "Compact");
		final Setting.Toggle icons = toggle("icons", "Show icons", true);
		final Setting.Toggle level = toggle("level", "Show level", true);
		final Setting.Toggle duration = toggle("duration", "Show time left", true);
		final Setting.Toggle colored = toggle("colored", "Color good and bad effects", true);
		final Setting.Toggle hideVanilla = toggle("hideVanilla", "Hide the vanilla effect icons", true);
		private final Map<Object, Integer> maxDuration = new HashMap<>();

		PotionEffects() { super("effects", "Potion Effects", Items.POTION, "Your active effects, with icons and time left", false, 0.80f, 0.45f); }

		@Override public boolean hideVanillaEffects() { return enabled() && hideVanilla.on(); }

		private record Line(String name, String time, int color, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, float left, boolean ending) {}

		private List<Line> lines(Minecraft mc) {
			List<Line> out = new ArrayList<>();
			if (mc.player == null || mc.player.getActiveEffects().isEmpty() && mc.screen instanceof gg.surge.client.gui.HudEditorScreen) {
				out.add(new Line("Speed II", "1:30", colored.on() ? 0xFF8CFFA0 : fg(), null, 0.7f, false));
				out.add(new Line("Strength", "0:45", colored.on() ? 0xFF8CFFA0 : fg(), null, 0.35f, false));
				return out;
			}
			for (MobEffectInstance e : mc.player.getActiveEffects()) {
				int amp = e.getAmplifier();
				String name = e.getEffect().value().getDisplayName().getString();
				if (level.on() && amp > 0) name += amp < ROMAN.length ? ROMAN[amp] : " " + (amp + 1);
				String time = "";
				float left = 1f;
				boolean ending = false;
				if (e.isInfiniteDuration()) time = "\u221E";
				else {
					int d = e.getDuration();
					int max = Math.max(d, maxDuration.getOrDefault(e.getEffect(), d));
					maxDuration.put(e.getEffect(), max);
					left = d / (float) Math.max(1, max);
					ending = d < 200;
					int sec = d / 20;
					time = (sec / 60) + ":" + String.format("%02d", sec % 60);
				}
				if (!duration.on()) time = "";
				int col = fg();
				if (colored.on()) col = e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL ? 0xFFFF8C8C : 0xFF8CFFA0;
				out.add(new Line(name, time, col, e.getEffect(), left, ending));
			}
			return out;
		}

		private int rowH() { return style.is("Compact") ? 13 : style.is("Vanilla") ? 26 : 22; }

		@Override public int width(Minecraft mc) {
			int w = style.is("Vanilla") ? 110 : 60;
			for (Line l : lines(mc)) {
				int t = style.is("Compact") ? boxWidth(mc, l.name() + (l.time().isEmpty() ? "" : " " + l.time())) + (icons.on() ? 12 : 0)
					: mc.font.width(l.name()) + (icons.on() ? 30 : 10);
				w = Math.max(w, t);
			}
			return w;
		}

		@Override public int height(Minecraft mc) { return Math.max(rowH(), lines(mc).size() * (rowH() + (style.is("Compact") ? 0 : 2))); }

		private void icon(GuiGraphics g, Line l, int x, int y, int size, float alpha) {
			if (!icons.on()) return;
			if (l.effect() == null) {
				g.fill(x + 2, y + 2, x + size - 2, y + size - 2, Colors.alpha(l.color(), alpha));
				return;
			}
			g.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, net.minecraft.client.gui.Gui.getMobEffectSprite(l.effect()), x, y, size, size, alpha);
		}

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			List<Line> ls = lines(mc);
			int w = width(mc);
			int rh = rowH();
			long now = System.currentTimeMillis();
			for (int i = 0; i < ls.size(); i++) {
				Line l = ls.get(i);
				float alpha = l.ending() ? 0.55f + 0.45f * (float) Math.abs(Math.sin(now / 250.0)) : 1f;
				switch (style.get()) {
					case "Compact" -> {
						int yy = y + i * 13;
						if (bgOn()) g.fill(x, yy, x + w, yy + 13, bg());
						int tx = x + 4;
						if (icons.on()) { icon(g, l, x + 2, yy + 2, 9, alpha); tx += 10; }
						String s = bracket(l.name() + (l.time().isEmpty() ? "" : " " + l.time()));
						g.drawString(mc.font, s, tx, yy + 3, Colors.alpha(l.color(), alpha), shadowOn());
					}
					case "Vanilla" -> {
						int yy = y + i * (rh + 2);
						g.fill(x, yy, x + w, yy + rh, 0xFF000000);
						g.fill(x + 1, yy + 1, x + w - 1, yy + rh - 1, 0xFF8B8B8B);
						g.fill(x + 1, yy + 1, x + w - 2, yy + 2, 0xFFFFFFFF);
						g.fill(x + 1, yy + 1, x + 2, yy + rh - 2, 0xFFFFFFFF);
						g.fill(x + 2, yy + rh - 2, x + w - 1, yy + rh - 1, 0xFF373737);
						g.fill(x + w - 2, yy + 2, x + w - 1, yy + rh - 1, 0xFF373737);
						icon(g, l, x + 4, yy + 4, 18, alpha);
						int tx = x + (icons.on() ? 26 : 6);
						g.drawString(mc.font, l.name(), tx, yy + 4, 0xFFFFFFFF, true);
						if (!l.time().isEmpty()) g.drawString(mc.font, l.time(), tx, yy + 14, Colors.alpha(0xFF7F7F7F, alpha), true);
					}
					default -> {
						int yy = y + i * (rh + 2);
						if (bgOn()) g.fill(x, yy, x + w, yy + rh, bg());
						icon(g, l, x + 3, yy + 3, 16, alpha);
						int tx = x + (icons.on() ? 23 : 5);
						g.drawString(mc.font, l.name(), tx, yy + 3, Colors.alpha(l.color(), alpha), shadowOn());
						if (!l.time().isEmpty()) g.drawString(mc.font, l.time(), tx, yy + 12, Colors.alpha(0xFFB9B9BE, alpha), shadowOn());
						g.fill(x, yy + rh - 1, x + Math.round(w * l.left()), yy + rh, Colors.alpha(l.color(), 0.85f));
					}
				}
			}
		}
	}

	static String action(Minecraft mc, boolean showToggled) {
		LocalPlayer p = mc.player;
		boolean sprintMod = Mods.TOGGLE_SPRINT.enabled() && Mods.sprintToggled();
		boolean sneakMod = Mods.TOGGLE_SNEAK.enabled();
		if (p == null) return "Sprinting (Toggled)";
		String a;
		if (p.getAbilities().flying) a = "Flying";
		else if (p.isFallFlying()) a = "Gliding";
		else if (p.isPassenger()) a = "Riding";
		else if (p.isSwimming()) a = "Swimming";
		else if (p.isVisuallyCrawling()) a = "Crawling";
		else if (p.isShiftKeyDown()) a = sneakMod && showToggled ? "Sneaking (Toggled)" : "Sneaking";
		else if (p.isSprinting()) a = sprintMod && showToggled ? "Sprinting (Toggled)" : "Sprinting";
		else if (sprintMod && showToggled) a = "Sprint Toggled";
		else return "";
		if (p.isUsingItem() && p.isBlocking()) a += ", Blocking";
		return a;
	}

	static String pretty(String id) {
		StringBuilder b = new StringBuilder();
		for (String w : id.split("_")) if (!w.isEmpty()) b.append(b.isEmpty() ? "" : " ").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
		return b.toString();
	}

	public static HudModule byId(String id) {
		for (HudModule m : ALL) if (m.id.equals(id)) return m;
		return null;
	}
}
