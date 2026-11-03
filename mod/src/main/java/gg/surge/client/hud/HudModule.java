package gg.surge.client.hud;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.mods.Setting;
import gg.surge.client.mods.SurgeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;

/**
 * A draggable on-screen element (FPS counter, coords, keystrokes…).
 * Every HUD mod gets the same look settings: scale, background, text color, shadow, brackets.
 */
public abstract class HudModule extends SurgeMod {
	public static final int TEXT = 0xFFFFFFFF;

	public final Setting.Slider scaleSetting;
	public final Setting.Toggle background;
	public final Setting.Color backgroundColor;
	public final Setting.Slider backgroundOpacity;
	public final Setting.Color textColor;
	public final Setting.Toggle shadow;
	public final Setting.Choice brackets;

	protected HudModule(String id, String name, Item icon, String description, boolean defEnabled, float defX, float defY) {
		super(id, name, Category.HUD, icon, description, defEnabled);
		this.defX = defX;
		this.defY = defY;
		header("Look");
		scaleSetting = add(new Setting.Slider(this, "scale", "Scale", 1f, 0.5f, 2.5f, 0.05f, "x") {
			@Override public Float get() { float s = state().scale; return s <= 0 ? 1f : s; }

			@Override public void set(Float v) { state().scale = v; SurgeConfig.markDirty(); }

			@Override public void reset() { state().scale = 1f; }
		});
		background = toggle("bg", "Background", true);
		backgroundColor = color("bgColor", "Background color", "000000").showIf(() -> background.on());
		backgroundOpacity = slider("bgOpacity", "Background opacity", 45, 0, 100, 5, "%").showIf(() -> background.on());
		textColor = color("textColor", "Text color", "FFFFFF");
		shadow = toggle("shadow", "Text shadow", true);
		brackets = choice("brackets", "Brackets", "None", "None", "[ ]", "( )", "< >");
		header(name);
		lookCount = settings.size();
	}

	private final int lookCount;

	/** The mod's own settings first, then the shared look settings. */
	@Override
	public java.util.List<Setting<?>> pageSettings() {
		java.util.List<Setting<?>> out = new java.util.ArrayList<>();
		if (settings.size() > lookCount) {
			out.add(settings.get(lookCount - 1));
			out.addAll(settings.subList(lookCount, settings.size()));
		}
		out.addAll(settings.subList(0, lookCount - 1));
		return out;
	}

	/** Size in GUI pixels, used for layout and the HUD editor. */
	public abstract int width(Minecraft mc);

	public abstract int height(Minecraft mc);

	/** Draw at the given top-left corner. */
	public abstract void render(GuiGraphics g, Minecraft mc, int x, int y);

	public float scale() {
		return scaleSetting.get();
	}

	public int scaledWidth(Minecraft mc) { return Math.max(1, Math.round(width(mc) * scale())); }

	public int scaledHeight(Minecraft mc) { return Math.max(1, Math.round(height(mc) * scale())); }

	public int screenX(GuiGraphics g, Minecraft mc) {
		return clamp(Math.round(state().x * g.guiWidth()), 0, g.guiWidth() - scaledWidth(mc));
	}

	public int screenY(GuiGraphics g, Minecraft mc) {
		return clamp(Math.round(state().y * g.guiHeight()), 0, g.guiHeight() - scaledHeight(mc));
	}

	/** Draws the module at its saved position and scale. */
	public void draw(GuiGraphics g, Minecraft mc) {
		int x = screenX(g, mc), y = screenY(g, mc);
		float s = scale();
		if (Math.abs(s - 1f) < 0.001f) { render(g, mc, x, y); return; }
		g.pose().pushMatrix();
		g.pose().translate(x, y);
		g.pose().scale(s, s);
		render(g, mc, 0, 0);
		g.pose().popMatrix();
	}

	protected static int clamp(int v, int min, int max) {
		return Math.max(min, Math.min(max, v));
	}

	// ---------- shared text drawing (the global style in Settings can override every mod) ----------
	private static boolean global() {
		return gg.surge.client.mods.Mods.HUD_OVERRIDE.on();
	}

	public boolean bgOn() {
		return global() ? gg.surge.client.mods.Mods.HUD_BG.on() : background.on();
	}

	public boolean shadowOn() {
		return global() ? gg.surge.client.mods.Mods.HUD_SHADOW.on() : shadow.on();
	}

	private Setting.Color textSetting() {
		return global() ? gg.surge.client.mods.Mods.HUD_TEXT : textColor;
	}

	public int bg() {
		if (global()) return gg.surge.client.mods.Mods.HUD_BG_COLOR.argb(Math.round(gg.surge.client.mods.Mods.HUD_BG_OPACITY.get() * 2.55f));
		return backgroundColor.argb(Math.round(backgroundOpacity.get() * 2.55f));
	}

	public int fg() {
		return textSetting().rgb(0);
	}

	protected String bracket(String s) {
		String b = global() ? gg.surge.client.mods.Mods.HUD_BRACKETS.get() : brackets.get();
		return switch (b) {
			case "[ ]" -> "[" + s + "]";
			case "( )" -> "(" + s + ")";
			case "< >" -> "<" + s + ">";
			default -> s;
		};
	}

	/** Draws text, letter by letter when the color is chroma so the rainbow flows across it. */
	protected void text(GuiGraphics g, Minecraft mc, String s, int x, int y, int color) {
		Setting.Color tc = textSetting();
		if (!tc.chroma() || color != fg()) {
			g.drawString(mc.font, s, x, y, color, shadowOn());
			return;
		}
		int cx = x;
		for (int i = 0; i < s.length(); i++) {
			String ch = String.valueOf(s.charAt(i));
			g.drawString(mc.font, ch, cx, y, tc.rgb(-cx * 0.004f), shadowOn());
			cx += mc.font.width(ch);
		}
	}

	/** Standard boxed one-line label used by most text modules. */
	protected void box(GuiGraphics g, Minecraft mc, int x, int y, String text) {
		box(g, mc, x, y, boxWidth(mc, text), text);
	}

	protected void box(GuiGraphics g, Minecraft mc, int x, int y, int w, String text) {
		String s = bracket(text);
		if (bgOn()) g.fill(x, y, x + w, y + 13, bg());
		text(g, mc, s, x + (w - mc.font.width(s)) / 2, y + 3, fg());
	}

	protected int boxWidth(Minecraft mc, String text) {
		return mc.font.width(bracket(text)) + (bgOn() ? 8 : 2);
	}
}
