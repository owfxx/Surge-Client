package gg.surge.client.features;

import com.mojang.blaze3d.platform.NativeImage;
import gg.surge.client.mods.Mods;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

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
	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "Surge Clips"); t.setDaemon(true); return t; });
	private static final AtomicInteger PENDING = new AtomicInteger();
	private static long lastGrab;
	private static boolean saving;

	private Clips() {}

	private static int fps() {
		return switch (Mods.CLIP_FPS.get()) { case "15" -> 15; case "30" -> 30; default -> 20; };
	}

	public static void frame(Minecraft mc) {
		if (!Mods.CLIPS.enabled() || mc.level == null) {
			if (!FRAMES.isEmpty() && !saving) synchronized (FRAMES) { FRAMES.clear(); }
			return;
		}
		long now = System.nanoTime();
		if (now - lastGrab < 1_000_000_000L / fps() || PENDING.get() > 3) return;
		lastGrab = now;
		PENDING.incrementAndGet();
		try {
			Screenshot.takeScreenshot(mc.getMainRenderTarget(), img -> {
				try {
					WORKER.execute(() -> {
						try { encode(img); } finally { img.close(); PENDING.decrementAndGet(); }
					});
				} catch (Throwable t) {
					img.close();
					PENDING.decrementAndGet();
				}
			});
		} catch (Throwable t) {
			PENDING.decrementAndGet();
			fail("grab", t);
		}
	}

	private static void encode(NativeImage img) {
		try {
			int step = Math.max(1, Math.round(img.getWidth() / 720f));
			int w = (img.getWidth() / step) & ~1, h = (img.getHeight() / step) & ~1;
			BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			int[] row = new int[w];
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					int c = img.getPixel(x * step, y * step);
					row[x] = c & 0xFFFFFF;
				}
				bi.setRGB(0, y, w, 1, row, 0, w);
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream(48_000);
			ImageWriter wr = ImageIO.getImageWritersByFormatName("jpeg").next();
			ImageWriteParam p = wr.getDefaultWriteParam();
			p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			p.setCompressionQuality(Mods.CLIP_QUALITY.get() / 100f);
			try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
				wr.setOutput(ios);
				wr.write(null, new IIOImage(bi, null, null), p);
			}
			wr.dispose();
			long now = System.currentTimeMillis();
			long keep = Math.round(Mods.CLIP_LENGTH.get() * 1000);
			synchronized (FRAMES) {
				FRAMES.addLast(new Frame(out.toByteArray(), w, h, now));
				while (!FRAMES.isEmpty() && now - FRAMES.peekFirst().at() > keep) FRAMES.pollFirst();
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
				int realFps = (int) Math.max(5, Math.min(60, Math.round(use.size() / secs)));
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
