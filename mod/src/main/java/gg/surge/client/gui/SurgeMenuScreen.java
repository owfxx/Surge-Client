package gg.surge.client.gui;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.features.CommandKeys;
import gg.surge.client.features.Waypoints;
import gg.surge.client.mods.Colors;
import gg.surge.client.mods.Mods;
import gg.surge.client.mods.Setting;
import gg.surge.client.mods.SurgeMod;
import gg.surge.client.perf.PerfCheck;
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
		PROFILES("Profiles", Items.BOOKSHELF), FPS("Settings", Items.COMPARATOR), PERF("FPS check", Items.CLOCK);
		final String label;
		final Item icon;
		ItemStack stack;

		Tab(String label, Item icon) {
			this.label = label;
			this.icon = icon;
		}

		net.minecraft.resources.Identifier tex() {
			String n = switch (this) { case ALL -> "cat_all"; case HUD -> "cat_hud"; case PVP -> "cat_pvp"; case VISUAL -> "cat_visual"; case UTILITY -> "cat_utility"; case PROFILES -> "cat_profiles"; case FPS -> "cat_settings"; case PERF -> "cat_fps"; };
			return net.minecraft.resources.Identifier.fromNamespaceAndPath("surge", "textures/gui/icon_" + n + "_32.png");
		}

		ItemStack stack() {
			if (stack == null) stack = new ItemStack(icon);
			return stack;
		}
	}

	private static final int CARD_H = 66, GAP = 6;
	private int SIDEBAR_W = 104, HEADER_H = 26;
	private static Tab tab = Tab.ALL;
	private static String query = "";

	private final Screen parent;
	private SurgeMod openMod;
	private EditBox search;
	private final List<Row> rows = new ArrayList<>();
	private final List<EditBox> boxes = new ArrayList<>();
	private boolean needsRebuild;

	private int px, py, pw, ph, cx, cy, cw, ch, wx, wy, ww, wh;
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

	public static SurgeMenuScreen profiles(Screen parent) {
		tab = Tab.PROFILES;
		query = "";
		return new SurgeMenuScreen(parent);
	}

	public static SurgeMenuScreen perf(Screen parent) {
		tab = Tab.PERF;
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
		if (Ui.glassBackdrop(g, () -> renderBlurredBackground(g), this.width, this.height)) return;
		float k = openProgress();
		g.fill(0, 0, this.width, this.height, Colors.alpha(0x50000000, k));
	}

	private float openProgress() {
		float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f);
		return 1f - (1f - t) * (1f - t) * (1f - t);
	}

	@Override
	protected void init() {
		if (openedAt == 0) { openedAt = System.currentTimeMillis(); Ui.openSound("preset:pop"); }
		ww = Math.min(this.width - 12, Math.max(420, Math.min(640, Math.round(this.width * 0.75f))));
		wh = Math.min(this.height - 12, Math.max(240, Math.min(400, Math.round(this.height * 0.8f))));
		wx = (this.width - ww) / 2;
		wy = (this.height - wh) / 2;
		int m = ww < 480 ? 6 : 9;
		SIDEBAR_W = ww < 480 ? 92 : 108;
		px = wx + m;
		py = wy + m;
		pw = ww - m * 2;
		ph = wh - m * 2;
		cx = px + SIDEBAR_W + 10;
		cy = py + HEADER_H + 6;
		cw = px + pw - 6 - cx;
		ch = py + ph - cy;
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

	private int pendingScroll;

	public void autotestSearch(String q) {
		if (search != null) search.setValue(q);
	}

	public void autotestScroll() {
		pendingScroll = 20;
	}

	private boolean gridMode() {
		return openMod == null && (!query.isEmpty() || (tab != Tab.FPS && tab != Tab.PROFILES && tab != Tab.PERF));
	}

	private void buildPage() {
		clearWidgets();
		boxes.clear();
		rows.clear();
		addWidget(search);
		scroll = scrollTarget = 0;
		if (openMod == Mods.COMMAND_KEYS) { settingsRows(openMod); keyRows(); }
		else if (openMod == Mods.DROP_PROTECT) { settingsRows(openMod); dropRows(); }
		else if (openMod != null) settingsRows(openMod);
		else if (query.isEmpty() && tab == Tab.FPS) { settingsRows(Mods.GENERAL); rows.add(new HeaderRow(() -> "FPS boost")); fpsRows(); }
		else if (query.isEmpty() && tab == Tab.PROFILES) profileRows();
		else if (query.isEmpty() && tab == Tab.PERF) rows.add(new PerfRow());
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
			if (m == Mods.PROFILES || m == Mods.GENERAL) continue;
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
		if (mod.toggleable && mod != Mods.GENERAL) rows.add(new ModKeyRow(mod));
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
		rows.add(new ButtonRow("Not sure what's slowing you down?", "FPS check", () -> { tab = Tab.PERF; buildPage(); }));
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
		rows.add(new InfoRow("Turn one on and Surge downloads it for you. Changes happen the next time you start the game."));
		for (PerfMods.Mod m : mods) {
			rows.add(new ToggleRow(m.title(), null, () -> PerfMods.wanted(m), () -> PerfMods.toggle(m), "pm:" + m.slug())
				.status(() -> PerfMods.status(m))
				.tip(m.description()));
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

	private void profileRows() {
		rows.add(new InfoRow("A profile saves every mod, setting and HUD position. Profiles are shared between all your modpacks."));
		rows.add(new NewProfileRow());
		List<String> names = gg.surge.client.features.Profiles.names();
		if (!names.isEmpty()) rows.add(new HeaderRow(() -> "Saved profiles"));
		for (String n : names) rows.add(new ProfileRow(n));
		rows.add(new HeaderRow(() -> "Servers"));
		rows.add(new ToggleRow("Remember a setup for each server", null, Mods.PROFILE_PER_SERVER::on, Mods.PROFILE_PER_SERVER::flip, "pf:server"));
		rows.add(new HeaderRow(() -> "Share"));
		rows.add(new ShareCodeRow());
		rows.add(new ImportCodeRow());
		rows.add(new ButtonRow("Copy your setup as text instead", "Export", gg.surge.client.features.Profiles::export));
		rows.add(new ButtonRow("Paste a setup someone sent you as text", "Import", gg.surge.client.features.Profiles::importClipboard));
		rows.add(new HeaderRow(() -> "Everything"));
		rows.add(new ButtonRow("Turn every mod off", "All off", () -> gg.surge.client.features.Profiles.allMods(false)));
		rows.add(new ButtonRow("Turn every mod on", "All on", () -> gg.surge.client.features.Profiles.allMods(true)));
		rows.add(new ButtonRow("Reset all settings", "Reset", gg.surge.client.features.Profiles::reset));
	}

	private static final int[] SHARE_HOURS = {1, 24, 168, 0};
	private static final String[] SHARE_NAMES = {"1 hour", "1 day", "1 week", "Never"};
	private static int shareIdx = 1;
	private static String lastShareCode = "";

	private final class ShareCodeRow extends Row {
		@Override int height() { return lastShareCode.isEmpty() ? 24 : 34; }

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			String lbl = w > 380 ? "Share code, ends after" : "Ends after";
			g.drawString(font, lbl, x + 6, y + 8, Ui.TEXT, false);
			int lx = x + 10 + font.width(lbl), pw2 = Math.min(50, (x + w - 76 - lx - 6) / 4);
			for (int i = 0; i < SHARE_HOURS.length; i++) {
				int idx = i;
				if (Ui.pill(g, font, mx, my, lx + i * (pw2 + 2), y + 4, pw2, 16, SHARE_NAMES[i], shareIdx == i) && hov && clickL) action = () -> shareIdx = idx;
			}
			if (Ui.pill(g, font, mx, my, x + w - 70, y + 4, 66, 16, "Make code", false) && hov && clickL) action = () -> gg.surge.client.features.Profiles.shareCode(SHARE_HOURS[shareIdx], code -> { lastShareCode = code; needsRebuild = true; });
			if (!lastShareCode.isEmpty()) g.drawString(font, "Your code: " + lastShareCode + " (copied)", x + 6, y + 24, 0xFF8CFFA0, false);
		}
	}

	private final class ImportCodeRow extends Row {
		final EditBox box;

		ImportCodeRow() {
			box = new EditBox(font, 0, 0, 100, 10, Component.literal("Code"));
			box.setBordered(false);
			box.setMaxLength(16);
			box.setHint(Component.literal("Type a share code").withStyle(ChatFormatting.DARK_GRAY));
			addWidget(box);
			boxes.add(box);
		}

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			int bw = w - 4 - 70 - 4;
			Ui.box(g, x + 4, y + 4, bw, 16, 0xFF161619, box.isFocused() ? Ui.ACCENT : Ui.EDGE);
			box.setX(x + 9);
			box.setY(y + 8);
			box.setWidth(bw - 10);
			box.visible = true;
			box.render(g, mx, my, delta);
			if (Ui.pill(g, font, mx, my, x + w - 70, y + 4, 66, 16, "Use code", false) && hov && clickL) action = () -> gg.surge.client.features.Profiles.importCode(box.getValue(), () -> { box.setValue(""); needsRebuild = true; });
		}
	}

	private void dropRows() {
		rows.add(new HeaderRow(() -> "Your protected items"));
		java.util.List<String> list = SurgeConfig.get().dropItems;
		if (list == null) { SurgeConfig.get().dropItems = list = new ArrayList<>(); }
		for (String id : new ArrayList<>(list)) rows.add(new DropItemRow(id));
		rows.add(new AddDropRow());
	}

	private final class DropItemRow extends Row {
		final String id;
		final ItemStack stack;

		DropItemRow(String id) {
			this.id = id;
			var key = net.minecraft.resources.Identifier.tryParse(id);
			var item = key == null ? null : net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(key);
			stack = item == null ? ItemStack.EMPTY : new ItemStack(item);
		}

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			base(g, x, y, w, hov, stack.isEmpty() ? id : stack.getHoverName().getString(), stack.isEmpty() ? null : stack);
			if (Ui.iconButton(g, mx, my, x + w - 20, y + 3, 16, Ui.CROSS) && hov && clickL) action = () -> {
				SurgeConfig.get().dropItems.remove(id);
				SurgeConfig.save();
				needsRebuild = true;
			};
		}
	}

	private final class AddDropRow extends Row {
		final EditBox box;
		String error = "";

		AddDropRow() {
			box = new EditBox(font, 0, 0, 100, 10, Component.literal("Item"));
			box.setBordered(false);
			box.setMaxLength(64);
			box.setHint(Component.literal("Item name, like diamond sword").withStyle(ChatFormatting.DARK_GRAY));
			addWidget(box);
			boxes.add(box);
		}

		@Override int height() { return error.isEmpty() ? 24 : 34; }

		private void add(net.minecraft.world.item.Item item) {
			if (item == null || item == net.minecraft.world.item.Items.AIR) { error = "No item called that"; return; }
			String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();
			if (!SurgeConfig.get().dropItems.contains(id)) SurgeConfig.get().dropItems.add(id);
			SurgeConfig.save();
			needsRebuild = true;
		}

		private net.minecraft.world.item.Item find(String q) {
			String t = q.trim().toLowerCase(Locale.ROOT);
			if (t.isEmpty()) return null;
			String asId = t.replace(' ', '_');
			var key = net.minecraft.resources.Identifier.tryParse(asId.contains(":") ? asId : "minecraft:" + asId);
			if (key != null && net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key)) return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(key);
			for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
				if (new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT).equals(t)) return item;
			}
			for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
				if (new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT).contains(t)) return item;
			}
			return null;
		}

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			int bw = w - 4 - 50 - 4 - 80 - 4;
			Ui.box(g, x + 4, y + 4, bw, 16, 0xFF161619, box.isFocused() ? Ui.ACCENT : Ui.EDGE);
			box.setX(x + 9);
			box.setY(y + 8);
			box.setWidth(bw - 10);
			box.visible = true;
			box.render(g, mx, my, delta);
			if (Ui.pill(g, font, mx, my, x + 8 + bw, y + 4, 50, 16, "Add", false) && hov && clickL) action = () -> add(find(box.getValue()));
			if (Ui.pill(g, font, mx, my, x + 12 + bw + 50, y + 4, 80, 16, "Add held item", false) && hov && clickL) action = () -> {
				if (minecraft.player != null && !minecraft.player.getMainHandItem().isEmpty()) add(minecraft.player.getMainHandItem().getItem());
				else error = "Hold an item first";
			};
			if (!error.isEmpty()) g.drawString(font, error, x + 6, y + 24, 0xFFFF7A7A, false);
		}
	}

	private void keyRows() {
		rows.add(new HeaderRow(() -> "Your command keys"));
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
			g.pose().pushMatrix();
			g.pose().translate(0, (1f - k) * 8f);
		}

		Ui.cutBox(g, wx, wy, ww, wh, 5, 0xC8121214, 0xFF2C2C31);
		g.fill(px + SIDEBAR_W + 4, py + HEADER_H + 2, px + SIDEBAR_W + 5, py + ph - 2, 0x18FFFFFF);
		header(g, mx, my, delta);
		sidebar(g, mx, my);

		if (pendingScroll > 0) { pendingScroll--; scroll = scrollTarget = Math.max(0, contentH - viewH()); }
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

	private static final String[] LEFT = {"....X...", "...XX...", "..XXX...", ".XXXX...", "..XXX...", "...XX...", "....X...", "........"};

	private boolean topButton(GuiGraphics g, int mx, int my, int x, int y, int w, String label, ItemStack icon, boolean primary) {
		boolean hov = Ui.hover(mx, my, x, y, w, 18);
		Ui.mcButton(g, x, y, w, 18, hov, false, 1f);
		int tx = x + 6;
		if (icon != null) { Ui.item(g, icon, x + 4, y + 2, 14); tx = x + 21; }
		g.drawString(font, label, tx, y + 5, hov ? 0xFFFFFFA0 : Ui.TEXT, true);
		return hov;
	}

	private void header(GuiGraphics g, int mx, int my, float delta) {
		int y = py;
		boolean bh = Ui.hover(mx, my, px, y, 18, 18);
		Ui.mcButton(g, px, y, 18, 18, bh, false, 1f);
		Ui.icon(g, LEFT, px + 5, y + 5, 1, bh ? 0xFFFFFFA0 : Ui.TEXT);
		if (bh && clickL) action = this::onClose;
		Ui.logo(g, px + 25, y + 1, 16);
		g.drawString(font, Ui.ten("SURGE"), px + 45, y + 5, Ui.TEXT, true);
		String fps = minecraft.getFps() + " fps";
		g.drawString(font, fps, px + 50 + font.width(Ui.ten("SURGE")), y + 5, Ui.FAINT, false);

		int bx = px + pw;
		boolean roomy = pw >= 420;
		int ew = font.width("Edit HUD") + 12;
		bx -= ew;
		if (topButton(g, mx, my, bx, y, ew, "Edit HUD", null, true) && clickL) action = () -> minecraft.setScreen(new HudEditorScreen(this));
		int ww = roomy ? font.width("Wardrobe") + 27 : 20;
		bx -= ww + 4;
		if (topButton(g, mx, my, bx, y, ww, roomy ? "Wardrobe" : "", WARDROBE_ICON, false) && clickL) action = () -> minecraft.setScreen(new WardrobeScreen(this));
		int left = cx;
		int sw = Math.max(60, Math.min(170, bx - 8 - left));
		int sx = left;
		boolean focused = search.isFocused();
		Ui.cutBox(g, sx, y, sw, 18, 3, 0xFF141416, focused ? Ui.ACCENT_2 : 0xFF2E2E33);
		Ui.icon(g, Ui.SEARCH, sx + 5, y + 5, 1, focused ? Ui.ACCENT_2 : 0xFF55555C);
		search.setX(sx + 17);
		search.setY(y + 5);
		search.setWidth(sw - 22);
		search.render(g, mx, my, delta);
		g.fill(px, y + HEADER_H - 5, px + pw, y + HEADER_H - 4, 0xFF2A2A2E);
	}

	private static final ItemStack WARDROBE_ICON = new ItemStack(Items.LEATHER_CHESTPLATE), EMOTE_ICON = new ItemStack(Items.ARMOR_STAND);

	private void sidebar(GuiGraphics g, int mx, int my) {
		int y = py + HEADER_H + 6;
		int rowH = ph < 230 ? 18 : 21;
		for (Tab t : Tab.values()) {
			if (t == Tab.PROFILES) { g.fill(px + 4, y + 2, px + SIDEBAR_W - 4, y + 3, 0xFF26262A); y += 7; }
			boolean sel = t == tab && openMod == null && query.isEmpty();
			boolean selSoft = t == tab && (openMod != null || !query.isEmpty());
			if (Ui.flat(g, mx, my, px, y, SIDEBAR_W, rowH - 2, sel || selSoft, "tab:" + t) && clickL) {
				action = () -> { tab = t; openMod = null; query = ""; search.setValue(""); buildPage(); };
			}
			Ui.iconTex(g, t.tex(), px + 7, y + (rowH - 2 - 14) / 2, 14, 1f);
			boolean hov = Ui.hover(mx, my, px, y, SIDEBAR_W, rowH - 2);
			g.drawString(font, Ui.fit(font, t.label, SIDEBAR_W - 30), px + 26, y + (rowH - 10) / 2, sel || selSoft ? Ui.TEXT : hov ? 0xFFD4D4D8 : Ui.FAINT, false);
			y += rowH;
		}
		int on = 0, total = 0;
		for (SurgeMod m : Mods.all()) if (m.toggleable) { total++; if (m.enabled()) on++; }
		if (py + ph - 12 > y + 6) g.drawString(font, Ui.fit(font, on + " of " + total + " mods on", SIDEBAR_W), px + 2, py + ph - 10, 0xFF4A4A52, false);
	}

	private void grid(GuiGraphics g, int mx, int my) {
		List<SurgeMod> mods = visibleMods();
		int cols = Math.max(2, (cw + GAP) / 88);
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
		float on = Ui.anim("on:" + m.id, m.enabled() || !m.toggleable);
		int yy = y;
		boolean en = m.enabled() || !m.toggleable;
		int lift = Math.round(a * 1.5f);
		yy -= lift;
		g.fill(x + 1, yy + CARD_H - 1 + lift, x + w - 1, yy + CARD_H + 1 + lift, 0x40000000);
		Ui.cutBox(g, x, yy, w, CARD_H, 4, Colors.lerp(0xF0141417, 0xF01C1C21, a), Colors.lerp(0xFF26262D, 0xFF3A3A44, a));
		int iconCol = Colors.lerp(0xFF8A8A93, 0xFFFFFFFF, Math.max(on, a));
		Ui.modIcon(g, m, x + w / 2 - 10, yy + 8, 20, iconCol);
		String name = Ui.fit(font, m.name, w - 10);
		g.drawString(font, name, x + (w - font.width(name)) / 2, yy + 33, Colors.lerp(Ui.MUTED, Ui.TEXT, Math.max(on, a)), false);

		int by = yy + CARD_H - 17;
		int barCol = !m.toggleable ? Colors.lerp(0xFF202026, 0xFF2A2A32, a) : Colors.lerp(0xFF2A1A1C, 0xFF1F4D2A, on);
		if (m.toggleable && hov) barCol = Colors.lerp(barCol, 0xFFFFFFFF, 0.06f);
		Ui.cut(g, x + 3, by, w - 6, 14, 2, barCol);
		String label = !m.toggleable ? "Open" : m.enabled() ? "Enabled" : "Disabled";
		int lc = !m.toggleable ? Ui.MUTED : Colors.lerp(0xFFE07A7A, 0xFF8CFFA8, on);
		g.drawString(font, label, x + (w - font.width(label)) / 2, by + 3, lc, false);

		boolean gearHov = false;
		if (m.hasSettings()) {
			int gx = x + w - 13, gy = yy + 5;
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
		int x = cx + cw + 2;
		g.fill(x, top, x + 2, top + h, 0x18FFFFFF);
		int barH = Math.max(16, h * h / contentH);
		int barY = top + Math.round((h - barH) * (scroll / Math.max(1, contentH - h)));
		g.fill(x, barY, x + 2, barY + barH, Ui.ACCENT_2);
	}

	private void page(GuiGraphics g, int mx, int my, float delta) {
		int top = cy;
		if (openMod != null) {
			SurgeMod m = openMod;
			if (Ui.iconButton(g, mx, my, cx, cy, 20, Ui.BACK) && clickL) action = this::back;
			Ui.modIcon(g, m, cx + 26, cy + 2, 16, Ui.TEXT);
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
			int x = cx + cw + 2;
			g.fill(x, top, x + 2, top + viewH, 0x18FFFFFF);
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
			if (a > 0.01f) Ui.cut(g, x, y + 1, w, height() - 2, 3, Colors.alpha(0xFF222226, a));
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
			g.drawString(font, t, x + 2, y + 9, Ui.TEXT, false);
			int lx = x + 8 + font.width(t);
			g.fill(lx, y + 13, x + w, y + 14, 0xFF26262A);
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
			Ui.mcButton(g, bx, y + 3, bw, 16, over, false, 1f);
			boolean left = mx < bx + bw / 2;
			g.drawString(font, "<", bx + 5, y + 7, over && left ? 0xFFFFFFA0 : 0xFFA0A0A0, true);
			g.drawString(font, ">", bx + bw - 10, y + 7, over && !left ? 0xFFFFFFA0 : 0xFFA0A0A0, true);
			String v = Ui.fit(font, value.get(), bw - 26);
			g.drawString(font, v, bx + (bw - font.width(v)) / 2, y + 7, over ? 0xFFFFFFA0 : Ui.TEXT, true);
			if (over && clickL) action = () -> cycle.accept(left ? -1 : 1);
			else if (hov && clickL) action = () -> cycle.accept(1);
		}
	}

	private final class ColorRow extends Row {
		final Setting.Color setting;
		boolean open;
		float hue, sat = 1f, val = 1f;

		final EditBox hex;
		boolean typing;

		ColorRow(Setting.Color setting) {
			this.setting = setting;
			hex = new EditBox(font, 0, 0, 48, 10, Component.literal("Hex"));
			hex.setBordered(false);
			hex.setMaxLength(7);
			hex.setValue(setting.chroma() ? "" : setting.get().toUpperCase());
			hex.setHint(Component.literal("FFFFFF").withStyle(ChatFormatting.DARK_GRAY));
			hex.setResponder(v -> {
				String c = v.trim().replace("#", "");
				if (c.matches("[0-9a-fA-F]{6}")) { typing = true; setting.set(c.toUpperCase()); loadHsv(); typing = false; }
			});
			hex.visible = false;
			addWidget(hex);
			boxes.add(hex);
		}

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
			if (!typing) hex.setValue(setting.get());
		}

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			base(g, x, y, w, hov && my < y + 22, setting.name, null);
			hex.visible = false;
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
					if (over && clickL) action = () -> { open = !open; if (open) { loadHsv(); hex.setValue(setting.chroma() ? "" : setting.get().toUpperCase()); } };
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
			Ui.box(g, cx2 - 4, py + 27, 42, 14, 0xFF161619, hex.isFocused() ? Ui.ACCENT : Ui.EDGE);
			g.drawString(font, "#", cx2 - 1, py + 30, Ui.MUTED, false);
			hex.setX(cx2 + 5);
			hex.setY(py + 30);
			hex.setWidth(32);
			hex.visible = true;
			if (!hex.isFocused() && !setting.chroma() && !hex.getValue().equalsIgnoreCase(setting.get())) hex.setValue(setting.get().toUpperCase());
			hex.render(g, mx, my, delta);
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

	private final class ModKeyRow extends Row {
		final SurgeMod mod;

		ModKeyRow(SurgeMod mod) { this.mod = mod; }

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			base(g, x, y, w, hov, "Toggle key", null);
			keyButton.draw(g, x + w - 4 - 90, y + 3, 90, mx, my, hov, () -> mod.state().key, k -> { mod.state().key = k; SurgeConfig.markDirty(); }, mod);
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

	private final class PerfRow extends Row {
		int lastW = 300;

		private List<String> lines(String text, int w) {
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

		private int findingH(PerfCheck.Finding f, int w) {
			return 18 + lines(f.detail, w - 20 - (f.fixLabel != null ? 84 : 0)).size() * 10 + 6;
		}

		@Override int height() {
			PerfCheck.Report r = PerfCheck.report;
			if (r == null || PerfCheck.state == PerfCheck.State.RECORDING) return 120;
			int h = 50 + 74 + 18;
			for (PerfCheck.Finding f : r.findings()) h += findingH(f, lastW) + 4;
			return h + 30;
		}

		private boolean button(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, boolean primary) {
			boolean over = Ui.hover(mx, my, x, y, w, h);
			float a = Ui.anim("pb:" + label + y, over);
			Ui.cutBox(g, x, y, w, h, 3, primary ? Colors.lerp(0xFFE4E4E7, 0xFFFFFFFF, a) : Colors.lerp(0xFF2A2A2E, 0xFF36363B, a), primary ? 0xFFFFFFFF : 0xFF3A3A40);
			String t = Ui.fit(font, label, w - 6);
			g.drawString(font, t, x + (w - font.width(t)) / 2, y + (h - 8) / 2, primary ? 0xFF141416 : Ui.TEXT, false);
			return over;
		}

		private Runnable startAction() {
			return () -> {
				if (minecraft.level == null) { setTip("perf:start", "Join a world or server first"); return; }
				PerfCheck.start();
				minecraft.setScreen(null);
			};
		}

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			lastW = w;
			PerfCheck.Report r = PerfCheck.report;
			if (PerfCheck.state == PerfCheck.State.RECORDING) {
				g.drawString(font, "Checking… " + PerfCheck.secondsLeft() + "s left", x + 4, y + 10, Ui.TEXT, false);
				g.drawString(font, "Close this menu and play normally.", x + 4, y + 24, Ui.MUTED, false);
				if (button(g, mx, my, x + 4, y + 42, 80, 18, "Stop", false) && clickL) action = PerfCheck::cancel;
				return;
			}
			if (r == null) {
				Ui.cutBox(g, x, y + 2, w, 112, 6, 0x9A1C1C1F, 0xFF2E2E33);
				Ui.item(g, new ItemStack(Items.CLOCK), x + 12, y + 14, 16);
				g.drawString(font, "Find out why your game lags", x + 36, y + 18, Ui.TEXT, false);
				int ly = y + 38;
				for (String l : lines("Surge watches your game for " + PerfCheck.SECONDS + " seconds while you play. Then it tells you what causes low FPS and stutters, like RAM, chunk loading, entities or heavy settings, and fixes most of it with one click.", w - 24)) {
					g.drawString(font, l, x + 12, ly, Ui.MUTED, false);
					ly += 10;
				}
				boolean inWorld = minecraft.level != null;
				if (button(g, mx, my, x + 12, y + 88, 120, 18, inWorld ? "Start the check" : "Join a world first", inWorld) && clickL && inWorld) action = startAction();
				g.drawString(font, "Walking around helps", x + 140, y + 93, Ui.FAINT, false);
				return;
			}
			int bw = (w - 8) / 3;
			String[][] stats = {{String.valueOf(Math.round(r.avgFps())), "average FPS"}, {String.valueOf(Math.round(r.lowFps())), "1% low FPS"}, {String.valueOf(r.stutters()), r.stutters() == 1 ? "stutter" : "stutters"}};
			int[] colors = {Ui.TEXT, r.lowFps() < 30 ? 0xFFFF8C8C : r.lowFps() < 55 ? 0xFFFFC56B : 0xFF8CFFA0, r.stutters() > 10 ? 0xFFFF8C8C : r.stutters() > 3 ? 0xFFFFC56B : 0xFF8CFFA0};
			for (int i = 0; i < 3; i++) {
				int bx = x + i * (bw + 4);
				Ui.cutBox(g, bx, y + 2, bw, 40, 5, 0x9A1C1C1F, 0xFF2E2E33);
				g.pose().pushMatrix();
				g.pose().translate(bx + 8, y + 8);
				g.pose().scale(2f, 2f);
				g.drawString(font, Ui.ten(stats[i][0]), 0, 0, colors[i], false);
				g.pose().popMatrix();
				g.drawString(font, stats[i][1], bx + 8, y + 28, Ui.FAINT, false);
			}
			int gy = y + 50, gh = 56;
			Ui.cutBox(g, x, gy, w, gh + 18, 5, 0xFF161619, 0xFF2A2A2E);
			float[] f = r.frames();
			float top = Math.max(40f, r.spikeMs() * 2f);
			for (float v : f) top = Math.max(top, Math.min(v, 150f));
			int gx0 = x + 6, gw = w - 12, base = gy + gh;
			int ref = base - Math.round(16.7f / top * (gh - 6));
			for (int i = gx0; i < gx0 + gw; i += 3) g.fill(i, ref, i + 1, ref + 1, 0xFF3A3A40);
			g.drawString(font, "60", x + w - 18, ref - 9, 0xFF55555C, false);
			int nb = Math.max(1, gw);
			for (int b = 0; b < nb && f.length > 0; b++) {
				int from = (int) ((long) b * f.length / nb), to = Math.max(from + 1, (int) ((long) (b + 1) * f.length / nb));
				float mxv = 0;
				boolean gcHit = false, mvHit = false;
				for (int k = from; k < to && k < f.length; k++) {
					if (f[k] > mxv) { mxv = f[k]; gcHit = r.gc()[k]; mvHit = r.moving()[k]; }
				}
				int bh = Math.max(1, Math.round(Math.min(mxv, top) / top * (gh - 6)));
				boolean spike = mxv >= r.spikeMs();
				int col = !spike ? 0xFF55555C : gcHit ? 0xFFFFB347 : mvHit ? 0xFF6CB4FF : 0xFFFF8C8C;
				g.fill(gx0 + b, base - bh, gx0 + b + 1, base, col);
			}
			int lx = x + 8, lyy = base + 6;
			int[][] legend = {{0xFFFFB347}, {0xFF6CB4FF}, {0xFFFF8C8C}};
			String[] names = {"memory cleanup", "chunk loading", "other"};
			for (int i = 0; i < 3; i++) {
				g.fill(lx, lyy + 2, lx + 5, lyy + 7, legend[i][0]);
				g.drawString(font, names[i], lx + 8, lyy, Ui.FAINT, false);
				lx += font.width(names[i]) + 18;
			}
			String worst = "worst " + Math.round(r.worstMs()) + " ms";
			g.drawString(font, worst, x + w - 8 - font.width(worst), lyy, Ui.FAINT, false);

			int fy = gy + gh + 26;
			g.drawString(font, Ui.ten("What we found"), x + 2, fy, Ui.TEXT, false);
			fy += 14;
			for (PerfCheck.Finding fd : r.findings()) {
				int fh = findingH(fd, w);
				int sev = switch (fd.severity) { case 2 -> 0xFFFF6B6B; case 1 -> 0xFFFFB347; case -1 -> 0xFF6EE07A; default -> 0xFF6E6F74; };
				Ui.cutBox(g, x, fy, w, fh, 4, 0x9A1C1C1F, 0xFF2A2A2E);
				g.fill(x + 1, fy + 4, x + 3, fy + fh - 4, sev);
				g.drawString(font, Ui.fit(font, fd.title, w - 110), x + 10, fy + 6, Ui.TEXT, false);
				int ly = fy + 18;
				for (String l : lines(fd.detail, w - 20 - (fd.fixLabel != null ? 84 : 0))) { g.drawString(font, l, x + 10, ly, Ui.MUTED, false); ly += 10; }
				if (fd.fixLabel != null) {
					int bx = x + w - 84;
					if (fd.fixed) g.drawString(font, "Done", bx + 40 - font.width("Done") / 2, fy + 8, 0xFF8CFFA0, false);
					else if (button(g, mx, my, bx, fy + 4, 78, 16, fd.fixLabel, true) && clickL) action = () -> { fd.fix.run(); fd.fixed = true; };
				}
				fy += fh + 4;
			}
			boolean inWorld = minecraft.level != null;
			if (button(g, mx, my, x, fy + 4, 100, 18, "Run again", false) && clickL && inWorld) action = startAction();
			long mins = (System.currentTimeMillis() - r.at()) / 60000;
			g.drawString(font, mins < 1 ? "Checked just now" : "Checked " + mins + " min ago", x + 108, fy + 9, Ui.FAINT, false);
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

	private final class NewProfileRow extends Row {
		final EditBox box;

		NewProfileRow() {
			box = new EditBox(font, 0, 0, 100, 10, Component.literal("Name"));
			box.setBordered(false);
			box.setMaxLength(32);
			box.setHint(Component.literal("Name, like PvP or Building").withStyle(ChatFormatting.DARK_GRAY));
			addWidget(box);
			boxes.add(box);
		}

		@Override int height() { return 26; }

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			int bw = 120;
			Ui.box(g, x, y + 4, w - bw - 6, 18, 0xFF161619, box.isFocused() ? Ui.ACCENT_2 : Ui.EDGE);
			box.setX(x + 6);
			box.setY(y + 9);
			box.setWidth(w - bw - 18);
			box.visible = true;
			box.render(g, mx, my, delta);
			boolean enter = box.isFocused() && GLFW.glfwGetKey(minecraft.getWindow().handle(), GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;
			if ((Ui.pill(g, font, mx, my, x + w - bw, y + 4, bw, 18, "Save current setup", true) && clickL) || enter) action = () -> {
				if (gg.surge.client.features.Profiles.saveAs(box.getValue())) { box.setValue(""); needsRebuild = true; }
			};
		}
	}

	private final class ProfileRow extends Row {
		final String name;
		final EditBox box;
		boolean renaming;
		long confirmDelete;

		ProfileRow(String name) {
			this.name = name;
			box = new EditBox(font, 0, 0, 100, 10, Component.literal("Name"));
			box.setBordered(false);
			box.setMaxLength(32);
			box.setValue(name);
			addWidget(box);
			boxes.add(box);
		}

		@Override int height() { return 26; }

		@Override void draw(GuiGraphics g, int x, int y, int w, int mx, int my, boolean hov, float delta) {
			boolean active = name.equals(gg.surge.client.features.Profiles.active());
			float a = Ui.anim(this, hov);
			Ui.cutBox(g, x, y + 2, w, 22, 3, Colors.lerp(0x9A1C1C1F, 0xC0242428, a), active ? Ui.ACCENT_2 : 0xFF2A2A2E);
			int bx = x + w - 4;
			String[] labels = renaming ? new String[]{"Cancel", "Done"} : new String[]{System.currentTimeMillis() - confirmDelete < 3000 ? "Sure?" : "Delete", "Rename", "Save", active ? "Loaded" : "Load"};
			int[] widths = new int[labels.length];
			for (int i = 0; i < labels.length; i++) widths[i] = font.width(labels[i]) + 12;
			for (int i = 0; i < labels.length; i++) {
				bx -= widths[i];
				boolean primary = i == labels.length - 1 && !active;
				boolean hit = Ui.pill(g, font, mx, my, bx, y + 5, widths[i], 16, labels[i], primary) && clickL;
				String lab = labels[i];
				if (hit) action = () -> {
					switch (lab) {
						case "Load" -> gg.surge.client.features.Profiles.load(name);
						case "Save" -> gg.surge.client.features.Profiles.saveAs(name);
						case "Rename" -> { renaming = true; box.setValue(name); box.setFocused(true); setFocused(box); }
						case "Cancel" -> renaming = false;
						case "Done" -> { if (gg.surge.client.features.Profiles.rename(name, box.getValue())) needsRebuild = true; renaming = false; }
						case "Delete" -> confirmDelete = System.currentTimeMillis();
						case "Sure?" -> { gg.surge.client.features.Profiles.delete(name); needsRebuild = true; }
						default -> { }
					}
				};
				bx -= 4;
			}
			if (renaming) {
				Ui.box(g, x + 6, y + 5, bx - x - 10, 16, 0xFF161619, Ui.ACCENT_2);
				box.setX(x + 11);
				box.setY(y + 9);
				box.setWidth(bx - x - 20);
				box.visible = true;
				box.render(g, mx, my, delta);
			} else {
				box.visible = false;
				g.drawString(font, Ui.fit(font, name, bx - x - 20), x + 10, y + 9, Ui.TEXT, false);
			}
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
