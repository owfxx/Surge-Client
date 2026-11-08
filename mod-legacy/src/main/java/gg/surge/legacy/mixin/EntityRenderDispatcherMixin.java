package gg.surge.legacy.mixin;

import gg.surge.legacy.perf.Perf;
import net.minecraft.client.render.CameraView;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void surge$cull(Entity e, CameraView view, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
		try {
			if (Perf.skipEntity(e, x, y, z)) cir.setReturnValue(false);
		} catch (Throwable ignored) { }
	}
}
