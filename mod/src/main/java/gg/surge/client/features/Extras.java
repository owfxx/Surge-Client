package gg.surge.client.features;

import gg.surge.client.mixin.OptionsAccessor;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class Extras {
	private Extras() {}

	private static final Map<EquipmentSlot, Boolean> WARNED = new HashMap<>();
	private static long dropConfirmAt;
	private static int dropConfirmSlot = -1;
	private static int tick;

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) return;
		tick++;
		if (Mods.TRAILS.enabled()) trails(mc);
		if (Mods.DURABILITY.enabled() && tick % 10 == 0) durability(mc);
	}

	private static ParticleOptions trailParticle() {
		return switch (Mods.TRAIL_TYPE.get()) {
			case "Soul" -> ParticleTypes.SOUL_FIRE_FLAME;
			case "Crit" -> ParticleTypes.CRIT;
			case "Magic" -> ParticleTypes.ENCHANTED_HIT;
			case "Hearts" -> ParticleTypes.HEART;
			case "Notes" -> ParticleTypes.NOTE;
			case "End rod" -> ParticleTypes.END_ROD;
			case "Cherry" -> ParticleTypes.GLOW;
			case "Smoke" -> ParticleTypes.SMOKE;
			default -> ParticleTypes.FLAME;
		};
	}

	private static void trails(Minecraft mc) {
		ParticleOptions type = trailParticle();
		int amount = Mods.TRAIL_AMOUNT.asInt();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof Projectile pr)) continue;
			if (e.distanceToSqr(mc.player) > 96 * 96) continue;
			if (Mods.TRAIL_OWN.on() && pr.getOwner() != mc.player) continue;
			if (e.getDeltaMovement().lengthSqr() < 0.01) continue;
			for (int i = 0; i < amount; i++) {
				double t = i / (double) amount;
				double x = e.xo + (e.getX() - e.xo) * t, y = e.yo + (e.getY() - e.yo) * t, z = e.zo + (e.getZ() - e.zo) * t;
				mc.level.addParticle(type, x, y + e.getBbHeight() / 2, z, 0, 0, 0);
			}
		}
	}

	public static void render(GuiGraphics g, Minecraft mc) {
	}

	private static void durability(Minecraft mc) {
		List<EquipmentSlot> slots = new ArrayList<>();
		if (Mods.DURA_ARMOR.on()) slots.addAll(List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET));
		if (Mods.DURA_HANDS.on()) slots.addAll(List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND));
		float limit = Mods.DURA_PERCENT.get() / 100f;
		for (EquipmentSlot slot : slots) {
			ItemStack s = mc.player.getItemBySlot(slot);
			boolean low = !s.isEmpty() && s.isDamageableItem() && (s.getMaxDamage() - s.getDamageValue()) / (float) s.getMaxDamage() <= limit;
			if (low && !WARNED.getOrDefault(slot, false)) {
				int pct = Math.round((s.getMaxDamage() - s.getDamageValue()) * 100f / s.getMaxDamage());
				mc.gui.setOverlayMessage(Component.literal("⚠ " + s.getHoverName().getString() + " is almost broken (" + pct + "%)").withStyle(st -> st.withColor(0xFF6B6B)), false);
				if (Mods.DURA_SOUND.on()) mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 1.6f, 0.4f));
			}
			WARNED.put(slot, low);
		}
	}

	private static boolean valuable(ItemStack s) {
		if (Mods.DROP_ALL.on()) return true;
		if (Mods.DROP_ENCHANTED.on() && s.isEnchanted()) return true;
		if (Mods.DROP_NAMED.on() && s.has(DataComponents.CUSTOM_NAME)) return true;
		if (Mods.DROP_RARE.on()) {
			if (s.is(Items.TOTEM_OF_UNDYING) || s.is(Items.ELYTRA) || s.is(Items.ENCHANTED_GOLDEN_APPLE) || s.is(Items.NETHER_STAR) || s.is(Items.BEACON) || s.is(Items.MACE)) return true;
			String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
			if (id.startsWith("netherite_")) return true;
			if (id.startsWith("diamond_") && s.isDamageableItem()) return true;
		}
		return false;
	}

	public static boolean allowDrop(Minecraft mc) {
		if (!Mods.DROP_PROTECT.enabled() || mc.player == null) return true;
		ItemStack s = mc.player.getMainHandItem();
		if (s.isEmpty() || !valuable(s)) return true;
		int slot = mc.player.getInventory().getSelectedSlot();
		long now = System.currentTimeMillis();
		if (slot == dropConfirmSlot && now - dropConfirmAt < 1500) {
			dropConfirmSlot = -1;
			return true;
		}
		dropConfirmSlot = slot;
		dropConfirmAt = now;
		mc.gui.setOverlayMessage(Component.literal("Press drop again to drop " + s.getHoverName().getString()).withStyle(st -> st.withColor(0xFFE07A)), false);
		return false;
	}

	public static long dayTime(long real) {
		long base = switch (Mods.TIME_PRESET.get()) {
			case "Sunrise" -> 23500;
			case "Noon" -> 6000;
			case "Sunset" -> 12500;
			case "Night" -> 14500;
			case "Midnight" -> 18000;
			case "Custom" -> Mods.TIME_CUSTOM.asInt();
			default -> 3000;
		};
		if (Mods.TIME_CYCLE.on()) base += real % 24000;
		return ((real / 24000) * 24000) + (base % 24000);
	}

	public static void applyGlint(Minecraft mc) {
		if (mc == null || mc.options == null) return;
		OptionsAccessor o = (OptionsAccessor) mc.options;
		boolean on = Mods.GLINT.enabled();
		o.surge$glintSpeed().set(on ? Mods.GLINT_SPEED.get() / 100.0 : 0.5);
		o.surge$glintStrength().set(on ? Mods.GLINT_STRENGTH.get() / 100.0 : 0.75);
	}
}
