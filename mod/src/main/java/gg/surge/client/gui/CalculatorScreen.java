package gg.surge.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalculatorScreen extends Screen {
	private static final String[][] KEYS = {
		{"C", "(", ")", "DEL"},
		{"7", "8", "9", "÷"},
		{"4", "5", "6", "×"},
		{"1", "2", "3", "-"},
		{"0", ".", "^", "+"},
		{"√", "%", "π", "="},
	};
	private static String expr = "";
	private static final List<String> HISTORY = new ArrayList<>();
	private static String last = "";

	private final Screen parent;
	private final long openedAt = System.currentTimeMillis();
	private boolean wasDown;
	private int px, py, pw, ph;
	private String flash;
	private long flashAt;

	public CalculatorScreen(Screen parent) {
		super(Component.literal("Calculator"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		pw = 168;
		ph = 236;
		px = (width - pw) / 2;
		py = (height - ph) / 2;
		if (!opened) { opened = true; Ui.openSound("preset:pop"); }
	}

	private boolean opened;

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (parent != null && minecraft.level == null) parent.renderBackground(g, mouseX, mouseY, delta);
		if (Ui.glassBackdrop(g, () -> renderBlurredBackground(g), this.width, this.height)) return;
		g.fill(0, 0, width, height, gg.surge.client.mods.Colors.alpha(0x90000000, Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f)));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		renderBackground(g, mx, my, delta);
		Ui.frame();
		boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasDown;
		wasDown = down;
		boolean glass = Ui.glass();
		if (glass) Ui.glassRect(g, px, py, pw, ph, 8, 1.2f);
		else {
			Ui.shadow(g, px, py, pw, ph);
			Ui.box(g, px, py, pw, ph, Ui.BG, Ui.EDGE);
		}
		g.drawString(font, Component.literal("Calculator").withStyle(ChatFormatting.BOLD), px + 10, py + 9, Ui.TEXT, !glass);
		if (Ui.hover(mx, my, px + pw - 18, py + 6, 12, 12)) {
			g.fill(px + pw - 18, py + 6, px + pw - 6, py + 18, 0x30FFFFFF);
			if (click) { onClose(); return; }
		}
		Ui.centered(g, font, "x", px + pw - 12, py + 8, 0xFFB4B4BA);

		int dx = px + 8, dy = py + 24, dw = pw - 16, dh = 52;
		g.fill(dx, dy, dx + dw, dy + dh, glass ? 0x30000000 : 0xFF18181B);
		int hy = dy + 4;
		for (int i = Math.max(0, HISTORY.size() - 2); i < HISTORY.size(); i++) {
			String hs = Ui.fit(font, HISTORY.get(i), dw - 8);
			g.drawString(font, hs, dx + dw - 4 - font.width(hs), hy, 0xFF71717A, false);
			hy += 10;
		}
		String shown = expr.isEmpty() ? "0" : expr;
		while (font.width(shown) > dw - 8 && shown.length() > 1) shown = shown.substring(1);
		boolean caret = (System.currentTimeMillis() / 500) % 2 == 0;
		int ex = dx + dw - 4 - font.width(shown);
		g.drawString(font, shown, ex, dy + 25, Ui.TEXT, false);
		if (caret) g.fill(dx + dw - 3, dy + 24, dx + dw - 2, dy + 34, 0xC0FFFFFF);
		String preview = preview();
		if (flash != null && System.currentTimeMillis() - flashAt < 1500) preview = flash;
		if (!preview.isEmpty()) g.drawString(font, preview, dx + dw - 4 - font.width(preview), dy + 39, 0xFFA1A1AA, false);

		int gx = px + 8, gy = dy + dh + 6, gap = 4;
		int bw = (pw - 16 - gap * 3) / 4, bh = (py + ph - 8 - gy - gap * 5) / 6;
		for (int r = 0; r < KEYS.length; r++) {
			for (int c = 0; c < 4; c++) {
				String k = KEYS[r][c];
				int x = gx + c * (bw + gap), y = gy + r * (bh + gap);
				boolean hov = Ui.hover(mx, my, x, y, bw, bh);
				Ui.hoverTick("calc:" + k, hov);
				boolean op = c == 3 || r == 0;
				boolean eq = k.equals("=");
				int fill = eq ? Ui.ACCENT : op ? 0xFF2E2E33 : 0xFF3A3A3D;
				if (hov) fill = eq ? Ui.ACCENT_HOVER : op ? 0xFF3A3A40 : 0xFF48484C;
				if (glass) Ui.glassRect(g, x, y, bw, bh, 5, hov ? 1.4f : eq ? 1.25f : 0.9f);
				else Ui.box(g, x, y, bw, bh, fill, Ui.EDGE);
				Ui.centered(g, font, k, x + bw / 2, y + (bh - 8) / 2, eq && !glass ? 0xFF101012 : Ui.TEXT);
				if (hov && click) { press(k); Ui.click(); }
			}
		}
		super.render(g, mx, my, delta);
	}

	private void press(String k) {
		switch (k) {
			case "C" -> expr = "";
			case "DEL" -> { if (!expr.isEmpty()) expr = expr.substring(0, expr.length() - 1); }
			case "=" -> equals();
			case "÷" -> expr += "/";
			case "×" -> expr += "*";
			case "√" -> expr += "sqrt(";
			case "π" -> expr += "pi";
			default -> expr += k;
		}
		if (expr.length() > 200) expr = expr.substring(0, 200);
	}

	private void equals() {
		if (expr.isBlank()) return;
		try {
			double v = new Parser(expr).parse();
			String out = format(v);
			HISTORY.add(pretty(expr) + " = " + out);
			if (HISTORY.size() > 20) HISTORY.remove(0);
			last = out;
			expr = out;
		} catch (RuntimeException e) {
			flash = "Can't calculate that";
			flashAt = System.currentTimeMillis();
		}
	}

	private String preview() {
		if (expr.isBlank()) return "";
		try {
			String v = format(new Parser(expr).parse());
			return v.equals(expr) ? "" : "= " + v;
		} catch (RuntimeException e) {
			return "";
		}
	}

	private static String pretty(String s) {
		return s.replace("*", "×").replace("/", "÷");
	}

	static String format(double v) {
		if (Double.isNaN(v) || Double.isInfinite(v)) throw new ArithmeticException();
		if (v == Math.rint(v) && Math.abs(v) < 1e15) return Long.toString((long) v);
		String s = String.format(Locale.ROOT, "%.10f", v);
		s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
		if (Math.abs(v) >= 1e15 || (Math.abs(v) < 1e-6 && v != 0)) s = String.format(Locale.ROOT, "%.6e", v);
		return s;
	}

	@Override
	public boolean keyPressed(KeyEvent e) {
		int key = e.key();
		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { equals(); return true; }
		if (key == GLFW.GLFW_KEY_BACKSPACE) { press("DEL"); return true; }
		if (key == GLFW.GLFW_KEY_DELETE) { expr = ""; return true; }
		if (key == GLFW.GLFW_KEY_C && (e.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
			minecraft.keyboardHandler.setClipboard(last.isEmpty() ? expr : last);
			return true;
		}
		if (key == GLFW.GLFW_KEY_V && (e.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
			String clip = minecraft.keyboardHandler.getClipboard().replaceAll("[^0-9a-zA-Z+\\-*/^%().,]", "");
			expr += clip.replace(',', '.');
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public boolean charTyped(CharacterEvent e) {
		String s = e.codepointAsString();
		if (s.equals("=")) { equals(); return true; }
		if (s.equals(",")) s = ".";
		if (s.equals("x")) s = "*";
		if (s.matches("[0-9a-wyz+\\-*/^%().!]")) {
			expr += s;
			return true;
		}
		return false;
	}

	@Override
	public void onClose() {
		Ui.closeSound();
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	static final class Parser {
		private final String s;
		private int i;

		Parser(String s) { this.s = s.replace(" ", "").toLowerCase(Locale.ROOT); }

		double parse() {
			double v = sum();
			if (i < s.length()) throw new IllegalArgumentException();
			return v;
		}

		private boolean eat(char c) {
			if (i < s.length() && s.charAt(i) == c) { i++; return true; }
			return false;
		}

		private double sum() {
			double v = product();
			while (true) {
				if (eat('+')) v += product();
				else if (eat('-')) v -= product();
				else return v;
			}
		}

		private double product() {
			double v = unary();
			while (true) {
				if (eat('*')) v *= unary();
				else if (eat('/')) v /= unary();
				else if (i < s.length() && (s.charAt(i) == '(' || Character.isLetter(s.charAt(i)))) v *= unary();
				else return v;
			}
		}

		private double unary() {
			if (eat('-')) return -unary();
			if (eat('+')) return unary();
			return power();
		}

		private double power() {
			double v = postfix();
			if (eat('^')) v = Math.pow(v, unary());
			return v;
		}

		private double postfix() {
			double v = atom();
			while (true) {
				if (eat('%')) v /= 100;
				else if (eat('!')) {
					if (v < 0 || v > 170 || v != Math.rint(v)) throw new ArithmeticException();
					double f = 1;
					for (int k = 2; k <= v; k++) f *= k;
					v = f;
				} else return v;
			}
		}

		private double atom() {
			if (eat('(')) {
				double v = sum();
				eat(')');
				return v;
			}
			int start = i;
			if (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) {
				while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
				if (i < s.length() && s.charAt(i) == 'e' && i + 1 < s.length() && (Character.isDigit(s.charAt(i + 1)) || s.charAt(i + 1) == '-')) {
					i += 2;
					while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
				}
				return Double.parseDouble(s.substring(start, i));
			}
			while (i < s.length() && Character.isLetter(s.charAt(i))) i++;
			String name = s.substring(start, i);
			switch (name) {
				case "pi" -> { return Math.PI; }
				case "e" -> { return Math.E; }
				default -> { }
			}
			double a = atom();
			return switch (name) {
				case "sqrt" -> Math.sqrt(a);
				case "sin" -> Math.sin(Math.toRadians(a));
				case "cos" -> Math.cos(Math.toRadians(a));
				case "tan" -> Math.tan(Math.toRadians(a));
				case "log" -> Math.log10(a);
				case "ln" -> Math.log(a);
				case "abs" -> Math.abs(a);
				case "round" -> Math.rint(a);
				default -> throw new IllegalArgumentException(name);
			};
		}
	}
}
