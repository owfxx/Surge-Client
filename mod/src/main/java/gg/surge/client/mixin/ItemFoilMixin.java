package gg.surge.client.mixin;

import gg.surge.client.mods.Mods;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemFoilMixin {
	@Inject(method = "hasFoil", at = @At("RETURN"), cancellable = true, require = 0)
	private void surge$shiny(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() || !Mods.SHINY_POTS.enabled()) return;
		ItemStack s = (ItemStack) (Object) this;
		if (s.getItem() instanceof PotionItem) cir.setReturnValue(true);
	}
}
