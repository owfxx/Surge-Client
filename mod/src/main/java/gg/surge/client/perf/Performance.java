package gg.surge.client.perf;

import gg.surge.client.config.SurgeConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class Performance {
	public static volatile boolean hideFoliage;
	public static volatile boolean cullEntities = true;
	public static volatile boolean occlusion = true;
	public static volatile boolean cullBlockEntities = true;
	public static volatile boolean cullParticles = true;

	public static volatile double ITEM_DISTANCE = 40, ORB_DISTANCE = 32, DECOR_DISTANCE = 56;
	public static volatile double BLOCK_ENTITY_DISTANCE = 64, PARTICLE_DISTANCE = 32, NAMETAG_DISTANCE = 48;
	public static volatile boolean hideFarNametags = true;

	public record Change(Function<Options, OptionInstance<?>> option, Object fast) {}

	public record Feature(String key, String name, String description, boolean invisible, boolean needsReload, Item icon, List<Change> changes) {
		public boolean defaultOn() { return invisible; }
	}

	public static final List<Feature> FEATURES = List.of(
		new Feature("cullEntities", "Entity Culling", "Skip far-away dropped items, XP orbs, arrows, armor stands and item frames", true, false, Items.ARMOR_STAND, List.of()),
		new Feature("occlusion", "Hidden Mob Culling", "Skip mobs and items hidden behind walls. Players are always drawn", true, false, Items.STONE_BRICKS, List.of()),
		new Feature("cullBlockEntities", "Block Entity Culling", "Skip chests, signs, banners and heads more than 64 blocks away", true, false, Items.CHEST, List.of()),
		new Feature("cullParticles", "Particle Culling", "Skip particles more than 32 blocks away", true, false, Items.FIREWORK_STAR, List.of()),
		new Feature("farNametags", "Far Nametag Culling", "Skip drawing nametags of players far away (lots of FPS on busy servers)", true, false, Items.NAME_TAG, List.of()),
		new Feature("unlimitedFps", "No VSync", "Turn off VSync so your FPS isn't locked to your monitor", true, false, Items.REDSTONE_TORCH,
			List.of(new Change(Options::enableVsync, false))),
		new Feature("afkSaver", "AFK Power Saver", "Lower FPS while the game is in the background or you're AFK", true, false, Items.RED_BED,
			List.of(new Change(Options::inactivityFpsLimit, "AFK"))),
		new Feature("lightBiomeBlend", "Light Biome Blend", "Faster chunk loading; grass and water colors still blend", true, true, Items.GRASS_BLOCK,
			List.of(new Change(Options::biomeBlendRadius, 1))),
		new Feature("lightWeather", "Light Weather", "Rain and snow drawn a little closer to you", true, false, Items.WATER_BUCKET,
			List.of(new Change(Options::weatherRadius, 7))),
		new Feature("fastLeaves", "Fast Leaves", "Solid leaves instead of see-through ones", false, true, Items.OAK_LEAVES,
			List.of(new Change(Options::cutoutLeaves, false))),
		new Feature("fastTransparency", "Fast Transparency", "Turn off improved transparency", false, true, Items.GLASS,
			List.of(new Change(Options::improvedTransparency, false))),
		new Feature("noShadows", "No Entity Shadows", "Skip the round shadows under mobs and items", false, false, Items.COAL,
			List.of(new Change(Options::entityShadows, false))),
		new Feature("fewerParticles", "Fewer Particles", "Use the decreased particle setting", false, false, Items.GUNPOWDER,
			List.of(new Change(Options::particles, "DECREASED"))),
		new Feature("instantChunks", "Instant Chunks", "No fade-in when chunks appear", false, false, Items.STONE,
			List.of(new Change(Options::chunkSectionFadeInTime, 0.0))),
		new Feature("fastClouds", "Fast Clouds", "Flat clouds drawn at a shorter range", false, false, Items.WHITE_WOOL,
			List.of(new Change(Options::cloudStatus, "FAST"), new Change(Options::cloudRange, 64))),
		new Feature("noBiomeBlend", "No Biome Blend", "Fastest chunk loading, sharp color edges between biomes", false, true, Items.DIRT,
			List.of(new Change(Options::biomeBlendRadius, 0))),
		new Feature("noSmoothLighting", "No Smooth Lighting", "Flat lighting, a big boost on weaker PCs", false, true, Items.TORCH,
			List.of(new Change(Options::ambientOcclusion, false))),
		new Feature("lowMipmaps", "Lower Mipmaps", "Less texture smoothing far away", false, true, Items.PAINTING,
			List.of(new Change(Options::mipmapLevels, 2))),
		new Feature("hideFoliage", "Hide Foliage", "Don't draw grass, flowers and bushes", false, true, Items.SHORT_GRASS, List.of()),
		new Feature("shortEntityDistance", "Short Entity Distance", "Draw mobs and players a bit closer", false, false, Items.ZOMBIE_HEAD,
			List.of(new Change(Options::entityDistanceScaling, 0.75))),
		new Feature("noClouds", "No Clouds", "Hide clouds completely", false, false, Items.GRAY_WOOL,
			List.of(new Change(Options::cloudStatus, "OFF")))
	);

	private static volatile Set<Block> foliage = Collections.emptySet();
	private static final String[] FOLIAGE_IDS = {
		"short_grass", "tall_grass", "fern", "large_fern", "dead_bush", "bush", "short_dry_grass", "tall_dry_grass",
		"firefly_bush", "leaf_litter", "pink_petals", "wildflowers", "sunflower", "lilac", "rose_bush", "peony",
		"dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip", "white_tulip",
		"pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley", "torchflower", "wither_rose",
		"open_eyeblossom", "closed_eyeblossom", "cactus_flower", "seagrass", "tall_seagrass", "nether_sprouts",
		"crimson_roots", "warped_roots", "hanging_roots", "glow_lichen", "pale_hanging_moss", "small_dripleaf"
	};

	private Performance() {}

	public static boolean isFoliage(Block block) {
		return foliage.contains(block);
	}

	public static boolean enabled(Feature f) {
		return SurgeConfig.get().boost.getOrDefault(f.key(), f.defaultOn());
	}

	public static void set(Minecraft mc, Feature f, boolean on) {
		SurgeConfig.get().boost.put(f.key(), on);
		apply(mc);
	}

	private static Object serialize(Object v) {
		return v instanceof Enum<?> e ? e.name() : v;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Object convert(OptionInstance option, Object v) {
		Object cur = option.get();
		if (cur instanceof Enum<?> e) return Enum.valueOf(e.getDeclaringClass(), String.valueOf(v));
		if (cur instanceof Integer) return ((Number) v).intValue();
		if (cur instanceof Double) return ((Number) v).doubleValue();
		if (cur instanceof Boolean) return v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v));
		return v;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static boolean write(OptionInstance option, Object v) {
		try {
			Object value = convert(option, v);
			if (value.equals(option.get())) return false;
			option.set(value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static void apply(Minecraft mc) {
		SurgeConfig c = SurgeConfig.get();
		Options o = mc.options;
		Map<String, Object> saved = c.saved;
		boolean reload = false;
		if (c.boostVersion < 2) { c.boost.clear(); c.boostVersion = 2; }

		for (int i = FEATURES.size() - 1; i >= 0; i--) {
			Feature f = FEATURES.get(i);
			if (enabled(f)) continue;
			for (int j = 0; j < f.changes().size(); j++) {
				String key = f.key() + "#" + j;
				if (saved.containsKey(key) && write(f.changes().get(j).option().apply(o), saved.remove(key))) reload |= f.needsReload();
			}
		}
		for (Feature f : FEATURES) {
			if (!enabled(f)) continue;
			for (int j = 0; j < f.changes().size(); j++) {
				Change ch = f.changes().get(j);
				OptionInstance<?> opt = ch.option().apply(o);
				String key = f.key() + "#" + j;
				if (!saved.containsKey(key)) saved.put(key, serialize(opt.get()));
				if (write(opt, ch.fast())) reload |= f.needsReload();
			}
		}

		cullEntities = enabled(byKey("cullEntities"));
		occlusion = enabled(byKey("occlusion"));
		cullBlockEntities = enabled(byKey("cullBlockEntities"));
		cullParticles = enabled(byKey("cullParticles"));
		hideFarNametags = enabled(byKey("farNametags"));
		applyDistances();
		if (foliage.isEmpty()) foliage = buildFoliageSet();
		boolean foliageOn = enabled(byKey("hideFoliage"));
		if (foliageOn != hideFoliage) { hideFoliage = foliageOn; reload = true; }

		o.save();
		SurgeConfig.save();
		if (reload && mc.level != null && mc.levelRenderer != null) mc.levelRenderer.allChanged();
	}

	public static void applyDistances() {
		SurgeConfig c = SurgeConfig.get();
		ITEM_DISTANCE = c.cullItems;
		ORB_DISTANCE = Math.max(16, c.cullItems * 0.8);
		DECOR_DISTANCE = c.cullItems * 1.4;
		BLOCK_ENTITY_DISTANCE = c.cullBlockEntities;
		PARTICLE_DISTANCE = c.cullParticles;
		NAMETAG_DISTANCE = c.nametagDistance;
	}

	public static Feature byKey(String key) {
		for (Feature f : FEATURES) if (f.key().equals(key)) return f;
		throw new IllegalArgumentException(key);
	}

	public enum Preset { MAX_FPS, BALANCED, QUALITY }

	public static void preset(Minecraft mc, Preset p) {
		Map<String, Boolean> b = SurgeConfig.get().boost;
		for (Feature f : FEATURES) {
			b.put(f.key(), switch (p) {
				case MAX_FPS -> true;
				case BALANCED -> f.defaultOn();
				case QUALITY -> false;
			});
		}
		if (p == Preset.MAX_FPS) b.put("lightBiomeBlend", false);
		if (p == Preset.MAX_FPS) {
			Options o = mc.options;
			if (o.renderDistance().get() > 12) o.renderDistance().set(12);
			if (o.simulationDistance().get() > 8) o.simulationDistance().set(8);
		}
		apply(mc);
	}

	public static Preset currentPreset() {
		boolean all = true, none = true, defaults = true;
		for (Feature f : FEATURES) {
			boolean on = enabled(f);
			if (!f.key().equals("lightBiomeBlend")) all &= on;
			none &= !on;
			defaults &= on == f.defaultOn() || f.key().equals("lightBiomeBlend");
		}
		return all ? Preset.MAX_FPS : none ? Preset.QUALITY : defaults ? Preset.BALANCED : null;
	}

	private static Set<Block> buildFoliageSet() {
		Set<Block> set = Collections.newSetFromMap(new IdentityHashMap<>());
		for (String id : FOLIAGE_IDS) {
			Block b = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("minecraft", id));
			if (b != Blocks.AIR) set.add(b);
		}
		return set;
	}
}
