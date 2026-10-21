package com.rickmorty.plugin.utils;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Отправка ресурспака игрокам.
 *
 * <p>Используется vanilla API {@code Player#setResourcePack(String url, String hash)}
 * (доступен с 1.8+, хэш — SHA1 архива в hex). На Paper дополнительно
 * вызывается перегрузка с параметром force через рефлексию, чтобы не
 * ломать сборку под Spigot.</p>
 *
 * <p>Альтернативный способ без кода — прописать в server.properties:
 * {@code resource-pack=<URL>} и {@code resource-pack-sha1=<HASH>}.</p>
 */
public final class ResourcePackUtil {

    private ResourcePackUtil() {}

    /**
     * Отправляет ресурспак одному игроку согласно конфигу.
     *
     * @param plugin экземпляр плагина
     * @param player целевой игрок
     */
    public static void sendPack(RickAndMortyPlugin plugin, Player player) {
        var cfg = plugin.getPluginConfig();
        if (!cfg.rpEnabled()) return;
        try {
            String url = cfg.rpUrl();
            String hash = cfg.rpHash();
            boolean ok;
            if (hash == null || hash.isEmpty()) {
                ok = player.setResourcePack(url);
            } else {
                // Spigot/Paper: setResourcePack(String url, String hash)
                ok = player.setResourcePack(url, hash);
            }
            // Paper-специфичная перегрузка setResourcePack(url, hash, message, force)
            if (ok && cfg.rpForce()) {
                tryPaperForce(player, url, hash, cfg.rpMessage());
            } else if (ok && !cfg.rpMessage().isEmpty()) {
                MessageUtil.sendRaw(player, cfg.rpMessage());
            }
            MessageUtil.debug(plugin, "Ресурспак отправлен: " + player.getName() + " ok=" + ok);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING,
                    "Не удалось отправить ресурспак игроку " + player.getName(), ex);
        }
    }

    /** Пробует вызвать Paper-перегрузку с force-флагом через рефлексию. */
    private static void tryPaperForce(Player player, String url, String hash, String prompt) {
        try {
            Method m = player.getClass().getMethod(
                    "setResourcePack", String.class, String.class, String.class, boolean.class);
            m.invoke(player, url, hash, prompt, true);
        } catch (ReflectiveOperationException ignored) {
            // Spigot без Paper API — оставляем обычный запрос пакета
            if (!prompt.isEmpty()) MessageUtil.sendRaw(player, prompt);
        }
    }

    /** Рассылает пакет всем онлайн-игрокам (при старте/релоаде). */
    public static void broadcastPack(RickAndMortyPlugin plugin) {
        if (!plugin.getPluginConfig().rpEnabled()) {
            MessageUtil.debug(plugin, "Ресурспак отключён или URL пуст — пропускаем рассылку");
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            sendPack(plugin, p);
        }
    }
}
