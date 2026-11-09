package gg.surge.client.mixin;

import gg.surge.client.features.MoreFeatures;
import gg.surge.client.mods.Mods;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
	protected DisconnectedScreenMixin(Component title) { super(title); }

	@Inject(method = "init", at = @At("TAIL"), require = 0)
	private void surge$init(CallbackInfo ci) {
		if (!Mods.RECONNECT.enabled() || MoreFeatures.lastAddress == null) return;
		Button b = addRenderableWidget(Button.builder(Component.literal("Reconnect"), x -> MoreFeatures.reconnect(minecraft)).bounds(this.width / 2 - 100, this.height - 32, 200, 20).build());
		MoreFeatures.reconnectButton(this, b);
	}
}
