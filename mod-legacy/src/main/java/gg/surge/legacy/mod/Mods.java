package gg.surge.legacy.mod;

import gg.surge.legacy.cosmetic.Cosmetics;
import gg.surge.legacy.feature.Features;
import gg.surge.legacy.hud.Hud;
import gg.surge.legacy.perf.Perf;

import java.util.ArrayList;
import java.util.List;

public final class Mods {
	private static List<Mod> all;

	private Mods() {}

	public static List<Mod> all() {
		if (all == null) {
			List<Mod> l = new ArrayList<Mod>();
			l.addAll(Hud.ALL);
			l.addAll(Features.ALL);
			l.addAll(Perf.ALL);
			l.add(Cosmetics.MOD);
			l.add(gg.surge.legacy.gui.Theme.MENU);
			all = l;
		}
		return all;
	}
}
