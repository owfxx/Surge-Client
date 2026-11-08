package gg.surge.client.mixin;

import gg.surge.client.Badges;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Display.TextDisplay.class)
public abstract class TextDisplayMixin {
	@Inject(method = "getText", at = @At("RETURN"), cancellable = true)
	private void surge$badge(CallbackInfoReturnable<Component> cir) {
		Entity self = (Entity) (Object) this;
		if (!self.level().isClientSide()) return;
		Entity v = self.getVehicle();
		if (v instanceof Player p && cir.getReturnValue() != null && !cir.getReturnValue().getString().isBlank() && Badges.show(p.getUUID())) cir.setReturnValue(Badges.prefix(p.getUUID(), cir.getReturnValue()));
	}
}
