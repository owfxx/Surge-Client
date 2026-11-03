package gg.surge.client.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
	@Accessor("x") double surge$x();
	@Accessor("y") double surge$y();
	@Accessor("z") double surge$z();
}
