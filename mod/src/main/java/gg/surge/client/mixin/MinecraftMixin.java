package gg.surge.client.mixin;

import gg.surge.client.mods.Mods;
import gg.surge.client.gui.SurgeTitleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private Screen surge$mainMenu(Screen screen) {
		if (screen instanceof TitleScreen && Mods.MAIN_MENU.enabled()) return new SurgeTitleScreen();
		return screen;
	}
}
