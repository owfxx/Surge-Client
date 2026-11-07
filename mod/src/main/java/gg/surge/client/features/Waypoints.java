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

public final class Waypoints {
	private static final String[] COLORS = {"B483FF", "55FFFF", "55FF55", "FFFF55", "FFAA00", "FF5555", "FF77DD", "FFFFFF"};

	private Waypoints() {}

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

	private static double ex, ey, ez, fx, fy, fz, rx, ry, rz, ux, uy, uz, focal, halfW, halfH;

	static void setupCamera(GuiGraphics g, Minecraft mc) {
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

	static double[] project(double x, double y, double z) {
		double vx = x - ex, vy = y - ey, vz = z - ez;
		double cz = vx * fx + vy * fy + vz * fz;
		if (cz < 0.05) return null;
		double cx = vx * rx + vy * ry + vz * rz, cy = vx * ux + vy * uy + vz * uz;
		return new double[]{halfW + cx / cz * focal, halfH - cy / cz * focal, cz};
	}

	private static void line(GuiGraphics g, double[] a, double[] b, int col, float width) {
		if (a == null || b == null) return;
		double x0 = a[0], y0 = a[1], x1 = b[0], y1 = b[1];
		double w = halfW * 2, h = halfH * 2, t0 = 0, t1 = 1, dx = x1 - x0, dy = y1 - y0;
		double[] pp = {-dx, dx, -dy, dy}, qq = {x0 + 50, w + 50 - x0, y0 + 50, h + 50 - y0};
		for (int i = 0; i < 4; i++) {
			if (pp[i] == 0) { if (qq[i] < 0) return; continue; }
			double r = qq[i] / pp[i];
			if (pp[i] < 0) { if (r > t1) return; if (r > t0) t0 = r; }
			else { if (r < t0) return; if (r < t1) t1 = r; }
		}
		double sx = x0 + dx * t0, sy = y0 + dy * t0, len = Math.hypot(dx, dy) * (t1 - t0);
		if (len < 0.5) return;
		g.pose().pushMatrix();
		g.pose().translate((float) sx, (float) sy);
		g.pose().rotate((float) Math.atan2(dy, dx));
		g.pose().translate(0f, -width / 2f);
		g.pose().scale((float) len, width);
		g.fill(0, 0, 1, 1, col);
		g.pose().popMatrix();
	}

	private static double[][] clip3(double[] p, double[] q) {
		double nz = 0.06;
		double pz = (p[0] - ex) * fx + (p[1] - ey) * fy + (p[2] - ez) * fz, qz = (q[0] - ex) * fx + (q[1] - ey) * fy + (q[2] - ez) * fz;
		if (pz < nz && qz < nz) return null;
		if (pz < nz) { double t = (nz - pz) / (qz - pz); p = new double[]{p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t, p[2] + (q[2] - p[2]) * t}; }
		else if (qz < nz) { double t = (nz - qz) / (pz - qz); q = new double[]{q[0] + (p[0] - q[0]) * t, q[1] + (p[1] - q[1]) * t, q[2] + (p[2] - q[2]) * t}; }
		return new double[][]{project(p[0], p[1], p[2]), project(q[0], q[1], q[2])};
	}

	private static final int[][] EDGES = {{0, 1}, {1, 3}, {3, 2}, {2, 0}, {4, 5}, {5, 7}, {7, 6}, {6, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};

	private static void blockGlow(GuiGraphics g, SurgeConfig.Waypoint wp, int rgb, float pulse) {
		int glow = ((int) (70 * pulse) << 24) | rgb, core = ((int) (230 * pulse) << 24) | rgb;
		for (int[] e : EDGES) {
			double[] p = {wp.x + (e[0] & 1), wp.y + ((e[0] >> 1) & 1), wp.z + ((e[0] >> 2) & 1)};
			double[] q = {wp.x + (e[1] & 1), wp.y + ((e[1] >> 1) & 1), wp.z + ((e[1] >> 2) & 1)};
			double[][] ab = clip3(p, q);
			if (ab == null || ab[0] == null || ab[1] == null) continue;
			line(g, ab[0], ab[1], glow, 3f);
			line(g, ab[0], ab[1], core, 1f);
		}
	}

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
				double topY = wp.y + 1 + Math.max(40, dist * 0.8);
				for (int k = 0; k < 10; k++) {
					double ya = wp.y + 1 + (topY - wp.y - 1) * k / 10.0, yb = wp.y + 1 + (topY - wp.y - 1) * (k + 1) / 10.0;
					double[][] ab = clip3(new double[]{cx, ya, cz}, new double[]{cx, yb, cz});
					if (ab == null || ab[0] == null || ab[1] == null) continue;
					line(g, ab[0], ab[1], ((int) (120 * (1f - k / 10f) * pulse) << 24) | rgb, 3f);
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
