package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public final class MoreFeatures {
	private MoreFeatures() {}

	private static CameraType snapSaved, autoSaved;
	private static boolean snapping, autoActive;
	private static int lastHurt;
	private static float hitAngle;
	private static long hitAt;
	public static ServerAddress lastAddress;
	public static ServerData lastServer;

	public static void tick(Minecraft mc, boolean snapKey) {
		if (mc.player == null) {
			snapping = false;
			autoActive = false;
			return;
		}
		boolean wantSnap = Mods.SNAPLOOK.enabled() && snapKey && mc.screen == null;
		if (wantSnap && !snapping) {
			snapping = true;
			snapSaved = mc.options.getCameraType();
			mc.options.setCameraType(Mods.SL_VIEW.is("Back") ? CameraType.THIRD_PERSON_BACK : CameraType.THIRD_PERSON_FRONT);
		} else if (!wantSnap && snapping) {
			snapping = false;
			if (snapSaved != null) mc.options.setCameraType(snapSaved);
		}
		if (!snapping) {
			boolean want = Mods.AUTO_PERSPECTIVE.enabled() && (Mods.AP_ELYTRA.on() && mc.player.isFallFlying() || Mods.AP_RIDING.on() && mc.player.isPassenger());
			if (want && !autoActive) {
				autoActive = true;
				autoSaved = mc.options.getCameraType();
				if (autoSaved == CameraType.FIRST_PERSON) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			} else if (!want && autoActive) {
				autoActive = false;
				if (autoSaved != null && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) mc.options.setCameraType(autoSaved);
			}
		}
		int h = mc.player.hurtTime;
		if (h > 0 && lastHurt == 0) {
			hitAngle = mc.player.getHurtDir();
			hitAt = System.currentTimeMillis();
		}
		lastHurt = h;
	}

	public static void render(GuiGraphics g, Minecraft mc) {
		if (mc.player == null || mc.level == null || mc.options.hideGui) return;
		boolean world = Mods.MOB_HEALTH.enabled() || Mods.LOOT_BEAMS.enabled() || Mods.DESPAWN_TIMER.enabled() || Mods.LIGHT_OVERLAY.enabled();
		if (world) {
			Waypoints.setupCamera(g, mc);
			if (Mods.LIGHT_OVERLAY.enabled()) lightOverlay(g, mc);
			if (Mods.LOOT_BEAMS.enabled() || Mods.DESPAWN_TIMER.enabled()) items(g, mc);
			if (Mods.MOB_HEALTH.enabled()) mobHealth(g, mc);
		}
		if (Mods.HIT_INDICATOR.enabled()) hitIndicator(g);
	}

	private static void hitIndicator(GuiGraphics g) {
		long age = System.currentTimeMillis() - hitAt;
		if (age > 900) return;
		float a = 1f - age / 900f;
		int rgb = Mods.HI_COLOR.rgb(0) & 0xFFFFFF;
		float cx = g.guiWidth() / 2f, cy = g.guiHeight() / 2f;
		double ang = Math.toRadians(hitAngle);
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().rotate((float) -ang + (float) Math.PI);
		for (int i = 0; i < 9; i++) {
			int w = 18 - i * 2;
			int al = (int) (220 * a * (1f - i / 10f));
			g.fill(-w, -44 - i, w, -43 - i, (al << 24) | rgb);
		}
		g.pose().popMatrix();
	}

	private static boolean sees(Minecraft mc, Entity e) {
		return mc.player.hasLineOfSight(e);
	}

	private static void label(GuiGraphics g, Minecraft mc, double x, double y, double z, String text, int color, int edge) {
		double[] p = Waypoints.project(x, y, z);
		if (p == null) return;
		int sx = (int) Math.round(p[0]), sy = (int) Math.round(p[1]);
		if (sx < -40 || sy < -20 || sx > g.guiWidth() + 40 || sy > g.guiHeight() + 20) return;
		float k = (float) Math.max(0.5, Math.min(1.0, 8.0 / Math.max(1.0, p[2])));
		int tw = mc.font.width(text);
		g.pose().pushMatrix();
		g.pose().translate(sx, sy);
		g.pose().scale(k, k);
		g.fill(-tw / 2 - 3, -2, tw / 2 + 3, 10, 0x90000000);
		if (edge != 0) g.fill(-tw / 2 - 3, -2, tw / 2 + 3, -1, edge);
		g.drawString(mc.font, text, -tw / 2, 0, color, false);
		g.pose().popMatrix();
	}

	private static void mobHealth(GuiGraphics g, Minecraft mc) {
		double r = Mods.MH_RANGE.get();
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive() || le.isInvisible()) continue;
			if (e instanceof Player && !Mods.MH_PLAYERS.on()) continue;
			if (e instanceof net.minecraft.world.entity.decoration.ArmorStand) continue;
			if (e.distanceToSqr(mc.player) > r * r) continue;
			float hp = le.getHealth(), max = Math.max(1f, le.getMaxHealth());
			if (Mods.MH_HURT.on() && hp >= max) continue;
			if (!sees(mc, e)) continue;
			double y = e.getY() + e.getBbHeight() + (e.hasCustomName() || e instanceof Player ? 0.75 : 0.35);
			float pct = hp / max;
			int col = pct > 0.6f ? 0xFF6BFF8A : pct > 0.3f ? 0xFFFFE07A : 0xFFFF6B6B;
			switch (Mods.MH_STYLE.get()) {
				case "Number" -> label(g, mc, e.getX(), y, e.getZ(), String.format(java.util.Locale.ROOT, "%.1f / %.0f", hp, max), col, 0);
				case "Bar" -> bar(g, e.getX(), y, e.getZ(), pct, col);
				default -> label(g, mc, e.getX(), y, e.getZ(), "❤ " + String.format(java.util.Locale.ROOT, "%.1f", hp / 2f), 0xFFFF5A5A, 0);
			}
		}
	}

	private static void bar(GuiGraphics g, double x, double y, double z, float pct, int col) {
		double[] p = Waypoints.project(x, y, z);
		if (p == null) return;
		float k = (float) Math.max(0.5, Math.min(1.0, 8.0 / Math.max(1.0, p[2])));
		g.pose().pushMatrix();
		g.pose().translate((float) p[0], (float) p[1]);
		g.pose().scale(k, k);
		g.fill(-21, -1, 21, 5, 0xB0000000);
		g.fill(-20, 0, -20 + Math.round(40 * Math.max(0f, Math.min(1f, pct))), 4, col);
		g.pose().popMatrix();
	}

	private static int rarityColor(ItemStack s) {
		Rarity r = s.getRarity();
		if (r == Rarity.EPIC) return 0xFF55FF;
		if (r == Rarity.RARE) return 0x55FFFF;
		if (r == Rarity.UNCOMMON || s.isEnchanted()) return 0xFFFF55;
		return 0xFFFFFF;
	}

	private static void items(GuiGraphics g, Minecraft mc) {
		double beam = Mods.LOOT_BEAMS.enabled() ? Mods.LB_RANGE.get() : 0, timer = Mods.DESPAWN_TIMER.enabled() ? Mods.DT_RANGE.get() : 0;
		double max = Math.max(beam, timer);
		float pulse = 0.75f + 0.25f * (float) Math.sin(System.currentTimeMillis() / 350.0);
		for (Entity e : mc.level.entitiesForRendering()) {
			if (!(e instanceof ItemEntity it)) continue;
			double d2 = e.distanceToSqr(mc.player);
			if (d2 > max * max || !sees(mc, e)) continue;
			ItemStack s = it.getItem();
			int rgb = rarityColor(s);
			if (beam > 0 && d2 <= beam * beam && (!Mods.LB_RARE.on() || rgb != 0xFFFFFF)) {
				double[] a = Waypoints.project(e.getX(), e.getY() + 0.15, e.getZ()), b = Waypoints.project(e.getX(), e.getY() + 3.0, e.getZ());
				if (a != null && b != null) {
					double dx = b[0] - a[0], dy = b[1] - a[1], len = Math.hypot(dx, dy);
					if (len > 1) {
						float w = (float) Math.max(1.5, 0.14 * Waypoints.focal() / Math.max(1.0, a[2]));
						int base = (int) (170 * pulse) << 24 | rgb, top = rgb;
						g.pose().pushMatrix();
						g.pose().translate((float) a[0], (float) a[1]);
						g.pose().rotate((float) (Math.atan2(dy, dx) - Math.PI / 2));
						int hw = Math.max(1, Math.round(w / 2));
						g.fillGradient(-hw, 0, hw, (int) Math.round(len), base, top);
						g.fillGradient(-Math.max(1, hw / 3), 0, Math.max(1, hw / 3), (int) Math.round(len * 0.8), (int) (200 * pulse) << 24 | 0xFFFFFF, 0x00FFFFFF);
						g.pose().popMatrix();
					}
				}
			}
			if (timer > 0 && d2 <= timer * timer) {
				int left = Math.max(0, 6000 - it.getAge()) / 20;
				String t = left >= 60 ? (left / 60) + ":" + String.format("%02d", left % 60) : left + "s";
				int col = left > 60 ? 0xFFFFFFFF : left > 15 ? 0xFFFFE07A : 0xFFFF6B6B;
				label(g, mc, e.getX(), e.getY() + 0.75, e.getZ(), t, col, 0);
			}
		}
	}

	private static void lightOverlay(GuiGraphics g, Minecraft mc) {
		int r = Mods.LO_RANGE.asInt();
		BlockPos c = mc.player.blockPosition();
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(), below = new BlockPos.MutableBlockPos();
		int drawn = 0;
		for (int dx = -r; dx <= r && drawn < 600; dx++) for (int dz = -r; dz <= r && drawn < 600; dz++) for (int dy = -4; dy <= 3; dy++) {
			pos.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
			below.set(pos.getX(), pos.getY() - 1, pos.getZ());
			BlockState here = mc.level.getBlockState(pos), ground = mc.level.getBlockState(below);
			if (!here.getCollisionShape(mc.level, pos).isEmpty() || !here.getFluidState().isEmpty()) continue;
			if (!ground.isFaceSturdy(mc.level, below, net.minecraft.core.Direction.UP)) continue;
			int block = mc.level.getBrightness(LightLayer.BLOCK, pos);
			int sky = Mods.LO_DAY.on() ? mc.level.getBrightness(LightLayer.SKY, pos) : 0;
			int light = Math.max(block, sky);
			if (light > 0 && !Mods.LO_NUMBERS.on()) continue;
			double[] p = Waypoints.project(pos.getX() + 0.5, pos.getY() + 0.02, pos.getZ() + 0.5);
			if (p == null) continue;
			int sx = (int) Math.round(p[0]), sy = (int) Math.round(p[1]);
			if (sx < 0 || sy < 0 || sx > g.guiWidth() || sy > g.guiHeight()) continue;
			drawn++;
			int col = light == 0 ? 0xFFFF4D4D : light < 8 ? 0xFFFFC14D : 0xFF6BFF8A;
			if (Mods.LO_NUMBERS.on()) {
				String n = String.valueOf(light);
				float k = (float) Math.max(0.45, Math.min(0.9, 6.0 / Math.max(1.0, p[2])));
				g.pose().pushMatrix();
				g.pose().translate(sx, sy);
				g.pose().scale(k, k);
				g.drawString(mc.font, n, -mc.font.width(n) / 2, -4, col, true);
				g.pose().popMatrix();
			} else {
				int s = (int) Math.max(1, Math.round(5.0 / Math.max(1.0, p[2]) * 2));
				g.fill(sx - s, sy - s, sx + s + 1, sy + s + 1, (0xB0 << 24) | (col & 0xFFFFFF));
			}
		}
	}

	public static boolean hideToast(Object toast) {
		if (!Mods.TOASTS.enabled()) return false;
		if (toast instanceof net.minecraft.client.gui.components.toasts.AdvancementToast) return Mods.TOAST_ADV.on();
		if (toast instanceof net.minecraft.client.gui.components.toasts.RecipeToast) return Mods.TOAST_RECIPE.on();
		if (toast instanceof net.minecraft.client.gui.components.toasts.TutorialToast) return Mods.TOAST_TUTORIAL.on();
		if (toast instanceof net.minecraft.client.gui.components.toasts.SystemToast) return Mods.TOAST_SYSTEM.on();
		return false;
	}

	private static java.lang.ref.WeakReference<net.minecraft.client.gui.screens.Screen> rcScreen = new java.lang.ref.WeakReference<>(null);
	private static net.minecraft.client.gui.components.Button rcButton;
	private static long rcShown;

	public static void onScreenInit(Minecraft mc, net.minecraft.client.gui.screens.Screen s) {
		if (!(s instanceof net.minecraft.client.gui.screens.DisconnectedScreen) || !Mods.RECONNECT.enabled() || lastAddress == null) return;
		net.minecraft.client.gui.components.Button b = net.minecraft.client.gui.components.Button.builder(net.minecraft.network.chat.Component.literal("Reconnect"), x -> reconnect(mc)).bounds(s.width / 2 - 100, s.height - 32, 200, 20).build();
		net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(s).add(b);
		if (rcScreen.get() != s) rcShown = System.currentTimeMillis();
		rcScreen = new java.lang.ref.WeakReference<>(s);
		rcButton = b;
		net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.afterTick(s).register(MoreFeatures::screenTick);
	}

	public static void screenTick(net.minecraft.client.gui.screens.Screen s) {
		if (rcButton == null || rcScreen.get() != s || !Mods.RC_AUTO.on()) return;
		long left = Mods.RC_DELAY.asInt() * 1000L - (System.currentTimeMillis() - rcShown);
		if (left <= 0) {
			rcButton = null;
			reconnect(Minecraft.getInstance());
			return;
		}
		rcButton.setMessage(net.minecraft.network.chat.Component.literal("Reconnect (" + (left / 1000 + 1) + ")"));
	}

	public static void reconnect(Minecraft mc) {
		if (lastAddress == null || lastServer == null) return;
		net.minecraft.client.gui.screens.Screen parent = new net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen(new net.minecraft.client.gui.screens.TitleScreen());
		net.minecraft.client.gui.screens.ConnectScreen.startConnecting(parent, mc, lastAddress, lastServer, false, null);
	}
}
