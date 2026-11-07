package gg.surge.client.emotes;

import gg.surge.client.emotes.Emotes.Emote;
import gg.surge.client.emotes.Emotes.Pose;

final class EmoteAnimations {
	private EmoteAnimations() {}

	private static float sin(float a) { return (float) Math.sin(a); }

	private static float cos(float a) { return (float) Math.cos(a); }

	private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

	static void pose(Emote e, Pose p, float t) {
		switch (e) {
			case WAVE -> {
				float w = sin(t * 9f);
				float hi = Emotes.ease(clamp(t / 0.3f));
				p.rArm(-15 + w * 5, 0, (150 + w * 24) * hi).lArm(4, 0, -6).head(-8, -14, 6 + w * 3).body(0, -6, 0).bob(-Math.abs(sin(t * 4.5f)) * 0.5f);
			}
			case CLAP -> {
				float c = Math.abs(sin(t * 7f));
				p.rArm(-75, -(8 + 22 * c), 0).lArm(-75, 8 + 22 * c, 0).head(4 - 6 * c, 0, 0);
			}
			case FLUSTERED -> {
				float shake = t < 1.2f ? sin(t * 14f) * (1.2f - t) * 14f : 0f;
				float s = sin(t * 2.2f);
				float tap = Math.max(0f, sin(t * 6f));
				p.rArm(-145 + s * 4, -28, 8).lArm(-145 - s * 4, 28, -8)
					.head(20 + s * 3, shake + s * 10, s * 9).body(0, s * 10, 0).sway(s * 0.4f)
					.rLeg(0, 0, -4).lLeg(28 * tap, 0, 6).bob(-tap * 0.3f);
			}
			case FACEPALM -> {
				float up = Emotes.ease(clamp(t / 0.45f)), down = Emotes.ease(clamp((t - e.seconds + 0.6f) / 0.5f));
				float a = up * (1 - down);
				float shake = t > 0.6f ? sin(t * 6.5f) * 9 * a : 0;
				p.rArm(-128 * a, -38 * a, 0).lArm(-20 * a, 25 * a, -6).head(30 * a, shake, 0).lean(7 * a).bob(0.4f * a);
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
				float tap = sin(t * 8f) * 3;
				float look = sin(t * 0.8f);
				p.rArm(-112 + tap, -34, 0).lArm(-42, 42, 0).head(-14 + look * 6, 14 + sin(t * 0.6f) * 10, 8).sway(sin(t * 1.2f) * 0.4f).body(0, look * 4, 0);
			}
			case LAUGH -> {
				float shake = sin(t * 20f);
				float cyc = t % 1.4f;
				float slap = Emotes.ease(clamp((cyc - 0.85f) / 0.15f)) * (1 - Emotes.ease(clamp((cyc - 1.15f) / 0.2f)));
				p.lean(-14 * (1 - slap) + 20 * slap + shake * 2).rArm(-30 - 25 * slap, -30 * (1 - slap), 10 * slap).lArm(-35, 32, 0)
					.head(-24 * (1 - slap) + 12 * slap + shake * 5, 0, 0).bob(Math.abs(shake) * 0.5f).rLeg(-8 * slap, 0, 0);
			}
			case BOW -> {
				float ph = (t % 3f) / 3f;
				float down = Emotes.ease(clamp(ph / 0.35f)) * (1 - Emotes.ease(clamp((ph - 0.65f) / 0.35f)));
				p.lean(40 * down).rArm(-70 * down, -45 * down, 0).lArm(25 * down, 0, -5).head(10 * down, 0, 0);
			}
			case HEART -> {
				float beat = Math.max(0, sin(t * 7f));
				float s = sin(t * 2f);
				p.rArm(-168, 0, 24 + beat * 5).lArm(-168, 0, -24 - beat * 5).head(-16, 0, s * 6).sway(s * 0.6f).body(0, s * 6, 0).bob(-beat * 0.4f);
			}
			case FLOSS -> {
				float f = sin(t * 7f);
				float swing = sin(t * 14f);
				p.rArm(18 * swing, 0, 30 * f).lArm(-18 * swing, 0, 30 * f).body(0, 0, -5 * f).sway(-1.5f * f).head(0, 0, 6 * f);
			}
			case TPOSE -> p.rArm(0, 0, 90).lArm(0, 0, -90).head(0, 0, 0).body(0, 0, 0);
			case SIT -> {
				float br = sin(t * 2f);
				p.bob(10f).rLeg(-88, 8, 0).lLeg(-88, -8, 0).rArm(-20, 0, 12).lArm(-20, 0, -12).head(4 + br * 2, sin(t * 0.7f) * 12, 0);
			}
			case YAWN -> {
				float a = Emotes.ease(clamp(t / 0.8f)) * (1 - Emotes.ease(clamp((t - 2.3f) / 0.8f)));
				float tremble = a > 0.95f ? sin(t * 24f) * 2 : 0;
				p.head(-32 * a, 0, 6 * a * sin(t * 1.5f)).rArm(0, 0, 170 * a + tremble + 6).lArm(-70 * a, 35 * a, -6).lean(-8 * a).bob(-1.2f * a);
			}
			case GRIDDY -> {
				float b = t * e.bpm / 60f;
				float side = sin(b * (float) Math.PI);
				float hop = Math.abs(sin(b * (float) Math.PI));
				boolean goggles = ((int) (b / 8)) % 2 == 1;
				if (goggles) p.rArm(-150, -35, 0).lArm(-150, 35, 0).head(-6, side * 8, 0);
				else p.rArm(-60 - 40 * side, -10, 12).lArm(-60 + 40 * side, 10, -12).head(6 * hop, side * 6, 0);
				p.rLeg(-35 * Math.max(0, side), 0, 4).lLeg(-35 * Math.max(0, -side), 0, -4).lean(10).bob(-hop * 1.5f).sway(side * 0.6f);
			}
			case MOONWALK -> {
				float b = t * e.bpm / 60f;
				float s = sin(b * (float) Math.PI * 0.5f);
				float r = Math.max(0, s), l = Math.max(0, -s);
				p.rLeg(18 * l - 6, 0, 0).lLeg(18 * r - 6, 0, 0).body(0, 0, 0).lean(-6)
					.rArm(14 * s, 0, 10).lArm(-14 * s, 0, -10).head(-4, 0, sin(b * (float) Math.PI) * 4).bob(-Math.abs(s) * 0.8f);
				if (((int) (b / 4)) % 4 == 3) p.rArm(-150, -20, 20).head(-20, 0, 0);
			}
			case SHUFFLE -> {
				float b = t * e.bpm / 60f;
				float s = sin(b * (float) Math.PI * 2f);
				float kick = sin(b * (float) Math.PI);
				p.rLeg(-20 * Math.max(0, kick), 0, 25 * Math.max(0, s)).lLeg(-20 * Math.max(0, -kick), 0, -25 * Math.max(0, -s))
					.rArm(-30 - 30 * kick, 0, 25).lArm(-30 + 30 * kick, 0, -25).bob(-Math.abs(s) * 1.6f).head(8 * Math.abs(s), 0, 0).sway(kick * 0.5f);
			}
			case DISCO -> {
				float b = t * e.bpm / 60f;
				boolean upBeat = ((int) (b / 2)) % 2 == 0;
				float hip = sin(b * (float) Math.PI);
				if (upBeat) p.rArm(-170, -10, 35).head(-20, -15, 0);
				else p.rArm(-40, 30, -40).head(10, 15, 0);
				p.lArm(-20, 0, -15 - 10 * Math.abs(hip)).body(0, 10 * hip, 0).sway(hip * 1.2f).rLeg(0, 0, 6 + 4 * hip).lLeg(-10 * Math.max(0, hip), 0, -6);
			}
			case CRY -> {
				float sob = sin(t * 10f);
				float rub = sin(t * 3.2f);
				float in = Emotes.ease(clamp(t / 0.35f));
				p.rArm((-126 + rub * 5) * in, (-36 + rub * 4) * in, 0).lArm((-126 - rub * 5) * in, (36 - rub * 4) * in, 0)
					.head(14 + sob * 3, rub * 4, 0).lean(5 + Math.abs(sob) * 2).bob(Math.abs(sob) * 0.4f);
			}
		}
	}
}
