package gg.surge.client.mixin;

import gg.surge.client.cosmetics.CosmeticLayer;
import gg.surge.client.emotes.Emotes;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Emotes: after vanilla poses your player, the emote's animation takes over. */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void surge$emote(AvatarRenderState state, CallbackInfo ci) {
		net.minecraft.world.entity.player.Player p = CosmeticLayer.playerOf(state);
		if (p == null) return;
		if (p == net.minecraft.client.Minecraft.getInstance().player) Emotes.animate((PlayerModel) (Object) this);
		else Emotes.animateRemote((PlayerModel) (Object) this, p.getUUID());
	}
}
