package gg.surge.client.gui;

import gg.surge.client.mods.Colors;
import gg.surge.client.mods.Mods;
import gg.surge.client.net.Social;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SocialScreen extends Screen {
	private enum Tab { CHAT, FRIENDS }

	private static Tab tab = Tab.CHAT;
	private static String selected;

	private final Screen parent;
	private EditBox input, addBox;
	private int px, py, pw, ph;
	private boolean wasL = true, wasR = true, wasEnter;
	private String note = "";
	private int noteColor = Ui.MUTED;
	private long noteAt;
	private float scroll, scrollTarget;
	private int contentH;
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
		pw = Math.min(500, this.width - 16);
		ph = Math.min(300, this.height - 16);
		px = (this.width - pw) / 2;
		py = (this.height - ph) / 2;
		clearWidgets();
		input = new EditBox(font, 0, 0, 100, 10, Component.literal("Message"));
		input.setBordered(false);
		input.setMaxLength(500);
		input.setHint(Component.literal("Message").withStyle(ChatFormatting.DARK_GRAY));
		addBox = new EditBox(font, 0, 0, 100, 10, Component.literal("Name"));
		addBox.setBordered(false);
		addBox.setMaxLength(16);
		addBox.setHint(Component.literal("Minecraft name").withStyle(ChatFormatting.DARK_GRAY));
		addWidget(input);
		addWidget(addBox);
		Social.screenOpen = true;
		Social.refresh();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		g.fill(0, 0, this.width, this.height, Colors.alpha(0x90000000, Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f)));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return menuLabels == null && !(getFocused() instanceof EditBox);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0) { scrollTarget = Math.max(0, Math.min(Math.max(0, contentH), scrollTarget - (float) scrollY * 24f)); return true; }
		return false;
	}

	private void note(String s, int color) {
		note = s;
		noteColor = color;
		noteAt = System.currentTimeMillis();
	}

	private void openMenu(int x, int y, List<String> labels, List<Runnable> actions) {
		menuX = Math.min(x, this.width - 110);
		menuY = Math.min(y, this.height - labels.size() * 16 - 6);
		menuLabels = labels;
		menuActions = actions;
	}

	private void friendMenu(int x, int y, Social.Friend f) {
		boolean isMuted = Social.muted.contains(f.uuid());
		List<String> l = new ArrayList<>();
		List<Runnable> a = new ArrayList<>();
		l.add("Message"); a.add(() -> { tab = Tab.CHAT; selected = f.uuid(); });
		if (!f.server().isEmpty() && !f.server().equals("Singleplayer")) { l.add("Join their server"); a.add(() -> join(f)); }
		l.add(isMuted ? "Unmute" : "Mute"); a.add(() -> { if (isMuted) Social.unmute(f.uuid()); else Social.mute(f.uuid()); note(isMuted ? "Unmuted " + f.name() : "Muted " + f.name(), Ui.MUTED); });
		l.add("Remove friend"); a.add(() -> { Social.remove(f.uuid()); if (f.uuid().equals(selected)) selected = null; note("Removed " + f.name(), Ui.MUTED); });
		l.add("Block"); a.add(() -> { Social.block(f.uuid()); if (f.uuid().equals(selected)) selected = null; note("Blocked " + f.name(), 0xFFFF8C8C); });
		openMenu(x, y, l, a);
	}

	private void join(Social.Friend f) {
		String ip = f.server();
		if (!ServerAddress.isValidAddress(ip)) { note("Can't join that server", 0xFFFF8C8C); return; }
		ConnectScreen.startConnecting(this, minecraft, ServerAddress.parseString(ip), new ServerData(f.name() + "'s server", ip, ServerData.Type.OTHER), false, null);
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
			int idx = -1;
			for (int i = 0; i < menuLabels.size(); i++) if (Ui.hover(mx, my, menuX, menuY + 3 + i * 16, 104, 16)) idx = i;
			Runnable run = idx >= 0 ? menuActions.get(idx) : null;
			menuLabels = null;
			if (run != null) { Ui.click(); run.run(); }
			click = rclick = false;
		}

		Ui.shadow(g, px, py, pw, ph);
		Ui.box(g, px, py, pw, ph, Ui.BG, Ui.EDGE);
		Ui.logo(g, px + 9, py + 8, 16);
		g.drawString(font, Component.literal("Social").withStyle(ChatFormatting.BOLD), px + 30, py + 12, Ui.TEXT, true);
		if (Ui.iconButton(g, mx, my, px + pw - 26, py + 7, 18, Ui.CROSS) && click) action = this::onClose;

		String st = Social.status();
		String stLabel = Social.statusLabel(st);
		int sw = font.width(stLabel) + 26, sx = px + pw - 32 - sw;
		boolean stHov = Ui.hover(mx, my, sx, py + 7, sw, 18);
		float sa = Ui.anim("social:status", stHov);
		Ui.box(g, sx, py + 7, sw, 18, Colors.lerp(0xFF1C1C1F, 0xFF2C2C31, sa), Ui.EDGE);
		dot(g, sx + 8, py + 16, Social.statusColor(st.equals("invisible") ? "offline" : st));
		g.drawString(font, stLabel, sx + 16, py + 12, Ui.TEXT, false);
		if (stHov && click) {
			List<String> ls = List.of("Online", "Idle", "Busy", "Appear offline");
			List<Runnable> as = new ArrayList<>();
			for (String v : new String[]{"Online", "Idle", "Busy", "Offline"}) as.add(() -> Mods.STATUS.set(v));
			openMenu(sx, py + 27, ls, as);
			click = false;
		}

		int tx = px + 92;
		for (Tab t : Tab.values()) {
			String label = t == Tab.CHAT ? "Chat" : "Friends";
			int badge = t == Tab.CHAT ? sumUnread() : Social.incoming.size();
			int w = font.width(label) + 18 + (badge > 0 ? 12 : 0);
			if (Ui.pill(g, font, mx, my, tx, py + 8, w, 16, label + (badge > 0 ? "  " : ""), t == tab) && click && t != tab) action = () -> { tab = t; scroll = scrollTarget = 0; };
			if (badge > 0) {
				int bx = tx + w - 14;
				g.fill(bx, py + 11, bx + 10, py + 21, 0xFFE11D48);
				Ui.centered(g, font, badge > 9 ? "9+" : String.valueOf(badge), bx + 5, py + 12, 0xFFFFFFFF);
			}
			tx += w + 4;
		}
		g.fill(px + 1, py + 31, px + pw - 1, py + 32, Ui.EDGE);

		input.visible = false;
		addBox.visible = false;
		if (!Social.available()) {
			List<String> lines = wrap(Social.unavailableReason(), pw - 60);
			for (int i = 0; i < lines.size(); i++) Ui.centered(g, font, lines.get(i), px + pw / 2, py + ph / 2 - 10 + i * 11, Ui.MUTED);
		} else if (tab == Tab.CHAT) {
			action = chat(g, mx, my, click, rclick, enterPressed, delta, action);
		} else {
			action = friendsTab(g, mx, my, click, rclick, enterPressed, delta, action);
		}

		if (!note.isEmpty() && System.currentTimeMillis() - noteAt < 4000) {
			int nw = font.width(note) + 12;
			Ui.box(g, px + (pw - nw) / 2, py + ph - 22, nw, 16, 0xF0141418, noteColor);
			g.drawString(font, note, px + (pw - nw) / 2 + 6, py + ph - 18, noteColor, false);
		}

		if (menuLabels != null) {
			g.nextStratum();
			int h = menuLabels.size() * 16 + 6;
			Ui.shadow(g, menuX, menuY, 104, h);
			Ui.box(g, menuX, menuY, 104, h, 0xFA18181B, Ui.EDGE);
			for (int i = 0; i < menuLabels.size(); i++) {
				int y = menuY + 3 + i * 16;
				boolean hov = Ui.hover(mx, my, menuX, y, 104, 16);
				if (hov) g.fill(menuX + 2, y, menuX + 102, y + 16, 0xFF3A3256);
				String lab = menuLabels.get(i);
				int col = lab.equals("Block") || lab.equals("Remove friend") ? 0xFFFF8C8C : Ui.TEXT;
				g.drawString(font, lab, menuX + 8, y + 4, col, false);
			}
		}
		if (action != null) { Ui.click(); action.run(); }
	}

	private static void dot(GuiGraphics g, int cx, int cy, int color) {
		g.fill(cx - 2, cy - 3, cx + 3, cy + 4, color);
		g.fill(cx - 3, cy - 2, cx + 4, cy + 3, color);
	}

	private int sumUnread() {
		int n = 0;
		for (int v : Social.UNREAD.values()) n += v;
		return n;
	}

	private Runnable chat(GuiGraphics g, int mx, int my, boolean click, boolean rclick, boolean enter, float delta, Runnable action) {
		List<Social.Friend> fr = Social.friends;
		int lx = px + 1, lw = 150, top = py + 33, bottom = py + ph - 1;
		g.fill(lx, top, lx + lw, bottom, Ui.SIDEBAR);
		g.fill(lx + lw, top, lx + lw + 1, bottom, Ui.EDGE);
		if (fr.isEmpty()) {
			List<String> lines = wrap("No friends yet. Add some in the Friends tab.", lw - 16);
			for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), lx + 8, top + 10 + i * 10, Ui.MUTED, false);
		}
		if (selected == null && !fr.isEmpty()) selected = fr.get(0).uuid();
		int y = top + 4;
		for (Social.Friend f : fr) {
			if (y + 26 > bottom) break;
			boolean sel = f.uuid().equals(selected);
			boolean hov = Ui.flat(g, mx, my, lx + 3, y, lw - 6, 26, sel, "chat:" + f.uuid());
			dot(g, lx + 13, y + 9, Social.statusColor(f.status()));
			g.drawString(font, Ui.fit(font, f.name(), lw - 50), lx + 22, y + 5, Ui.TEXT, false);
			List<Social.Message> msgs = Social.CHATS.get(f.uuid());
			String sub = !f.server().isEmpty() ? f.server() : msgs != null && !msgs.isEmpty() ? msgs.get(msgs.size() - 1).text() : Social.statusLabel(f.status());
			g.drawString(font, Ui.fit(font, sub, lw - 30), lx + 22, y + 15, Ui.FAINT, false);
			int un = Social.UNREAD.getOrDefault(f.uuid(), 0);
			if (un > 0) {
				g.fill(lx + lw - 20, y + 8, lx + lw - 8, y + 18, Ui.ACCENT);
				Ui.centered(g, font, un > 9 ? "9+" : String.valueOf(un), lx + lw - 14, y + 9, 0xFFFFFFFF);
			}
			if (Social.muted.contains(f.uuid())) g.drawString(font, "muted", lx + lw - 6 - font.width("muted"), y + 5, Ui.FAINT, false);
			if (hov && click) action = () -> { selected = f.uuid(); scroll = scrollTarget = 0; };
			if (hov && rclick) friendMenu(mx, my, f);
			y += 28;
		}

		Social.Friend f = selected == null ? null : Social.friend(selected);
		Social.openChat = f == null ? null : f.uuid();
		if (f == null) return action;
		Social.UNREAD.remove(f.uuid());
		int cx = lx + lw + 8, cw = px + pw - 8 - cx;
		dot(g, cx + 4, top + 11, Social.statusColor(f.status()));
		g.drawString(font, Component.literal(f.name()).withStyle(ChatFormatting.BOLD), cx + 12, top + 7, Ui.TEXT, false);
		String where = Social.statusLabel(f.status()) + (f.server().isEmpty() ? "" : "  on " + f.server());
		g.drawString(font, Ui.fit(font, where, cw - 90), cx + 12, top + 18, Ui.MUTED, false);
		if (!f.server().isEmpty() && !f.server().equals("Singleplayer")) {
			if (Ui.pill(g, font, mx, my, cx + cw - 50, top + 6, 50, 16, "Join", true) && click) action = () -> join(f);
		}
		g.fill(cx, top + 30, cx + cw, top + 31, 0xFF34343A);

		int areaTop = top + 34, areaBottom = bottom - 28;
		List<Social.Message> msgs = Social.CHATS.getOrDefault(f.uuid(), List.of());
		List<String[]> rows = new ArrayList<>();
		SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");
		for (Social.Message m : msgs) {
			boolean mine = m.from().equals(Social.myUuid);
			List<String> lines = wrap(m.text(), (int) (cw * 0.72f) - 12);
			for (int i = 0; i < lines.size(); i++) rows.add(new String[]{mine ? "R" : "L", lines.get(i), i == lines.size() - 1 ? fmt.format(new Date(m.ts())) : "", i == 0 ? "first" : ""});
		}
		int lineH = 11;
		int total = rows.size() * lineH + countFirsts(rows) * 6;
		contentH = Math.max(0, total - (areaBottom - areaTop));
		scrollTarget = Math.min(scrollTarget, contentH);
		scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		g.enableScissor(cx, areaTop, cx + cw, areaBottom);
		int yy = areaBottom - total + Math.round(scroll);
		for (String[] row : rows) {
			if (!row[3].isEmpty()) yy += 6;
			boolean mine = row[0].equals("R");
			int tw = font.width(row[1]) + 10;
			int bx = mine ? cx + cw - tw - 2 : cx + 2;
			if (yy + lineH > areaTop && yy < areaBottom) {
				g.fill(bx, yy, bx + tw, yy + lineH, mine ? 0xFF5B34B5 : 0xFF2E2E33);
				g.drawString(font, row[1], bx + 5, yy + 2, 0xFFFFFFFF, false);
				if (!row[2].isEmpty()) {
					int tx = mine ? bx - 4 - font.width(row[2]) : bx + tw + 4;
					g.drawString(font, row[2], tx, yy + 2, Ui.FAINT, false);
				}
			}
			yy += lineH;
		}
		g.disableScissor();
		if (msgs.isEmpty()) Ui.centered(g, font, "Say hi to " + f.name(), cx + cw / 2, (areaTop + areaBottom) / 2, Ui.FAINT);

		int iy = bottom - 24;
		int sendW = 48;
		Ui.box(g, cx, iy, cw - sendW - 4, 18, 0xFF161619, input.isFocused() ? Ui.ACCENT : Ui.EDGE);
		input.visible = true;
		input.setX(cx + 6);
		input.setY(iy + 5);
		input.setWidth(cw - sendW - 16);
		input.render(g, mx, my, delta);
		if (click) {
			boolean over = Ui.hover(mx, my, cx, iy, cw - sendW - 4, 18);
			input.setFocused(over);
			setFocused(over ? input : null);
		}
		boolean send = (Ui.pill(g, font, mx, my, cx + cw - sendW, iy, sendW, 18, "Send", true) && click) || (enter && input.isFocused());
		if (send && !input.getValue().isBlank()) {
			String text = input.getValue();
			input.setValue("");
			scrollTarget = 0;
			action = () -> Social.message(f.uuid(), text, err -> note(err, 0xFFFF8C8C));
		}
		return action;
	}

	private static int countFirsts(List<String[]> rows) {
		int n = 0;
		for (String[] r : rows) if (!r[3].isEmpty()) n++;
		return n;
	}

	private Runnable friendsTab(GuiGraphics g, int mx, int my, boolean click, boolean rclick, boolean enter, float delta, Runnable action) {
		Social.openChat = null;
		int x = px + 12, w = pw - 24, top = py + 40;
		g.drawString(font, "Add a friend", x, top, Ui.ACCENT_2, false);
		int bw = Math.min(200, w - 110);
		Ui.box(g, x, top + 12, bw, 18, 0xFF161619, addBox.isFocused() ? Ui.ACCENT : Ui.EDGE);
		addBox.visible = true;
		addBox.setX(x + 6);
		addBox.setY(top + 17);
		addBox.setWidth(bw - 12);
		addBox.render(g, mx, my, delta);
		if (click) {
			boolean over = Ui.hover(mx, my, x, top + 12, bw, 18);
			addBox.setFocused(over);
			setFocused(over ? addBox : null);
		}
		boolean add = (Ui.pill(g, font, mx, my, x + bw + 6, top + 12, 96, 18, "Send request", true) && click) || (enter && addBox.isFocused());
		if (add && !addBox.getValue().isBlank()) {
			String name = addBox.getValue();
			addBox.setValue("");
			action = () -> Social.addFriend(name, msg -> note(msg, msg.startsWith("Request") || msg.startsWith("You're") ? 0xFF8CFFA0 : 0xFFFF8C8C));
		}

		int listTop = top + 40, listBottom = py + ph - 8;
		List<Object[]> rows = new ArrayList<>();
		if (!Social.incoming.isEmpty()) { rows.add(new Object[]{"h", "Friend requests (" + Social.incoming.size() + ")"}); for (var p : Social.incoming) rows.add(new Object[]{"in", p}); }
		if (!Social.outgoing.isEmpty()) { rows.add(new Object[]{"h", "Sent requests"}); for (var p : Social.outgoing) rows.add(new Object[]{"out", p}); }
		rows.add(new Object[]{"h", "Friends (" + Social.friends.size() + ")"});
		if (Social.friends.isEmpty()) rows.add(new Object[]{"t", "Nobody yet. Friends need to play with Surge Client too."});
		for (var f : Social.friends) rows.add(new Object[]{"f", f});
		if (!Social.blocked.isEmpty()) { rows.add(new Object[]{"h", "Blocked"}); for (var p : Social.blocked) rows.add(new Object[]{"b", p}); }

		int total = 0;
		for (Object[] r : rows) total += r[0].equals("h") ? 18 : 22;
		contentH = Math.max(0, total - (listBottom - listTop));
		scrollTarget = Math.min(scrollTarget, contentH);
		scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-Ui.dt() * 18f));
		g.enableScissor(x - 2, listTop, x + w + 2, listBottom);
		int y = listTop - Math.round(scroll);
		boolean inView = Ui.hover(mx, my, x, listTop, w, listBottom - listTop);
		for (Object[] r : rows) {
			String kind = (String) r[0];
			if (kind.equals("h")) {
				g.drawString(font, (String) r[1], x, y + 7, Ui.ACCENT_2, false);
				y += 18;
				continue;
			}
			if (kind.equals("t")) {
				g.drawString(font, (String) r[1], x + 4, y + 7, Ui.FAINT, false);
				y += 22;
				continue;
			}
			boolean hov = inView && Ui.hover(mx, my, x, y, w, 20);
			if (hov) g.fill(x, y, x + w, y + 20, 0xFF2E2E33);
			int bx = x + w - 4;
			switch (kind) {
				case "in" -> {
					Social.Person p = (Social.Person) r[1];
					g.drawString(font, p.name(), x + 8, y + 6, Ui.TEXT, false);
					bx -= 60;
					if (Ui.pill(g, font, mx, my, bx, y + 2, 60, 16, "Decline", false) && click && hov) action = () -> Social.decline(p.uuid());
					bx -= 64;
					if (Ui.pill(g, font, mx, my, bx, y + 2, 60, 16, "Accept", true) && click && hov) action = () -> { Social.accept(p.uuid()); note("You and " + p.name() + " are friends", 0xFF8CFFA0); };
					bx -= 52;
					if (Ui.pill(g, font, mx, my, bx, y + 2, 48, 16, "Block", false) && click && hov) action = () -> Social.block(p.uuid());
				}
				case "out" -> {
					Social.Person p = (Social.Person) r[1];
					g.drawString(font, p.name(), x + 8, y + 6, Ui.TEXT, false);
					g.drawString(font, "waiting", x + 16 + font.width(p.name()), y + 6, Ui.FAINT, false);
					bx -= 60;
					if (Ui.pill(g, font, mx, my, bx, y + 2, 60, 16, "Cancel", false) && click && hov) action = () -> Social.cancel(p.uuid());
				}
				case "f" -> {
					Social.Friend f = (Social.Friend) r[1];
					dot(g, x + 8, y + 10, Social.statusColor(f.status()));
					g.drawString(font, f.name(), x + 16, y + 6, Ui.TEXT, false);
					String sub = Social.statusLabel(f.status()) + (f.server().isEmpty() ? "" : " on " + f.server()) + (Social.muted.contains(f.uuid()) ? "  (muted)" : "");
					g.drawString(font, Ui.fit(font, sub, w - 220), x + 22 + font.width(f.name()), y + 6, Ui.FAINT, false);
					bx -= 18;
					boolean more = Ui.iconButton(g, mx, my, bx, y + 2, 16, new String[]{"........", "........", "........", "XX.XX.XX", "XX.XX.XX", "........", "........", "........"});
					if (more && click && hov) friendMenu(mx, my, f);
					bx -= 70;
					if (Ui.pill(g, font, mx, my, bx, y + 2, 66, 16, "Message", true) && click && hov) action = () -> { tab = Tab.CHAT; selected = f.uuid(); };
					if (hov && rclick) friendMenu(mx, my, f);
				}
				case "b" -> {
					Social.Person p = (Social.Person) r[1];
					g.drawString(font, p.name(), x + 8, y + 6, Ui.FAINT, false);
					bx -= 64;
					if (Ui.pill(g, font, mx, my, bx, y + 2, 64, 16, "Unblock", false) && click && hov) action = () -> Social.unblock(p.uuid());
				}
				default -> { }
			}
			y += 22;
		}
		g.disableScissor();
		return action;
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
