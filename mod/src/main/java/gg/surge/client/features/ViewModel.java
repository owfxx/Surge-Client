package gg.surge.client.features;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gg.surge.client.mods.Mods;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * View Model and Shield Fix: move, turn and resize the items you hold in first person.
 * Applied right before the item is drawn (after vanilla positioned it), so the arm stays put.
 */
public final class ViewModel {
	private ViewModel() {}

	public static void apply(PoseStack ps, Player player, InteractionHand hand, ItemStack stack) {
		boolean main = hand == InteractionHand.MAIN_HAND;
		HumanoidArm arm = main ? player.getMainArm() : player.getMainArm().getOpposite();
		float side = arm == HumanoidArm.RIGHT ? 1f : -1f;

		if (Mods.SHIELD_FIX.enabled() && stack.is(Items.SHIELD)) {
			float low = Mods.SHIELD_LOWER.get() / 100f;
			boolean blocking = player.isBlocking() && player.getUsedItemHand() == hand;
			ps.translate(0f, -0.35f * low * (blocking ? Mods.SHIELD_BLOCKING.get() / 100f : 1f), 0f);
			float sc = Mods.SHIELD_SCALE.get() / 100f;
			ps.scale(sc, sc, sc);
		}

		if (!Mods.VIEW_MODEL.enabled()) return;
		if (main) {
			ps.translate(side * Mods.VM_MAIN_X.get(), Mods.VM_MAIN_Y.get(), Mods.VM_MAIN_Z.get());
			rotate(ps, side, Mods.VM_MAIN_RX.get(), Mods.VM_MAIN_RY.get(), Mods.VM_MAIN_RZ.get());
			float s = Mods.VM_MAIN_SCALE.get() / 100f;
			ps.scale(s, s, s);
		} else {
			ps.translate(side * Mods.VM_OFF_X.get(), Mods.VM_OFF_Y.get(), Mods.VM_OFF_Z.get());
			rotate(ps, side, Mods.VM_OFF_RX.get(), Mods.VM_OFF_RY.get(), Mods.VM_OFF_RZ.get());
			float s = Mods.VM_OFF_SCALE.get() / 100f;
			ps.scale(s, s, s);
		}
	}

	private static void rotate(PoseStack ps, float side, float x, float y, float z) {
		if (x != 0) ps.mulPose(Axis.XP.rotationDegrees(x));
		if (y != 0) ps.mulPose(Axis.YP.rotationDegrees(y * side));
		if (z != 0) ps.mulPose(Axis.ZP.rotationDegrees(z * side));
	}
}
