package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Totem Tweaks: pop counter in chat, smaller or hidden totem animation (see ScreenEffectRendererMixin). */
public final class TotemTweaks {
	private static final Map<UUID, Integer> POPS = new HashMap<>();
	private static Object lastLevel;

	private TotemTweaks() {}

	public static void popped(ClientboundEntityEventPacket packet) {
		Minecraft mc = Minecraft.getInstance();
		if (!Mods.TOTEMS.enabled() || mc.level == null) return;
		if (lastLevel != mc.level) { POPS.clear(); lastLevel = mc.level; }
		Entity e = packet.getEntity(mc.level);
		if (!(e instanceof Player p)) return;
		int n = POPS.merge(p.getUUID(), 1, Integer::sum);
		if (!Mods.TOTEM_CHAT.on() || mc.player == null) return;
		boolean you = p == mc.player;
		if (you && !Mods.TOTEM_CHAT_SELF.on()) return;
		mc.player.displayClientMessage(Component.literal(you ? "You popped a totem" : p.getName().getString() + " popped a totem")
			.withStyle(ChatFormatting.GOLD)
			.append(Component.literal(" (" + n + (n == 1 ? " pop)" : " pops)")).withStyle(ChatFormatting.GRAY)), false);
	}

	public static void clear(UUID id) {
		POPS.remove(id);
	}
}
