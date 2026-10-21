package com.rickmorty.plugin.models;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Реестр 3D-моделей плагина.
 *
 * <p>Выбирает активный бэкенд по приоритету:
 * ModelEngine → ItemsAdder → VanillaCMD. Хранит соответствие
 * {@link ModelId} → числовое значение custom_model_data для предметов.</p>
 *
 * <p>Использование:</p>
 * <pre>{@code
 * ModelRegistry reg = plugin.getModelRegistry();
 * reg.applyToEntity(villagerNpc, ModelId.RICK);          // привязка модели моба
 * ItemStack gun = reg.applyToItem(stack, ModelId.PORTAL_GUN); // модель предмета
 * reg.playAnimation(npc, Animation.WALK);                 // анимация
 * }</pre>
 */
public final class ModelRegistry {

    private final RickAndMortyPlugin plugin;
    private ModelProvider active;

    /** Стабильные CMD-значения: не менять без пересборки ресурспака. */
    private static final Map<ModelId, Integer> CMD_VALUES = new LinkedHashMap<>();
    static {
        int base = 1000;
        for (ModelId id : ModelId.values()) CMD_VALUES.put(id, base++);
    }

    public ModelRegistry(RickAndMortyPlugin plugin) {
        this.plugin = plugin;
    }

    /** Определяет доступный бэкенд. Вызывается при старте и reload. */
    public void init() {
        try {
            ModelEngineBridge me = new ModelEngineBridge(plugin);
            if (me.isAvailable()) { active = me; return; }

            ItemsAdderBridge ia = new ItemsAdderBridge(plugin);
            if (ia.isAvailable()) { active = ia; return; }

            active = new VanillaItemModelProvider(plugin);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка инициализации ModelRegistry", ex);
            active = new VanillaItemModelProvider(plugin);
        }
        plugin.getLogger().info("Активный бэкенд моделей: " + active.name());
    }

    public ModelProvider getActive() { return active; }
    public String getActiveBackend() { return active == null ? "none" : active.name(); }

    public void applyToEntity(Entity entity, ModelId id) {
        try { active.applyToEntity(entity, id); }
        catch (Exception e) { plugin.getLogger().log(Level.WARNING, "applyToEntity " + id, e); }
    }

    public ItemStack applyToItem(ItemStack item, ModelId id) {
        try { return active.applyToItem(item, id, CMD_VALUES.getOrDefault(id, 999)); }
        catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "applyToItem " + id, e);
            return item;
        }
    }

    public void playAnimation(Entity entity, Animation anim) {
        try { active.playAnimation(entity, anim); }
        catch (Exception e) {
            com.rickmorty.plugin.utils.MessageUtil.debug(plugin, "animation error: " + e);
        }
    }

    public void release(Entity entity) {
        try { active.release(entity); } catch (Exception ignored) {}
    }
}
