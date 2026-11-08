package gg.surge.legacy.mixin;

import gg.surge.legacy.perf.Perf;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
	@Shadow private List<Particle>[][] particles;

	@Inject(method = "addParticle(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
	private void surge$limit(Particle p, CallbackInfo ci) {
		if (!Perf.PARTICLE_LIMIT.enabled()) return;
		int n = 0;
		for (List<Particle>[] layer : particles) for (List<Particle> l : layer) n += l.size();
		if (n >= Perf.PARTICLE_MAX.asInt()) ci.cancel();
	}
}
