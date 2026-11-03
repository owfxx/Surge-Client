package gg.surge.client.mods;

import gg.surge.client.config.SurgeConfig;

import java.util.Map;
import java.util.function.BooleanSupplier;

public abstract class Setting<T> {
	public final SurgeMod mod;
	public final String key;
	public final String name;
	public final T def;
	private BooleanSupplier visible = () -> true;

	protected Setting(SurgeMod mod, String key, String name, T def) {
		this.mod = mod;
		this.key = key;
		this.name = name;
		this.def = def;
	}

	@SuppressWarnings("unchecked")
	public <S extends Setting<T>> S showIf(BooleanSupplier cond) {
		this.visible = cond;
		return (S) this;
	}

	public boolean visible() {
		return visible.getAsBoolean();
	}

	protected Map<String, Object> opts() {
		return mod.state().opts;
	}

	protected Object raw() {
		return opts().get(key);
	}

	public abstract T get();

	public void set(T value) {
		opts().put(key, value);
		SurgeConfig.markDirty();
		mod.changed();
	}

	public void reset() {
		opts().remove(key);
		SurgeConfig.save();
		mod.changed();
	}

	public boolean stored() {
		return true;
	}

	public static class Toggle extends Setting<Boolean> {
		public Toggle(SurgeMod mod, String key, String name, boolean def) { super(mod, key, name, def); }

		@Override public Boolean get() { return raw() instanceof Boolean b ? b : def; }

		public boolean on() { return get(); }

		public void flip() { set(!get()); }
	}

	public static class Slider extends Setting<Float> {
		public final float min, max, step;
		public final String suffix;

		public Slider(SurgeMod mod, String key, String name, float def, float min, float max, float step, String suffix) {
			super(mod, key, name, def);
			this.min = min;
			this.max = max;
			this.step = step;
			this.suffix = suffix;
		}

		@Override public Float get() {
			float v = raw() instanceof Number n ? n.floatValue() : def;
			return Math.max(min, Math.min(max, v));
		}

		public int asInt() { return Math.round(get()); }

		public float snap(float v) {
			v = Math.max(min, Math.min(max, v));
			v = min + Math.round((v - min) / step) * step;
			return Math.round(v * 1000f) / 1000f;
		}

		public float fraction() { return max == min ? 0 : (get() - min) / (max - min); }

		public void setFraction(float f) {
			float v = snap(min + f * (max - min));
			if (v != get()) set(v);
		}

		public String format() {
			float v = get();
			String num = step >= 1 ? String.valueOf(Math.round(v)) : step >= 0.1f ? String.format("%.1f", v) : String.format("%.2f", v);
			return num + suffix;
		}
	}

	public static class Choice extends Setting<String> {
		public final String[] values;
		public java.util.function.Function<String, String> label = v -> v;

		public Choice(SurgeMod mod, String key, String name, String def, String... values) {
			super(mod, key, name, def);
			this.values = values;
		}

		@Override public String get() {
			Object r = raw();
			if (r instanceof String s) for (String v : values) if (v.equals(s)) return s;
			return def;
		}

		public boolean is(String v) { return get().equals(v); }

		public void cycle(int dir) {
			int i = 0;
			for (int k = 0; k < values.length; k++) if (values[k].equals(get())) i = k;
			set(values[(i + dir + values.length) % values.length]);
		}
	}

	public static class Color extends Setting<String> {
		public static final String[] PALETTE = {"FFFFFF", "AAAAAA", "555555", "000000", "FF5555", "FFAA00", "FFFF55", "55FF55",
			"55FFFF", "5599FF", "B483FF", "FF77DD", "chroma"};

		public Color(SurgeMod mod, String key, String name, String def) { super(mod, key, name, def); }

		@Override public String get() { return raw() instanceof String s ? s : def; }

		public boolean chroma() { return "chroma".equals(get()); }

		public int rgb(float offset) {
			return chroma() ? Colors.chroma(offset) : Colors.parse(get());
		}

		public int argb(int alpha) {
			return (alpha << 24) | (rgb(0) & 0xFFFFFF);
		}
	}

	public static class Button extends Setting<Void> {
		public final String label;
		public final Runnable action;

		public Button(SurgeMod mod, String name, String label, Runnable action) {
			super(mod, "", name, null);
			this.label = label;
			this.action = action;
		}

		@Override public Void get() { return null; }

		@Override public boolean stored() { return false; }
	}

	public static class Header extends Setting<Void> {
		public Header(SurgeMod mod, String name) { super(mod, "", name, null); }

		@Override public Void get() { return null; }

		@Override public boolean stored() { return false; }
	}
}
