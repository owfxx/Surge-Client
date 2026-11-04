package gg.surge.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("leftPos")
	int surge$left();

	@Accessor("topPos")
	int surge$top();

	@Accessor("imageWidth")
	int surge$width();

	@Accessor("imageHeight")
	int surge$height();
}
