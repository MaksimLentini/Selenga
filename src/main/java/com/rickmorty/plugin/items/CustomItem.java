package com.rickmorty.plugin.items;

/**
 * Кастомные предметы плагина. id совпадает с ключом в config.yml (shop.rick.*)
 * и аргументом /rick give.
 */
public enum CustomItem {
    PORTAL_GUN("portal_gun", "Портальная пушка"),
    PLUMBUS("plumbus", "Плюмбус"),
    MEGA_SEEDS("mega_seeds", "Мега-семена"),
    RICK_FLASK("rick_flask", "Фляга Рика");

    private final String id;
    private final String displayName;

    CustomItem(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }

    public static CustomItem fromString(String s) {
        if (s == null) return null;
        for (CustomItem ci : values()) if (ci.id.equalsIgnoreCase(s)) return ci;
        // алиасы
        return switch (s.toLowerCase(java.util.Locale.ROOT)) {
            case "gun", "portalgun" -> PORTAL_GUN;
            case "seeds" -> MEGA_SEEDS;
            case "flask" -> RICK_FLASK;
            default -> null;
        };
    }
}
