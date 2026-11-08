package gg.surge.legacy.mixin;

import gg.surge.legacy.perf.Perf;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRenderDispatcherMixin {
	@Shadow public double cameraX;
	@Shadow public double cameraY;
	@Shadow public double cameraZ;

	@Inject(method = "renderEntity(Lnet/minecraft/block/entity/BlockEntity;FI)V", at = @At("HEAD"), cancellable = true)
	private void surge$cull(BlockEntity be, float tickDelta, int stage, CallbackInfo ci) {
		try {
			if (Perf.skipBlockEntity(be, cameraX, cameraY, cameraZ)) ci.cancel();
		} catch (Throwable ignored) { }
	}
}
