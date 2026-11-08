package gg.surge.client.mixin;

import gg.surge.client.perf.Performance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private void surge$cullFar(BlockEntity blockEntity, float partialTick, ModelFeatureRenderer.CrumblingOverlay overlay, CallbackInfoReturnable<?> cir) {
		if (!Performance.cullBlockEntities && !Performance.occlusion) return;
		BlockEntityType<?> type = blockEntity.getType();
		if (type == BlockEntityType.BEACON || type == BlockEntityType.END_GATEWAY || type == BlockEntityType.END_PORTAL || type == BlockEntityType.STRUCTURE_BLOCK) return;
		Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().position();
		BlockPos pos = blockEntity.getBlockPos();
		if (Performance.occlusion && gg.surge.client.perf.Occlusion.hidden(pos, cam.x, cam.y, cam.z)) { cir.setReturnValue(null); return; }
		if (!Performance.cullBlockEntities) return;
		double dx = pos.getX() + 0.5 - cam.x, dy = pos.getY() + 0.5 - cam.y, dz = pos.getZ() + 0.5 - cam.z;
		double max = Performance.BLOCK_ENTITY_DISTANCE;
		if (dx * dx + dy * dy + dz * dz > max * max) cir.setReturnValue(null);
	}
}
