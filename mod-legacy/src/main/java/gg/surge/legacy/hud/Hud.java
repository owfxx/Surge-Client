package gg.surge.legacy.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.surge.legacy.feature.Features;
import gg.surge.legacy.feature.Stats;
import gg.surge.legacy.gui.Draw;
import gg.surge.legacy.mod.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Hud {
	public static final List<HudMod> ALL = new ArrayList<HudMod>();
	private static final String[] DIRS = {"S", "SW", "W", "NW", "N", "NE", "E", "SE"};

	private Hud() {}

	static String facing(float yawDeg) {
		float yaw = ((yawDeg % 360) + 360) % 360;
		return DIRS[Math.round(yaw / 45f) % 8];
	}

	private static ItemStack st(Item i) { return new ItemStack(i); }

	static {
		ALL.add(new TextHud("fps", "FPS", st(Items.REDSTONE), "Frames per second", true, 0.005f, 0.01f, "FPS", "After", "144") {
			@Override protected String value() { return String.valueOf(MinecraftClient.getCurrentFps()); }
		});
		ALL.add(new TextHud("cps", "CPS", new ItemStack(Blocks.STONE_BUTTON), "Clicks per second", true, 0.005f, 0.055f, "CPS", "After", "8 | 3") {
			final Setting.Toggle right = toggle("right", "Show right clicks", true);

			@Override protected String value() { return right.on() ? Stats.leftCps() + " | " + Stats.rightCps() : String.valueOf(Stats.leftCps()); }
		});
		ALL.add(new Coordinates());
		ALL.add(new TextHud("ping", "Ping", st(Items.REPEATER), "Your latency to the server", false, 1f, 0.10f, "ms", "After", "32") {
			final Setting.Toggle colored = toggle("colored", "Color by ping", true);

			@Override protected String value() {
				MinecraftClient mc = mc();
				if (mc.getNetworkHandler() == null) return "0";
				PlayerListEntry e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
				return String.valueOf(e == null ? 0 : e.getLatency());
			}

			@Override public int fg() {
				if (!colored.on() || mc().player == null) return super.fg();
				int ms;
				try { ms = Integer.parseInt(value()); } catch (Exception e) { return super.fg(); }
				return ms < 80 ? 0xFF6BFF8A : ms < 160 ? 0xFFFFE07A : 0xFFFF6B6B;
			}
		});
		ALL.add(new TextHud("speed", "Speed", st(Items.SUGAR), "How fast you're moving", false, 1f, 0.145f, "", "After", "5.61 b/s") {
			final Setting.Choice unit = choice("unit", "Unit", "b/s", "b/s", "km/h");

			@Override protected String value() {
				ClientPlayerEntity p = mc().player;
				double dx = p.x - p.prevX, dz = p.z - p.prevZ;
				double bps = Math.sqrt(dx * dx + dz * dz) * 20;
				return unit.is("km/h") ? String.format("%.1f km/h", bps * 3.6) : String.format("%.2f b/s", bps);
			}
		});
		ALL.add(new TextHud("clock", "Clock", st(Items.CLOCK), "Real-world time", false, 1f, 0.01f, "", "After", "12:00") {
			final Setting.Toggle h24 = toggle("24h", "24-hour time", true);
			final Setting.Toggle seconds = toggle("seconds", "Show seconds", false);

			@Override protected String value() {
				String f = h24.on() ? (seconds.on() ? "HH:mm:ss" : "HH:mm") : (seconds.on() ? "h:mm:ss a" : "h:mm a");
				return new SimpleDateFormat(f, Locale.ROOT).format(new Date());
			}

			@Override protected String text() { return value(); }
		});
		ALL.add(new TextHud("reach", "Reach Display", st(Items.DIAMOND_SWORD), "Distance of your last hit", false, 1f, 0.19f, "blocks", "After", "3.04") {
			@Override protected String value() { return Stats.reach(); }
		});
		ALL.add(new TextHud("combo", "Combo Counter", st(Items.BLAZE_POWDER), "Hits in a row without getting hit", false, 1f, 0.235f, "combo", "After", "4") {
			@Override protected String value() { return String.valueOf(Stats.combo()); }
		});
		ALL.add(new TextHud("memory", "Memory", st(Items.COMPARATOR), "How much RAM the game is using", false, 1f, 0.055f, "", "After", "42%") {
			@Override protected String value() {
				Runtime r = Runtime.getRuntime();
				long used = r.totalMemory() - r.freeMemory();
				return "Mem " + (used * 100 / r.maxMemory()) + "% " + String.format("%.1f/%.1f GB", used / 1073741824.0, r.maxMemory() / 1073741824.0);
			}

			@Override protected String text() { return value(); }
		});
		ALL.add(new TextHud("server", "Server Address", new ItemStack(Blocks.BEACON), "The server you're playing on", false, 1f, 0.28f, "", "After", "play.example.net") {
			@Override protected String value() { return mc().getCurrentServerEntry() != null ? mc().getCurrentServerEntry().address : "Singleplayer"; }
		});
		ALL.add(new TextHud("saturation", "Saturation", st(Items.COOKED_BEEF), "Your hidden hunger saturation", false, 1f, 0.325f, "Saturation", "Before", "5.0") {
			@Override protected String value() { return String.format("%.1f", mc().player.getHungerManager().getSaturationLevel()); }
		});
		ALL.add(new TextHud("sprint", "Action Status", st(Items.FEATHER), "Shows if you are sprinting, sneaking or flying", true, 0.005f, 0.96f, "", "After", "Sprinting (Toggled)") {
			final Setting.Toggle toggled = toggle("toggled", "Show (Toggled)", true);

			@Override protected String value() { return Features.actionStatus(mc(), toggled.on()); }

			@Override public void render(int x, int y) { if (!value().isEmpty() || mc().player == null || editing()) super.render(x, y); }
		});
		ALL.add(new TextHud("biome", "Biome", new ItemStack(Blocks.GRASS), "The biome you're standing in", false, 1f, 0.37f, "", "After", "Plains") {
			final Setting.Toggle colored = toggle("colored", "Color by biome", true);

			@Override protected String value() { return biomeName(mc()); }

			@Override public int fg() {
				if (!colored.on() || mc().player == null) return super.fg();
				try { return Coordinates.biomeColor(value()); } catch (Exception e) { return super.fg(); }
			}
		});
		ALL.add(new Keystrokes());
		ALL.add(new ArmorStatus());
		ALL.add(new Potions());
	}

	static String biomeName(MinecraftClient mc) {
		if (mc.player == null || mc.world == null) return "Plains";
		return mc.world.getBiome(new BlockPos(mc.player.x, mc.player.y, mc.player.z)).name;
	}

	public static boolean editing() {
		return MinecraftClient.getInstance().currentScreen instanceof gg.surge.legacy.gui.HudEditorScreen;
	}

	public static HudMod byId(String id) {
		for (HudMod m : ALL) if (m.id.equals(id)) return m;
		return null;
	}

	public static void render(MinecraftClient mc, int gw, int gh) {
		if (mc.options.hudHidden || mc.options.debugEnabled) return;
		if (mc.currentScreen instanceof gg.surge.legacy.gui.HudEditorScreen) return;
		if (mc.currentScreen instanceof gg.surge.legacy.gui.MenuScreen && gg.surge.legacy.gui.Theme.glass()) return;
		for (HudMod m : ALL) {
			if (!m.enabled()) continue;
			try { m.draw(gw, gh); } catch (Throwable ignored) { }
		}
	}

	static final class Coordinates extends HudMod {
		private static final class Seg {
			final String text;
			final int color;

			Seg(String text, int color) {
				this.text = text;
				this.color = color;
			}
		}

		final Setting.Choice layout = choice("layout", "Layout", "Vertical", "Vertical", "Horizontal");
		final Setting.Toggle labels = toggle("labels", "Show X Y Z labels", true);
		final Setting.Toggle showY = toggle("showY", "Show Y", true);
		final Setting.Slider decimals = slider("decimals", "Decimals", 0, 0, 2, 1, "");
		final Setting.Toggle customLabels = toggle("customLabels", "Color labels", false);
		final Setting.Color labelColor = color("labelColor", "Label color", "6BB8FF").showIf(() -> customLabels.on());
		final Setting.Toggle direction = toggle("direction", "Show direction", true);
		final Setting.Choice dirFormat = choice("dirFormat", "Direction style", "Letter", "Letter", "Letter and axis", "Full name").showIf(() -> direction.on());
		final Setting.Toggle biome = toggle("biome", "Show biome", false);
		final Setting.Toggle biomeColor = toggle("biomeColor", "Color biome by type", true).showIf(() -> biome.on());
		final Setting.Toggle chunk = toggle("chunk", "Show chunk", false);
		final Setting.Toggle otherDim = toggle("otherDim", "Show Nether or Overworld position", false);

		private static final Map<String, String> FULL = new HashMap<String, String>(), AXIS = new HashMap<String, String>();

		static {
			String[][] f = {{"N", "North", "-Z"}, {"S", "South", "+Z"}, {"E", "East", "+X"}, {"W", "West", "-X"}, {"NE", "North East", "+X -Z"}, {"NW", "North West", "-X -Z"}, {"SE", "South East", "+X +Z"}, {"SW", "South West", "-X +Z"}};
			for (String[] r : f) { FULL.put(r[0], r[1]); AXIS.put(r[0], r[2]); }
		}

		Coordinates() { super("coords", "Coordinates", new ItemStack(Items.COMPASS), "Your position, direction, biome and chunk", true, 0.005f, 0.10f); }

		private String dir() {
			String d = mc().player == null ? "N" : facing(mc().player.yaw);
			if (dirFormat.is("Letter and axis")) return d + " " + AXIS.get(d);
			if (dirFormat.is("Full name")) return FULL.get(d);
			return d;
		}

		static int biomeColor(String name) {
			String b = name.toLowerCase(Locale.ROOT);
			if (b.contains("hell") || b.contains("nether")) return 0xFFFF6B5A;
			if (b.contains("end")) return 0xFFD6A8FF;
			if (b.contains("frozen") || b.contains("ice") || b.contains("cold") || b.contains("snow")) return 0xFFDFF4FF;
			if (b.contains("ocean") || b.contains("river")) return 0xFF4FA3FF;
			if (b.contains("stone beach")) return 0xFFB0B4BA;
			if (b.contains("beach")) return 0xFFF2E3A4;
			if (b.contains("desert")) return 0xFFF1D27A;
			if (b.contains("mesa")) return 0xFFE8874A;
			if (b.contains("savanna")) return 0xFFC9C25A;
			if (b.contains("jungle")) return 0xFF3FD35A;
			if (b.contains("swamp")) return 0xFF8BA65A;
			if (b.contains("mushroom")) return 0xFFE07AC6;
			if (b.contains("roofed")) return 0xFF4FA84F;
			if (b.contains("taiga")) return 0xFF6FB89A;
			if (b.contains("flower") || b.contains("sunflower")) return 0xFFA6F07A;
			if (b.contains("extreme") || b.contains("hills")) return 0xFFB0B4BA;
			if (b.contains("forest") || b.contains("birch")) return 0xFF6BD86B;
			if (b.contains("plains")) return 0xFF9FE26F;
			return 0xFFFFFFFF;
		}

		private List<List<Seg>> lines() {
			List<List<Seg>> out = new ArrayList<List<Seg>>();
			ClientPlayerEntity p = mc().player;
			int fg = fg(), lc = customLabels.on() ? labelColor.rgb(0) : fg;
			String f = "%." + decimals.asInt() + "f";
			double x = p == null ? 128 : p.x, y = p == null ? 64 : p.getBoundingBox().minY, z = p == null ? -256 : p.z;
			if (layout.is("Horizontal")) {
				List<Seg> l = new ArrayList<Seg>();
				if (labels.on()) l.add(new Seg("XYZ: ", lc));
				l.add(new Seg(String.format(f, x) + "  " + (showY.on() ? String.format(f, y) + "  " : "") + String.format(f, z), fg));
				if (direction.on()) l.add(new Seg("  " + dir(), fg));
				out.add(l);
			} else {
				out.add(row(lc, fg, "X: ", String.format(f, x)));
				if (showY.on()) out.add(row(lc, fg, "Y: ", String.format(f, y)));
				out.add(row(lc, fg, "Z: ", String.format(f, z)));
				if (direction.on()) out.add(row(lc, fg, "Facing: ", dir()));
			}
			if (chunk.on()) {
				int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
				out.add(row(lc, fg, "Chunk: ", (bx >> 4) + " " + (bz >> 4) + "  (" + (bx & 15) + " " + (bz & 15) + ")"));
			}
			if (otherDim.on() && mc().world != null) {
				int type = mc().world.dimension.getType();
				if (type != 1) {
					boolean nether = type == -1;
					double k = nether ? 8 : 0.125;
					out.add(row(lc, fg, nether ? "Overworld: " : "Nether: ", (int) Math.floor(x * k) + " " + (int) Math.floor(z * k)));
				}
			}
			if (biome.on()) {
				String name = biomeName(mc());
				out.add(row(lc, biomeColor.on() ? biomeColor(name) : fg, "Biome: ", name));
			}
			return out;
		}

		private List<Seg> row(int lc, int vc, String label, String value) {
			List<Seg> l = new ArrayList<Seg>();
			if (labels.on()) l.add(new Seg(label, lc));
			l.add(new Seg(value, vc));
			return l;
		}

		private int lineWidth(List<Seg> l) {
			int w = 0;
			for (Seg s : l) w += Draw.width(s.text);
			return w + Draw.width(bracket(""));
		}

		@Override public int width() {
			int w = 40;
			for (List<Seg> l : lines()) w = Math.max(w, lineWidth(l) + 8);
			return w;
		}

		@Override public int height() { return lines().size() * 10 + 4; }

		@Override public void render(int x, int y) {
			List<List<Seg>> ls = lines();
			int w = 40;
			for (List<Seg> l : ls) w = Math.max(w, lineWidth(l) + 8);
			if (background.on()) panel(x, y, w, ls.size() * 10 + 4);
			String open = bracket("");
			String left = open.isEmpty() ? "" : open.substring(0, 1), right = open.isEmpty() ? "" : open.substring(1);
			int fg = fg();
			for (int i = 0; i < ls.size(); i++) {
				int cx = x + 4, cy = y + 3 + i * 10;
				if (!left.isEmpty()) { text(left, cx, cy, fg); cx += Draw.width(left); }
				for (Seg s : ls.get(i)) { text(s.text, cx, cy, s.color); cx += Draw.width(s.text); }
				if (!right.isEmpty()) text(right, cx, cy, fg);
			}
		}
	}

	static final class Keystrokes extends HudMod {
		final Setting.Toggle mouse = toggle("mouse", "Show mouse buttons", true);
		final Setting.Toggle space = toggle("space", "Show space bar", true);
		final Setting.Toggle cps = toggle("cps", "Show CPS on mouse buttons", true).showIf(() -> mouse.on());
		final Setting.Color pressed = color("pressed", "Pressed color", "FFFFFF");
		private final float[] anim = new float[7];
		private long last = System.currentTimeMillis();

		Keystrokes() { super("keystrokes", "Keystrokes", new ItemStack(Items.FEATHER), "Shows the keys and mouse buttons you press", true, 0.005f, 0.42f); }

		@Override public int width() { return 3 * 22 + 2 * 2; }

		@Override public int height() { return 22 * 2 + 2 + (mouse.on() ? 24 : 0) + (space.on() ? 12 : 0); }

		private void key(int i, int x, int y, int w, int h, String label, boolean down, float dt) {
			anim[i] += ((down ? 1f : 0f) - anim[i]) * Math.min(1f, dt * 18f);
			if (gg.surge.legacy.gui.Theme.glass()) {
				if (background.on()) panel(x, y, w, h);
				if (anim[i] > 0.01f) {
					Draw.glass(x, y, w, h, Math.min(5, Math.min(w, h) / 3), 0.4f + anim[i] * 2.2f);
					Draw.rect(x + 2, y + 2, w - 4, h - 4, Draw.alpha(pressed.rgb(0), anim[i] * 0.5f));
				}
			} else {
				int base = background.on() ? bg() : 0;
				Draw.rect(x, y, w, h, Draw.lerp(base, Draw.alpha(pressed.rgb(0), 0.85f), anim[i]));
			}
			int tc = Draw.lerp(fg(), 0xFF111111, anim[i]);
			Draw.text(label, x + (w - Draw.width(label)) / 2, y + (h - 8) / 2, tc, shadow.on() && anim[i] < 0.5f);
		}

		@Override public void render(int x, int y) {
			long now = System.currentTimeMillis();
			float dt = Math.min(0.1f, (now - last) / 1000f);
			last = now;
			MinecraftClient mc = mc();
			KeyBinding[] k = {mc.options.forwardKey, mc.options.leftKey, mc.options.backKey, mc.options.rightKey};
			key(0, x + 24, y, 22, 22, keyName(k[0]), k[0].isPressed(), dt);
			key(1, x, y + 24, 22, 22, keyName(k[1]), k[1].isPressed(), dt);
			key(2, x + 24, y + 24, 22, 22, keyName(k[2]), k[2].isPressed(), dt);
			key(3, x + 48, y + 24, 22, 22, keyName(k[3]), k[3].isPressed(), dt);
			int yy = y + 48;
			if (mouse.on()) {
				key(4, x, yy, 34, 22, cps.on() ? Stats.leftCps() + " CPS" : "LMB", mc.options.attackKey.isPressed(), dt);
				key(5, x + 36, yy, 34, 22, cps.on() ? Stats.rightCps() + " CPS" : "RMB", mc.options.useKey.isPressed(), dt);
				yy += 24;
			}
			if (space.on()) key(6, x, yy, 70, 10, "-----", mc.options.jumpKey.isPressed(), dt);
		}

		private static String keyName(KeyBinding b) {
			String n = net.minecraft.client.option.GameOptions.getFormattedNameForKeyCode(b.getCode());
			return n.length() > 3 ? n.substring(0, 3) : n;
		}
	}

	static final class ArmorStatus extends HudMod {
		final Setting.Choice show = choice("show", "Durability", "Number", "Number", "Percent", "Hidden");
		final Setting.Toggle hand = toggle("hand", "Show held item", true);

		ArmorStatus() { super("armor", "Armor Status", new ItemStack(Items.DIAMOND_CHESTPLATE), "Your armor and its durability", false, 1f, 0.75f); }

		private List<ItemStack> items() {
			List<ItemStack> out = new ArrayList<ItemStack>();
			ClientPlayerEntity p = mc().player;
			boolean empty = p == null;
			if (p != null) {
				boolean any = false;
				for (ItemStack a : p.inventory.armor) if (a != null) any = true;
				empty = !any && editing();
			}
			if (empty) {
				out.add(new ItemStack(Items.DIAMOND_HELMET));
				out.add(new ItemStack(Items.DIAMOND_CHESTPLATE));
				out.add(new ItemStack(Items.DIAMOND_LEGGINGS));
				out.add(new ItemStack(Items.DIAMOND_BOOTS));
				return out;
			}
			for (int i = 3; i >= 0; i--) if (p.inventory.armor[i] != null) out.add(p.inventory.armor[i]);
			if (hand.on() && p.inventory.getMainHandStack() != null) out.add(p.inventory.getMainHandStack());
			return out;
		}

		private String dur(ItemStack s) {
			if (!s.isDamageable() || show.is("Hidden")) return s.count > 1 ? String.valueOf(s.count) : "";
			int left = s.getMaxDamage() - s.getDamage();
			return show.is("Percent") ? (left * 100 / Math.max(1, s.getMaxDamage())) + "%" : String.valueOf(left);
		}

		@Override public int width() {
			int w = 20;
			for (ItemStack s : items()) w = Math.max(w, 22 + Draw.width(dur(s)));
			return w + 4;
		}

		@Override public int height() { return Math.max(18, items().size() * 18 + 2); }

		@Override public void render(int x, int y) {
			List<ItemStack> list = items();
			if (background.on()) panel(x, y, width(), height());
			int yy = y + 1;
			for (ItemStack s : list) {
				Draw.item(s, x + 2, yy, 16);
				String d = dur(s);
				int c = fg();
				if (s.isDamageable()) {
					float f = 1f - s.getDamage() / (float) Math.max(1, s.getMaxDamage());
					if (f < 0.25f) c = 0xFFFF5555; else if (f < 0.5f) c = 0xFFFFCC55;
				}
				text(d, x + 22, yy + 4, c);
				yy += 18;
			}
		}
	}

	static final class Potions extends HudMod {
		private static final Identifier INV = new Identifier("textures/gui/container/inventory.png");
		final Setting.Toggle icons = toggle("icons", "Show icons", true);

		Potions() { super("potions", "Potion Effects", new ItemStack(Items.POTION), "Your active effects and how long they last", true, 1f, 0.45f); }

		private List<String[]> rows() {
			List<String[]> out = new ArrayList<String[]>();
			ClientPlayerEntity p = mc().player;
			if (p == null || p.getStatusEffectInstances().isEmpty() && editing()) { out.add(new String[]{"Speed II", "1:30", "0"}); out.add(new String[]{"Strength", "0:45", "4"}); return out; }
			for (StatusEffectInstance e : p.getStatusEffectInstances()) {
				StatusEffect eff = StatusEffect.STATUS_EFFECTS[e.getEffectId()];
				if (eff == null) continue;
				String name = I18n.translate(eff.getTranslationKey());
				if (e.getAmplifier() > 0) name += " " + roman(e.getAmplifier() + 1);
				int sec = e.getDuration() / 20;
				String t = e.getDuration() > 32767 * 0.9 ? "**:**" : (sec / 60) + ":" + String.format("%02d", sec % 60);
				out.add(new String[]{name, t, String.valueOf(eff.hasIcon() ? eff.getIconLevel() : -1)});
			}
			return out;
		}

		private static String roman(int n) {
			String[] r = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
			return n < r.length ? r[n] : String.valueOf(n);
		}

		@Override public int width() {
			int w = 60;
			for (String[] r : rows()) w = Math.max(w, Math.max(Draw.width(r[0]), Draw.width(r[1])) + (icons.on() ? 26 : 8));
			return w;
		}

		@Override public int height() { return Math.max(22, rows().size() * 22); }

		@Override public void render(int x, int y) {
			List<String[]> rows = rows();
			if (rows.isEmpty()) return;
			if (background.on()) panel(x, y, width(), height());
			int yy = y;
			for (String[] r : rows) {
				int tx = x + 4;
				int icon = Integer.parseInt(r[2]);
				if (icons.on()) {
					if (icon >= 0) {
						GlStateManager.color(1f, 1f, 1f, 1f);
						GlStateManager.enableBlend();
						mc().getTextureManager().bindTexture(INV);
						net.minecraft.client.gui.DrawableHelper.drawTexture(x + 3, yy + 2, icon % 8 * 18, 198 + icon / 8 * 18, 18, 18, 256, 256);
					}
					tx = x + 24;
				}
				text(r[0], tx, yy + 2, fg());
				text(r[1], tx, yy + 12, 0xFFAAAAAA);
				yy += 22;
			}
		}
	}
}
