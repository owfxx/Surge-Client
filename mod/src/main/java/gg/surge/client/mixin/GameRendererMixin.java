package gg.surge.client.mixin;

import gg.surge.client.features.Zoom;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void surge$zoom(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
		if (!useFovSetting) return;
		float d = Zoom.divisor();
		float fov = d != 1f ? cir.getReturnValue() / d : cir.getReturnValue();
		if (d != 1f) cir.setReturnValue(fov);
		Zoom.recordFov(fov);
	}
}
