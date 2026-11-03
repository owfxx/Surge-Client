package gg.surge.client.emotes;

import gg.surge.client.emotes.Emotes.Emote;
import gg.surge.client.emotes.Emotes.Pose;

/**
 * The keyframes of every emote, written as smooth functions of time (seconds).
 * Angles are degrees. Arms: x -90 points forward, -180 straight up; z +90 lifts the
 * right arm out to the side (-90 for the left). Head: x + looks down.
 */
final class EmoteAnimations {
	private EmoteAnimations() {}

	private static float sin(float a) { return (float) Math.sin(a); }

	private static float cos(float a) { return (float) Math.cos(a); }

	private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

	static void pose(Emote e, Pose p, float t) {
		switch (e) {
			case WAVE -> {
				float w = sin(t * 10f);
				p.rArm(0, 0, 150 + w * 22).lArm(0, 0, -6).head(-6, -10, 4 + w * 2).body(0, -4, 0);
			}
			case CLAP -> {
				float c = Math.abs(sin(t * 7f));
				p.rArm(-75, -(8 + 22 * c), 0).lArm(-75, 8 + 22 * c, 0).head(4 - 6 * c, 0, 0);
			}
			case FLUSTERED -> {
				float s = sin(t * 2.2f);
				float tap = Math.abs(sin(t * 3f));
				p.rArm(-145, -28, 8).lArm(-145, 28, -8)
					.head(18, s * 12, s * 8).body(0, s * 8, 0)
					.rLeg(0, 0, -5).lLeg(-6 - tap * 10, 0, 5);
			}
			case FACEPALM -> {
				float shake = sin(t * 6f);
				p.rArm(-125, -38, 0).lArm(0, 0, -5).head(28, shake * 8, 0).lean(6);
			}
			case DAB -> {
				float d = Emotes.ease(clamp(t * 4f));
				p.lArm(0, 0, -125 * d).rArm(-105 * d, -55 * d, 0).head(35 * d, -25 * d, 0).lean(8 * d);
			}
			case SHRUG -> {
				float s = Emotes.ease(0.5f - 0.5f * cos(t * (float) (Math.PI * 2 / 1.2)));
				p.rArm(-25 * s, 0, 32 * s).lArm(-25 * s, 0, -32 * s).head(-6 * s, 0, 14 * s);
			}
			case SALUTE -> p.rArm(-118, -50, 14).lArm(0, 0, -4).head(-6, 0, 0);
			case CHEER -> {
				float j = Math.abs(sin(t * 5f));
				float pump = sin(t * 10f) * 14;
				p.rArm(0, 0, 160 + pump).lArm(0, 0, -160 - pump).head(-15, 0, 0)
					.bob(-j * 2.5f).rLeg(0, 0, 4 * j).lLeg(0, 0, -4 * j);
			}
			case THINKING -> {
				float tap = sin(t * 5f) * 3;
				p.rArm(-110, -32, 0).lArm(-40, 40, 0).head(-12 + tap, 12, 8);
			}
			case LAUGH -> {
				float shake = sin(t * 18f);
				p.lean(-12 + shake * 2).rArm(-35, -30, 0).lArm(-35, 30, 0).head(-20 + shake * 5, 0, 0);
			}
			case BOW -> {
				float ph = (t % 3f) / 3f;
				float down = Emotes.ease(clamp(ph / 0.35f)) * (1 - Emotes.ease(clamp((ph - 0.65f) / 0.35f)));
				p.lean(40 * down).rArm(-70 * down, -45 * down, 0).lArm(25 * down, 0, -5).head(10 * down, 0, 0);
			}
			case HEART -> {
				float s = sin(t * 2f);
				p.rArm(-170, 0, 22).lArm(-170, 0, -22).head(-18, 0, s * 5).sway(s * 0.6f);
			}
			case BOXING -> {
				float ph = (t * 2.4f) % 2f;
				float right = ph < 1 ? sin(ph * (float) Math.PI) : 0;
				float left = ph >= 1 ? sin((ph - 1) * (float) Math.PI) : 0;
				p.rArm(-80 - 14 * right, -18 + 18 * right, 0).lArm(-80 - 14 * left, 18 - 18 * left, 0)
					.body(0, -15 * right + 15 * left, 0).head(5, 0, 0)
					.rLeg(14, 0, 0).lLeg(-14, 0, 0).bob(Math.abs(sin(t * 6f)) * 0.6f);
			}
			case FLOSS -> {
				float f = sin(t * 7f);
				float swing = sin(t * 14f);
				p.rArm(18 * swing, 0, 30 * f).lArm(-18 * swing, 0, 30 * f).body(0, 0, -5 * f).sway(-1.5f * f).head(0, 0, 6 * f);
			}
			case GROOVE -> {
				float b = sin(t * 8f);
				float side = sin(t * 4f);
				p.bob(Math.abs(b) * 1.2f).head(8 * Math.abs(b), 10 * side, 0)
					.rArm(-30 - 20 * b, 0, 20).lArm(-30 + 20 * b, 0, -20).body(0, 8 * side, 0).sway(side * 0.8f);
			}
			case ZOMBIE -> {
				float w = sin(t * 4f);
				p.rArm(-90 + w * 5, 0, 0).lArm(-90 - w * 5, 0, 0).head(15, 0, 10).rLeg(20 * w, 0, 0).lLeg(-20 * w, 0, 0);
			}
			case TPOSE -> p.rArm(0, 0, 90).lArm(0, 0, -90).head(0, 0, 0).body(0, 0, 0);
			case SIT -> {
				float br = sin(t * 2f);
				p.bob(10f).rLeg(-88, 8, 0).lLeg(-88, -8, 0).rArm(-20, 0, 12).lArm(-20, 0, -12).head(4 + br * 2, sin(t * 0.7f) * 12, 0);
			}
			case POINT -> {
				float wig = sin(t * 9f) * 3;
				p.rArm(-90 + wig, -12, 0).lArm(0, 0, -6).head(-4, -10, 0).body(0, -8, 0);
			}
			case PEACE -> {
				float bounce = Math.abs(sin(t * 6f));
				p.rArm(-150, -24, 26 + bounce * 6).lArm(0, 0, -6).head(-6, 0, 14).bob(-bounce * 0.8f);
			}
			case STRETCH -> {
				float up = Emotes.ease(clamp(t / 0.9f)) * (1 - Emotes.ease(clamp((t - 2.2f) / 0.8f)));
				p.rArm(0, 0, 180 * up + 6).lArm(0, 0, -180 * up - 6).head(-25 * up, 0, sin(t * 3f) * 6 * up).lean(-8 * up).bob(-1.5f * up);
			}
			case TWIRL -> {
				float s2 = Emotes.ease(clamp(t / 1.4f));
				p.spin(360 * s2).rArm(0, 0, 70).lArm(0, 0, -70).head(-10, 0, 0).bob(-Math.abs(sin(s2 * (float) Math.PI)) * 2f);
			}
			case JUMP -> {
				float j = Math.abs(sin(t * 4.5f));
				p.bob(-j * 6f).rArm(0, 0, 150 + j * 20).lArm(0, 0, -150 - j * 20).rLeg(-25 * j, 0, 8 * j).lLeg(-25 * j, 0, -8 * j).head(-15, 0, 0);
			}
			case MEDITATE -> {
				float fl = sin(t * 1.6f);
				p.bob(9f + fl * 1.5f).rLeg(-85, 35, 0).lLeg(-85, -35, 0).rArm(-35, 20, 18).lArm(-35, -20, -18).head(8, 0, 0);
			}
			case HEADBANG -> {
				float h = sin(t * 11f);
				p.head(25 + h * 25, 0, 0).lean(8 + h * 6).rArm(-60, -15, 0).lArm(-60, 15, 0).bob(Math.abs(h) * 0.6f);
			}
			case YAWN -> {
				float a = Emotes.ease(clamp(t / 0.8f)) * (1 - Emotes.ease(clamp((t - 2.2f) / 0.9f)));
				p.head(-30 * a, 0, 0).rArm(-150 * a, 0, 30 * a).lArm(-60 * a, 30 * a, 0).lean(-6 * a);
			}
			case CRY -> {
				float sob = sin(t * 14f);
				p.rArm(-135, -30, 0).lArm(-135, 30, 0).head(25 + sob * 3, 0, 0).lean(10);
			}
		}
	}
}
