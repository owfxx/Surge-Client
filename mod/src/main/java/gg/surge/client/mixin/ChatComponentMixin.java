package gg.surge.client.mixin;

import gg.surge.client.mods.Mods;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
	@Shadow @Final private List<GuiMessage> allMessages;

	@Shadow protected abstract void refreshTrimmedMessages();

	@Unique private static String surge$last;
	@Unique private static int surge$count;

	@Inject(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
		at = @At("HEAD"), cancellable = true)
	private void surge$filter(Component message, MessageSignature sig, GuiMessageTag tag, CallbackInfo ci) {
		if (!Mods.CHAT.enabled() || !Mods.CHAT_HIDE_JOINS.on()) return;
		if (message.getContents() instanceof TranslatableContents tc && (tc.getKey().startsWith("multiplayer.player.joined") || tc.getKey().equals("multiplayer.player.left"))) ci.cancel();
	}

	@ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
		at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private Component surge$timestamp(Component message) {
		gg.surge.client.features.AutoGG.check(message.getString());
		message = gg.surge.client.features.NickHider.apply(message);
		if (!Mods.CHAT.enabled()) return message;
		String raw = message.getString();
		if (Mods.CHAT_STACK.on()) {
			if (raw.equals(surge$last) && !raw.isBlank() && !allMessages.isEmpty()) {
				surge$count++;
				allMessages.remove(0);
				refreshTrimmedMessages();
				message = Component.empty().append(message).append(Component.literal(" [x" + surge$count + "]").withStyle(ChatFormatting.GRAY));
			} else {
				surge$last = raw;
				surge$count = 1;
			}
		}
		if (Mods.CHAT_MENTION.on()) {
			Minecraft mc = Minecraft.getInstance();
			String me = mc.getUser().getName().toLowerCase(Locale.ROOT);
			String lower = raw.toLowerCase(Locale.ROOT);
			int at = lower.indexOf(me);
			boolean own = lower.startsWith("<" + me + ">") || lower.startsWith(me + ":") || lower.startsWith(me + " »");
			if (at >= 0 && !own && surge$count <= 1) {
				mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.NOTE_BLOCK_PLING.value(), 1.6f, 0.6f));
				message = Component.literal("» ").withStyle(ChatFormatting.GOLD).append(message);
			}
		}
		if (!Mods.CHAT_TIME.on()) return message;
		String pattern = (Mods.CHAT_24H.on() ? "HH:mm" : "h:mm") + (Mods.CHAT_SECONDS.on() ? ":ss" : "");
		String time = LocalTime.now().format(DateTimeFormatter.ofPattern(pattern));
		return Component.literal("[" + time + "] ").withStyle(ChatFormatting.GRAY).append(message);
	}

	@ModifyConstant(method = {"addMessageToDisplayQueue", "addMessageToQueue"}, constant = @Constant(intValue = 100))
	private int surge$history(int max) {
		return Mods.CHAT.enabled() && Mods.CHAT_LONG.on() ? 1000 : max;
	}
}
