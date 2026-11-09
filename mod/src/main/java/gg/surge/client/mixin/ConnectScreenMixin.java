package gg.surge.client.mixin;

import gg.surge.client.features.MoreFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {
	@Inject(method = "startConnecting", at = @At("HEAD"), require = 0)
	private static void surge$remember(Screen parent, Minecraft mc, ServerAddress address, ServerData data, boolean quick, TransferState transfer, CallbackInfo ci) {
		MoreFeatures.lastAddress = address;
		MoreFeatures.lastServer = data;
	}
}
