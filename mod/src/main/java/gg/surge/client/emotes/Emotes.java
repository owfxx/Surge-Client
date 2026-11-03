package gg.surge.client.emotes;

import gg.surge.client.economy.Wallet;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Custom animated emotes. Each emote poses the player model's head, body, arms and
 * legs over time (see PlayerModelMixin), so they're real animations instead of
 * in-game actions. Emotes show on your own player.
 */
public final class Emotes {
	public enum Emote {
		WAVE("Wave", 3.0f, false), CLAP("Clap", 3.0f, false), FLUSTERED("Flustered", 4.0f, false),
		FACEPALM("Facepalm", 3.0f, false), DAB("Dab", 2.2f, false), SHRUG("Shrug", 2.4f, false),
		SALUTE("Salute", 3.0f, false), CHEER("Cheer", 3.5f, false), THINKING("Thinking", 4.0f, false),
		LAUGH("Laugh", 3.5f, false), BOW("Bow", 3.0f, false), HEART("Heart", 3.5f, false),
		BOXING("Boxing", 4.0f, true), FLOSS("Floss", 4.0f, true), GROOVE("Groove", 4.0f, true),
		ZOMBIE("Zombie", 4.0f, true), TPOSE("T-Pose", 3.0f, false), CRY("Cry", 4.0f, false),
		SIT("Sit", 6.0f, true), POINT("Point", 2.5f, false), PEACE("Peace", 2.5f, false), STRETCH("Stretch", 3.2f, false),
		TWIRL("Twirl", 1.6f, false), JUMP("Jump for Joy", 3.0f, false), MEDITATE("Meditate", 6.0f, true),
		HEADBANG("Headbang", 4.0f, true), YAWN("Yawn", 3.2f, false),
		GUITAR("Air Guitar", 4.0f, true), ROBOT("Robot", 4.0f, true), CHICKEN("Chicken Dance", 4.0f, true),
		WAVE_BOTH("Big Wave", 3.0f, false), VICTORY("Victory", 3.0f, false), SHY("Shy", 3.5f, false),
		KICK("Kick", 1.8f, false), SALSA("Salsa", 4.0f, true), DRUMS("Drums", 4.0f, true), SWIM("Swim", 4.0f, true);

		public final String label;
		public final float seconds;
		public final boolean loops;

		Emote(String label, float seconds, boolean loops) {
			this.label = label;
			this.seconds = seconds;
			this.loops = loops;
		}

		public static Emote byName(String name) {
			for (Emote e : values()) if (e.name().equals(name)) return e;
			return null;
		}
	}

	private static Emote current;
	private static long startNanos;
	private static CameraType previousCamera;
	/** Shown on your model while hovering in the wheel or wardrobe (doesn't need to be owned). */
	private static Emote preview;
	private static long previewStart;
	private static int particleTick;

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
		mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
		gg.surge.client.net.SurgeNet.playedEmote(emote.name());
	}

	public static void stop(Minecraft mc) {
		if (current == null) return;
		if (previousCamera != null) mc.options.setCameraType(previousCamera);
		current = null;
		gg.surge.client.net.SurgeNet.playedEmote("none");
	}

	public static void tick(Minecraft mc) {
		LocalPlayer p = mc.player;
		if (current == null) return;
		if (p == null) { current = null; return; }
		var o = mc.options;
		float t = (System.nanoTime() - startNanos) / 1e9f;
		// Moving, jumping or attacking ends the emote, like other clients.
		if (t > 0.25f && (o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown() || o.keyJump.isDown() || o.keyAttack.isDown())) {
			stop(mc);
			return;
		}
		if (++particleTick % 8 == 0 && mc.level != null && (current == Emote.FLUSTERED || current == Emote.HEART)) {
			mc.level.addParticle(ParticleTypes.HEART, p.getX() + (Math.random() - 0.5) * 0.6, p.getY() + 2.2, p.getZ() + (Math.random() - 0.5) * 0.6, 0, 0.05, 0);
		}
		if (particleTick % 5 == 0 && mc.level != null && current == Emote.CRY) {
			mc.level.addParticle(ParticleTypes.FALLING_WATER, p.getX() + (Math.random() - 0.5) * 0.3, p.getY() + 1.6, p.getZ() + (Math.random() - 0.5) * 0.3, 0, 0, 0);
		}
		if (!current.loops && t > current.seconds) stop(mc);
		else if (current.loops && t > current.seconds * 3) stop(mc);
	}

	// ------------------------------------------------------------------ animation
	/** Another Surge player's emote (from the online service). */
	public static void animateRemote(PlayerModel m, java.util.UUID id) {
		long[] start = new long[1];
		Emote e = gg.surge.client.net.SurgeNet.remoteEmote(id, start);
		if (e == null) return;
		run(m, e, (System.currentTimeMillis() - start[0]) / 1000f, false);
	}

	/** Called from PlayerModelMixin after vanilla posed your model. */
	public static void animate(PlayerModel m) {
		Emote e;
		float t;
		if (preview != null) { e = preview; t = (System.nanoTime() - previewStart) / 1e9f; }
		else if (current != null) { e = current; t = (System.nanoTime() - startNanos) / 1e9f; }
		else return;
		run(m, e, t, preview != null);
	}

	private static void run(PlayerModel m, Emote e, float t, boolean previewing) {
		// ease in over the first 0.2s, and out over the last 0.25s of one-shot emotes
		float in = ease(Math.min(1f, t / 0.2f));
		if (previewing && !e.loops) t = t % (e.seconds + 0.6f);
		float out = e.loops || previewing ? 1f : ease(Math.min(1f, Math.max(0f, (e.seconds - t) / 0.25f)));
		float k = in * out;
		Pose p = new Pose(m, k);
		EmoteAnimations.pose(e, p, t);
		p.apply();
	}

	static float ease(float x) {
		return x * x * (3 - 2 * x);
	}

	/**
	 * Target angles in degrees for each part, blended with the vanilla pose by k.
	 * Model space: y points down, the player faces -z, the right arm is at -x.
	 */
	public static final class Pose {
		final PlayerModel m;
		final float k;
		float[] head, body, rArm, lArm, rLeg, lLeg;
		float lean; // whole-body lean forward (+) or back (-), pivoting at the hips
		float bob;  // move everything up (-) or down (+), in model pixels
		float sway; // move the upper body sideways, in model pixels
		float spin; // turn the whole body around (degrees)

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
			set(m.body, body);
			set(m.rightArm, rArm);
			set(m.leftArm, lArm);
			set(m.rightLeg, rLeg);
			set(m.leftLeg, lLeg);
			float a = (float) Math.toRadians(lean) * k;
			if (a != 0) {
				// rotate the upper body around the hips (y = 12)
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
