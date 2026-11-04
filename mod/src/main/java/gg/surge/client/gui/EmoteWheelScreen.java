package gg.surge.client.gui;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.cosmetics.Preview;
import gg.surge.client.emotes.Emotes;
import gg.surge.client.mods.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class EmoteWheelScreen extends Screen {
	private static final int PER_PAGE = 8, MAX_PAGES = 9;
	private static int page;
	private boolean wasDown = true, wasRight = true;
	private final boolean[] keyWas = new boolean[11];
	private long openedAt;
	private int hoveredSlot = -1;
	private boolean editing;
	private int editSlot = -1;
	private float listScroll, listScrollTarget;
	private int listMax;
	private long pageChangedAt;

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

	private static List<Emotes.Emote> owned() {
		List<Emotes.Emote> out = new ArrayList<>();
		for (Emotes.Emote e : Emotes.Emote.values()) if (Emotes.owned(e)) out.add(e);
		return out;
	}

	private static List<List<String>> layout() {
		SurgeConfig c = SurgeConfig.get();
		if (c.emotePages != null && !c.emotePages.isEmpty()) {
			for (List<String> p : c.emotePages) while (p.size() < PER_PAGE) p.add("");
			return c.emotePages;
		}
		List<List<String>> auto = new ArrayList<>();
		List<Emotes.Emote> all = owned();
		for (int i = 0; i < Math.max(1, all.size()); i += PER_PAGE) {
			List<String> p = new ArrayList<>();
			for (int j = 0; j < PER_PAGE; j++) p.add(i + j < all.size() ? all.get(i + j).name() : "");
			auto.add(p);
			if (auto.size() == MAX_PAGES) break;
		}
		return auto;
	}

	private static List<List<String>> editable() {
		SurgeConfig c = SurgeConfig.get();
		if (c.emotePages == null || c.emotePages.isEmpty()) {
			List<List<String>> copy = new ArrayList<>();
			for (List<String> p : layout()) copy.add(new ArrayList<>(p));
			c.emotePages = copy;
		}
		return c.emotePages;
	}

	private static Emotes.Emote at(List<String> p, int i) {
		if (i >= p.size()) return null;
		Emotes.Emote e = Emotes.Emote.byName(p.get(i));
		return e != null && Emotes.owned(e) ? e : null;
	}

	private void setPage(int p) {
		if (p == page) return;
		page = p;
		pageChangedAt = System.currentTimeMillis();
		editSlot = -1;
		Ui.click();
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (editing && mouseX > this.width - 150) {
			listScrollTarget = Math.max(0, Math.min(listMax, listScrollTarget - (float) scrollY * 20f));
			return true;
		}
		int pages = layout().size();
		if (scrollY != 0 && pages > 1) { setPage((page + (scrollY < 0 ? 1 : -1) + pages) % pages); return true; }
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return true;
	}

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

	private boolean key(int idx, int glfwKey) {
		boolean d = GLFW.glfwGetKey(minecraft.getWindow().handle(), glfwKey) == GLFW.GLFW_PRESS;
		boolean pressed = d && !keyWas[idx];
		keyWas[idx] = d;
		return pressed;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		Ui.frame();
		long win = minecraft.getWindow().handle();
		boolean down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean right = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasDown, rclick = right && !wasRight;
		wasDown = down;
		wasRight = right;
		Runnable action = null;

		List<Emotes.Emote> all = owned();
		int cx = this.width / 2, cy = this.height / 2;
		if (editing) cx = Math.min(cx, (this.width - 150) / 2 + 4);
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
		List<List<String>> pagesList = layout();
		int pages = pagesList.size();
		if (page >= pages) page = 0;
		List<String> slots = pagesList.get(page);

		int R = Math.round(Math.min(110, Math.min(this.width - (editing ? 150 : 0), this.height) / 2 - 30) * (0.9f + 0.1f * k));
		int r = Math.round(R * 0.55f);
		double seg = Math.PI * 2 / PER_PAGE;
		double start = -Math.PI / 2 - seg / 2;
		float flip = Math.min(1f, (System.currentTimeMillis() - pageChangedAt) / 160f);
		start += (1f - flip) * (1f - flip) * 0.35;

		double dx = mx - cx, dy = my - cy;
		int hov = -1;
		double dist2 = dx * dx + dy * dy;
		if (dist2 > (r * 0.5) * (r * 0.5) && (!editing || dist2 < (R + 14) * (R + 14))) {
			double ang = Math.atan2(dy, dx) - start;
			ang = (ang % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2);
			hov = (int) (ang / seg);
			if (!editing && at(slots, hov) == null) hov = -1;
		}
		if (hov != hoveredSlot) { hoveredSlot = hov; if (hov >= 0) Ui.hoverTick("wheel:" + hov + ":" + page, true); }

		int[] colors = new int[PER_PAGE], grow = new int[PER_PAGE];
		float[] has = new float[PER_PAGE];
		for (int i = 0; i < PER_PAGE; i++) {
			float ha = Ui.anim("wheelseg:" + i, i == hov || (editing && i == editSlot));
			has[i] = ha;
			boolean filled = at(slots, i) != null;
			int base = filled ? 0xC8181820 : 0x70101014;
			int hi = editing && i == editSlot ? Colors.alpha(Ui.ACCENT, 0.94f) : filled ? Colors.alpha(Ui.ACCENT_DIM, 0.94f) : 0xA0303040;
			colors[i] = Colors.alpha(Colors.lerp(base, hi, ha), k * (0.6f + 0.4f * flip));
			grow[i] = Math.round(ha * 6);
		}
		ring(g, cx, cy, r, R, start, PER_PAGE, colors, grow);
		for (int i = 0; i < PER_PAGE; i++) {
			Emotes.Emote e = at(slots, i);
			float ha = has[i];
			double mid = start + (i + 0.5) * seg;
			int lr = (r + R) / 2 + Math.round(ha * 3);
			int lx = cx + (int) Math.round(Math.cos(mid) * lr), ly = cy + (int) Math.round(Math.sin(mid) * lr);
			if (e == null) {
				if (editing) Ui.centered(g, font, "+", lx, ly - 4, Colors.alpha(0x80FFFFFF, k));
				continue;
			}
			String label = Ui.fit(font, e.label, (R - r) + 24);
			g.drawString(font, label, lx - font.width(label) / 2, ly - 4, Colors.alpha(Colors.lerp(0xFFD8D8DC, 0xFFFFFFFF, ha), k * flip), true);
			g.drawString(font, String.valueOf(i + 1), lx - 2, ly + 6, Colors.alpha(0x60FFFFFF, k * flip), false);
		}

		Emotes.Emote hovered = hov >= 0 ? at(slots, hov) : null;
		if (!editing) {
			if (hovered != null && click) action = () -> { onClose(); Emotes.play(minecraft, hovered); };
			for (int i = 0; i < PER_PAGE; i++) {
				if (key(i, GLFW.GLFW_KEY_1 + i)) {
					Emotes.Emote play = at(slots, i);
					if (play != null) action = () -> { onClose(); Emotes.play(minecraft, play); };
				}
			}
			if (key(8, GLFW.GLFW_KEY_0) && pages > 1) setPage((page + 1) % pages);
		} else {
			if (hov >= 0 && click) { editSlot = hov == editSlot ? -1 : hov; Ui.click(); }
			if (hov >= 0 && rclick) {
				int slot = hov;
				action = () -> { editable().get(page).set(slot, ""); SurgeConfig.save(); };
			}
		}

		Emotes.Emote show = hovered;
		Emotes.setPreview(show);
		disc(g, cx, cy, r - 4, Colors.alpha(0xD0141418, k));
		if (minecraft.player != null) Preview.draw(g, cx - r + 10, cy - r + 6, cx + r - 10, cy + r - 16, Math.round(r * 0.55f));
		String title = editing ? (editSlot >= 0 ? "Pick an emote for slot " + (editSlot + 1) : "Click a slot to fill it") : hovered != null ? hovered.label : "Pick an emote";
		Ui.centeredShadow(g, font, Ui.fit(font, title, r * 2 - 10), cx, cy + r - 14, hovered != null || editing ? Ui.TEXT : Ui.MUTED);

		int pw = 14, totalW = pages * (pw + 3) + (editing && pages < MAX_PAGES ? pw + 3 : 0);
		int px0 = cx - totalW / 2, py0 = cy + R + 12;
		for (int p = 0; p < pages; p++) {
			int x = px0 + p * (pw + 3);
			boolean ph = Ui.hover(mx, my, x, py0, pw, 14);
			Ui.box(g, x, py0, pw, 14, p == page ? Ui.ACCENT : ph ? 0xE0303036 : 0xB0181820, p == page ? Ui.ACCENT_2 : Ui.EDGE);
			Ui.centered(g, font, String.valueOf(p + 1), x + pw / 2 + 1, py0 + 3, 0xFFFFFFFF);
			if (ph && click) { int to = p; action = () -> setPage(to); }
		}
		if (editing && pages < MAX_PAGES) {
			int x = px0 + pages * (pw + 3);
			boolean ph = Ui.hover(mx, my, x, py0, pw, 14);
			Ui.box(g, x, py0, pw, 14, ph ? 0xE0303036 : 0xB0181820, Ui.EDGE);
			Ui.centered(g, font, "+", x + pw / 2 + 1, py0 + 3, Ui.ACCENT_2);
			if (ph && click) action = () -> {
				List<String> np = new ArrayList<>();
				for (int i = 0; i < PER_PAGE; i++) np.add("");
				List<List<String>> l = editable();
				l.add(np);
				SurgeConfig.save();
				setPage(l.size() - 1);
			};
		}
		String hint = editing ? "Right click a slot to empty it" : pages > 1 ? "Scroll or press 0 for the next page" : "";
		if (!hint.isEmpty()) Ui.centered(g, font, hint, cx, py0 + 19, 0x80FFFFFF);

		int by = Math.max(6, cy - R - 30);
		if (Ui.pill(g, font, mx, my, cx - 40, by, 80, 16, editing ? "Done" : "Edit wheel", editing) && click) action = () -> { editing = !editing; editSlot = -1; };
		if (editing) {
			if (Ui.pill(g, font, mx, my, cx + 46, by, 80, 16, "Clear page", false) && click) action = () -> { List<String> p = editable().get(page); for (int i = 0; i < p.size(); i++) p.set(i, ""); SurgeConfig.save(); };
			if (pages > 1 && Ui.pill(g, font, mx, my, cx - 126, by, 80, 16, "Delete page", false) && click) action = () -> { editable().remove(page); SurgeConfig.save(); page = Math.max(0, page - 1); };
			action = emoteList(g, mx, my, click, all, action);
		}

		if (action != null) { Ui.click(); action.run(); }
	}

	private Runnable emoteList(GuiGraphics g, int mx, int my, boolean click, List<Emotes.Emote> all, Runnable action) {
		int x = this.width - 146, w = 140, top = 24, bottom = this.height - 10;
		Ui.box(g, x, top - 18, w, bottom - top + 18, 0xE0141418, Ui.EDGE);
		g.drawString(font, "Your emotes", x + 8, top - 12, Ui.ACCENT_2, false);
		listMax = Math.max(0, all.size() * 18 - (bottom - top));
		listScrollTarget = Math.min(listScrollTarget, listMax);
		listScroll += (listScrollTarget - listScroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		List<String> slots = layout().get(page);
		g.enableScissor(x, top, x + w, bottom);
		int y = top - Math.round(listScroll);
		for (Emotes.Emote e : all) {
			boolean onPage = slots.contains(e.name());
			boolean h = Ui.hover(mx, my, x + 3, y, w - 6, 17) && my >= top && my < bottom;
			if (h) { g.fill(x + 3, y, x + w - 3, y + 17, 0xFF2E2E33); Emotes.setPreview(e); }
			g.drawString(font, e.label, x + 9, y + 5, onPage ? Ui.FAINT : Ui.TEXT, false);
			if (onPage) g.drawString(font, "✔", x + w - 14, y + 5, Ui.ACCENT_2, false);
			if (h && click) {
				int slot = editSlot >= 0 ? editSlot : slots.indexOf("");
				if (slot < 0) action = () -> {};
				else action = () -> {
					List<String> p = editable().get(page);
					p.set(slot, e.name());
					SurgeConfig.save();
					editSlot = p.indexOf("");
				};
			}
			y += 18;
		}
		g.disableScissor();
		return action;
	}

	@Override
	public void removed() {
		Emotes.setPreview(null);
		super.removed();
	}
}
