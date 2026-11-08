package gg.surge.legacy.mixin;

import gg.surge.legacy.gui.Draw;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
	@Inject(method = "render", at = @At("TAIL"))
	private void surge$brand(int mx, int my, float delta, CallbackInfo ci) {
		Screen s = (Screen) (Object) this;
		Draw.text("Surge Client  |  Right Shift for mods", 2, s.height - 20, 0xFFB483FF, true);
	}
}
