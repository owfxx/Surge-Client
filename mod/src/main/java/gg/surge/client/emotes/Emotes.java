package gg.surge.client.emotes;

import gg.surge.client.economy.Wallet;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;

public final class Emotes {
	public enum Emote {
		WAVE("Wave", 3.0f, false), CLAP("Clap", 3.0f, false), FACEPALM("Facepalm", 3.0f, false),
		DAB("Dab", 2.2f, false), SHRUG("Shrug", 2.4f, false), SALUTE("Salute", 3.0f, false),
		CHEER("Cheer", 3.5f, false), LAUGH("Laugh", 3.5f, false), BOW("Bow", 3.0f, false),
		HEART("Heart", 3.5f, false), FLOSS("Floss", 4.0f, true), TPOSE("T-Pose", 4.0f, true),
		CRY("Cry", 4.0f, false), SIT("Sit", 6.0f, true), GRIDDY("Griddy", 13.2f, "griddy", 146),
		DISCO("Disco Fever", 16.0f, "disco", 120);

		public final String label;
		public final float seconds;
		public final boolean loops;
		public final String music;
		public final float bpm;

		Emote(String label, float seconds, boolean loops) {
			this.label = label;
			this.seconds = seconds;
			this.loops = loops;
			this.music = null;
			this.bpm = 120;
		}

		Emote(String label, float seconds, String music, float bpm) {
			this.label = label;
			this.seconds = seconds;
			this.loops = false;
			this.music = music;
			this.bpm = bpm;
		}

		public static Emote byName(String name) {
			for (Emote e : values()) if (e.name().equals(name)) return e;
			return null;
		}
	}

	public static final java.util.Set<Emote> WALKABLE = java.util.EnumSet.of(Emote.WAVE, Emote.CLAP, Emote.FACEPALM, Emote.SHRUG, Emote.SALUTE,
		Emote.LAUGH, Emote.HEART, Emote.TPOSE, Emote.DAB, Emote.CRY);
	public static volatile boolean walking;

	private static Emote current;
	private static long startNanos;
	private static CameraType previousCamera;
	private static Emote preview;
	private static long previewStart;

	private Emotes() {}

	public static boolean playing() {
		return current != null;
	}

	public static boolean owned(Emote e) {
		return Wallet.get().owns("emote:" + e.name());
	}

	public static void setPreview(Emote e) {
		if (e != preview) previewStart = System.nanoTime();
		preview = e;
	}

	public static void play(Minecraft mc, Emote emote) {
		LocalPlayer p = mc.player;
		if (p == null || !owned(emote)) return;
		if (current != null) stop(mc);
		current = emote;
		startNanos = System.nanoTime();
		previousCamera = mc.options.getCameraType();
		if (WALKABLE.contains(emote)) { if (previousCamera.isFirstPerson()) mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); }
		else mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
		gg.surge.client.net.SurgeNet.playedEmote(emote.name());
		EmoteMusic.start(p, emote, System.currentTimeMillis());
	}

	public static void stop(Minecraft mc) {
		if (current == null) return;
		if (previousCamera != null) mc.options.setCameraType(previousCamera);
		current = null;
		if (mc.player != null) EmoteMusic.stop(mc.player.getUUID());
		gg.surge.client.net.SurgeNet.playedEmote("none");
	}

	public static void tick(Minecraft mc) {
		EmoteMusic.tick(mc);
		LocalPlayer p = mc.player;
		if (current == null) return;
		if (p == null) { current = null; return; }
		var o = mc.options;
		float t = (System.nanoTime() - startNanos) / 1e9f;
		boolean walkable = WALKABLE.contains(current);
		boolean moved = o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown() || o.keyJump.isDown();
		if (t > 0.25f && (o.keyAttack.isDown() || o.keyUse.isDown() || (walkable ? o.keyShift.isDown() : moved))) {
			stop(mc);
			return;
		}
		float limit = current.loops ? current.seconds * 3 : current.seconds;
		if (t > limit) stop(mc);
	}

	public static void animateRemote(PlayerModel m, java.util.UUID id) {
		long[] start = new long[1];
		Emote e = gg.surge.client.net.SurgeNet.remoteEmote(id, start);
		if (e == null) return;
		run(m, e, (System.currentTimeMillis() - start[0]) / 1000f, false);
	}

	public static void animateCaptured(PlayerModel m, Emote e, float t, boolean previewing) {
		if (e != null) run(m, e, t, previewing);
	}

	public static Object[] previewLook() {
		return preview == null ? null : new Object[]{preview, (System.nanoTime() - previewStart) / 1e9f};
	}

	public static Object[] localLook() {
		if (preview != null && gg.surge.client.cosmetics.Preview.clean) return new Object[]{preview, (System.nanoTime() - previewStart) / 1e9f, true};
		if (current != null) return new Object[]{current, (System.nanoTime() - startNanos) / 1e9f, false};
		return null;
	}

	public static void animate(PlayerModel m) {
		Emote e;
		float t;
		if (preview != null && gg.surge.client.cosmetics.Preview.clean) { e = preview; t = (System.nanoTime() - previewStart) / 1e9f; }
		else if (current != null) { e = current; t = (System.nanoTime() - startNanos) / 1e9f; }
		else return;
		run(m, e, t, preview != null);
	}

	private static void run(PlayerModel m, Emote e, float t, boolean previewing) {
		float in = ease(Math.min(1f, t / 0.2f));
		if (previewing && !e.loops) t = t % (e.seconds + 0.6f);
		float out = e.loops || previewing ? 1f : ease(Math.min(1f, Math.max(0f, (e.seconds - t) / 0.25f)));
		float k = in * out;
		Pose p = new Pose(m, k);
		p.upperOnly = walking && WALKABLE.contains(e);
		EmoteAnimations.pose(e, p, t);
		p.apply();
	}

	static float ease(float x) {
		return x * x * (3 - 2 * x);
	}

	public static final class Pose {
		final PlayerModel m;
		final float k;
		float[] head, body, rArm, lArm, rLeg, lLeg;
		float lean;
		float bob;
		float sway;
		float spin;
		boolean upperOnly;

		Pose(PlayerModel m, float k) {
			this.m = m;
			this.k = k;
		}

		public Pose head(float x, float y, float z) { head = new float[]{x, y, z}; return this; }

		public Pose body(float x, float y, float z) { body = new float[]{x, y, z}; return this; }

		public Pose rArm(float x, float y, float z) { rArm = new float[]{x, y, z}; return this; }

		public Pose lArm(float x, float y, float z) { lArm = new float[]{x, y, z}; return this; }

		public Pose rLeg(float x, float y, float z) { rLeg = new float[]{x, y, z}; return this; }

		public Pose lLeg(float x, float y, float z) { lLeg = new float[]{x, y, z}; return this; }

		public Pose lean(float deg) { lean = deg; return this; }

		public Pose bob(float px) { bob = px; return this; }

		public Pose sway(float px) { sway = px; return this; }

		public Pose spin(float deg) { spin = deg; return this; }

		private void set(ModelPart part, float[] deg) {
			if (deg == null) return;
			float rad = (float) Math.PI / 180f;
			part.xRot += (deg[0] * rad - part.xRot) * k;
			part.yRot += (deg[1] * rad - part.yRot) * k;
			part.zRot += (deg[2] * rad - part.zRot) * k;
		}

		void apply() {
			set(m.head, head);
			float oy = m.body.yRot, oz = m.body.zRot;
			set(m.body, body);
			float dy = m.body.yRot - oy, dz = m.body.zRot - oz;
			set(m.rightArm, rArm);
			set(m.leftArm, lArm);
			if (dy != 0 || dz != 0) {
				float cy = (float) Math.cos(dy), sy = (float) Math.sin(dy), cz = (float) Math.cos(dz), sz = (float) Math.sin(dz);
				for (ModelPart arm : new ModelPart[]{m.rightArm, m.leftArm}) {
					float x = arm.x, y = arm.y;
					float rx = x * cz - y * sz, ry = x * sz + y * cz;
					arm.x = rx * cy + arm.z * sy;
					arm.z = -rx * sy + arm.z * cy;
					arm.y = ry;
					arm.yRot += dy;
					arm.zRot += dz;
				}
			}
			if (upperOnly) return;
			set(m.rightLeg, rLeg);
			set(m.leftLeg, lLeg);
			float a = (float) Math.toRadians(lean) * k;
			if (a != 0) {
				float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
				m.body.xRot += a;
				m.body.y += 12 - 12 * cos;
				m.body.z -= 12 * sin;
				m.head.xRot += a;
				m.head.y += 12 - 12 * cos;
				m.head.z -= 12 * sin;
				m.rightArm.xRot += a;
				m.leftArm.xRot += a;
				m.rightArm.y += 10 - 10 * cos;
				m.leftArm.y += 10 - 10 * cos;
				m.rightArm.z -= 10 * sin;
				m.leftArm.z -= 10 * sin;
			}
			float b = bob * k, s = sway * k;
			if (b != 0 || s != 0) {
				for (ModelPart part : new ModelPart[]{m.head, m.body, m.rightArm, m.leftArm}) { part.y += b; part.x += s; }
				m.rightLeg.y += b;
				m.leftLeg.y += b;
			}
			float sp = (float) Math.toRadians(spin) * k;
			if (sp != 0) {
				float cs = (float) Math.cos(sp), sn = (float) Math.sin(sp);
				for (ModelPart part : new ModelPart[]{m.head, m.body, m.rightArm, m.leftArm, m.rightLeg, m.leftLeg}) {
					float x = part.x, z = part.z;
					part.x = x * cs + z * sn;
					part.z = -x * sn + z * cs;
					part.yRot += sp;
				}
			}
		}
	}
}
