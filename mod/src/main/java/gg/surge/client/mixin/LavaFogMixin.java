package gg.surge.client.mixin;

import gg.surge.client.features.FogTweaks;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.LavaFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LavaFogEnvironment.class)
public abstract class LavaFogMixin {
	@Inject(method = "setupFog", at = @At("TAIL"))
	private void surge$fog(FogData data, Camera camera, ClientLevel level, float renderDistance, DeltaTracker delta, CallbackInfo ci) {
		FogTweaks.simple(data, Mods.FOG_LAVA.on());
	}
}
