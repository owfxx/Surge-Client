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
				float a = Emotes.ease(clamp(t / 0.8f)) * (1 - Emotes.ease(clamp((t - 2.3f) / 0.8f)));
				float tremble = a > 0.95f ? sin(t * 24f) * 2 : 0;
				p.head(-32 * a, 0, 6 * a * sin(t * 1.5f)).rArm(0, 0, 170 * a + tremble + 6).lArm(-70 * a, 35 * a, -6).lean(-8 * a).bob(-1.2f * a);
			}
			case GUITAR -> {
				float strum = sin(t * 14f);
				float nod = Math.abs(sin(t * 5f));
				p.lArm(-70, 35, -10).rArm(-35 + strum * 18, -25, 0).head(14 * nod, -8, 0).lean(6 + 4 * nod).bob(nod * 0.8f)
					.rLeg(10 * nod, 0, 3).lLeg(-6, 0, -3);
			}
			case ROBOT -> {
				int step = (int) (t * 3f) % 4;
				float ra = step == 0 ? -90 : step == 1 ? 0 : step == 2 ? -90 : -45;
				float la = step == 0 ? 0 : step == 1 ? -90 : step == 2 ? -45 : -90;
				float hy = step % 2 == 0 ? 25 : -25;
				p.rArm(ra, 0, 0).lArm(la, 0, 0).head(0, hy, 0).body(0, hy * 0.3f, 0);
			}
			case CHICKEN -> {
				float flap = Math.abs(sin(t * 12f));
				float peck = sin(t * 6f);
				p.rArm(-20, 0, 25 + flap * 30).lArm(-20, 0, -25 - flap * 30).head(10 + peck * 12, 0, 0)
					.lean(12).bob(Math.abs(peck) * 1.2f).rLeg(-15, 0, 10).lLeg(-15, 0, -10);
			}
			case WAVE_BOTH -> {
				float w = sin(t * 9f);
				p.rArm(0, 0, 155 + w * 20).lArm(0, 0, -155 + w * 20).head(-12, 0, w * 5).sway(w * 0.5f);
			}
			case VICTORY -> {
				float up = Emotes.ease(clamp(t / 0.35f));
				float pump = Math.abs(sin(t * 8f)) * up;
				p.rArm(0, 0, 165 * up + pump * 10).lArm(-20, 20, -10).head(-22 * up, 0, 0).bob(-pump * 2f).lean(-6 * up);
			}
			case SHY -> {
				float s = sin(t * 2.5f);
				p.rArm(-40, -45, 0).lArm(-40, 45, 0).head(30, 20 + s * 6, 10).body(0, 10, 0)
					.rLeg(0, 0, 0).lLeg(-12 - Math.abs(s) * 8, 0, 6);
			}
			case KICK -> {
				float wind = Emotes.ease(clamp(t / 0.45f));
				float kick = Emotes.ease(clamp((t - 0.45f) / 0.2f));
				float back = Emotes.ease(clamp((t - 1.1f) / 0.6f));
				float leg = (25 * wind - 115 * kick) * (1 - back);
				p.rLeg(leg, 0, 0).lLeg(0, 0, -4).rArm(30 * kick * (1 - back), 0, 20).lArm(-60 * kick * (1 - back), 0, -30).lean(-10 * kick * (1 - back)).head(-5, 0, 0);
			}
			case SALSA -> {
				float b = sin(t * 7f);
				float hip = sin(t * 3.5f);
				p.rArm(-80, -20, 30 + 10 * b).lArm(-50, 30, -40).body(0, 18 * hip, -4 * hip).sway(hip * 1.2f)
					.rLeg(-14 * Math.max(0, b), 0, 4).lLeg(-14 * Math.max(0, -b), 0, -4).head(-4, -10 * hip, 4 * hip);
			}
			case DRUMS -> {
				float r = sin(t * 16f), l = sin(t * 16f + (float) Math.PI);
				float nod = Math.abs(sin(t * 8f));
				p.rArm(-55 - r * 20, -15, 0).lArm(-55 - l * 20, 15, 0).head(10 * nod, 0, 0).lean(10).bob(nod * 0.6f);
			}
			case SWIM -> {
				float s = sin(t * 5f), c = cos(t * 5f);
				p.rArm(-180 * Math.max(0, s), 0, 30 + 40 * c).lArm(-180 * Math.max(0, -s), 0, -30 - 40 * c)
					.head(-15, 20 * s, 0).lean(20).rLeg(18 * s, 0, 0).lLeg(-18 * s, 0, 0).body(0, 10 * s, 0);
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
			case RUNNING_MAN -> {
				float b = t * e.bpm / 60f;
				float s = sin(b * (float) Math.PI);
				float up = Math.max(0, s), dn = Math.max(0, -s);
				p.rLeg(-70 * up + 25 * dn, 0, 0).lLeg(-70 * dn + 25 * up, 0, 0)
					.rArm(-80 + 40 * s, -10, 10).lArm(-80 - 40 * s, 10, -10).bob(-Math.abs(s) * 2f).head(10 * Math.abs(s), 0, 0).lean(6);
			}
			case DISCO -> {
				float b = t * e.bpm / 60f;
				boolean upBeat = ((int) (b / 2)) % 2 == 0;
				float hip = sin(b * (float) Math.PI);
				if (upBeat) p.rArm(-170, -10, 35).head(-20, -15, 0);
				else p.rArm(-40, 30, -40).head(10, 15, 0);
				p.lArm(-20, 0, -15 - 10 * Math.abs(hip)).body(0, 10 * hip, 0).sway(hip * 1.2f).rLeg(0, 0, 6 + 4 * hip).lLeg(-10 * Math.max(0, hip), 0, -6);
			}
			case TWIST -> {
				float b = t * e.bpm / 60f;
				float s = sin(b * (float) Math.PI);
				p.body(0, 35 * s, 0).rArm(-70, -20 + 25 * s, 30).lArm(-70, 20 + 25 * s, -30)
					.rLeg(-8, -30 * s, 6).lLeg(-8, -30 * s, -6).head(0, -15 * s, 0).bob(1.5f + Math.abs(s) * 1.5f);
			}
			case CHARLESTON -> {
				float b = t * e.bpm / 60f;
				float s = sin(b * (float) Math.PI * 0.5f);
				float jazz = sin(b * (float) Math.PI * 4f) * 10;
				p.rLeg(-40 * s, 0, 10 * Math.abs(s)).lLeg(40 * s * 0.4f, 0, -10 * Math.abs(s))
					.rArm(-100 + 30 * s, -30, 20 + jazz).lArm(-100 - 30 * s, 30, -20 - jazz).lean(8 * s).head(-8, 0, 6 * s).bob(-Math.abs(s) * 1.2f);
			}
			case PONY -> {
				float b = t * e.bpm / 60f;
				float s = sin(b * (float) Math.PI);
				float bounce = Math.abs(sin(b * (float) Math.PI * 2f));
				boolean lasso = ((int) (b / 8)) % 2 == 1;
				if (lasso) p.rArm(-175, 0, 20 + sin(b * (float) Math.PI * 2f) * 25).lArm(-70, 35, 0);
				else p.rArm(-70, -35, 0).lArm(-70, 35, 0);
				p.rLeg(-20 * Math.max(0, s), 0, 8).lLeg(-20 * Math.max(0, -s), 0, -8).bob(-bounce * 2.5f).head(6 * bounce, 0, 0).sway(s * 0.8f);
			}
			case TOPROCK -> {
				float b = t * e.bpm / 60f;
				float s = sin(b * (float) Math.PI * 0.5f);
				float open = Math.abs(sin(b * (float) Math.PI));
				p.rLeg(-15 * Math.max(0, s), 0, -18 * Math.max(0, s)).lLeg(-15 * Math.max(0, -s), 0, 18 * Math.max(0, -s))
					.rArm(-40 - 30 * open, -30 + 50 * open, 20).lArm(-40 - 30 * open, 30 - 50 * open, -20).lean(12).body(0, 15 * s, 0).head(5, 10 * s, 0).bob(-open * 1.2f);
			}
			case WOAH -> {
				float b = t * e.bpm / 60f;
				int bar = (int) (b / 2);
				float hit = 1f - clamp((b % 2f) * 3f);
				boolean right = bar % 2 == 0;
				if (right) p.rArm(-95, 40, 0).lArm(-30 - 30 * hit, 0, -20).lean(-14 * hit + 4).head(-10 * hit, -20, 0).body(0, -15, 0);
				else p.lArm(-95, -40, 0).rArm(-30 - 30 * hit, 0, 20).lean(-14 * hit + 4).head(-10 * hit, 20, 0).body(0, 15, 0);
				p.rLeg(0, 0, 6).lLeg(0, 0, -6).bob(hit * 2f);
			}
			case CRY -> {
				float sob = sin(t * 9f);
				float rub = sin(t * 3f);
				float lift = Emotes.ease(clamp(((t % 2.6f) - 1.9f) / 0.2f)) * (1 - Emotes.ease(clamp(((t % 2.6f) - 2.3f) / 0.25f)));
				p.rArm(-140 + rub * 9 + lift * 30, -30 + rub * 6, 4).lArm(-140 - rub * 9 + lift * 30, 30 - rub * 6, -4)
					.head(28 + sob * 4 - lift * 30, rub * 5, 0).lean(14 + sob * 2 - lift * 8).bob(Math.abs(sob) * 0.6f);
			}
		}
	}
}
