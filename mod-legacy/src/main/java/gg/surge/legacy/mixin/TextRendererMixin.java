package gg.surge.legacy.mixin;

import gg.surge.legacy.feature.Features;
import net.minecraft.client.font.TextRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextRenderer.class)
public class TextRendererMixin {
	@ModifyVariable(method = "draw(Ljava/lang/String;FFIZ)I", at = @At("HEAD"), argsOnly = true)
	private String surge$nick(String s) {
		return Features.nick(s);
	}

	@ModifyVariable(method = "getStringWidth", at = @At("HEAD"), argsOnly = true)
	private String surge$nickWidth(String s) {
		return Features.nick(s);
	}
}
