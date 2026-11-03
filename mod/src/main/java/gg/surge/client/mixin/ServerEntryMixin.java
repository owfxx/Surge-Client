package gg.surge.client.mixin;

import gg.surge.client.features.ServerRegions;
import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Server Region: where each server is, shown small under its ping in the server list. */
@Mixin(targets = "net.minecraft.client.gui.screens.multiplayer.ServerSelectionList$OnlineServerEntry")
public abstract class ServerEntryMixin {
	@Shadow @Final private ServerData serverData;

	@Inject(method = "renderContent", at = @At("TAIL"))
	private void surge$region(GuiGraphics g, int mouseX, int mouseY, boolean hovered, float partialTick, CallbackInfo ci) {
		if (!Mods.SERVER_REGION.enabled()) return;
		String region = ServerRegions.get(serverData.ip);
		if (region == null || region.isEmpty()) return;
		AbstractSelectionList.Entry<?> e = (AbstractSelectionList.Entry<?>) (Object) this;
		var font = Minecraft.getInstance().font;
		int right = e.getContentRight() - 2, bottom = e.getContentY() + e.getContentHeight() - 9;
		g.pose().pushMatrix();
		g.pose().translate(right, bottom);
		g.pose().scale(0.75f, 0.75f);
		g.drawString(font, region, -font.width(region), 0, 0xFF9CA3AF, false);
		g.pose().popMatrix();
	}
}
