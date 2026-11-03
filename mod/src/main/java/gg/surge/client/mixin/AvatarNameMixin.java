package gg.surge.client.mixin;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvatarRenderer.class)
public abstract class AvatarNameMixin {
	@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("RETURN"), cancellable = true)
	private void surge$ownName(Avatar entity, double distSq, CallbackInfoReturnable<Boolean> cir) {
		Minecraft mc = Minecraft.getInstance();
		if (entity == mc.player && Mods.NAMETAGS.enabled() && Mods.TAG_OWN.on() && !mc.options.getCameraType().isFirstPerson()
			&& !mc.options.hideGui && !(mc.screen != null && gg.surge.client.cosmetics.Preview.clean)) cir.setReturnValue(true);
	}
}
