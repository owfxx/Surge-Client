package gg.surge.legacy.mixin;

import gg.surge.legacy.feature.Features;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public class ChatHudMixin {
	@ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;I)V", at = @At("HEAD"), argsOnly = true)
	private Text surge$message(Text t) {
		try {
			Features.chat(t.asUnformattedString());
			return Features.stamp(t);
		} catch (Throwable e) {
			return t;
		}
	}
}
