package com.rickmorty.plugin.entities;

/**
 * Типы персонажей плагина. Каждый привязан к ModelId и vanilla-носителю.
 */
public enum CharacterType {
    /** Торговец-NPC (носитель — Villager, профессиональный лайфак). */
    RICK("rick", "§bРик Санчес"),
    /** Компаньон, следует за игроком (носитель — Wolf, приручённый). */
    MORTY("morty", "§aМорти Смит"),
    /** Враждебный моб (носитель — Zombie с кастомным AI-таргетингом). */
    MR_MEESEEKS("mr_meeseeks", "§eМистер Мисикс"),
    /** Мини-босс (носитель — Spider, быстрый и ядовитый). */
    PICKLE_RICK("pickle_rick", "§5Пикаль Рик"),
    /** Питомец (носитель — Rabbit / Parrot в 1.20). */
    SNOW_BALL("snow_ball", "§fСнежный шар");

    private final String pdcValue;
    private final String displayName;

    CharacterType(String pdcValue, String displayName) {
        this.pdcValue = pdcValue;
        this.displayName = displayName;
    }

    public String pdcValue() { return pdcValue; }
    public String displayName() { return displayName; }

    public com.rickmorty.plugin.models.ModelId modelId() {
        return switch (this) {
            case RICK -> com.rickmorty.plugin.models.ModelId.RICK;
            case MORTY -> com.rickmorty.plugin.models.ModelId.MORTY;
            case MR_MEESEEKS -> com.rickmorty.plugin.models.ModelId.MR_MEESEEKS;
            case PICKLE_RICK -> com.rickmorty.plugin.models.ModelId.PICKLE_RICK;
            case SNOW_BALL -> com.rickmorty.plugin.models.ModelId.SNOW_BALL;
        };
    }

    /** Разбор аргумента команды /rick spawn. */
    public static CharacterType fromString(String s) {
        if (s == null) return null;
        return switch (s.toLowerCase(java.util.Locale.ROOT)) {
            case "rick" -> RICK;
            case "morty" -> MORTY;
            case "meeseeks", "mr_meeseeks", "мисикс" -> MR_MEESEEKS;
            case "pickle", "pickle_rick" -> PICKLE_RICK;
            case "snowball", "snow_ball", "squanchy", "сквинчи" -> SNOW_BALL;
            default -> null;
        };
    }
}
