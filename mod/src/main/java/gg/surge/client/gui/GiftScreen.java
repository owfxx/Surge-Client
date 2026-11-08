package gg.surge.client.gui;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.cosmetics.CapturedLook;
import gg.surge.client.cosmetics.Preview;
import gg.surge.client.economy.Shop;
import gg.surge.client.emotes.Emotes;
import gg.surge.client.mods.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class GiftScreen extends Screen {
	private final Screen parent;
	private final String heading, note;
	private final int coins;
	private final List<Shop.Item> items = new ArrayList<>();
	private boolean wasDown = true;
	private long openedAt;

	public GiftScreen(Screen parent, String heading, String note, int coins, List<String> itemIds) {
		super(Component.literal(heading));
		this.parent = parent;
		this.heading = heading;
		this.note = note == null ? "" : note;
		this.coins = coins;
		for (String id : itemIds) {
			Shop.Item it = Shop.byId(id);
			if (it != null) items.add(it);
		}
	}

	public static void show(String heading, String note, int coins, List<String> itemIds) {
		Minecraft mc = Minecraft.getInstance();
		mc.execute(() -> {
			mc.setScreen(new GiftScreen(mc.screen, heading, note, coins, itemIds));
			try { gg.surge.client.features.CustomSounds.preview("preset:chime"); } catch (Exception ignored) { }
		});
	}

	@Override
	protected void init() {
		if (openedAt == 0) openedAt = System.currentTimeMillis();
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
		if (parent != null && minecraft.level == null) parent.renderBackground(g, mouseX, mouseY, delta);
		g.fill(0, 0, this.width, this.height, Colors.alpha(0xB0000000, Math.min(1f, (System.currentTimeMillis() - openedAt) / 200f)));
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	private Shop.Item wearable() {
		for (Shop.Item i : items) if (i.category().equals("cosmetic")) return i;
		return null;
	}

	private static void wear(Shop.Item i) {
		SurgeConfig c = SurgeConfig.get();
		String type = i.id().substring(0, i.id().indexOf(':'));
		switch (type) {
			case "wings" -> c.wings = i.key();
			case "halo" -> c.halo = i.key();
			default -> c.cape = i.key();
		}
		SurgeConfig.save();
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		renderBackground(g, mx, my, delta);
		Ui.frame();
		boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasDown;
		wasDown = down;
		Runnable action = null;
		float k = Math.min(1f, (System.currentTimeMillis() - openedAt) / 260f);
		float pop = 1f - (1f - k) * (1f - k);

		int shown = Math.min(4, items.size());
		int card = 74;
		int pw = Math.min(this.width - 16, Math.max(240, shown * (card + 6) + 30)), ph = Math.min(this.height - 16, shown > 0 ? 200 : 120);
		int px = (this.width - pw) / 2, py = (this.height - ph) / 2 + Math.round((1 - pop) * 16);
		Ui.shadow(g, px, py, pw, ph);
		Ui.box(g, px, py, pw, ph, Ui.BG, Ui.EDGE);
		g.fill(px + 1, py + 1, px + pw - 1, py + 3, 0xFFF2C430);
		Ui.logo(g, px + pw / 2 - 9, py + 10, 18);
		Ui.centeredShadow(g, font, Component.literal(heading).withStyle(ChatFormatting.BOLD).getString(), px + pw / 2, py + 34, 0xFFFFE07A);
		int y = py + 48;
		if (!note.isBlank()) {
			for (var line : font.split(Component.literal(note), pw - 30)) {
				g.drawCenteredString(font, line, px + pw / 2, y, Ui.MUTED);
				y += 10;
			}
			y += 2;
		}
		if (coins > 0) {
			String s = "+" + coins + " Surge Coins";
			int tw = font.width(s) + 14;
			Ui.coin(g, px + pw / 2 - tw / 2, y - 1, 10);
			g.drawString(font, s, px + pw / 2 - tw / 2 + 14, y, 0xFFFFE07A, true);
			y += 14;
		}
		if (shown > 0) {
			int total = shown * card + (shown - 1) * 6;
			int cx = px + (pw - total) / 2;
			int ch = Math.max(60, py + ph - 34 - y);
			for (int i = 0; i < shown; i++) {
				Shop.Item it = items.get(i);
				int x = cx + i * (card + 6);
				Ui.cutBox(g, x, y, card, ch, 4, 0xFF1E1E21, 0xFF34343A);
				CapturedLook look;
				if (it.category().equals("emote")) {
					Emotes.Emote e = Emotes.Emote.byName(it.key());
					look = new CapturedLook(null, null, null, e, (System.currentTimeMillis() % 100000) / 1000f, true);
				} else {
					String sub = it.sub();
					look = new CapturedLook(sub.equals("cape") ? it.key() : null, sub.equals("wings") ? it.key() : null, sub.equals("halo") ? it.key() : null, null, 0f, false);
				}
				float turn = it.sub().equals("cape") ? 180f + (float) Math.sin(System.currentTimeMillis() / 900.0) * 25f : (System.currentTimeMillis() / 30f) % 360f;
				int size = Math.max(20, (ch - 16) * 4 / 10);
				if (it.sub().equals("halo")) size = size * 3 / 2;
				Preview.drawCard(g, x + 2, y + 2, x + card - 2, y + ch - 13, size, turn, look);
				g.drawCenteredString(font, Ui.fit(font, it.name(), card - 6), x + card / 2, y + ch - 11, Ui.TEXT);
			}
		}
		Shop.Item w = wearable();
		int bw = 90, by = py + ph - 26;
		if (w != null) {
			int bx = px + pw / 2 - bw - 3;
			boolean hov = Ui.hover(mx, my, bx, by, bw, 20);
			Ui.mcButton(g, bx, by, bw, 20, hov, false, 1f);
			g.drawCenteredString(font, "Wear it", bx + bw / 2, by + 6, Ui.TEXT);
			if (hov && click) action = () -> { wear(w); onClose(); };
		}
		int bx = w != null ? px + pw / 2 + 3 : px + pw / 2 - bw / 2;
		boolean hov = Ui.hover(mx, my, bx, by, bw, 20);
		Ui.mcButton(g, bx, by, bw, 20, hov, false, 1f);
		g.drawCenteredString(font, "Awesome", bx + bw / 2, by + 6, Ui.TEXT);
		if (hov && click) action = this::onClose;
		super.render(g, mx, my, delta);
		if (action != null) { Ui.click(); action.run(); }
	}
}
