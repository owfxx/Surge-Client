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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WingsLayer.class)
public abstract class WingsLayerMixin {
	@Inject(method = "getPlayerElytraTexture", at = @At("HEAD"), cancellable = true)
	private static void surge$capeElytra(HumanoidRenderState state, CallbackInfoReturnable<Identifier> cir) {
		if (!(state instanceof AvatarRenderState) || !Mods.COSMETICS.enabled() || !Mods.COS_CAPE_ELYTRA.on()) return;
		CapturedLook look = ((SurgeState) state).surge$look();
		if (look == null || look.cape() == null || CapeTextures.plainElytra(look.cape())) return;
		Identifier tex = CapeTextures.get(look.cape());
		if (tex != null) cir.setReturnValue(tex);
	}

	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0), require = 0)
	private void surge$followEmote(com.mojang.blaze3d.vertex.PoseStack ps, net.minecraft.client.renderer.SubmitNodeCollector q, int light, HumanoidRenderState state, float a, float b, CallbackInfo ci) {
		if (!(state instanceof AvatarRenderState)) return;
		CapturedLook look = ((SurgeState) state).surge$look();
		if (look == null || look.emote() == null) return;
		Object parent = ((net.minecraft.client.renderer.entity.layers.RenderLayer<?, ?>) (Object) this).getParentModel();
		if (parent instanceof net.minecraft.client.model.HumanoidModel<?> m) m.body.translateAndRotate(ps);
	}
}
