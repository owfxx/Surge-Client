package gg.surge.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gg.surge.client.mods.Mods;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntityRenderer.class)
public class ItemPhysicsMixin {
	private static AABB surge$box;
	private static int surge$seed;

	@Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At("HEAD"), require = 0)
	private void surge$grab(ItemEntityRenderState state, PoseStack ps, SubmitNodeCollector q, CameraRenderState cam, CallbackInfo ci) {
		surge$box = state.item.getModelBoundingBox();
		surge$seed = state.seed;
	}

	private static boolean surge$flat() {
		return surge$box != null && surge$box.maxZ - surge$box.minZ < 0.1;
	}

	@Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), require = 0)
	private void surge$translate(PoseStack ps, float x, float y, float z) {
		if (!Mods.ITEM_PHYSICS.enabled() || surge$box == null) { ps.translate(x, y, z); return; }
		ps.translate(0f, surge$flat() ? (float) surge$box.maxZ + 0.01f : (float) -surge$box.minY, 0f);
	}

	@Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"), require = 0)
	private void surge$rotate(PoseStack ps, Quaternionfc q) {
		if (!Mods.ITEM_PHYSICS.enabled() || surge$box == null) { ps.mulPose(q); return; }
		ps.mulPose(Axis.YP.rotationDegrees(Math.floorMod(surge$seed, 360)));
		if (surge$flat()) ps.mulPose(Axis.XP.rotationDegrees(90f));
	}
}
