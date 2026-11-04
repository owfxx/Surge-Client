package gg.surge.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class SkinArt {
	private static volatile Object skin;
	private static boolean requested;

	private SkinArt() {}

	private static void request(Minecraft mc) {
		if (requested) return;
		requested = true;
		try {
			Object profile = Minecraft.class.getMethod("getGameProfile").invoke(mc);
			Object manager = mc.getSkinManager();
			for (Method m : manager.getClass().getMethods()) {
				if (m.getParameterCount() != 1 || !m.getParameterTypes()[0].isInstance(profile)) continue;
				Object r = m.invoke(manager, profile);
				if (accept(r)) return;
				if (r instanceof CompletableFuture<?> f) { f.thenAccept(SkinArt::accept); return; }
			}
		} catch (Throwable ignored) { }
	}

	private static boolean accept(Object r) {
		if (r instanceof Optional<?> o) r = o.orElse(null);
		if (r instanceof Supplier<?> s) r = s.get();
		if (r != null && r.getClass().getSimpleName().contains("Skin")) { skin = r; return true; }
		return false;
	}

	private static Object current(Minecraft mc) {
		request(mc);
		if (skin != null) return skin;
		try { return DefaultPlayerSkin.get(mc.getUser().getProfileId()); } catch (Throwable e) { return null; }
	}

	private static Identifier texture(Object o, int depth) {
		if (o == null || depth > 3) return null;
		if (o instanceof Identifier id) return id;
		try {
			if (o.getClass().isRecord()) {
				for (RecordComponent c : o.getClass().getRecordComponents()) {
					Object v = c.getAccessor().invoke(o);
					if (v instanceof Identifier id && c.getName().toLowerCase().contains("texture")) return id;
				}
				for (RecordComponent c : o.getClass().getRecordComponents()) {
					Object v = c.getAccessor().invoke(o);
					Identifier id = texture(v, depth + 1);
					if (id != null) return id;
				}
			}
			for (Method m : o.getClass().getMethods()) {
				if (m.getParameterCount() == 0 && m.getReturnType() == Identifier.class && m.getName().toLowerCase().contains("texture")) return (Identifier) m.invoke(o);
			}
		} catch (Throwable ignored) { }
		return null;
	}

	private static boolean slim(Object skin) {
		try {
			for (RecordComponent c : skin.getClass().getRecordComponents()) {
				Object v = c.getAccessor().invoke(skin);
				if (v != null && v.getClass().isEnum() && String.valueOf(v).toUpperCase().contains("SLIM")) return true;
			}
		} catch (Throwable ignored) { }
		return false;
	}

	private static void part(GuiGraphics g, Identifier tex, int x, int y, int s, int u, int v, int w, int h) {
		g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, (float) u, (float) v, w * s, h * s, w, h, 64, 64);
	}

	public static boolean draw(GuiGraphics g, int cx, int top, int scale) {
		Minecraft mc = Minecraft.getInstance();
		Object sk = current(mc);
		Identifier tex = texture(sk, 0);
		if (tex == null) return false;
		boolean slim = slim(sk);
		int aw = slim ? 3 : 4;
		int s = scale;
		int x = cx - 8 * s;
		int armL = x + (4 - aw) * s, armR = x + 12 * s;
		g.fill(x + 4 * s + 2 * s, top + 33 * s, x + 12 * s - 2 * s, top + 33 * s + s, 0x40000000);
		part(g, tex, armL, top + 8 * s, s, 44, 20, aw, 12);
		part(g, tex, armL, top + 8 * s, s, 44, 36, aw, 12);
		part(g, tex, armR, top + 8 * s, s, 36, 52, aw, 12);
		part(g, tex, armR, top + 8 * s, s, 52, 52, aw, 12);
		part(g, tex, x + 4 * s, top + 20 * s, s, 4, 20, 4, 12);
		part(g, tex, x + 4 * s, top + 20 * s, s, 4, 36, 4, 12);
		part(g, tex, x + 8 * s, top + 20 * s, s, 20, 52, 4, 12);
		part(g, tex, x + 8 * s, top + 20 * s, s, 4, 52, 4, 12);
		part(g, tex, x + 4 * s, top + 8 * s, s, 20, 20, 8, 12);
		part(g, tex, x + 4 * s, top + 8 * s, s, 20, 36, 8, 12);
		part(g, tex, x + 4 * s, top, s, 8, 8, 8, 8);
		part(g, tex, x + 4 * s, top, s, 40, 8, 8, 8);
		return true;
	}
}
