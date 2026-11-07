package gg.surge.client.mixin;

import gg.surge.client.mods.Mods;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
	@Inject(method = "isDown", at = @At("HEAD"), cancellable = true)
	private void surge$toggles(CallbackInfoReturnable<Boolean> cir) {
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.options == null || mc.player == null || mc.screen != null) return;
		Object self = this;
		if (self == mc.options.keySprint && Mods.TOGGLE_SPRINT.enabled() && Mods.sprintToggled()) cir.setReturnValue(true);
		else if (self == mc.options.keyShift && Mods.TOGGLE_SNEAK.enabled() && Mods.sneakToggled() && !mc.player.getAbilities().flying) cir.setReturnValue(true);
	}
}
