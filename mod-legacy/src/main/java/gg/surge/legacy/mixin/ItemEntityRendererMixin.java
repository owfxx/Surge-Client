package gg.surge.legacy.mixin;

import gg.surge.legacy.perf.Perf;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
	@Inject(method = "method_10222", at = @At("RETURN"), cancellable = true)
	private void surge$stacks(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
		if (Perf.STACKS.enabled() && cir.getReturnValue() > 1) cir.setReturnValue(1);
	}
}
