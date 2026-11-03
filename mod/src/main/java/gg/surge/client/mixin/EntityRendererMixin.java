package gg.surge.client.mixin;

import gg.surge.client.Badges;
import gg.surge.client.perf.Performance;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Surge badge on player nametags, and entity culling: small, decorative entities stop rendering beyond a short distance. Mobs and players are never culled. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void surge$cullFar(Entity entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
		if (!Performance.cullEntities) return;
		EntityType<?> type = entity.getType();
		double max;
		if (type == EntityType.ITEM || type == EntityType.ARROW || type == EntityType.SPECTRAL_ARROW) max = Performance.ITEM_DISTANCE;
		else if (type == EntityType.EXPERIENCE_ORB) max = Performance.ORB_DISTANCE;
		else if (type == EntityType.ARMOR_STAND || type == EntityType.ITEM_FRAME || type == EntityType.GLOW_ITEM_FRAME) max = Performance.DECOR_DISTANCE;
		else return;
		if (entity.distanceToSqr(camX, camY, camZ) > max * max) cir.setReturnValue(false);
	}

	/** TNT Timer: lit TNT always gets a "nametag" (the countdown). */
	@Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Entity;D)Z", at = @At("RETURN"), cancellable = true)
	private void surge$tntName(Entity entity, double distSq, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof net.minecraft.world.entity.item.PrimedTnt && gg.surge.client.mods.Mods.TNT_TIMER.enabled()) cir.setReturnValue(true);
	}

	@Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
	private void surge$badge(Entity entity, CallbackInfoReturnable<Component> cir) {
		if (entity instanceof net.minecraft.world.entity.item.PrimedTnt tnt && gg.surge.client.mods.Mods.TNT_TIMER.enabled()) {
			float sec = tnt.getFuse() / 20f;
			net.minecraft.ChatFormatting col = sec > 2 ? net.minecraft.ChatFormatting.GREEN : sec > 1 ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.RED;
			cir.setReturnValue(Component.literal(String.format("%.1fs", sec)).withStyle(col));
			return;
		}
		if (!(entity instanceof Player)) return;
		Component name = cir.getReturnValue();
		if (name == null) return;
		if (Performance.hideFarNametags) {
			Minecraft mc = Minecraft.getInstance();
			double max = Performance.NAMETAG_DISTANCE;
			if (entity != mc.player && entity.distanceToSqr(mc.gameRenderer.getMainCamera().position()) > max * max) {
				cir.setReturnValue(null);
				return;
			}
		}
		Component out = name;
		String flag = gg.surge.client.net.SurgeNet.flagGlyph(entity.getUUID());
		if (!flag.isEmpty()) out = Component.empty().append(out).append(Component.literal(" " + flag).withStyle(st -> st.withColor(0xFFFFFF)));
		if (Badges.show(entity.getUUID())) out = Badges.prefix(out);
		if (out != name) cir.setReturnValue(out);
	}
}
