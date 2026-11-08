package gg.surge.legacy.mixin;

import gg.surge.legacy.feature.Features;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class WorldMixin {
	@Shadow @Final public boolean isClient;

	@Inject(method = "getRainGradient", at = @At("HEAD"), cancellable = true)
	private void surge$rain(float d, CallbackInfoReturnable<Float> cir) {
		if (isClient && Features.WEATHER.enabled()) cir.setReturnValue(0f);
	}

	@Inject(method = "getThunderGradient", at = @At("HEAD"), cancellable = true)
	private void surge$thunder(float d, CallbackInfoReturnable<Float> cir) {
		if (isClient && Features.WEATHER.enabled()) cir.setReturnValue(0f);
	}
}
