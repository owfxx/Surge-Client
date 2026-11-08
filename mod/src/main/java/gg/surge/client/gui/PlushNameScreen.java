package gg.surge.client.gui;

import gg.surge.client.config.SurgeConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class PlushNameScreen extends Screen {
	private final Screen parent;
	private EditBox box;
	private boolean wasDown = true, wasEnter = true;

	public PlushNameScreen(Screen parent) {
		super(Component.literal("Skin Plush"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		box = new EditBox(font, this.width / 2 - 90, this.height / 2 - 4, 180, 12, Component.literal("Name"));
		box.setBordered(false);
		box.setMaxLength(16);
		box.setFilter(s -> s.matches("[A-Za-z0-9_]*"));
		box.setHint(Component.literal("Any Minecraft name").withStyle(ChatFormatting.DARK_GRAY));
		box.setValue(SurgeConfig.get().petName == null ? "" : SurgeConfig.get().petName);
		addRenderableWidget(box);
		setInitialFocus(box);
	}

	private void done() {
		SurgeConfig.get().petName = box.getValue().trim();
		SurgeConfig.get().pet = "plush";
		SurgeConfig.save();
		minecraft.setScreen(parent);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		renderBackground(g, mx, my, delta);
		Ui.frame();
		boolean down = GLFW.glfwGetMouseButton(minecraft.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean click = down && !wasDown;
		wasDown = down;
		boolean enter = GLFW.glfwGetKey(minecraft.getWindow().handle(), GLFW.GLFW_KEY_ENTER) == GLFW.GLFW_PRESS;
		boolean pressEnter = enter && !wasEnter;
		wasEnter = enter;
		int pw = 220, ph = 96, px = (this.width - pw) / 2, py = (this.height - ph) / 2 - 20;
		Ui.shadow(g, px, py, pw, ph + 20);
		Ui.box(g, px, py, pw, ph + 20, Ui.BG, Ui.EDGE);
		Ui.centeredShadow(g, font, "Whose skin should your plush have?", this.width / 2, py + 12, Ui.TEXT);
		g.drawCenteredString(font, "Leave it empty to use your own", this.width / 2, py + 26, Ui.MUTED);
		Ui.box(g, px + 16, this.height / 2 - 9, pw - 32, 20, 0xFF161619, box.isFocused() ? Ui.ACCENT : Ui.EDGE);
		box.setX(px + 22);
		box.setWidth(pw - 44);
		super.render(g, mx, my, delta);
		int bw = 90, by = py + ph - 6;
		boolean hov = Ui.hover(mx, my, this.width / 2 - bw / 2, by, bw, 20);
		Ui.mcButton(g, this.width / 2 - bw / 2, by, bw, 20, hov, false, 1f);
		g.drawCenteredString(font, "Done", this.width / 2, by + 6, Ui.TEXT);
		if ((hov && click) || pressEnter) { Ui.click(); done(); }
	}
}
