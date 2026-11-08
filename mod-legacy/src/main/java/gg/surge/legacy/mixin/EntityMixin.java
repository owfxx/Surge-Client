package gg.surge.legacy.mixin;

import gg.surge.legacy.feature.Features;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
	@Inject(method = "increaseTransforms", at = @At("HEAD"), cancellable = true)
	private void surge$look(float yaw, float pitch, CallbackInfo ci) {
		if ((Object) this == MinecraftClient.getInstance().getCameraEntity() && Features.look(yaw, pitch)) ci.cancel();
	}
}
