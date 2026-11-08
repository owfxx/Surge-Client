package gg.surge.legacy.mod;

import gg.surge.legacy.Config;

public abstract class Setting<T> {
	public final Mod mod;
	public final String key, name;
	protected final T def;
	private java.util.function.BooleanSupplier visible;

	protected Setting(Mod mod, String key, String name, T def) {
		this.mod = mod;
		this.key = key;
		this.name = name;
		this.def = def;
	}

	@SuppressWarnings("unchecked")
	public <S extends Setting<T>> S showIf(java.util.function.BooleanSupplier cond) {
		this.visible = cond;
		return (S) this;
	}

	public boolean visible() {
		return visible == null || visible.getAsBoolean();
	}

	protected String raw() {
		return Config.mod(mod.id).values.get(key);
	}

	protected void store(String v) {
		Config.mod(mod.id).values.put(key, v);
		Config.markDirty();
		mod.changed();
	}

	public void reset() {
		Config.mod(mod.id).values.remove(key);
		Config.markDirty();
		mod.changed();
	}

	public abstract T get();

	public static final class Toggle extends Setting<Boolean> {
		public Toggle(Mod mod, String key, String name, boolean def) { super(mod, key, name, def); }

		@Override public Boolean get() { String r = raw(); return r == null ? def : Boolean.parseBoolean(r); }

		public boolean on() { return get(); }

		public void set(boolean v) { store(String.valueOf(v)); }

		public void flip() { set(!on()); }
	}

	public static final class Slider extends Setting<Float> {
		public final float min, max, step;
		public final String suffix;

		public Slider(Mod mod, String key, String name, float def, float min, float max, float step, String suffix) {
			super(mod, key, name, def);
			this.min = min;
			this.max = max;
			this.step = step;
			this.suffix = suffix;
		}

		@Override public Float get() {
			try { String r = raw(); return r == null ? def : Math.max(min, Math.min(max, Float.parseFloat(r))); } catch (Exception e) { return def; }
		}

		public int asInt() { return Math.round(get()); }

		public void set(float v) {
			v = Math.max(min, Math.min(max, Math.round(v / step) * step));
			store(String.valueOf(v));
		}

		public float fraction() { return max == min ? 0 : (get() - min) / (max - min); }

		public void setFraction(float f) { set(min + (max - min) * Math.max(0, Math.min(1, f))); }

		public String format() {
			float v = get();
			String s = step >= 1 ? String.valueOf(Math.round(v)) : step >= 0.1f ? String.format("%.1f", v) : String.format("%.2f", v);
			return s + suffix;
		}
	}

	public static final class Choice extends Setting<String> {
		public final String[] values;

		public Choice(Mod mod, String key, String name, String def, String... values) {
			super(mod, key, name, def);
			this.values = values;
		}

		@Override public String get() {
			String r = raw();
			if (r != null) for (String v : values) if (v.equals(r)) return v;
			return def;
		}

		public boolean is(String v) { return get().equals(v); }

		public void cycle(int dir) {
			int i = 0;
			for (int k = 0; k < values.length; k++) if (values[k].equals(get())) i = k;
			store(values[(i + dir + values.length) % values.length]);
		}
	}

	public static final class Color extends Setting<String> {
		public Color(Mod mod, String key, String name, String def) { super(mod, key, name, def); }

		@Override public String get() { String r = raw(); return r == null ? def : r; }

		public boolean chroma() { return "chroma".equals(get()); }

		public void set(String v) { store(v); }

		public int rgb(float offset) {
			if (chroma()) return java.awt.Color.HSBtoRGB(((System.currentTimeMillis() % 4000) / 4000f + offset) % 1f, 0.6f, 1f) | 0xFF000000;
			try { return 0xFF000000 | Integer.parseInt(get(), 16); } catch (Exception e) { return 0xFFFFFFFF; }
		}

		public int argb(int alpha) {
			return (rgb(0) & 0xFFFFFF) | (alpha << 24);
		}
	}

	public static final class Button extends Setting<Void> {
		public final String label;
		public final Runnable action;

		public Button(Mod mod, String name, String label, Runnable action) {
			super(mod, "", name, null);
			this.label = label;
			this.action = action;
		}

		@Override public Void get() { return null; }
	}
}
