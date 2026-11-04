package gg.surge.client.gui;

import gg.surge.client.mods.Colors;
import gg.surge.client.net.Social;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public final class SocialToasts {
	private SocialToasts() {}

	public static void render(GuiGraphics g, Minecraft mc, int screenW) {
		if (mc.screen instanceof SocialScreen) return;
		List<Social.Toast> list;
		synchronized (Social.TOASTS) { list = new ArrayList<>(Social.TOASTS); }
		if (list.isEmpty()) return;
		long now = System.currentTimeMillis();
		g.nextStratum();
		int y = 6;
		for (Social.Toast t : list) {
			long age = now - t.at();
			if (age > 6000) continue;
			float in = Math.min(1f, age / 220f), out = Math.min(1f, (6000 - age) / 300f);
			float k = Math.min(in, out);
			k = 1f - (1f - k) * (1f - k) * (1f - k);
			String title = Ui.fit(mc.font, t.title(), 150), text = Ui.fit(mc.font, t.text(), 150);
			int w = Math.max(mc.font.width(title), mc.font.width(text)) + 30;
			int x = screenW - 6 - Math.round(w * k);
			Ui.box(g, x, y, w, 28, Colors.alpha(0xF2202024, k), Colors.alpha(Ui.ACCENT, k));
			Ui.logo(g, x + 5, y + 6, 16);
			g.drawString(mc.font, title, x + 25, y + 5, Colors.alpha(0xFFFFFFFF, k), false);
			g.drawString(mc.font, text, x + 25, y + 16, Colors.alpha(0xFFA9AAAE, k), false);
			g.fill(x + 1, y + 26, x + 1 + Math.round((w - 2) * (1f - age / 6000f)), y + 27, Colors.alpha(Ui.ACCENT_2, k));
			y += Math.round(32 * k);
		}
	}
}
