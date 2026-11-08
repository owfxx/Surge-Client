package gg.surge.legacy.mixin;

import gg.surge.legacy.cosmetic.CapeTextures;
import gg.surge.legacy.cosmetic.Cosmetics;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public class PlayerCapeMixin {
	@Inject(method = "getSkinId()Lnet/minecraft/util/Identifier;", at = @At("HEAD"), cancellable = true)
	private void surge$cape(CallbackInfoReturnable<Identifier> cir) {
		try {
			String c = Cosmetics.forPlayer((PlayerEntity) (Object) this, "cape");
			if (c != null) {
				Identifier id = CapeTextures.get(c);
				if (id != null) cir.setReturnValue(id);
			}
		} catch (Throwable ignored) { }
	}

	@Inject(method = "canRenderCapeTexture", at = @At("HEAD"), cancellable = true)
	private void surge$hasCape(CallbackInfoReturnable<Boolean> cir) {
		try {
			if (Cosmetics.forPlayer((PlayerEntity) (Object) this, "cape") != null) cir.setReturnValue(true);
		} catch (Throwable ignored) { }
	}
}
