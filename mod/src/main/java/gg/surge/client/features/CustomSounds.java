package gg.surge.client.features;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.blaze3d.audio.SoundBuffer;
import gg.surge.client.mods.Mods;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.util.valueproviders.ConstantFloat;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomSounds {
	public static final List<String> PRESETS = List.of("pop", "click", "ding", "coin", "laser", "bubble", "chime", "thump", "boing", "whoosh", "blip", "crit", "bell", "zap");
	public static final List<String> POPULAR = List.of(
		"entity.player.attack.strong", "entity.player.attack.crit", "entity.player.attack.sweep", "entity.player.attack.knockback",
		"entity.player.attack.weak", "entity.player.attack.nodamage", "entity.player.hurt", "entity.arrow.hit_player", "entity.arrow.shoot",
		"entity.experience_orb.pickup", "entity.player.levelup", "entity.item.pickup", "entity.generic.eat", "entity.generic.drink",
		"block.note_block.pling", "ui.button.click", "entity.ender_pearl.throw", "entity.generic.explode", "block.anvil.land", "entity.firework_rocket.blast");

	private static final Gson GSON = new Gson();
	private static final Map<String, String> MAP = new LinkedHashMap<>();
	private static final Map<String, CompletableFuture<SoundBuffer>> BUFFERS = new ConcurrentHashMap<>();
	private static final Map<String, String> SOURCES = new ConcurrentHashMap<>();
	private static boolean loaded;
	private static String preview;

	private CustomSounds() {}

	private static Path file() {
		return Path.of(System.getProperty("user.home"), ".surgeclient", "sounds.json");
	}

	public static Path folder() {
		Path p = FabricLoader.getInstance().getGameDir().resolve("custom sounds");
		try { Files.createDirectories(p); } catch (Exception ignored) { }
		return p;
	}

	private static void load() {
		if (loaded) return;
		loaded = true;
		try {
			Map<String, String> m = GSON.fromJson(Files.readString(file()), new TypeToken<Map<String, String>>() {}.getType());
			if (m != null) MAP.putAll(m);
		} catch (Exception ignored) { }
	}

	private static void save() {
		try {
			Files.createDirectories(file().getParent());
			Files.writeString(file(), GSON.toJson(MAP));
		} catch (Exception ignored) { }
	}

	public static Map<String, String> all() {
		load();
		return MAP;
	}

	public static String get(String event) {
		load();
		return MAP.get(event);
	}

	public static void set(String event, String source) {
		load();
		if (source == null) MAP.remove(event);
		else MAP.put(event, source);
		save();
	}

	public static void resetAll() {
		load();
		MAP.clear();
		save();
	}

	public static String label(String source) {
		if (source == null) return "Default";
		if (source.startsWith("preset:")) {
			String n = source.substring(7);
			return Character.toUpperCase(n.charAt(0)) + n.substring(1);
		}
		return Path.of(source).getFileName().toString();
	}

	public static List<Path> userFiles() {
		List<Path> out = new ArrayList<>();
		try (var s = Files.walk(folder(), 3)) {
			s.filter(Files::isRegularFile).filter(p -> playable(p.getFileName().toString())).sorted().forEach(out::add);
		} catch (Exception ignored) { }
		return out;
	}

	public static boolean playable(String name) {
		String n = name.toLowerCase(Locale.ROOT);
		return n.endsWith(".mp3") || n.endsWith(".wav") || n.endsWith(".ogg");
	}

	private static String keyFor(String source) {
		String k = Integer.toHexString(source.hashCode()) + Integer.toHexString(source.length());
		SOURCES.put(k, source);
		return k;
	}

	public static Sound swap(SoundInstance instance, Sound original) {
		String src = preview;
		if (src == null) {
			if (!Mods.CUSTOM_SOUNDS.enabled() || instance == null || original == null) return original;
			src = get(instance.getIdentifier().toString());
			if (src == null) return original;
		}
		int dist = original != null ? original.getAttenuationDistance() : 16;
		return new Sound(Identifier.fromNamespaceAndPath("surge", "custom/" + keyFor(src)), ConstantFloat.of(1f), ConstantFloat.of(1f), 1, Sound.Type.FILE, false, false, dist);
	}

	public static CompletableFuture<SoundBuffer> buffer(Identifier path) {
		if (!path.getNamespace().equals("surge") || !path.getPath().startsWith("sounds/custom/")) return null;
		String key = path.getPath().substring("sounds/custom/".length());
		if (key.endsWith(".ogg")) key = key.substring(0, key.length() - 4);
		String src = SOURCES.get(key);
		if (src == null) return null;
		return BUFFERS.computeIfAbsent(key, k -> CompletableFuture.supplyAsync(() -> decode(src), net.minecraft.util.Util.backgroundExecutor()));
	}

	public static void clearBuffers() {
		for (var f : BUFFERS.values()) f.thenAccept(SoundBuffer::discardAlBuffer);
		BUFFERS.clear();
	}

	public static void forget(String source) {
		String k = Integer.toHexString(source.hashCode()) + Integer.toHexString(source.length());
		var f = BUFFERS.remove(k);
		if (f != null) f.thenAccept(SoundBuffer::discardAlBuffer);
	}

	public static void preview(String source) {
		preview(source, 1f, 1f);
	}

	public static void preview(String source, float pitch, float volume) {
		Minecraft mc = Minecraft.getInstance();
		preview = source;
		try {
			mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), pitch, volume));
		} finally {
			preview = null;
		}
	}

	public static void playOriginal(String event) {
		Minecraft mc = Minecraft.getInstance();
		var ev = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.parse(event));
		String keep = MAP.remove(event);
		try {
			mc.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(ev, 1f, 1f));
		} finally {
			if (keep != null) MAP.put(event, keep);
		}
	}

	private static SoundBuffer decode(String src) {
		try {
			byte[] raw;
			String name;
			if (src.startsWith("preset:")) {
				name = src.substring(7) + ".wav";
				try (InputStream in = CustomSounds.class.getResourceAsStream("/assets/surge/custom_sounds/" + name)) {
					raw = in.readAllBytes();
				}
			} else {
				name = src;
				raw = Files.readAllBytes(Path.of(src));
			}
			float[] mono;
			float rate;
			String lower = name.toLowerCase(Locale.ROOT);
			if (lower.endsWith(".mp3")) {
				Pcm p = mp3(raw);
				mono = p.data; rate = p.rate;
			} else if (lower.endsWith(".ogg")) {
				Pcm p = ogg(raw);
				mono = p.data; rate = p.rate;
			} else {
				Pcm p = wav(raw);
				mono = p.data; rate = p.rate;
			}
			int max = (int) (rate * 20);
			int n = Math.min(mono.length, max);
			ByteBuffer out = org.lwjgl.BufferUtils.createByteBuffer(n * 2).order(ByteOrder.LITTLE_ENDIAN);
			for (int i = 0; i < n; i++) out.putShort((short) Math.round(Math.max(-1f, Math.min(1f, mono[i])) * 32767));
			out.flip();
			return new SoundBuffer(out, new AudioFormat(rate, 16, 1, true, false));
		} catch (Throwable t) {
			org.slf4j.LoggerFactory.getLogger("surge").warn("Couldn't load custom sound " + label(src), t);
			ByteBuffer out = org.lwjgl.BufferUtils.createByteBuffer(2);
			return new SoundBuffer(out, new AudioFormat(22050, 16, 1, true, false));
		}
	}

	private record Pcm(float[] data, float rate) {}

	private static Pcm mp3(byte[] raw) throws Exception {
		javazoom.jl.decoder.Bitstream bs = new javazoom.jl.decoder.Bitstream(new java.io.ByteArrayInputStream(raw));
		javazoom.jl.decoder.Decoder dec = new javazoom.jl.decoder.Decoder();
		FloatList out = new FloatList();
		float rate = 44100;
		javazoom.jl.decoder.Header h;
		while ((h = bs.readFrame()) != null) {
			javazoom.jl.decoder.SampleBuffer sb = (javazoom.jl.decoder.SampleBuffer) dec.decodeFrame(h, bs);
			rate = sb.getSampleFrequency();
			int ch = sb.getChannelCount();
			short[] s = sb.getBuffer();
			int len = sb.getBufferLength();
			for (int i = 0; i + ch - 1 < len; i += ch) {
				float sum = 0;
				for (int c = 0; c < ch; c++) sum += s[i + c];
				out.add(sum / ch / 32768f);
			}
			bs.closeFrame();
			if (out.size > rate * 20) break;
		}
		bs.close();
		return new Pcm(out.toArray(), rate);
	}

	private static Pcm ogg(byte[] raw) throws Exception {
		try (net.minecraft.client.sounds.JOrbisAudioStream s = new net.minecraft.client.sounds.JOrbisAudioStream(new java.io.ByteArrayInputStream(raw))) {
			AudioFormat f = s.getFormat();
			int ch = Math.max(1, f.getChannels());
			FloatList all = new FloatList();
			while (s.readChunk(all::add) && all.size < f.getSampleRate() * ch * 20) { }
			float[] mono = new float[all.size / ch];
			for (int i = 0; i < mono.length; i++) {
				float sum = 0;
				for (int c = 0; c < ch; c++) sum += all.data[i * ch + c];
				mono[i] = sum / ch;
			}
			return new Pcm(mono, f.getSampleRate());
		}
	}

	private static Pcm wav(byte[] raw) throws Exception {
		try (AudioInputStream in0 = AudioSystem.getAudioInputStream(new BufferedInputStream(new java.io.ByteArrayInputStream(raw)))) {
			AudioFormat src = in0.getFormat();
			AudioFormat pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, src.getSampleRate(), 16, src.getChannels(), src.getChannels() * 2, src.getSampleRate(), false);
			try (AudioInputStream in = AudioSystem.getAudioInputStream(pcm, in0)) {
				ByteArrayOutputStream bo = new ByteArrayOutputStream();
				in.transferTo(bo);
				byte[] b = bo.toByteArray();
				int ch = pcm.getChannels();
				int frames = b.length / (2 * ch);
				float[] mono = new float[frames];
				ByteBuffer bb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
				for (int i = 0; i < frames; i++) {
					float sum = 0;
					for (int c = 0; c < ch; c++) sum += bb.getShort((i * ch + c) * 2);
					mono[i] = sum / ch / 32768f;
				}
				return new Pcm(mono, pcm.getSampleRate());
			}
		}
	}

	private static final class FloatList {
		float[] data = new float[1 << 16];
		int size;

		void add(float v) {
			if (size == data.length) data = java.util.Arrays.copyOf(data, size * 2);
			data[size++] = v;
		}

		float[] toArray() {
			return java.util.Arrays.copyOf(data, size);
		}
	}
}
