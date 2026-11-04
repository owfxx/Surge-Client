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

	private record Popup(double x, double y, double z, float amount, long at, double drift) {}

	private static final Map<Integer, Float> HEALTH = new HashMap<>();
	private static final List<Popup> POPUPS = new ArrayList<>();
	private static final Map<EquipmentSlot, Boolean> WARNED = new HashMap<>();
	private static long dropConfirmAt;
	private static int dropConfirmSlot = -1;
	private static int tick;

	public static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) {
			HEALTH.clear();
			POPUPS.clear();
			return;
		}
		tick++;
		if (Mods.TRAILS.enabled()) trails(mc);
		if (Mods.DAMAGE_NUMBERS.enabled()) trackHealth(mc);
		else HEALTH.clear();
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

	private static void trackHealth(Minecraft mc) {
		Map<Integer, Float> seen = new HashMap<>();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof LivingEntity le) || e.distanceToSqr(mc.player) > 32 * 32) continue;
			if (le == mc.player && !Mods.DMG_OWN.on()) continue;
			float hp = le.getHealth();
			Float before = HEALTH.get(e.getId());
			seen.put(e.getId(), hp);
			if (before == null) continue;
			float diff = before - hp;
			if (diff >= 0.5f || (diff <= -0.5f && Mods.DMG_HEAL.on())) {
				POPUPS.add(new Popup(e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), diff, System.currentTimeMillis(), (Math.random() - 0.5) * 0.6));
				if (POPUPS.size() > 60) POPUPS.remove(0);
			}
		}
		HEALTH.clear();
		HEALTH.putAll(seen);
	}

	public static void render(GuiGraphics g, Minecraft mc) {
		if (!Mods.DAMAGE_NUMBERS.enabled() || POPUPS.isEmpty() || mc.player == null) return;
		Waypoints.setupCamera(g, mc);
		long now = System.currentTimeMillis();
		float size = Mods.DMG_SIZE.get() / 100f;
		boolean hearts = Mods.DMG_FORMAT.is("Hearts");
		int col = Mods.DMG_COLOR.rgb(0) & 0xFFFFFF;
		Iterator<Popup> it = POPUPS.iterator();
		while (it.hasNext()) {
			Popup p = it.next();
			float age = (now - p.at()) / 1200f;
			if (age >= 1f) { it.remove(); continue; }
			double[] s = Waypoints.project(p.x() + p.drift() * age, p.y() + age * 0.8, p.z());
			if (s == null) continue;
			float amt = Math.abs(p.amount());
			String text = (p.amount() < 0 ? "+" : "-") + (hearts ? (amt / 2f == Math.floor(amt / 2f) ? String.valueOf((int) (amt / 2f)) : String.format("%.1f", amt / 2f)) + "❤" : String.format("%.1f", amt));
			int c = p.amount() < 0 ? 0x55FF77 : col;
			int alpha = (int) (255 * Math.min(1f, (1f - age) * 2.5f));
			float sc = (float) Math.max(0.6, Math.min(2.2, 6.0 / Math.max(1.0, s[2]))) * size;
			g.pose().pushMatrix();
			g.pose().translate((float) s[0], (float) s[1]);
			g.pose().scale(sc, sc);
			g.drawString(mc.font, text, -mc.font.width(text) / 2, -4, (alpha << 24) | c, true);
			g.pose().popMatrix();
		}
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
