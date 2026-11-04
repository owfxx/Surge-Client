package gg.surge.client.mixin;

import gg.surge.client.features.Extras;
import gg.surge.client.mods.Mods;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientLevel$ClientLevelData")
public abstract class LevelTimeMixin {
	@Inject(method = "getDayTime", at = @At("RETURN"), cancellable = true)
	private void surge$time(CallbackInfoReturnable<Long> cir) {
		if (Mods.TIME_CHANGER.enabled()) cir.setReturnValue(Extras.dayTime(cir.getReturnValue()));
	}
}
