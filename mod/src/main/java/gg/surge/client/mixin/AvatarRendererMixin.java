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

/** Clean previews: no armor, no held items, standing still. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
	private void surge$cleanPreview(Avatar entity, AvatarRenderState s, float partialTick, CallbackInfo ci) {
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
