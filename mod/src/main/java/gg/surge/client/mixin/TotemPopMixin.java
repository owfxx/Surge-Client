package gg.surge.client.mixin;

import gg.surge.client.features.TotemTweaks;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Totem Tweaks: count totem pops (entity event 35). TAIL only runs on the game thread. */
@Mixin(ClientPacketListener.class)
public abstract class TotemPopMixin {
	@Inject(method = "handleEntityEvent", at = @At("TAIL"))
	private void surge$totemPop(ClientboundEntityEventPacket packet, CallbackInfo ci) {
		if (packet.getEventId() == 35) TotemTweaks.popped(packet);
	}
}
