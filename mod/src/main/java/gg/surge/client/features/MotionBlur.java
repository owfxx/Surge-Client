package gg.surge.client.features;

import gg.surge.client.mixin.GameRendererAccessor;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Motion blur: blends each frame with the last one (post effects in assets/surge/post_effect).
 * The game clears post effects when the camera changes, so this re-applies it every tick.
 */
public final class MotionBlur {
	private static final int[] LEVELS = {20, 35, 50, 65, 80};
	private static Identifier applied;

	private MotionBlur() {}

	private static Identifier wanted() {
		float s = Mods.BLUR_STRENGTH.get();
		int best = LEVELS[0];
		for (int l : LEVELS) if (Math.abs(l - s) < Math.abs(best - s)) best = l;
		return Identifier.fromNamespaceAndPath("surge", "motion_blur_" + best);
	}

	public static void tick(Minecraft mc) {
		try {
			Identifier current = mc.gameRenderer.currentPostEffect();
			if (Mods.MOTION_BLUR.enabled() && mc.level != null) {
				Identifier want = wanted();
				if (!want.equals(current)) {
					((GameRendererAccessor) mc.gameRenderer).surge$setPostEffect(want);
					applied = want;
				}
			} else if (applied != null) {
				if (applied.equals(current)) mc.gameRenderer.clearPostEffect();
				applied = null;
			}
		} catch (Throwable ignored) {
			// leave the screen alone if post effects aren't available
		}
	}
}
