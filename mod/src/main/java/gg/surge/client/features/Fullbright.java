package gg.surge.client.features;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.mixin.OptionInstanceAccessor;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;

/** Fullbright: see in the dark. Turning it off puts your brightness back. */
public final class Fullbright {
	private static final double BRIGHT = 12.0;

	private Fullbright() {}

	/** Options get saved and reloaded (and other mods touch brightness), so keep it applied. */
	public static void tick(Minecraft mc) {
		if (!Mods.FULLBRIGHT.enabled()) return;
		var gamma = mc.options.gamma();
		if (gamma.get() < BRIGHT - 0.01) ((OptionInstanceAccessor) (Object) gamma).surge$setRawValue(BRIGHT);
	}

	public static void apply(Minecraft mc) {
		SurgeConfig c = SurgeConfig.get();
		var gamma = mc.options.gamma();
		if (Mods.FULLBRIGHT.enabled()) {
			if (!c.saved.containsKey("fullbright#gamma")) c.saved.put("fullbright#gamma", Math.min(1.0, gamma.get()));
			((OptionInstanceAccessor) (Object) gamma).surge$setRawValue(BRIGHT);
		} else if (c.saved.containsKey("fullbright#gamma")) {
			gamma.set(((Number) c.saved.remove("fullbright#gamma")).doubleValue());
		}
		SurgeConfig.save();
	}
}
