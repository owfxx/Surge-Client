package gg.surge.legacy.mod;

import gg.surge.legacy.Config;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Mod {
	public final String id, name, description;
	public final Category category;
	public final ItemStack icon;
	public final boolean defEnabled;
	public boolean toggleable = true;
	public final List<Setting<?>> settings = new ArrayList<Setting<?>>();
	private Runnable onChange;

	public Mod(String id, String name, Category category, ItemStack icon, String description, boolean defEnabled) {
		this.id = id;
		this.name = name;
		this.category = category;
		this.icon = icon;
		this.description = description;
		this.defEnabled = defEnabled;
	}

	public boolean enabled() {
		if (!toggleable) return true;
		Boolean b = Config.mod(id).enabled;
		return b == null ? defEnabled : b;
	}

	public void setEnabled(boolean on) {
		Config.mod(id).enabled = on;
		Config.markDirty();
		changed();
	}

	public void toggle() {
		setEnabled(!enabled());
	}

	public Mod onChange(Runnable r) {
		this.onChange = r;
		return this;
	}

	void changed() {
		if (onChange != null) try { onChange.run(); } catch (Throwable ignored) { }
	}

	protected <S extends Setting<?>> S add(S s) {
		settings.add(s);
		return s;
	}

	public Setting.Toggle toggle(String key, String name, boolean def) { return add(new Setting.Toggle(this, key, name, def)); }

	public Setting.Slider slider(String key, String name, float def, float min, float max, float step, String suffix) { return add(new Setting.Slider(this, key, name, def, min, max, step, suffix)); }

	public Setting.Choice choice(String key, String name, String def, String... values) { return add(new Setting.Choice(this, key, name, def, values)); }

	public Setting.Color color(String key, String name, String def) { return add(new Setting.Color(this, key, name, def)); }

	public Setting.Button button(String name, String label, Runnable action) { return add(new Setting.Button(this, name, label, action)); }
}
