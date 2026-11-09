package gg.surge.client.mixin;

import gg.surge.client.mods.Mods;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LevelRenderer.class)
public class BlockOutlineMixin {
	@ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
	private int surge$color(int color) {
		if (!Mods.BLOCK_OUTLINE.enabled()) return color;
		int a = Math.round(Mods.BO_ALPHA.get() * 2.55f);
		return (a << 24) | (Mods.BO_COLOR.rgb(0) & 0xFFFFFF);
	}

	@ModifyVariable(method = "renderHitOutline", at = @At("HEAD"), ordinal = 0, argsOnly = true, require = 0)
	private float surge$width(float width) {
		return Mods.BLOCK_OUTLINE.enabled() ? Mods.BO_WIDTH.get() : width;
	}
}
