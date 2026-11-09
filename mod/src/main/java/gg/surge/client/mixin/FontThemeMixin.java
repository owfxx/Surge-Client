package gg.surge.client.mixin;

import gg.surge.client.gui.Ui;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Font.class)
public class FontThemeMixin {
	private Font surge$swap() {
		return Ui.swapFont((Font) (Object) this);
	}

	@Inject(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), cancellable = true, require = 0)
	private void surge$w1(String s, CallbackInfoReturnable<Integer> cir) {
		Font f = surge$swap();
		if (f != null) cir.setReturnValue(f.width(s));
	}

	@Inject(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"), cancellable = true, require = 0)
	private void surge$w2(FormattedText s, CallbackInfoReturnable<Integer> cir) {
		Font f = surge$swap();
		if (f != null) cir.setReturnValue(f.width(s));
	}

	@Inject(method = "width(Lnet/minecraft/util/FormattedCharSequence;)I", at = @At("HEAD"), cancellable = true, require = 0)
	private void surge$w3(FormattedCharSequence s, CallbackInfoReturnable<Integer> cir) {
		Font f = surge$swap();
		if (f != null) cir.setReturnValue(f.width(s));
	}

	@Inject(method = "plainSubstrByWidth(Ljava/lang/String;IZ)Ljava/lang/String;", at = @At("HEAD"), cancellable = true, require = 0)
	private void surge$sub(String s, int w, boolean tail, CallbackInfoReturnable<String> cir) {
		Font f = surge$swap();
		if (f != null) cir.setReturnValue(f.plainSubstrByWidth(s, w, tail));
	}

	@Inject(method = "substrByWidth", at = @At("HEAD"), cancellable = true, require = 0)
	private void surge$sub2(FormattedText s, int w, CallbackInfoReturnable<FormattedText> cir) {
		Font f = surge$swap();
		if (f != null) cir.setReturnValue(f.substrByWidth(s, w));
	}

	@Inject(method = "split", at = @At("HEAD"), cancellable = true, require = 0)
	private void surge$split(FormattedText s, int w, CallbackInfoReturnable<List<FormattedCharSequence>> cir) {
		Font f = surge$swap();
		if (f != null) cir.setReturnValue(f.split(s, w));
	}

	@Inject(method = "wordWrapHeight", at = @At("HEAD"), cancellable = true, require = 0)
	private void surge$wrap(FormattedText s, int w, CallbackInfoReturnable<Integer> cir) {
		Font f = surge$swap();
		if (f != null) cir.setReturnValue(f.wordWrapHeight(s, w));
	}
}
