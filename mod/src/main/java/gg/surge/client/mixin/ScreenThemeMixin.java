package gg.surge.client.mixin;

import gg.surge.client.gui.Ui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class ScreenThemeMixin {
	@Inject(method = "renderWithTooltipAndSubtitles", at = @At("HEAD"), require = 0)
	private void surge$begin(GuiGraphics g, int mx, int my, float d, CallbackInfo ci) {
		Ui.beginScreen((Screen) (Object) this);
	}

	@Inject(method = "renderWithTooltipAndSubtitles", at = @At("RETURN"), require = 0)
	private void surge$end(GuiGraphics g, int mx, int my, float d, CallbackInfo ci) {
		Ui.endScreen();
	}
}
