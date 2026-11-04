package gg.surge.client.mixin;

import gg.surge.client.mods.Mods;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
	@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
		at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Component surge$timestamp(Component message) {
		gg.surge.client.features.AutoGG.check(message.getString());
		message = gg.surge.client.features.NickHider.apply(message);
		if (!Mods.CHAT.enabled() || !Mods.CHAT_TIME.on()) return message;
		String pattern = (Mods.CHAT_24H.on() ? "HH:mm" : "h:mm") + (Mods.CHAT_SECONDS.on() ? ":ss" : "");
		String time = LocalTime.now().format(DateTimeFormatter.ofPattern(pattern));
		return Component.literal("[" + time + "] ").withStyle(ChatFormatting.GRAY).append(message);
	}
}
