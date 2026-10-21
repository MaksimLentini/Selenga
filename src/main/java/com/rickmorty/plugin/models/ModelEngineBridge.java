package com.rickmorty.plugin.models;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Мост к ModelEngine через рефлексию.
 *
 * <p>Плагин компилируется без jar-а ModelEngine (он проприетарный и
 * раздаётся с сайта TiledMedia). В рантайме, если плагин ModelEngine
 * установлен, мы находим:</p>
 * <pre>
 *   com.ticiox.modelengine.api.ModelEngineAPI  (v4+)
 *     -> getModelManager() : IModelManager
 *        IModelManager.getKnownModels() : Map&lt;String, ImmutableModel&gt;
 *           ImmutableModel.spawnLivingEntity(Location, Set&lt;Trait&gt;) : LivingEntity
 *           ImmutableModel.playAnimation(LivingEntity, String)
 * </pre>
 *
 * <p>Эквивалент прямого кода (для документации):</p>
 * <pre>{@code
 * IModelManager registry = ModelEngine.getInstance().getModelRegistry();
 * ImmutableModel rick = registry.getKnownModels().get("rickandmorty:rick");
 * LivingEntity modelEntity = rick.spawnLivingEntity(loc, EnumSet.of(Trait.MOVEMENT));
 * rick.playAnimation(modelEntity, "walk");
 * }</pre>
 */
public final class ModelEngineBridge implements ModelProvider {

    private final RickAndMortyPlugin plugin;
    private Object modelManager;                 // IModelManager
    private Method getKnownModels;               // -> Map<String, ImmutableModel>
    private Method spawnLivingEntity;            // ImmutableModel#spawnLivingEntity(Location, Set)
    private Method playAnimation;                // ImmutableModel#playAnimation(LivingEntity, String)
    private boolean available;

    /** Сущность-модель ModelEngine, привязанная к vanilla-носителю. */
    private final Map<java.util.UUID, Object> boundModels = new HashMap<>();

    public ModelEngineBridge(RickAndMortyPlugin plugin) {
        this.plugin = plugin;
        probe();
    }

    /** Пытается разрешить классы и методы ModelEngine. Не бросает исключений. */
    private void probe() {
        try {
            if (plugin.getServer().getPluginManager().getPlugin("ModelEngine") == null) return;
            Class<?> api = Class.forName("com.ticiox.modelengine.api.ModelEngineAPI");
            Object instance = api.getMethod("getModelManager").invoke(null);
            if (instance == null) return;
            this.modelManager = instance;
            this.getKnownModels = instance.getClass().getMethod("getKnownModels");
            Class<?> modelClass = Class.forName("com.ticiox.modelengine.api.models.buffers.ImmutableModel");
            this.spawnLivingEntity = modelClass.getMethod("spawnLivingEntity",
                    org.bukkit.Location.class, java.util.Set.class);
            this.playAnimation = modelClass.getMethod("playAnimation",
                    org.bukkit.entity.LivingEntity.class, String.class);
            this.available = true;
            plugin.getLogger().info("ModelEngine обнаружен — 3D-модели мобов активны.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("ModelEngine не установлен — используется резерв рендеринга.");
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING,
                    "ModelEngine найден, но API не совпал (обновите ModelEngine до v4+).", ex);
        }
    }

    @Override public String name() { return "ModelEngine"; }
    @Override public boolean isAvailable() { return available; }

    @Override
    public void applyToEntity(Entity entity, ModelId id) {
        if (!available) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> models = (Map<String, Object>) getKnownModels.invoke(modelManager);
            Object model = models.get(id.fullId());
            if (model == null) {
                MessageUtil.warn(plugin, "Модель '" + id.fullId() + "' не найдена в ModelEngine. "
                        + "Проверьте ресурспак и plugins/ModelEngine/models/rickandmorty.yml");
                return;
            }
            java.util.Set<?> traits = resolveAllTraits();
            Object spawned = spawnLivingEntity.invoke(model, entity.getLocation(), traits);
            if (spawned != null) {
                boundModels.put(entity.getUniqueId(), spawned);
                // Прячем vanilla-тело носителя, чтобы не было "двойника"
                entity.setVisibleByDefault(false);
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Не удалось заспавнить модель " + id, ex);
        }
    }

    /** EnumSet.allOf(Trait.class) рефлексивно (класс Trait есть только при ME). */
    private java.util.Set<?> resolveAllTraits() {
        try {
            Class<?> trait = Class.forName("com.ticiox.modelengine.api.models.Trait");
            return java.util.EnumSet.allOf(asSubclass(trait));
        } catch (Exception ignored) {
            return java.util.Collections.emptySet();
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Enum> asSubclass(Class<?> c) {
        return (Class<? extends Enum>) c;
    }

    @Override
    public ItemStack applyToItem(ItemStack item, ModelId id, int cmd) {
        // Для предметов ModelEngine не обязателен — используем custom model data
        VanillaItemModelProvider.applyCmd(item, cmd);
        return item;
    }

    @Override
    public void release(Entity entity) {
        boundModels.remove(entity.getUniqueId());
    }

    @Override
    public void playAnimation(Entity entity, Animation anim) {
        if (!available) return;
        Object spawned = boundModels.get(entity.getUniqueId());
        if (spawned == null || !(spawned instanceof org.bukkit.entity.LivingEntity le)) return;
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> models = (Map<String, Object>) getKnownModels.invoke(modelManager);
            // Ищем модель по имени модели-носителя
            for (Object m : models.values()) {
                try { playAnimation.invoke(m, le, anim.key()); } catch (Exception ignored) {}
            }
        } catch (Exception ex) {
            MessageUtil.debug(plugin, "Анимация " + anim.key() + " не проиграна: " + ex.getMessage());
        }
    }
}
