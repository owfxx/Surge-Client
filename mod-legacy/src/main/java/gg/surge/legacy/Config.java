package gg.surge.legacy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class Config {
	public static final class ModState {
		public Boolean enabled;
		public float x = -1, y = -1, scale = 1f;
		public Map<String, String> values = new HashMap<String, String>();
	}

	public static final class Data {
		public Map<String, ModState> mods = new HashMap<String, ModState>();
		public int defaults;
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Data data;
	private static boolean dirty;
	private static long lastSave;

	private Config() {}

	public static File dir() {
		File d = new File(System.getProperty("user.home"), ".surgeclient");
		d.mkdirs();
		return d;
	}

	private static File file() {
		return new File(dir(), "legacy.json");
	}

	public static Data get() {
		if (data == null) {
			try (Reader r = new InputStreamReader(new FileInputStream(file()), StandardCharsets.UTF_8)) {
				data = GSON.fromJson(r, Data.class);
			} catch (Exception e) {
				data = null;
			}
			if (data == null) data = new Data();
			if (data.mods == null) data.mods = new HashMap<String, ModState>();
		}
		return data;
	}

	public static ModState mod(String id) {
		ModState s = get().mods.get(id);
		if (s == null) {
			s = new ModState();
			get().mods.put(id, s);
		}
		if (s.values == null) s.values = new HashMap<String, String>();
		return s;
	}

	public static void markDirty() {
		dirty = true;
	}

	public static void tick() {
		if (dirty && System.currentTimeMillis() - lastSave > 1500) save();
	}

	public static void save() {
		dirty = false;
		lastSave = System.currentTimeMillis();
		File f = file(), tmp = new File(f.getPath() + ".tmp");
		try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
			GSON.toJson(get(), w);
		} catch (Exception e) {
			return;
		}
		f.delete();
		tmp.renameTo(f);
	}
}
