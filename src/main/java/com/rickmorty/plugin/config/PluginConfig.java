package com.rickmorty.plugin.config;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Типизированная обёртка над config.yml.
 *
 * <p>Все обращения к конфигу идут через этот класс, чтобы избежать
 * {@code NullPointerException} при отсутствующих секциях: каждый геттер
 * имеет значение по умолчанию.</p>
 */
public final class PluginConfig {

    private final RickAndMortyPlugin plugin;

    // resource_pack
    private boolean rpEnabled;
    private String rpUrl = "";
    private String rpHash = "";
    private boolean rpOnJoin = true;
    private boolean rpForce = false;
    private String rpMessage = "";

    // economy
    private String currencyName = "Шмекль";
    private String currencyPlural = "Шмеклей";
    private int startingBalance = 50;
    private int dailyBonus = 20;
    private int killMeeseeks = 5;
    private int killPickle = 25;

    // shop prices
    private int pricePortalGun = 150;
    private int priceMegaSeeds = 40;
    private int priceFlask = 25;
    private int pricePlumbus = 10;
    private int priceSnowball = 80;

    // portal command
    private String portalMode = "CITADEL";
    private String citadelWorld = "Citadel";
    private int portalRandomOffset = 1000;

    // portal gun
    private int gunMaxDistance = 64;
    private int gunCooldownSec = 3;
    private int gunTeleportDelayTicks = 5;
    private int gunPairLifetimeSec = 120;

    // mega seeds
    private int seedsDuration = 60;
    private int seedsSpeed = 2;
    private int seedsStrength = 1;
    private int seedsCooldown = 180;

    // flask
    private List<PotionEffectType> flaskPool = new ArrayList<>();
    private int flaskMinDur = 10;
    private int flaskMaxDur = 45;

    // natural spawn
    private boolean spawnEnabled = true;
    private int spawnInterval = 30;
    private int spawnMaxPerWorld = 12;
    private double chanceMeeseeks = 0.35;
    private double chancePickle = 0.10;
    private double chanceSnowBall = 0.15;

    private boolean debug = false;
    private String prefix = "&8[&bRick&Morty&8]&r ";

    public PluginConfig(RickAndMortyPlugin plugin) {
        this.plugin = plugin;
    }

    /** Загружает все значения из config.yml со значениями по умолчанию. */
    public void load() {
        FileConfiguration c = plugin.getConfig();

        rpEnabled = c.getBoolean("resource_pack.enabled", true);
        rpUrl = c.getString("resource_pack.url", "");
        rpHash = c.getString("resource_pack.hash", "");
        rpOnJoin = c.getBoolean("resource_pack.request_on_join", true);
        rpForce = c.getBoolean("resource_pack.force", false);
        rpMessage = c.getString("resource_pack.message", "");

        currencyName = c.getString("economy.currency_name", "Шмекль");
        currencyPlural = c.getString("economy.currency_name_plural", "Шмеклей");
        startingBalance = c.getInt("economy.starting_balance", 50);
        dailyBonus = c.getInt("economy.daily_bonus", 20);
        killMeeseeks = c.getInt("economy.kill_reward.meeseeks", 5);
        killPickle = c.getInt("economy.kill_reward.pickle_rick", 25);

        pricePortalGun = c.getInt("shop.rick.portal_gun", 150);
        priceMegaSeeds = c.getInt("shop.rick.mega_seeds", 40);
        priceFlask = c.getInt("shop.rick.flask", 25);
        pricePlumbus = c.getInt("shop.rick.plumbus", 10);
        priceSnowball = c.getInt("shop.rick.snowball_pet", 80);

        portalMode = c.getString("portal.mode", "CITADEL").toUpperCase(Locale.ROOT);
        citadelWorld = c.getString("portal.world_name", "Citadel");
        portalRandomOffset = c.getInt("portal.random_offset", 1000);

        gunMaxDistance = c.getInt("portal_gun.max_distance", 64);
        gunCooldownSec = c.getInt("portal_gun.cooldown_seconds", 3);
        gunTeleportDelayTicks = c.getInt("portal_gun.teleport_delay_ticks", 5);
        gunPairLifetimeSec = c.getInt("portal_gun.pair_lifetime_seconds", 120);

        seedsDuration = c.getInt("mega_seeds.duration_seconds", 60);
        seedsSpeed = c.getInt("mega_seeds.speed_amplifier", 2);
        seedsStrength = c.getInt("mega_seeds.strength_amplifier", 1);
        seedsCooldown = c.getInt("mega_seeds.cooldown_seconds", 180);

        flaskMinDur = c.getInt("flask.min_duration_seconds", 10);
        flaskMaxDur = c.getInt("flask.max_duration_seconds", 45);
        flaskPool.clear();
        for (String name : c.getStringList("flask.effects_pool")) {
            PotionEffectType t = PotionEffectType.getByName(name.toUpperCase(Locale.ROOT));
            if (t != null) flaskPool.add(t);
            else plugin.getLogger().warning("Неизвестный эффект в flask.effects_pool: " + name);
        }
        if (flaskPool.isEmpty()) {
            PotionEffectType def = PotionEffectType.getByName("SPEED");
            if (def != null) flaskPool.add(def);
        }

        spawnEnabled = c.getBoolean("natural_spawn.enabled", true);
        spawnInterval = Math.max(1, c.getInt("natural_spawn.check_interval_seconds", 30));
        spawnMaxPerWorld = c.getInt("natural_spawn.max_entities_per_world", 12);
        chanceMeeseeks = c.getDouble("natural_spawn.chances.mr_meeseeks", 0.35);
        chancePickle = c.getDouble("natural_spawn.chances.pickle_rick", 0.10);
        chanceSnowBall = c.getDouble("natural_spawn.chances.snow_ball", 0.15);

        debug = c.getBoolean("debug", false);
        prefix = c.getString("prefix", "&8[&bRick&Morty&8]&r ");
    }

    /** Возвращает случайную реплику из секции dialogues.<key> или дефолт. */
    public String randomDialogue(String key, String player) {
        List<String> list = plugin.getConfig().getStringList("dialogues." + key);
        String raw;
        if (list.isEmpty()) {
            raw = switch (key) {
                case "rick_greeting" -> "&b[Rick] &fWubba lubba dub dub!";
                case "morty_greeting" -> "&a[Morty] &fGeez...";
                case "meeseeks_attack" -> "&e[Meeseeks] &fLook at me!";
                case "pickle_rick_greeting" -> "&5[Pickle Rick] &fI'm Pickle Rick!";
                default -> "&b[Rick] &f...";
            };
        } else {
            raw = list.get(plugin.getServer().getRandom().nextInt(list.size()));
        }
        return raw.replace("%player%", player);
    }

    public String dialogue(String key) {
        return plugin.getConfig().getString("dialogues." + key, "");
    }

    // --- getters -------------------------------------------------------
    public boolean rpEnabled() { return rpEnabled && !rpUrl.isEmpty(); }
    public String rpUrl() { return rpUrl; }
    public String rpHash() { return rpHash; }
    public boolean rpOnJoin() { return rpOnJoin; }
    public boolean rpForce() { return rpForce; }
    public String rpMessage() { return rpMessage; }

    public String currencyName() { return currencyName; }
    public String currencyPlural() { return currencyPlural; }
    public int startingBalance() { return startingBalance; }
    public int dailyBonus() { return dailyBonus; }
    public int killRewardMeeseeks() { return killMeeseeks; }
    public int killRewardPickle() { return killPickle; }

    public int pricePortalGun() { return pricePortalGun; }
    public int priceMegaSeeds() { return priceMegaSeeds; }
    public int priceFlask() { return priceFlask; }
    public int pricePlumbus() { return pricePlumbus; }
    public int priceSnowball() { return priceSnowball; }

    public String portalMode() { return portalMode; }
    public String citadelWorld() { return citadelWorld; }
    public int portalRandomOffset() { return portalRandomOffset; }

    public int gunMaxDistance() { return gunMaxDistance; }
    public int gunCooldownSec() { return gunCooldownSec; }
    public int gunTeleportDelayTicks() { return gunTeleportDelayTicks; }
    public int gunPairLifetimeSec() { return gunPairLifetimeSec; }

    public int seedsDuration() { return seedsDuration; }
    public int seedsSpeedAmp() { return seedsSpeed; }
    public int seedsStrengthAmp() { return seedsStrength; }
    public int seedsCooldown() { return seedsCooldown; }

    public List<PotionEffectType> flaskPool() { return flaskPool; }
    public int flaskMinDur() { return flaskMinDur; }
    public int flaskMaxDur() { return flaskMaxDur; }

    public boolean naturalSpawnEnabled() { return spawnEnabled; }
    public int spawnIntervalSeconds() { return spawnInterval; }
    public int spawnMaxPerWorld() { return spawnMaxPerWorld; }
    public double chanceMeeseeks() { return chanceMeeseeks; }
    public double chancePickle() { return chancePickle; }
    public double chanceSnowBall() { return chanceSnowBall; }

    public boolean debug() { return debug; }
    public String prefix() { return prefix; }
}
