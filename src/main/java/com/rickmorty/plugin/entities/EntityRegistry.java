package com.rickmorty.plugin.entities;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import com.rickmorty.plugin.models.Animation;
import com.rickmorty.plugin.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Реестр персонажей: спавн NPC/мобов, привязка моделей, поведение.
 *
 * <p>Все сущности плагина помечаются PDC-ключом {@code rickandmorty:character}
 * со значением {@link CharacterType#pdcValue()} — по нему листенеры
 * определяют "свой" моб или нет.</p>
 */
public final class EntityRegistry {

    private final RickAndMortyPlugin plugin;
    private final NamespacedKey keyChar;
    private final NamespacedKey keyOwner; // для Морти-компаньона: UUID игрока

    /** Активные заспавненные персонажи (для корректного despawnAll). */
    private final List<UUID> tracked = new ArrayList<>();
    private BukkitTask followTask;

    public EntityRegistry(RickAndMortyPlugin plugin) {
        this.plugin = plugin;
        this.keyChar = new NamespacedKey(plugin, "character");
        this.keyOwner = new NamespacedKey(plugin, "owner");
        startFollowTask();
    }

    /** Идентификатор типа персонажа по сущности (null если не нашёлся). */
    public CharacterType typeOf(Entity e) {
        String v = e.getPersistentDataContainer().get(keyChar, PersistentDataType.STRING);
        if (v == null) return null;
        for (CharacterType t : CharacterType.values()) if (t.pdcValue().equals(v)) return t;
        return null;
    }

    public boolean isPluginEntity(Entity e) { return typeOf(e) != null; }

    /**
     * Спавнит персонажа указанного типа у локации.
     *
     * @return заспавненная сущность или null при ошибке
     */
    public LivingEntity spawn(CharacterType type, Location loc, Player owner) {
        try {
            LivingEntity ent = switch (type) {
                case RICK -> spawnRick(loc);
                case MORTY -> spawnMorty(loc, owner);
                case MR_MEESEEKS -> spawnMeeseeks(loc);
                case PICKLE_RICK -> spawnPickleRick(loc);
                case SNOW_BALL -> spawnSnowBall(loc, owner);
            };
            ent.getPersistentDataContainer().set(keyChar, PersistentDataType.STRING, type.pdcValue());
            if (owner != null && type == CharacterType.MORTY) {
                ent.getPersistentDataContainer().set(keyOwner, PersistentDataType.STRING,
                        owner.getUniqueId().toString());
            }
            plugin.getModelRegistry().applyToEntity(ent, type.modelId());
            plugin.getModelRegistry().playAnimation(ent, Animation.IDLE);
            tracked.add(ent.getUniqueId());
            MessageUtil.debug(plugin, "Заспавнен " + type + " в " + loc.getWorld().getName());
            return ent;
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка спавна " + type, ex);
            return null;
        }
    }

    // ---------- Конкретные персонажи ----------

    /** Рик — торговец: носитель Villager, без AI-блуждания, неуязвим. */
    private LivingEntity spawnRick(Location loc) {
        Villager v = loc.getWorld().spawn(loc, Villager.class);
        v.setCustomName(MessageUtil.color(com.rickmorty.plugin.entities.CharacterType.RICK.displayName()));
        v.setCustomNameVisible(true);
        v.setAI(false);            // стоит на месте как NPC
        v.setSilent(false);
        v.setInvulnerable(true);
        v.setProfession(Villager.Profession.NITWIT);
        v.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, -1, 0, false, false));
        // Инвиз-носитель: реальную модель отдаёт ресурспак/ModelEngine
        return v;
    }

    /** Морти — компаньон: носитель Wolf, приручён владельцу, следует за ним. */
    private LivingEntity spawnMorty(Location loc, Player owner) {
        Wolf w = loc.getWorld().spawn(loc, Wolf.class);
        w.setCustomName(MessageUtil.color(CharacterType.MORTY.displayName()));
        w.setCustomNameVisible(true);
        if (owner != null) {
            w.setOwner(owner);
            w.setTamed(true);
        }
        w.setInvulnerable(false);
        w.setMaxHealth(20);
        return w;
    }

    /** Мистер Мисикс — враждебный моб: носитель Zombie-Villager. */
    private LivingEntity spawnMeeseeks(Location loc) {
        Zombie z = loc.getWorld().spawn(loc, Zombie.class);
        z.setVillagerZombie(true);
        z.setCustomName(MessageUtil.color(CharacterType.MR_MEESEEKS.displayName()));
        z.setCustomNameVisible(true);
        z.setShouldDespawn(false);
        z.setBaby(false);
        z.setAI(true);
        z.setTarget(findNearestPlayer(loc, 30));
        return z;
    }

    /** Пикаль Рик — мини-босс: носитель Spider, усиленный. */
    private LivingEntity spawnPickleRick(Location loc) {
        Spider s = loc.getWorld().spawn(loc, Spider.class);
        s.setCustomName(MessageUtil.color(CharacterType.PICKLE_RICK.displayName()));
        s.setCustomNameVisible(true);
        s.setShouldDespawn(false);
        s.setMaxHealth(60);
        s.setHealth(60);
        s.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1, false, false));
        s.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, -1, 1, false, false));
        s.setTarget(findNearestPlayer(loc, 40));
        return s;
    }

    /** Снежный шар / Сквинчи — питомец: носитель Parrot, сидит на плече. */
    private LivingEntity spawnSnowBall(Location loc, Player owner) {
        Parrot p = loc.getWorld().spawn(loc, Parrot.class);
        p.setCustomName(MessageUtil.color(CharacterType.SNOW_BALL.displayName()));
        p.setCustomNameVisible(true);
        p.setShouldDespawn(false);
        if (owner != null) {
            p.setOwner(owner.getUniqueId());
            // "Езда" на плече: привязка через чанкер позиции в followTask
            p.getPersistentDataContainer().set(keyOwner, PersistentDataType.STRING,
                    owner.getUniqueId().toString());
        }
        p.setVariant(org.bukkit.entity.Parrot.Variant.BLUE);
        return p;
    }

    // ---------- Поведение ----------

    /** Таск: Морти следует за владельцем, Снежный шар сидит на плече. */
    private void startFollowTask() {
        followTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity e : world.getEntities()) {
                        if (!(e instanceof LivingEntity le)) continue;
                        CharacterType t = typeOf(le);
                        if (t == null) continue;
                        String ownerId = le.getPersistentDataContainer().get(keyOwner, PersistentDataType.STRING);
                        if (ownerId == null) continue;
                        Player owner = Bukkit.getPlayer(UUID.fromString(ownerId));
                        if (owner == null || !owner.isOnline()) continue;

                        if (t == CharacterType.MORTY) {
                            teleportIfFar(le, owner, 25);
                            if (le.getLocation().distanceSquared(owner.getLocation()) > 9) {
                                Vector dir = owner.getLocation().toVector()
                                        .subtract(le.getLocation().toVector()).normalize().multiply(0.25);
                                le.setVelocity(dir);
                                plugin.getModelRegistry().playAnimation(le, Animation.WALK);
                            }
                        } else if (t == CharacterType.SNOW_BALL) {
                            Location shoulder = owner.getEyeLocation().add(0, 0.2, 0);
                            if (le.getLocation().distanceSquared(shoulder) > 1.0) {
                                le.teleport(shoulder);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Ошибка follow-task", ex);
            }
        }, 20L, 10L);
    }

    private void teleportIfFar(LivingEntity pet, Player owner, double blocks) {
        if (!pet.getWorld().equals(owner.getWorld())
                || pet.getLocation().distanceSquared(owner.getLocation()) > blocks * blocks) {
            pet.teleport(owner.getLocation().add(1, 0, 1));
        }
    }

    private LivingEntity findNearestPlayer(Location loc, double radius) {
        Entity nearest = null;
        double best = radius * radius;
        for (Player p : loc.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(loc);
            if (d < best && !p.getGameMode().equals(org.bukkit.GameMode.CREATIVE)) {
                best = d; nearest = p;
            }
        }
        return nearest instanceof LivingEntity l ? l : null;
    }

    // ---------- Натуральный спавн по биомам/времени ----------

    /**
     * Вызывается планировщиком каждые N секунд: шанс заспавнить моба
     * рядом со случайным игроком в зависимости от биома и времени суток.
     */
    public void naturalSpawnTick() {
        var cfg = plugin.getPluginConfig();
        for (World world : Bukkit.getWorlds()) {
            long count = world.getEntities().stream().filter(this::isPluginEntity).count();
            if (count >= cfg.spawnMaxPerWorld()) continue;
            for (Player p : world.getPlayers()) {
                Location at = randomNearby(p, 24, 40);
                if (at == null) continue;
                Biome biome = at.getBlock().getBiome();
                long time = world.getTime();
                boolean day = time < 12000 || time > 23000;

                if (day && isPlainsLike(biome) && roll(cfg.chanceMeeseeks())) {
                    spawn(CharacterType.MR_MEESEEKS, at, null);
                } else if (!day && biome == Biome.SWAMP && roll(cfg.chancePickle())) {
                    spawn(CharacterType.PICKLE_RICK, at, null);
                } else if ((biome == Biome.SNOWY_PLAINS || biome == Biome.FROZEN_OCEAN)
                        && roll(cfg.chanceSnowBall())) {
                    spawn(CharacterType.SNOW_BALL, at, p);
                }
                break; // один моб на мир за тик
            }
        }
    }

    private boolean isPlainsLike(Biome b) {
        return b == Biome.PLAINS || b == Biome.SUNFLOWER_PLAINS || b == Biome.FOREST
                || b == Biome.BIRCH_FOREST || b == Biome.DESERT;
    }

    private boolean roll(double chance) {
        return plugin.getServer().getRandom().nextDouble() < chance;
    }

    private Location randomNearby(Player p, int minR, int maxR) {
        World w = p.getWorld();
        for (int i = 0; i < 8; i++) {
            double angle = plugin.getServer().getRandom().nextDouble() * Math.PI * 2;
            double r = minR + plugin.getServer().getRandom().nextInt(maxR - minR);
            Location loc = p.getLocation().clone()
                    .add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
            loc.setY(w.getHighestBlockYAt(loc) + 1);
            if (loc.getBlock().isEmpty() && loc.clone().subtract(0, 1, 0).getBlock().isSolid()) {
                return loc;
            }
        }
        return null;
    }

    /** Убирает всех персонажей плагина (при выключении/reload). */
    public void despawnAll() {
        try {
            if (followTask != null) followTask.cancel();
            for (World w : Bukkit.getWorlds()) {
                for (Entity e : new ArrayList<>(w.getEntities())) {
                    if (isPluginEntity(e)) {
                        plugin.getModelRegistry().release(e);
                        e.remove();
                    }
                }
            }
            tracked.clear();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Ошибка despawnAll", ex);
        }
    }
}
