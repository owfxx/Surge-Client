package gg.surge.legacy.hud;

import gg.surge.legacy.gui.Draw;
import gg.surge.legacy.mod.Setting;
import net.minecraft.item.ItemStack;

public abstract class TextHud extends HudMod {
	public final Setting.Choice labelPos;
	private final String label, sample;

	protected TextHud(String id, String name, ItemStack icon, String description, boolean def, float x, float y, String label, String labelDef, String sample) {
		super(id, name, icon, description, def, x, y);
		this.label = label;
		this.sample = sample;
		labelPos = label.isEmpty() ? null : choice("label", "Label", labelDef, "After", "Before", "Hidden");
	}

	protected abstract String value();

	protected String text() {
		String v;
		try { v = mc().player == null ? sample : value(); } catch (Exception e) { v = sample; }
		if (v.isEmpty() && Hud.editing()) v = sample;
		if (labelPos == null || labelPos.is("Hidden")) return v;
		return labelPos.is("Before") ? label + ": " + v : v + " " + label;
	}

	@Override public int width() { return Math.max(46, Draw.width(bracket(text())) + 10); }

	@Override public int height() { return 14; }

	@Override public void render(int x, int y) {
		String s = bracket(text());
		int w = width();
		if (background.on()) panel(x, y, w, 14);
		text(s, x + (w - Draw.width(s)) / 2, y + 3, fg());
	}
}
