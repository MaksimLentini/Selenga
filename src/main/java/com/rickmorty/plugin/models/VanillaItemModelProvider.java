package com.rickmorty.plugin.models;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Резервный бэкенд моделей: vanilla custom_model_data (1.20.x).
 *
 * <p>Модели предметов подменяются через ресурспак по значениям
 * {@code customModelData} (Overrides-файлы в {@code models/item/*.json}).
 * Мобы отображаются vanilla-носителем (например, Villager для Рика) с
 * кастомным скином/именем — полноценного entity-рендеринга без
 * ModelEngine/ProtocolLib нет, это известное ограничение.</p>
 */
public final class VanillaItemModelProvider implements ModelProvider {

    private final RickAndMortyPlugin plugin;

    public VanillaItemModelProvider(RickAndMortyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String name() { return "VanillaCMD"; }
    @Override public boolean isAvailable() { return true; }

    /** Пишет custom model data в item meta (работает на Spigot 1.14+). */
    static void applyCmd(ItemStack item, int value) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(value);
            item.setItemMeta(meta);
        }
    }

    @Override
    public void applyToEntity(Entity entity, ModelId id) {
        // Помечаем сущность PDC-ключом модели — листенеры и ME-мост могут
        // подхватить метку позже, если ModelEngine появится на сервере.
        entity.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "model_id"),
                PersistentDataType.STRING, id.fullId());
    }

    @Override
    public ItemStack applyToItem(ItemStack item, ModelId id, int cmd) {
        applyCmd(item, cmd);
        return item;
    }

    @Override
    public void release(Entity entity) { /* no-op */ }

    @Override
    public void playAnimation(Entity entity, Animation anim) { /* no-op: нет ME */ }
}
