package gg.surge.legacy.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import gg.surge.legacy.feature.Features;
import net.minecraft.client.render.WorldRenderer;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {
	@Redirect(method = "drawBlockOutline", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;color(FFFF)V"))
	private void surge$color(float r, float g, float b, float a) {
		if (!Features.OUTLINE.enabled()) { GlStateManager.color(r, g, b, a); return; }
		int c = Features.OUTLINE_COLOR.rgb(0);
		GlStateManager.color(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, Features.OUTLINE_ALPHA.get() / 100f);
	}

	@Redirect(method = "drawBlockOutline", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glLineWidth(F)V"))
	private void surge$width(float w) {
		GL11.glLineWidth(Features.OUTLINE.enabled() ? Features.OUTLINE_WIDTH.get() : w);
	}
}
