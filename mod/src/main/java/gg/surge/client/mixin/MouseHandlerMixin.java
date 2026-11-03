package gg.surge.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import gg.surge.client.features.Zoom;
import net.minecraft.client.MouseHandler;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** While zoomed, the mouse wheel changes the zoom instead of the hotbar slot. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onScroll", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSpectator()Z"), cancellable = true)
	private void surge$zoomScroll(CallbackInfo ci, @Local Vector2i scroll) {
		if (scroll.y != 0 && Zoom.scroll(scroll.y)) ci.cancel();
	}
}
