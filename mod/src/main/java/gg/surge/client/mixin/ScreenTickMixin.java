package gg.surge.client.mixin;

import gg.surge.client.features.MoreFeatures;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenTickMixin {
	@Inject(method = "tick", at = @At("HEAD"), require = 0)
	private void surge$tick(CallbackInfo ci) {
		MoreFeatures.screenTick((Screen) (Object) this);
	}
}
