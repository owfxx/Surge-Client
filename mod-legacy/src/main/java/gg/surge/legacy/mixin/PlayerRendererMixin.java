package gg.surge.legacy.mixin;

import gg.surge.legacy.cosmetic.SurgeFeature;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerRendererMixin {
	@Inject(method = "<init>(Lnet/minecraft/client/render/entity/EntityRenderDispatcher;Z)V", at = @At("TAIL"))
	private void surge$features(EntityRenderDispatcher d, boolean slim, CallbackInfo ci) {
		((LivingRendererAccessor) this).surge$features().add(new SurgeFeature((PlayerEntityRenderer) (Object) this));
	}
}
