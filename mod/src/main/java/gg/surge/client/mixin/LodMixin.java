package gg.surge.client.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.resource.ResourceHandle;
import gg.surge.client.lod.Lod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LodMixin {
	@Inject(method = "addMainPass", at = @At("HEAD"), require = 0)
	private void surge$frustum(FrameGraphBuilder fg, Frustum frustum, Matrix4f mv, GpuBufferSlice fog, boolean b, LevelRenderState state, DeltaTracker dt, ProfilerFiller prof, CallbackInfo ci) {
		Lod.setFrustum(frustum);
	}

	@Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V", ordinal = 0), require = 0)
	private void surge$lod(GpuBufferSlice fog, LevelRenderState state, ProfilerFiller prof, Matrix4f modelView, ResourceHandle<?> a, ResourceHandle<?> b, boolean c, ResourceHandle<?> d, ResourceHandle<?> e, CallbackInfo ci) {
		Lod.render(modelView, state.cameraRenderState.pos);
	}
}
