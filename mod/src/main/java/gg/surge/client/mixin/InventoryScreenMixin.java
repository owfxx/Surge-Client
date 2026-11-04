package gg.surge.client.mixin;

import gg.surge.client.cosmetics.Preview;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
	@ModifyArg(method = "renderEntityInInventoryFollowsMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;submitEntityRenderState"), index = 3)
	private static Quaternionf surge$turn(Quaternionf rotation) {
		float yaw = Preview.yaw;
		if (Preview.clean && yaw != 0f) return new Quaternionf(rotation).rotateY((float) Math.toRadians(yaw));
		return rotation;
	}
}
