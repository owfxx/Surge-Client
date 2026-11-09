package gg.surge.client.hud;

import gg.surge.client.gui.HudEditorScreen;
import gg.surge.client.mods.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MoreModules {
	private MoreModules() {}

	private static long tpsAt;
	private static long tpsGame = -1;
	private static double tps = 20;
	private static long cpuAt;
	private static double cpu;

	static List<HudModule> create() {
		List<HudModule> out = new ArrayList<>();
		out.add(new LookAt());
		out.add(new MouseStrokes());
		out.add(new Paperdoll());
		out.add(new HorseStats());
		out.add(new HudModules.TextModule("tps", "TPS", Items.REPEATER, "How fast the server is running, 20 is perfect", false, 1f, 0.42f, "TPS", "After", "20.0", mc -> String.format(Locale.ROOT, "%.1f", tps)));
		out.add(new HudModules.TextModule("system", "CPU Usage", Items.OBSERVER, "How much of your processor the game is using", false, 1f, 0.465f, "CPU", "Before", "18%", mc -> Math.round(cpu * 100) + "%"));
		return out;
	}

	static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) { tpsGame = -1; return; }
		long now = System.currentTimeMillis(), game = mc.level.getGameTime();
		if (tpsGame < 0) { tpsGame = game; tpsAt = now; }
		else if (now - tpsAt >= 2000) {
			double t = (game - tpsGame) / ((now - tpsAt) / 1000.0);
			tps = Math.max(0, Math.min(20, tps * 0.5 + t * 0.5));
			tpsGame = game;
			tpsAt = now;
		}
		if (now - cpuAt > 1000) {
			cpuAt = now;
			try {
				var os = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
				double v = os.getProcessCpuLoad();
				if (v >= 0) cpu = cpu * 0.4 + v * 0.6;
			} catch (Throwable ignored) { }
		}
	}

	static final class LookAt extends HudModule {
		final Setting.Toggle entities = toggle("entities", "Show mobs too", true);

		LookAt() { super("lookat", "Look At", Items.SPYGLASS, "Shows the name of the block or mob you're looking at", false, 0.5f - 0.06f, 0.075f); }

		private record Info(ItemStack icon, String name, String sub) {}

		private Info info(Minecraft mc) {
			if (mc.player == null || mc.level == null) return new Info(new ItemStack(Items.GRASS_BLOCK), "Grass Block", "minecraft");
			HitResult hr = mc.hitResult;
			if (hr instanceof EntityHitResult eh && entities.on()) {
				Entity e = eh.getEntity();
				String sub = e instanceof LivingEntity le ? String.format(Locale.ROOT, "%.1f / %.0f ❤", le.getHealth() / 2f, le.getMaxHealth() / 2f) : "";
				ItemStack egg = net.minecraft.world.item.SpawnEggItem.byId(e.getType()) != null ? new ItemStack(net.minecraft.world.item.SpawnEggItem.byId(e.getType())) : ItemStack.EMPTY;
				return new Info(egg, e.getDisplayName().getString(), sub);
			}
			if (hr instanceof BlockHitResult bh && hr.getType() == HitResult.Type.BLOCK) {
				BlockState st = mc.level.getBlockState(bh.getBlockPos());
				if (st.isAir()) return null;
				ItemStack icon = new ItemStack(st.getBlock().asItem());
				String ns = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getNamespace();
				return new Info(icon, st.getBlock().getName().getString(), ns.equals("minecraft") ? "" : ns);
			}
			return null;
		}

		@Override public int width(Minecraft mc) {
			Info i = info(mc);
			if (i == null) return 80;
			return Math.max(80, Math.max(mc.font.width(i.name()), mc.font.width(i.sub())) + 30);
		}

		@Override public int height(Minecraft mc) { return 22; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			Info i = info(mc);
			if (i == null) {
				if (mc.screen instanceof HudEditorScreen) i = new Info(new ItemStack(Items.GRASS_BLOCK), "Grass Block", "");
				else return;
			}
			int w = width(mc);
			if (bgOn()) panel(g, x, y, x + w, y + 22);
			if (!i.icon().isEmpty()) g.renderItem(i.icon(), x + 3, y + 3);
			if (i.sub().isEmpty()) text(g, mc, i.name(), x + 23, y + 7, fg());
			else {
				text(g, mc, i.name(), x + 23, y + 3, fg());
				g.drawString(mc.font, i.sub(), x + 23, y + 12, 0xFFA1A1AA, false);
			}
		}
	}

	static final class MouseStrokes extends HudModule {
		final Setting.Color dot = color("dot", "Dot color", "FFFFFF");
		private float mx, my, lastYaw = Float.NaN, lastPitch;
		private final float[][] trail = new float[14][2];
		private long last = System.nanoTime();

		MouseStrokes() { super("mousestrokes", "Mouse Strokes", Items.STRING, "Shows how you move your mouse", false, 1f, 0.55f); }

		@Override public int width(Minecraft mc) { return 48; }

		@Override public int height(Minecraft mc) { return 48; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			long now = System.nanoTime();
			float dt = Math.min(0.1f, (now - last) / 1e9f);
			last = now;
			if (mc.player != null) {
				float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
				if (!Float.isNaN(lastYaw)) { mx += (yaw - lastYaw) * 0.6f; my += (pitch - lastPitch) * 0.6f; }
				lastYaw = yaw;
				lastPitch = pitch;
			}
			float k = (float) Math.exp(-dt * 8);
			mx *= k;
			my *= k;
			mx = Math.max(-20, Math.min(20, mx));
			my = Math.max(-20, Math.min(20, my));
			System.arraycopy(trail, 0, trail, 1, trail.length - 1);
			trail[0] = new float[]{mx, my};
			if (bgOn()) panel(g, x, y, x + 48, y + 48);
			g.fill(x + 23, y + 6, x + 25, y + 42, 0x30FFFFFF);
			g.fill(x + 6, y + 23, x + 42, y + 25, 0x30FFFFFF);
			int rgb = dot.rgb(0) & 0xFFFFFF;
			for (int i = trail.length - 1; i >= 0; i--) {
				int a = (int) (200 * (1f - i / (float) trail.length)) & 0xFF;
				int px = x + 24 + Math.round(trail[i][0]), py = y + 24 + Math.round(trail[i][1]);
				int r = i == 0 ? 2 : 1;
				g.fill(px - r, py - r, px + r, py + r, (a << 24) | rgb);
			}
		}
	}

	static final class Paperdoll extends HudModule {
		final Setting.Toggle onlyMoving = toggle("moving", "Only while sprinting, swimming or flying", false);

		Paperdoll() { super("paperdoll", "Paperdoll", Items.ARMOR_STAND, "A small model of your character on screen", false, 0.02f, 0.3f); }

		@Override public int width(Minecraft mc) { return 40; }

		@Override public int height(Minecraft mc) { return 66; }

		@Override public void draw(GuiGraphics g, Minecraft mc) {
			if (mc.player == null) return;
			var p = mc.player;
			if (onlyMoving.on() && !(mc.screen instanceof HudEditorScreen) && !(p.isSprinting() || p.isSwimming() || p.isFallFlying() || p.getAbilities().flying)) return;
			int x = screenX(g, mc), y = screenY(g, mc);
			float s = scale();
			int w = Math.round(40 * s), h = Math.round(66 * s);
			if (bgOn()) panel(g, x, y, x + w, y + h);
			InventoryScreen.renderEntityInInventoryFollowsMouse(g, x, y, x + w, y + h, Math.max(8, Math.round(28 * s)), 0.0625f, x + w / 2f + 30, y + h / 2f - 10, p);
		}

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) { }
	}

	static final class HorseStats extends HudModule {
		HorseStats() { super("horse", "Horse Stats", Items.SADDLE, "Speed, jump height and health of the horse you ride or look at", false, 1f, 0.2f); }

		private AbstractHorse horse(Minecraft mc) {
			if (mc.player == null) return null;
			if (mc.player.getVehicle() instanceof AbstractHorse h) return h;
			if (mc.crosshairPickEntity instanceof AbstractHorse h) return h;
			return null;
		}

		private String[] lines(Minecraft mc) {
			AbstractHorse h = horse(mc);
			if (h == null) return new String[]{"Speed  9.5 b/s", "Jump  3.2 blocks", "Health  13 ❤"};
			double speed = h.getAttributeValue(Attributes.MOVEMENT_SPEED) * 42.16;
			double j = h.getAttributeValue(Attributes.JUMP_STRENGTH);
			double jump = -0.1817584952 * j * j * j + 3.689713992 * j * j + 2.128599134 * j - 0.343930367;
			return new String[]{String.format(Locale.ROOT, "Speed  %.1f b/s", speed), String.format(Locale.ROOT, "Jump  %.1f blocks", jump), String.format(Locale.ROOT, "Health  %.0f ❤", h.getMaxHealth() / 2f)};
		}

		@Override public int width(Minecraft mc) {
			int w = 60;
			for (String l : lines(mc)) w = Math.max(w, mc.font.width(l) + 10);
			return w;
		}

		@Override public int height(Minecraft mc) { return 34; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			if (horse(mc) == null && !(mc.screen instanceof HudEditorScreen)) return;
			int w = width(mc);
			if (bgOn()) panel(g, x, y, x + w, y + 34);
			String[] ls = lines(mc);
			for (int i = 0; i < ls.length; i++) text(g, mc, ls[i], x + 5, y + 3 + i * 10, fg());
		}
	}
}
