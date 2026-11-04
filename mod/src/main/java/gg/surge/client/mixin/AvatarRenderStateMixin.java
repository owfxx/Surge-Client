package gg.surge.client.mixin;

import gg.surge.client.cosmetics.CapturedLook;
import gg.surge.client.cosmetics.SurgeState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements SurgeState {
	@Unique private CapturedLook surge$look;

	@Override public CapturedLook surge$look() { return surge$look; }

	@Override public void surge$setLook(CapturedLook look) { surge$look = look; }
}
