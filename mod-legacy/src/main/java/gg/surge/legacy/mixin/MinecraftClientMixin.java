package gg.surge.legacy.mixin;

import gg.surge.legacy.SurgeLegacy;
import gg.surge.legacy.perf.Perf;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void surge$tick(CallbackInfo ci) {
		SurgeLegacy.tick((MinecraftClient) (Object) this);
	}

	@Inject(method = "getMaxFramerate", at = @At("RETURN"), cancellable = true)
	private void surge$fps(CallbackInfoReturnable<Integer> cir) {
		int v = Perf.maxFps(cir.getReturnValue());
		if (v != cir.getReturnValue()) cir.setReturnValue(v);
	}
}
