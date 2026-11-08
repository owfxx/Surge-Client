package gg.surge.client;

import com.mojang.blaze3d.platform.InputConstants;
import gg.surge.client.config.SurgeConfig;
import gg.surge.client.cosmetics.CosmeticLayer;
import gg.surge.client.cosmetics.Cosmetics;
import gg.surge.client.economy.Sales;
import gg.surge.client.emotes.Emotes;
import gg.surge.client.features.CombatStats;
import gg.surge.client.features.CommandKeys;
import gg.surge.client.features.Crosshair;
import gg.surge.client.features.DamageTint;
import gg.surge.client.features.Freelook;
import gg.surge.client.features.Fullbright;
import gg.surge.client.features.OptionTweaks;
import gg.surge.client.features.Presence;
import gg.surge.client.features.Waypoints;
import gg.surge.client.features.Zoom;
import gg.surge.client.gui.AdminScreen;
import gg.surge.client.gui.EmoteWheelScreen;
import gg.surge.client.gui.HudEditorScreen;
import gg.surge.client.gui.MenuOverlay;
import gg.surge.client.gui.SurgeMenuScreen;
import gg.surge.client.gui.WardrobeScreen;
import gg.surge.client.hud.HudModule;
import gg.surge.client.hud.HudModules;
import gg.surge.client.mods.Mods;
import gg.surge.client.perf.Performance;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings({"unchecked", "rawtypes"})
public class SurgeClient implements ClientModInitializer {
	public static final String MOD_ID = "surge";
	private static KeyMapping menuKey, emoteKey, zoomKey, freelookKey, hudKey, sprintKey, fullbrightKey, waypointKey, stopwatchKey, wardrobeKey, clipKey;
	private static boolean menuKeyWasDown, adminWasDown;

	private static KeyMapping key(String name, int glfw, KeyMapping.Category category) {
		return KeyBindingHelper.registerKeyBinding(new KeyMapping("key.surge." + name, InputConstants.Type.KEYSYM, glfw, category));
	}

	@Override
	public void onInitializeClient() {
		SurgeConfig.load();
		gg.surge.client.features.SelfUpdate.start();

		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "surge"));
		menuKey = key("menu", GLFW.GLFW_KEY_RIGHT_SHIFT, category);
		zoomKey = key("zoom", GLFW.GLFW_KEY_C, category);
		freelookKey = key("freelook", GLFW.GLFW_KEY_LEFT_ALT, category);
		emoteKey = key("emotes", GLFW.GLFW_KEY_B, category);
		hudKey = key("hud_editor", GLFW.GLFW_KEY_UNKNOWN, category);
		sprintKey = key("toggle_sprint", GLFW.GLFW_KEY_UNKNOWN, category);
		fullbrightKey = key("fullbright", GLFW.GLFW_KEY_UNKNOWN, category);
		waypointKey = key("waypoint", GLFW.GLFW_KEY_UNKNOWN, category);
		stopwatchKey = key("stopwatch", GLFW.GLFW_KEY_UNKNOWN, category);
		wardrobeKey = key("wardrobe", GLFW.GLFW_KEY_UNKNOWN, category);
		clipKey = key("save_clip", GLFW.GLFW_KEY_F8, category);

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			CombatStats.onAttack(player, entity);
			if (Mods.HIT_PARTICLES.enabled() && player == Minecraft.getInstance().player) {
				var pe = Minecraft.getInstance().particleEngine;
				for (int i = 0; i < Mods.HIT_PARTICLE_COUNT.asInt(); i++) {
					if (Mods.HIT_PARTICLE_ALWAYS.on()) pe.createTrackingEmitter(entity, net.minecraft.core.particles.ParticleTypes.CRIT);
					pe.createTrackingEmitter(entity, net.minecraft.core.particles.ParticleTypes.ENCHANTED_HIT);
				}
			}
			if (Mods.HIT_SOUND.enabled() && player == Minecraft.getInstance().player) {
				float vol = Mods.HIT_SOUND_VOL.get() / 100f;
				var sound = switch (Mods.HIT_SOUND_TYPE.get()) {
					case "Pop" -> net.minecraft.sounds.SoundEvents.ITEM_PICKUP;
					case "Click" -> net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value();
					default -> net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP;
				};
				Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(sound, 1.0f + (float) Math.random() * 0.1f, vol));
			}
			return InteractionResult.PASS;
		});

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), SurgeClient::renderHud);
		HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (g, dt) -> {
			if (Mods.CROSSHAIR.enabled()) Crosshair.render(g, Minecraft.getInstance()); else vanilla.render(g, dt);
		});
		HudElementRegistry.replaceElement(VanillaHudElements.STATUS_EFFECTS, vanilla -> (g, dt) -> {
			HudModule effects = HudModules.byId("effects");
			boolean hide = effects instanceof gg.surge.client.hud.HideVanillaEffects a && a.hideVanillaEffects();
			if (!hide) vanilla.render(g, dt);
		});
		HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, vanilla -> (g, dt) -> {
			if (!Mods.SCOREBOARD.enabled()) { vanilla.render(g, dt); return; }
			if (Mods.SCORE_HIDE.on()) return;
			scaled(g, Mods.SCORE_SCALE.get() / 100f, g.guiWidth(), g.guiHeight() / 2f, () -> vanilla.render(g, dt));
		});
		HudElementRegistry.replaceElement(VanillaHudElements.BOSS_BAR, vanilla -> (g, dt) -> {
			if (!Mods.BOSSBAR.enabled()) { vanilla.render(g, dt); return; }
			if (Mods.BOSS_HIDE.on()) return;
			scaled(g, Mods.BOSS_SCALE.get() / 100f, g.guiWidth() / 2f, 0, () -> vanilla.render(g, dt));
		});
		HudElementRegistry.replaceElement(VanillaHudElements.TITLE_AND_SUBTITLE, vanilla -> (g, dt) -> {
			if (!Mods.TITLES.enabled()) { vanilla.render(g, dt); return; }
			scaled(g, Mods.TITLE_SCALE.get() / 100f, g.guiWidth() / 2f, g.guiHeight() / 2f, () -> vanilla.render(g, dt));
		});
		HudElementRegistry.replaceElement(VanillaHudElements.FOOD_BAR, vanilla -> (g, dt) -> {
			vanilla.render(g, dt);
			gg.surge.client.features.AppleSkin.render(g, Minecraft.getInstance());
		});
		HudElementRegistry.replaceElement(VanillaHudElements.MISC_OVERLAYS, vanilla -> (g, dt) -> {
			Minecraft mc = Minecraft.getInstance();
			boolean pumpkin = mc.player != null && mc.player.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN);
			if (pumpkin && Mods.OVERLAYS.enabled() && Mods.HIDE_PUMPKIN.on()) return;
			vanilla.render(g, dt);
		});

		ClientLifecycleEvents.CLIENT_STARTED.register(mc -> {
			Performance.apply(mc);
			Fullbright.apply(mc);
			DamageTint.apply(mc);
			OptionTweaks.apply(mc);
			gg.surge.client.features.Extras.applyGlint(mc);
			Sales.load();
			gg.surge.client.economy.Locks.load();
		});
		ClientTickEvents.END_CLIENT_TICK.register(SurgeClient::tick);
		net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack, ctx, type, lines) -> gg.surge.client.features.Qol.tooltip(stack, lines));
		if (gg.surge.client.dev.AutoTest.active()) ClientTickEvents.END_CLIENT_TICK.register(gg.surge.client.dev.AutoTest::tick);
		MenuOverlay.register();
		LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
			if (renderer instanceof AvatarRenderer) helper.register(new CosmeticLayer((AvatarRenderer) renderer));
		});
	}

	private static void scaled(GuiGraphics g, float s, float ax, float ay, Runnable draw) {
		if (Math.abs(s - 1f) < 0.01f) { draw.run(); return; }
		g.pose().pushMatrix();
		g.pose().translate(ax, ay);
		g.pose().scale(s, s);
		g.pose().translate(-ax, -ay);
		draw.run();
		g.pose().popMatrix();
	}

	private static void tick(Minecraft mc) {
		SurgeConfig.flush();

		boolean typing = mc.screen != null && mc.screen.getFocused() instanceof EditBox;
		boolean menuDown = isBoundKeyDown(mc, menuKey);
		boolean pressedNow = menuDown && !menuKeyWasDown;
		menuKeyWasDown = menuDown;
		if (pressedNow && !typing && (mc.screen instanceof SurgeMenuScreen || mc.screen instanceof HudEditorScreen || mc.screen instanceof WardrobeScreen)) {
			mc.screen.onClose();
		}

		long win = mc.getWindow().handle();
		boolean f4 = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_F4) == GLFW.GLFW_PRESS;
		boolean ctrl = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
		boolean shift = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
		boolean adminDown = f4 && ctrl && shift;
		if (adminDown && !adminWasDown && !(mc.screen instanceof AdminScreen) && AdminScreen.allowed(mc)) {
			mc.setScreen(new AdminScreen(mc.screen));
		}
		adminWasDown = adminDown;

		while (menuKey.consumeClick()) {
			if (mc.screen == null) mc.setScreen(new SurgeMenuScreen());
		}
		while (emoteKey.consumeClick()) {
			if (mc.screen == null && mc.player != null) mc.setScreen(new EmoteWheelScreen());
		}
		while (hudKey.consumeClick()) {
			if (mc.screen == null) mc.setScreen(new HudEditorScreen(null));
		}
		while (wardrobeKey.consumeClick()) {
			if (mc.screen == null) mc.setScreen(new WardrobeScreen(null));
		}
		while (fullbrightKey.consumeClick()) Mods.FULLBRIGHT.toggle();
		while (waypointKey.consumeClick()) Waypoints.addHere(mc);
		while (stopwatchKey.consumeClick()) HudModules.toggleStopwatch();
		while (clipKey.consumeClick()) gg.surge.client.features.Clips.save(mc);
		while (sprintKey.consumeClick()) Mods.flipSprint();

		Zoom.setActive(mc, mc.screen == null && mc.player != null && zoomKey.isDown());
		Freelook.update(mc, freelookKey.isDown());
		CombatStats.tick(mc);
		HudModules.tick(mc);
		CommandKeys.tick(mc);
		Presence.tick(mc);
		DamageTint.tick(mc);
		Fullbright.tick(mc);
		gg.surge.client.features.MotionBlur.tick(mc);
		Waypoints.tick(mc);
		gg.surge.client.features.GlintColor.tick(mc);
		gg.surge.client.net.SurgeNet.tick(mc);
		gg.surge.client.net.Social.tick(mc);
		gg.surge.client.net.Inbox.tick(mc);
		gg.surge.client.lod.Lod.tick(mc);
		gg.surge.client.features.Extras.tick(mc);
		gg.surge.client.economy.Rewards.tick(mc);
		gg.surge.client.economy.Wallet.refreshRemote(false);
		gg.surge.client.features.Profiles.tick(mc);

		LocalPlayer p = mc.player;
		if (p == null) return;
		if (!SurgeConfig.get().welcomed && p.tickCount > 60) {
			SurgeConfig.get().welcomed = true;
			SurgeConfig.save();
			String menu = KeyBindingHelper.getBoundKeyOf(menuKey).getDisplayName().getString();
			String emotes = KeyBindingHelper.getBoundKeyOf(emoteKey).getDisplayName().getString();
			gg.surge.client.net.Social.notice("Welcome to Surge Client", menu + ": mods   " + emotes + ": emotes");
			gg.surge.client.net.Social.notice("Wardrobe and friends", "In the Esc menu, on the right");
		}

		var opts = mc.options;
		if (Mods.TOGGLE_SPRINT.enabled()) while (opts.keySprint.consumeClick()) Mods.flipSprint();
		if (Mods.TOGGLE_SNEAK.enabled()) {
			while (opts.keyShift.consumeClick()) Mods.setSneak(!Mods.sneakToggled());
			if (p.isDeadOrDying() || p.getAbilities().flying) Mods.setSneak(false);
		} else Mods.setSneak(false);
		Emotes.tick(mc);
	}

	private static boolean isBoundKeyDown(Minecraft mc, KeyMapping key) {
		InputConstants.Key bound = KeyBindingHelper.getBoundKeyOf(key);
		if (bound.getType() != InputConstants.Type.KEYSYM || bound.getValue() < 0) return false;
		return GLFW.glfwGetKey(mc.getWindow().handle(), bound.getValue()) == GLFW.GLFW_PRESS;
	}

	private static void renderHud(GuiGraphics g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		gg.surge.client.features.Clips.frame(mc);
		HudModules.pollClicks(mc);
		boolean wasRecording = gg.surge.client.perf.PerfCheck.state == gg.surge.client.perf.PerfCheck.State.RECORDING;
		gg.surge.client.perf.PerfCheck.frame(mc);
		if (wasRecording && gg.surge.client.perf.PerfCheck.state == gg.surge.client.perf.PerfCheck.State.DONE && mc.screen == null) {
			mc.setScreen(gg.surge.client.gui.SurgeMenuScreen.perf(null));
			return;
		}
		if (gg.surge.client.perf.PerfCheck.state == gg.surge.client.perf.PerfCheck.State.RECORDING && mc.screen == null) {
			String t = "Checking performance  " + gg.surge.client.perf.PerfCheck.secondsLeft() + "s";
			int w = mc.font.width(t) + 16, x = (g.guiWidth() - w) / 2;
			gg.surge.client.gui.Ui.cutBox(g, x, 4, w, 16, 3, 0xD0101012, 0xFF45454C);
			g.drawString(mc.font, t, x + 8, 8, 0xFFE4E4E7, false);
		}
		if (mc.options.hideGui || mc.screen instanceof HudEditorScreen) return;
		boolean debug = mc.debugEntries.isOverlayVisible();
		if (!debug) {
			Waypoints.render(g, mc);
			gg.surge.client.features.Extras.render(g, mc);
			for (HudModule m : HudModules.ALL) {
				if (m.enabled()) m.draw(g, mc);
			}
		}
		if (mc.screen == null) gg.surge.client.gui.SocialToasts.render(g, mc, g.guiWidth());
	}
}
