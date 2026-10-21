package com.rickmorty.plugin.core;

import com.rickmorty.plugin.commands.RickCommand;
import com.rickmorty.plugin.config.PluginConfig;
import com.rickmorty.plugin.entities.EntityRegistry;
import com.rickmorty.plugin.items.ItemRegistry;
import com.rickmorty.plugin.items.PortalGunManager;
import com.rickmorty.plugin.listeners.PortalGunListener;
import com.rickmorty.plugin.listeners.PlayerConnectionListener;
import com.rickmorty.plugin.listeners.ShopInteractionListener;
import com.rickmorty.plugin.models.ModelRegistry;
import com.rickmorty.plugin.utils.EconomyManager;
import com.rickmorty.plugin.utils.MessageUtil;
import com.rickmorty.plugin.utils.ResourcePackUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Главный класс плагина RickAndMortyPlugin.
 *
 * <p>Архитектура:</p>
 * <ul>
 *   <li>{@link PluginConfig} — типизированная обёртка над config.yml;</li>
 *   <li>{@link ModelRegistry} — абстрактный слой 3D-моделей (ModelEngine / ItemsAdder / vanilla);</li>
 *   <li>{@link EntityRegistry} — NPC и мобы (Рик, Морти, Мистер Мисикс, Пикаль Рик, Снежный шар);</li>
 *   <li>{@link ItemRegistry} — кастомные предметы (портальная пушка, плюмбус, мега-семена, фляга);</li>
 *   <li>{@link EconomyManager} — валюта "Шмекли" на PersistentDataContainer;</li>
 *   <li>Листенеры — Portal Gun, магазин, выдача ресурспака.</li>
 * </ul>
 *
 * <p>Совместимость: Paper/Spigot 1.20.x (api-version 1.20). Все опциональные
 * зависимости (ProtocolLib, ModelEngine, ItemsAdder) определяются в рантайме;
 * без них плагин деградирует до vanilla-представления сущностей.</p>
 */
public final class RickAndMortyPlugin extends JavaPlugin {

    private static RickAndMortyPlugin instance;

    private PluginConfig pluginConfig;
    private ModelRegistry modelRegistry;
    private EntityRegistry entityRegistry;
    private ItemRegistry itemRegistry;
    private EconomyManager economyManager;
    private PortalGunManager portalGunManager;
    private BukkitTask spawnTask;

    @Override
    public void onEnable() {
        instance = this;
        long t0 = System.currentTimeMillis();
        getLogger().info("Инициализация RickAndMortyPlugin v" + getDescription().getVersion() + "...");

        try {
            // 1. Конфиг
            saveDefaultConfig();
            pluginConfig = new PluginConfig(this);
            pluginConfig.load();

            // 2. Ядра сервисов
            economyManager = new EconomyManager(this);
            modelRegistry = new ModelRegistry(this);
            modelRegistry.init(); // определяет ModelEngine/ItemsAdder через рефлексию
            entityRegistry = new EntityRegistry(this);
            itemRegistry = new ItemRegistry(this);
            portalGunManager = new PortalGunManager(this);

            // 3. Команды
            RickCommand rickCommand = new RickCommand(this);
            Objects.requireNonNull(getCommand("rick"), "Команда 'rick' не найдена в plugin.yml")
                    .setExecutor(rickCommand);
            Objects.requireNonNull(getCommand("rick")).setTabCompleter(rickCommand);

            // 4. Листенеры
            var pm = Bukkit.getPluginManager();
            pm.registerEvents(new PortalGunListener(this), this);
            pm.registerEvents(new PlayerConnectionListener(this), this);
            pm.registerEvents(new ShopInteractionListener(this), this);

            // 5. Отправка ресурспака (конфиг resource_pack.url)
            ResourcePackUtil.broadcastPack(this);

            // 6. Натуральный спавн мобов по биомам/времени
            startNaturalSpawnTask();

            getSLF4JLogger().info("Модели: {}. ProtocolLib: {}. Время запуска: {} мс",
                    modelRegistry.getActiveBackend(),
                    pm.getPlugin("ProtocolLib") != null ? "да" : "нет",
                    System.currentTimeMillis() - t0);
            MessageUtil.broadcastConsole(this, "&aПлагин &bRick and Morty&a активирован. Wubba lubba dub dub!");
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Критическая ошибка при включении плагина — плагин отключается", ex);
            pmDisable();
        }
    }

    @Override
    public void onDisable() {
        try {
            if (spawnTask != null) spawnTask.cancel();
            if (entityRegistry != null) entityRegistry.despawnAll();
            getLogger().info("RickAndMortyPlugin выключен. Морти в шоке.");
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Ошибка при выключении плагина", ex);
        }
    }

    /**
     * Полная перезагрузка конфигурации и перерегистрация моделей.
     * Вызывается из /rick reload.
     */
    public void reloadPlugin() {
        reloadConfig();
        pluginConfig.load();
        modelRegistry.init();
        entityRegistry.despawnAll();
        if (spawnTask != null) spawnTask.cancel();
        startNaturalSpawnTask();
        ResourcePackUtil.broadcastPack(this);
    }

    private void startNaturalSpawnTask() {
        if (!pluginConfig.naturalSpawnEnabled()) return;
        long period = pluginConfig.spawnIntervalSeconds() * 20L;
        spawnTask = Bukkit.getScheduler().runTaskTimer(this,
                () -> {
                    try {
                        entityRegistry.naturalSpawnTick();
                    } catch (Exception e) {
                        getLogger().log(Level.WARNING, "Ошибка тика натурального спавна", e);
                    }
                }, period, period);
    }

    private void pmDisable() {
        Bukkit.getPluginManager().disablePlugin(this);
    }

    public static RickAndMortyPlugin getInstance() { return instance; }
    public PluginConfig getPluginConfig() { return pluginConfig; }
    public ModelRegistry getModelRegistry() { return modelRegistry; }
    public EntityRegistry getEntityRegistry() { return entityRegistry; }
    public ItemRegistry getItemRegistry() { return itemRegistry; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public PortalGunManager getPortalGunManager() { return portalGunManager; }
}
