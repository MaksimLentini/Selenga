package com.rickmorty.plugin.models;

/**
 * Стандартный набор анимаций моделей.
 *
 * <p>Каждая модель персонажа в ресурспаке должна содержать анимации
 * (файл {@code animations/<model>.json} в формате Minecraft Animation Format v7.0,
 * экспортируется из Blockbench):</p>
 * <ul>
 *   <li>{@link #IDLE}   — дыхание/покой (loop);</li>
 *   <li>{@link #WALK}   — ходьба (loop, запускается при движении);</li>
 *   <li>{@link #ATTACK} — удар (один проигрываемый кадр-цикл);</li>
 *   <li>{@link #DEATH}  — смерть (один раз, затем hide/skin).</li>
 * </ul>
 */
public enum Animation {
    IDLE("idle"),
    WALK("walk"),
    ATTACK("attack"),
    DEATH("death");

    private final String key;

    Animation(String key) { this.key = key; }

    /** Имя анимации, совпадающее с "animations.<key>" в JSON-файле анимаций. */
    public String key() { return key; }
}
