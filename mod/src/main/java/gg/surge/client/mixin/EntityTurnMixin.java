package gg.surge.client.mixin;

import gg.surge.client.features.Freelook;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Freelook: mouse movement turns the camera instead of your player. */
@Mixin(Entity.class)
public abstract class EntityTurnMixin {
	@Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true)
	private void surge$freelook(double dx, double dy, CallbackInfo ci) {
		if (Freelook.active() && (Object) this == Minecraft.getInstance().player) {
			Freelook.turn(dx, dy);
			ci.cancel();
		}
	}
}
