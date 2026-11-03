package gg.surge.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import gg.surge.client.mods.Mods;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {
	@Inject(method = "renderItemActivationAnimation", at = @At("HEAD"), cancellable = true)
	private void surge$totemStart(PoseStack poseStack, float partialTick, net.minecraft.client.renderer.SubmitNodeCollector q, CallbackInfo ci) {
		if (!Mods.TOTEMS.enabled()) return;
		if (Mods.TOTEM_HIDE.on()) { ci.cancel(); return; }
		float s = Mods.TOTEM_SCALE.get() / 100f;
		poseStack.pushPose();
		poseStack.scale(s, s, 1f);
	}

	@Inject(method = "renderItemActivationAnimation", at = @At("RETURN"))
	private void surge$totemEnd(PoseStack poseStack, float partialTick, net.minecraft.client.renderer.SubmitNodeCollector q, CallbackInfo ci) {
		if (Mods.TOTEMS.enabled() && !Mods.TOTEM_HIDE.on()) poseStack.popPose();
	}

	@Inject(method = "renderFire", at = @At("HEAD"))
	private static void surge$lowerFire(PoseStack poseStack, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
		poseStack.pushPose();
		if (Mods.LOW_FIRE.enabled()) poseStack.translate(0.0F, -Mods.FIRE_HEIGHT.get() / 100f, 0.0F);
	}

	@Inject(method = "renderFire", at = @At("RETURN"))
	private static void surge$lowerFireEnd(PoseStack poseStack, MultiBufferSource buffers, TextureAtlasSprite sprite, CallbackInfo ci) {
		poseStack.popPose();
	}
}
