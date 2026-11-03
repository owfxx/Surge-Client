package gg.surge.client.gui;

import gg.surge.client.cosmetics.Preview;
import gg.surge.client.emotes.Emotes;
import gg.surge.client.mods.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Emote wheel (B by default): a ring of eight slots around your character. Point at a slot
 * (the direction from the center is enough) and your character previews it; click or press
 * 1-8 to play it. Scroll for more pages.
 */
public class EmoteWheelScreen extends Screen {
	private static final int PER_PAGE = 8;
	private static int page;
	private boolean wasDown = true;
	private long openedAt;
	private int hoveredSlot = -1;

	public EmoteWheelScreen() {
		super(Component.literal("Emotes"));
	}

	@Override
	protected void init() {
		openedAt = System.currentTimeMillis();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, Colors.alpha(0x80000000, openProgress()));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private float openProgress() {
		float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / 170f);
		return 1f - (1f - t) * (1f - t) * (1f - t);
	}

	private List<Emotes.Emote> owned() {
		List<Emotes.Emote> out = new ArrayList<>();
		for (Emotes.Emote e : Emotes.Emote.values()) if (Emotes.owned(e)) out.add(e);
		return out;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int pages = Math.max(1, (owned().size() + PER_PAGE - 1) / PER_PAGE);
		if (scrollY != 0 && pages > 1) { page = (page + (scrollY < 0 ? 1 : -1) + pages) % pages; Ui.click(); return true; }
		return false;
	}

	/**
	 * Draws the ring row by row: each pixel belongs to a slot by its angle; neighbouring pixels of the
	 * same slot are merged into one fill, so it's cheap and has no overlapping transparency.
	 * grow[i] pushes slot i outward (hover), colors[i] is its color, gaps keep 1.5px between slots.
	 */
	private static void ring(GuiGraphics g, int cx, int cy, int r0, int r1, double start, int n, int[] colors, int[] grow) {
		int maxR = r1 + 8;
		double seg = Math.PI * 2 / n;
		for (int y = -maxR; y <= maxR; y++) {
			int runStart = 0, runSlot = -2;
			for (int x = -maxR; x <= maxR + 1; x++) {
				int slot = -1;
				if (x <= maxR) {
					double d = Math.sqrt(x * x + y * y);
					if (d >= r0) {
						double ang = Math.atan2(y, x) - start;
						ang = (ang % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
						int sIdx = (int) (ang / seg);
						double fromEdge = Math.min(ang - sIdx * seg, (sIdx + 1) * seg - ang) * d;
						if (d < r1 + grow[sIdx] && fromEdge > 1.5) slot = sIdx;
					}
				}
				if (slot != runSlot) {
					if (runSlot >= 0) g.fill(cx + runStart, cy + y, cx + x, cy + y + 1, colors[runSlot]);
					runSlot = slot;
					runStart = x;
				}
			}
		}
	}

	private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int y = -r; y <= r; y++) {
			int w = (int) Math.floor(Math.sqrt(r * r - y * y));
			g.fill(cx - w, cy + y, cx + w + 1, cy + y + 1, color);
		}
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		Ui.frame();
		boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasDown;
		wasDown = down;
		Runnable action = null;

		List<Emotes.Emote> all = owned();
		int cx = this.width / 2, cy = this.height / 2;
		float k = openProgress();

		if (all.isEmpty()) {
			Ui.box(g, cx - 110, cy - 30, 220, 60, Ui.BG, Ui.EDGE);
			Ui.centeredShadow(g, font, "You don't have any emotes yet", cx, cy - 18, Ui.TEXT);
			if (Ui.pill(g, font, mx, my, cx - 50, cy + 2, 100, 18, "Get emotes", true) && click) {
				action = () -> minecraft.setScreen(WardrobeScreen.open(null, WardrobeScreen.Section.EMOTES));
			}
			if (action != null) { Ui.click(); action.run(); }
			return;
		}
		int pages = Math.max(1, (all.size() + PER_PAGE - 1) / PER_PAGE);
		if (page >= pages) page = 0;
		List<Emotes.Emote> slots = all.subList(page * PER_PAGE, Math.min(all.size(), (page + 1) * PER_PAGE));

		int R = Math.round(Math.min(110, Math.min(this.width, this.height) / 2 - 24) * (0.9f + 0.1f * k));
		int r = Math.round(R * 0.55f);
		double seg = Math.PI * 2 / PER_PAGE;
		double start = -Math.PI / 2 - seg / 2;

		// which slot the mouse points at (direction from the center, outside the middle)
		double dx = mx - cx, dy = my - cy;
		int hov = -1;
		if (dx * dx + dy * dy > (r * 0.5) * (r * 0.5)) {
			double ang = Math.atan2(dy, dx) - start;
			ang = (ang % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
			hov = (int) (ang / seg);
			if (hov >= slots.size()) hov = -1;
		}
		if (hov != hoveredSlot) { hoveredSlot = hov; if (hov >= 0) Ui.hoverTick("wheel:" + hov + ":" + page, true); }

		int[] colors = new int[PER_PAGE], grow = new int[PER_PAGE];
		float[] has = new float[PER_PAGE];
		for (int i = 0; i < PER_PAGE; i++) {
			float ha = Ui.anim("wheelseg:" + i, i == hov);
			has[i] = ha;
			colors[i] = Colors.alpha(i < slots.size() ? Colors.lerp(0xC8181820, 0xF04A2E8A, ha) : 0x70101014, k);
			grow[i] = Math.round(ha * 6);
		}
		ring(g, cx, cy, r, R, start, PER_PAGE, colors, grow);
		for (int i = 0; i < slots.size(); i++) {
			float ha = has[i];
			double mid = start + (i + 0.5) * seg;
			int lr = (r + R) / 2 + Math.round(ha * 3);
			int lx = cx + (int) Math.round(Math.cos(mid) * lr), ly = cy + (int) Math.round(Math.sin(mid) * lr);
			String label = Ui.fit(font, slots.get(i).label, (R - r) + 24);
			g.drawString(font, label, lx - font.width(label) / 2, ly - 4, Colors.alpha(Colors.lerp(0xFFD8D8DC, 0xFFFFFFFF, ha), k), true);
			g.drawString(font, String.valueOf(i + 1), lx - 2, ly + 6, Colors.alpha(0x60FFFFFF, k), false);
		}

		Emotes.Emote hovered = hov >= 0 ? slots.get(hov) : null;
		if (hovered != null && click) action = () -> { onClose(); Emotes.play(minecraft, hovered); };
		for (int i = 0; i < slots.size(); i++) {
			if (GLFW.glfwGetKey(minecraft.getWindow().handle(), GLFW.GLFW_KEY_1 + i) == GLFW.GLFW_PRESS) {
				Emotes.Emote play = slots.get(i);
				action = () -> { onClose(); Emotes.play(minecraft, play); };
			}
		}

		// center: your character previewing the hovered emote
		Emotes.setPreview(hovered);
		disc(g, cx, cy, r - 4, Colors.alpha(0xD0141418, k));
		if (minecraft.player != null) Preview.draw(g, cx - r + 10, cy - r + 6, cx + r - 10, cy + r - 16, Math.round(r * 0.55f));
		String title = hovered != null ? hovered.label : "Pick an emote";
		Ui.centeredShadow(g, font, title, cx, cy + r - 14, hovered != null ? Ui.TEXT : Ui.MUTED);

		if (pages > 1) {
			int dotsW = pages * 8;
			for (int p = 0; p < pages; p++) g.fill(cx - dotsW / 2 + p * 8, cy + R + 14, cx - dotsW / 2 + p * 8 + 5, cy + R + 16, p == page ? Ui.ACCENT_2 : 0x60FFFFFF);
			Ui.centered(g, font, "Scroll for more", cx, cy + R + 20, 0x80FFFFFF);
		}

		if (action != null) { Ui.click(); action.run(); }
	}

	@Override
	public void removed() {
		Emotes.setPreview(null);
		super.removed();
	}
}
