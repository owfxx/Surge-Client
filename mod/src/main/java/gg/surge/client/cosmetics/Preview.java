package gg.surge.client.cosmetics;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public final class Preview {
	public static volatile boolean clean;
	public static volatile float yaw;
	public static volatile CapturedLook card;
	public static volatile Float forced;
	private static volatile float nudge;

	public static void turn(float degrees) {
		nudge += degrees;
	}

	private static final class Spin {
		float yaw, target;
		double lastMouse;
		boolean dragging;
		long touchedAt, frameAt = System.nanoTime();
	}

	private static final Map<String, Spin> SPINS = new HashMap<>();

	private Preview() {}

	private static void renderPlayer(GuiGraphics g, int x1, int y1, int x2, int y2, float size, float turn, CapturedLook look) {
		Minecraft mc = Minecraft.getInstance();
		boolean wasClean = clean;
		CapturedLook wasCard = card;
		clean = true;
		card = look;
		try {
			var renderer = mc.getEntityRenderDispatcher().getPlayerRenderer(mc.player);
			net.minecraft.client.renderer.entity.state.AvatarRenderState st = renderer.createRenderState();
			renderer.extractRenderState(mc.player, st, 1f);
			st.bodyRot = 180f + turn;
			st.yRot = 0f;
			st.xRot = 0f;
			st.nameTag = null;
			st.shadowRadius = 0f;
			st.shadowPieces.clear();
			st.leashStates = null;
			st.lightCoords = 0xF000F0;
			st.outlineColor = 0;
			st.displayFireAnimation = false;
			st.isInvisible = false;
			org.joml.Quaternionf rot = new org.joml.Quaternionf().rotateZ((float) Math.PI);
			g.submitEntityRenderState(st, size / Math.max(0.1f, st.scale), new org.joml.Vector3f(0f, st.boundingBoxHeight / 2f + 0.0625f, 0f), rot, new org.joml.Quaternionf(), x1, y1, x2, y2);
		} catch (Throwable ignored) {
		} finally {
			clean = wasClean;
			card = wasCard;
		}
	}

	public static void drawCard(GuiGraphics g, int x1, int y1, int x2, int y2, int size, float turn, CapturedLook look) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null) {
			MenuAvatar.render(g, x1, y1, x2, y2, size, turn, look != null ? look : MenuAvatar.ownLook());
			return;
		}
		renderPlayer(g, x1, y1, x2, y2, size, turn, look);
	}

	private static float facing() {
		String sub = Cosmetics.previewSub;
		if ("cape".equals(sub) || "wings".equals(sub)) return 180f;
		if ("halo".equals(sub)) return 25f;
		return 0f;
	}

	public static void draw(GuiGraphics g, int x1, int y1, int x2, int y2, int size) {
		Minecraft mc = Minecraft.getInstance();
		Spin s = SPINS.computeIfAbsent(x1 + ":" + y1 + ":" + x2 + ":" + y2, k -> new Spin());
		long now = System.nanoTime();
		float dt = Math.min(0.1f, (now - s.frameAt) / 1e9f);
		s.frameAt = now;

		double scale = mc.getWindow().getGuiScale();
		double mx = mc.mouseHandler.xpos() / scale, my = mc.mouseHandler.ypos() / scale;
		boolean down = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		boolean over = mx >= x1 && mx < x2 && my >= y1 && my < y2;
		if (nudge != 0f) {
			s.target += nudge;
			nudge = 0f;
			forced = null;
			s.touchedAt = System.currentTimeMillis() + 4000;
		}
		if (down && (s.dragging || over)) {
			if (s.dragging) {
				float d = (float) (mx - s.lastMouse) * 1.6f;
				if (Math.abs(d) > 0.01f) { s.target += d; forced = null; }
			}
			s.dragging = true;
			s.touchedAt = System.currentTimeMillis();
		} else {
			s.dragging = false;
		}
		s.lastMouse = mx;

		if (!s.dragging && (forced != null || System.currentTimeMillis() - s.touchedAt > 2500)) {
			float want = forced != null ? forced : facing();
			if (forced == null && want == 0f && Cosmetics.previewSub == null) want = (float) Math.sin(System.currentTimeMillis() / 2400.0) * 25f;
			float diff = ((want - s.target) % 360 + 540) % 360 - 180;
			s.target += diff * (1f - (float) Math.exp(-dt * 6f));
		}
		s.yaw += (s.target - s.yaw) * (1f - (float) Math.exp(-dt * 14f));

		if (mc.player == null) {
			MenuAvatar.render(g, x1, y1, x2, y2, size, s.yaw, MenuAvatar.ownLook());
			return;
		}
		renderPlayer(g, x1, y1, x2, y2, size, s.yaw, null);
	}
}
