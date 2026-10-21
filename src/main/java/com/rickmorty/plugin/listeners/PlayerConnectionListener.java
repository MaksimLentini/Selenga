package com.rickmorty.plugin.listeners;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import com.rickmorty.plugin.utils.MessageUtil;
import com.rickmorty.plugin.utils.ResourcePackUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Листенер соединения игроков.
 *
 * <p>При входе (PlayerJoinEvent):</p>
 * <ol>
 *   <li>инициализируется баланс шмеклей (getBalance() при первом обращении);</li>
 *   <li>с задержкой 40 тиков отправляется ресурспак (config resource_pack.url),
 *       чтобы клиент успел войти в мир — иначе пакет иногда игнорируется.</li>
 * </ol>
 */
public final class PlayerConnectionListener implements Listener {

    private final RickAndMortyPlugin plugin;

    public PlayerConnectionListener(RickAndMortyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        try {
            Player p = e.getPlayer();
            // Инициализация экономики (ленивая выдача starting_balance)
            int bal = plugin.getEconomyManager().getBalance(p);
            MessageUtil.send(p, plugin, "&fДобро пожаловать! У вас "
                    + plugin.getEconomyManager().format(bal) + ". &7/rick help");

            if (plugin.getPluginConfig().rpEnabled() && plugin.getPluginConfig().rpOnJoin()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            ResourcePackUtil.sendPack(plugin, p);
                        } catch (Exception ex) {
                            plugin.getLogger().warning("RP send failed for " + p.getName() + ": " + ex);
                        }
                    }
                }.runTaskLater(plugin, 40L);
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("PlayerJoin error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        // Порталы игрока остаются в PDC (переживают перезаход);
        // чистим только in-memory кулдауны листенера — они в Map, ок.
        MessageUtil.debug(plugin, e.getPlayer().getName() + " вышел. Морти скучает.");
    }
}
