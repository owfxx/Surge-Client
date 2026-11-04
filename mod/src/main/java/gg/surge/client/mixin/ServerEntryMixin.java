package gg.surge.client.mixin;

import gg.surge.client.features.ServerRegions;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.gui.screens.multiplayer.ServerSelectionList$OnlineServerEntry")
public abstract class ServerEntryMixin {
	@Shadow @Final private ServerData serverData;

	@Inject(method = "renderContent", at = @At("TAIL"))
	private void surge$region(GuiGraphics g, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
		if (!Mods.SERVER_REGION.enabled()) return;
		String region = ServerRegions.get(serverData.ip);
		if (region == null || region.isEmpty()) return;
		LayoutElement e = (LayoutElement) (Object) this;
		var font = Minecraft.getInstance().font;
		String code = region.contains(", ") ? region.substring(region.lastIndexOf(", ") + 2) : "";
		String flag = code.isEmpty() ? "" : gg.surge.client.features.Flags.glyph(code);
		String label = code.isEmpty() ? region : (flag.isEmpty() ? code : flag + " " + code);
		int x = e.getX() + 37 + font.width(serverData.name) + 6;
		int y = e.getY() + 3;
		int max = e.getX() + e.getWidth() - 60;
		if (x + font.width(label) > max) return;
		g.drawString(font, label, x, y, 0xFF9CA3AF, false);
	}
}
