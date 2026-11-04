package gg.surge.client.features;

public final class Flags {
	public static final String[] CODES = {"DE", "AT", "CH", "NL", "BE", "FR", "IT", "ES", "PT", "GB", "IE", "PL", "CZ", "SE", "NO", "DK", "FI", "RU", "UA", "TR", "US", "CA", "MX", "BR", "AR", "JP", "KR", "CN", "IN", "AU", "NZ", "ZA", "GR", "HU", "RO", "LT", "EE", "LV"};
	public static final String[] NAMES = {"Germany", "Austria", "Switzerland", "Netherlands", "Belgium", "France", "Italy", "Spain", "Portugal", "United Kingdom", "Ireland", "Poland", "Czechia", "Sweden", "Norway", "Denmark", "Finland", "Russia", "Ukraine", "Türkiye", "United States", "Canada", "Mexico", "Brazil", "Argentina", "Japan", "South Korea", "China", "India", "Australia", "New Zealand", "South Africa", "Greece", "Hungary", "Romania", "Lithuania", "Estonia", "Latvia"};

	public static final net.minecraft.network.chat.FontDescription ICONS = new net.minecraft.network.chat.FontDescription.Resource(net.minecraft.resources.Identifier.fromNamespaceAndPath("surge", "icons"));

	private Flags() {}

	public static net.minecraft.network.chat.MutableComponent icon(String glyph) {
		return net.minecraft.network.chat.Component.literal(glyph).withStyle(s -> s.withColor(0xFFFFFF).withFont(ICONS));
	}

	public static String glyph(String code) {
		for (int i = 0; i < CODES.length; i++) if (CODES[i].equalsIgnoreCase(code)) return String.valueOf((char) (0xE100 + i));
		return "";
	}

	public static String name(String code) {
		for (int i = 0; i < CODES.length; i++) if (CODES[i].equalsIgnoreCase(code)) return NAMES[i];
		return code;
	}
}
