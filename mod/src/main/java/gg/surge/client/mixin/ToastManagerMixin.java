package gg.surge.client.mixin;

import gg.surge.client.features.MoreFeatures;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ToastManager.class)
public class ToastManagerMixin {
	@Inject(method = "addToast", at = @At("HEAD"), cancellable = true, require = 0)
	private void surge$hide(Toast toast, CallbackInfo ci) {
		if (MoreFeatures.hideToast(toast)) ci.cancel();
	}
}
