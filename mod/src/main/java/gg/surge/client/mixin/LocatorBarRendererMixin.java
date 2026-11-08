package gg.surge.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.contextualbar.LocatorBarRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.waypoints.PartialTickSupplier;
import net.minecraft.world.waypoints.TrackedWaypoint;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(LocatorBarRenderer.class)
public abstract class LocatorBarRendererMixin {
	private static TrackedWaypoint surge$current;

	@Inject(method = "method_70870", at = @At("HEAD"), require = 0)
	private void surge$remember(Entity camera, Level level, PartialTickSupplier ticks, GuiGraphics g, int top, TrackedWaypoint waypoint, CallbackInfo ci) {
		surge$current = waypoint;
	}

	@Redirect(method = "method_70870", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"), require = 0)
	private void surge$head(GuiGraphics g, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h, int color) {
		TrackedWaypoint wp = surge$current;
		if (wp != null && Mods.LOCATOR_HEADS.enabled()) {
			UUID id = wp.id().left().orElse(null);
			Minecraft mc = Minecraft.getInstance();
			PlayerInfo info = id == null || mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(id);
			if (info != null) {
				if (Mods.LH_OUTLINE.on()) g.fill(x - 1, y - 1, x + w + 1, y + h + 1, color | 0xFF000000);
				else g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
				PlayerFaceRenderer.draw(g, info.getSkin(), x, y, w);
				return;
			}
		}
		g.blitSprite(pipeline, sprite, x, y, w, h, color);
	}
}
