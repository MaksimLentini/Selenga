package com.rickmorty.plugin.models;

import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

/**
 * Абстрактный слой 3D-моделей ("ModelProvider").
 *
 * <p>Плагин не зависит от конкретной библиотеки рендеринга: реализация
 * выбирается в {@link ModelRegistry} по принципу "что установлено":
 * ModelEngine → ItemsAdder → VanillaFallback (шлем-атрибут / OF CID).</p>
 *
 * <p>Пример привязки к ModelEngine API (см. также {@link ModelEngineBridge}):</p>
 * <pre>{@code
 * // Прямой код с API ModelEngine 4 (если добавить jar в classpath):
 * IModelManager mm = ModelEngine.getInstance().getModelRegistry();
 * ImmutableModel model = mm.getKnownModels().get(ModelKey.of("rickandmorty:rick"));
 * LivingEntity m = model.spawnLivingEntity(spawnLocation, EnumSet.allOf(Trait.class));
 * m.playAnimation("walk");
 * }</pre>
 */
public interface ModelProvider {

    /** Человекочитаемое имя бэкенда для логов. */
    String name();

    /** Готов ли бэкенд рендерить модели (библиотека установлена и загружена). */
    boolean isAvailable();

    /**
     * Привязывает модель к уже существующей сущности (NPC/моб).
     * Для ModelEngine это может означать спавн entity-модели рядом и
     * скрытие vanilla-носителя.
     *
     * @param entity целевая сущность
     * @param id     идентификатор модели
     */
    void applyToEntity(Entity entity, ModelId id);

    /**
     * Применяет кастомную модель предмета к ItemStack
     * (через NBT/components — custom_model_data).
     */
    ItemStack applyToItem(ItemStack item, ModelId id, int customModelDataValue);

    /** Останавливает анимации/скрывает модели при удалении сущности. */
    void release(Entity entity);

    /** Воспроизвести анимацию на сущности, если бэкенд это поддерживает. */
    default void playAnimation(Entity entity, Animation anim) {}
}
