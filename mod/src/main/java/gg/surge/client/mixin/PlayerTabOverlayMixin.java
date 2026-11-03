package gg.surge.client.mixin;

import gg.surge.client.Badges;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.minecraft.client.gui.components.PlayerTabOverlay")
public abstract class PlayerTabOverlayMixin {
	@Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
	private void surge$badge(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
		Component name = cir.getReturnValue();
		if (name == null) return;
		if (Badges.show(info.getProfile().id())) cir.setReturnValue(Badges.prefix(name));
	}

	@Inject(method = "renderPingIcon", at = @At("HEAD"), cancellable = true)
	private void surge$ping(net.minecraft.client.gui.GuiGraphics g, int width, int x, int y, PlayerInfo info, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		if (!gg.surge.client.mods.Mods.TAB_LIST.enabled() || !gg.surge.client.mods.Mods.TAB_PING_NUMBER.on()) return;
		int ms = info.getLatency();
		String t = String.valueOf(ms);
		int col = !gg.surge.client.mods.Mods.TAB_PING_COLOR.on() ? 0xFFFFFFFF : ms < 80 ? 0xFF55FF55 : ms < 160 ? 0xFFFFFF55 : ms < 300 ? 0xFFFFAA00 : 0xFFFF5555;
		var font = Minecraft.getInstance().font;
		g.pose().pushMatrix();
		g.pose().translate(x + width - 1, y + 2);
		g.pose().scale(0.75f, 0.75f);
		g.drawString(font, t, -font.width(t), 0, col, true);
		g.pose().popMatrix();
		ci.cancel();
	}

	@com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
	private void surge$noBackground(net.minecraft.client.gui.GuiGraphics g, int x1, int y1, int x2, int y2, int color, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
		if (gg.surge.client.mods.Mods.TAB_LIST.enabled() && gg.surge.client.mods.Mods.TAB_NO_BG.on()) return;
		original.call(g, x1, y1, x2, y2, color);
	}
}
