package gg.surge.client.hud;

import gg.surge.client.gui.HudEditorScreen;
import gg.surge.client.mods.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class MoreModules {
	private MoreModules() {}

	private static long tpsAt;
	private static long tpsGame = -1;
	private static double tps = 20;
	private static long cpuAt;
	private static double cpu;

	static List<HudModule> create() {
		List<HudModule> out = new ArrayList<>();
		out.add(new LookAt());
		out.add(new ImageModule());
		out.add(new MouseStrokes());
		out.add(new Paperdoll());
		out.add(new HorseStats());
		out.add(new HudModules.TextModule("tps", "TPS", Items.REPEATER, "How fast the server is running, 20 is perfect", false, 1f, 0.42f, "TPS", "After", "20.0", mc -> String.format(Locale.ROOT, "%.1f", tps)));
		out.add(new HudModules.TextModule("system", "CPU Usage", Items.OBSERVER, "How much of your processor the game is using", false, 1f, 0.465f, "CPU", "Before", "18%", mc -> Math.round(cpu * 100) + "%"));
		return out;
	}

	static void tick(Minecraft mc) {
		if (mc.player == null || mc.level == null) { tpsGame = -1; return; }
		long now = System.currentTimeMillis(), game = mc.level.getGameTime();
		if (tpsGame < 0) { tpsGame = game; tpsAt = now; }
		else if (now - tpsAt >= 2000) {
			double t = (game - tpsGame) / ((now - tpsAt) / 1000.0);
			tps = Math.max(0, Math.min(20, tps * 0.5 + t * 0.5));
			tpsGame = game;
			tpsAt = now;
		}
		if (now - cpuAt > 1000) {
			cpuAt = now;
			try {
				var os = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
				double v = os.getProcessCpuLoad();
				if (v >= 0) cpu = cpu * 0.4 + v * 0.6;
			} catch (Throwable ignored) { }
		}
	}

	static final class LookAt extends HudModule {
		final Setting.Toggle entities = toggle("entities", "Show mobs too", true);

		LookAt() { super("lookat", "Look At", Items.SPYGLASS, "Shows the name of the block or mob you're looking at", false, 0.5f - 0.06f, 0.075f); }

		private record Info(ItemStack icon, String name, String sub) {}

		private Info info(Minecraft mc) {
			if (mc.player == null || mc.level == null) return new Info(new ItemStack(Items.GRASS_BLOCK), "Grass Block", "minecraft");
			HitResult hr = mc.hitResult;
			if (hr instanceof EntityHitResult eh && entities.on()) {
				Entity e = eh.getEntity();
				String sub = e instanceof LivingEntity le ? String.format(Locale.ROOT, "%.1f / %.0f ❤", le.getHealth() / 2f, le.getMaxHealth() / 2f) : "";
				ItemStack egg = net.minecraft.world.item.SpawnEggItem.byId(e.getType()) != null ? new ItemStack(net.minecraft.world.item.SpawnEggItem.byId(e.getType())) : ItemStack.EMPTY;
				return new Info(egg, e.getDisplayName().getString(), sub);
			}
			if (hr instanceof BlockHitResult bh && hr.getType() == HitResult.Type.BLOCK) {
				BlockState st = mc.level.getBlockState(bh.getBlockPos());
				if (st.isAir()) return null;
				ItemStack icon = new ItemStack(st.getBlock().asItem());
				String ns = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getNamespace();
				return new Info(icon, st.getBlock().getName().getString(), ns.equals("minecraft") ? "" : ns);
			}
			return null;
		}

		@Override public int width(Minecraft mc) {
			Info i = info(mc);
			if (i == null) return 80;
			return Math.max(80, Math.max(mc.font.width(i.name()), mc.font.width(i.sub())) + 30);
		}

		@Override public int height(Minecraft mc) { return 22; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			Info i = info(mc);
			if (i == null) {
				if (mc.screen instanceof HudEditorScreen) i = new Info(new ItemStack(Items.GRASS_BLOCK), "Grass Block", "");
				else return;
			}
			int w = width(mc);
			if (bgOn()) panel(g, x, y, x + w, y + 22);
			if (!i.icon().isEmpty()) g.renderItem(i.icon(), x + 3, y + 3);
			if (i.sub().isEmpty()) text(g, mc, i.name(), x + 23, y + 7, fg());
			else {
				text(g, mc, i.name(), x + 23, y + 3, fg());
				g.drawString(mc.font, i.sub(), x + 23, y + 12, 0xFFA1A1AA, false);
			}
		}
	}

	static final class MouseStrokes extends HudModule {
		final Setting.Color dot = color("dot", "Dot color", "FFFFFF");
		private float mx, my, lastYaw = Float.NaN, lastPitch;
		private final float[][] trail = new float[14][2];
		private long last = System.nanoTime();

		MouseStrokes() { super("mousestrokes", "Mouse Strokes", Items.STRING, "Shows how you move your mouse", false, 1f, 0.55f); }

		@Override public int width(Minecraft mc) { return 48; }

		@Override public int height(Minecraft mc) { return 48; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			long now = System.nanoTime();
			float dt = Math.min(0.1f, (now - last) / 1e9f);
			last = now;
			if (mc.player != null) {
				float yaw = mc.player.getYRot(), pitch = mc.player.getXRot();
				if (!Float.isNaN(lastYaw)) { mx += (yaw - lastYaw) * 0.6f; my += (pitch - lastPitch) * 0.6f; }
				lastYaw = yaw;
				lastPitch = pitch;
			}
			float k = (float) Math.exp(-dt * 8);
			mx *= k;
			my *= k;
			mx = Math.max(-20, Math.min(20, mx));
			my = Math.max(-20, Math.min(20, my));
			System.arraycopy(trail, 0, trail, 1, trail.length - 1);
			trail[0] = new float[]{mx, my};
			if (bgOn()) panel(g, x, y, x + 48, y + 48);
			g.fill(x + 23, y + 6, x + 25, y + 42, 0x30FFFFFF);
			g.fill(x + 6, y + 23, x + 42, y + 25, 0x30FFFFFF);
			int rgb = dot.rgb(0) & 0xFFFFFF;
			for (int i = trail.length - 1; i >= 0; i--) {
				int a = (int) (200 * (1f - i / (float) trail.length)) & 0xFF;
				int px = x + 24 + Math.round(trail[i][0]), py = y + 24 + Math.round(trail[i][1]);
				int r = i == 0 ? 2 : 1;
				g.fill(px - r, py - r, px + r, py + r, (a << 24) | rgb);
			}
		}
	}

	static final class Paperdoll extends HudModule {
		final Setting.Toggle onlyMoving = toggle("moving", "Only while sprinting, swimming or flying", false);

		Paperdoll() { super("paperdoll", "Paperdoll", Items.ARMOR_STAND, "A small model of your character on screen", false, 0.02f, 0.3f); }

		@Override public int width(Minecraft mc) { return 40; }

		@Override public int height(Minecraft mc) { return 66; }

		@Override public void draw(GuiGraphics g, Minecraft mc) {
			if (mc.player == null) return;
			var p = mc.player;
			if (onlyMoving.on() && !(mc.screen instanceof HudEditorScreen) && !(p.isSprinting() || p.isSwimming() || p.isFallFlying() || p.getAbilities().flying)) return;
			int x = screenX(g, mc), y = screenY(g, mc);
			float s = scale();
			int w = Math.round(40 * s), h = Math.round(66 * s);
			if (bgOn()) panel(g, x, y, x + w, y + h);
			InventoryScreen.renderEntityInInventoryFollowsMouse(g, x, y, x + w, y + h, Math.max(8, Math.round(28 * s)), 0.0625f, x + w / 2f + 30, y + h / 2f - 10, p);
		}

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) { }
	}

	static final class HorseStats extends HudModule {
		HorseStats() { super("horse", "Horse Stats", Items.SADDLE, "Speed, jump height and health of the horse you ride or look at", false, 1f, 0.2f); }

		private AbstractHorse horse(Minecraft mc) {
			if (mc.player == null) return null;
			if (mc.player.getVehicle() instanceof AbstractHorse h) return h;
			if (mc.crosshairPickEntity instanceof AbstractHorse h) return h;
			return null;
		}

		private String[] lines(Minecraft mc) {
			AbstractHorse h = horse(mc);
			if (h == null) return new String[]{"Speed  9.5 b/s", "Jump  3.2 blocks", "Health  13 ❤"};
			double speed = h.getAttributeValue(Attributes.MOVEMENT_SPEED) * 42.16;
			double j = h.getAttributeValue(Attributes.JUMP_STRENGTH);
			double jump = -0.1817584952 * j * j * j + 3.689713992 * j * j + 2.128599134 * j - 0.343930367;
			return new String[]{String.format(Locale.ROOT, "Speed  %.1f b/s", speed), String.format(Locale.ROOT, "Jump  %.1f blocks", jump), String.format(Locale.ROOT, "Health  %.0f ❤", h.getMaxHealth() / 2f)};
		}

		@Override public int width(Minecraft mc) {
			int w = 60;
			for (String l : lines(mc)) w = Math.max(w, mc.font.width(l) + 10);
			return w;
		}

		@Override public int height(Minecraft mc) { return 34; }

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			if (horse(mc) == null && !(mc.screen instanceof HudEditorScreen)) return;
			int w = width(mc);
			if (bgOn()) panel(g, x, y, x + w, y + 34);
			String[] ls = lines(mc);
			for (int i = 0; i < ls.length; i++) text(g, mc, ls[i], x + 5, y + 3 + i * 10, fg());
		}
	}

	static final class ImageFile extends Setting.Choice {
		private String[] cached = new String[0];
		private long listedAt;

		ImageFile(gg.surge.client.mods.SurgeMod mod) { super(mod, "file", "Image", "None", "None"); }

		static java.nio.file.Path folder() {
			java.nio.file.Path p = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("surge images");
			try { java.nio.file.Files.createDirectories(p); } catch (Exception ignored) { }
			return p;
		}

		String[] files() {
			if (System.currentTimeMillis() - listedAt > 2000) {
				listedAt = System.currentTimeMillis();
				try (var st = java.nio.file.Files.list(folder())) {
					cached = st.map(f -> f.getFileName().toString()).filter(n -> n.matches("(?i).+\\.(png|jpe?g|gif|bmp)")).sorted(String.CASE_INSENSITIVE_ORDER).toArray(String[]::new);
				} catch (Exception e) {
					cached = new String[0];
				}
			}
			return cached;
		}

		@Override public String get() {
			Object r = raw();
			return r instanceof String str && !str.isEmpty() ? str : "None";
		}

		@Override public void cycle(int dir) {
			String[] f = files();
			if (f.length == 0) { set("None"); return; }
			int idx = -1;
			for (int k = 0; k < f.length; k++) if (f[k].equals(get())) idx = k;
			idx = idx < 0 ? (dir > 0 ? 0 : f.length - 1) : (idx + dir + f.length) % f.length;
			set(f[idx]);
		}
	}

	static final class ImageModule extends HudModule {
		final ImageFile file = add(new ImageFile(this));
		final Setting.Slider size = slider("size", "Width", 96, 16, 400, 4, "px");
		final Setting.Slider opacity = slider("opacity", "Opacity", 100, 5, 100, 5, "%");
		final Setting.Slider rotation = slider("rotation", "Rotation", 0, -180, 180, 5, "\u00b0");
		final Setting.Color tint = color("tint", "Tint", "FFFFFF");
		final Setting.Toggle flip = toggle("flip", "Mirror", false);
		private static volatile String picked;
		private String loaded;
		private long loadedStamp;
		private long checkedAt;
		private net.minecraft.resources.Identifier tex;
		private int texW = 1, texH = 1;
		private static final net.minecraft.resources.Identifier ID = net.minecraft.resources.Identifier.fromNamespaceAndPath("surge", "hud_image");

		ImageModule() {
			super("image", "Image", Items.PAINTING, "Put any picture on your screen, pick one and change its size, opacity and rotation", false, 0.8f, 0.6f);
			button("Pick an image", "Browse", ImageModule::browse);
			button("Image folder", "Open", () -> { try { net.minecraft.util.Util.getPlatform().openPath(ImageFile.folder()); } catch (Exception ignored) { } });
		}

		private static void browse() {
			java.util.concurrent.CompletableFuture.runAsync(() -> {
				try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
					org.lwjgl.PointerBuffer f = stack.mallocPointer(5);
					f.put(stack.UTF8("*.png")).put(stack.UTF8("*.jpg")).put(stack.UTF8("*.jpeg")).put(stack.UTF8("*.gif")).put(stack.UTF8("*.bmp"));
					f.flip();
					String path = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog("Pick an image", null, f, "Images", false);
					if (path == null) return;
					java.nio.file.Path src = java.nio.file.Path.of(path);
					java.nio.file.Path dst = ImageFile.folder().resolve(src.getFileName().toString());
					if (!src.toAbsolutePath().normalize().equals(dst.toAbsolutePath().normalize())) java.nio.file.Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
					picked = dst.getFileName().toString();
				} catch (Throwable ignored) { }
			});
		}

		private void ensure(Minecraft mc) {
			String p = picked;
			if (p != null) { picked = null; file.set(p); }
			String name = file.get();
			if (name.equals("None")) { if (tex != null) release(mc); return; }
			if (name.equals(loaded) && System.currentTimeMillis() - checkedAt < 1000) return;
			checkedAt = System.currentTimeMillis();
			java.nio.file.Path path = ImageFile.folder().resolve(name);
			long stamp;
			try { stamp = java.nio.file.Files.getLastModifiedTime(path).toMillis(); } catch (Exception e) { release(mc); return; }
			if (name.equals(loaded) && stamp == loadedStamp) return;
			loaded = name;
			loadedStamp = stamp;
			try {
				if (name.toLowerCase(java.util.Locale.ROOT).endsWith(".png")) {
					try (java.io.InputStream in = java.nio.file.Files.newInputStream(path)) {
						com.mojang.blaze3d.platform.NativeImage ni = com.mojang.blaze3d.platform.NativeImage.read(in);
						if (ni.getWidth() <= 2048 && ni.getHeight() <= 2048) {
							mc.getTextureManager().register(ID, new net.minecraft.client.renderer.texture.DynamicTexture(() -> "surge hud image", ni));
							tex = ID;
							texW = ni.getWidth();
							texH = ni.getHeight();
							return;
						}
						ni.close();
					} catch (Throwable ignored) { }
				}
				java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(path.toFile());
				if (img == null) { release(mc); loaded = name; return; }
				int w = img.getWidth(), h = img.getHeight();
				float k = Math.min(1f, 1024f / Math.max(w, h));
				int nw = Math.max(1, Math.round(w * k)), nh = Math.max(1, Math.round(h * k));
				com.mojang.blaze3d.platform.NativeImage ni = new com.mojang.blaze3d.platform.NativeImage(nw, nh, false);
				for (int y = 0; y < nh; y++) for (int x = 0; x < nw; x++) ni.setPixel(x, y, img.getRGB(Math.min(w - 1, (int) (x / k)), Math.min(h - 1, (int) (y / k))));
				mc.getTextureManager().register(ID, new net.minecraft.client.renderer.texture.DynamicTexture(() -> "surge hud image", ni));
				tex = ID;
				texW = nw;
				texH = nh;
			} catch (Throwable t) {
				release(mc);
				loaded = name;
			}
		}

		private void release(Minecraft mc) {
			if (tex != null) mc.getTextureManager().release(tex);
			tex = null;
			loaded = null;
		}

		@Override public int width(Minecraft mc) { return size.asInt(); }

		@Override public int height(Minecraft mc) {
			ensure(mc);
			return tex == null ? Math.max(16, size.asInt() / 2) : Math.max(1, Math.round(size.get() * texH / (float) texW));
		}

		@Override public void render(GuiGraphics g, Minecraft mc, int x, int y) {
			ensure(mc);
			int w = width(mc), h = height(mc);
			boolean editing = mc.screen instanceof HudEditorScreen;
			if (tex == null) {
				if (editing) {
					g.fill(x, y, x + w, y + h, 0x40FFFFFF);
					String t = "Pick an image";
					g.drawString(mc.font, t, x + (w - mc.font.width(t)) / 2, y + (h - 8) / 2, 0xFFFFFFFF, true);
				}
				return;
			}
			int a = Math.round(opacity.get() * 2.55f);
			int color = (a << 24) | (tint.rgb(0) & 0xFFFFFF);
			g.pose().pushMatrix();
			g.pose().translate(x + w / 2f, y + h / 2f);
			if (rotation.get() != 0) g.pose().rotate((float) Math.toRadians(rotation.get()));
			if (flip.on()) g.pose().scale(-1f, 1f);
			g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, -w / 2, -h / 2, 0f, 0f, w, h, texW, texH, texW, texH, color);
			g.pose().popMatrix();
		}
	}
}
