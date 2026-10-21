package com.rickmorty.plugin.items;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import com.rickmorty.plugin.models.ModelId;
import com.rickmorty.plugin.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Создание и распознавание кастомных предметов.
 *
 * <p>Каждый предмет помечается PDC-ключом {@code rickandmorty:item} со
 * значением id — это защита от подделки (игрок не может переименовать
 * обычную вещь и продать Рику).</p>
 */
public final class ItemRegistry {

    private final RickAndMortyPlugin plugin;
    private final NamespacedKey keyItem;

    public ItemRegistry(RickAndMortyPlugin plugin) {
        this.plugin = plugin;
        this.keyItem = new NamespacedKey(plugin, "item");
    }

    public NamespacedKey itemKey() { return keyItem; }

    /** Возвращает id кастомного предмета на стеке или null. */
    public String customId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        return stack.getItemMeta().getPersistentDataContainer()
                .get(keyItem, PersistentDataType.STRING);
    }

    public boolean isCustom(ItemStack stack, CustomItem ci) {
        return ci.id().equals(customId(stack));
    }

    /** Фабрика: собирает ItemStack по типу. */
    public ItemStack create(CustomItem ci) {
        try {
            return switch (ci) {
                case PORTAL_GUN -> buildPortalGun();
                case PLUMBUS -> buildPlumbus();
                case MEGA_SEEDS -> buildMegaSeeds();
                case RICK_FLASK -> buildFlask();
            };
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка создания предмета " + ci, ex);
            return null;
        }
    }

    private ItemStack base(Material mat, CustomItem ci, ModelId model) {
        ItemStack s = new ItemStack(mat);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setDisplayName(MessageUtil.color("&d&l" + ci.displayName()));
            m.setUnbreakable(true);
            m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            m.getPersistentDataContainer().set(keyItem, PersistentDataType.STRING, ci.id());
            s.setItemMeta(m);
        }
        return plugin.getModelRegistry().applyToItem(s, model);
    }

    /** Портальная пушка: носитель — Blaze Rod, lore с инструкцией. */
    private ItemStack buildPortalGun() {
        ItemStack s = base(Material.BLAZE_ROD, CustomItem.PORTAL_GUN, ModelId.PORTAL_GUN);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            List<String> lore = new ArrayList<>();
            lore.add(MessageUtil.color("&7ЛКМ — установить портал A"));
            lore.add(MessageUtil.color("&7ПКМ — установить портал B"));
            lore.add(MessageUtil.color("&7Вход в портал телепортирует к паре"));
            lore.add("");
            lore.add(MessageUtil.color("&bWubba lubba dub dub!"));
            m.setLore(lore);
            m.addEnchant(Enchantment.LUCK, 1, true);
            s.setItemMeta(m);
        }
        return s;
    }

    /** Плюмбус: декоративный носитель — Carrot on a Stick. */
    private ItemStack buildPlumbus() {
        ItemStack s = base(Material.CARROT_ON_A_STICK, CustomItem.PLUMBUS, ModelId.PLUMBUS);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setLore(List.of(
                    MessageUtil.color("&7Служит для шлюзования галактических"),
                    MessageUtil.color("&7приливов через хранилище шмика.")));
            s.setItemMeta(m);
        }
        return s;
    }

    /** Мега-семена: носитель — Beetle Spawn Egg? Нет: Nether Wart. */
    private ItemStack buildMegaSeeds() {
        ItemStack s = base(Material.NETHER_WART, CustomItem.MEGA_SEEDS, ModelId.MEGA_SEEDS);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setLore(List.of(
                    MessageUtil.color("&aПравый клик: Скорость II + Сила I на 60 сек"),
                    MessageUtil.color("&cПередозировка ведёт к монструификации")));
            s.setItemMeta(m);
        }
        return s;
    }

    /** Фляга Рика: носитель — Potion. Эффект случайный при каждом usage. */
    private ItemStack buildFlask() {
        ItemStack s = new ItemStack(Material.POTION);
        ItemMeta m = s.getItemMeta();
        if (m != null) {
            m.setDisplayName(MessageUtil.color("&9&lФляга Рика"));
            m.setLore(List.of(
                    MessageUtil.color("&7Выпей. Стань лучше. Или хуже."),
                    MessageUtil.color("&7Случайный эффект.")));
            m.setUnbreakable(true);
            m.getPersistentDataContainer().set(keyItem, PersistentDataType.STRING,
                    CustomItem.RICK_FLASK.id());
            s.setItemMeta(m);
        }
        return plugin.getModelRegistry().applyToItem(s, ModelId.RICK_FLASK);
    }

    /** Применяет эффекты мега-семян игроку (duration/amps из конфига). */
    public void consumeMegaSeeds(Player p) {
        var cfg = plugin.getPluginConfig();
        int ticks = cfg.seedsDuration() * 20;
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ticks, cfg.seedsSpeedAmp(), false, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, ticks, cfg.seedsStrengthAmp(), false, true));
        // Отрицательная "побочка": тошнота 3 сек — как в сериале
        p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 60, 0, false, true));
        MessageUtil.send(p, plugin, "&aМега-семена приняты! Ощущаю себя... &fбольшим&7.");
    }

    /** Выпивает флягу: случайный эффект из пула конфига. */
    public void drinkFlask(Player p) {
        var cfg = plugin.getPluginConfig();
        List<PotionEffectType> pool = cfg.flaskPool();
        if (pool.isEmpty()) return;
        PotionEffectType type = pool.get(plugin.getServer().getRandom().nextInt(pool.size()));
        int dur = (cfg.flaskMinDur()
                + plugin.getServer().getRandom().nextInt(Math.max(1, cfg.flaskMaxDur() - cfg.flaskMinDur()))) * 20;
        int amp = plugin.getServer().getRandom().nextInt(2); // 0..1
        boolean harmful = type.getName().equals("HARM") || type.getName().equals("CONFUSION")
                || type.getName().equals("POISON") || type.getName().equals("BLINDNESS");
        if (harmful) {
            AttributeInstance ai = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            double dmg = Math.min(6, (ai == null ? 20 : ai.getValue()) * 0.15);
            p.damage(dmg);
        } else {
            p.addPotionEffect(new PotionEffect(type, dur, amp, false, true));
        }
        MessageUtil.send(p, plugin, "&9*глоток* " + (harmful
                ? "&cЧёрт... это была не та фляга." : "&aЙоу! Вкусно и почти безопасно."));
    }
}
