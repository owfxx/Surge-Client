package gg.surge.client.mixin;

import gg.surge.client.gui.Ui;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsThemeMixin {
	@Unique private static boolean surge$inside;

	@ModifyArgs(method = "fill(Lcom/mojang/blaze3d/pipeline/RenderPipeline;IIIII)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;submitColoredRectangle(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;IIIIILjava/lang/Integer;)V"), require = 0)
	private void surge$fill(Args a) {
		if (!Ui.themed()) return;
		a.set(6, Ui.remap((Integer) a.get(6)));
		Integer second = a.get(7);
		if (second != null) a.set(7, Ui.remap(second));
	}

	@ModifyArgs(method = "fillGradient", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;submitColoredRectangle(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/client/gui/render/TextureSetup;IIIIILjava/lang/Integer;)V"), require = 0)
	private void surge$gradient(Args a) {
		if (!Ui.themed()) return;
		a.set(6, Ui.remap((Integer) a.get(6)));
		Integer second = a.get(7);
		if (second != null) a.set(7, Ui.remap(second));
	}

	@Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void surge$text(Font font, String s, int x, int y, int color, boolean shadow, CallbackInfo ci) {
		if (surge$inside || !Ui.themed()) return;
		surge$inside = true;
		try {
			Font f = Ui.swapFont(font);
			((GuiGraphics) (Object) this).drawString(f != null ? f : font, s, x, y, Ui.remapText(color), Ui.textShadow(shadow));
		} finally {
			surge$inside = false;
		}
		ci.cancel();
	}

	@Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void surge$textSeq(Font font, FormattedCharSequence s, int x, int y, int color, boolean shadow, CallbackInfo ci) {
		if (surge$inside || !Ui.themed()) return;
		surge$inside = true;
		try {
			Font f = Ui.swapFont(font);
			((GuiGraphics) (Object) this).drawString(f != null ? f : font, s, x, y, Ui.remapText(color), Ui.textShadow(shadow));
		} finally {
			surge$inside = false;
		}
		ci.cancel();
	}
}
