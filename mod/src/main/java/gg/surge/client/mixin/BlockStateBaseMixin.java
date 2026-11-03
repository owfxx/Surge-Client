package gg.surge.client.mixin;

import gg.surge.client.perf.Performance;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
	@Shadow
	public abstract Block getBlock();

	@Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
	private void surge$hideFoliage(CallbackInfoReturnable<RenderShape> cir) {
		if (Performance.hideFoliage && Performance.isFoliage(this.getBlock())) {
			cir.setReturnValue(RenderShape.INVISIBLE);
		}
	}
}
