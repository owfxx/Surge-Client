package gg.surge.client.mixin;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class HitboxMixin {
	private static Entity surge$entity;

	@Inject(method = "showHitboxes", at = @At("HEAD"), cancellable = true, require = 0)
	private void surge$start(Entity e, float partial, boolean server, CallbackInfo ci) {
		surge$entity = e;
		if (Mods.HITBOXES.enabled() && Mods.HB_PLAYERS.on() && !(e instanceof Player)) ci.cancel();
	}

	@ModifyArg(method = "showHitboxes", at = @At(value = "INVOKE", target = "Lnet/minecraft/gizmos/GizmoStyle;stroke(I)Lnet/minecraft/gizmos/GizmoStyle;", ordinal = 0), require = 0)
	private int surge$box(int color) {
		if (!Mods.HITBOXES.enabled() || color != -1) return color;
		Entity e = surge$entity;
		if (Mods.HB_COMBAT.on() && e != null) {
			if (e instanceof LivingEntity l && l.hurtTime > 0) return 0xFF000000 | (Mods.HB_HIT.rgb(0) & 0xFFFFFF);
			Minecraft mc = Minecraft.getInstance();
			if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() == e) return 0xFF000000 | (Mods.HB_TARGET.rgb(0) & 0xFFFFFF);
		}
		return 0xFF000000 | (Mods.HB_COLOR.rgb(0) & 0xFFFFFF);
	}

	@ModifyArg(method = "showHitboxes", at = @At(value = "INVOKE", target = "Lnet/minecraft/gizmos/GizmoStyle;stroke(I)Lnet/minecraft/gizmos/GizmoStyle;", ordinal = 2), require = 0)
	private int surge$eyes(int color) {
		return Mods.HITBOXES.enabled() && !Mods.HB_EYES.on() ? 0 : color;
	}
}
