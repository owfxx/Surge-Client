package gg.surge.client.mixin;

import gg.surge.client.mods.Mods;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenTintMixin {
	@Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true, require = 0)
	private void surge$tint(GuiGraphics g, CallbackInfo ci) {
		gg.surge.client.SurgeClient.drawUnderScreen(g);
		if (Mods.GUI_SCALES.enabled() && Mods.GS_NO_TINT.on()) {
			gg.surge.client.features.GuiBackdrop.draw(g);
			ci.cancel();
		}
	}

	@Inject(method = "renderTransparentBackground", at = @At("TAIL"), require = 0)
	private void surge$backdrop(GuiGraphics g, CallbackInfo ci) {
		gg.surge.client.features.GuiBackdrop.draw(g);
	}
}
