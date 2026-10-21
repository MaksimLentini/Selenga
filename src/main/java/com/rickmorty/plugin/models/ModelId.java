package com.rickmorty.plugin.models;

/**
 * Идентификаторы всех 3D-моделей плагина.
 *
 * <p>Каждый id соответствует модели в ресурспаке по пути
 * {@code assets/rickandmorty/modelengine/models/<id>.json} (ModelEngine)
 * или {@code assets/rickandmorty/models/item/<id>.json} (vanilla/OF/CIT).</p>
 */
public enum ModelId {
    RICK("rick", "Рик Санчес"),
    MORTY("morty", "Морти Смит"),
    MR_MEESEEKS("mr_meeseeks", "Мистер Мисикс"),
    PICKLE_RICK("pickle_rick", "Пикаль Рик"),
    SNOW_BALL("snow_ball", "Снежный шар (Сквинчи)"),
    PORTAL_GUN("portal_gun", "Портальная пушка"),
    PLUMBUS("plumbus", "Плюмбус"),
    MEGA_SEEDS("mega_seeds", "Мега-семена"),
    RICK_FLASK("rick_flask", "Фляга Рика"),
    SHMEKLI("shmekli", "Шмекль (валюта)");

    private final String path;
    private final String displayName;

    ModelId(String path, String displayName) {
        this.path = path;
        this.displayName = displayName;
    }

    /** Путь модели внутри ресурспака без расширения. */
    public String path() { return path; }
    public String displayName() { return displayName; }

    /** Полный id для ModelEngine: rickandmorty:<path> */
    public String fullId() { return "rickandmorty:" + path; }
}
