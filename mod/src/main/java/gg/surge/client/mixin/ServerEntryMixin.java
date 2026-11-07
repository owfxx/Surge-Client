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
		net.minecraft.network.chat.Component label = code.isEmpty() || flag.isEmpty() ? net.minecraft.network.chat.Component.literal(code.isEmpty() ? region : code)
			: net.minecraft.network.chat.Component.empty().append(gg.surge.client.features.Flags.icon(flag)).append(" " + code);
		int right = e.getX() + e.getWidth() - 5;
		int w = font.width(label);
		int x = right - w;
		int y = e.getY() + 12;
		g.drawString(font, label, x, y, 0xFF9CA3AF, false);
		if (mouseX >= x - 2 && mouseX <= right + 2 && mouseY >= y - 1 && mouseY <= y + 9) {
			String place = code.isEmpty() ? region : region.substring(0, region.lastIndexOf(", ")) + ", " + gg.surge.client.features.Flags.name(code);
			var mc = Minecraft.getInstance();
			gg.surge.client.gui.Ui.tooltip(g, font, "Server location: " + place, mouseX, mouseY, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
		}
	}
}
