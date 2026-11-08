package gg.surge.client.cosmetics;

import gg.surge.client.config.SurgeConfig;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.PlayerSkin;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.function.Supplier;

public final class MenuAvatar {
	private static Supplier<PlayerSkin> lookup;

	private MenuAvatar() {}

	public static PlayerSkin skin() {
		Minecraft mc = Minecraft.getInstance();
		if (lookup == null) {
			try {
				lookup = mc.getSkinManager().createLookup(mc.getGameProfile(), false);
			} catch (Throwable e) {
				PlayerSkin d = DefaultPlayerSkin.getDefaultSkin();
				lookup = () -> d;
			}
		}
		try {
			PlayerSkin s = lookup.get();
			return s != null ? s : DefaultPlayerSkin.getDefaultSkin();
		} catch (Throwable e) {
			return DefaultPlayerSkin.getDefaultSkin();
		}
	}

	private static String pick(String type, String saved) {
		if (Cosmetics.previewSub != null && Cosmetics.previewSub.equals(type)) return Cosmetics.previewKey;
		if (!Mods.COSMETICS.enabled()) return null;
		String eq = Cosmetics.equipped(type, saved);
		return "none".equals(eq) ? null : eq;
	}

	public static CapturedLook ownLook() {
		var c = SurgeConfig.get();
		Object[] l = gg.surge.client.emotes.Emotes.previewLook();
		return new CapturedLook(pick("cape", c.cape), pick("wings", c.wings), pick("halo", c.halo),
			l == null ? null : (gg.surge.client.emotes.Emotes.Emote) l[0], l == null ? 0f : (Float) l[1], l != null, petPick());
	}

	private static String petPick() {
		if (Cosmetics.previewSub != null && Cosmetics.previewSub.equals("pet")) return Cosmetics.previewKey;
		if (!Mods.COSMETICS.enabled()) return null;
		String w = Cosmetics.petWire();
		return "none".equals(w) ? null : w;
	}

	public static void render(GuiGraphics g, int x1, int y1, int x2, int y2, float size, float yaw, CapturedLook look) {
		AvatarRenderState st = new AvatarRenderState();
		PlayerSkin skin = skin();
		st.entityType = EntityType.PLAYER;
		st.skin = skin;
		st.showHat = st.showJacket = st.showLeftPants = st.showRightPants = st.showLeftSleeve = st.showRightSleeve = true;
		st.showCape = (look == null || look.cape() == null) && skin.cape() != null;
		st.bodyRot = 180f + yaw;
		st.yRot = 0f;
		st.xRot = 0f;
		st.scale = 1f;
		st.ageScale = 1f;
		st.ageInTicks = (System.currentTimeMillis() % 3_600_000L) / 50f;
		st.boundingBoxWidth = 0.6f;
		st.boundingBoxHeight = 1.8f;
		st.eyeHeight = 1.62f;
		st.lightCoords = 0xF000F0;
		st.walkAnimationPos = 0f;
		st.walkAnimationSpeed = 0f;
		st.capeFlap = 0f;
		st.capeLean = 0f;
		st.capeLean2 = 0f;
		st.id = -7;
		((SurgeState) st).surge$setLook(look);
		Quaternionf rot = new Quaternionf().rotateZ((float) Math.PI);
		Quaternionf cam = new Quaternionf();
		Vector3f t = new Vector3f(0f, 0.9f + 0.0625f, 0f);
		boolean was = Preview.clean;
		Preview.clean = true;
		try {
			g.submitEntityRenderState(st, size, t, rot, cam, x1, y1, x2, y2);
		} finally {
			Preview.clean = was;
		}
	}
}
