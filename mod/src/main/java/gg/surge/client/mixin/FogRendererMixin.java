package gg.surge.client.mixin;

import gg.surge.client.features.FogTweaks;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
	@Inject(method = "computeFogColor", at = @At("RETURN"))
	private void surge$fogColor(Camera camera, float partial, ClientLevel level, int renderDistance, float darken, CallbackInfoReturnable<Vector4f> cir) {
		FogTweaks.color(cir.getReturnValue());
	}
}
