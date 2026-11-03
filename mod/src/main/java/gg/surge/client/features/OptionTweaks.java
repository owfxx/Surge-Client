package gg.surge.client.features;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;

import java.util.Map;

/**
 * Mods that work by changing a vanilla option (Toggle Sneak, Hurt Cam, No View Bobbing,
 * Static FOV, Clean Overlays). While a mod is on, Surge sets the option; when it's
 * switched off, your own value is put back.
 */
public final class OptionTweaks {
	private OptionTweaks() {}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void tweak(String key, OptionInstance option, boolean on, Object value) {
		Map<String, Object> saved = SurgeConfig.get().saved;
		String k = "tweak#" + key;
		try {
			if (on) {
				Object cur = option.get();
				if (!saved.containsKey(k)) saved.put(k, cur);
				Object v = cur instanceof Double ? ((Number) value).doubleValue() : value;
				if (!v.equals(cur)) option.set(v);
			} else if (saved.containsKey(k)) {
				Object old = saved.remove(k);
				Object cur = option.get();
				Object v = cur instanceof Double ? ((Number) old).doubleValue() : old;
				option.set(v);
			}
		} catch (Exception ignored) {
			// value not valid in this version
		}
	}

	/** Makes the vanilla options match the mods. Safe to call any time. */
	public static void apply(Minecraft mc) {
		var o = mc.options;
		tweak("toggleCrouch", o.toggleCrouch(), Mods.TOGGLE_SNEAK.enabled(), true);
		tweak("damageTilt", o.damageTiltStrength(), Mods.HURT_CAM.enabled(), Mods.HURT_STRENGTH.get() / 100.0);
		tweak("bobView", o.bobView(), Mods.NO_BOBBING.enabled(), false);
		tweak("fovEffect", o.fovEffectScale(), Mods.FOV_EFFECTS.enabled(), Mods.FOV_AMOUNT.get() / 100.0);
		tweak("vignette", o.vignette(), Mods.OVERLAYS.enabled() && Mods.HIDE_VIGNETTE.on(), false);
		o.save();
		SurgeConfig.save();
	}
}
