package gg.surge.client.mixin;

import gg.surge.client.mods.Mods;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Weather Changer: what you see on your screen only; the server's weather is untouched. */
@Mixin(Level.class)
public abstract class LevelWeatherMixin {
	@Inject(method = "getRainLevel(F)F", at = @At("HEAD"), cancellable = true)
	private void surge$rain(float partialTick, CallbackInfoReturnable<Float> cir) {
		if ((Object) this instanceof ClientLevel && Mods.WEATHER.enabled()) cir.setReturnValue(Mods.WEATHER_TYPE.is("Clear") ? 0f : 1f);
	}

	@Inject(method = "getThunderLevel(F)F", at = @At("HEAD"), cancellable = true)
	private void surge$thunder(float partialTick, CallbackInfoReturnable<Float> cir) {
		if ((Object) this instanceof ClientLevel && Mods.WEATHER.enabled()) cir.setReturnValue(Mods.WEATHER_TYPE.is("Thunder") ? 1f : 0f);
	}
}
