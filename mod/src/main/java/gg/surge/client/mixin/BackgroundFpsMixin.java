package gg.surge.client.mixin;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FramerateLimitTracker.class)
public abstract class BackgroundFpsMixin {
	@Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true, require = 0)
	private void surge$background(CallbackInfoReturnable<Integer> cir) {
		if (!Mods.BG_FPS.enabled()) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc == null || mc.getWindow() == null) return;
		if (mc.getWindow().isMinimized() || mc.getWindow().isIconified()) cir.setReturnValue(Math.min(cir.getReturnValue(), 2));
		else if (!mc.isWindowActive()) cir.setReturnValue(Math.min(cir.getReturnValue(), Math.round(Mods.BG_FPS_LIMIT.get())));
	}
}
