package gg.surge.client.mixin;

import net.minecraft.client.gui.components.PlayerSkinWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerSkinWidget.class)
public interface PlayerSkinWidgetAccessor {
	@Accessor("rotationY")
	float surge$rotY();

	@Accessor("rotationY")
	void surge$setRotY(float v);

	@Accessor("rotationX")
	void surge$setRotX(float v);
}
