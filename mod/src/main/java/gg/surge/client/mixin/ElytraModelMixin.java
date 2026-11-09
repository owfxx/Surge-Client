package gg.surge.client.mixin;

import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.object.equipment.ElytraModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ElytraModel.class)
public class ElytraModelMixin {
	@Redirect(method = "createLayer", at = @At(value = "NEW", target = "(F)Lnet/minecraft/client/model/geom/builders/CubeDeformation;"), require = 0)
	private static CubeDeformation surge$thin(float grow) {
		return new CubeDeformation(grow, grow, -0.5f);
	}
}
