package gg.surge.client.dev;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.economy.Shop;
import gg.surge.client.economy.Wallet;
import gg.surge.client.emotes.Emotes;
import gg.surge.client.gui.EmoteWheelScreen;
import gg.surge.client.gui.HudEditorScreen;
import gg.surge.client.gui.ServerTabsScreen;
import gg.surge.client.gui.SocialScreen;
import gg.surge.client.gui.SurgeMenuScreen;
import gg.surge.client.gui.WardrobeScreen;
import gg.surge.client.hud.HudModule;
import gg.surge.client.mods.Mods;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class AutoTest {
	private static final Logger LOG = LoggerFactory.getLogger("SurgeAutoTest");
	private record Step(String name, int delay, Consumer<Minecraft> action) {}

	private static final List<Step> STEPS = new ArrayList<>();
	private static int index = -1, ticks, total;
	private static boolean worldRequested, inWorld;

	private AutoTest() {}

	public static boolean active() {
		return System.getProperty("surge.autotest") != null;
	}

	private static void shot(Minecraft mc, String name) {
		try {
			Screenshot.grab(mc.gameDirectory, "surge-" + name + ".png", mc.getMainRenderTarget(), 1, c -> LOG.info("Screenshot {}: {}", name, c.getString()));
		} catch (Throwable t) {
			LOG.error("Screenshot {} failed", name, t);
		}
	}

	private static void open(Minecraft mc, Screen s) {
		mc.setScreen(s);
	}

	private static void plan() {
		STEPS.add(new Step("01-title", 80, mc -> { mc.options.onboardAccessibility = false; open(mc, new TitleScreen()); }));
		STEPS.add(new Step("01b-title-log", 30, mc -> gg.surge.client.gui.SurgeTitleScreen.autotestLog()));
		STEPS.add(new Step("01c-title-hover", 30, mc -> hover(mc, 0.85, 0.068)));
		STEPS.add(new Step("01d-title-wardrobe", 40, mc -> { SurgeConfig.get().wings = "angel"; open(mc, new gg.surge.client.gui.WardrobeScreen(mc.screen)); }));
		STEPS.add(new Step("02-mods-menu", 30, mc -> open(mc, new SurgeMenuScreen(mc.screen))));
		STEPS.add(new Step("03-settings", 30, mc -> open(mc, SurgeMenuScreen.general(null))));
		STEPS.add(new Step("world", 0, mc -> createWorld(mc)));
		STEPS.add(new Step("04-hud", 60, mc -> { open(mc, null); Mods.CLIPS.setEnabled(true); mc.options.setCameraType(CameraType.FIRST_PERSON); }));
		STEPS.add(new Step("05-third-person", 40, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT)));
		STEPS.add(new Step("05b-back-view", 30, mc -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK)));
		STEPS.add(new Step("05e-rune-cape", 25, mc -> { SurgeConfig.get().cape = "runes"; SurgeConfig.get().wings = "none"; }));
		STEPS.add(new Step("05f-flame-cape", 25, mc -> { SurgeConfig.get().cape = "flame"; }));
		STEPS.add(new Step("05g-cat-cape", 25, mc -> { SurgeConfig.get().cape = "blinkcat"; }));
		STEPS.add(new Step("05h-koi-cape", 25, mc -> { SurgeConfig.get().cape = "koi"; }));
		STEPS.add(new Step("05i-angel-wings", 25, mc -> { SurgeConfig.get().cape = "none"; SurgeConfig.get().wings = "angel"; }));
		STEPS.add(new Step("05j-halo-helmet", 30, mc -> { SurgeConfig.get().wings = "none"; SurgeConfig.get().halo = "angel"; mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET)); mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }));
		STEPS.add(new Step("05c-halo-front", 30, mc -> { SurgeConfig.get().wings = "none"; SurgeConfig.get().halo = "surge"; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }));
		STEPS.add(new Step("05d-clip", 20, mc -> { org.slf4j.LoggerFactory.getLogger("surge").info("Clips buffered frames: " + gg.surge.client.features.Clips.frames()); gg.surge.client.features.Clips.save(mc); }));
		STEPS.add(new Step("06-emote-griddy", 30, mc -> Emotes.play(mc, Emotes.Emote.GRIDDY)));
		STEPS.add(new Step("07-emote-disco", 30, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.DISCO); }));
		STEPS.add(new Step("07c-emote-cry", 30, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.CRY); }));
		STEPS.add(new Step("07d-emote-dab", 30, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.DAB); }));
		STEPS.add(new Step("07i-emote-wave", 25, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.WAVE); }));
		STEPS.add(new Step("07j-emote-salute", 25, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.SALUTE); }));
		STEPS.add(new Step("07k-emote-clap", 25, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.CLAP); }));
		STEPS.add(new Step("07f-emote-heart", 25, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.HEART); }));
		STEPS.add(new Step("07g-emote-floss", 25, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.FLOSS); }));
		STEPS.add(new Step("07h-emote-tpose", 25, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.TPOSE); }));
		STEPS.add(new Step("07e-emote-cheer", 25, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.CHEER); }));
		STEPS.add(new Step("07l-emote-shuffle", 30, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.SHUFFLE); }));
		STEPS.add(new Step("07m0-platform", 50, mc -> { Emotes.stop(mc); mc.player.connection.sendCommand("fill ~-4 ~30 ~-4 ~4 ~30 ~4 minecraft:smooth_stone"); mc.player.connection.sendCommand("tp @s ~ ~31 ~ 0 10"); mc.player.getAbilities().flying = false; mc.player.onUpdateAbilities(); mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY); }));
		STEPS.add(new Step("07m-emote-zombie", 24, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.ZOMBIE); }));
		STEPS.add(new Step("07m-emote-robot", 24, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.ROBOT); }));
		STEPS.add(new Step("07m-emote-airguitar", 24, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.AIRGUITAR); }));
		STEPS.add(new Step("07m-emote-jumpjacks", 24, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.JUMPJACKS); }));
		STEPS.add(new Step("07m-emote-flex", 24, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.FLEX); }));
		STEPS.add(new Step("07m-emote-kiss", 24, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.KISS); }));
		STEPS.add(new Step("07m-emote-spin", 24, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.SPIN); }));
		STEPS.add(new Step("07m-emote-faceplant", 40, mc -> { Emotes.stop(mc); Emotes.play(mc, Emotes.Emote.FACEPLANT); }));
		STEPS.add(new Step("07n-emotes-done", 10, mc -> Emotes.stop(mc)));
		STEPS.add(new Step("07s-idle-stretch", 120, mc -> { SurgeConfig.get().idle = "IDLE_STRETCH"; }));
		STEPS.add(new Step("07s-idle-watch", 100, mc -> { SurgeConfig.get().idle = "IDLE_WATCH"; }));
		STEPS.add(new Step("07s-idle-scratch", 100, mc -> { SurgeConfig.get().idle = "IDLE_SCRATCH"; }));
		STEPS.add(new Step("07s-idle-box", 100, mc -> { SurgeConfig.get().idle = "IDLE_BOX"; }));
		STEPS.add(new Step("07s-idle-phone", 100, mc -> { SurgeConfig.get().idle = "IDLE_PHONE"; }));
		STEPS.add(new Step("07s-idle-whistle", 100, mc -> { SurgeConfig.get().idle = "IDLE_WHISTLE"; }));
		STEPS.add(new Step("07t-elytra-emote", 40, mc -> { SurgeConfig.get().idle = "none"; Emotes.stop(mc); mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ELYTRA)); Emotes.play(mc, Emotes.Emote.DAB); mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); }));
		STEPS.add(new Step("07v-lod-collect", 300, mc -> { gg.surge.client.mods.Mods.LOD.setEnabled(true); mc.player.connection.sendCommand("gamemode spectator"); mc.player.connection.sendCommand("tp @s ~ ~100 ~"); mc.options.setCameraType(CameraType.FIRST_PERSON); mc.player.setXRot(40f); }));
		STEPS.add(new Step("07w-lod-far", 120, mc -> { org.slf4j.LoggerFactory.getLogger("surge").info("Distant terrain " + gg.surge.client.lod.Lod.stats() + " rd=" + mc.options.getEffectiveRenderDistance()); mc.options.renderDistance().set(3); mc.player.setYRot(mc.player.getYRot() + 180f); mc.player.setXRot(35f); mc.options.setCameraType(CameraType.FIRST_PERSON); }));
		STEPS.add(new Step("07w1-lod-low", 100, mc -> gg.surge.client.mods.Mods.LOD_QUALITY.set("Low")));
		STEPS.add(new Step("07w2-lod-medium", 100, mc -> gg.surge.client.mods.Mods.LOD_QUALITY.set("Medium")));
		STEPS.add(new Step("07w3-lod-high", 100, mc -> gg.surge.client.mods.Mods.LOD_QUALITY.set("High")));
		STEPS.add(new Step("07w4-lod-ultra", 100, mc -> gg.surge.client.mods.Mods.LOD_QUALITY.set("Ultra")));
		STEPS.add(new Step("07w5-lod-full", 140, mc -> gg.surge.client.mods.Mods.LOD_QUALITY.set("Full")));
		STEPS.add(new Step("07x-lod-stats", 20, mc -> org.slf4j.LoggerFactory.getLogger("surge").info("Distant terrain " + gg.surge.client.lod.Lod.stats())));
		STEPS.add(new Step("07xa-wings-angel", 50, mc -> { mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY); mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY); mc.player.connection.sendCommand("gamemode creative"); mc.player.getAbilities().flying = true; mc.player.onUpdateAbilities(); gg.surge.client.mods.Mods.LOD.setEnabled(false); SurgeConfig.get().cape = "none"; SurgeConfig.get().halo = "none"; SurgeConfig.get().wings = "angel"; mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); mc.player.setXRot(15f); }));
		STEPS.add(new Step("07xb-wings-surge", 40, mc -> { SurgeConfig.get().wings = "surge"; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }));
		STEPS.add(new Step("07xc-wings-flame", 40, mc -> { SurgeConfig.get().wings = "flame"; mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); mc.player.setYRot(mc.player.getYRot() + 60f); }));
		STEPS.add(new Step("07xd-wings-gold", 40, mc -> { SurgeConfig.get().wings = "gold"; }));
		STEPS.add(new Step("07xd-wings-bat", 30, mc -> { SurgeConfig.get().wings = "bat"; }));
		STEPS.add(new Step("07xd-wings-dragon", 30, mc -> { SurgeConfig.get().wings = "dragon"; }));
		STEPS.add(new Step("07xd-wings-butterfly", 30, mc -> { SurgeConfig.get().wings = "butterfly"; }));
		STEPS.add(new Step("07xd-wings-fairy", 30, mc -> { SurgeConfig.get().wings = "fairy"; }));
		STEPS.add(new Step("07xd-halo-crown", 30, mc -> { SurgeConfig.get().wings = "none"; SurgeConfig.get().halo = "crown"; mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }));
		STEPS.add(new Step("07xd-halo-orbit", 30, mc -> { SurgeConfig.get().halo = "orbit"; }));
		STEPS.add(new Step("07xd-halo-rainbow", 30, mc -> { SurgeConfig.get().halo = "rainbow"; mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); }));
		STEPS.add(new Step("07xe-cape-phoenix", 30, mc -> { SurgeConfig.get().halo = "none"; SurgeConfig.get().wings = "none"; SurgeConfig.get().cape = "phoenix"; mc.player.setYRot(mc.player.getYRot() - 60f); }));
		STEPS.add(new Step("07xf-cape-bloodmoon", 30, mc -> SurgeConfig.get().cape = "bloodmoon"));
		STEPS.add(new Step("07xg-cape-obsidian", 30, mc -> SurgeConfig.get().cape = "obsidian"));
		STEPS.add(new Step("07xh-elytra-phoenix", 40, mc -> { SurgeConfig.get().cape = "phoenix"; mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ELYTRA)); }));
		STEPS.add(new Step("07xi-elytra-koi", 30, mc -> { SurgeConfig.get().cape = "koi"; mc.player.setYRot(mc.player.getYRot() + 30f); }));
		STEPS.add(new Step("07x-lod-reset", 40, mc -> { mc.options.renderDistance().set(8); gg.surge.client.mods.Mods.LOD.setEnabled(false); mc.player.connection.sendCommand("tp @s ~ ~-100 ~"); SurgeConfig.get().wings = "none"; SurgeConfig.get().cape = "none"; }));
		STEPS.add(new Step("07y-custom-sounds", 40, mc -> { Mods.CUSTOM_SOUNDS.setEnabled(true); gg.surge.client.features.CustomSounds.set("minecraft:entity.player.levelup", "preset:chime"); mc.setScreen(new gg.surge.client.gui.CustomSoundsScreen(null)); gg.surge.client.features.CustomSounds.preview("preset:ding"); }));
		STEPS.add(new Step("07z-custom-sounds-done", 20, mc -> { mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 1f, 1f)); org.slf4j.LoggerFactory.getLogger("surge").info("Custom sounds mapped: " + gg.surge.client.features.CustomSounds.all()); gg.surge.client.features.CustomSounds.resetAll(); Mods.CUSTOM_SOUNDS.setEnabled(false); open(mc, null); }));
		STEPS.add(new Step("07u-cleanup", 5, mc -> { Emotes.stop(mc); mc.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY); }));
		STEPS.add(new Step("07b-featured", 50, mc -> { Emotes.stop(mc); mc.options.setCameraType(CameraType.FIRST_PERSON); open(mc, WardrobeScreen.open(null, WardrobeScreen.Section.FEATURED)); }));
		STEPS.add(new Step("08-wardrobe", 50, mc -> { Emotes.stop(mc); mc.options.setCameraType(CameraType.FIRST_PERSON); open(mc, WardrobeScreen.open(null, WardrobeScreen.Section.COSMETICS)); }));
		STEPS.add(new Step("08b-angles", 40, mc -> open(mc, new Screen(net.minecraft.network.chat.Component.literal("angles")) {
			@Override
			public void render(net.minecraft.client.gui.GuiGraphics g, int mx, int my, float d) {
				int w = this.width / 8;
				for (int i = 0; i < 8; i++) {
					gg.surge.client.cosmetics.Preview.drawCard(g, i * w, 20, i * w + w, 140, 40, i * 45f, new gg.surge.client.cosmetics.CapturedLook("surge", null, null, null, 0f, false));
					g.drawString(font, String.valueOf(i * 45), i * w + 4, 150, 0xFFFFFFFF, true);
					g.enableScissor(0, 160, this.width, 300);
					gg.surge.client.cosmetics.Preview.drawCard(g, i * w, 170, i * w + w, 290, 40, i * 45f, new gg.surge.client.cosmetics.CapturedLook(null, "surge", null, null, 0f, false));
					g.disableScissor();
				}
			}
		})));
		STEPS.add(new Step("08c-wardrobe-wings", 30, mc -> { WardrobeScreen.autotestSub("Capes"); open(mc, WardrobeScreen.open(null, WardrobeScreen.Section.COSMETICS)); }));
		STEPS.add(new Step("09-wardrobe-emotes", 50, mc -> open(mc, WardrobeScreen.open(null, WardrobeScreen.Section.EMOTES))));
		STEPS.add(new Step("10-wardrobe-outfits", 40, mc -> open(mc, WardrobeScreen.open(null, WardrobeScreen.Section.OUTFITS))));
		STEPS.add(new Step("11-coins", 30, mc -> open(mc, WardrobeScreen.open(null, WardrobeScreen.Section.CURRENCY))));
		STEPS.add(new Step("12-social", 40, mc -> { fakeSocial(); open(mc, new SocialScreen(null)); }));
		STEPS.add(new Step("12b-social-friends", 30, mc -> clickAt(mc, 0.24, 0.135)));
		STEPS.add(new Step("12c-social-requests", 30, mc -> { if (mc.screen instanceof SocialScreen ss) ss.autotestRequestsTab(); }));
		STEPS.add(new Step("13-emote-wheel", 40, mc -> open(mc, new EmoteWheelScreen())));
		STEPS.add(new Step("14-hud-editor", 40, mc -> open(mc, new HudEditorScreen(null))));
		STEPS.add(new Step("15-pause", 40, mc -> { open(mc, null); mc.pauseGame(false); }));
		STEPS.add(new Step("16-mods-ingame", 30, mc -> open(mc, new SurgeMenuScreen(null))));
		STEPS.add(new Step("16b-perf-intro", 30, mc -> open(mc, SurgeMenuScreen.perf(null))));
		STEPS.add(new Step("16c-perf-running", 60, mc -> { open(mc, null); gg.surge.client.perf.PerfCheck.start(); }));
		STEPS.add(new Step("16d-perf-report", 420, mc -> { }));
		STEPS.add(new Step("17-servers", 40, mc -> open(mc, new JoinMultiplayerScreen(new TitleScreen()))));
		STEPS.add(new Step("17b-friends-tab", 40, mc -> open(mc, new ServerTabsScreen(mc.screen, ServerTabsScreen.Mode.FRIENDS))));
		STEPS.add(new Step("18-discover", 60, mc -> open(mc, new ServerTabsScreen(mc.screen, ServerTabsScreen.Mode.DISCOVER))));
		STEPS.add(new Step("19-beta-cape", 30, mc -> open(mc, new gg.surge.client.gui.BetaCapeScreen(null))));
		STEPS.add(new Step("19b-minimap-settings", 30, mc -> open(mc, SurgeMenuScreen.settings(Mods.hud("minimap"), null))));
		STEPS.add(new Step("19c-profiles", 30, mc -> open(mc, SurgeMenuScreen.settings(Mods.PROFILES, null))));
		STEPS.add(new Step("19d-inventory", 30, mc -> open(mc, new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player))));
		STEPS.add(new Step("19e-gift", 40, mc -> gg.surge.client.gui.GiftScreen.show("You've been gifted 2 items!", "Thanks for testing Surge.", 500, java.util.List.of("cape:koi", "emote:SHUFFLE"))));
		STEPS.add(new Step("19f-admin", 40, mc -> open(mc, new gg.surge.client.gui.AdminScreen(null))));
		STEPS.add(new Step("19g-profiles-tab", 30, mc -> open(mc, SurgeMenuScreen.profiles(null))));
		STEPS.add(new Step("20-chat", 30, mc -> { open(mc, null); mc.gui.getChat().addMessage(net.minecraft.network.chat.Component.literal("Surge Client test message")); }));
		STEPS.add(new Step("21-glass-menu", 40, mc -> { Mods.THEME.set("Custom"); Mods.THEME_COLOR.set("EDEADE"); open(mc, new SurgeMenuScreen(null)); }));
		STEPS.add(new Step("21b-glass-settings", 30, mc -> open(mc, SurgeMenuScreen.settings(Mods.hud("coords"), null))));
		STEPS.add(new Step("21c-glass-hud", 30, mc -> { Mods.hud("keystrokes").setEnabled(true); Mods.hud("cps").setEnabled(true); open(mc, null); }));
		STEPS.add(new Step("21d-glass-wardrobe", 40, mc -> open(mc, new gg.surge.client.gui.WardrobeScreen(null))));
		STEPS.add(new Step("21e-glass-general", 30, mc -> open(mc, SurgeMenuScreen.general(null))));
		STEPS.add(new Step("21f-glass-off", 20, mc -> { Mods.THEME.set("Ash"); Mods.THEME_COLOR.set("6B6B73"); open(mc, null); }));
	}

	private static void hover(Minecraft mc, double fx, double fy) {
		long win = mc.getWindow().handle();
		org.lwjgl.glfw.GLFW.glfwSetCursorPos(win, mc.getWindow().getScreenWidth() * fx, mc.getWindow().getScreenHeight() * fy);
	}

	private static void clickAt(Minecraft mc, double fx, double fy) {
		hover(mc, fx, fy);
		if (mc.screen instanceof SocialScreen ss) ss.autotestFriendsTab();
	}

	private static void fakeSocial() {
		System.setProperty("surge.apiUrl", "https://surge-test.invalid");
		System.setProperty("surge.token", "0123456789abcdef0123456789abcdef0123456789abcdef");
		gg.surge.client.net.Social.useSession("0123456789abcdef0123456789abcdef0123456789abcdef");
		gg.surge.client.net.Social.myUuid = "aaaa0000aaaa0000aaaa0000aaaa0000";
		java.util.List<gg.surge.client.net.Social.Friend> fr = new ArrayList<>();
		fr.add(new gg.surge.client.net.Social.Friend("069a79f444e94726a5befca90e38aaf5", "Notch", "online", "mc.hypixel.net"));
		fr.add(new gg.surge.client.net.Social.Friend("853c80ef3c3749fdaa49938b674adae6", "jeb_", "idle", ""));
		fr.add(new gg.surge.client.net.Social.Friend("61699b2ed3274a019f1e0ea8c3f06bc6", "Dinnerbone", "busy", "play.cubecraft.net"));
		fr.add(new gg.surge.client.net.Social.Friend("c06f89064c8a49119c29ea1dbd1aab82", "MHF_Steve", "offline", ""));
		gg.surge.client.net.Social.friends = fr;
		gg.surge.client.net.Social.incoming = new ArrayList<>(java.util.List.of(new gg.surge.client.net.Social.Person("f7c77d999f154a66a87dc4a51ef30d19", "hypixel")));
		long now = System.currentTimeMillis();
		String me = gg.surge.client.net.Social.myUuid, n = "069a79f444e94726a5befca90e38aaf5";
		java.util.List<gg.surge.client.net.Social.Message> m = new ArrayList<>();
		m.add(new gg.surge.client.net.Social.Message(1, n, me, "hey, you on?", now - 86400000L - 600000, false, 0));
		m.add(new gg.surge.client.net.Social.Message(2, me, n, "yeah! what are you playing?", now - 86400000L - 500000, false, 0));
		m.add(new gg.surge.client.net.Social.Message(3, n, me, "bedwars on hypixel, want to join? we need one more for the team", now - 300000, false, 0));
		m.add(new gg.surge.client.net.Social.Message(4, me, n, "omw", now - 120000, false, 0));
		gg.surge.client.net.Social.CHATS.put(n, m);
		gg.surge.client.net.Social.CHATS.put("853c80ef3c3749fdaa49938b674adae6", new ArrayList<>(java.util.List.of(new gg.surge.client.net.Social.Message(5, "853c80ef3c3749fdaa49938b674adae6", me, "check out the new cape", now - 3600000, false, 0))));
		gg.surge.client.net.Social.UNREAD.put("853c80ef3c3749fdaa49938b674adae6", 2);
	}

	private static void createWorld(Minecraft mc) {
		worldRequested = true;
		Wallet w = Wallet.get();
		for (Shop.Item i : Shop.ITEMS) w.owned.add(i.id());
		w.coins = 12500;
		SurgeConfig c = SurgeConfig.get();
		c.cape = "surge";
		c.wings = "surge";
		c.halo = "none";
		for (String id : new String[]{"keystrokes", "cps", "fps", "coords", "armor", "minimap", "effects", "compass"}) {
			HudModule m = Mods.hud(id);
			if (m != null) m.setEnabled(true);
		}
		LevelSettings settings = new LevelSettings("SurgeTest", GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(FeatureFlags.DEFAULT_FLAGS), WorldDataConfiguration.DEFAULT);
		mc.createWorldOpenFlows().createFreshLevel("SurgeTest" + System.currentTimeMillis(), settings, WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions, new TitleScreen());
	}

	public static void tick(Minecraft mc) {
		if (!active()) return;
		if (++total > 20 * 60 * 6) { LOG.error("Auto test timed out at step {}", index); mc.stop(); return; }
		if (STEPS.isEmpty()) plan();
		if (index < 0) {
			if (mc.getOverlay() != null || mc.screen == null) return;
			index = 0;
			ticks = 0;
			STEPS.get(0).action().accept(mc);
			return;
		}
		Step s = STEPS.get(index);
		if (s.name().equals("world")) {
			if (mc.player == null || mc.level == null) return;
			if (!inWorld) { inWorld = true; ticks = 0; }
			if (++ticks < 100) return;
			next(mc);
			return;
		}
		if (++ticks < s.delay()) return;
		shot(mc, s.name());
		next(mc);
	}

	private static void next(Minecraft mc) {
		index++;
		ticks = 0;
		if (index >= STEPS.size()) {
			LOG.info("SURGE AUTOTEST DONE");
			mc.stop();
			return;
		}
		try {
			STEPS.get(index).action().accept(mc);
		} catch (Throwable t) {
			LOG.error("Step {} failed", STEPS.get(index).name(), t);
		}
	}
}
