package gg.surge.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import gg.surge.client.features.CustomSounds;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundEngine.class)
public abstract class CustomSoundEngineMixin {
	@ModifyExpressionValue(method = "play", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getSound()Lnet/minecraft/client/resources/sounds/Sound;"), require = 0)
	private Sound surge$swap(Sound original, SoundInstance instance) {
		return CustomSounds.swap(instance, original);
	}
}
