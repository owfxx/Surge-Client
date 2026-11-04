package gg.surge.client.hud;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.features.CombatStats;
import gg.surge.client.features.ServerRegions;
import gg.surge.client.features.Waypoints;
import gg.surge.client.mods.Colors;
import gg.surge.client.mods.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;

final class ExtraModules {
	private ExtraModules() {}

	static int kills, deaths, streak;
	private static boolean wasDead;
	private static LivingEntity lastTarget;
	private static boolean targetCounted;
	private static final int[] CPS = new int[60];
	private static long lastCpsSample;

	static List<HudModule> create() {
		List<HudModule> out = new ArrayList<>();
		out.add(new HitDelay());
		out.add(new CpsGraph());
		out.add(new KillCounter());
		out.add(new HudModules.TextModule("blocks", "Block Counter", Items.BRICKS, "How many blocks you're carrying, for bridging", false, 0.005f, 0.685f, "blocks", "After", "128", mc -> {
			int n = 0;
			var inv = mc.player.getInventory();
			for (int i = 0; i < inv.getContainerSize(); i++) {
				ItemStack s = inv.getItem(i);
				if (s.getItem() instanceof BlockItem) n += s.getCount();
			}
			return String.valueOf(n);
		}));
		out.add(new InventoryHud());
		out.add(new Minimap());
		out.add(new ServerInfo());
		return out;
	}

	static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (p == null) return;
		boolean dead = p.isDeadOrDying();
		if (dead && !wasDead) { deaths++; streak = 0; }
		wasDead = dead;
		Entity t = CombatStats.target();
		if (t instanceof LivingEntity le && le != lastTarget) { lastTarget = le; targetCounted = false; }
		if (lastTarget != null && !targetCounted && lastTarget.isDeadOrDying()) {
			targetCounted = true;
			if (lastTarget instanceof Player || KillCounter.mobs()) { kills++; streak++; }
		}
		long now = System.currentTimeMillis();
		if (now - lastCpsSample >= 100) {
			lastCpsSample = now;
			System.arraycopy(CPS, 1, CPS, 0, CPS.length - 1);
			CPS[CPS.length - 1] = HudModules.leftCps();
		}
	}

	static final class HitDelay extends HudModule {
		final Setting.Choice style = choice("style", "Style", "Bar", "Bar", "Percent");

		HitDelay() { super("hitdelay", "Hit Delay", Items.WOODEN_SWORD, "Shows when your next hit will do full damage", false, 0.46f, 0.56f); }

		private float value(Minecraft mc) { return mc.player == null ? 0.7f : mc.player.getAttackStrengthScale(0f); }

		@Override public int width(Minecraft mc) { return style.is("Bar") ? 60 : boxWidth(mc, "100%"); }

		@Override public int height(Minecraft mc) { return style.is("Bar") ? 6 : 13; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			float v = value(mc);
			if (style.is("Percent")) { box(g, mc, x, y, width(mc), Math.round(v * 100) + "%"); return; }
			g.fill(x, y, x + 60, y + 6, bgOn() ? bg() : 0x60000000);
			int col = v >= 1f ? 0xFF6BFF8A : Colors.lerp(0xFFFF6B6B, 0xFFFFE07A, v);
			g.fill(x + 1, y + 1, x + 1 + Math.round(58 * v), y + 5, col);
		}
	}

	static final class CpsGraph extends HudModule {
		final Setting.Color line = color("line", "Bar color", "B483FF");

		CpsGraph() { super("cpsgraph", "CPS Graph", Items.LEVER, "Your clicks per second over the last few seconds", false, 0.005f, 0.73f); }

		@Override public int width(Minecraft mc) { return 92; }

		@Override public int height(Minecraft mc) { return 34; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			if (bgOn()) g.fill(x, y, x + 92, y + 34, bg());
			int max = 12;
			for (int v : CPS) max = Math.max(max, v);
			for (int i = 0; i < CPS.length; i++) {
				int v = mc.player == null ? (int) (6 + 4 * Math.sin(i * 0.4)) : CPS[i];
				int h = Math.round(22f * v / max);
				int bx = x + 2 + i * 3 / 2;
				g.fill(bx, y + 30 - h, bx + 1, y + 30, 0xFF000000 | line.rgb(i * 0.02f));
			}
			text(g, mc, (mc.player == null ? 8 : HudModules.leftCps()) + " CPS", x + 3, y + 2, fg());
		}
	}

	static final class KillCounter extends HudModule {
		static Setting.Toggle mobsSetting;
		final Setting.Toggle showStreak = toggle("streak", "Show kill streak", true);
		final Setting.Toggle showKd = toggle("kd", "Show K/D", true);

		KillCounter() {
			super("kills", "Kills and Deaths", Items.SKELETON_SKULL, "Kills, deaths and your streak this session", false, 0.94f, 0.24f);
			mobsSetting = toggle("mobs", "Count mob kills", false);
			button("Reset", "Reset", () -> { kills = 0; deaths = 0; streak = 0; });
		}

		static boolean mobs() { return mobsSetting != null && mobsSetting.on(); }

		private String line() {
			String s = "K " + kills + "  D " + deaths;
			if (showKd.on()) s += "  " + String.format("%.2f", deaths == 0 ? kills : kills / (float) deaths);
			if (showStreak.on() && streak > 1) s += "  Streak " + streak;
			return s;
		}

		@Override public int width(Minecraft mc) { return boxWidth(mc, line()); }

		@Override public int height(Minecraft mc) { return 13; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) { box(g, mc, x, y, line()); }
	}

	static final class InventoryHud extends HudModule {
		final Setting.Toggle hotbar = toggle("hotbar", "Include hotbar", false);

		InventoryHud() { super("inventory", "Inventory HUD", Items.CHEST, "Shows what's in your inventory without opening it", false, 0.70f, 0.80f); }

		private int rows() { return hotbar.on() ? 4 : 3; }

		@Override public int width(Minecraft mc) { return 9 * 18 + 4; }

		@Override public int height(Minecraft mc) { return rows() * 18 + 4; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			if (bgOn()) g.fill(x, y, x + width(mc), y + height(mc), bg());
			if (mc.player == null) return;
			var inv = mc.player.getInventory();
			for (int r = 0; r < rows(); r++) {
				for (int c = 0; c < 9; c++) {
					int slot = r < 3 ? 9 + r * 9 + c : c;
					int sx = x + 2 + c * 18, sy = y + 2 + r * 18;
					g.fill(sx, sy, sx + 17, sy + 17, 0x30FFFFFF);
					ItemStack s = inv.getItem(slot);
					if (!s.isEmpty()) {
						g.renderItem(s, sx, sy);
						g.renderItemDecorations(mc.font, s, sx, sy);
					}
				}
			}
		}
	}

	static final class ServerInfo extends HudModule {
		final Setting.Toggle region = toggle("region", "Show region", true);
		final Setting.Toggle players = toggle("players", "Show player count", true);

		ServerInfo() { super("serverinfo", "Server Info", Items.BEACON, "Server address, ping, players and where it's hosted", false, 0.94f, 0.29f); }

		private List<String> lines(Minecraft mc) {
			List<String> out = new ArrayList<>();
			if (mc.player == null || mc.getConnection() == null) {
				out.add("play.example.net");
				out.add("32 ms  12 online");
				if (region.on()) out.add("Europe, DE");
				return out;
			}
			String ip = mc.getCurrentServer() != null ? mc.getCurrentServer().ip : "Singleplayer";
			out.add(ip);
			PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
			String second = (info == null ? 0 : info.getLatency()) + " ms";
			if (players.on()) second += "  " + mc.getConnection().getOnlinePlayers().size() + " online";
			out.add(second);
			if (region.on() && mc.getCurrentServer() != null) {
				String r = ServerRegions.get(ip);
				if (r != null && !r.isEmpty()) out.add(r);
			}
			return out;
		}

		@Override public int width(Minecraft mc) {
			int w = 0;
			for (String s : lines(mc)) w = Math.max(w, mc.font.width(s));
			return w + 10;
		}

		@Override public int height(Minecraft mc) { return lines(mc).size() * 10 + 4; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			List<String> ls = lines(mc);
			if (bgOn()) g.fill(x, y, x + width(mc), y + height(mc), bg());
			for (int i = 0; i < ls.size(); i++) text(g, mc, ls.get(i), x + 5, y + 3 + i * 10, i == 0 ? fg() : Colors.alpha(fg(), 0.75f));
		}
	}

	static final class Minimap extends HudModule {
		final Setting.Slider size = slider("size", "Size", 96, 64, 192, 8, "px");
		final Setting.Choice zoom = choice("zoom", "Zoom", "1x", "0.5x", "1x", "2x", "4x");
		final Setting.Choice shape = choice("shape", "Shape", "Square", "Square", "Round");
		final Setting.Toggle showWaypoints = toggle("waypoints", "Show waypoints", true);
		final Setting.Toggle showCoords = toggle("coords", "Show coordinates", true);

		private int[] colors = new int[0];
		private int cacheN, cacheX, cacheZ, cacheScale;
		private long cacheAt;

		Minimap() { super("minimap", "Minimap", Items.MAP, "A live map of the area around you", false, 0.84f, 0.02f); }

		private int blocksPerSample() {
			return switch (zoom.get()) { case "0.5x" -> 4; case "2x" -> 1; case "4x" -> 1; default -> 2; };
		}

		private int px() { return zoom.is("4x") ? 3 : zoom.is("2x") ? 2 : 1; }

		@Override public int width(Minecraft mc) { return size.asInt(); }

		@Override public int height(Minecraft mc) { return size.asInt() + (showCoords.on() ? 12 : 0); }

		private void rebuild(Minecraft mc, int n, int sc) {
			LocalPlayer p = mc.player;
			int ox = p.getBlockX(), oz = p.getBlockZ();
			int[] out = new int[n * n];
			int[] heights = new int[n * n];
			boolean ceiling = mc.level.dimensionType().hasCeiling();
			BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
			for (int j = 0; j < n; j++) {
				for (int i = 0; i < n; i++) {
					int wx = ox + (i - n / 2) * sc, wz = oz + (j - n / 2) * sc;
					int top;
					if (ceiling) {
						top = p.getBlockY() + 1;
						pos.set(wx, top, wz);
						while (top > mc.level.getMinY() && mc.level.getBlockState(pos).isAir()) { top--; pos.setY(top); }
					} else {
						top = mc.level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
						pos.set(wx, top, wz);
					}
					BlockState st = mc.level.getBlockState(pos);
					MapColor mcol = st.getMapColor(mc.level, pos);
					int col = mcol == MapColor.NONE ? 0x101014 : mcol.col;
					out[j * n + i] = col;
					heights[j * n + i] = top;
				}
			}
			for (int j = 1; j < n; j++) {
				for (int i = 0; i < n; i++) {
					int d = heights[j * n + i] - heights[(j - 1) * n + i];
					int c = out[j * n + i];
					out[j * n + i] = d > 0 ? Colors.lerp(0xFF000000 | c, 0xFFFFFFFF, 0.12f) & 0xFFFFFF : d < 0 ? Colors.lerp(0xFF000000 | c, 0xFF000000, 0.2f) & 0xFFFFFF : c;
				}
			}
			colors = out;
			cacheN = n; cacheX = ox; cacheZ = oz; cacheScale = sc;
			cacheAt = System.currentTimeMillis();
		}

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			int s = size.asInt(), ppx = px(), sc = blocksPerSample();
			int n = s / ppx;
			boolean round = shape.is("Round");
			if (mc.player == null || mc.level == null) {
				g.fill(x, y, x + s, y + s, 0xFF3B6B2A);
				return;
			}
			LocalPlayer p = mc.player;
			boolean moved = Math.abs(p.getBlockX() - cacheX) >= sc * 2 || Math.abs(p.getBlockZ() - cacheZ) >= sc * 2;
			if (cacheN != n || cacheScale != sc || moved || System.currentTimeMillis() - cacheAt > 1000) rebuild(mc, n, sc);
			int r2 = (s / 2) * (s / 2);
			g.fill(x - 1, y - 1, x + s + 1, y + s + 1, 0xFF0C0C0D);
			for (int j = 0; j < n; j++) {
				int runStart = -1, runCol = 0;
				for (int i = 0; i <= n; i++) {
					int col = -1;
					if (i < n) {
						int dx = i * ppx + ppx / 2 - s / 2, dy = j * ppx + ppx / 2 - s / 2;
						if (!round || dx * dx + dy * dy <= r2) col = colors[j * n + i];
					}
					if (col != runCol || col == -1) {
						if (runStart >= 0 && runCol != -1) g.fill(x + runStart * ppx, y + j * ppx, x + i * ppx, y + (j + 1) * ppx, 0xFF000000 | runCol);
						runStart = col == -1 ? -1 : i;
						runCol = col;
					}
				}
			}
			int cx = x + s / 2, cy = y + s / 2;
			double perPx = sc / (double) ppx;
			if (showWaypoints.on()) {
				for (SurgeConfig.Waypoint wp : Waypoints.here(mc)) {
					int wx = cx + (int) Math.round((wp.x + 0.5 - p.getX()) / perPx), wy = cy + (int) Math.round((wp.z + 0.5 - p.getZ()) / perPx);
					if (!inside(wx - cx, wy - cy, s, round)) continue;
					g.fill(wx - 2, wy - 2, wx + 2, wy + 2, 0xFF000000 | Colors.parse(wp.color));
				}
			}
			double yaw = Math.toRadians(p.getYRot());
			double fx = -Math.sin(yaw), fz = Math.cos(yaw);
			for (int k = 0; k <= 5; k++) {
				int ax = cx + (int) Math.round(fx * k), ay = cy + (int) Math.round(fz * k);
				g.fill(ax - (k < 3 ? 1 : 0), ay - (k < 3 ? 1 : 0), ax + 1, ay + 1, 0xFFFF4040);
			}
			g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
			g.drawString(mc.font, "N", cx - 2, y + 2, 0xFFFFFFFF, true);
			if (showCoords.on()) {
				String c = p.getBlockX() + ", " + p.getBlockY() + ", " + p.getBlockZ();
				g.drawString(mc.font, c, cx - mc.font.width(c) / 2, y + s + 3, fg(), shadowOn());
			}
		}

		private static boolean inside(int dx, int dy, int s, boolean round) {
			int h = s / 2 - 2;
			return round ? dx * dx + dy * dy <= h * h : Math.abs(dx) <= h && Math.abs(dy) <= h;
		}
	}
}
