package gg.surge.client.mixin;

import gg.surge.client.perf.Performance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void surge$cullFar(Particle particle, CallbackInfo ci) {
		if (!Performance.cullParticles) return;
		ParticleAccessor p = (ParticleAccessor) particle;
		Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().position();
		double dx = p.surge$x() - cam.x, dy = p.surge$y() - cam.y, dz = p.surge$z() - cam.z;
		double max = Performance.PARTICLE_DISTANCE;
		if (dx * dx + dy * dy + dz * dz > max * max) ci.cancel();
	}
}
