package gg.surge.client.mixin;

import gg.surge.client.mods.Mods;
import gg.surge.client.gui.SurgeTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen surge$mainMenu(Screen screen) {
		if (screen instanceof TitleScreen && Mods.MAIN_MENU.enabled()) return new SurgeTitleScreen();
		return screen;
	}

	@Inject(method = "setScreen", at = @At("TAIL"))
	private void surge$guiScale(Screen screen, CallbackInfo ci) {
		gg.surge.client.features.GuiScales.screenChanged((Minecraft) (Object) this);
	}

	@ModifyArg(method = "resizeDisplay", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;setGuiScale(I)V"))
	private int surge$inventoryScale(int scale) {
		return gg.surge.client.features.GuiScales.adjust(scale);
	}
}
