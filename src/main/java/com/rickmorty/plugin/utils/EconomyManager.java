package com.rickmorty.plugin.utils;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Валюта "Шмекли" на PersistentDataContainer игрока.
 *
 * <p>Баланс хранится прямо в данных игрока (key {@code rickandmorty:shmeklis}),
 * поэтому переживает перезаходы и не требует внешних YAML/SQL-хранилищ.
 * Последний бонус — {@code rickandmorty:last_bonus} (System.currentTimeMillis()).</p>
 */
public final class EconomyManager {

    private final RickAndMortyPlugin plugin;
    private final NamespacedKey keyBalance;
    private final NamespacedKey keyLastBonus;

    public EconomyManager(RickAndMortyPlugin plugin) {
        this.plugin = plugin;
        this.keyBalance = new NamespacedKey(plugin, "shmeklis");
        this.keyLastBonus = new NamespacedKey(plugin, "last_bonus");
    }

    private PersistentDataContainer pdc(Player p) {
        return p.getPersistentDataContainer();
    }

    /** Текущий баланс; при первом обращении выдаётся starting_balance. */
    public int getBalance(Player p) {
        Integer v = pdc(p).get(keyBalance, PersistentDataType.INTEGER);
        if (v == null) {
            v = plugin.getPluginConfig().startingBalance();
            setBalance(p, v);
        }
        return v;
    }

    public void setBalance(Player p, int amount) {
        pdc(p).set(keyBalance, PersistentDataType.INTEGER, Math.max(0, amount));
    }

    /** Начисляет shmeklis, возвращает новый баланс. */
    public int deposit(Player p, int shmeklis) {
        int nb = getBalance(p) + Math.max(0, shmeklis);
        setBalance(p, nb);
        return nb;
    }

    /** Списывает, если хватает. true = сделка прошла. */
    public boolean withdraw(Player p, int shmeklis) {
        int bal = getBalance(p);
        if (bal < shmeklis) return false;
        setBalance(p, bal - shmeklis);
        return true;
    }

    /** Выдаёт ежедневный бонус, если прошло >= 24ч с предыдущего. */
    public boolean claimDaily(Player p) {
        Long last = pdc(p).get(keyLastBonus, PersistentDataType.LONG);
        long now = System.currentTimeMillis();
        if (last != null && now - last < 24L * 3600_000L) return false;
        pdc(p).set(keyLastBonus, PersistentDataType.LONG, now);
        deposit(p, plugin.getPluginConfig().dailyBonus());
        return true;
    }

    /** Форматирует сумму: "12 Шмеклей". */
    public String format(int amount) {
        var cfg = plugin.getPluginConfig();
        String word = (amount % 10 == 1 && amount % 100 != 11)
                ? cfg.currencyName() : cfg.currencyPlural();
        return "&6" + amount + "&f " + word;
    }
}
