package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

public final class AppleSkin {
	private static final Identifier FULL = Identifier.fromNamespaceAndPath("minecraft", "hud/food_full");
	private static final Identifier HALF = Identifier.fromNamespaceAndPath("minecraft", "hud/food_half");

	private AppleSkin() {}

	private static FoodProperties held(LocalPlayer p) {
		for (InteractionHand hand : InteractionHand.values()) {
			ItemStack s = p.getItemInHand(hand);
			FoodProperties f = s.isEmpty() ? null : s.get(DataComponents.FOOD);
			if (f != null) return f;
		}
		return null;
	}

	public static void render(GuiGraphics g, Minecraft mc) {
		if (!Mods.APPLESKIN.enabled() || mc.player == null || mc.options.hideGui) return;
		LocalPlayer p = mc.player;
		if (p.getAbilities().instabuild || p.isSpectator()) return;
		int right = g.guiWidth() / 2 + 91;
		int y = g.guiHeight() - 39;
		int food = p.getFoodData().getFoodLevel();
		float sat = p.getFoodData().getSaturationLevel();

		if (Mods.APPLE_PREVIEW.on()) {
			FoodProperties f = held(p);
			if (f != null) {
				int after = Math.min(20, food + f.nutrition());
				float alpha = 0.45f + 0.35f * (float) Math.sin(System.currentTimeMillis() / 180.0);
				for (int i = 0; i < 10; i++) {
					int points = i * 2 + 2;
					if (points <= food) continue;
					int x = right - i * 8 - 9;
					if (after >= points) g.blitSprite(RenderPipelines.GUI_TEXTURED, FULL, x, y, 9, 9, alpha);
					else if (after == points - 1) g.blitSprite(RenderPipelines.GUI_TEXTURED, HALF, x, y, 9, 9, alpha);
				}
			}
		}

		if (Mods.APPLE_SAT.on() && sat > 0) {
			for (int i = 0; i < 10; i++) {
				float s = Math.max(0, Math.min(2, sat - i * 2));
				if (s <= 0) break;
				int x = right - i * 8 - 9;
				int col = s >= 2 ? 0xFFFFC22E : 0xB0FFC22E;
				int w = s >= 2 ? 9 : 5;
				g.fill(x, y, x + w, y + 1, col);
				g.fill(x, y + 8, x + w, y + 9, col);
				g.fill(x, y + 1, x + 1, y + 8, col);
				if (s >= 2) g.fill(x + 8, y + 1, x + 9, y + 8, col);
			}
		}

		if (Mods.APPLE_TEXT.on()) {
			String t = String.format("%.1f", sat);
			g.drawString(mc.font, t, right + 3, y + 1, 0xFFFFC22E, true);
		}
	}
}
