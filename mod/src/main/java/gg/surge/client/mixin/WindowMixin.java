package gg.surge.client.mixin;

import com.mojang.blaze3d.platform.Window;
import gg.surge.client.features.Borderless;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
	@Inject(method = "toggleFullScreen", at = @At("HEAD"), cancellable = true, require = 0)
	private void surge$borderless(CallbackInfo ci) {
		if (Borderless.bypass || !Borderless.wanted()) return;
		Borderless.toggle((Window) (Object) this);
		ci.cancel();
	}
}
