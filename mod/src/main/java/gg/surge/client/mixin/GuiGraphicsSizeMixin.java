package gg.surge.client.mixin;

import gg.surge.client.features.GuiScales;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsSizeMixin {
	@Inject(method = "guiWidth", at = @At("RETURN"), cancellable = true, require = 0)
	private void surge$width(CallbackInfoReturnable<Integer> cir) {
		if (GuiScales.hudFactor != 1f) cir.setReturnValue(Math.round(cir.getReturnValue() / GuiScales.hudFactor));
	}

	@Inject(method = "guiHeight", at = @At("RETURN"), cancellable = true, require = 0)
	private void surge$height(CallbackInfoReturnable<Integer> cir) {
		if (GuiScales.hudFactor != 1f) cir.setReturnValue(Math.round(cir.getReturnValue() / GuiScales.hudFactor));
	}
}
