package com.rickmorty.plugin.models;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

/**
 * Мост к ItemsAdder (опциональный бэкенд моделей предметов).
 *
 * <p>ItemsAdder позволяет регистрировать предметы из ресурспака
 * ({@code /iaeditor}) и выдавать их командой {@code /i give namespace:id}.
 * Мы не тянем API-классы в compile-time: если ItemsAdder установлен,
 * выдача предметов идёт через его консольную команду, а модели мобов
 * остаются на ModelEngine/VanillaCMD.</p>
 */
public final class ItemsAdderBridge implements ModelProvider {

    private final RickAndMortyPlugin plugin;
    private final boolean available;

    public ItemsAdderBridge(RickAndMortyPlugin plugin) {
        this.plugin = plugin;
        this.available = plugin.getServer().getPluginManager().getPlugin("ItemsAdder") != null;
        if (available) {
            plugin.getLogger().info("ItemsAdder обнаружен — модели предметов доступны через custom_model_data.");
        }
    }

    @Override public String name() { return "ItemsAdder"; }
    @Override public boolean isAvailable() { return available; }

    @Override
    public void applyToEntity(Entity entity, ModelId id) {
        // ItemsAdder не рендерит живых мобов; делегируем vanilla-маркировке
        new VanillaItemModelProvider(plugin).applyToEntity(entity, id);
    }

    @Override
    public ItemStack applyToItem(ItemStack item, ModelId id, int customModelDataValue) {
        // Для совместимости сOverrides-моделями ItemsAdder тоже используем CMD
        VanillaItemModelProvider.applyCmd(item, customModelDataValue);
        return item;
    }

    @Override
    public void release(Entity entity) { /* no-op */ }
}
