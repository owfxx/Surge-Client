package gg.surge.client.features;

import gg.surge.client.mods.Mods;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class Clips {
	private record Frame(byte[] jpg, int w, int h, long at) {}

	private static final Deque<Frame> FRAMES = new ArrayDeque<>();
	private static final int THREADS = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 2));
	private static final ExecutorService WORKER = Executors.newFixedThreadPool(THREADS, r -> { Thread t = new Thread(r, "Surge Clips"); t.setDaemon(true); t.setPriority(Thread.MIN_PRIORITY + 1); return t; });
	private static final AtomicInteger PENDING = new AtomicInteger();
	private static long lastGrab;
	private static com.mojang.blaze3d.pipeline.RenderTarget small;
	private static boolean saving;

	private Clips() {}

	private static int fps() {
		try { return Math.max(10, Math.min(120, Integer.parseInt(Mods.CLIP_FPS.get()))); } catch (Exception e) { return 30; }
	}

	private static int targetHeight() {
		return switch (Mods.CLIP_RES.get()) { case "480p" -> 480; case "1080p" -> 1080; case "Full" -> 100_000; default -> 720; };
	}

	private static final int SLOTS = 3;
	private static final com.mojang.blaze3d.buffers.GpuBuffer[] GPU = new com.mojang.blaze3d.buffers.GpuBuffer[SLOTS];
	private static final boolean[] BUSY = new boolean[SLOTS];
	private static final java.util.concurrent.ConcurrentLinkedQueue<java.nio.ByteBuffer> SPARE = new java.util.concurrent.ConcurrentLinkedQueue<>();
	private static long gpuSize;

	private static java.nio.ByteBuffer spare(int bytes) {
		java.nio.ByteBuffer b;
		while ((b = SPARE.poll()) != null) {
			if (b.capacity() >= bytes) return b;
			org.lwjgl.system.MemoryUtil.memFree(b);
		}
		return org.lwjgl.system.MemoryUtil.memAlloc(bytes);
	}

	public static void frame(Minecraft mc) {
		if (!Mods.CLIPS.enabled() || mc.level == null) {
			if (!FRAMES.isEmpty() && !saving) synchronized (FRAMES) { FRAMES.clear(); }
			return;
		}
		long now = System.nanoTime();
		long gap = 1_000_000_000L / fps();
		if (now - lastGrab < gap || PENDING.get() > THREADS * 2 + 1) return;
		if (mc.isPaused() || (Mods.CLIP_SMART.on() && mc.getFps() < 30 && fps() > 20)) return;
		try {
			com.mojang.blaze3d.textures.GpuTexture tex = mc.getMainRenderTarget().getColorTexture();
			if (tex == null) return;
			int w = tex.getWidth(0), h = tex.getHeight(0), px = tex.getFormat().pixelSize();
			long size = (long) w * h * px;
			if (size != gpuSize) {
				for (int i = 0; i < SLOTS; i++) {
					if (BUSY[i]) return;
					if (GPU[i] != null) GPU[i].close();
					GPU[i] = null;
				}
				gpuSize = size;
			}
			int slot = -1;
			for (int i = 0; i < SLOTS; i++) if (!BUSY[i]) { slot = i; break; }
			if (slot < 0) return;
			var device = com.mojang.blaze3d.systems.RenderSystem.getDevice();
			if (GPU[slot] == null) GPU[slot] = device.createBuffer(() -> "surge clip", com.mojang.blaze3d.buffers.GpuBuffer.USAGE_MAP_READ | com.mojang.blaze3d.buffers.GpuBuffer.USAGE_COPY_DST, size);
			lastGrab = now - Math.min(gap, Math.max(0, now - lastGrab - gap));
			long at = System.currentTimeMillis();
			int s = slot;
			com.mojang.blaze3d.buffers.GpuBuffer buf = GPU[s];
			BUSY[s] = true;
			PENDING.incrementAndGet();
			var enc = device.createCommandEncoder();
			enc.copyTextureToBuffer(tex, buf, 0L, () -> {
				java.nio.ByteBuffer copy = null;
				try (com.mojang.blaze3d.buffers.GpuBuffer.MappedView view = enc.mapBuffer(buf, true, false)) {
					java.nio.ByteBuffer src = view.data();
					copy = spare((int) size);
					org.lwjgl.system.MemoryUtil.memCopy(org.lwjgl.system.MemoryUtil.memAddress(src), org.lwjgl.system.MemoryUtil.memAddress(copy), size);
				} catch (Throwable t) {
					fail("read", t);
				} finally {
					BUSY[s] = false;
				}
				if (copy == null) { PENDING.decrementAndGet(); return; }
				java.nio.ByteBuffer data = copy;
				try {
					WORKER.execute(() -> {
						try { encode(data, w, h, px, at); } finally { SPARE.offer(data); PENDING.decrementAndGet(); }
					});
				} catch (Throwable t) {
					SPARE.offer(data);
					PENDING.decrementAndGet();
				}
			}, 0);
		} catch (Throwable t) {
			fail("grab", t);
		}
	}

	private static void encode(java.nio.ByteBuffer raw, int sw, int sh, int px, long at) {
		try {
			java.nio.ByteBuffer src = raw.duplicate().order(java.nio.ByteOrder.nativeOrder());
			float k = Math.min(1f, targetHeight() / (float) sh);
			int w = Math.max(2, Math.round(sw * k)) & ~1, h = Math.max(2, Math.round(sh * k)) & ~1;
			BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			int[] dst = ((java.awt.image.DataBufferInt) bi.getRaster().getDataBuffer()).getData();
			for (int y = 0; y < h; y++) {
				int sy = sh - 1 - Math.min(sh - 1, (int) (y / k));
				int row = sy * sw;
				int dy = y * w;
				for (int x = 0; x < w; x++) {
					int v = src.getInt((row + Math.min(sw - 1, (int) (x / k))) * px);
					dst[dy + x] = ((v & 0xFF) << 16) | (v & 0xFF00) | ((v >> 16) & 0xFF);
				}
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream(64_000);
			ImageWriter wr = ImageIO.getImageWritersByFormatName("jpeg").next();
			ImageWriteParam p = wr.getDefaultWriteParam();
			p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			p.setCompressionQuality(Mods.CLIP_QUALITY.get() / 100f);
			try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
				wr.setOutput(ios);
				wr.write(null, new IIOImage(bi, null, null), p);
			}
			wr.dispose();
			long keep = Math.round(Mods.CLIP_LENGTH.get() * 1000);
			synchronized (FRAMES) {
				FRAMES.addLast(new Frame(out.toByteArray(), w, h, at));
				long newest = 0;
				for (Frame f : FRAMES) newest = Math.max(newest, f.at());
				while (!FRAMES.isEmpty() && newest - FRAMES.peekFirst().at() > keep) FRAMES.pollFirst();
				long total = 0;
				for (Frame f : FRAMES) total += f.jpg().length;
				long cap = Math.min(900L << 20, Runtime.getRuntime().maxMemory() / 4);
				while (total > cap && FRAMES.size() > 10) total -= FRAMES.pollFirst().jpg().length;
			}
		} catch (Throwable t) {
			fail("encode", t);
		}
	}

	private static boolean warned;

	private static void fail(String where, Throwable t) {
		if (warned) return;
		warned = true;
		org.slf4j.LoggerFactory.getLogger("surge").warn("Clips " + where + " failed", t);
	}

	public static int frames() {
		synchronized (FRAMES) { return FRAMES.size(); }
	}

	public static void save(Minecraft mc) {
		if (!Mods.CLIPS.enabled()) {
			gg.surge.client.net.Social.notice("Clips are off", "Turn on Clips in the Surge menu first");
			return;
		}
		List<Frame> copy;
		synchronized (FRAMES) { copy = new ArrayList<>(FRAMES); }
		copy.sort(java.util.Comparator.comparingLong(Frame::at));
		if (copy.size() < 10) {
			gg.surge.client.net.Social.notice("Nothing to save yet", "Play for a few seconds, then try again");
			return;
		}
		saving = true;
		int fps = fps();
		WORKER.execute(() -> {
			try {
				Path dir = FabricLoader.getInstance().getGameDir().resolve("clips");
				Files.createDirectories(dir);
				Path file = dir.resolve("clip-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".avi");
				Frame first = copy.get(0);
				List<Frame> use = new ArrayList<>();
				for (Frame f : copy) if (f.w() == first.w() && f.h() == first.h()) use.add(f);
				double secs = Math.max(0.5, (use.get(use.size() - 1).at() - use.get(0).at()) / 1000.0);
				int realFps = (int) Math.max(5, Math.min(120, Math.round(use.size() / secs)));
				try (OutputStream os = Files.newOutputStream(file)) { writeAvi(os, use, first.w(), first.h(), realFps > 0 ? realFps : fps); }
				org.slf4j.LoggerFactory.getLogger("surge").info("Clip saved " + file + " frames=" + use.size() + " bytes=" + Files.size(file));
				Minecraft.getInstance().execute(() -> gg.surge.client.net.Social.notice("Clip saved", "clips/" + file.getFileName()));
			} catch (Exception e) {
				Minecraft.getInstance().execute(() -> gg.surge.client.net.Social.notice("Couldn't save the clip", String.valueOf(e.getMessage())));
			} finally {
				saving = false;
			}
		});
	}

	private static void le32(DataOutputStream d, int v) throws IOException { d.writeInt(Integer.reverseBytes(v)); }

	private static void le16(DataOutputStream d, int v) throws IOException { d.writeShort(Short.reverseBytes((short) v)); }

	private static void tag(DataOutputStream d, String s) throws IOException { d.writeBytes(s); }

	private static void writeAvi(OutputStream raw, List<Frame> frames, int w, int h, int fps) throws IOException {
		int n = frames.size();
		int moviSize = 4;
		int maxFrame = 0;
		for (Frame f : frames) { int len = f.jpg().length; moviSize += 8 + len + (len & 1); maxFrame = Math.max(maxFrame, len); }
		int strl = 4 + (8 + 56) + (8 + 40);
		int hdrl = 4 + (8 + 56) + (8 + strl);
		int idx = 16 * n;
		int riff = 4 + (8 + hdrl) + (8 + moviSize) + (8 + idx);
		DataOutputStream d = new DataOutputStream(new java.io.BufferedOutputStream(raw, 1 << 16));
		tag(d, "RIFF"); le32(d, riff); tag(d, "AVI ");
		tag(d, "LIST"); le32(d, hdrl); tag(d, "hdrl");
		tag(d, "avih"); le32(d, 56);
		le32(d, 1_000_000 / fps); le32(d, maxFrame * fps); le32(d, 0); le32(d, 0x10);
		le32(d, n); le32(d, 0); le32(d, 1); le32(d, maxFrame);
		le32(d, w); le32(d, h); le32(d, 0); le32(d, 0); le32(d, 0); le32(d, 0);
		tag(d, "LIST"); le32(d, strl); tag(d, "strl");
		tag(d, "strh"); le32(d, 56);
		tag(d, "vids"); tag(d, "MJPG"); le32(d, 0); le16(d, 0); le16(d, 0); le32(d, 0);
		le32(d, 1); le32(d, fps); le32(d, 0); le32(d, n); le32(d, maxFrame); le32(d, -1); le32(d, 0);
		le16(d, 0); le16(d, 0); le16(d, w); le16(d, h);
		tag(d, "strf"); le32(d, 40);
		le32(d, 40); le32(d, w); le32(d, h); le16(d, 1); le16(d, 24); tag(d, "MJPG"); le32(d, w * h * 3);
		le32(d, 0); le32(d, 0); le32(d, 0); le32(d, 0);
		tag(d, "LIST"); le32(d, moviSize); tag(d, "movi");
		for (Frame f : frames) {
			tag(d, "00dc"); le32(d, f.jpg().length); d.write(f.jpg());
			if ((f.jpg().length & 1) == 1) d.writeByte(0);
		}
		tag(d, "idx1"); le32(d, idx);
		int off = 4;
		for (Frame f : frames) {
			tag(d, "00dc"); le32(d, 0x10); le32(d, off); le32(d, f.jpg().length);
			off += 8 + f.jpg().length + (f.jpg().length & 1);
		}
		d.flush();
	}
}
