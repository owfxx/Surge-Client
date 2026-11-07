package gg.surge.client.mods;

import gg.surge.client.config.SurgeConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class SurgeMod {
	public enum Category {
		HUD("HUD"), PVP("PvP"), VISUAL("Visual"), UTILITY("Utility");
		public final String label;
		Category(String label) { this.label = label; }
	}

	public final String id;
	public final String name;
	public final String description;
	public final Category category;
	private final Item icon;
	private ItemStack iconStack;
	protected final boolean defEnabled;
	protected float defX, defY;
	public final List<Setting<?>> settings = new ArrayList<>();
	private Consumer<Boolean> onToggle = on -> { };
	private Runnable onChange = () -> { };
	public boolean toggleable = true;

	public SurgeMod(String id, String name, Category category, Item icon, String description, boolean defEnabled) {
		this.id = id;
		this.name = name;
		this.category = category;
		this.icon = icon;
		this.description = description;
		this.defEnabled = defEnabled || "ping".equals(id) ? DEFAULT_ON.contains(id) : false;
	}

	private static final java.util.Set<String> DEFAULT_ON = java.util.Set.of("fps", "ping", "general", "cosmetics", "main_menu", "discord", "profiles");

	public ItemStack icon() {
		if (iconStack == null) iconStack = new ItemStack(icon);
		return iconStack;
	}

	public SurgeConfig.ModuleState state() {
		return SurgeConfig.get().module(id, defEnabled, defX, defY);
	}

	public boolean enabled() {
		return !toggleable || state().enabled;
	}

	public void setEnabled(boolean on) {
		if (!toggleable || state().enabled == on) return;
		state().enabled = on;
		SurgeConfig.save();
		onToggle.accept(on);
	}

	public void toggle() {
		setEnabled(!state().enabled);
	}

	public SurgeMod onToggle(Consumer<Boolean> c) {
		this.onToggle = c;
		return this;
	}

	public SurgeMod onChange(Runnable r) {
		this.onChange = r;
		return this;
	}

	void changed() {
		onChange.run();
	}

	public void resetSettings() {
		state().opts.clear();
		SurgeConfig.save();
		changed();
	}

	public <S extends Setting<?>> S add(S s) {
		settings.add(s);
		return s;
	}

	public Setting.Toggle toggle(String key, String name, boolean def) {
		return add(new Setting.Toggle(this, key, name, def));
	}

	public Setting.Slider slider(String key, String name, float def, float min, float max, float step, String suffix) {
		return add(new Setting.Slider(this, key, name, def, min, max, step, suffix));
	}

	public Setting.Choice choice(String key, String name, String def, String... values) {
		return add(new Setting.Choice(this, key, name, def, values));
	}

	public Setting.Color color(String key, String name, String def) {
		return add(new Setting.Color(this, key, name, def));
	}

	public Setting.Button button(String name, String label, Runnable action) {
		return add(new Setting.Button(this, name, label, action));
	}

	public Setting.Header header(String name) {
		return add(new Setting.Header(this, name));
	}

	public List<Setting<?>> pageSettings() {
		return settings;
	}

	public boolean hasSettings() {
		return !settings.isEmpty();
	}
}
