package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Optional;

public final class NickHider {
	private NickHider() {}

	public static boolean on() {
		return Mods.NICK_HIDER.enabled();
	}

	public static String nick() {
		String n = Mods.NICK_NAME.get();
		return n.equals("Hidden") ? "•••••" : n;
	}

	public static Component apply(Component c) {
		if (c == null || !on()) return c;
		String name = Minecraft.getInstance().getUser().getName();
		if (name.isEmpty() || !c.getString().contains(name)) return c;
		String nick = nick();
		MutableComponent out = Component.empty();
		c.visit((style, text) -> {
			out.append(Component.literal(text.replace(name, nick)).withStyle(style));
			return Optional.empty();
		}, Style.EMPTY);
		return out;
	}
}
