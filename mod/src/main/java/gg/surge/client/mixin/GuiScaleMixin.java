package gg.surge.client.mixin;

import gg.surge.client.features.GuiScales;
import gg.surge.client.mods.Mods;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiScaleMixin {
	@Unique private boolean surge$hotbar, surge$chat, surge$tab, surge$title, surge$side;

	@Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"))
	private void surge$hotbarIn(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
		surge$hotbar = GuiScales.push(g, GuiScales.scale(Mods.GS_HOTBAR.asInt()), g.guiWidth() / 2f, g.guiHeight());
	}

	@Inject(method = "renderHotbarAndDecorations", at = @At("RETURN"))
	private void surge$hotbarOut(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
		if (surge$hotbar) g.pose().popMatrix();
		surge$hotbar = false;
	}

	@Inject(method = "renderChat", at = @At("HEAD"))
	private void surge$chatIn(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
		surge$chat = GuiScales.push(g, GuiScales.scale(Mods.GS_CHAT.asInt()), 0f, g.guiHeight() - 40f);
	}

	@Inject(method = "renderChat", at = @At("RETURN"))
	private void surge$chatOut(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
		if (surge$chat) g.pose().popMatrix();
		surge$chat = false;
	}

	@Inject(method = "renderTabList", at = @At("HEAD"))
	private void surge$tabIn(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
		surge$tab = GuiScales.push(g, GuiScales.scale(Mods.GS_TAB.asInt()), g.guiWidth() / 2f, 0f);
	}

	@Inject(method = "renderTabList", at = @At("RETURN"))
	private void surge$tabOut(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
		if (surge$tab) g.pose().popMatrix();
		surge$tab = false;
	}

	@Inject(method = "renderTitle", at = @At("HEAD"))
	private void surge$titleIn(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
		surge$title = GuiScales.push(g, GuiScales.scale(Mods.GS_TITLE.asInt()), g.guiWidth() / 2f, g.guiHeight() / 2f);
	}

	@Inject(method = "renderTitle", at = @At("RETURN"))
	private void surge$titleOut(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
		if (surge$title) g.pose().popMatrix();
		surge$title = false;
	}

	@Inject(method = "renderScoreboardSidebar", at = @At("HEAD"))
	private void surge$sideIn(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
		surge$side = GuiScales.push(g, GuiScales.scale(Mods.GS_SIDEBAR.asInt()), g.guiWidth(), g.guiHeight() / 2f);
	}

	@Inject(method = "renderScoreboardSidebar", at = @At("RETURN"))
	private void surge$sideOut(GuiGraphics g, DeltaTracker d, CallbackInfo ci) {
		if (surge$side) g.pose().popMatrix();
		surge$side = false;
	}
}
