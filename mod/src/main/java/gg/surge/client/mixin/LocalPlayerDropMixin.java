package gg.surge.client.mixin;

import gg.surge.client.features.Extras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerDropMixin {
	@Inject(method = {"drop", "dropSelectedItem"}, at = @At("HEAD"), cancellable = true)
	private void surge$protect(boolean all, CallbackInfoReturnable<Boolean> cir) {
		if (!Extras.allowDrop(Minecraft.getInstance())) cir.setReturnValue(false);
	}
}
