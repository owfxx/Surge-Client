package gg.surge.legacy.mixin;

import gg.surge.legacy.SurgeLegacy;
import gg.surge.legacy.feature.Features;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.Window;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class InGameHudMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void surge$hud(float tickDelta, CallbackInfo ci) {
		SurgeLegacy.hud(MinecraftClient.getInstance(), tickDelta);
	}

	@Inject(method = "renderScoreboardObjective", at = @At("HEAD"), cancellable = true)
	private void surge$hideBoard(ScoreboardObjective o, Window w, CallbackInfo ci) {
		if (Features.SCOREBOARD.enabled() && Features.SB_HIDE.on()) ci.cancel();
	}

	@Redirect(method = "renderScoreboardObjective", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;draw(Ljava/lang/String;III)I", ordinal = 1))
	private int surge$score(TextRenderer r, String s, int x, int y, int color) {
		if (Features.SCOREBOARD.enabled() && Features.SB_NUMBERS.on()) return 0;
		return r.draw(s, x, y, color);
	}
}
