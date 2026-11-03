package gg.surge.client.mixin;

import gg.surge.client.features.Freelook;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Camera.class)
public abstract class CameraMixin {
	@ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"), index = 0)
	private float surge$yaw(float yaw) {
		return Freelook.active() ? Freelook.cameraYaw() : yaw;
	}

	@ModifyArg(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"), index = 1)
	private float surge$pitch(float pitch) {
		return Freelook.active() ? Freelook.cameraPitch() : pitch;
	}
}
