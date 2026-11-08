package gg.surge.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface GameModeAccessor {
	@Accessor("destroyProgress")
	float surge$destroyProgress();

	@Accessor("destroyBlockPos")
	net.minecraft.core.BlockPos surge$destroyPos();
}
