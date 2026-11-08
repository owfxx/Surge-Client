package gg.surge.legacy.mixin;

import gg.surge.legacy.feature.Features;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerInteractionManager.class)
public class InteractionManagerMixin {
	@Inject(method = "attackEntity", at = @At("HEAD"))
	private void surge$attack(PlayerEntity player, Entity target, CallbackInfo ci) {
		try { Features.attacked(MinecraftClient.getInstance(), target); } catch (Throwable ignored) { }
	}
}
