package gg.surge.client.gui;

import gg.surge.client.mods.Colors;
import gg.surge.client.mods.Mods;
import gg.surge.client.net.Social;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;
import org.lwjgl.glfw.GLFW;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SocialScreen extends Screen {
	private enum Tab { CHAT, FRIENDS, REQUESTS }

	private static Tab tab = Tab.CHAT;
	private static String selected;

	private final Screen parent;
	private EditBox input, addBox, searchBox;
	private boolean searching, adding;
	private int m, top;
	private boolean wasL = true, wasR = true, wasEnter;
	private String note = "";
	private int noteColor = Ui.MUTED;
	private long noteAt;
	private float scroll, scrollTarget, listScroll, listScrollTarget;
	private int contentH, listH;
	private long openedAt;
	private int menuX, menuY;
	private List<String> menuLabels;
	private List<Runnable> menuActions;

	public SocialScreen(Screen parent) {
		super(Component.literal("Social"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		if (openedAt == 0) openedAt = System.currentTimeMillis();
		m = this.width < 480 ? 6 : 12;
		top = m + 26;
		clearWidgets();
		input = box("Message", 500);
		addBox = box("Minecraft name", 16);
		searchBox = box("Search", 16);
		Social.screenOpen = true;
		Social.refresh();
	}

	private EditBox box(String hint, int max) {
		EditBox b = new EditBox(font, 0, 0, 100, 10, Component.literal(hint));
		b.setBordered(false);
		b.setMaxLength(max);
		b.setHint(Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY));
		b.visible = false;
		addWidget(b);
		return b;
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		float p = Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f);
		g.fill(0, 0, this.width, this.height, Colors.alpha(0xE8141416, p));
		g.fillGradient(0, this.height / 2, this.width, this.height, 0x00000000, Colors.alpha(0x60000000, p));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		if (menuLabels != null) { menuLabels = null; return false; }
		return !(getFocused() instanceof EditBox e && !e.getValue().isEmpty());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY == 0) return false;
		if (tab == Tab.CHAT && mouseX < m + railW()) listScrollTarget = Math.max(0, Math.min(Math.max(0, listH), listScrollTarget - (float) scrollY * 24f));
		else scrollTarget = Math.max(0, Math.min(Math.max(0, contentH), scrollTarget + (float) scrollY * 24f));
		return true;
	}

	private void note(String s, int color) {
		note = s;
		noteColor = color;
		noteAt = System.currentTimeMillis();
	}

	private void openMenu(int x, int y, List<String> labels, List<Runnable> actions) {
		int w = 60;
		for (String l : labels) w = Math.max(w, font.width(l) + 16);
		menuX = Math.min(x, this.width - w - 4);
		menuY = Math.min(y, this.height - labels.size() * 16 - 8);
		menuLabels = labels;
		menuActions = actions;
	}

	private int menuWidth() {
		int w = 60;
		for (String l : menuLabels) w = Math.max(w, font.width(l) + 16);
		return w;
	}

	private void friendMenu(int x, int y, Social.Friend f) {
		boolean isMuted = Social.muted.contains(f.uuid());
		List<String> l = new ArrayList<>();
		List<Runnable> a = new ArrayList<>();
		l.add("Message"); a.add(() -> { tab = Tab.CHAT; selected = f.uuid(); scroll = scrollTarget = 0; });
		if (joinable(f)) { l.add("Join their game"); a.add(() -> join(f)); }
		l.add(isMuted ? "Unmute" : "Mute"); a.add(() -> { if (isMuted) Social.unmute(f.uuid()); else Social.mute(f.uuid()); note(isMuted ? "Unmuted " + f.name() : "Muted " + f.name(), Ui.MUTED); });
		l.add("Remove friend"); a.add(() -> { Social.remove(f.uuid()); if (f.uuid().equals(selected)) selected = null; note("Removed " + f.name(), Ui.MUTED); });
		l.add("Block"); a.add(() -> { Social.block(f.uuid()); if (f.uuid().equals(selected)) selected = null; note("Blocked " + f.name(), 0xFFFF8C8C); });
		openMenu(x, y, l, a);
	}

	private static boolean joinable(Social.Friend f) {
		return !"offline".equals(f.status()) && !f.server().isEmpty() && !f.server().equals("Singleplayer") && ServerAddress.isValidAddress(f.server());
	}

	private void join(Social.Friend f) {
		String ip = f.server();
		if (!ServerAddress.isValidAddress(ip)) { note("Can't join that server", 0xFFFF8C8C); return; }
		ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(ip), new ServerData(f.name() + "'s server", ip, ServerData.Type.OTHER), false, null);
	}

	private static UUID uuid(String raw) {
		try {
			String s = raw.replace("-", "");
			if (s.length() != 32) return null;
			return UUID.fromString(s.substring(0, 8) + "-" + s.substring(8, 12) + "-" + s.substring(12, 16) + "-" + s.substring(16, 20) + "-" + s.substring(20));
		} catch (Exception e) {
			return null;
		}
	}

	private void face(GuiGraphics g, String rawUuid, int x, int y, int size) {
		UUID id = uuid(rawUuid);
		PlayerSkin skin = null;
		if (id != null && minecraft.getConnection() != null) {
			PlayerInfo info = minecraft.getConnection().getPlayerInfo(id);
			if (info != null) skin = info.getSkin();
		}
		if (skin == null) skin = id != null ? DefaultPlayerSkin.get(id) : DefaultPlayerSkin.getDefaultSkin();
		PlayerFaceRenderer.draw(g, skin, x, y, size);
	}

	private static String when(long ts) {
		Calendar now = Calendar.getInstance(), c = Calendar.getInstance();
		c.setTimeInMillis(ts);
		if (now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)) return new SimpleDateFormat("HH:mm").format(new Date(ts));
		now.add(Calendar.DAY_OF_YEAR, -1);
		if (now.get(Calendar.YEAR) == c.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == c.get(Calendar.DAY_OF_YEAR)) return "Yesterday";
		return new SimpleDateFormat("MMM d").format(new Date(ts));
	}

	private boolean iconButton(GuiGraphics g, int mx, int my, int x, int y, String[] icon, boolean on) {
		boolean hov = Ui.hover(mx, my, x, y, 16, 16);
		Ui.cutBox(g, x, y, 16, 16, 3, on ? 0xFF3A3A40 : hov ? 0xFF303035 : 0xFF202023, on ? Ui.ACCENT_2 : 0xFF34343A);
		Ui.icon(g, icon, x + 4, y + 4, 1, Ui.TEXT);
		return hov;
	}

	private static final String[] ADD = {".XX.....", "XXXX..X.", "XXXX.XXX", ".XX...X.", "XXXX....", "XXXX....", "........", "........"};
	private static final String[] MENU = {"........", "XXXXXXX.", "........", "XXXXXXX.", "........", "XXXXXXX.", "........", "........"};

	private int railW() {
		return Math.max(108, Math.min(170, Math.round(this.width * 0.24f)));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		Ui.frame();
		long win = minecraft.getWindow().handle();
		boolean l = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean r = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
		boolean click = l && !wasL, rclick = r && !wasR;
		wasL = l;
		wasR = r;
		boolean enter = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;
		boolean enterPressed = enter && !wasEnter;
		wasEnter = enter;
		Runnable action = null;

		if (menuLabels != null && (click || rclick)) {
			int idx = -1, w = menuWidth();
			for (int i = 0; i < menuLabels.size(); i++) if (Ui.hover(mx, my, menuX, menuY + 3 + i * 16, w, 16)) idx = i;
			Runnable run = idx >= 0 ? menuActions.get(idx) : null;
			menuLabels = null;
			if (run != null) { Ui.click(); run.run(); }
			click = rclick = false;
		}
		if (click) {
			for (EditBox b : new EditBox[]{input, addBox, searchBox}) {
				boolean over = b.visible && Ui.hover(mx, my, b.getX() - 6, b.getY() - 6, b.getWidth() + 12, 20);
				b.setFocused(over);
				if (over) setFocused(b);
			}
		}
		input.visible = addBox.visible = searchBox.visible = false;

		action = topBar(g, mx, my, click, action);
		if (!Social.available()) {
			List<String> lines = wrap(Social.unavailableReason(), Math.min(320, this.width - 40));
			for (int i = 0; i < lines.size(); i++) Ui.centered(g, font, lines.get(i), this.width / 2, this.height / 2 - 10 + i * 11, Ui.MUTED);
		} else action = switch (tab) {
			case CHAT -> chats(g, mx, my, click, rclick, enterPressed, delta, action);
			case FRIENDS -> roster(g, mx, my, click, rclick, delta, action);
			case REQUESTS -> requests(g, mx, my, click, enterPressed, delta, action);
		};

		if (!note.isEmpty() && System.currentTimeMillis() - noteAt < 4000) {
			long age = System.currentTimeMillis() - noteAt;
			float a = Math.min(1f, Math.min(age / 150f, (4000 - age) / 400f));
			g.nextStratum();
			String n = Ui.fit(font, note, this.width - 60);
			int nw = font.width(n) + 16, nx = (this.width - nw) / 2, ny = this.height - m - 22 + Math.round((1 - a) * 6);
			Ui.cutBox(g, nx, ny, nw, 17, 3, Colors.alpha(0xF5101012, a), Colors.alpha(noteColor, a * 0.8f));
			g.drawString(font, n, nx + 8, ny + 5, Colors.alpha(noteColor, a), false);
		}
		if (menuLabels != null) {
			g.nextStratum();
			int w = menuWidth(), h = menuLabels.size() * 16 + 6;
			Ui.cutBox(g, menuX, menuY, w, h, 3, 0xFA1B1B1E, 0xFF45454C);
			for (int i = 0; i < menuLabels.size(); i++) {
				int y = menuY + 3 + i * 16;
				if (Ui.hover(mx, my, menuX, y, w, 16)) { g.fill(menuX + 2, y, menuX + w - 2, y + 16, 0xFF2E2E33); g.fill(menuX + 2, y, menuX + 3, y + 16, Ui.ACCENT_2); }
				String lab = menuLabels.get(i);
				g.drawString(font, lab, menuX + 8, y + 4, lab.equals("Block") || lab.equals("Remove friend") ? 0xFFFF8C8C : Ui.TEXT, false);
			}
		}
		if (action != null) { Ui.click(); action.run(); }
	}

	private static final String[] BACK = {"....X...", "...XX...", "..XXX...", ".XXXX...", "..XXX...", "...XX...", "....X...", "........"};

	private Runnable topBar(GuiGraphics g, int mx, int my, boolean click, Runnable action) {
		int y = m;
		boolean bh = Ui.hover(mx, my, m, y, 18, 18);
		Ui.cutBox(g, m, y, 18, 18, 3, bh ? 0xFF34343A : 0xFF232327, 0xFF3A3A40);
		Ui.icon(g, BACK, m + 5, y + 5, 1, Ui.TEXT);
		if (bh && click) action = this::onClose;
		g.drawString(font, Ui.ten("SOCIAL"), m + 26, y + 5, Ui.TEXT, true);

		String st = Social.status();
		String stLabel = Social.statusLabel(st);
		int sw = font.width(stLabel) + 22, sx = this.width - m - sw;
		boolean sh = Ui.hover(mx, my, sx, y, sw, 18);
		Ui.cutBox(g, sx, y, sw, 18, 3, sh ? 0xFF303035 : 0xFF232327, 0xFF3A3A40);
		statusDot(g, sx + 7, y + 7, st.equals("invisible") ? "offline" : st);
		g.drawString(font, stLabel, sx + 16, y + 5, Ui.MUTED, false);
		if (sh && click) {
			List<Runnable> as = new ArrayList<>();
			for (String v : new String[]{"Online", "Idle", "Busy", "Offline"}) as.add(() -> Mods.STATUS.set(v));
			action = () -> openMenu(sx, y + 20, List.of("Online", "Idle", "Busy", "Appear offline"), as);
		}
		int ax = sx - 20;
		if (iconButton(g, mx, my, ax, y + 1, ADD, tab == Tab.REQUESTS) && click) action = () -> { tab = Tab.REQUESTS; adding = true; };

		int tx = m + 26 + font.width(Ui.ten("SOCIAL")) + 22, selX = -1, selW = 0;
		for (Tab t : Tab.values()) {
			String label = switch (t) { case CHAT -> "Chats"; case FRIENDS -> "Friends"; case REQUESTS -> "Requests"; };
			int badge = switch (t) { case CHAT -> sumUnread(); case FRIENDS -> 0; case REQUESTS -> Social.incoming.size(); };
			int w = font.width(label);
			boolean hov = Ui.hover(mx, my, tx - 3, y, w + 6, 18);
			g.drawString(font, label, tx, y + 5, t == tab ? Ui.TEXT : hov ? 0xFFD4D4D8 : Ui.FAINT, false);
			if (t == tab) { selX = tx; selW = w; }
			if (badge > 0) {
				String b = badge > 9 ? "9+" : String.valueOf(badge);
				int bx = tx + w + 3;
				g.fill(bx, y + 4, bx + font.width(b) + 5, y + 14, 0xFFE11D48);
				g.drawString(font, b, bx + 3, y + 5, 0xFFFFFFFF, false);
				w += font.width(b) + 8;
			}
			if (hov && click && t != tab) action = () -> { tab = t; listScroll = listScrollTarget = 0; scroll = scrollTarget = 0; };
			tx += w + 16;
		}
		g.fill(m, y + 21, this.width - m, y + 22, 0xFF2A2A2E);
		if (selX >= 0) {
			float ux = Ui.approach("stab:x", selX, 18f), uw = Ui.approach("stab:w", selW, 18f);
			g.fill(Math.round(ux), y + 20, Math.round(ux + uw), y + 22, Ui.ACCENT_2);
		}
		return action;
	}

	private void statusDot(GuiGraphics g, int x, int y, String status) {
		g.fill(x - 1, y - 1, x + 6, y + 6, 0xFF141416);
		g.fill(x, y, x + 5, y + 5, Social.statusColor(status));
	}

	private void ringFace(GuiGraphics g, String uuid, int x, int y, int size, String status) {
		Ui.outline(g, x - 2, y - 2, size + 4, size + 4, Social.statusColor(status));
		g.fill(x - 1, y - 1, x + size + 1, y + size + 1, 0xFF141416);
		face(g, uuid, x, y, size);
	}

	private boolean button(GuiGraphics g, int mx, int my, int x, int y, int w, int h, String label, boolean primary, int textColor) {
		boolean hov = Ui.hover(mx, my, x, y, w, h);
		float a = Ui.anim("sb:" + label + x + "," + y, hov);
		int fill = primary ? Colors.lerp(0xFFE4E4E7, 0xFFFFFFFF, a) : Colors.lerp(0xFF2A2A2E, 0xFF36363B, a);
		Ui.cutBox(g, x, y, w, h, 3, fill, primary ? 0xFFFFFFFF : 0xFF3A3A40);
		String t = Ui.fit(font, label, w - 6);
		g.drawString(font, t, x + (w - font.width(t)) / 2, y + (h - 8) / 2, primary ? 0xFF141416 : textColor, false);
		return hov;
	}

	private List<Social.Friend> filtered() {
		String q = searchBox.getValue().trim().toLowerCase();
		List<Social.Friend> fr = new ArrayList<>();
		for (Social.Friend f : Social.friends) if (q.isEmpty() || f.name().toLowerCase().contains(q)) fr.add(f);
		return fr;
	}

	private int searchField(GuiGraphics g, int mx, int my, int x, int y, int w, float delta) {
		Ui.cutBox(g, x, y, w, 16, 3, 0xFF141416, searchBox.isFocused() ? Ui.ACCENT_2 : 0xFF2E2E33);
		Ui.icon(g, Ui.SEARCH, x + 5, y + 4, 1, 0xFF55555C);
		searchBox.visible = true;
		searchBox.setX(x + 17);
		searchBox.setY(y + 4);
		searchBox.setWidth(w - 22);
		searchBox.render(g, mx, my, delta);
		return y + 20;
	}

	private Runnable chats(GuiGraphics g, int mx, int my, boolean click, boolean rclick, boolean enter, float delta, Runnable action) {
		int rw = railW(), x = m, y0 = top + 4, bottom = this.height - m;
		int listTop = searchField(g, mx, my, x, y0, rw, delta);
		List<Social.Friend> fr = filtered();
		fr.sort((a, b) -> Long.compare(lastTs(b.uuid()), lastTs(a.uuid())));
		int rowH = 26;
		listH = Math.max(0, fr.size() * rowH - (bottom - listTop));
		listScrollTarget = Math.min(listScrollTarget, listH);
		listScroll += (listScrollTarget - listScroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		if (fr.isEmpty()) {
			List<String> lines = wrap(Social.friends.isEmpty() ? "No friends yet. Add someone with the + button up top." : "Nobody found", rw - 4);
			for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x + 2, listTop + 6 + i * 10, Ui.FAINT, false);
		}
		g.enableScissor(x, listTop, x + rw, bottom);
		int y = listTop - Math.round(listScroll);
		for (Social.Friend f : fr) {
			boolean sel = f.uuid().equals(selected);
			boolean hov = Ui.hover(mx, my, x, y, rw, rowH - 2) && my >= listTop && my < bottom;
			float a = Ui.anim("sr:" + f.uuid(), hov || sel);
			if (a > 0.01f) Ui.cut(g, x, y, rw, rowH - 2, 3, Colors.alpha(sel ? 0xFF2A2A2F : 0xFF222226, a));
			if (sel) g.fill(x, y + 4, x + 2, y + rowH - 6, Ui.ACCENT_2);
			ringFace(g, f.uuid(), x + 7, y + 4, 16, f.status());
			int un = Social.UNREAD.getOrDefault(f.uuid(), 0);
			List<Social.Message> msgs = Social.CHATS.get(f.uuid());
			Social.Message last = msgs == null || msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
			g.drawString(font, Ui.fit(font, f.name(), rw - 36 - (un > 0 ? 14 : 0)), x + 29, y + 3, Ui.TEXT, false);
			String sub = last != null ? (last.from().equals(Social.myUuid) ? "You: " : "") + last.text() : !f.server().isEmpty() && !"offline".equals(f.status()) ? f.server() : Social.statusLabel(f.status());
			g.drawString(font, Ui.fit(font, sub, rw - 34), x + 29, y + 13, Ui.FAINT, false);
			if (un > 0) {
				String b = un > 9 ? "9+" : String.valueOf(un);
				g.fill(x + rw - 14, y + 3, x + rw - 4, y + 13, 0xFFE11D48);
				Ui.centered(g, font, b, x + rw - 9, y + 4, 0xFFFFFFFF);
			}
			if (hov && click) action = () -> { selected = f.uuid(); scroll = scrollTarget = 0; };
			if (hov && rclick) friendMenu(mx, my, f);
			y += rowH;
		}
		g.disableScissor();

		int cx1 = x + rw + 10;
		boolean wide = this.width - m - cx1 >= 330;
		int profW = wide ? Math.min(150, (this.width - m - cx1) / 3) : 0;
		int cx2 = this.width - m - (wide ? profW + 10 : 0);
		Social.Friend f = selected == null ? null : Social.friend(selected);
		if (f == null && !Social.friends.isEmpty()) { selected = Social.friends.get(0).uuid(); f = Social.friend(selected); }
		Social.openChat = f == null ? null : f.uuid();
		if (f == null) {
			Ui.centered(g, font, "Your chats show up here", (cx1 + this.width - m) / 2, this.height / 2, Ui.FAINT);
			return action;
		}
		Social.UNREAD.remove(f.uuid());
		action = conversation(g, mx, my, click, enter, delta, cx1, cx2, f, !wide, action);
		if (wide) action = profile(g, mx, my, click, cx2 + 10, profW, f, action);
		return action;
	}

	private Runnable conversation(GuiGraphics g, int mx, int my, boolean click, boolean enter, float delta, int x1, int x2, Social.Friend f, boolean header, Runnable action) {
		int w = x2 - x1, y0 = top + 4;
		final Social.Friend fr = f;
		int areaTop = y0;
		if (header) {
			ringFace(g, f.uuid(), x1 + 2, y0 + 2, 14, f.status());
			g.drawString(font, Ui.fit(font, f.name(), w - 100), x1 + 22, y0 + 5, Ui.TEXT, false);
			int hx = x2 - 16;
			if (iconButton(g, mx, my, hx, y0, MENU, false) && click) friendMenu(hx, y0 + 18, fr);
			if (joinable(f)) { hx -= 52; if (button(g, mx, my, hx, y0, 48, 16, "Join", true, Ui.TEXT) && click) action = () -> join(fr); }
			areaTop = y0 + 22;
		}
		int areaBottom = this.height - m - 26;
		Ui.cut(g, x1, areaTop, w, areaBottom - areaTop, 5, 0xFF18181B);
		List<Social.Message> msgs = Social.CHATS.getOrDefault(f.uuid(), List.of());
		List<Object[]> rows = new ArrayList<>();
		String lastDay = "", lastFrom = "";
		long lastTs = 0;
		SimpleDateFormat day = new SimpleDateFormat("EEE, MMM d"), clock = new SimpleDateFormat("HH:mm");
		int textW = w - 40;
		for (Social.Message msg : msgs) {
			String d = day.format(new Date(msg.ts()));
			if (!d.equals(lastDay)) { rows.add(new Object[]{"day", d}); lastDay = d; lastFrom = ""; }
			boolean mine = msg.from().equals(Social.myUuid);
			if (!msg.from().equals(lastFrom) || msg.ts() - lastTs > 5 * 60_000) rows.add(new Object[]{"head", mine ? "You" : f.name(), clock.format(new Date(msg.ts())), mine, msg.from()});
			rows.add(new Object[]{"msg", wrap(msg.text(), textW), mine});
			lastFrom = msg.from();
			lastTs = msg.ts();
		}
		int total = 8;
		for (Object[] row : rows) total += rowHeight(row);
		contentH = Math.max(0, total - (areaBottom - areaTop));
		scrollTarget = Math.min(scrollTarget, contentH);
		scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		g.enableScissor(x1, areaTop + 1, x2, areaBottom - 1);
		int yy = areaBottom - total + Math.round(scroll) + 4;
		for (Object[] row : rows) {
			int h = rowHeight(row);
			if (yy + h > areaTop && yy < areaBottom) {
				switch ((String) row[0]) {
					case "day" -> {
						String d = (String) row[1];
						int dw = font.width(d) + 10, dx = x1 + (w - dw) / 2;
						g.fill(x1 + 10, yy + 7, x2 - 10, yy + 8, 0xFF26262A);
						Ui.cutBox(g, dx, yy + 1, dw, 13, 3, 0xFF18181B, 0xFF2E2E33);
						g.drawString(font, d, dx + 5, yy + 4, Ui.FAINT, false);
					}
					case "head" -> {
						boolean mine = (Boolean) row[3];
						face(g, (String) row[4], x1 + 10, yy + 4, 12);
						g.drawString(font, (String) row[1], x1 + 28, yy + 6, mine ? Ui.ACCENT_2 : Ui.TEXT, false);
						g.drawString(font, (String) row[2], x1 + 32 + font.width((String) row[1]), yy + 6, 0xFF55555C, false);
					}
					default -> {
						@SuppressWarnings("unchecked") List<String> lines = (List<String>) row[1];
						boolean mine = (Boolean) row[2];
						g.fill(x1 + 15, yy, x1 + 16, yy + lines.size() * 10 + 1, mine ? Colors.alpha(Ui.ACCENT_2, 0.6f) : 0xFF34343A);
						for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x1 + 28, yy + 1 + i * 10, mine ? 0xFFE4E4E7 : 0xFFD0D0D4, false);
					}
				}
			}
			yy += h;
		}
		g.disableScissor();
		if (msgs.isEmpty()) {
			Ui.centered(g, font, "This is the start of your chat with " + f.name(), x1 + w / 2, (areaTop + areaBottom) / 2 - 6, Ui.MUTED);
			Ui.centered(g, font, "Messages are only kept for a while", x1 + w / 2, (areaTop + areaBottom) / 2 + 6, Ui.FAINT);
		}

		int iy = this.height - m - 20;
		Ui.cutBox(g, x1, iy, w, 20, 4, 0xFF1E1E21, input.isFocused() ? Ui.ACCENT_2 : 0xFF2E2E33);
		input.visible = true;
		input.setHint(Component.literal("Message " + f.name()).withStyle(ChatFormatting.DARK_GRAY));
		input.setX(x1 + 8);
		input.setY(iy + 6);
		input.setWidth(w - 50);
		input.render(g, mx, my, delta);
		if (!input.getValue().isBlank()) g.drawString(font, "Enter", x2 - 8 - font.width("Enter"), iy + 6, 0xFF55555C, false);
		if (enter && input.isFocused() && !input.getValue().isBlank()) {
			String text = input.getValue();
			input.setValue("");
			scrollTarget = 0;
			action = () -> Social.message(fr.uuid(), text, err -> note(err, 0xFFFF8C8C));
		}
		return action;
	}

	private Runnable profile(GuiGraphics g, int mx, int my, boolean click, int x, int w, Social.Friend f, Runnable action) {
		int y = top + 4, bottom = this.height - m;
		Ui.cutBox(g, x, y, w, bottom - y, 6, 0xFF1A1A1D, 0xFF2A2A2E);
		int fs = Math.min(40, w / 3);
		int cx = x + w / 2;
		ringFace(g, f.uuid(), cx - fs / 2, y + 14, fs, f.status());
		Ui.centered(g, font, Ui.fit(font, f.name(), w - 10), cx, y + fs + 24, Ui.TEXT);
		Ui.centered(g, font, Social.statusLabel(f.status()), cx, y + fs + 36, Social.statusColor(f.status()));
		int by = y + fs + 54;
		boolean online = !"offline".equals(f.status()) && !f.server().isEmpty();
		if (online) {
			Ui.cutBox(g, x + 8, by, w - 16, joinable(f) ? 44 : 26, 4, 0xFF222226, 0xFF2E2E33);
			g.drawString(font, "Playing on", x + 14, by + 5, Ui.FAINT, false);
			g.drawString(font, Ui.fit(font, f.server(), w - 28), x + 14, by + 15, Ui.TEXT, false);
			if (joinable(f) && button(g, mx, my, x + 14, by + 26, w - 28, 14, "Join game", true, Ui.TEXT) && click) action = () -> join(f);
			by += (joinable(f) ? 44 : 26) + 8;
		}
		boolean isMuted = Social.muted.contains(f.uuid());
		int bh = 16, bw = w - 16;
		if (by + bh * 3 + 8 < bottom) {
			if (button(g, mx, my, x + 8, by, bw, bh, isMuted ? "Unmute" : "Mute", false, Ui.TEXT) && click) action = () -> { if (isMuted) Social.unmute(f.uuid()); else Social.mute(f.uuid()); note(isMuted ? "Unmuted " + f.name() : "Muted " + f.name(), Ui.MUTED); };
			if (button(g, mx, my, x + 8, by + bh + 4, bw, bh, "Remove friend", false, 0xFFFF8C8C) && click) action = () -> { Social.remove(f.uuid()); selected = null; note("Removed " + f.name(), Ui.MUTED); };
			if (button(g, mx, my, x + 8, by + (bh + 4) * 2, bw, bh, "Block", false, 0xFFFF8C8C) && click) action = () -> { Social.block(f.uuid()); selected = null; note("Blocked " + f.name(), 0xFFFF8C8C); };
		}
		return action;
	}

	private Runnable roster(GuiGraphics g, int mx, int my, boolean click, boolean rclick, float delta, Runnable action) {
		int x = m, w = this.width - m * 2, y0 = top + 4, bottom = this.height - m;
		searchField(g, mx, my, x, y0, Math.min(180, w), delta);
		List<Social.Friend> fr = filtered();
		fr.sort((a, b) -> Boolean.compare("offline".equals(a.status()), "offline".equals(b.status())));
		long on = fr.stream().filter(f -> !"offline".equals(f.status())).count();
		String count = on + " online, " + fr.size() + " total";
		g.drawString(font, count, x + w - font.width(count), y0 + 4, Ui.FAINT, false);
		int gridTop = y0 + 22, gap = 6;
		int cols = Math.max(2, (w + gap) / (120 + gap));
		int cw = (w - (cols - 1) * gap) / cols, ch = 46;
		listH = Math.max(0, ((fr.size() + cols - 1) / cols) * (ch + gap) - (bottom - gridTop));
		listScrollTarget = Math.min(listScrollTarget, listH);
		listScroll += (listScrollTarget - listScroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		if (fr.isEmpty()) Ui.centered(g, font, Social.friends.isEmpty() ? "No friends yet. Press + to add someone." : "Nobody found", this.width / 2, gridTop + 40, Ui.FAINT);
		g.enableScissor(x, gridTop, x + w, bottom);
		for (int i = 0; i < fr.size(); i++) {
			Social.Friend f = fr.get(i);
			int cx = x + (i % cols) * (cw + gap), cy = gridTop + (i / cols) * (ch + gap) - Math.round(listScroll);
			if (cy + ch < gridTop || cy > bottom) continue;
			boolean hov = Ui.hover(mx, my, cx, cy, cw, ch) && my >= gridTop && my < bottom;
			boolean offline = "offline".equals(f.status());
			float a = Ui.anim("rc:" + f.uuid(), hov);
			Ui.cutBox(g, cx, cy, cw, ch, 5, Colors.lerp(0xFF1E1E21, 0xFF27272B, a), Colors.lerp(0xFF2E2E33, 0xFF6A6A72, a));
			g.fill(cx + 1, cy + 5, cx + 3, cy + ch - 5, Social.statusColor(f.status()));
			ringFace(g, f.uuid(), cx + 10, cy + 9, 24, f.status());
			g.drawString(font, Ui.fit(font, f.name(), cw - 46), cx + 42, cy + 9, offline ? Ui.MUTED : Ui.TEXT, false);
			String where = !offline && !f.server().isEmpty() ? f.server() : Social.statusLabel(f.status());
			g.drawString(font, Ui.fit(font, where, cw - 46), cx + 42, cy + 21, Ui.FAINT, false);
			int bx = cx + 42;
			if (hov) {
				if (button(g, mx, my, bx, cy + ch - 15, 44, 12, "Message", false, Ui.TEXT) && click) action = () -> { tab = Tab.CHAT; selected = f.uuid(); scroll = scrollTarget = 0; };
				if (joinable(f) && cw - 46 >= 94 && button(g, mx, my, bx + 48, cy + ch - 15, 36, 12, "Join", true, Ui.TEXT) && click) action = () -> join(f);
			}
			if (hov && rclick) friendMenu(mx, my, f);
		}
		g.disableScissor();
		return action;
	}

	private Runnable requests(GuiGraphics g, int mx, int my, boolean click, boolean enter, float delta, Runnable action) {
		Social.openChat = null;
		int w = Math.min(420, this.width - m * 2), x = (this.width - w) / 2, y = top + 8;
		g.drawString(font, "Add a friend", x, y, Ui.TEXT, false);
		g.drawString(font, "Type their Minecraft name", x, y + 11, Ui.FAINT, false);
		int bw = w - 96;
		Ui.cutBox(g, x, y + 24, bw, 20, 4, 0xFF141416, addBox.isFocused() ? Ui.ACCENT_2 : 0xFF34343A);
		addBox.visible = true;
		addBox.setX(x + 7);
		addBox.setY(y + 30);
		addBox.setWidth(bw - 14);
		addBox.render(g, mx, my, delta);
		if (adding && !addBox.isFocused() && addBox.getValue().isEmpty()) { addBox.setFocused(true); setFocused(addBox); adding = false; }
		boolean send = (button(g, mx, my, x + bw + 6, y + 24, 90, 20, "Send request", true, Ui.TEXT) && click) || (enter && addBox.isFocused());
		if (send && !addBox.getValue().isBlank()) {
			String name = addBox.getValue();
			addBox.setValue("");
			action = () -> Social.addFriend(name, msg -> note(msg, msg.startsWith("Request") || msg.startsWith("You're") ? 0xFF8CFFA0 : 0xFFFF8C8C));
		}

		List<Object[]> rows = new ArrayList<>();
		rows.add(new Object[]{"h", Social.incoming.isEmpty() ? "No requests right now" : "Waiting for you (" + Social.incoming.size() + ")"});
		for (var p : Social.incoming) rows.add(new Object[]{"in", p});
		if (!Social.outgoing.isEmpty()) { rows.add(new Object[]{"h", "Sent"}); for (var p : Social.outgoing) rows.add(new Object[]{"out", p}); }
		if (!Social.blocked.isEmpty()) { rows.add(new Object[]{"h", "Blocked"}); for (var p : Social.blocked) rows.add(new Object[]{"b", p}); }
		int listTop = y + 56, bottom = this.height - m, total = 0;
		for (Object[] r : rows) total += r[0].equals("h") ? 18 : 28;
		listH = Math.max(0, total - (bottom - listTop));
		listScrollTarget = Math.min(listScrollTarget, listH);
		listScroll += (listScrollTarget - listScroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		g.enableScissor(x - 2, listTop, x + w + 2, bottom);
		int yy = listTop - Math.round(listScroll);
		for (Object[] r : rows) {
			String kind = (String) r[0];
			if (kind.equals("h")) { g.drawString(font, (String) r[1], x, yy + 6, Ui.FAINT, false); yy += 18; continue; }
			Social.Person p = (Social.Person) r[1];
			boolean hov = Ui.hover(mx, my, x, yy, w, 26) && my >= listTop && my < bottom;
			Ui.cutBox(g, x, yy, w, 26, 4, hov ? 0xFF27272B : 0xFF1E1E21, kind.equals("in") ? 0xFF4A4A52 : 0xFF2E2E33);
			face(g, p.uuid(), x + 7, yy + 5, 16);
			g.drawString(font, Ui.fit(font, p.name(), w - 150), x + 30, yy + 9, kind.equals("b") ? Ui.FAINT : Ui.TEXT, false);
			int bx = x + w - 6;
			switch (kind) {
				case "in" -> {
					bx -= 54;
					if (button(g, mx, my, bx, yy + 5, 54, 16, "Decline", false, Ui.MUTED) && click && hov) action = () -> Social.decline(p.uuid());
					bx -= 58;
					if (button(g, mx, my, bx, yy + 5, 54, 16, "Accept", true, Ui.TEXT) && click && hov) action = () -> { Social.accept(p.uuid()); note("You and " + p.name() + " are friends", 0xFF8CFFA0); };
				}
				case "out" -> {
					bx -= 54;
					if (button(g, mx, my, bx, yy + 5, 54, 16, "Cancel", false, Ui.MUTED) && click && hov) action = () -> Social.cancel(p.uuid());
				}
				case "b" -> {
					bx -= 58;
					if (button(g, mx, my, bx, yy + 5, 58, 16, "Unblock", false, Ui.TEXT) && click && hov) action = () -> Social.unblock(p.uuid());
				}
				default -> { }
			}
			yy += 28;
		}
		g.disableScissor();
		return action;
	}

	private static int rowHeight(Object[] row) {
		return switch ((String) row[0]) {
			case "day" -> 20;
			case "head" -> 18;
			default -> {
				@SuppressWarnings("unchecked") List<String> lines = (List<String>) row[1];
				yield lines.size() * 10 + 2;
			}
		};
	}

	private static long lastTs(String uuid) {
		List<Social.Message> m = Social.CHATS.get(uuid);
		return m == null || m.isEmpty() ? 0 : m.get(m.size() - 1).ts();
	}

	private int sumUnread() {
		int n = 0;
		for (int v : Social.UNREAD.values()) n += v;
		return n;
	}

	private List<String> wrap(String text, int w) {
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

	public void autotestRequestsTab() {
		tab = Tab.REQUESTS;
	}

	public void autotestFriendsTab() {
		tab = Tab.FRIENDS;
		selected = "069a79f444e94726a5befca90e38aaf5";
	}

	@Override
	public void removed() {
		Social.screenOpen = false;
		Social.openChat = null;
		super.removed();
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}
