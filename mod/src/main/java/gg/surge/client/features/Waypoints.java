package gg.surge.client.features;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.mods.Colors;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Waypoints: saved per server (or singleplayer world) and dimension. Markers are
 * projected onto the HUD with the camera's own angles, so they show through walls.
 */
public final class Waypoints {
	private static final String[] COLORS = {"B483FF", "55FFFF", "55FF55", "FFFF55", "FFAA00", "FF5555", "FF77DD", "FFFFFF"};

	private Waypoints() {}

	/** Which server/world + dimension you're in, e.g. "play.example.net|minecraft:overworld". */
	public static String worldKey(Minecraft mc) {
		if (mc.level == null) return "";
		String where;
		if (mc.getCurrentServer() != null) where = mc.getCurrentServer().ip;
		else if (mc.getSingleplayerServer() != null) where = "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
		else where = "unknown";
		return where + "|" + mc.level.dimension().identifier();
	}

	public static List<SurgeConfig.Waypoint> here(Minecraft mc) {
		List<SurgeConfig.Waypoint> out = new ArrayList<>();
		if (!Mods.WAYPOINTS.enabled()) return out;
		String key = worldKey(mc);
		for (SurgeConfig.Waypoint w : SurgeConfig.get().waypoints) if (w.visible && w.world.equals(key)) out.add(w);
		return out;
	}

	public static void addHere(Minecraft mc) {
		if (mc.player == null) return;
		List<SurgeConfig.Waypoint> all = SurgeConfig.get().waypoints;
		SurgeConfig.Waypoint w = new SurgeConfig.Waypoint();
		w.name = "Waypoint " + (all.size() + 1);
		w.x = mc.player.getBlockX();
		w.y = mc.player.getBlockY();
		w.z = mc.player.getBlockZ();
		w.world = worldKey(mc);
		w.color = COLORS[all.size() % COLORS.length];
		all.add(w);
		SurgeConfig.save();
		if (mc.player != null) mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("Added " + w.name + " at " + w.x + ", " + w.y + ", " + w.z), true);
	}

	public static String nextColor(String c) {
		for (int i = 0; i < COLORS.length; i++) if (COLORS[i].equalsIgnoreCase(c)) return COLORS[(i + 1) % COLORS.length];
		return COLORS[0];
	}

	// ---------- projection (Minecraft's own view vectors, so it matches the camera exactly) ----------
	private static double ex, ey, ez, fx, fy, fz, rx, ry, rz, ux, uy, uz, focal, halfW, halfH;

	private static void setupCamera(GuiGraphics g, Minecraft mc) {
		Camera cam = mc.gameRenderer.getMainCamera();
		Vec3 eye = cam.position();
		ex = eye.x; ey = eye.y; ez = eye.z;
		double yaw = Math.toRadians(cam.yRot()), pitch = Math.toRadians(cam.xRot());
		fx = -Math.sin(yaw) * Math.cos(pitch); fy = -Math.sin(pitch); fz = Math.cos(yaw) * Math.cos(pitch);
		rx = -Math.cos(yaw); ry = 0; rz = -Math.sin(yaw);
		ux = ry * fz - rz * fy; uy = rz * fx - rx * fz; uz = rx * fy - ry * fx;
		halfW = g.guiWidth() / 2.0;
		halfH = g.guiHeight() / 2.0;
		focal = halfH / Math.tan(Math.toRadians(Zoom.lastFov()) / 2.0);
	}

	/** Screen x, y and depth of a world point, or null when it's behind you. */
	private static double[] project(double x, double y, double z) {
		double vx = x - ex, vy = y - ey, vz = z - ez;
		double cz = vx * fx + vy * fy + vz * fz;
		if (cz < 0.05) return null;
		double cx = vx * rx + vy * ry + vz * rz, cy = vx * ux + vy * uy + vz * uz;
		return new double[]{halfW + cx / cz * focal, halfH - cy / cz * focal, cz};
	}

	/** Thin line between two screen points (drawn as tiny fills). */
	private static void line(GuiGraphics g, double[] a, double[] b, int col) {
		if (a == null || b == null) return;
		double dx = b[0] - a[0], dy = b[1] - a[1];
		int steps = (int) Math.min(1200, Math.max(Math.abs(dx), Math.abs(dy)));
		if (steps == 0) return;
		for (int i = 0; i <= steps; i++) {
			int x = (int) Math.round(a[0] + dx * i / steps), y = (int) Math.round(a[1] + dy * i / steps);
			g.fill(x, y, x + 1, y + 1, col);
		}
	}

	private static final int[][] EDGES = {{0, 1}, {1, 3}, {3, 2}, {2, 0}, {4, 5}, {5, 7}, {7, 6}, {6, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

	/** Glowing outline of the waypoint's block, seen through walls. */
	private static void blockGlow(GuiGraphics g, SurgeConfig.Waypoint wp, int rgb, float pulse) {
		double[][] c = new double[8][];
		for (int i = 0; i < 8; i++) c[i] = project(wp.x + (i & 1), wp.y + ((i >> 1) & 1), wp.z + ((i >> 2) & 1));
		int glow = ((int) (70 * pulse) << 24) | rgb, core = ((int) (230 * pulse) << 24) | rgb;
		for (int[] e : EDGES) {
			double[] a = c[e[0]], b = c[e[1]];
			if (a == null || b == null) continue;
			line(g, new double[]{a[0] + 1, a[1]}, new double[]{b[0] + 1, b[1]}, glow);
			line(g, new double[]{a[0], a[1] + 1}, new double[]{b[0], b[1] + 1}, glow);
			line(g, a, b, core);
		}
	}

	/** Creates (or moves) the "Death" waypoint when you die. */
	private static boolean wasDead;

	public static void tick(Minecraft mc) {
		if (mc.player == null) { wasDead = false; return; }
		boolean dead = mc.player.isDeadOrDying();
		if (dead && !wasDead && Mods.WAYPOINTS.enabled() && Mods.WP_DEATH.on()) {
			String key = worldKey(mc);
			SurgeConfig.get().waypoints.removeIf(w -> "Death".equals(w.name) && w.world.equals(key));
			SurgeConfig.Waypoint w = new SurgeConfig.Waypoint();
			w.name = "Death";
			w.x = mc.player.getBlockX();
			w.y = mc.player.getBlockY();
			w.z = mc.player.getBlockZ();
			w.world = key;
			w.color = "FF5555";
			SurgeConfig.get().waypoints.add(w);
			SurgeConfig.save();
		}
		wasDead = dead;
	}

	/** Draws the in-world markers on the HUD. */
	public static void render(GuiGraphics g, Minecraft mc) {
		if (!Mods.WAYPOINTS.enabled() || !Mods.WP_WORLD.on() || mc.player == null || mc.level == null) return;
		List<SurgeConfig.Waypoint> list = here(mc);
		if (list.isEmpty()) return;
		setupCamera(g, mc);
		double range = Mods.WP_RANGE.get();
		float pulse = 0.75f + 0.25f * (float) Math.sin(System.currentTimeMillis() / 400.0);
		float labelScale = Mods.WP_SCALE.get() / 100f;

		for (SurgeConfig.Waypoint wp : list) {
			double cx = wp.x + 0.5, cy = wp.y + 0.5, cz = wp.z + 0.5;
			double dist = Math.sqrt((cx - ex) * (cx - ex) + (cy - ey) * (cy - ey) + (cz - ez) * (cz - ez));
			if (dist > range) continue;
			int rgb = Colors.parse(wp.color) & 0xFFFFFF;
			int col = 0xFF000000 | rgb;

			if (Mods.WP_BEAM.on()) {
				double[] base = project(cx, wp.y + 1, cz), top = project(cx, wp.y + 1 + Math.max(40, dist * 0.8), cz);
				if (base != null && top != null) {
					double dx = top[0] - base[0], dy = top[1] - base[1];
					int steps = (int) Math.min(800, Math.max(Math.abs(dx), Math.abs(dy)));
					for (int i = 0; i <= steps; i++) {
						float fade = 1f - i / (float) Math.max(1, steps);
						int x = (int) Math.round(base[0] + dx * i / Math.max(1, steps)), y = (int) Math.round(base[1] + dy * i / Math.max(1, steps));
						g.fill(x - 1, y, x + 2, y + 1, ((int) (120 * fade * pulse) << 24) | rgb);
					}
				}
			}
			if (Mods.WP_GLOW.on() && dist <= Mods.WP_GLOW_RANGE.get()) blockGlow(g, wp, rgb, pulse);

			double[] p = project(cx, wp.y + 1.3, cz);
			if (p == null) continue;
			int sx = (int) Math.round(p[0]), sy = (int) Math.round(p[1]);
			if (sx < -60 || sx > g.guiWidth() + 60 || sy < -30 || sy > g.guiHeight() + 30) continue;

			switch (Mods.WP_MARKER.get()) {
				case "Dot" -> { g.fill(sx - 2, sy - 2, sx + 3, sy + 3, 0xC0000000); g.fill(sx - 1, sy - 1, sx + 2, sy + 2, col); }
				case "Pin" -> {
					g.fill(sx - 3, sy - 9, sx + 4, sy - 3, 0xC0000000);
					g.fill(sx - 2, sy - 8, sx + 3, sy - 4, col);
					g.fill(sx, sy - 3, sx + 1, sy + 1, col);
				}
				case "None" -> { }
				default -> {
					for (int i = 0; i < 4; i++) g.fill(sx - i, sy - 3 + i, sx + i + 1, sy - 2 + i, col);
					for (int i = 0; i < 3; i++) g.fill(sx - 2 + i, sy + 1 + i, sx + 3 - i, sy + 2 + i, col);
				}
			}

			boolean showLabel = switch (Mods.WP_LABEL.get()) {
				case "Always" -> true;
				case "When near" -> dist <= Mods.WP_NEAR.get();
				case "When looking" -> Math.abs(sx - halfW) < 40 && Math.abs(sy - halfH) < 30;
				default -> false;
			};
			if (!showLabel) continue;
			String label = wp.name + (Mods.WP_DISTANCE.on() ? "  " + Math.round(dist) + "m" : "");
			int tw = mc.font.width(label);
			g.pose().pushMatrix();
			g.pose().translate(sx, sy + 6);
			g.pose().scale(labelScale, labelScale);
			g.fill(-tw / 2 - 4, 0, tw / 2 + 4, 12, 0xA0000000);
			g.fill(-tw / 2 - 4, 0, tw / 2 + 4, 1, col);
			g.drawString(mc.font, label, -tw / 2, 2, 0xFFFFFFFF, false);
			g.pose().popMatrix();
		}
	}
}
