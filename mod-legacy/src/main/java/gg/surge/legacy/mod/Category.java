package gg.surge.legacy.mod;

public enum Category {
	ALL("All"), HUD("HUD"), PVP("PvP"), VISUAL("Visual"), UTILITY("Utility"), PERFORMANCE("Performance"), COSMETICS("Cosmetics");

	public final String label;

	Category(String label) {
		this.label = label;
	}
}
