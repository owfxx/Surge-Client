package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Qol {
	private Qol() {}

	private record Pickup(Item item, ItemStack icon, int[] count, long[] at) {}

	private static final List<Pickup> PICKUPS = new ArrayList<>();
	private static Map<Item, Integer> lastCounts = null;
	private static boolean wasDead;
	private static long lastBeat;
	private static int lastScreenHash;

	public static void tick(Minecraft mc) {
		if (mc.player == null) { lastCounts = null; return; }
		boolean dead = mc.player.isDeadOrDying();
		if (dead && !wasDead && Mods.DEATH_COORDS.enabled()) deathMessage(mc);
		wasDead = dead;
		if (Mods.ITEM_PICKUPS.enabled()) pickups(mc);
		else lastCounts = null;
		if (Mods.LOW_HEALTH.enabled() && Mods.LH_SOUND.on() && low(mc) && !dead) {
			long now = System.currentTimeMillis();
			float hp = mc.player.getHealth();
			long gap = (long) (550 + 500 * Math.max(0f, hp / Math.max(1f, Mods.LH_HEALTH.get())));
			if (now - lastBeat > gap) { lastBeat = now; CustomSounds.preview("preset:thump"); }
		}
	}

	private static boolean low(Minecraft mc) {
		return mc.player != null && !mc.player.isCreative() && !mc.player.isSpectator() && mc.player.getHealth() <= Mods.LH_HEALTH.get();
	}

	private static void deathMessage(Minecraft mc) {
		var p = mc.player;
		String coords = p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ();
		String dim = mc.level == null ? "" : mc.level.dimension().identifier().getPath().replace('_', ' ');
		Component c = Component.literal("You died at ").withStyle(ChatFormatting.GRAY)
			.append(Component.literal(coords).withStyle(s -> s.withColor(0xFFE07A).withUnderlined(true)
				.withClickEvent(new ClickEvent.CopyToClipboard(coords))
				.withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy")))))
			.append(Component.literal(dim.isEmpty() ? "" : " in the " + dim).withStyle(ChatFormatting.GRAY));
		mc.gui.getChat().addMessage(c);
	}

	private static void pickups(Minecraft mc) {
		Map<Item, Integer> now = new HashMap<>();
		var inv = mc.player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (!s.isEmpty()) now.merge(s.getItem(), s.getCount(), Integer::sum);
		}
		int screen = mc.screen == null ? 0 : System.identityHashCode(mc.screen);
		if (lastCounts != null && screen == 0 && lastScreenHash == 0) {
			long t = System.currentTimeMillis();
			for (var e : now.entrySet()) {
				int diff = e.getValue() - lastCounts.getOrDefault(e.getKey(), 0);
				if (diff <= 0) continue;
				Pickup found = null;
				for (Pickup p : PICKUPS) if (p.item() == e.getKey() && t - p.at()[0] < 3000) { found = p; break; }
				if (found != null) { found.count()[0] += diff; found.at()[0] = t; }
				else PICKUPS.add(new Pickup(e.getKey(), new ItemStack(e.getKey()), new int[]{diff}, new long[]{t}));
			}
			while (PICKUPS.size() > 6) PICKUPS.remove(0);
		}
		lastScreenHash = screen;
		lastCounts = now;
	}

	public static void render(GuiGraphics g, Minecraft mc) {
		if (mc.player == null) return;
		if (Mods.LOW_HEALTH.enabled() && Mods.LH_VIGNETTE.on() && low(mc)) {
			float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 160.0);
			int a = (int) (110 * pulse);
			int w = g.guiWidth(), h = g.guiHeight(), d = Math.max(24, h / 6);
			int c = (a << 24) | 0xC01818;
			g.fillGradient(0, 0, w, d, c, 0);
			g.fillGradient(0, h - d, w, h, 0, c);
			for (int i = 0; i < d; i++) {
				int al = (int) (a * (1f - i / (float) d));
				g.fill(i, 0, i + 1, h, (al << 24) | 0xC01818);
				g.fill(w - i - 1, 0, w - i, h, (al << 24) | 0xC01818);
			}
		}
		if (Mods.ITEM_PICKUPS.enabled() && !PICKUPS.isEmpty()) {
			long t = System.currentTimeMillis();
			PICKUPS.removeIf(p -> t - p.at()[0] > 3500);
			int x = g.guiWidth() - 8, y = g.guiHeight() - 60;
			for (int i = PICKUPS.size() - 1; i >= 0; i--) {
				Pickup p = PICKUPS.get(i);
				float age = (t - p.at()[0]) / 3500f;
				float a = age > 0.75f ? 1f - (age - 0.75f) / 0.25f : Math.min(1f, (t - p.at()[0]) / 150f);
				String s = "+" + p.count()[0] + " " + p.icon().getHoverName().getString();
				int tw = mc.font.width(s);
				int bx = x - tw - 26;
				int slide = (int) ((1f - Math.min(1f, (t - p.at()[0]) / 180f)) * 20);
				g.fill(bx + slide, y - 2, x + slide, y + 18, ((int) (150 * a) << 24) | 0x0E0E11);
				g.renderItem(p.icon(), bx + 3 + slide, y);
				g.drawString(mc.font, s, bx + 22 + slide, y + 5, ((int) (255 * a) << 24) | 0xFFFFFF, false);
				y -= 22;
			}
		}
	}

	public static void tooltip(ItemStack stack, List<Component> lines) {
		if (!Mods.TOOLTIPS.enabled() || stack.isEmpty()) return;
		if (Mods.TT_DURABILITY.on() && stack.isDamageableItem()) {
			int left = stack.getMaxDamage() - stack.getDamageValue();
			float pct = left / (float) stack.getMaxDamage();
			int col = pct > 0.5f ? 0x8CFFA8 : pct > 0.2f ? 0xFFE07A : 0xFF7A7A;
			lines.add(Component.literal("Durability: ").withStyle(ChatFormatting.GRAY).append(Component.literal(left + " / " + stack.getMaxDamage()).withStyle(s -> s.withColor(col))));
		}
		if (Mods.TT_FOOD.on()) {
			var food = stack.get(DataComponents.FOOD);
			if (food != null) lines.add(Component.literal("Food: ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal(food.nutrition() / 2f + " hunger").withStyle(s -> s.withColor(0xFFC56B)))
				.append(Component.literal(", " + String.format(java.util.Locale.ROOT, "%.1f", food.saturation()) + " saturation").withStyle(s -> s.withColor(0xFFE07A))));
		}
		if (Mods.TT_STACK.on() && stack.getMaxStackSize() > 1 && stack.getCount() > 1) {
			lines.add(Component.literal("Stack: " + stack.getCount() + " / " + stack.getMaxStackSize()).withStyle(ChatFormatting.DARK_GRAY));
		}
	}
}
