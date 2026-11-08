package gg.surge.client.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import gg.surge.client.emotes.Emotes;
import gg.surge.client.net.Skins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class Pets {
	public static final List<String> KINDS = List.of("plush", "bee", "warden", "allay", "axolotl", "fox", "frog");
	private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
	private static final Map<String, String> UUIDS = new ConcurrentHashMap<>();
	private static final Map<String, Long> ASKED = new ConcurrentHashMap<>();
	private static final Map<String, Model> MODELS = new HashMap<>();
	private static final Identifier STEVE = Identifier.withDefaultNamespace("textures/entity/player/wide/steve.png");

	private Pets() {}

	private record Model(ModelPart root, Function<String, ModelPart> parts) {
		ModelPart part(String name) {
			try { return parts.apply(name); } catch (Exception e) { return null; }
		}
	}

	private static ModelLayerLocation layer(String kind, boolean slim) {
		return switch (kind) {
			case "bee" -> ModelLayers.BEE;
			case "warden" -> ModelLayers.WARDEN;
			case "allay" -> ModelLayers.ALLAY;
			case "axolotl" -> ModelLayers.AXOLOTL;
			case "fox" -> ModelLayers.FOX;
			case "frog" -> ModelLayers.FROG;
			default -> slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER;
		};
	}

	private static Identifier texture(String kind) {
		return Identifier.withDefaultNamespace(switch (kind) {
			case "bee" -> "textures/entity/bee/bee.png";
			case "warden" -> "textures/entity/warden/warden.png";
			case "allay" -> "textures/entity/allay/allay.png";
			case "axolotl" -> "textures/entity/axolotl/axolotl_lucy.png";
			case "fox" -> "textures/entity/fox/fox.png";
			default -> "textures/entity/frog/temperate_frog.png";
		});
	}

	private static float scale(String kind) {
		return switch (kind) {
			case "bee" -> 0.55f;
			case "warden" -> 0.17f;
			case "allay" -> 0.55f;
			case "axolotl" -> 0.45f;
			case "fox" -> 0.4f;
			case "frog" -> 0.52f;
			default -> 0.23f;
		};
	}

	public static String displayName(String kind) {
		return switch (kind) {
			case "plush" -> "Skin Plush";
			case "bee" -> "Bee Buddy";
			case "warden" -> "Tiny Warden";
			case "allay" -> "Allay Friend";
			case "axolotl" -> "Axolotl";
			case "fox" -> "Sleepy Fox";
			case "frog" -> "Frog";
			default -> kind;
		};
	}

	private static Model model(String key, String kind, boolean slim) {
		String k = key + "|" + kind + (slim ? "s" : "");
		Model m = MODELS.get(k);
		if (m != null) return m;
		if (MODELS.size() > 96) MODELS.clear();
		ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(layer(kind, slim));
		m = new Model(root, root.createPartLookup());
		MODELS.put(k, m);
		return m;
	}

	private static String uuidFor(String name) {
		String n = name.toLowerCase(java.util.Locale.ROOT);
		String u = UUIDS.get(n);
		if (u != null) return u.isEmpty() ? null : u;
		Long at = ASKED.get(n);
		if (at != null && System.currentTimeMillis() - at < 120_000) return null;
		ASKED.put(n, System.currentTimeMillis());
		try {
			HTTP.sendAsync(HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + n)).timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString())
				.thenAccept(r -> {
					if (r.statusCode() == 200) {
						String id = com.google.gson.JsonParser.parseString(r.body()).getAsJsonObject().get("id").getAsString();
						UUIDS.put(n, id);
					} else if (r.statusCode() == 404 || r.statusCode() == 204) UUIDS.put(n, "");
				});
		} catch (Exception ignored) { }
		return null;
	}

	private static float sin(float v) {
		return (float) Math.sin(v);
	}

	private static void rot(ModelPart p, float x, float y, float z) {
		if (p == null) return;
		p.xRot += x;
		p.yRot += y;
		p.zRot += z;
	}

	public static void submit(PlayerModel parent, PoseStack ps, SubmitNodeCollector q, int light, String pet, float age, Emotes.Emote emote, float emoteTime, int key) {
		if (pet == null || pet.isEmpty() || "none".equals(pet)) return;
		String kind = pet, name = "";
		int c = pet.indexOf(':');
		if (c > 0) { kind = pet.substring(0, c); name = pet.substring(c + 1); }
		if (!KINDS.contains(kind)) return;
		Minecraft mc = Minecraft.getInstance();

		Identifier tex;
		boolean slim = false;
		if (kind.equals("plush")) {
			if (name.isEmpty()) name = mc.getUser().getName();
			String id = uuidFor(name);
			Skins.Skin sk = id == null ? null : Skins.get(id);
			tex = sk == null ? STEVE : sk.texture();
			slim = sk != null && sk.slim();
		} else tex = texture(kind);

		Model m = model(String.valueOf(key), kind, slim);
		for (ModelPart p : m.root().getAllParts()) p.resetPose();

		boolean dancing = emote != null;
		float beat = dancing ? emoteTime * (emote.bpm > 0 ? emote.bpm / 60f : 2f) * (float) Math.PI : 0f;
		float hop = dancing ? Math.abs(sin(beat)) : 0f;
		float t = age * 0.05f;

		ps.pushPose();
		parent.body.translateAndRotate(ps);
		boolean flies = kind.equals("bee") || kind.equals("allay");
		float hover = flies ? -0.16f + 0.04f * sin(age * 0.12f) : 0f;
		ps.translate(0.4f, -0.01f + hover - hop * 0.09f, 0.03f);
		ps.mulPose(Axis.YP.rotationDegrees(-18f + (dancing ? sin(beat * 0.5f) * 20f : sin(t * 0.7f) * 6f)));
		if (dancing) ps.mulPose(Axis.ZP.rotationDegrees(sin(beat) * 8f));
		float s = scale(kind);
		float squish = 1f + (dancing ? hop * 0.12f : 0.03f * sin(age * 0.09f));
		ps.scale(s / (float) Math.sqrt(squish), s * squish, s / (float) Math.sqrt(squish));
		ps.translate(0f, -1.501f, 0f);

		switch (kind) {
			case "bee" -> {
				float flap = sin(age * (dancing ? 3.2f : 2.4f)) * 0.6f;
				ModelPart lw = m.part("left_wing"), rw = m.part("right_wing");
				if (rw != null) { rw.yRot = 0f; rw.zRot = flap; }
				if (lw != null) { lw.yRot = 0f; lw.zRot = -flap; }
				rot(m.part("body"), 0.05f * sin(age * 0.1f), 0f, 0f);
				rot(m.part("left_antenna"), 0.15f * sin(age * 0.2f), 0, 0);
				rot(m.part("right_antenna"), 0.15f * sin(age * 0.2f + 1f), 0, 0);
				ModelPart st = m.part("stinger");
				if (st != null) st.visible = true;
			}
			case "warden" -> {
				rot(m.part("left_tendril"), 0, 0, 0.25f + 0.25f * sin(age * 0.25f));
				rot(m.part("right_tendril"), 0, 0, -0.25f - 0.25f * sin(age * 0.25f + 1f));
				rot(m.part("head"), 0.05f * sin(t), 0.2f * sin(t * 0.6f), 0);
				float arm = dancing ? -2.4f + sin(beat) * 0.4f : 0.08f * sin(age * 0.06f);
				rot(m.part("left_arm"), arm, 0, dancing ? -0.3f : 0);
				rot(m.part("right_arm"), dancing ? -2.4f - sin(beat) * 0.4f : -0.08f * sin(age * 0.06f), 0, dancing ? 0.3f : 0);
				rot(m.part("left_leg"), -1.4f, -0.2f, 0);
				rot(m.part("right_leg"), -1.4f, 0.2f, 0);
				rot(m.part("body"), 0.1f, 0, 0);
			}
			case "allay" -> {
				float flap = sin(age * (dancing ? 1.6f : 0.9f)) * 0.5f;
				ModelPart lw = m.part("left_wing"), rw = m.part("right_wing");
				if (rw != null) { rw.xRot = 0.43f; rw.yRot = -0.6f + flap; }
				if (lw != null) { lw.xRot = 0.43f; lw.yRot = 0.6f - flap; }
				float arm = dancing ? -2.6f + sin(beat) * 0.5f : -0.2f + 0.1f * sin(age * 0.08f);
				rot(m.part("left_arm"), arm, 0, -0.1f);
				rot(m.part("right_arm"), dancing ? -2.6f - sin(beat) * 0.5f : arm, 0, 0.1f);
				rot(m.part("head"), 0.06f * sin(t), 0.25f * sin(t * 0.5f), 0);
			}
			case "axolotl" -> {
				rot(m.part("tail"), 0, 0.35f * sin(age * (dancing ? 0.5f : 0.15f)), 0);
				rot(m.part("top_gills"), 0.2f * sin(age * 0.2f), 0, 0);
				rot(m.part("left_gills"), 0, 0.25f * sin(age * 0.2f), 0);
				rot(m.part("right_gills"), 0, -0.25f * sin(age * 0.2f), 0);
				rot(m.part("head"), 0, 0.15f * sin(t), 0);
				for (String leg : new String[]{"left_front_leg", "right_front_leg", "left_hind_leg", "right_hind_leg"}) rot(m.part(leg), dancing ? 0.6f * sin(beat + leg.length()) : 0.1f * sin(age * 0.1f), 0, 0);
			}
			case "fox" -> {
				boolean asleep = !dancing;
				ModelPart body = m.part("body");
				if (asleep) {
					rot(body, 0, 0, 0);
					rot(m.part("head"), 0.25f, -0.4f + 0.04f * sin(age * 0.05f), 0.2f);
					for (String leg : new String[]{"left_front_leg", "right_front_leg", "left_hind_leg", "right_hind_leg"}) {
						ModelPart l = m.part(leg);
						if (l != null) l.visible = false;
					}
					ps.translate(0, 0.35f, 0);
				} else rot(m.part("head"), 0, 0.3f * sin(beat), 0);
				rot(m.part("tail"), 0, 0, (dancing ? 0.6f : 0.12f) * sin(age * (dancing ? 0.6f : 0.08f)));
			}
			case "frog" -> {
				ModelPart croak = m.part("croaking_body");
				if (croak != null) croak.visible = dancing && sin(beat) > 0.6f;
				ModelPart tongue = m.part("tongue");
				if (tongue != null) tongue.visible = false;
				rot(m.part("head"), dancing ? -0.15f * hop : 0.03f * sin(age * 0.1f), 0, 0);
				rot(m.part("left_leg"), 0, 0, 0);
			}
			default -> {
				ModelPart head = m.part("head");
				if (head != null) { head.xScale = head.yScale = head.zScale = 1.55f; head.yRot = (dancing ? 0.4f * sin(beat * 0.5f) : 0.25f * sin(t * 0.5f)); head.zRot = dancing ? 0.15f * sin(beat) : 0.06f * sin(t * 0.8f); }
				ModelPart la = m.part("left_arm"), ra = m.part("right_arm");
				if (dancing) {
					if (la != null) { la.xRot = -2.6f + sin(beat) * 0.5f; la.zRot = -0.3f; }
					if (ra != null) { ra.xRot = -2.6f - sin(beat) * 0.5f; ra.zRot = 0.3f; }
				} else {
					if (la != null) { la.xRot = -0.25f + 0.08f * sin(age * 0.07f); la.zRot = -0.15f; }
					if (ra != null) { ra.xRot = -0.25f - 0.08f * sin(age * 0.07f); ra.zRot = 0.15f; }
				}
				float dangle = 0.25f * sin(age * 0.11f);
				ModelPart ll = m.part("left_leg"), rl = m.part("right_leg");
				if (ll != null) { ll.xRot = -1.45f + (dancing ? 0.5f * sin(beat) : dangle); ll.yRot = -0.15f; }
				if (rl != null) { rl.xRot = -1.45f + (dancing ? -0.5f * sin(beat) : -dangle); rl.yRot = 0.15f; }
				ps.translate(0, 0.72f, 0);
			}
		}
		q.submitModelPart(m.root(), ps, kind.equals("plush") ? RenderTypes.entityTranslucent(tex) : RenderTypes.entityCutoutNoCull(tex), light, OverlayTexture.NO_OVERLAY, null);
		ps.popPose();
	}
}
