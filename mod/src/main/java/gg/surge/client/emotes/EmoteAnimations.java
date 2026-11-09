package gg.surge.client.emotes;

import gg.surge.client.emotes.Emotes.Emote;
import gg.surge.client.emotes.Emotes.Pose;

final class EmoteAnimations {
	private EmoteAnimations() {}

	private static final float PI = (float) Math.PI;

	private static float sin(float a) { return (float) Math.sin(a); }

	private static float cos(float a) { return (float) Math.cos(a); }

	private static float abs(float a) { return Math.abs(a); }

	private static float max(float a, float b) { return Math.max(a, b); }

	private static float min(float a, float b) { return Math.min(a, b); }

	private static float pow(float a, float b) { return (float) Math.pow(a, b); }

	private static float floor(float a) { return (float) Math.floor(a); }

	private static float fract(float a) { return a - (float) Math.floor(a); }

	private static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

	private static float ease(float x) { return x * x * (3 - 2 * x); }

	private static float back(float x) { float c = 1.70158f, d = x - 1; return 1 + (c + 1) * d * d * d + c * d * d; }

	private static float mix(float a, float b, float k) { return a + (b - a) * k; }

	private static float sel(float i, float... v) { return v[Math.max(0, Math.min(v.length - 1, (int) i))]; }

	static void pose(Emote e, Pose p, float t) {
		switch (e) {
			case WAVE -> {
				float up = back(clamp(t / 0.42f));
				float w = t > 0.3f ? sin((t - 0.3f) * 10.5f) : 0f;
				float fade = 1f - 0.35f * clamp((t - 2.2f) / 0.6f);
				float br = sin(t * 2.4f);
				p.rArm(-12f - 6f * w * fade, -8f + 12f * w * fade, (148f + 20f * w * fade) * up);
				p.lArm(3f + br * 2f, 0f, -5f - br * 2f);
				p.head(-8f * up, -12f * up + 4f * w * fade, 9f * up);
				p.body(0f, -9f * up, 0f);
				p.sway(-0.35f * up);
				p.bob(-abs(sin(t * 5.2f)) * 0.35f * up);
				p.lLeg(0f, 0f, -2f * up);
				p.rLeg(0f, 0f, 3f * up);
			}
			case CLAP -> {
				float f = max(0f, t - 0.25f) * (5.5f + 1.5f * clamp(t / 2f));
				float v = abs(sin(f * PI));
				float open = t < 0.25f ? ease(t / 0.25f) : pow(v, 0.55f);
				float hit = 1f - open;
				p.rArm(-66f - 8f * hit, -(3f + 21f * open), 4f * open);
				p.lArm(-66f - 8f * hit, 3f + 21f * open, -4f * open);
				p.head(-4f + 7f * hit, sin(t * 2.1f) * 7f, sin(t * 1.3f) * 3f);
				p.lean(3f * hit);
				p.bob(-0.45f * hit + 0.2f * abs(sin(t * 2.8f)));
				p.body(0f, sin(t * 2.1f) * 4f, 0f);
			}
			case FACEPALM -> {
				float up = back(clamp((t - 0.08f) / 0.45f));
				float down = ease(clamp((t - 2.45f) / 0.45f));
				float a = up * (1f - down);
				float sigh = sin(clamp((t - 0.55f) / 1.4f) * PI);
				float shake = t > 0.75f ? sin((t - 0.75f) * 5.6f) * 11f * (1f - clamp((t - 1.9f) / 0.5f)) : 0f;
				p.rArm(-134f * a, -32f * a, 6f * a);
				p.lArm(10f * a, 16f * a, -16f * a);
				p.head(26f * a + 7f * sigh * a, shake * a, -5f * a);
				p.lean(6f * a + 6f * sigh * a);
				p.bob((0.3f + 0.5f * sigh) * a);
				p.sway(shake * 0.02f * a);
			}
			case DAB -> {
				float pre = ease(clamp(t / 0.14f)) * (1f - ease(clamp((t - 0.14f) / 0.1f)));
				float d = back(clamp((t - 0.14f) / 0.2f));
				float jit = t > 0.4f ? sin(t * 31f) * 1.2f * d * (1f - clamp((t - 0.9f) / 0.4f)) : 0f;
				p.lArm(-14f * d + 20f * pre, 6f * d, -128f * d + jit - 10f * pre);
				p.rArm(-102f * d + 15f * pre, -64f * d, 8f * d + 12f * pre);
				p.head(34f * d, 30f * d, -8f * d);
				p.body(0f, 14f * d, 0f);
				p.lean(10f * d);
				p.bob(1.1f * pre - 0.2f * d);
				p.sway(-0.45f * d);
				p.rLeg(-4f * d, 0f, 4f * d);
				p.lLeg(0f, 0f, -7f * d);
			}
			case SHRUG -> {
				float s = back(clamp((t - 0.05f) / 0.3f)) * (1f - ease(clamp((t - 1.55f) / 0.45f)));
				float s2 = t > 0.45f && t < 1.4f ? sin((t - 0.45f) * 9f) * 0.12f * (1f - clamp((t - 0.45f) / 0.9f)) : 0f;
				float k = s + s2;
				p.rArm(-38f * k, 14f * k, 34f * k);
				p.lArm(-38f * k, -14f * k, -34f * k);
				p.head(-4f * k, -8f * k, 16f * k);
				p.bob(-0.9f * k);
				p.lean(-3f * k);
				p.body(0f, 0f, 3f * k);
			}
			case SALUTE -> {
				float u = back(clamp((t - 0.08f) / 0.32f));
				float hold = t > 0.45f ? 1f : 0f;
				float br = sin(t * 2.2f);
				p.rArm((-122f + 2f * br * hold) * u, -52f * u, 18f * u);
				p.lArm(2f, 0f, -3f);
				p.head(-10f * u, 3f * u, 0f);
				p.lean(-4f * u);
				p.bob(-0.25f * u + 0.08f * br);
				p.rLeg(0f, 0f, 1f);
				p.lLeg(0f, 0f, -1f);
			}
			case CHEER -> {
				float pre = t < 0.25f ? sin(t / 0.25f * PI) : 0f;
				float ph = fract(max(0f, t - 0.25f) * 1.6f);
				float air = t > 0.25f ? max(0f, sin(ph * PI * 1.25f - 0.15f * PI)) : 0f;
				float crouch = t > 0.25f ? max(0f, -sin(ph * PI * 1.25f - 0.15f * PI)) : pre;
				float pump = sin(ph * PI * 2f) * 16f;
				float up = back(clamp(t / 0.35f));
				p.rArm(-8f * air, -6f, (150f + pump) * up);
				p.lArm(-8f * air, 6f, (-150f - pump) * up);
				p.head(-18f * up + 6f * crouch, sin(t * 3f) * 6f, 0f);
				p.bob(-3.2f * air + 1.2f * crouch);
				p.rLeg(-14f * air, 0f, 7f * air);
				p.lLeg(-6f * air, 0f, -7f * air);
				p.lean(-4f * air + 5f * crouch);
			}
			case LAUGH -> {
				float cyc = fract(t / 1.75f);
				float bk = ease(clamp(cyc / 0.18f)) * (1f - ease(clamp((cyc - 0.5f) / 0.15f)));
				float over = ease(clamp((cyc - 0.5f) / 0.15f)) * (1f - ease(clamp((cyc - 0.88f) / 0.12f)));
				float slap = over * pow(abs(sin((cyc - 0.5f) * PI * 5.5f)), 2f);
				float sh = sin(t * 23f) * (0.7f + 0.3f * bk);
				p.lean(-15f * bk + 24f * over + sh * 2f);
				p.rArm(-28f * bk - 50f * over + 12f * slap, -26f * bk + 6f * over, 8f * bk + 12f * slap);
				p.lArm(-40f * bk - 20f * over, 34f * bk + 10f * over, -4f * bk);
				p.head(-26f * bk + 14f * over + sh * 6f, sin(t * 3f) * 6f, sin(t * 2f) * 4f);
				p.bob(abs(sh) * 0.5f + 0.6f * over);
				p.rLeg(-6f * slap, 0f, 2f);
				p.lLeg(4f * over, 0f, -2f);
			}
			case BOW -> {
				float ph = fract(t / 3f);
				float down = ease(clamp((ph - 0.05f) / 0.3f)) * (1f - ease(clamp((ph - 0.68f) / 0.27f)));
				float flourish = sin(clamp(ph / 0.32f) * PI);
				p.lean(44f * down);
				p.rArm(-62f * down - 20f * flourish, -58f * down, 12f * flourish);
				p.lArm(28f * down, -6f * down, -34f * down - 12f * flourish);
				p.head(14f * down, 0f, 0f);
				p.lLeg(18f * down, 0f, -3f * down);
				p.rLeg(-6f * down, 0f, 2f * down);
				p.bob(0.4f * down);
			}
			case HEART -> {
				float up = back(clamp(t / 0.45f));
				float beat = pow(max(0f, sin(t * 7.5f)), 3f);
				float s = sin(t * 2.2f);
				p.rArm((-158f - beat * 6f) * up, -24f * up, -18f * up);
				p.lArm((-158f - beat * 6f) * up, 24f * up, 18f * up);
				p.head(-12f * up, s * 6f, s * 9f);
				p.body(0f, s * 6f, s * 3f);
				p.sway(s * 0.7f);
				p.bob(-beat * 0.45f - 0.1f);
				p.rLeg(0f, 0f, 2f + 2f * s);
				p.lLeg(0f, 0f, -2f + 2f * s);
			}
			case FLOSS -> {
				float f = sin(t * PI * 2f * 1.15f);
				float sw = sin(t * PI * 2f * 2.3f);
				float lag = sin(t * PI * 2f * 1.15f - 0.6f);
				p.rArm(30f * sw, 0f, 40f * f);
				p.lArm(-30f * sw, 0f, 40f * f);
				p.body(0f, 6f * sw, -10f * lag);
				p.sway(-2.6f * lag);
				p.head(-4f, 6f * sw, 9f * lag);
				p.rLeg(0f, 0f, -6f * lag);
				p.lLeg(0f, 0f, -6f * lag);
				p.bob(-abs(sw) * 0.35f);
			}
			case TPOSE -> {
				p.rArm(0f, 0f, 90f);
				p.lArm(0f, 0f, -90f);
				p.head(0f, 0f, 0f);
				p.body(0f, 0f, 0f);
			}
			case SIT -> {
				float sit = ease(clamp(t / 0.6f));
				float br = sin(t * 2f);
				float look = sin(t * 0.55f) * 22f + sin(t * 1.3f) * 6f;
				p.bob(10f * sit);
				p.rLeg(-86f * sit, 10f * sit, 0f);
				p.lLeg(-80f * sit, -6f * sit, 0f);
				p.rArm(36f * sit, 0f, 16f * sit);
				p.lArm(36f * sit, 0f, -16f * sit);
				p.lean(-8f * sit + br * 1.2f);
				p.head(6f * sit + br * 2f, look * sit, sin(t * 0.8f) * 4f);
			}
			case GRIDDY -> {
				float b = t * 146f / 60f;
				float ph = b * PI;
				float side = sin(ph);
				float hop = pow(abs(side), 0.7f);
				float goggles = fract(b / 16f) > 0.5f ? 1f : 0f;
				float g = goggles;
				p.rArm((-60f - 40f * side) * (1f - g) - 152f * g, -10f * (1f - g) - 34f * g, 12f * (1f - g));
				p.lArm((-60f + 40f * side) * (1f - g) - 152f * g, 10f * (1f - g) + 34f * g, -12f * (1f - g));
				p.head(6f * hop - 6f * g, side * 7f, side * 3f);
				p.rLeg(-38f * max(0f, side) + 10f * max(0f, -side), 0f, 4f);
				p.lLeg(-38f * max(0f, -side) + 10f * max(0f, side), 0f, -4f);
				p.lean(11f + 3f * hop);
				p.bob(-hop * 1.6f);
				p.sway(side * 0.7f);
				p.body(0f, side * 8f, 0f);
			}
			case SHUFFLE -> {
				float b = t * 126f / 60f;
				float ph = b * PI;
				float step = sin(ph);
				float lift = max(0f, sin(ph * 2f));
				float tw = sin(ph * 2f);
				float ts = fract(b / 16f) > 0.5f ? 1f : 0f;
				float n = 1f - ts;
				p.rLeg((-55f * max(0f, step) + 18f * max(0f, -step)) * n - 12f * lift * ts, 26f * tw * ts, 3f * n + (8f + 10f * max(0f, tw)) * ts);
				p.lLeg((-55f * max(0f, -step) + 18f * max(0f, step)) * n - 12f * (1f - lift) * ts, -26f * tw * ts, -3f * n + (-8f - 10f * max(0f, -tw)) * ts);
				p.rArm((-45f + 35f * step) * n + (-38f - 14f * lift) * ts, -20f * ts, 10f * n + 18f * ts);
				p.lArm((-45f - 35f * step) * n + (-38f - 14f * lift) * ts, 20f * ts, -10f * n - 18f * ts);
				p.body(0f, 12f * tw * ts + 5f * step * n, 0f);
				p.lean(6f * n + 2f);
				p.sway(step * 0.9f);
				p.bob(-abs(sin(ph * 2f)) * 1.5f);
				p.head(6f * lift, step * 8f, 0f);
			}
			case DISCO -> {
				float b = t * 120f / 60f;
				float hip = sin(b * PI);
				float up = fract(b / 4f) < 0.5f ? 1f : 0f;
				float tr = ease(clamp(fract(b / 2f) * 4f)) ;
				float pt = up;
				p.rArm(-172f * pt + (-40f) * (1f - pt), -10f * pt + 30f * (1f - pt), 36f * pt - 42f * (1f - pt));
				p.lArm(-22f, 0f, -18f - 12f * abs(hip));
				p.head(-22f * pt + 12f * (1f - pt), -16f * pt + 16f * (1f - pt), hip * 4f);
				p.body(0f, 12f * hip, 0f);
				p.sway(hip * 1.3f);
				p.rLeg(0f, 0f, 6f + 5f * hip);
				p.lLeg(-12f * max(0f, hip), 0f, -6f);
				p.bob(-abs(hip) * 0.6f * tr);
			}
			case ZOMBIE -> {
				float l = sin(t * 2.2f);
				float stumble = pow(max(0f, sin(t * 1.1f + 0.5f)), 6f);
				float limp = sin(t * 4.4f);
				p.rArm(-90f + l * 5f + 6f * stumble, -6f + limp * 3f, 2f);
				p.lArm(-84f - l * 5f + 6f * stumble, 6f - limp * 3f, -2f);
				p.head(14f + l * 4f + 10f * stumble, l * 10f, 14f * sin(t * 1.1f));
				p.sway(l * 0.6f);
				p.body(0f, l * 7f, l * 3f);
				p.lean(6f + 8f * stumble);
				p.rLeg(-12f * max(0f, l), 0f, 2f);
				p.lLeg(-12f * max(0f, -l), 0f, -2f);
				p.bob(abs(limp) * 0.4f);
			}
			case ROBOT -> {
				float bt = t * 2.4f;
				float i = floor(bt) % 8f;
				float j = (i + 7f) % 8f;
				float k = clamp(fract(bt) / 0.22f);
				float ek = back(k);
				float ra = mix(sel(j, -90f, -90f, 0f, 0f, -90f, -170f, 0f, -45f), sel(i, -90f, -90f, 0f, 0f, -90f, -170f, 0f, -45f), ek);
				float rb = mix(sel(j, 0f, 0f, 0f, 0f, -40f, 0f, 0f, 0f), sel(i, 0f, 0f, 0f, 0f, -40f, 0f, 0f, 0f), ek);
				float rc = mix(sel(j, 0f, 0f, 90f, 0f, 0f, 0f, 0f, 45f), sel(i, 0f, 0f, 90f, 0f, 0f, 0f, 0f, 45f), ek);
				float la = mix(sel(j, 0f, -90f, 0f, -90f, -90f, 0f, -170f, -45f), sel(i, 0f, -90f, 0f, -90f, -90f, 0f, -170f, -45f), ek);
				float lb = mix(sel(j, 0f, 0f, 0f, 0f, 40f, 0f, 0f, 0f), sel(i, 0f, 0f, 0f, 0f, 40f, 0f, 0f, 0f), ek);
				float lc = mix(sel(j, 0f, 0f, -90f, 0f, 0f, 0f, 0f, -45f), sel(i, 0f, 0f, -90f, 0f, 0f, 0f, 0f, -45f), ek);
				float hd = mix(sel(j, 0f, 30f, -30f, 0f, 0f, 20f, -20f, 0f), sel(i, 0f, 30f, -30f, 0f, 0f, 20f, -20f, 0f), ek);
				p.rArm(ra, rb, rc);
				p.lArm(la, lb, lc);
				p.head(0f, hd, 0f);
				p.body(0f, hd * 0.3f, 0f);
				p.bob(0.25f * (1f - k));
			}
			case AIRGUITAR -> {
				float wind = fract(t / 2.4f) > 0.75f ? 1f : 0f;
				float wp = fract(t / 2.4f * 4f) * PI * 2f;
				float strum = sin(t * 21f);
				float bang = pow(abs(sin(t * 5.5f)), 1.5f);
				p.rArm((-36f + strum * 18f) * (1f - wind) + (-90f - 120f * sin(wp)) * wind, -24f * (1f - wind), 10f * (1f - wind) + 60f * wind);
				p.lArm(-72f, 42f, -8f);
				p.head(22f * bang, 0f, sin(t * 2.75f) * 6f);
				p.lean(-7f + bang * 6f);
				p.rLeg(-6f * bang, 0f, 8f);
				p.lLeg(-14f, 0f, -9f);
				p.bob(-bang * 0.5f + 0.6f);
			}
			case JUMPJACKS -> {
				float ph = fract(t * 1.2f);
				float j = 0.5f - 0.5f * cos(ph * PI * 2f);
				float air = max(0f, sin(ph * PI * 2f - PI / 2f) * 0f + sin(ph * PI));
				float land = pow(max(0f, cos(ph * PI * 2f)), 4f);
				p.rArm(-4f * j, 0f, 18f + 152f * j);
				p.lArm(-4f * j, 0f, -18f - 152f * j);
				p.rLeg(0f, 0f, 4f + 15f * j);
				p.lLeg(0f, 0f, -4f - 15f * j);
				p.bob(-2.2f * air + 0.6f * land);
				p.head(-5f * j + 3f * land, 0f, 0f);
			}
			case FLEX -> {
				float up = back(clamp(t / 0.32f));
				float second = ease(clamp((t - 1.8f) / 0.3f));
				float pump = pow(max(0f, sin(t * 6f)), 2f);
				float f = 1f - second;
				p.rArm((-30f * f - 70f * second) * up, (-20f * f - 40f * second) * up, ((128f + 10f * pump) * f + 30f * second) * up);
				p.lArm((-30f * f - 70f * second) * up, (20f * f + 40f * second) * up, ((-128f - 10f * pump) * f - 30f * second) * up);
				p.head((-12f + 8f * pump) * up * f + 12f * second, sin(t * 2.5f) * 6f, 0f);
				p.lean((-6f + 4f * pump) * f + 10f * second);
				p.bob(-0.3f * up + 0.8f * second);
				p.rLeg(0f, 0f, 5f * up);
				p.lLeg(0f, 0f, -5f * up);
			}
			case KISS -> {
				float a = back(clamp(t / 0.38f));
				float out = ease(clamp((t - 0.85f) / 0.3f));
				float fall = ease(clamp((t - 1.9f) / 0.6f));
				p.rArm(((-124f + 92f * out) * a) * (1f - fall), ((-34f + 16f * out) * a) * (1f - fall), ((8f + 40f * out) * a) * (1f - fall));
				p.lArm(4f, 0f, -6f);
				p.head(-6f * a + 6f * out, -12f * a + 8f * out, 8f * a);
				p.lean(-3f * a + 6f * out * (1f - fall));
				p.bob(-0.25f * out);
				p.lLeg(-10f * out * (1f - fall), 0f, -2f);
			}
			case SPIN -> {
				float s = ease(clamp(t / 1.7f));
				float arms = sin(clamp(t / 1.7f) * PI);
				p.spin(s * 720f);
				p.rArm(0f, 0f, 40f + 50f * arms);
				p.lArm(0f, 0f, -40f - 50f * arms);
				p.head(-12f * arms, 0f, 6f * arms);
				p.lean(-4f * arms);
				p.bob(-abs(sin(t * 5f)) * 0.6f * arms);
				p.rLeg(0f, 0f, 6f * arms);
				p.lLeg(-20f * arms, 0f, -8f * arms);
			}
			case FACEPLANT -> {
				float dz = t < 0.7f ? 1f : 0f;
				float wob = sin(t * 9f) * 8f * (1f - clamp((t - 0.4f) / 0.3f));
				float fall = t < 0.7f ? 0f : clamp((t - 0.7f) / 0.32f);
				float ff = fall * fall;
				float bounce = t > 1.02f ? sin(clamp((t - 1.02f) / 0.3f) * PI) * (1f - clamp((t - 1.02f) / 0.3f)) : 0f;
				p.head(wob + 12f * ff, wob, wob * 0.8f);
				p.rArm(-20f - 150f * ff, 0f, 18f + 10f * ff);
				p.lArm(-20f - 150f * ff, 0f, -18f - 10f * ff);
				p.lean(wob * 0.5f + 84f * ff - 8f * bounce);
				p.bob(11f * ff);
				p.rLeg(84f * ff, 0f, 4f);
				p.lLeg(84f * ff, 0f, -4f);
				p.sway(wob * 0.04f);
			}
			case CRY -> {
				float ein = back(clamp(t / 0.4f));
				float sob = pow(abs(sin(t * 4.5f)), 3f);
				float rub = sin(t * 3.4f);
				p.rArm((-128f + rub * 6f) * ein, (-36f + rub * 5f) * ein, 0f);
				p.lArm((-128f - rub * 6f) * ein, (36f - rub * 5f) * ein, 0f);
				p.head(18f + sob * 6f, rub * 5f, sin(t * 1.3f) * 5f);
				p.lean(6f + sob * 4f);
				p.bob(sob * 0.6f);
				p.body(0f, 0f, sin(t * 1.3f) * 2f);
			}
		}
	}
}
