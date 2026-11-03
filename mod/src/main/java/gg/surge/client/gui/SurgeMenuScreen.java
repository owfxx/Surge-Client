package gg.surge.client.gui;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.features.CommandKeys;
import gg.surge.client.features.Waypoints;
import gg.surge.client.mods.Colors;
import gg.surge.client.mods.Mods;
import gg.surge.client.mods.Setting;
import gg.surge.client.mods.SurgeMod;
import gg.surge.client.perf.PerfMods;
import gg.surge.client.perf.Performance;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class SurgeMenuScreen extends Screen {
	private enum Tab {
		ALL("All mods", Items.NETHER_STAR), HUD("HUD", Items.ITEM_FRAME), PVP("PvP", Items.NETHERITE_SWORD),
		VISUAL("Visual", Items.AMETHYST_CLUSTER), UTILITY("Utility", Items.IRON_PICKAXE),
		KEYS("Command Keys", Items.COMMAND_BLOCK), FPS("Settings", Items.COMPARATOR);
		final String label;
		final Item icon;
		ItemStack stack;

		Tab(String label, Item icon) {
			this.label = label;
			this.icon = icon;
		}

		ItemStack stack() {
			if (stack == null) stack = new ItemStack(icon);
			return stack;
		}
	}

	private static final int SIDEBAR_W = 104, HEADER_H = 30, CARD_H = 64, GAP = 6;
	private static Tab tab = Tab.ALL;
	private static String query = "";

	private final Screen parent;
	private SurgeMod openMod;
	private EditBox search;
	private final List<Row> rows = new ArrayList<>();
	private final List<EditBox> boxes = new ArrayList<>();
	private boolean needsRebuild;

	private int px, py, pw, ph, cx, cy, cw, ch;
	private boolean wasL = true, wasR = true, escWas = true;
	private boolean clickL, clickR, downL;
	private Runnable action;
	private boolean silent;
	private String tip;
	private long tipSince;
	private Object tipKey;
	private float scroll, scrollTarget;
	private int contentH;
	private SliderModel dragging;
	private java.util.function.BiConsumer<Integer, Integer> drag2d;
	private int dragX, dragW;
	private IntConsumer listening;
	private final Map<Integer, Boolean> keyPrev = new HashMap<>();
	private long openedAt;

	public SurgeMenuScreen() {
		this(null);
	}

	public SurgeMenuScreen(Screen parent) {
		super(Component.literal("Surge Client"));
		this.parent = parent;
	}

	public static SurgeMenuScreen general(Screen parent) {
		tab = Tab.FPS;
		query = "";
		return new SurgeMenuScreen(parent);
	}

	public static SurgeMenuScreen settings(SurgeMod mod, Screen parent) {
		SurgeMenuScreen s = new SurgeMenuScreen(parent);
		s.openMod = mod;
		return s;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return openMod == null && listening == null && !(getFocused() instanceof EditBox);
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		float k = openProgress();
		g.fill(0, 0, this.width, this.height, Colors.alpha(0x90000000, k));
	}

	private float openProgress() {
		float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f);
		return 1f - (1f - t) * (1f - t) * (1f - t);
	}

	@Override
	protected void init() {
		if (openedAt == 0) openedAt = System.currentTimeMillis();
		pw = Math.min(470, this.width - 16);
		ph = Math.min(300, this.height - 16);
		px = (this.width - pw) / 2;
		py = (this.height - ph) / 2;
		cx = px + SIDEBAR_W + 8;
		cy = py + HEADER_H + 6;
		cw = px + pw - 10 - cx;
		ch = py + ph - 8 - cy;
		if (search == null) {
			search = new EditBox(font, 0, 0, 110, 10, Component.literal("Search"));
			search.setBordered(false);
			search.setMaxLength(32);
			search.setHint(Component.literal("Search mods").withStyle(ChatFormatting.DARK_GRAY));
			search.setValue(query);
			search.setResponder(v -> {
				if (!v.equals(query)) {
					query = v;
					scrollTarget = scroll = 0;
					if (!v.isEmpty() && openMod != null) { openMod = null; needsRebuild = true; }
				}
			});
		}
		buildPage();
	}

	private boolean gridMode() {
		return openMod == null && (!query.isEmpty() || (tab != Tab.FPS && tab != Tab.KEYS));
	}

	private void buildPage() {
		clearWidgets();
		boxes.clear();
		rows.clear();
		addWidget(search);
		scroll = scrollTarget = 0;
		if (openMod != null) settingsRows(openMod);
		else if (query.isEmpty() && tab == Tab.FPS) { settingsRows(Mods.GENERAL); rows.add(new HeaderRow(() -> "FPS boost")); fpsRows(); }
		else if (query.isEmpty() && tab == Tab.KEYS) keyRows();
	}

	private void open(SurgeMod mod) {
		openMod = mod;
		listening = null;
		buildPage();
	}

	private void back() {
		openMod = null;
		listening = null;
		buildPage();
	}

	private List<SurgeMod> visibleMods() {
		List<SurgeMod> out = new ArrayList<>();
		String q = query.toLowerCase(Locale.ROOT).trim();
		for (SurgeMod m : Mods.all()) {
			if (!q.isEmpty()) {
				if (m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q)) out.add(m);
				continue;
			}
			boolean show = switch (tab) {
				case ALL -> true;
				case HUD -> m.category == SurgeMod.Category.HUD;
				case PVP -> m.category == SurgeMod.Category.PVP;
				case VISUAL -> m.category == SurgeMod.Category.VISUAL;
				case UTILITY -> m.category == SurgeMod.Category.UTILITY;
				default -> false;
			};
			if (show) out.add(m);
		}
		return out;
	}

	private void settingsRows(SurgeMod mod) {
		List<Setting<?>> list = mod.pageSettings();
		for (int i = 0; i < list.size(); i++) {
			Setting<?> s = list.get(i);
			if (s instanceof Setting.Header h) {
				boolean hasMore = false;
				for (int j = i + 1; j < list.size() && !(list.get(j) instanceof Setting.Header); j++) hasMore = true;
				if (hasMore) rows.add(new HeaderRow(() -> h.name));
			} else if (s instanceof Setting.Toggle t) {
				rows.add(new ToggleRow(t.name, null, t::on, t::flip, s).when(s::visible));
			} else if (s instanceof Setting.Slider sl) {
				rows.add(new SliderRow(sl.name, new SliderModel() {
					public float fraction() { return sl.fraction(); }
					public void setFraction(float f) { sl.setFraction(f); }
					public String text() { return sl.format(); }
				}).when(s::visible));
			} else if (s instanceof Setting.Choice c) {
				rows.add(new ChoiceRow(c.name, () -> c.label.apply(c.get()), c::cycle).when(s::visible));
			} else if (s instanceof Setting.Color c) {
				rows.add(new ColorRow(c).when(s::visible));
			} else if (s instanceof Setting.Button b) {
				rows.add(new ButtonRow(b.name, b.label, b.action).when(s::visible));
			}
		}
		if (mod == Mods.WAYPOINTS) waypointRows();
		if (mod == Mods.COMMAND_KEYS) {
			rows.add(new ButtonRow("Your command keys", "Edit", () -> { openMod = null; tab = Tab.KEYS; buildPage(); }));
		}
		if (mod == Mods.ZOOM || mod == Mods.FREELOOK) {
			rows.add(new InfoRow("Change the key in Options > Controls > Key Binds, under Surge Client."));
		}
		if (!list.isEmpty()) rows.add(new ButtonRow("Reset settings", "Reset", mod::resetSettings));
	}

	private void waypointRows() {
		List<SurgeConfig.Waypoint> all = SurgeConfig.get().waypoints;
		String here = minecraft != null ? Waypoints.worldKey(minecraft) : "";
		List<SurgeConfig.Waypoint> shown = new ArrayList<>();
		for (SurgeConfig.Waypoint w : all) if (here.isEmpty() || w.world.equals(here)) shown.add(w);
		rows.add(new HeaderRow(() -> here.isEmpty() ? "All waypoints" : "Waypoints in this world"));
		if (shown.isEmpty()) rows.add(new InfoRow("No waypoints yet. Press Add here, or bind the Add Waypoint key in Controls."));
		for (SurgeConfig.Waypoint w : shown) rows.add(new WaypointRow(w));
	}

	private void fpsRows() {
		var o = minecraft.options;
		rows.add(new PresetRow());
		rows.add(new SliderRow("Render distance", intOption(o.renderDistance(), 2, 32, 1, " chunks")));
		rows.add(new SliderRow("Simulation distance", intOption(o.simulationDistance(), 5, 32, 1, " chunks")));
		rows.add(new SliderRow("Max FPS", new SliderModel() {
			public float fraction() { return (o.framerateLimit().get() - 10) / 250f; }
			public void setFraction(float f) {
				int v = Math.round((10 + f * 250) / 10f) * 10;
				if (v != o.framerateLimit().get()) o.framerateLimit().set(Math.max(10, Math.min(260, v)));
			}
			public String text() { int v = o.framerateLimit().get(); return v >= 260 ? "Unlimited" : String.valueOf(v); }
		}));
		rows.add(new SliderRow("Entity distance", new SliderModel() {
			public float fraction() { return (float) ((o.entityDistanceScaling().get() - 0.5) / 4.5); }
			public void setFraction(float f) {
				double v = Math.round((0.5 + f * 4.5) * 4) / 4.0;
				if (v != o.entityDistanceScaling().get()) o.entityDistanceScaling().set(v);
			}
			public String text() { return Math.round(o.entityDistanceScaling().get() * 100) + "%"; }
		}));
		rows.add(new ToggleRow("VSync", null, () -> o.enableVsync().get(), () -> o.enableVsync().set(!o.enableVsync().get()), "vsync"));
		SurgeConfig cfg = SurgeConfig.get();
		rows.add(new SliderRow("Entity culling distance", cullSlider(() -> cfg.cullItems, v -> cfg.cullItems = v, 16, 96)).tip("Dropped items, XP orbs and arrows farther than this aren't drawn"));
		rows.add(new SliderRow("Block entity distance", cullSlider(() -> cfg.cullBlockEntities, v -> cfg.cullBlockEntities = v, 24, 128)).tip("Chests, signs and banners farther than this aren't drawn"));
		rows.add(new SliderRow("Particle distance", cullSlider(() -> cfg.cullParticles, v -> cfg.cullParticles = v, 8, 64)).tip("Particles farther than this aren't drawn"));
		rows.add(new SliderRow("Nametag distance", cullSlider(() -> cfg.nametagDistance, v -> cfg.nametagDistance = v, 16, 128)).tip("Nametags farther than this aren't drawn"));

		rows.add(new HeaderRow(() -> "Surge FPS boost (no visual change)"));
		boolean pretty = true;
		for (Performance.Feature f : Performance.FEATURES) {
			if (pretty && !f.invisible()) {
				pretty = false;
				rows.add(new HeaderRow(() -> "Extra FPS (changes how the game looks)"));
			}
			rows.add(new ToggleRow(f.name(), new ItemStack(f.icon()), () -> Performance.enabled(f), () -> Performance.set(minecraft, f, !Performance.enabled(f)), "perf:" + f.key())
				.tip(f.description()));
		}

		List<PerfMods.Mod> mods = PerfMods.list();
		rows.add(new HeaderRow(() -> "Performance mods"));
		if (mods.isEmpty()) rows.add(new InfoRow("Start the game from the Surge launcher to manage these."));
		for (PerfMods.Mod m : mods) {
			rows.add(new ToggleRow(m.title(), null, () -> PerfMods.wanted(m), () -> PerfMods.toggle(m), "pm:" + m.file())
				.status(() -> PerfMods.needsRestart(m) ? "next start" : null)
				.tip(PerfMods.needsRestart(m) ? "Changes when you next press Play" : m.file()));
		}
	}

	private SliderModel cullSlider(IntSupplier get, IntConsumer set, int min, int max) {
		return new SliderModel() {
			public float fraction() { return (get.getAsInt() - min) / (float) (max - min); }
			public void setFraction(float f) {
				int v = Math.round((min + f * (max - min)) / 4f) * 4;
				v = Math.max(min, Math.min(max, v));
				if (v != get.getAsInt()) { set.accept(v); Performance.applyDistances(); SurgeConfig.markDirty(); }
			}
			public String text() { return get.getAsInt() + " blocks"; }
		};
	}

	private static SliderModel intOption(net.minecraft.client.OptionInstance<Integer> opt, int min, int max, int step, String suffix) {
		return new SliderModel() {
			public float fraction() { return (opt.get() - min) / (float) (max - min); }
			public void setFraction(float f) {
				int v = Math.round((min + f * (max - min)) / step) * step;
				v = Math.max(min, Math.min(max, v));
				if (v != opt.get()) opt.set(v);
			}
			public String text() { return opt.get() + suffix; }
		};
	}

	private void keyRows() {
		rows.add(new ToggleRow("Command Keys", null, () -> Mods.COMMAND_KEYS.state().enabled, Mods.COMMAND_KEYS::toggle, "ck:on"));
		rows.add(new InfoRow("Pick a key, then type what it sends. Start with / for a command. {x} {y} {z} {name} {server} are filled in for you."));
		List<SurgeConfig.CommandKey> list = SurgeConfig.get().commandKeys;
		for (SurgeConfig.CommandKey k : list) rows.add(new CommandKeyRow(k));
		rows.add(new AddRow("Add command key", () -> {
			list.add(new SurgeConfig.CommandKey());
			SurgeConfig.save();
			needsRebuild = true;
		}));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0) {
			scrollTarget = Math.max(0, Math.min(Math.max(0, contentH - viewH()), scrollTarget - (float) scrollY * 28f));
			return true;
		}
		return false;
	}

	private int viewTop() {
		return openMod != null ? cy + 34 : cy;
	}

	private int viewH() {
		return cy + ch - viewTop();
	}

	private void pollInput(int mx, int my) {
		long win = minecraft.getWindow().handle();
		boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		clickL = l && !wasL;
		clickR = r && !wasR;
		downL = l;
		wasL = l;
		wasR = r;

		boolean esc = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_ESCAPE) == GLFW.GLFW_PRESS;
		boolean escPressed = esc && !escWas;
		escWas = esc;

		if (listening != null) {
			int k = CommandKeys.pollPressed(minecraft, keyPrev);
			if (k >= 0) {
				IntConsumer target = listening;
				listening = null;
				target.accept(k == GLFW.GLFW_KEY_ESCAPE ? -1 : k);
				SurgeConfig.save();
			}
			return;
		}
		if (escPressed) {
			if (getFocused() instanceof EditBox) setFocused(null);
			else if (openMod != null) back();
		}
		if (clickL) {
			boolean onBox = false;
			for (EditBox b : boxes) if (b.visible && Ui.hover(mx, my, b.getX() - 4, b.getY() - 4, b.getWidth() + 8, b.getHeight() + 8)) onBox = true;
			if (search != null && Ui.hover(mx, my, search.getX() - 16, search.getY() - 5, search.getWidth() + 20, search.getHeight() + 10)) {
				onBox = true;
				setFocused(search);
				search.setFocused(true);
			}
			if (!onBox && getFocused() instanceof EditBox eb) { eb.setFocused(false); setFocused(null); }
		}
		if (!downL) { dragging = null; drag2d = null; }
	}

	private void listen(IntConsumer target) {
		keyPrev.clear();
		CommandKeys.pollPressed(minecraft, keyPrev);
		listening = target;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		Ui.frame();
		if (needsRebuild) { needsRebuild = false; buildPage(); }
		action = null;
		tip = null;
		pollInput(mx, my);

		float k = openProgress();
		boolean animating = k < 1f;
		if (animating) {
			float s = 0.94f + 0.06f * k;
			g.pose().pushMatrix();
			g.pose().translate(this.width / 2f, this.height / 2f);
			g.pose().scale(s, s);
			g.pose().translate(-this.width / 2f, -this.height / 2f);
		}

		Ui.shadow(g, px, py, pw, ph);
		Ui.box(g, px, py, pw, ph, Ui.BG, Ui.EDGE);
		g.fill(px + 1, py + 1, px + SIDEBAR_W, py + ph - 1, Ui.SIDEBAR);
		g.fill(px + SIDEBAR_W, py + 1, px + SIDEBAR_W + 1, py + ph - 1, Ui.EDGE);

		header(g, mx, my, delta);
		sidebar(g, mx, my);

		scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		if (Math.abs(scroll - scrollTarget) < 0.5f) scroll = scrollTarget;

		for (EditBox b : boxes) b.visible = false;
		if (gridMode()) grid(g, mx, my);
		else page(g, mx, my, delta);

		if (animating) g.pose().popMatrix();

		if (tip != null && tipKey != null && System.currentTimeMillis() - tipSince > 350) Ui.tooltip(g, font, tip, mx, my, this.width, this.height);
		if (action != null) { if (!silent) Ui.click(); Runnable a = action; action = null; a.run(); }
		silent = false;
	}

	private void setTip(Object key, String text) {
		if (text == null || text.isEmpty()) return;
		if (!key.equals(tipKey)) { tipKey = key; tipSince = System.currentTimeMillis(); }
		tip = text;
	}

	private void header(GuiGraphics g, int mx, int my, float delta) {
		Ui.logo(g, px + 9, py + 7, 16);
		g.drawString(font, Component.literal("Surge").withStyle(ChatFormatting.BOLD), px + 30, py + 11, Ui.TEXT, true);
		String fps = minecraft.getFps() + " fps";
		g.drawString(font, fps, px + 32 + font.width("Surge") + 6, py + 11, Ui.FAINT, false);

		int bx = px + pw - 26;
		if (Ui.iconButton(g, mx, my, bx, py + 6, 18, Ui.CROSS) && clickL) action = this::onClose;
		int ew = font.width("Edit HUD") + 16;
		bx -= ew + 4;
		if (Ui.pill(g, font, mx, my, bx, py + 6, ew, 18, "Edit HUD", true) && clickL) action = () -> minecraft.setScreen(new HudEditorScreen(this));
		int sw = Math.min(140, bx - (px + SIDEBAR_W + 8) - 8);
		int sx = bx - sw - 6;
		boolean focused = search.isFocused();
		Ui.box(g, sx, py + 6, sw, 18, 0xFF161619, focused ? Ui.ACCENT : Ui.EDGE);
		Ui.icon(g, Ui.SEARCH, sx + 5, py + 11, 1, focused ? Ui.ACCENT_2 : Ui.FAINT);
		search.setX(sx + 17);
		search.setY(py + 11);
		search.setWidth(sw - 22);
		search.render(g, mx, my, delta);
		g.fill(px + SIDEBAR_W + 1, py + HEADER_H, px + pw - 1, py + HEADER_H + 1, Ui.EDGE);
	}

	private void sidebar(GuiGraphics g, int mx, int my) {
		int y = py + 8;
		for (Tab t : Tab.values()) {
			if (t == Tab.KEYS) { g.fill(px + 10, y + 2, px + SIDEBAR_W - 10, y + 3, 0xFF2E2E33); y += 7; }
			boolean sel = t == tab && openMod == null && query.isEmpty();
			boolean selSoft = t == tab && (openMod != null || !query.isEmpty());
			if (Ui.flat(g, mx, my, px + 4, y, SIDEBAR_W - 8, 20, sel, "tab:" + t) && clickL) {
				action = () -> { tab = t; openMod = null; query = ""; search.setValue(""); buildPage(); };
			}
			Ui.item(g, t.stack(), px + 9, y + 3, 14);
			g.drawString(font, Ui.fit(font, t.label, SIDEBAR_W - 36), px + 28, y + 6, sel || selSoft ? Ui.TEXT : Ui.MUTED, false);
			y += 22;
		}
		int by = py + ph - 52;
		if (Ui.itemButton(g, mx, my, px + 8, by, 20, new ItemStack(Items.LEATHER_CHESTPLATE)) && clickL) action = () -> minecraft.setScreen(new WardrobeScreen(this));
		g.drawString(font, "Wardrobe", px + 34, by + 6, Ui.hover(mx, my, px + 8, by, SIDEBAR_W - 16, 20) ? Ui.TEXT : Ui.MUTED, false);
		if (Ui.hover(mx, my, px + 28, by, SIDEBAR_W - 36, 20) && clickL) action = () -> minecraft.setScreen(new WardrobeScreen(this));
		by += 24;
		if (Ui.itemButton(g, mx, my, px + 8, by, 20, new ItemStack(Items.ARMOR_STAND)) && clickL) action = () -> minecraft.setScreen(new EmoteWheelScreen());
		g.drawString(font, "Emotes", px + 34, by + 6, Ui.hover(mx, my, px + 8, by, SIDEBAR_W - 16, 20) ? Ui.TEXT : Ui.MUTED, false);
		if (Ui.hover(mx, my, px + 28, by, SIDEBAR_W - 36, 20) && clickL) action = () -> minecraft.setScreen(new EmoteWheelScreen());
	}

	private void grid(GuiGraphics g, int mx, int my) {
		List<SurgeMod> mods = visibleMods();
		int cols = Math.max(2, (cw + GAP) / 92);
		int cardW = (cw - (cols - 1) * GAP) / cols;
		int rowsN = (mods.size() + cols - 1) / cols;
		contentH = rowsN * (CARD_H + GAP);
		scrollTarget = Math.max(0, Math.min(Math.max(0, contentH - ch), scrollTarget));

		g.enableScissor(cx - 2, cy, cx + cw + 2, cy + ch);
		boolean inView = Ui.hover(mx, my, cx, cy, cw, ch);
		for (int i = 0; i < mods.size(); i++) {
			SurgeMod m = mods.get(i);
			int x = cx + (i % cols) * (cardW + GAP);
			int y = cy + (i / cols) * (CARD_H + GAP) - Math.round(scroll);
			if (y + CARD_H < cy || y > cy + ch) continue;
			card(g, m, x, y, cardW, mx, my, inView);
		}
		g.disableScissor();
		if (mods.isEmpty()) Ui.centered(g, font, "No mods match \"" + query + "\"", cx + cw / 2, cy + 40, Ui.MUTED);
		scrollbar(g, cy, ch);
	}

	private void card(GuiGraphics g, SurgeMod m, int x, int y, int w, int mx, int my, boolean inView) {
		boolean hov = inView && Ui.hover(mx, my, x, y, w, CARD_H);
		float a = Ui.anim("card:" + m.id, hov);
		float on = Ui.anim("on:" + m.id, m.enabled());
		int lift = Math.round(a);
		int yy = y - lift;
		Ui.chunky(g, x, yy, w, CARD_H, Colors.lerp(Ui.SURFACE, Ui.SURFACE_HOVER, a), 0xFF26262A);
		if (a > 0.02f) Ui.outline(g, x, yy, w, CARD_H, Colors.alpha(0xFFFFFFFF, a * 0.85f));
		Ui.item(g, m.icon(), x + w / 2 - 12, yy + 6, 24);
		String name = Ui.fit(font, m.name, w - 8);
		g.drawString(font, name, x + (w - font.width(name)) / 2, yy + 34, Ui.TEXT, true);

		int bx = x + 4, by = yy + CARD_H - 18, bw = w - 8, bh = 13;
		String status;
		int fill, lip;
		if (!m.toggleable) {
			status = "Open";
			fill = Ui.ACCENT;
			lip = Ui.ACCENT_DIM;
		} else {
			status = m.enabled() ? "Enabled" : "Disabled";
			fill = Colors.lerp(Ui.OFF, Ui.ON, on);
			lip = Colors.lerp(Ui.OFF_LIP, Ui.ON_LIP, on);
		}
		g.fill(bx, by, bx + bw, by + bh, Ui.EDGE);
		g.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, lip);
		g.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 3, fill);
		Ui.centeredShadow(g, font, status, bx + bw / 2, by + 2, Ui.TEXT);

		boolean gearHov = false;
		if (m.hasSettings()) {
			int gx = x + w - 13, gy = yy + 4;
			gearHov = hov && Ui.hover(mx, my, gx - 2, gy - 2, 12, 12);
			Ui.icon(g, Ui.GEAR, gx, gy, 1, gearHov ? Ui.TEXT : Colors.alpha(Ui.MUTED, 0.5f + a * 0.5f));
		}
		if (hov) {
			setTip("card:" + m.id, m.description + (m.hasSettings() ? "  (Right click for settings)" : ""));
			if (clickL) {
				if (gearHov || !m.toggleable) action = () -> openOrJump(m);
				else { action = () -> { m.toggle(); Ui.toggleSound(m.enabled()); }; silent = true; }
			} else if (clickR && m.hasSettings()) {
				action = () -> openOrJump(m);
			}
		}
	}

	private void openOrJump(SurgeMod m) {
		if (m == Mods.COMMAND_KEYS && !query.isEmpty()) { query = ""; search.setValue(""); }
		open(m);
	}

	private void scrollbar(GuiGraphics g, int top, int h) {
		if (contentH <= h) return;
		int x = cx + cw + 4;
		g.fill(x, top, x + 2, top + h, 0x30FFFFFF);
		int barH = Math.max(16, h * h / contentH);
		int barY = top + Math.round((h - barH) * (scroll / Math.max(1, contentH - h)));
		g.fill(x, barY, x + 2, barY + barH, Ui.ACCENT_2);
	}

	private void page(GuiGraphics g, int mx, int my, float delta) {
		int top = cy;
		if (openMod != null) {
			SurgeMod m = openMod;
			if (Ui.iconButton(g, mx, my, cx, cy, 20, Ui.BACK) && clickL) action = this::back;
			Ui.item(g, m.icon(), cx + 26, cy + 2, 16);
			g.drawString(font, Component.literal(m.name).withStyle(ChatFormatting.BOLD), cx + 46, cy + 1, Ui.TEXT, true);
			int descW = cw - 46 - (m.toggleable ? 32 : 0);
			g.drawString(font, Ui.fit(font, m.description, descW), cx + 46, cy + 12, Ui.MUTED, false);
			if (m.toggleable) {
				int sx = cx + cw - 24;
				Ui.toggleSwitch(g, sx, cy + 4, m.enabled(), "page:" + m.id);
				if (Ui.hover(mx, my, sx - 2, cy + 2, 26, 16) && clickL) action = m::toggle;
			}
			g.fill(cx, cy + 27, cx + cw, cy + 28, 0xFF34343A);
			top = cy + 34;
		}
		int viewH = cy + ch - top;
		int total = 0;
		for (Row r : rows) if (r.visible()) total += r.height();
		contentH = total;
		scrollTarget = Math.max(0, Math.min(Math.max(0, contentH - viewH), scrollTarget));

		g.enableScissor(cx - 2, top, cx + cw + 2, top + viewH);
		boolean inView = Ui.hover(mx, my, cx, top, cw, viewH);
		int y = top - Math.round(scroll);
		for (Row r : rows) {
			if (!r.visible()) continue;
			int h = r.height();
			if (y + h >= top && y <= top + viewH) r.draw(g, cx, y, cw - 6, mx, my, inView && Ui.hover(mx, my, cx, y, cw - 6, h), delta);
			y += h;
		}
		g.disableScissor();
		if (contentH > viewH) {
			int x = cx + cw + 4;
			g.fill(x, top, x + 2, top + viewH, 0x30FFFFFF);
			int barH = Math.max(16, viewH * viewH / contentH);
			int barY = top + Math.round((viewH - barH) * (scroll / Math.max(1, contentH - viewH)));
			g.fill(x, barY, x + 2, barY + barH, Ui.ACCENT_2);
		}

		if (dragging != null && downL) dragging.setFraction(Math.max(0, Math.min(1, (mx - dragX) / (float) dragW)));
		if (drag2d != null && downL) drag2d.accept(mx, my);
	}

	interface SliderModel {
		float fraction();

		void setFraction(float f);

		String text();
	}

	private abstract class Row {
		private BooleanSupplier when = () -> true;
		private String tipText;

		Row when(BooleanSupplier w) { this.when = w; return this; }

		Row tip(String t) { this.tipText = t; return this; }

		boolean visible() { return when.getAsBoolean(); }

		int height() { return 22; }

		abstract void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta);

		void base(GuiGraphics g, int x, int y, int w, boolean hov, String label, ItemStack icon) {
			float a = Ui.anim(this, hov);
			if (a > 0.01f) g.fill(x, y + 1, x + w, y + height() - 1, Colors.alpha(0xFF2E2E33, a));
			int tx = x + 6;
			if (icon != null) { Ui.item(g, icon, x + 4, y + (height() - 14) / 2, 14); tx = x + 23; }
			g.drawString(font, Ui.fit(font, label, w / 2 + 20), tx, y + (height() - 8) / 2, Ui.TEXT, false);
			if (hov && tipText != null) setTip(this, tipText);
		}
	}

	private final class HeaderRow extends Row {
		final Supplier<String> text;

		HeaderRow(Supplier<String> text) { this.text = text; }

		@Override int height() { return 20; }

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			String t = text.get();
			g.drawString(font, t, x + 2, y + 9, Ui.ACCENT_2, false);
			int lx = x + 8 + font.width(t);
			g.fill(lx, y + 13, x + w, y + 14, 0xFF34343A);
		}
	}

	private final class InfoRow extends Row {
		final String text;

		InfoRow(String text) { this.text = text; }

		private List<String> lines(int w) {
			List<String> out = new ArrayList<>();
			StringBuilder line = new StringBuilder();
			for (String word : text.split(" ")) {
				String next = line.isEmpty() ? word : line + " " + word;
				if (font.width(next) > w && !line.isEmpty()) { out.add(line.toString()); line = new StringBuilder(word); }
				else line = new StringBuilder(next);
			}
			if (!line.isEmpty()) out.add(line.toString());
			return out;
		}

		@Override int height() { return lines(cw - 18).size() * 10 + 8; }

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			List<String> ls = lines(w - 12);
			for (int i = 0; i < ls.size(); i++) g.drawString(font, ls.get(i), x + 6, y + 4 + i * 10, Ui.MUTED, false);
		}
	}

	private final class ToggleRow extends Row {
		final String label;
		final ItemStack icon;
		final BooleanSupplier get;
		final Runnable flip;
		final Object key;
		Supplier<String> status = () -> null;

		ToggleRow(String label, ItemStack icon, BooleanSupplier get, Runnable flip, Object key) {
			this.label = label;
			this.icon = icon;
			this.get = get;
			this.flip = flip;
			this.key = key;
		}

		ToggleRow status(Supplier<String> s) { this.status = s; return this; }

		@Override ToggleRow tip(String t) { super.tip(t); return this; }

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			base(g, x, y, w, hov, label, icon);
			int sx = x + w - 28;
			String st = status.get();
			if (st != null) g.drawString(font, st, sx - 6 - font.width(st), y + 7, Ui.YELLOW, false);
			Ui.toggleSwitch(g, sx, y + 5, get.getAsBoolean(), "sw:" + key);
			if (hov && clickL) { action = () -> { flip.run(); Ui.toggleSound(get.getAsBoolean()); }; silent = true; }
		}
	}

	private final class SliderRow extends Row {
		final String label;
		final SliderModel model;

		SliderRow(String label, SliderModel model) {
			this.label = label;
			this.model = model;
		}

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			base(g, x, y, w, hov, label, null);
			int sw = Math.min(120, w / 2 - 50);
			String val = model.text();
			int vw = Math.max(34, font.width(val));
			int sx = x + w - 8 - vw - 8 - sw;
			Ui.slider(g, sx, y + 5, sw, model.fraction(), dragging == model);
			g.drawString(font, val, x + w - 6 - font.width(val), y + 7, Ui.ACCENT_2, false);
			if (clickL && Ui.hover(mx, my, sx - 4, y + 2, sw + 8, 18) && hov) {
				dragging = model;
				dragX = sx;
				dragW = sw;
			}
		}
	}

	private final class ChoiceRow extends Row {
		final String label;
		final Supplier<String> value;
		final IntConsumer cycle;

		ChoiceRow(String label, Supplier<String> value, IntConsumer cycle) {
			this.label = label;
			this.value = value;
			this.cycle = cycle;
		}

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			base(g, x, y, w, hov, label, null);
			int bw = Math.min(110, w / 2 - 10);
			int bx = x + w - 4 - bw;
			boolean over = hov && Ui.hover(mx, my, bx, y + 3, bw, 16);
			float a = Ui.anim("choice:" + this, over);
			Ui.chunky(g, bx, y + 3, bw, 16, Colors.lerp(Ui.BUTTON, Ui.BUTTON_HOVER, a), Ui.BUTTON_LIP);
			boolean left = mx < bx + bw / 2;
			g.drawString(font, "<", bx + 5, y + 7, over && left ? Ui.TEXT : Ui.FAINT, false);
			g.drawString(font, ">", bx + bw - 10, y + 7, over && !left ? Ui.TEXT : Ui.FAINT, false);
			String v = Ui.fit(font, value.get(), bw - 26);
			g.drawString(font, v, bx + (bw - font.width(v)) / 2, y + 7, Ui.TEXT, true);
			if (over && clickL) action = () -> cycle.accept(left ? -1 : 1);
			else if (hov && clickL) action = () -> cycle.accept(1);
		}
	}

	private final class ColorRow extends Row {
		final Setting.Color setting;
		boolean open;
		float hue, sat = 1f, val = 1f;

		ColorRow(Setting.Color setting) { this.setting = setting; }

		@Override int height() { return open ? 22 + 64 : 22; }

		private void loadHsv() {
			int rgb = Colors.parse(setting.chroma() ? "FFFFFF" : setting.get());
			float r = ((rgb >> 16) & 255) / 255f, gg = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
			float max = Math.max(r, Math.max(gg, b)), min = Math.min(r, Math.min(gg, b)), d = max - min;
			val = max;
			sat = max == 0 ? 0 : d / max;
			if (d == 0) return;
			float h = max == r ? ((gg - b) / d) % 6 : max == gg ? (b - r) / d + 2 : (r - gg) / d + 4;
			hue = ((h / 6f) % 1f + 1f) % 1f;
		}

		private void store() {
			setting.set(String.format("%06X", Colors.hsv(hue, sat, val) & 0xFFFFFF));
		}

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			base(g, x, y, w, hov && my < y + 22, setting.name, null);
			String[] pal = Setting.Color.PALETTE;
			int sz = 10, gap = 2;
			int n = pal.length + 1;
			int total = n * (sz + gap) - gap;
			int sx = x + w - 4 - total;
			boolean custom = !setting.chroma();
			for (String p : pal) if (p.equalsIgnoreCase(setting.get())) custom = false;
			for (int i = 0; i < n; i++) {
				int bx = sx + i * (sz + gap), by = y + 6;
				boolean pick = i == pal.length;
				String v = pick ? null : pal[i];
				boolean sel = pick ? custom || open : v.equalsIgnoreCase(setting.get());
				boolean over = hov && Ui.hover(mx, my, bx - 1, by - 1, sz + 2, sz + 2);
				g.fill(bx - 1, by - 1, bx + sz + 1, by + sz + 1, sel ? 0xFFFFFFFF : over ? 0xFFB9B9BE : Ui.EDGE);
				if (pick) {
					g.fill(bx, by, bx + sz, by + sz, custom ? Colors.parse(setting.get()) : 0xFF2A2A2E);
					Ui.icon(g, new String[]{"..X..", "..X..", "XXXXX", "..X..", "..X.."}, bx + 3, by + 3, 1, custom ? 0xFF000000 : 0xFFFFFFFF);
					if (over && clickL) action = () -> { open = !open; if (open) loadHsv(); };
					if (over) setTip("picker:" + setting.key, "Pick any color");
				} else if ("chroma".equals(v)) {
					for (int c = 0; c < sz; c++) g.fill(bx + c, by, bx + c + 1, by + sz, Colors.chroma(c / (float) sz));
					if (over) setTip("sw:" + v, "Chroma (rainbow)");
				} else {
					g.fill(bx, by, bx + sz, by + sz, Colors.parse(v));
					if (over) setTip("sw:" + v, "#" + v);
				}
				if (!pick && over && clickL) action = () -> { setting.set(v); open = false; };
			}
			if (!open) return;
			int pw = Math.min(150, w - 70), ph = 46;
			int px = x + w - 4 - pw - 40, py = y + 24;
			for (int i = 0; i < pw; i++) {
				int top = Colors.hsv(hue, i / (float) (pw - 1), 1f);
				g.fillGradient(px + i, py, px + i + 1, py + ph, top, 0xFF000000);
			}
			Ui.outline(g, px - 1, py - 1, pw + 2, ph + 2, Ui.EDGE);
			int kx = px + Math.round(sat * (pw - 1)), ky = py + Math.round((1 - val) * (ph - 1));
			Ui.outline(g, kx - 2, ky - 2, 5, 5, 0xFFFFFFFF);
			int hy = py + ph + 5;
			for (int i = 0; i < pw; i++) g.fill(px + i, hy, px + i + 1, hy + 7, Colors.hsv(i / (float) (pw - 1), 1f, 1f));
			Ui.outline(g, px - 1, hy - 1, pw + 2, 9, Ui.EDGE);
			int hx = px + Math.round(hue * (pw - 1));
			g.fill(hx - 1, hy - 2, hx + 2, hy + 9, 0xFFFFFFFF);
			int cx2 = px + pw + 8;
			g.fill(cx2, py, cx2 + 30, py + 24, Ui.EDGE);
			g.fill(cx2 + 1, py + 1, cx2 + 29, py + 23, Colors.hsv(hue, sat, val));
			g.drawString(font, "#" + (setting.chroma() ? "------" : setting.get().toUpperCase()), cx2 - 2, py + 30, Ui.MUTED, false);
			if (clickL && Ui.hover(mx, my, px - 2, py - 2, pw + 4, ph + 4)) drag2d = (ax, ay) -> {
				sat = Math.max(0, Math.min(1, (ax - px) / (float) (pw - 1)));
				val = 1 - Math.max(0, Math.min(1, (ay - py) / (float) (ph - 1)));
				store();
			};
			if (clickL && Ui.hover(mx, my, px - 2, hy - 3, pw + 4, 12)) drag2d = (ax, ay) -> {
				hue = Math.max(0, Math.min(0.999f, (ax - px) / (float) (pw - 1)));
				store();
			};
		}
	}

	private final class ButtonRow extends Row {
		final String label, button;
		final Runnable run;

		ButtonRow(String label, String button, Runnable run) {
			this.label = label;
			this.button = button;
			this.run = run;
		}

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			base(g, x, y, w, hov, label, null);
			int bw = Math.max(60, font.width(button) + 16);
			int bx = x + w - 4 - bw;
			if (Ui.pill(g, font, mx, my, bx, y + 3, bw, 16, button, false) && hov && clickL) action = () -> { run.run(); needsRebuild = true; };
		}
	}

	private final class AddRow extends Row {
		final String label;
		final Runnable run;

		AddRow(String label, Runnable run) {
			this.label = label;
			this.run = run;
		}

		@Override int height() { return 26; }

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			float a = Ui.anim(this, hov);
			int col = Colors.lerp(0xFF4A4A50, Ui.ACCENT_2, a);
			for (int i = x + 2; i < x + w - 2; i += 4) { g.fill(i, y + 3, i + 2, y + 4, col); g.fill(i, y + 22, i + 2, y + 23, col); }
			for (int j = y + 3; j < y + 23; j += 4) { g.fill(x + 2, j, x + 3, j + 2, col); g.fill(x + w - 3, j, x + w - 2, j + 2, col); }
			int tw = font.width(label) + 12;
			Ui.icon(g, Ui.PLUS, x + (w - tw) / 2, y + 9, 1, col);
			g.drawString(font, label, x + (w - tw) / 2 + 12, y + 9, Colors.lerp(Ui.MUTED, Ui.TEXT, a), false);
			if (hov && clickL) action = run;
		}
	}

	private final class PresetRow extends Row {
		@Override int height() { return 30; }

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			Performance.Preset[] ps = Performance.Preset.values();
			String[] names = {"Max FPS", "Balanced", "Quality"};
			String[] tips = {"Every switch on, including ones that change how the game looks. Render distance capped at 12.",
				"All the FPS boosts you can't see. The game looks the same.", "All Surge FPS features off."};
			int bw = (w - 8) / 3;
			for (int i = 0; i < ps.length; i++) {
				int bx = x + i * (bw + 4);
				boolean active = Performance.currentPreset() == ps[i];
				boolean over = Ui.pill(g, font, mx, my, bx, y + 4, bw, 20, names[i], active);
				final Performance.Preset p = ps[i];
				if (over) setTip("preset:" + i, tips[i]);
				if (over && clickL) action = () -> Performance.preset(minecraft, p);
			}
		}
	}

	private final class KeyButton {
		void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, IntSupplier get, IntConsumer set, Object key) {
			boolean waiting = listening != null && keyOwner == key;
			String label = waiting ? "Press a key…" : CommandKeys.keyName(get.getAsInt());
			boolean over = Ui.pill(g, font, mx, my, x, y, w, 16, label, waiting);
			if (over && hov && clickL) action = () -> { keyOwner = key; listen(set); };
		}
	}

	private Object keyOwner;
	private final KeyButton keyButton = new KeyButton();

	private final class CommandKeyRow extends Row {
		final SurgeConfig.CommandKey ck;
		final EditBox box;

		CommandKeyRow(SurgeConfig.CommandKey ck) {
			this.ck = ck;
			box = new EditBox(font, 0, 0, 100, 10, Component.literal("Command"));
			box.setBordered(false);
			box.setMaxLength(256);
			box.setValue(ck.text == null ? "" : ck.text);
			box.setHint(Component.literal("/home or gg").withStyle(ChatFormatting.DARK_GRAY));
			box.setResponder(v -> { ck.text = v; SurgeConfig.markDirty(); });
			addWidget(box);
			boxes.add(box);
		}

		@Override int height() { return 24; }

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			float a = Ui.anim(this, hov);
			if (a > 0.01f) g.fill(x, y + 1, x + w, y + 23, Colors.alpha(0xFF2E2E33, a));
			keyButton.draw(g, x + 4, y + 4, 70, mx, my, hov, () -> ck.key, k -> ck.key = k, ck);
			int bx = x + 80, bw = w - 80 - 26;
			Ui.box(g, bx, y + 4, bw, 16, 0xFF161619, box.isFocused() ? Ui.ACCENT : Ui.EDGE);
			box.setX(bx + 5);
			box.setY(y + 8);
			box.setWidth(bw - 10);
			box.visible = true;
			box.render(g, mx, my, delta);
			if (Ui.iconButton(g, mx, my, x + w - 20, y + 4, 16, Ui.CROSS) && hov && clickL) action = () -> {
				SurgeConfig.get().commandKeys.remove(ck);
				SurgeConfig.save();
				needsRebuild = true;
			};
		}
	}

	private final class WaypointRow extends Row {
		final SurgeConfig.Waypoint wp;
		final EditBox box;

		WaypointRow(SurgeConfig.Waypoint wp) {
			this.wp = wp;
			box = new EditBox(font, 0, 0, 100, 10, Component.literal("Name"));
			box.setBordered(false);
			box.setMaxLength(32);
			box.setValue(wp.name);
			box.setResponder(v -> { wp.name = v; SurgeConfig.markDirty(); });
			addWidget(box);
			boxes.add(box);
		}

		@Override int height() { return 24; }

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			float a = Ui.anim(this, hov);
			if (a > 0.01f) g.fill(x, y + 1, x + w, y + 23, Colors.alpha(0xFF2E2E33, a));
			boolean overColor = hov && Ui.hover(mx, my, x + 4, y + 5, 14, 14);
			g.fill(x + 4, y + 5, x + 18, y + 19, overColor ? 0xFFFFFFFF : Ui.EDGE);
			g.fill(x + 5, y + 6, x + 17, y + 18, Colors.parse(wp.color));
			if (overColor && clickL) action = () -> { wp.color = Waypoints.nextColor(wp.color); SurgeConfig.save(); };
			int nameW = Math.max(60, w / 3);
			Ui.box(g, x + 22, y + 4, nameW, 16, 0xFF161619, box.isFocused() ? Ui.ACCENT : Ui.EDGE);
			box.setX(x + 27);
			box.setY(y + 8);
			box.setWidth(nameW - 10);
			box.visible = true;
			box.render(g, mx, my, delta);
			String coords = wp.x + ", " + wp.y + ", " + wp.z;
			g.drawString(font, Ui.fit(font, coords, w - nameW - 80), x + 28 + nameW, y + 8, Ui.MUTED, false);
			Ui.toggleSwitch(g, x + w - 50, y + 6, wp.visible, "wpv:" + System.identityHashCode(wp));
			if (hov && Ui.hover(mx, my, x + w - 52, y + 4, 26, 16) && clickL) action = () -> { wp.visible = !wp.visible; SurgeConfig.save(); };
			if (Ui.iconButton(g, mx, my, x + w - 20, y + 4, 16, Ui.CROSS) && hov && clickL) action = () -> {
				SurgeConfig.get().waypoints.remove(wp);
				SurgeConfig.save();
				needsRebuild = true;
			};
		}
	}

	@Override
	public void onClose() {
		SurgeConfig.save();
		minecraft.options.save();
		minecraft.setScreen(parent);
	}
}
