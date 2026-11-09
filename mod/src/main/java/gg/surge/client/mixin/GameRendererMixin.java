package gg.surge.client.mixin;

import gg.surge.client.features.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow private void bobView(com.mojang.blaze3d.vertex.PoseStack ps, float delta) { }

	@Shadow @org.spongepowered.asm.mixin.Final private com.mojang.blaze3d.resource.CrossFrameResourcePool resourcePool;

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;endFrame()V"), require = 0)
	private void surge$colorGrade(net.minecraft.client.DeltaTracker delta, boolean tick, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		gg.surge.client.features.ColorGrade.apply(net.minecraft.client.Minecraft.getInstance(), resourcePool);
	}

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void surge$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
		if (!useFovSetting) return;
		float d = Zoom.divisor();
		float fov = d != 1f ? cir.getReturnValue() / d : cir.getReturnValue();
		if (d != 1f) cir.setReturnValue(fov);
		Zoom.recordFov(fov);
	}

	@Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"), require = 0)
	private void surge$bob(GameRenderer self, com.mojang.blaze3d.vertex.PoseStack ps, float delta) {
		if (gg.surge.client.mods.Mods.MIN_BOBBING.on()) return;
		bobView(ps, delta);
	}
}
