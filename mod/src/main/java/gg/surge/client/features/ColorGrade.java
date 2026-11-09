package gg.surge.client.features;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import gg.surge.client.mixin.PostChainAccessor;
import gg.surge.client.mixin.PostPassAccessor;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Map;

public final class ColorGrade {
	private ColorGrade() {}

	private static final Identifier ID = Identifier.fromNamespaceAndPath("surge", "color_grade");
	private static PostChain chain;
	private static GpuBuffer buffer;
	private static float[] written;
	private static boolean broken;

	private static float[] values() {
		float sat = Mods.CG_SATURATION.get() / 100f, bri = Mods.CG_BRIGHTNESS.get() / 100f, con = Mods.CG_CONTRAST.get() / 100f;
		float hue = (float) Math.toRadians(Mods.CG_HUE.get()), gamma = 1f / Math.max(0.2f, Mods.CG_GAMMA.get() / 100f), vig = Mods.CG_VIGNETTE.get() / 100f;
		float tintA = Mods.CG_TINT_AMOUNT.get() / 100f;
		int tint = Mods.CG_TINT.rgb(0);
		float tr = (tint >> 16 & 255) / 255f, tg = (tint >> 8 & 255) / 255f, tb = (tint & 255) / 255f;
		switch (Mods.CG_PRESET.get()) {
			case "Grayscale" -> { sat = 0f; bri = 1f; con = 1.05f; hue = 0; tintA = 0; gamma = 1f; vig = 0.15f; }
			case "Vivid" -> { sat = 1.45f; bri = 1.03f; con = 1.1f; hue = 0; tintA = 0; gamma = 1f; vig = 0.1f; }
			case "Warm" -> { sat = 1.1f; bri = 1.03f; con = 1.04f; hue = 0; tr = 1f; tg = 0.78f; tb = 0.5f; tintA = 0.18f; gamma = 1f; vig = 0.15f; }
			case "Cold" -> { sat = 0.95f; bri = 1f; con = 1.05f; hue = 0; tr = 0.55f; tg = 0.75f; tb = 1f; tintA = 0.2f; gamma = 1f; vig = 0.15f; }
			case "Sepia" -> { sat = 0.3f; bri = 1.02f; con = 1.05f; hue = 0; tr = 1f; tg = 0.8f; tb = 0.55f; tintA = 0.65f; gamma = 1f; vig = 0.3f; }
			case "Noir" -> { sat = 0f; bri = 0.95f; con = 1.35f; hue = 0; tintA = 0; gamma = 1.1f; vig = 0.45f; }
			case "Cinematic" -> { sat = 0.9f; bri = 0.98f; con = 1.15f; hue = 0; tr = 0.6f; tg = 0.85f; tb = 0.9f; tintA = 0.12f; gamma = 1.05f; vig = 0.35f; }
			default -> { }
		}
		return new float[]{sat, bri, con, hue, tr, tg, tb, tintA, gamma, vig, 0, 0};
	}

	public static void apply(Minecraft mc, GraphicsResourceAllocator pool) {
		if (broken || !Mods.COLOR_GRADE.enabled() || mc.level == null) return;
		try {
			PostChain c = mc.getShaderManager().getPostChain(ID, LevelTargetBundle.MAIN_TARGETS);
			if (c == null) return;
			if (c != chain) { chain = c; buffer = null; written = null; }
			float[] v = values();
			if (buffer == null) {
				for (PostPass p : ((PostChainAccessor) c).surge$passes()) {
					Map<String, GpuBuffer> u = ((PostPassAccessor) p).surge$uniforms();
					GpuBuffer old = u.get("ColorGradeConfig");
					if (old == null) continue;
					GpuBuffer fresh = RenderSystem.getDevice().createBuffer(() -> "surge color grade", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, (int) old.size());
					u.put("ColorGradeConfig", fresh);
					old.close();
					buffer = fresh;
				}
				if (buffer == null) { broken = true; return; }
			}
			if (written == null || !java.util.Arrays.equals(written, v)) {
				ByteBuffer b = MemoryUtil.memAlloc((int) buffer.size());
				try {
					for (float f : v) b.putFloat(f);
					while (b.hasRemaining()) b.put((byte) 0);
					b.flip();
					RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), b);
				} finally {
					MemoryUtil.memFree(b);
				}
				written = v;
			}
			c.process(mc.getMainRenderTarget(), pool);
		} catch (Throwable t) {
			broken = true;
			org.slf4j.LoggerFactory.getLogger("surge").warn("Color customizer turned off: {}", t.toString());
		}
	}
}
