package com.rickmorty.plugin.items;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Логика Портальной пушки: точки A/B, пары порталов, телепорт.
 *
 * <p>Координаты порталов хранятся в {@link PersistentDataContainer}
 * игрока (переживают перезаход):</p>
 * <ul>
 *   <li>{@code rickandmorty:portal_a} — "world;x;y;z;yaw;pitch"</li>
 *   <li>{@code rickandmorty:portal_b} — аналогично</li>
 * </ul>
 *
 * <p>Порталы — зоны-маркеры радиуса ~1.2 блока вокруг точки установки.
 * Таск раз в тик проверяет, не вошёл ли игрок в зону портала, и
 * телепортирует к другому концу пары.</p>
 */
public final class PortalGunManager {

    private static final double PORTAL_RADIUS = 1.5;

    private final RickAndMortyPlugin plugin;
    private final NamespacedKey keyA;
    private final NamespacedKey keyB;

    /** Активная пара порталов одного игрока. */
    public record Pair(UUID owner, Location a, Location b, long createdAt) {}

    private final Map<UUID, Pair> pairsByOwner = new HashMap<>();
    /** uid игрока -> время последнего телепорта (антифлуд). */
    private final Map<UUID, Long> teleportCooldown = new HashMap<>();

    public PortalGunManager(RickAndMortyPlugin plugin) {
        this.plugin = plugin;
        this.keyA = new NamespacedKey(plugin, "portal_a");
        this.keyB = new NamespacedKey(plugin, "portal_b");
        startTickTask();
    }

    // ---------- Serialization ----------

    private static String ser(Location l) {
        return l.getWorld().getName() + ";" + l.getX() + ";" + l.getY() + ";" + l.getZ()
                + ";" + l.getYaw() + ";" + l.getPitch();
    }

    private static Location deser(String s) {
        try {
            String[] p = s.split(";");
            World w = Bukkit.getWorld(p[0]);
            if (w == null) return null;
            return new Location(w, Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                    Double.parseDouble(p[3]), Float.parseFloat(p[4]), Float.parseFloat(p[5]));
        } catch (Exception e) {
            return null;
        }
    }

    public Location getPortalA(Player p) {
        return readLocation(p.getPersistentDataContainer(), keyA);
    }

    public Location getPortalB(Player p) {
        return readLocation(p.getPersistentDataContainer(), keyB);
    }

    private Location readLocation(PersistentDataContainer cdc, NamespacedKey key) {
        String v = cdc.get(key, PersistentDataType.STRING);
        return v == null ? null : deser(v);
    }

    /**
     * Ставит портал на месте ray-trace попадания.
     *
     * @param slot 0 = портал A (ЛКМ), 1 = портал B (ПКМ)
     * @return true если успешно сохранён в PDC
     */
    public boolean placePortal(Player p, int slot, Location at) {
        try {
            var cdc = p.getPersistentDataContainer();
            cdc.set(slot == 0 ? keyA : keyB, PersistentDataType.STRING, ser(at));

            Location a = readLocation(cdc, keyA);
            Location b = readLocation(cdc, keyB);
            if (a != null && b != null) {
                pairsByOwner.put(p.getUniqueId(), new Pair(p.getUniqueId(), a, b, System.currentTimeMillis()));
            }
            spawnPortalEffect(at);
            return true;
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "placePortal failed", ex);
            return false;
        }
    }

    /** Визуальные партиклы портала. */
    public void spawnPortalEffect(Location at) {
        try {
            Location center = at.clone().add(0.5, 1.0, 0.5);
            center.getWorld().spawnParticle(Particle.PORTAL, center, 60, 0.25, 0.75, 0.25, 0.05, null, true);
            center.getWorld().spawnParticle(Particle.END_ROD, center, 20, 0.2, 0.6, 0.2, 0.01);
        } catch (Exception ignored) {
            // Разные версии по-разному называют партиклы — тихо пропускаем
        }
    }

    /** Возвращает пару игрока или null. */
    public Pair getPair(Player p) {
        Pair cached = pairsByOwner.get(p.getUniqueId());
        if (cached != null) return cached;
        // Восстановление из PDC после перезахода
        Location a = getPortalA(p);
        Location b = getPortalB(p);
        if (a != null && b != null) {
            Pair np = new Pair(p.getUniqueId(), a, b, System.currentTimeMillis());
            pairsByOwner.put(p.getUniqueId(), np);
            return np;
        }
        return null;
    }

    /**
     * Если игрок стоит в зоне портала — возвращает локацию другого конца,
     * иначе null. Учитывает кулдаун телепорта.
     */
    public Location checkTeleport(Player p) {
        long now = System.currentTimeMillis();
        Long last = teleportCooldown.get(p.getUniqueId());
        if (last != null && now - last < 1200) return null;

        Pair pair = getPair(p);
        if (pair == null || pair.a().getWorld() == null || pair.b().getWorld() == null) return null;

        Location from = null;
        Location to = null;
        if (inside(p.getLocation(), pair.a())) { from = pair.a(); to = pair.b(); }
        else if (inside(p.getLocation(), pair.b())) { from = pair.b(); to = pair.a(); }
        if (from == null) return null;

        teleportCooldown.put(p.getUniqueId(), now);
        Location dest = to.clone().add(0.5, 1.2, 0.5);
        dest.setYaw(p.getLocation().getYaw());
        dest.setPitch(p.getLocation().getPitch());
        return dest;
    }

    private boolean inside(Location playerLoc, Location portal) {
        if (playerLoc.getWorld() != portal.getWorld()) return false;
        Location center = portal.clone().add(0.5, 1.0, 0.5);
        return playerLoc.distance(center) <= PORTAL_RADIUS;
    }

    /** Тик: проверка входа в порталы + протухание пар. */
    private void startTickTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                int life = plugin.getPluginConfig().gunPairLifetimeSec();
                long now = System.currentTimeMillis();
                if (life > 0) {
                    pairsByOwner.entrySet().removeIf(e -> now - e.getValue().createdAt() > life * 1000L);
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Location dest = checkTeleport(p);
                    if (dest != null) {
                        int delay = plugin.getPluginConfig().gunTeleportDelayTicks();
                        p.getWorld().spawnParticle(Particle.PORTAL, p.getLocation(), 30, 0.3, 0.5, 0.3, 0.1);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            try {
                                p.teleport(dest);
                                dest.getWorld().spawnParticle(Particle.PORTAL, dest, 30, 0.3, 0.5, 0.3, 0.1);
                            } catch (Exception ex) {
                                plugin.getLogger().log(Level.WARNING, "teleport fail", ex);
                            }
                        }, delay);
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Portal tick error", ex);
            }
        }, 1L, Math.max(1, plugin.getPluginConfig().gunTeleportDelayTicks()));
    }

    /** Убирает порталы игрока (заклинание/сломанная пушка). */
    public void clearPortals(Player p) {
        pairsByOwner.remove(p.getUniqueId());
        p.getPersistentDataContainer().remove(keyA, keyB);
    }

    /** Все активные пары (для листенеров/отладки). */
    public java.util.Collection<Pair> activePairs() {
        return new ArrayList<>(pairsByOwner.values());
    }
}
