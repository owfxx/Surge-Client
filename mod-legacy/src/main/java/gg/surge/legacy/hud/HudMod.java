package gg.surge.legacy.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.surge.legacy.Config;
import gg.surge.legacy.gui.Draw;
import gg.surge.legacy.mod.Category;
import gg.surge.legacy.mod.Mod;
import gg.surge.legacy.mod.Setting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

public abstract class HudMod extends Mod {
	public final float defX, defY;
	public final Setting.Toggle background, shadow;
	public final Setting.Color bgColor, textColor;
	public final Setting.Slider bgOpacity;
	public final Setting.Choice brackets;

	protected HudMod(String id, String name, ItemStack icon, String description, boolean defEnabled, float defX, float defY) {
		super(id, name, Category.HUD, icon, description, defEnabled);
		this.defX = defX;
		this.defY = defY;
		background = toggle("bg", "Background", true);
		bgColor = color("bgColor", "Background color", "000000").showIf(() -> background.on());
		bgOpacity = slider("bgOpacity", "Background opacity", 45, 0, 100, 5, "%").showIf(() -> background.on());
		textColor = color("textColor", "Text color", "FFFFFF");
		shadow = toggle("shadow", "Text shadow", true);
		brackets = choice("brackets", "Brackets", "None", "None", "[ ]", "( )", "< >");
	}

	public abstract int width();

	public abstract int height();

	public abstract void render(int x, int y);

	public float scale() {
		float s = Config.mod(id).scale;
		return s <= 0 ? 1f : s;
	}

	public void setScale(float s) {
		Config.mod(id).scale = Math.max(0.5f, Math.min(2.5f, s));
		Config.markDirty();
	}

	public float fx() { float v = Config.mod(id).x; return v < 0 ? defX : v; }

	public float fy() { float v = Config.mod(id).y; return v < 0 ? defY : v; }

	public void setPos(float x, float y) {
		Config.mod(id).x = Math.max(0, Math.min(1, x));
		Config.mod(id).y = Math.max(0, Math.min(1, y));
		Config.markDirty();
	}

	public int sw() { return Math.max(1, Math.round(width() * scale())); }

	public int sh() { return Math.max(1, Math.round(height() * scale())); }

	public int screenX(int gw) { return Math.max(0, Math.min(gw - sw(), Math.round(fx() * gw))); }

	public int screenY(int gh) { return Math.max(0, Math.min(gh - sh(), Math.round(fy() * gh))); }

	public void draw(int gw, int gh) {
		int x = screenX(gw), y = screenY(gh);
		float s = scale();
		GlStateManager.pushMatrix();
		GlStateManager.translate((float) x, (float) y, 0f);
		if (Math.abs(s - 1f) > 0.001f) GlStateManager.scale(s, s, 1f);
		render(0, 0);
		GlStateManager.popMatrix();
		GlStateManager.color(1f, 1f, 1f, 1f);
	}

	public int bg() { return bgColor.argb(Math.round(bgOpacity.get() * 2.55f)); }

	public int fg() { return textColor.rgb(0); }

	protected String bracket(String s) {
		String b = brackets.get();
		if (b.equals("[ ]")) return "[" + s + "]";
		if (b.equals("( )")) return "(" + s + ")";
		if (b.equals("< >")) return "<" + s + ">";
		return s;
	}

	protected void text(String s, int x, int y, int color) {
		if (!textColor.chroma() || color != fg()) {
			Draw.text(s, x, y, color, shadow.on());
			return;
		}
		int cx = x;
		for (int i = 0; i < s.length(); i++) {
			String ch = String.valueOf(s.charAt(i));
			Draw.text(ch, cx, y, textColor.rgb(-cx * 0.004f), shadow.on());
			cx += Draw.width(ch);
		}
	}

	protected static MinecraftClient mc() {
		return MinecraftClient.getInstance();
	}
}
