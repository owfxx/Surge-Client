package gg.surge.client.mixin;

import gg.surge.client.lod.Lod;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class DepthFarMixin {
	@Inject(method = "getDepthFar", at = @At("RETURN"), cancellable = true, require = 0)
	private void surge$far(CallbackInfoReturnable<Float> cir) {
		if (Lod.active()) cir.setReturnValue(Math.max(cir.getReturnValue(), Lod.farBlocks() * 1.5f));
	}
}
