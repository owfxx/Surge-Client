package gg.surge.client.gui;

import gg.surge.client.features.CustomSounds;
import gg.surge.client.mods.Colors;
import gg.surge.client.mods.Mods;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomSoundsScreen extends Screen {
	private enum Filter { POPULAR, ALL, CHANGED }

	private static final int ROW = 15;
	private static final int ACCENT = 0xFFF2C430;

	private final Screen parent;
	private final List<String> events = new ArrayList<>();
	private List<String> shown = new ArrayList<>();
	private List<Path> files = new ArrayList<>();
	private List<Path> shownFiles = new ArrayList<>();
	private EditBox search, fileSearch;
	private Filter filter = Filter.POPULAR;
	private String selected;
	private String lastQuery = "\u0000", lastFileQuery = "\u0000";
	private Filter lastFilter;
	private double scroll, fileScroll;
	private boolean wasDown = true;
	private long openedAt;
	private int px, py, pw, ph, listX, listY, listW, listH, rightX, rightW, filesY, filesH;
	private volatile String picked;

	public CustomSoundsScreen(Screen parent) {
		super(Component.literal("Custom Sounds"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		if (openedAt == 0) openedAt = System.currentTimeMillis();
		if (events.isEmpty()) {
			for (var id : minecraft.getSoundManager().getAvailableSounds()) events.add(id.toString());
			events.sort(String::compareTo);
			if (selected == null) selected = CustomSounds.POPULAR.get(0);
		}
		files = CustomSounds.userFiles();
		pw = Math.min(600, this.width - 16);
		ph = Math.min(340, this.height - 16);
		px = (this.width - pw) / 2;
		py = (this.height - ph) / 2;
		listX = px + 10;
		listW = Math.max(150, pw * 42 / 100);
		rightX = listX + listW + 10;
		rightW = px + pw - 10 - rightX;
		String keep = search == null ? "" : search.getValue();
		String keepF = fileSearch == null ? "" : fileSearch.getValue();
		search = new EditBox(font, listX + 18, py + 39, listW - 24, 10, Component.literal("Search sounds"));
		search.setBordered(false);
		search.setMaxLength(64);
		search.setValue(keep);
		search.setHint(Component.literal("Search game sounds").withStyle(ChatFormatting.DARK_GRAY));
		addRenderableWidget(search);
		fileSearch = new EditBox(font, 0, 0, 100, 10, Component.literal("Search your sounds"));
		fileSearch.setBordered(false);
		fileSearch.setMaxLength(64);
		fileSearch.setValue(keepF);
		fileSearch.setHint(Component.literal("Search your files").withStyle(ChatFormatting.DARK_GRAY));
		addRenderableWidget(fileSearch);
		setInitialFocus(search);
		lastQuery = "\u0000";
		lastFileQuery = "\u0000";
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (parent != null && minecraft.level == null) parent.renderBackground(g, mouseX, mouseY, delta);
		if (Ui.glassBackdrop(g, () -> renderBlurredBackground(g), this.width, this.height)) return;
		g.fill(0, 0, this.width, this.height, Colors.alpha(0xB0000000, Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f)));
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	static String pretty(String id) {
		String p = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
		String ns = id.contains(":") ? id.substring(0, id.indexOf(':')) : "minecraft";
		int dot = p.indexOf('.');
		String rest = dot > 0 && dot < p.length() - 1 ? p.substring(dot + 1) : p;
		rest = rest.replace('.', ' ').replace('_', ' ');
		String out = Character.toUpperCase(rest.charAt(0)) + rest.substring(1);
		return ns.equals("minecraft") ? out : out + " (" + ns + ")";
	}

	static String group(String id) {
		String p = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
		int dot = p.indexOf('.');
		return dot > 0 ? p.substring(0, dot) : "other";
	}

	private void refilter() {
		String q = search.getValue().trim().toLowerCase(Locale.ROOT);
		if (q.equals(lastQuery) && filter == lastFilter) return;
		lastQuery = q;
		lastFilter = filter;
		List<String> base;
		if (!q.isEmpty()) base = events;
		else base = switch (filter) {
			case POPULAR -> CustomSounds.POPULAR.stream().map(s -> "minecraft:" + s).filter(events::contains).toList();
			case CHANGED -> new ArrayList<>(CustomSounds.all().keySet());
			default -> events;
		};
		List<String> out = new ArrayList<>();
		for (String e : base) {
			if (filter == Filter.CHANGED && CustomSounds.get(e) == null) continue;
			if (q.isEmpty() || e.contains(q) || pretty(e).toLowerCase(Locale.ROOT).contains(q)) out.add(e);
		}
		shown = out;
		scroll = 0;
	}

	private void refilterFiles() {
		String q = fileSearch.getValue().trim().toLowerCase(Locale.ROOT);
		if (q.equals(lastFileQuery)) return;
		lastFileQuery = q;
		List<Path> out = new ArrayList<>();
		for (Path p : files) if (q.isEmpty() || p.getFileName().toString().toLowerCase(Locale.ROOT).contains(q)) out.add(p);
		shownFiles = out;
		fileScroll = 0;
	}

	private String sel() {
		if (selected == null) return null;
		return selected.contains(":") ? selected : "minecraft:" + selected;
	}

	private void use(String source) {
		String e = sel();
		if (e == null) return;
		CustomSounds.set(e, source);
		if (source != null) CustomSounds.preview(source);
		else CustomSounds.playOriginal(e);
		lastQuery = "\u0000";
	}

	private void browse() {
		java.util.concurrent.CompletableFuture.runAsync(() -> {
			String path;
			try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
				org.lwjgl.PointerBuffer f = stack.mallocPointer(3);
				f.put(stack.UTF8("*.mp3")).put(stack.UTF8("*.wav")).put(stack.UTF8("*.ogg"));
				f.flip();
				path = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Pick a sound", null, f, "Sounds", false);
			} catch (Throwable t) {
				path = null;
			}
			if (path != null && CustomSounds.playable(path)) picked = path;
		});
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		if (Ui.hover((int) mx, (int) my, listX, listY, listW, listH)) {
			scroll = Math.max(0, Math.min(Math.max(0, shown.size() * ROW - listH), scroll - sy * ROW * 2));
			return true;
		}
		if (Ui.hover((int) mx, (int) my, rightX, filesY, rightW, filesH)) {
			fileScroll = Math.max(0, Math.min(Math.max(0, shownFiles.size() * 13 - filesH), fileScroll - sy * 13 * 2));
			return true;
		}
		return super.mouseScrolled(mx, my, sx, sy);
	}

	private boolean button(GuiGraphics g, int mx, int my, boolean click, int x, int y, int w, String label, boolean on) {
		boolean hov = Ui.hover(mx, my, x, y, w, 16);
		Ui.cutBox(g, x, y, w, 16, 2, on ? 0xFF3A3420 : hov ? Ui.SURFACE_HOVER : Ui.SURFACE, on ? ACCENT : Ui.EDGE);
		g.drawCenteredString(font, label, x + w / 2, y + 4, on ? Ui.YELLOW : Ui.TEXT);
		return hov && click;
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		renderBackground(g, mx, my, delta);
		Ui.frame();
		if (picked != null) {
			String p = picked;
			picked = null;
			CustomSounds.forget(p);
			use(p);
		}
		refilter();
		refilterFiles();
		boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasDown;
		wasDown = down;
		Runnable action = null;

		Ui.shadow(g, px, py, pw, ph);
		Ui.box(g, px, py, pw, ph, Ui.BG, Ui.EDGE);
		g.fill(px + 1, py + 1, px + pw - 1, py + 3, ACCENT);
		Ui.logo(g, px + 10, py + 9, 16);
		g.drawString(font, Component.literal("Custom Sounds").withStyle(ChatFormatting.BOLD), px + 31, py + 10, Ui.TEXT, true);
		g.drawString(font, "Pick a game sound, then choose what plays instead", px + 31, py + 21, Ui.MUTED, false);

		boolean on = Mods.CUSTOM_SOUNDS.enabled();
		String st = on ? "On" : "Off";
		int tw = 44;
		if (button(g, mx, my, click, px + pw - 10 - 18 - 6 - tw, py + 10, tw, st, on)) action = () -> Mods.CUSTOM_SOUNDS.setEnabled(!on);
		if (button(g, mx, my, click, px + pw - 10 - 18, py + 10, 18, "x", false)) action = this::onClose;

		Ui.cutBox(g, listX, py + 35, listW, 17, 2, 0xFF1A1A1D, 0xFF34343A);
		g.drawString(font, "⌕", listX + 6, py + 39, Ui.FAINT, false);
		search.setX(listX + 18);
		search.setY(py + 39);
		search.setWidth(listW - 24);

		int fy = py + 56;
		int changed = CustomSounds.all().size();
		String[] names = {"Popular", "All", "Changed " + changed};
		Filter[] fs = Filter.values();
		int fx = listX;
		for (int i = 0; i < 3; i++) {
			int w = font.width(names[i]) + 14;
			Filter f = fs[i];
			if (button(g, mx, my, click, fx, fy, w, names[i], filter == f && search.getValue().isBlank())) action = () -> { filter = f; search.setValue(""); };
			fx += w + 4;
		}

		listY = fy + 21;
		listH = py + ph - 10 - listY;
		Ui.cutBox(g, listX, listY - 1, listW, listH + 2, 2, 0xFF1A1A1D, 0xFF2A2A2E);
		g.enableScissor(listX + 1, listY, listX + listW - 1, listY + listH);
		int first = (int) (scroll / ROW);
		String self = sel();
		for (int i = first; i < shown.size(); i++) {
			int y = listY + i * ROW - (int) scroll;
			if (y > listY + listH) break;
			String e = shown.get(i);
			boolean hov = Ui.hover(mx, my, listX, Math.max(y, listY), listW, Math.min(ROW, listY + listH - y)) && my < listY + listH && my >= listY;
			boolean isSel = e.equals(self);
			if (isSel) g.fill(listX + 1, y, listX + listW - 1, y + ROW, 0xFF34301E);
			else if (hov) g.fill(listX + 1, y, listX + listW - 1, y + ROW, 0xFF26262A);
			if (isSel) g.fill(listX + 1, y, listX + 3, y + ROW, ACCENT);
			String rep = CustomSounds.get(e);
			int right = listX + listW - 6;
			if (rep != null) {
				String lab = Ui.fit(font, CustomSounds.label(rep), listW / 3);
				int lw = font.width(lab);
				g.drawString(font, lab, right - lw, y + 4, Ui.YELLOW, false);
				right -= lw + 6;
			}
			g.drawString(font, Ui.fit(font, pretty(e), right - listX - 10), listX + 7, y + 4, isSel ? Ui.TEXT : Ui.MUTED, false);
			if (hov && click) action = () -> { selected = e; CustomSounds.playOriginal(e); };
		}
		if (shown.isEmpty()) g.drawCenteredString(font, filter == Filter.CHANGED && search.getValue().isBlank() ? "Nothing changed yet" : "No sounds match", listX + listW / 2, listY + 10, Ui.FAINT);
		g.disableScissor();
		if (shown.size() * ROW > listH) {
			int bar = Math.max(12, listH * listH / (shown.size() * ROW));
			int by = listY + (int) ((listH - bar) * (scroll / Math.max(1, shown.size() * ROW - listH)));
			g.fill(listX + listW - 3, by, listX + listW - 1, by + bar, 0x80FFFFFF);
		}

		int ry = py + 35;
		Ui.cutBox(g, rightX, ry, rightW, 44, 3, 0xFF1E1E21, 0xFF34343A);
		if (self != null) {
			g.drawString(font, Ui.fit(font, pretty(self), rightW - 12), rightX + 7, ry + 6, Ui.TEXT, true);
			g.drawString(font, Ui.fit(font, self, rightW - 12), rightX + 7, ry + 17, Ui.FAINT, false);
			String cur = CustomSounds.get(self);
			g.drawString(font, "Plays: ", rightX + 7, ry + 29, Ui.MUTED, false);
			g.drawString(font, Ui.fit(font, CustomSounds.label(cur), rightW - 150), rightX + 7 + font.width("Plays: "), ry + 29, cur == null ? Ui.TEXT : Ui.YELLOW, false);
			int bx = rightX + rightW - 7 - 56;
			if (button(g, mx, my, click, bx, ry + 25, 56, "Original", false)) action = () -> CustomSounds.playOriginal(self);
			if (cur != null && button(g, mx, my, click, bx - 60, ry + 25, 56, "Reset", false)) action = () -> use(null);
		}

		int sy = ry + 52;
		g.drawString(font, "Built in", rightX, sy, Ui.MUTED, false);
		sy += 12;
		int cols = Math.max(3, rightW / 62);
		int cw = (rightW - (cols - 1) * 4) / cols;
		String cur = self == null ? null : CustomSounds.get(self);
		for (int i = 0; i < CustomSounds.PRESETS.size(); i++) {
			String pr = CustomSounds.PRESETS.get(i);
			int x = rightX + (i % cols) * (cw + 4), y = sy + (i / cols) * 20;
			String src = "preset:" + pr;
			if (button(g, mx, my, click, x, y, cw, CustomSounds.label(src), src.equals(cur))) action = () -> use(src);
		}
		sy += ((CustomSounds.PRESETS.size() + cols - 1) / cols) * 20 + 6;

		g.drawString(font, "Your sounds", rightX, sy + 4, Ui.MUTED, false);
		int bw = 54;
		if (button(g, mx, my, click, rightX + rightW - bw, sy, bw, "Browse", false)) action = this::browse;
		if (button(g, mx, my, click, rightX + rightW - bw * 2 - 4, sy, bw, "Folder", false)) action = () -> { try { net.minecraft.util.Util.getPlatform().openPath(CustomSounds.folder()); } catch (Exception ignored) { } };
		if (button(g, mx, my, click, rightX + rightW - bw * 3 - 8, sy, bw, "Refresh", false)) action = () -> { files = CustomSounds.userFiles(); lastFileQuery = "\u0000"; };
		sy += 20;
		Ui.cutBox(g, rightX, sy, rightW, 15, 2, 0xFF1A1A1D, 0xFF34343A);
		g.drawString(font, "⌕", rightX + 5, sy + 4, Ui.FAINT, false);
		fileSearch.setX(rightX + 16);
		fileSearch.setY(sy + 3);
		fileSearch.setWidth(rightW - 22);
		sy += 18;
		filesY = sy;
		filesH = py + ph - 10 - sy;
		Ui.cutBox(g, rightX, filesY - 1, rightW, filesH + 2, 2, 0xFF1A1A1D, 0xFF2A2A2E);
		g.enableScissor(rightX + 1, filesY, rightX + rightW - 1, filesY + filesH);
		if (shownFiles.isEmpty()) {
			List<String> lines = files.isEmpty()
				? List.of("Put mp3, wav or ogg files in your sounds folder,", "or press Browse to pick one from anywhere")
				: List.of("No files match");
			for (int i = 0; i < lines.size(); i++) g.drawCenteredString(font, lines.get(i), rightX + rightW / 2, filesY + 8 + i * 11, Ui.FAINT);
		}
		for (int i = (int) (fileScroll / 13); i < shownFiles.size(); i++) {
			int y = filesY + i * 13 - (int) fileScroll;
			if (y > filesY + filesH) break;
			Path p = shownFiles.get(i);
			String src = p.toAbsolutePath().toString();
			boolean hov = Ui.hover(mx, my, rightX, y, rightW, 13) && my >= filesY && my < filesY + filesH;
			boolean isCur = src.equals(cur);
			if (isCur) g.fill(rightX + 1, y, rightX + rightW - 1, y + 13, 0xFF34301E);
			else if (hov) g.fill(rightX + 1, y, rightX + rightW - 1, y + 13, 0xFF26262A);
			g.drawString(font, "♪", rightX + 5, y + 3, isCur ? Ui.YELLOW : Ui.FAINT, false);
			g.drawString(font, Ui.fit(font, p.getFileName().toString(), rightW - 22), rightX + 15, y + 3, isCur ? Ui.YELLOW : Ui.MUTED, false);
			if (hov && click) action = () -> use(src);
		}
		g.disableScissor();

		super.render(g, mx, my, delta);
		if (action != null) action.run();
	}
}
