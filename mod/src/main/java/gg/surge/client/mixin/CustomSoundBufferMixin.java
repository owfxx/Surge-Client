package gg.surge.client.mixin;

import com.mojang.blaze3d.audio.SoundBuffer;
import gg.surge.client.features.CustomSounds;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(SoundBufferLibrary.class)
public abstract class CustomSoundBufferMixin {
	@Inject(method = "getCompleteBuffer", at = @At("HEAD"), cancellable = true)
	private void surge$custom(Identifier path, CallbackInfoReturnable<CompletableFuture<SoundBuffer>> cir) {
		CompletableFuture<SoundBuffer> f = CustomSounds.buffer(path);
		if (f != null) cir.setReturnValue(f);
	}

	@Inject(method = "clear", at = @At("HEAD"))
	private void surge$clear(CallbackInfo ci) {
		CustomSounds.clearBuffers();
	}
}
