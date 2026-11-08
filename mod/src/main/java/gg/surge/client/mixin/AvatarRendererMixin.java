package gg.surge.client.mixin;

import gg.surge.client.cosmetics.Preview;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void surge$cleanPreview(Avatar entity, AvatarRenderState s, float partialTick, CallbackInfo ci) {
		gg.surge.client.cosmetics.SurgeState st = (gg.surge.client.cosmetics.SurgeState) s;
		gg.surge.client.cosmetics.CapturedLook card = Preview.clean ? Preview.card : null;
		if (card != null) st.surge$setLook(card);
		else if (entity instanceof net.minecraft.world.entity.player.Player p) {
			gg.surge.client.emotes.Emotes.Emote em = null;
			float t = 0f;
			boolean prev = false;
			if (p == net.minecraft.client.Minecraft.getInstance().player) {
				Object[] l = gg.surge.client.emotes.Emotes.localLook();
				if (l != null) { em = (gg.surge.client.emotes.Emotes.Emote) l[0]; t = (Float) l[1]; prev = (Boolean) l[2]; }
			} else {
				long[] start = new long[1];
				em = gg.surge.client.net.SurgeNet.remoteEmote(p.getUUID(), start);
				if (em != null) t = (System.currentTimeMillis() - start[0]) / 1000f;
			}
			st.surge$setLook(new gg.surge.client.cosmetics.CapturedLook(
				gg.surge.client.cosmetics.Cosmetics.forPlayer(p, "cape"), gg.surge.client.cosmetics.Cosmetics.forPlayer(p, "wings"),
				gg.surge.client.cosmetics.Cosmetics.forPlayer(p, "halo"), em, t, prev, gg.surge.client.cosmetics.Cosmetics.forPlayer(p, "pet")));
		} else st.surge$setLook(null);
		gg.surge.client.cosmetics.CapturedLook lk = st.surge$look();
		if (lk != null && lk.cape() != null && !"none".equals(lk.cape())) s.showCape = false;
		if (!Preview.clean) return;
		s.headEquipment = ItemStack.EMPTY;
		s.chestEquipment = ItemStack.EMPTY;
		s.legsEquipment = ItemStack.EMPTY;
		s.feetEquipment = ItemStack.EMPTY;
		s.headItem.clear();
		s.wornHeadType = null;
		s.rightHandItemState.clear();
		s.leftHandItemState.clear();
		s.rightHandItemStack = ItemStack.EMPTY;
		s.leftHandItemStack = ItemStack.EMPTY;
		s.rightArmPose = HumanoidModel.ArmPose.EMPTY;
		s.leftArmPose = HumanoidModel.ArmPose.EMPTY;
		s.attackTime = 0f;
		s.walkAnimationPos = 0f;
		s.walkAnimationSpeed = 0f;
		s.isCrouching = false;
		s.isFallFlying = false;
		s.isVisuallySwimming = false;
		s.isPassenger = false;
		s.isUsingItem = false;
		s.swimAmount = 0f;
		s.hasRedOverlay = false;
		s.deathTime = 0f;
		s.pose = Pose.STANDING;
	}
}
