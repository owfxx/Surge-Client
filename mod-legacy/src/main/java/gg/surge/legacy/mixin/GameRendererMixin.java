package gg.surge.legacy.mixin;

import gg.surge.legacy.feature.Features;
import gg.surge.legacy.perf.Perf;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
	@Shadow private void bobView(float tickDelta) { }

	@Redirect(method = "setupCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;bobView(F)V"), require = 0)
	private void surge$bob(GameRenderer self, float tickDelta) {
		if (!Features.MIN_BOBBING.enabled()) bobView(tickDelta);
	}

	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void surge$fov(float tickDelta, boolean changing, CallbackInfoReturnable<Float> cir) {
		if (changing) cir.setReturnValue(Features.fov(cir.getReturnValue()));
	}

	@Inject(method = "bobViewWhenHurt", at = @At("HEAD"), cancellable = true)
	private void surge$hurt(float tickDelta, CallbackInfo ci) {
		if (Features.HURTCAM.enabled()) ci.cancel();
	}

	@Inject(method = "render(FJ)V", at = @At("HEAD"))
	private void surge$frame(float tickDelta, long time, CallbackInfo ci) {
		Perf.frameStart();
	}

	@Redirect(method = "transformCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;yaw:F"))
	private float surge$yaw(Entity e) {
		return Features.freelook() ? Features.camYaw : e.yaw;
	}

	@Redirect(method = "transformCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevYaw:F"))
	private float surge$prevYaw(Entity e) {
		return Features.freelook() ? Features.camYaw : e.prevYaw;
	}

	@Redirect(method = "transformCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;pitch:F"))
	private float surge$pitch(Entity e) {
		return Features.freelook() ? Features.camPitch : e.pitch;
	}

	@Redirect(method = "transformCamera", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;prevPitch:F"))
	private float surge$prevPitch(Entity e) {
		return Features.freelook() ? Features.camPitch : e.prevPitch;
	}
}
