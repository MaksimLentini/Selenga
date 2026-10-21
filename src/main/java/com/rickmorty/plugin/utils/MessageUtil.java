package com.rickmorty.plugin.utils;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * Утилиты сообщений: цветные &-коды, префикс плагина, логирование.
 */
public final class MessageUtil {

    private MessageUtil() {}

    /** Транслирует &-коды в цвета ChatColor. */
    public static String color(String s) {
        return s == null ? "" : ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Отправляет сообщение с префиксом плагина. */
    public static void send(CommandSender to, RickAndMortyPlugin plugin, String msg) {
        to.sendMessage(color(plugin.getPluginConfig().prefix() + msg));
    }

    /** Сообщение без префикса. */
    public static void sendRaw(CommandSender to, String msg) {
        to.sendMessage(color(msg));
    }

    /** Отладочное сообщение в консоль (только при debug=true). */
    public static void debug(RickAndMortyPlugin plugin, String msg) {
        if (plugin.getPluginConfig().debug()) {
            plugin.getLogger().info("[DEBUG] " + msg);
        }
    }

    /** Информационное сообщение в консоль сервера. */
    public static void broadcastConsole(RickAndMortyPlugin plugin, String msg) {
        plugin.getLogger().info(color(msg).replaceAll("(?i)§.", ""));
    }

    /** Предупреждение в консоль (например, отсутствующая модель). */
    public static void warn(RickAndMortyPlugin plugin, String msg) {
        plugin.getLogger().warning(msg);
    }
}
