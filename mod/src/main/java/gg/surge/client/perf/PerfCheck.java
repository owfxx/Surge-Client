package gg.surge.client.perf;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.entity.Entity;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PerfCheck {
	public enum State { IDLE, RECORDING, DONE }

	public static final int SECONDS = 15;
	private static final int CAP = 30000;

	public static volatile State state = State.IDLE;
	public static Report report;

	private static final float[] FRAMES = new float[CAP];
	private static final boolean[] GC = new boolean[CAP], MOVING = new boolean[CAP];
	private static int n;
	private static long startNs, lastNs, lastGc, lastPosNs, lastEntNs, recordedNs;
	private static double lastX, lastZ;
	private static boolean movingNow;
	private static int maxEntities;
	private static float heapPeak;
	private static List<GarbageCollectorMXBean> beans;

	public static final class Finding {
		public final int severity;
		public final String title, detail, fixLabel;
		public final Runnable fix;
		public boolean fixed;

		Finding(int severity, String title, String detail, String fixLabel, Runnable fix) {
			this.severity = severity;
			this.title = title;
			this.detail = detail;
			this.fixLabel = fixLabel;
			this.fix = fix;
		}
	}

	public record Report(float avgFps, float lowFps, float worstMs, int stutters, int gcStutters, int moveStutters,
						 float[] frames, boolean[] gc, boolean[] moving, float spikeMs, int entities, float heapPeak,
						 float ramGb, long at, List<Finding> findings) {}

	private PerfCheck() {}

	public static void start() {
		n = 0;
		startNs = System.nanoTime();
		lastNs = 0;
		recordedNs = 0;
		lastGc = gcCount();
		lastPosNs = 0;
		lastEntNs = 0;
		maxEntities = 0;
		heapPeak = 0;
		movingNow = false;
		state = State.RECORDING;
	}

	public static void cancel() {
		state = report == null ? State.IDLE : State.DONE;
	}

	public static int secondsLeft() {
		return Math.max(0, SECONDS - (int) (recordedNs / 1_000_000_000L));
	}

	private static long gcCount() {
		if (beans == null) beans = ManagementFactory.getGarbageCollectorMXBeans();
		long c = 0;
		for (GarbageCollectorMXBean b : beans) c += Math.max(0, b.getCollectionCount());
		return c;
	}

	public static void frame(Minecraft mc) {
		if (state != State.RECORDING) return;
		if (mc.level == null || mc.player == null) { cancel(); return; }
		long now = System.nanoTime();
		if (mc.isPaused() || mc.screen != null) { lastNs = 0; return; }
		if (lastNs != 0 && n < CAP) {
			long d = now - lastNs;
			recordedNs += d;
			FRAMES[n] = d / 1e6f;
			long gc = gcCount();
			GC[n] = gc != lastGc;
			lastGc = gc;
			MOVING[n] = movingNow;
			n++;
		} else lastGc = gcCount();
		lastNs = now;
		if (now - lastPosNs > 250_000_000L) {
			double x = mc.player.getX(), z = mc.player.getZ();
			if (lastPosNs != 0) {
				double dist = Math.sqrt((x - lastX) * (x - lastX) + (z - lastZ) * (z - lastZ));
				movingNow = dist / ((now - lastPosNs) / 1e9) > 6.0;
			}
			lastX = x;
			lastZ = z;
			lastPosNs = now;
			Runtime r = Runtime.getRuntime();
			heapPeak = Math.max(heapPeak, (r.totalMemory() - r.freeMemory()) / (float) r.maxMemory());
		}
		if (now - lastEntNs > 1_000_000_000L) {
			int c = 0;
			for (Entity ignored : mc.level.entitiesForRendering()) c++;
			maxEntities = Math.max(maxEntities, c);
			lastEntNs = now;
		}
		if (recordedNs >= SECONDS * 1_000_000_000L || n >= CAP) finish(mc);
	}

	private static void finish(Minecraft mc) {
		report = analyze(mc);
		state = State.DONE;
	}

	private static String name(Object o) {
		return o instanceof Enum<?> e ? e.name() : String.valueOf(o);
	}

	private static Report analyze(Minecraft mc) {
		float[] f = Arrays.copyOf(FRAMES, n);
		boolean[] gc = Arrays.copyOf(GC, n), mv = Arrays.copyOf(MOVING, n);
		float[] sorted = f.clone();
		Arrays.sort(sorted);
		double sum = 0;
		for (float v : f) sum += v;
		float avg = n == 0 ? 0 : (float) (n / (sum / 1000.0));
		float median = n == 0 ? 16 : sorted[n / 2];
		float p99 = n == 0 ? 16 : sorted[Math.min(n - 1, (int) (n * 0.99))];
		float low = 1000f / Math.max(0.1f, p99);
		float worst = n == 0 ? 0 : sorted[n - 1];
		float spike = Math.max(median * 2.2f, median + 12f);
		int stutters = 0, gcS = 0, moveS = 0;
		for (int i = 0; i < n; i++) {
			if (f[i] < spike) continue;
			stutters++;
			boolean nearGc = gc[i] || (i + 1 < n && gc[i + 1]);
			if (nearGc) gcS++;
			else if (mv[i]) moveS++;
		}
		float ram = Runtime.getRuntime().maxMemory() / 1073741824f;

		Options o = mc.options;
		List<Finding> out = new ArrayList<>();
		int rd = o.renderDistance().get();
		if (heapPeak > 0.9f) {
			out.add(new Finding(2, "RAM almost full", "The game used " + Math.round(heapPeak * 100) + "% of its " + fmt(ram) + " GB. When it fills up, Minecraft freezes to clean up. Give it more RAM in the Surge launcher settings (4 to 6 GB is a good amount).", null, null));
		}
		if (gcS >= 3 && gcS >= stutters * 0.35f) {
			if (ram < 3f) out.add(new Finding(2, "Not enough RAM", gcS + " of your " + stutters + " stutters happened while the game was cleaning up memory. It only has " + fmt(ram) + " GB. Raise it to 4 GB in the Surge launcher settings.", null, null));
			else if (ram > 10f) out.add(new Finding(1, "Too much RAM", gcS + " stutters came from memory cleanup. With " + fmt(ram) + " GB each cleanup takes longer. 4 to 8 GB in the launcher settings is usually smoother.", null, null));
			else out.add(new Finding(1, "Memory cleanup stutter", gcS + " of your " + stutters + " stutters happened during memory cleanup. Lower render distance and fewer entities around you create less to clean up.",
				rd > 10 ? "Render distance " + Math.max(8, rd - 4) : null, rd > 10 ? () -> setRd(mc, Math.max(8, rd - 4)) : null));
		}
		if (moveS >= 3 && moveS >= stutters * 0.4f) {
			Performance.Feature blend = Performance.byKey("lightBiomeBlend");
			if (rd > 10) out.add(new Finding(1, "Stutters while moving", moveS + " stutters happened while you were moving, which means new chunks are loading. A lower render distance loads fewer chunks at once.",
				"Render distance " + Math.max(8, rd - 4), () -> setRd(mc, Math.max(8, rd - 4))));
			else if (!Performance.enabled(blend)) out.add(new Finding(1, "Stutters while moving", moveS + " stutters happened while new chunks were loading. Light Biome Blend makes chunks build faster.", "Turn on", () -> Performance.set(mc, blend, true)));
			else out.add(new Finding(1, "Stutters while moving", moveS + " stutters happened while new chunks were loading. Your settings are already light, so this is likely your CPU or the server sending chunks slowly.", null, null));
		}
		if (maxEntities > 250) {
			boolean canFix = o.entityDistanceScaling().get() > 0.75 || !Performance.enabled(Performance.byKey("cullEntities"));
			out.add(new Finding(1, "Lots of entities", "Up to " + maxEntities + " mobs, items and players were around you. Each one costs FPS. Drawing them a bit closer helps a lot on busy servers and farms.",
				canFix ? "Draw closer" : null, canFix ? () -> {
					if (o.entityDistanceScaling().get() > 0.75) o.entityDistanceScaling().set(0.75);
					Performance.set(mc, Performance.byKey("cullEntities"), true);
				} : null));
		}
		if (rd > 16 && avg < 90) out.add(new Finding(1, "High render distance", "Render distance is " + rd + " chunks. Going down to 12 often doubles FPS and most people don't notice the difference.", "Set to 12", () -> setRd(mc, 12)));
		if (mc.hasSingleplayerServer() && o.simulationDistance().get() > 10)
			out.add(new Finding(0, "High simulation distance", "In singleplayer your PC also runs the world. " + o.simulationDistance().get() + " chunks of ticking mobs and redstone is heavy.", "Set to 8", () -> { o.simulationDistance().set(8); o.save(); }));
		if (o.improvedTransparency().get() && avg < 120) {
			Performance.Feature ft = Performance.byKey("fastTransparency");
			out.add(new Finding(1, "Improved transparency is on", "This is one of the heaviest graphics settings. Turning it off barely changes how the game looks.", "Turn off", () -> Performance.set(mc, ft, true)));
		}
		if (name(o.cloudStatus().get()).equals("FANCY") && avg < 100) out.add(new Finding(0, "Fancy clouds", "Fast clouds look almost the same and cost less.", "Fast clouds", () -> Performance.set(mc, Performance.byKey("fastClouds"), true)));
		if (name(o.particles().get()).equals("ALL") && avg < 100) out.add(new Finding(0, "All particles", "Explosions, potions and farms can spawn thousands of particles.", "Fewer", () -> Performance.set(mc, Performance.byKey("fewerParticles"), true)));
		int cap = o.framerateLimit().get();
		if (cap < 260 && avg > cap * 0.9f) out.add(new Finding(0, "FPS is capped at " + cap, "Your game could run faster but Max FPS holds it back. That's fine if you like it, it saves power.", "Unlimited", () -> { o.framerateLimit().set(260); o.save(); }));
		else if (o.enableVsync().get() && avg > 50) out.add(new Finding(0, "VSync is on", "VSync locks your FPS to your monitor and adds a little input delay.", "Turn off", () -> Performance.set(mc, Performance.byKey("unlimitedFps"), true)));
		if (!FabricLoader.getInstance().isModLoaded("sodium"))
			out.add(new Finding(avg < 60 ? 1 : 0, "Sodium isn't installed", "Sodium is the biggest FPS boost there is for Fabric. Turn it on under Settings, Performance mods, then restart.", null, null));
		if (Runtime.getRuntime().availableProcessors() < 4)
			out.add(new Finding(0, "Few CPU cores", "Your PC has " + Runtime.getRuntime().availableProcessors() + " CPU threads. Closing other programs while playing helps.", null, null));
		out.sort((a, b) -> Integer.compare(b.severity, a.severity));
		if (out.stream().noneMatch(x -> x.severity >= 1)) out.add(0, new Finding(-1, low >= 50 ? "Your game runs smoothly" : "No obvious problem found",
			low >= 50 ? "No big stutters and your settings look good." : "Your settings look fine. Low FPS here most likely comes from your PC's graphics card or CPU.", null, null));
		return new Report(avg, low, worst, stutters, gcS, moveS, f, gc, mv, spike, maxEntities, heapPeak, ram, System.currentTimeMillis(), out);
	}

	private static void setRd(Minecraft mc, int v) {
		mc.options.renderDistance().set(v);
		mc.options.save();
	}

	private static String fmt(float gb) {
		return gb >= 10 ? String.valueOf(Math.round(gb)) : String.format("%.1f", gb);
	}
}
