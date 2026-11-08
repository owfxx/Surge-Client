package gg.surge.legacy.perf;

import gg.surge.legacy.mod.Category;
import gg.surge.legacy.mod.Mod;
import gg.surge.legacy.mod.Setting;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class Perf {
	public static final List<Mod> ALL = new ArrayList<Mod>();
	private static long frame;

	private Perf() {}

	private static Mod add(Mod m) {
		ALL.add(m);
		return m;
	}

	public static final Mod CULLING = add(new Mod("culling", "Hidden Mob Culling", Category.PERFORMANCE, new ItemStack(Blocks.CHEST), "Skips mobs, items and chests hidden behind walls. Players are always drawn", true));
	public static final Setting.Toggle CULL_BLOCKS = CULLING.toggle("blocks", "Also skip chests and signs", true);

	public static final Mod DISTANCE = add(new Mod("distance", "Entity Distance", Category.PERFORMANCE, new ItemStack(Items.ARROW), "Stops drawing small things that are far away", true));
	public static final Setting.Slider ITEM_DIST = DISTANCE.slider("items", "Dropped items and XP", 48, 16, 128, 8, " blocks");
	public static final Setting.Slider DECOR_DIST = DISTANCE.slider("decor", "Armor stands and item frames", 64, 16, 128, 8, " blocks");
	public static final Setting.Slider TILE_DIST = DISTANCE.slider("tiles", "Chests, signs and heads", 64, 16, 128, 8, " blocks");

	public static final Mod BACKGROUND = add(new Mod("bgfps", "Background FPS", Category.PERFORMANCE, new ItemStack(Items.CLOCK), "Lowers FPS while the game is in the background so your PC stays cool", true));
	public static final Setting.Slider BG_FPS = BACKGROUND.slider("fps", "FPS in background", 30, 5, 60, 5, "");

	public static final Mod STACKS = add(new Mod("stacks", "Fast Item Stacks", Category.PERFORMANCE, new ItemStack(Items.DIAMOND), "Draws one model for each dropped stack instead of up to five", true));

	public static final Mod PARTICLE_LIMIT = add(new Mod("particlelimit", "Particle Limit", Category.PERFORMANCE, new ItemStack(Items.FIREWORKS), "Caps how many particles can exist at once", false));
	public static final Setting.Slider PARTICLE_MAX = PARTICLE_LIMIT.slider("max", "Most particles", 1000, 200, 4000, 100, "");

	public static void frameStart() {
		frame++;
	}

	public static long frame() {
		return frame;
	}

	public static boolean skipEntity(Entity e, double camX, double camY, double camZ) {
		if (e instanceof PlayerEntity) return false;
		double dx = e.x - camX, dy = e.y - camY, dz = e.z - camZ, d2 = dx * dx + dy * dy + dz * dz;
		if (DISTANCE.enabled()) {
			double lim = 0;
			if (e instanceof ItemEntity || e instanceof ExperienceOrbEntity) lim = ITEM_DIST.get();
			else if (e instanceof ArmorStandEntity || e instanceof ItemFrameEntity) lim = DECOR_DIST.get();
			if (lim > 0 && d2 > lim * lim && !e.hasCustomName()) return true;
		}
		return CULLING.enabled() && Occlusion.hidden(e, camX, camY, camZ);
	}

	public static boolean skipBlockEntity(BlockEntity be, double camX, double camY, double camZ) {
		if (be instanceof BeaconBlockEntity) return false;
		if (DISTANCE.enabled()) {
			double dx = be.getPos().getX() + 0.5 - camX, dy = be.getPos().getY() + 0.5 - camY, dz = be.getPos().getZ() + 0.5 - camZ;
			double lim = TILE_DIST.get();
			if (dx * dx + dy * dy + dz * dz > lim * lim) return true;
		}
		return CULLING.enabled() && CULL_BLOCKS.on() && Occlusion.hidden(be.getPos(), camX, camY, camZ);
	}

	public static int maxFps(int current) {
		if (BACKGROUND.enabled() && !org.lwjgl.opengl.Display.isActive()) return Math.min(current, BG_FPS.asInt());
		return current;
	}

	public static void firstRun(MinecraftClient mc) {
		File mark = new File(mc.runDirectory, ".surge-fps");
		if (mark.exists()) return;
		mc.options.vsync = false;
		mc.options.maxFramerate = 260;
		try { org.lwjgl.opengl.Display.setVSyncEnabled(false); } catch (Throwable ignored) { }
		mc.options.save();
		try { mark.createNewFile(); } catch (Exception ignored) { }
	}
}
