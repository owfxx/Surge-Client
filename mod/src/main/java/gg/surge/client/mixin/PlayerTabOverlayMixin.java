package gg.surge.client.mixin;

import gg.surge.client.Badges;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Surge badge before names in the tab list. Targeted by name (and @Pseudo) so the
 * mod still builds and runs even if the tab list class is renamed in a future version.
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.gui.components.PlayerTabOverlay")
public abstract class PlayerTabOverlayMixin {
	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void surge$badge(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
		Component name = cir.getReturnValue();
		if (name == null) return;
		if (Badges.show(info.getProfile().id())) cir.setReturnValue(Badges.prefix(name));
	}
}
