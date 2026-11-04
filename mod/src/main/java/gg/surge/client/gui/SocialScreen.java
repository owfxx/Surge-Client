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
	private enum Tab { CHAT, FRIENDS }

	private static Tab tab = Tab.CHAT;
	private static String selected;

	private final Screen parent;
	private EditBox input, addBox, searchBox;
	private boolean searching, adding;
	private int wx, wy, ww, wh, leftW;
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
		ww = Math.min(640, this.width - 20);
		wh = Math.min(360, this.height - 20);
		wx = (this.width - ww) / 2;
		wy = (this.height - wh) / 2;
		leftW = Math.max(130, Math.min(200, (int) (ww * 0.32f)));
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
		g.fill(0, 0, this.width, this.height, Colors.alpha(0xB0101012, Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f)));
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
		if (mouseX < wx + leftW) listScrollTarget = Math.max(0, Math.min(Math.max(0, listH), listScrollTarget - (float) scrollY * 24f));
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
		l.add("Message"); a.add(() -> { tab = Tab.CHAT; selected = f.uuid(); });
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
		g.fill(x, y, x + 16, y + 16, on ? 0xFF4A4A52 : hov ? 0xFF3A3A40 : 0xFF2C2C30);
		Ui.icon(g, icon, x + 4, y + 4, 1, Ui.TEXT);
		return hov;
	}

	private static final String[] ADD = {".XX.....", "XXXX..X.", "XXXX.XXX", ".XX...X.", "XXXX....", "XXXX....", "........", "........"};
	private static final String[] MENU = {"........", "XXXXXXX.", "........", "XXXXXXX.", "........", "XXXXXXX.", "........", "........"};

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
				boolean over = b.visible && Ui.hover(mx, my, b.getX() - 6, b.getY() - 5, b.getWidth() + 12, 18);
				b.setFocused(over);
				if (over) setFocused(b);
			}
		}

		g.fill(wx, wy, wx + ww, wy + wh, 0xFF1E1E21);
		Ui.outline(g, wx - 1, wy - 1, ww + 2, wh + 2, 0xFF2C2C30);
		g.fill(wx + leftW, wy, wx + leftW + 2, wy + wh, 0xFF141416);
		input.visible = addBox.visible = searchBox.visible = false;

		action = left(g, mx, my, click, rclick, enterPressed, delta, action);
		if (!Social.available()) {
			int cx = wx + leftW + 2 + (ww - leftW - 2) / 2;
			List<String> lines = wrap(Social.unavailableReason(), ww - leftW - 40);
			for (int i = 0; i < lines.size(); i++) Ui.centered(g, font, lines.get(i), cx, wy + wh / 2 - 10 + i * 11, Ui.MUTED);
		} else if (tab == Tab.CHAT) action = chat(g, mx, my, click, rclick, enterPressed, delta, action);
		else action = friendPanel(g, mx, my, click, enterPressed, delta, action);

		if (!note.isEmpty() && System.currentTimeMillis() - noteAt < 4000) {
			g.nextStratum();
			int nw = font.width(note) + 14, nx = wx + leftW + 2 + (ww - leftW - 2 - nw) / 2, ny = wy + 30;
			g.fill(nx, ny, nx + nw, ny + 16, 0xF0101012);
			Ui.outline(g, nx, ny, nw, 16, noteColor);
			g.drawString(font, note, nx + 7, ny + 4, noteColor, false);
		}
		if (menuLabels != null) {
			g.nextStratum();
			int w = menuWidth(), h = menuLabels.size() * 16 + 6;
			g.fill(menuX, menuY, menuX + w, menuY + h, 0xFA202023);
			Ui.outline(g, menuX, menuY, w, h, 0xFF3A3A40);
			for (int i = 0; i < menuLabels.size(); i++) {
				int y = menuY + 3 + i * 16;
				if (Ui.hover(mx, my, menuX, y, w, 16)) g.fill(menuX + 2, y, menuX + w - 2, y + 16, 0xFF34343A);
				String lab = menuLabels.get(i);
				g.drawString(font, lab, menuX + 8, y + 4, lab.equals("Block") || lab.equals("Remove friend") ? 0xFFFF8C8C : Ui.TEXT, false);
			}
		}
		if (action != null) { Ui.click(); action.run(); }
	}

	private void statusDot(GuiGraphics g, int x, int y, String status) {
		g.fill(x - 1, y - 1, x + 6, y + 6, 0xFF1E1E21);
		g.fill(x, y, x + 5, y + 5, Social.statusColor(status));
	}

	private Runnable left(GuiGraphics g, int mx, int my, boolean click, boolean rclick, boolean enter, float delta, Runnable action) {
		int x = wx, w = leftW;
		g.drawString(font, "Social", x + 10, wy + 10, Ui.TEXT, false);
		int bx = x + w - 22;
		if (iconButton(g, mx, my, bx, wy + 5, Ui.SEARCH, searching) && click) action = () -> { searching = !searching; if (!searching) searchBox.setValue(""); };
		bx -= 19;
		if (iconButton(g, mx, my, bx, wy + 5, ADD, adding) && click) action = () -> { adding = !adding; tab = Tab.FRIENDS; };
		g.fill(x, wy + 26, x + w, wy + 27, 0xFF141416);

		int ty = wy + 34, tx = x + 10;
		for (Tab t : Tab.values()) {
			String label = t == Tab.CHAT ? "Chat" : "Friends";
			int badge = t == Tab.CHAT ? sumUnread() : Social.incoming.size();
			boolean hov = Ui.hover(mx, my, tx - 2, ty - 3, font.width(label) + 4, 14);
			g.drawString(font, label, tx, ty, t == tab ? Ui.ACCENT_2 : hov ? Ui.TEXT : Ui.MUTED, false);
			if (t == tab) g.fill(tx, ty + 11, tx + font.width(label), ty + 12, Ui.ACCENT_2);
			if (badge > 0) {
				String b = badge > 9 ? "9+" : String.valueOf(badge);
				int bxx = tx + font.width(label) + 3;
				g.fill(bxx, ty - 1, bxx + font.width(b) + 4, ty + 9, 0xFFE11D48);
				g.drawString(font, b, bxx + 2, ty, 0xFFFFFFFF, false);
			}
			if (hov && click && t != tab) action = () -> { tab = t; listScroll = listScrollTarget = 0; };
			tx += font.width(label) + 22;
		}
		int top = wy + 52;
		if (searching) {
			g.fill(x + 8, top, x + w - 8, top + 16, 0xFF141416);
			Ui.outline(g, x + 8, top, w - 16, 16, searchBox.isFocused() ? Ui.ACCENT_2 : 0xFF34343A);
			searchBox.visible = true;
			searchBox.setX(x + 13);
			searchBox.setY(top + 4);
			searchBox.setWidth(w - 26);
			searchBox.render(g, mx, my, delta);
			top += 20;
		}
		g.fill(x, top - 2, x + w, top - 1, 0xFF141416);

		int bottom = wy + wh - 24;
		String q = searchBox.getValue().trim().toLowerCase();
		List<Social.Friend> fr = new ArrayList<>();
		for (Social.Friend f : Social.friends) if (q.isEmpty() || f.name().toLowerCase().contains(q)) fr.add(f);
		if (tab == Tab.CHAT) {
			fr.sort((a, b) -> Long.compare(lastTs(b.uuid()), lastTs(a.uuid())));
			int rowH = 28;
			listH = Math.max(0, fr.size() * rowH - (bottom - top));
			listScrollTarget = Math.min(listScrollTarget, listH);
			listScroll += (listScrollTarget - listScroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
			g.enableScissor(x, top, x + w, bottom);
			int y = top - Math.round(listScroll);
			if (fr.isEmpty() && Social.available()) {
				List<String> lines = wrap(Social.friends.isEmpty() ? "No friends yet. Press the add button above to add some." : "Nobody found", w - 20);
				for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x + 10, top + 10 + i * 10, Ui.FAINT, false);
			}
			for (Social.Friend f : fr) {
				boolean sel = f.uuid().equals(selected);
				boolean hov = Ui.hover(mx, my, x, y, w, rowH) && my >= top && my < bottom;
				if (sel || hov) g.fill(x + 2, y, x + w - 2, y + rowH, sel ? 0xFF2C2C30 : 0xFF252528);
				face(g, f.uuid(), x + 8, y + 4, 20);
				statusDot(g, x + 23, y + 19, f.status());
				int un = Social.UNREAD.getOrDefault(f.uuid(), 0);
				List<Social.Message> msgs = Social.CHATS.get(f.uuid());
				Social.Message last = msgs == null || msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);
				String time = last == null ? "" : when(last.ts());
				int nameMax = w - 40 - (un > 0 ? 16 : font.width(time) + 6);
				g.drawString(font, Ui.fit(font, f.name(), nameMax), x + 34, y + 5, Ui.TEXT, false);
				String sub = last != null ? (last.from().equals(Social.myUuid) ? "You: " : "") + last.text() : !f.server().isEmpty() && !"offline".equals(f.status()) ? f.server() : Social.statusLabel(f.status());
				g.drawString(font, Ui.fit(font, sub, w - 44), x + 34, y + 16, Ui.FAINT, false);
				if (un > 0) {
					String b = un > 9 ? "9+" : String.valueOf(un);
					g.fill(x + w - 16, y + 5, x + w - 8, y + 15, 0xFFE11D48);
					Ui.centered(g, font, b, x + w - 12, y + 6, 0xFFFFFFFF);
				} else if (!time.isEmpty()) g.drawString(font, time, x + w - 8 - font.width(time), y + 5, Ui.FAINT, false);
				if (hov && click) action = () -> { selected = f.uuid(); scroll = scrollTarget = 0; };
				if (hov && rclick) friendMenu(mx, my, f);
				y += rowH;
			}
			g.disableScissor();
		} else action = friendList(g, mx, my, click, rclick, x, w, top, bottom, fr, action);

		String st = Social.status();
		String stLabel = Social.statusLabel(st);
		boolean sh = Ui.hover(mx, my, x + 4, bottom + 4, w - 8, 16);
		g.fill(x, bottom, x + w, bottom + 1, 0xFF141416);
		if (sh) g.fill(x + 4, bottom + 4, x + w - 4, bottom + 20, 0xFF2A2A2E);
		statusDot(g, x + 10, bottom + 10, st.equals("invisible") ? "offline" : st);
		g.drawString(font, stLabel, x + 20, bottom + 8, Ui.MUTED, false);
		if (sh && click) {
			List<Runnable> as = new ArrayList<>();
			for (String v : new String[]{"Online", "Idle", "Busy", "Offline"}) as.add(() -> Mods.STATUS.set(v));
			openMenu(x + 4, bottom - 70, List.of("Online", "Idle", "Busy", "Appear offline"), as);
		}
		return action;
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

	private Runnable friendList(GuiGraphics g, int mx, int my, boolean click, boolean rclick, int x, int w, int top, int bottom, List<Social.Friend> fr, Runnable action) {
		List<Object[]> rows = new ArrayList<>();
		if (!Social.incoming.isEmpty()) { rows.add(new Object[]{"h", "Requests"}); for (var p : Social.incoming) rows.add(new Object[]{"in", p}); }
		List<Social.Friend> online = new ArrayList<>(), offline = new ArrayList<>();
		for (var f : fr) ("offline".equals(f.status()) ? offline : online).add(f);
		rows.add(new Object[]{"h", "Online (" + online.size() + ")"});
		for (var f : online) rows.add(new Object[]{"f", f});
		if (!offline.isEmpty()) { rows.add(new Object[]{"h", "Offline (" + offline.size() + ")"}); for (var f : offline) rows.add(new Object[]{"f", f}); }
		if (!Social.outgoing.isEmpty()) { rows.add(new Object[]{"h", "Sent"}); for (var p : Social.outgoing) rows.add(new Object[]{"out", p}); }
		if (!Social.blocked.isEmpty()) { rows.add(new Object[]{"h", "Blocked"}); for (var p : Social.blocked) rows.add(new Object[]{"b", p}); }
		int total = 0;
		for (Object[] r : rows) total += r[0].equals("h") ? 16 : 24;
		listH = Math.max(0, total - (bottom - top));
		listScrollTarget = Math.min(listScrollTarget, listH);
		listScroll += (listScrollTarget - listScroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		g.enableScissor(x, top, x + w, bottom);
		int y = top - Math.round(listScroll);
		for (Object[] r : rows) {
			String kind = (String) r[0];
			if (kind.equals("h")) { g.drawString(font, (String) r[1], x + 10, y + 5, Ui.FAINT, false); y += 16; continue; }
			boolean hov = Ui.hover(mx, my, x, y, w, 24) && my >= top && my < bottom;
			Object o = r[1];
			String id = o instanceof Social.Friend f ? f.uuid() : ((Social.Person) o).uuid();
			String name = o instanceof Social.Friend f ? f.name() : ((Social.Person) o).name();
			boolean sel = id.equals(selected);
			if (sel || hov) g.fill(x + 2, y, x + w - 2, y + 24, sel ? 0xFF2C2C30 : 0xFF252528);
			face(g, id, x + 8, y + 4, 16);
			if (o instanceof Social.Friend f) statusDot(g, x + 19, y + 15, f.status());
			g.drawString(font, Ui.fit(font, name, w - 80), x + 30, y + 8, kind.equals("b") ? Ui.FAINT : Ui.TEXT, false);
			int bx = x + w - 8;
			switch (kind) {
				case "in" -> {
					Social.Person p = (Social.Person) o;
					bx -= 16;
					if (smallButton(g, mx, my, bx, y + 4, "x", 0xFFFF8C8C) && click && hov) action = () -> Social.decline(p.uuid());
					bx -= 18;
					if (smallButton(g, mx, my, bx, y + 4, "+", 0xFF8CFFA0) && click && hov) action = () -> { Social.accept(p.uuid()); note("You and " + p.name() + " are friends", 0xFF8CFFA0); };
				}
				case "out" -> {
					Social.Person p = (Social.Person) o;
					bx -= 16;
					if (smallButton(g, mx, my, bx, y + 4, "x", Ui.MUTED) && click && hov) action = () -> Social.cancel(p.uuid());
				}
				case "b" -> {
					Social.Person p = (Social.Person) o;
					int uw = font.width("Unblock") + 8;
					if (Ui.pill(g, font, mx, my, bx - uw, y + 4, uw, 16, "Unblock", false) && click && hov) action = () -> Social.unblock(p.uuid());
				}
				case "f" -> {
					Social.Friend f = (Social.Friend) o;
					if (hov && click) action = () -> selected = f.uuid();
					if (hov && rclick) friendMenu(mx, my, f);
				}
				default -> { }
			}
			y += 24;
		}
		g.disableScissor();
		return action;
	}

	private boolean smallButton(GuiGraphics g, int mx, int my, int x, int y, String label, int color) {
		boolean hov = Ui.hover(mx, my, x, y, 16, 16);
		g.fill(x, y, x + 16, y + 16, hov ? 0xFF3A3A40 : 0xFF2C2C30);
		Ui.centered(g, font, label, x + 8, y + 4, color);
		return hov;
	}

	private Runnable friendPanel(GuiGraphics g, int mx, int my, boolean click, boolean enter, float delta, Runnable action) {
		int x = wx + leftW + 2, w = ww - leftW - 2, cx = x + w / 2;
		Social.openChat = null;
		g.drawString(font, adding ? "Add a friend" : "Friends", x + 10, wy + 10, Ui.TEXT, false);
		g.fill(x, wy + 26, x + w, wy + 27, 0xFF141416);
		int y = wy + 40;
		g.drawString(font, "Add a friend by their Minecraft name", x + 12, y, Ui.MUTED, false);
		int bw = Math.min(220, w - 110);
		g.fill(x + 12, y + 14, x + 12 + bw, y + 32, 0xFF141416);
		Ui.outline(g, x + 12, y + 14, bw, 18, addBox.isFocused() ? Ui.ACCENT_2 : 0xFF34343A);
		addBox.visible = true;
		addBox.setX(x + 18);
		addBox.setY(y + 19);
		addBox.setWidth(bw - 12);
		addBox.render(g, mx, my, delta);
		if (adding && !addBox.isFocused() && addBox.getValue().isEmpty()) { addBox.setFocused(true); setFocused(addBox); adding = false; }
		boolean send = (Ui.pill(g, font, mx, my, x + 18 + bw, y + 14, 88, 18, "Send request", true) && click) || (enter && addBox.isFocused());
		if (send && !addBox.getValue().isBlank()) {
			String name = addBox.getValue();
			addBox.setValue("");
			action = () -> Social.addFriend(name, msg -> note(msg, msg.startsWith("Request") || msg.startsWith("You're") ? 0xFF8CFFA0 : 0xFFFF8C8C));
		}

		Social.Friend f = selected == null ? null : Social.friend(selected);
		int top = y + 50;
		if (f == null) {
			Ui.centered(g, font, "Pick a friend on the left to see what they're up to.", cx, top + 40, Ui.FAINT);
			return action;
		}
		g.fill(x + 12, top, x + w - 12, top + 72, 0xFF252528);
		face(g, f.uuid(), x + 22, top + 10, 32);
		statusDot(g, x + 48, top + 36, f.status());
		g.drawString(font, Component.literal(f.name()).withStyle(ChatFormatting.BOLD), x + 64, top + 12, Ui.TEXT, false);
		String where = Social.statusLabel(f.status()) + (!"offline".equals(f.status()) && !f.server().isEmpty() ? " on " + f.server() : "");
		g.drawString(font, Ui.fit(font, where, w - 90), x + 64, top + 25, Ui.MUTED, false);
		int bx = x + 64, by = top + 46;
		if (Ui.pill(g, font, mx, my, bx, by, 60, 16, "Message", true) && click) action = () -> { tab = Tab.CHAT; scroll = scrollTarget = 0; };
		bx += 64;
		if (joinable(f)) { if (Ui.pill(g, font, mx, my, bx, by, 60, 16, "Join game", false) && click) action = () -> join(f); bx += 64; }
		if (Ui.pill(g, font, mx, my, bx, by, 40, 16, "More", false) && click) friendMenu(mx, my, f);
		return action;
	}

	private Runnable chat(GuiGraphics g, int mx, int my, boolean click, boolean rclick, boolean enter, float delta, Runnable action) {
		int x = wx + leftW + 2, w = ww - leftW - 2;
		Social.Friend f = selected == null ? null : Social.friend(selected);
		if (f == null && !Social.friends.isEmpty()) { selected = Social.friends.get(0).uuid(); f = Social.friend(selected); }
		Social.openChat = f == null ? null : f.uuid();
		if (f == null) {
			Ui.centered(g, font, "Your chats show up here", x + w / 2, wy + wh / 2 - 4, Ui.FAINT);
			return action;
		}
		Social.UNREAD.remove(f.uuid());
		final Social.Friend fr = f;
		g.drawString(font, f.name(), x + 10, wy + 10, Ui.TEXT, false);
		face(g, f.uuid(), x + 14 + font.width(f.name()), wy + 6, 14);
		int hx = x + w - 22;
		if (iconButton(g, mx, my, hx, wy + 5, MENU, false) && click) friendMenu(hx, wy + 22, fr);
		if (joinable(f)) {
			int jw = font.width("Join Game") + 18;
			hx -= jw + 3;
			boolean jh = Ui.hover(mx, my, hx, wy + 5, jw, 16);
			g.fill(hx, wy + 5, hx + jw, wy + 21, jh ? 0xFF5A5A62 : 0xFF46464C);
			g.drawString(font, "↗ Join Game", hx + 6, wy + 9, Ui.TEXT, false);
			if (jh && click) action = () -> join(fr);
		}
		g.fill(x, wy + 26, x + w, wy + 27, 0xFF141416);

		int areaTop = wy + 30, areaBottom = wy + wh - 34;
		int maxBubble = (int) (w * 0.62f);
		List<Social.Message> msgs = Social.CHATS.getOrDefault(f.uuid(), List.of());
		List<Object[]> rows = new ArrayList<>();
		String lastDay = "";
		long lastTs = 0;
		String lastFrom = "";
		SimpleDateFormat day = new SimpleDateFormat("MMM d"), clock = new SimpleDateFormat("HH:mm");
		for (Social.Message m : msgs) {
			String d = day.format(new Date(m.ts()));
			if (!d.equals(lastDay)) { rows.add(new Object[]{"day", d}); lastDay = d; lastFrom = ""; }
			boolean mine = m.from().equals(Social.myUuid);
			boolean newGroup = !m.from().equals(lastFrom) || m.ts() - lastTs > 5 * 60_000;
			if (newGroup) rows.add(new Object[]{"time", clock.format(new Date(m.ts())), mine});
			rows.add(new Object[]{"msg", wrap(m.text(), maxBubble - 16), mine});
			lastFrom = m.from();
			lastTs = m.ts();
		}
		int total = 6;
		for (Object[] row : rows) total += rowHeight(row);
		contentH = Math.max(0, total - (areaBottom - areaTop));
		scrollTarget = Math.min(scrollTarget, contentH);
		scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		g.enableScissor(x, areaTop, x + w, areaBottom);
		int yy = areaBottom - total + Math.round(scroll) + 6;
		for (Object[] row : rows) {
			int h = rowHeight(row);
			if (yy + h > areaTop && yy < areaBottom) {
				switch ((String) row[0]) {
					case "day" -> {
						String d = (String) row[1];
						int dw = font.width(d);
						g.fill(x + 12, yy + 6, x + w / 2 - dw / 2 - 8, yy + 7, 0xFF34343A);
						g.fill(x + w / 2 + dw / 2 + 8, yy + 6, x + w - 12, yy + 7, 0xFF34343A);
						Ui.centered(g, font, d, x + w / 2, yy + 2, Ui.FAINT);
					}
					case "time" -> {
						String t = (String) row[1];
						boolean mine = (Boolean) row[2];
						g.pose().pushMatrix();
						int tx = mine ? x + w - 12 - Math.round(font.width(t) * 0.75f) : x + 12;
						g.pose().translate(tx, yy + 3);
						g.pose().scale(0.75f, 0.75f);
						g.drawString(font, t, 0, 0, Ui.FAINT, false);
						g.pose().popMatrix();
					}
					default -> {
						@SuppressWarnings("unchecked") List<String> lines = (List<String>) row[1];
						boolean mine = (Boolean) row[2];
						int bw = 0;
						for (String s : lines) bw = Math.max(bw, font.width(s));
						bw += 14;
						int bh = lines.size() * 10 + 8;
						int bx = mine ? x + w - 12 - bw : x + 12;
						g.fill(bx, yy, bx + bw, yy + bh, mine ? 0xFF4A4A52 : 0xFF2C2C30);
						g.fill(mine ? bx + bw - 2 : bx, yy + bh - 2, mine ? bx + bw : bx + 2, yy + bh, mine ? 0xFF5E5E66 : 0xFF38383E);
						for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), bx + 7, yy + 5 + i * 10, Ui.TEXT, false);
					}
				}
			}
			yy += h;
		}
		g.disableScissor();
		if (msgs.isEmpty()) Ui.centered(g, font, "Say hi to " + f.name(), x + w / 2, (areaTop + areaBottom) / 2, Ui.FAINT);

		int iy = wy + wh - 28;
		g.fill(x + 8, iy, x + w - 8, iy + 20, 0xFF29292D);
		if (input.isFocused()) Ui.outline(g, x + 8, iy, w - 16, 20, 0xFF4A4A52);
		input.visible = true;
		input.setHint(Component.literal("Message " + f.name()).withStyle(ChatFormatting.DARK_GRAY));
		input.setX(x + 16);
		input.setY(iy + 6);
		input.setWidth(w - 32);
		input.render(g, mx, my, delta);
		if (enter && input.isFocused() && !input.getValue().isBlank()) {
			String text = input.getValue();
			input.setValue("");
			scrollTarget = 0;
			action = () -> Social.message(fr.uuid(), text, err -> note(err, 0xFFFF8C8C));
		}
		return action;
	}

	private static int rowHeight(Object[] row) {
		return switch ((String) row[0]) {
			case "day" -> 18;
			case "time" -> 11;
			default -> {
				@SuppressWarnings("unchecked") List<String> lines = (List<String>) row[1];
				yield lines.size() * 10 + 12;
			}
		};
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
