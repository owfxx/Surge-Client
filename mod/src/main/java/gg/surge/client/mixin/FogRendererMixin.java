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
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
	@Inject(method = "computeFogColor", at = @At("RETURN"))
	private void surge$fogColor(Camera camera, float partial, ClientLevel level, int renderDistance, float darken, CallbackInfoReturnable<Vector4f> cir) {
		FogTweaks.color(cir.getReturnValue());
	}

	@ModifyArgs(method = "setupFog", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"))
	private void surge$fogDistances(Args args) {
		float f = FogTweaks.finalScale();
		if (f == 1f) return;
		for (int i = 3; i <= 6; i++) args.set(i, f < 0 ? 1_000_000f : (Float) args.get(i) * f);
	}
}
