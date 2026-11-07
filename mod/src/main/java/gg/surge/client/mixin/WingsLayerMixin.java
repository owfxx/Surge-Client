package gg.surge.client.mixin;

import gg.surge.client.cosmetics.CapeTextures;
import gg.surge.client.cosmetics.CapturedLook;
import gg.surge.client.cosmetics.SurgeState;
import gg.surge.client.mods.Mods;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WingsLayer.class)
public abstract class WingsLayerMixin {
	@Inject(method = "getPlayerElytraTexture", at = @At("HEAD"), cancellable = true)
	private static void surge$capeElytra(HumanoidRenderState state, CallbackInfoReturnable<Identifier> cir) {
		if (!(state instanceof AvatarRenderState) || !Mods.COSMETICS.enabled() || !Mods.COS_CAPE_ELYTRA.on()) return;
		CapturedLook look = ((SurgeState) state).surge$look();
		if (look == null || look.cape() == null) return;
		Identifier tex = CapeTextures.get(look.cape());
		if (tex != null) cir.setReturnValue(tex);
	}
}
