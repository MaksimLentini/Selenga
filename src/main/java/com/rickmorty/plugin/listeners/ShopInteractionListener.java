package com.rickmorty.plugin.listeners;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import com.rickmorty.plugin.entities.CharacterType;
import com.rickmorty.plugin.items.CustomItem;
import com.rickmorty.plugin.models.Animation;
import com.rickmorty.plugin.utils.MessageUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.logging.Level;

/**
 * Магазин Рика, диалоги NPC, урон/смерть мобов плагина.
 *
 * <ul>
 *   <li>Клик по Рику (ПКМ) → GUI-магазин за шмекли;</li>
 *   <li>Клик по Морти/Мисиксу/Пикаль Рику → реплика из config dialogues;</li>
 *   <li>Рик неуязвим (отмена урона);</li>
 *   <li>За убийство Мисикса/Пикаль Рика — награда шмеклями;</li>
 *   <li>Анимации attack/death прокидываются в ModelEngine, если он активен.</li>
 * </ul>
 */
public final class ShopInteractionListener implements Listener {

    private final RickAndMortyPlugin plugin;
    private final NamespacedKey keyShopEntry;

    public ShopInteractionListener(RickAndMortyPlugin plugin) {
        this.plugin = plugin;
        this.keyShopEntry = new NamespacedKey(plugin, "shop_entry");
    }

    // ---------- Взаимодействие с NPC ----------

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractAtEntityEvent e) {
        try {
            Entity clicked = e.getRightClicked();
            CharacterType t = plugin.getEntityRegistry().typeOf(clicked);
            if (t == null) return;
            Player p = e.getPlayer();

            switch (t) {
                case RICK -> {
                    e.setCancelled(true);
                    if (p.isSneaking()) {
                        MessageUtil.sendRaw(p, plugin.getPluginConfig()
                                .randomDialogue("rick_greeting", p.getName()));
                    } else {
                        openShop(p);
                    }
                }
                case MORTY -> MessageUtil.sendRaw(p, plugin.getPluginConfig()
                        .randomDialogue("morty_greeting", p.getName()));
                case MR_MEESEEKS -> MessageUtil.sendRaw(p, plugin.getPluginConfig()
                        .randomDialogue("meeseeks_attack", p.getName()));
                case PICKLE_RICK -> MessageUtil.sendRaw(p, plugin.getPluginConfig()
                        .randomDialogue("pickle_rick_greeting", p.getName()));
                case SNOW_BALL -> p.playSound(p.getLocation(),
                        org.bukkit.Sound.ENTITY_PARROT_AMBIENT, 1f, 1.4f);
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "onInteractEntity error", ex);
        }
    }

    // ---------- Урон ----------

    /** Рик неуязвим; мобы плагина получают урон нормально, но прокидываем анимацию. */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {
        try {
            if (!(event.getEntity() instanceof LivingEntity le)) return;
            CharacterType t = plugin.getEntityRegistry().typeOf(le);
            if (t == null) return;

            if (t == CharacterType.RICK) {
                event.setCancelled(true);   // в NPC нельзя стрелять
                return;
            }
            plugin.getModelRegistry().playAnimation(le, Animation.ATTACK);

            // Мисикс в ярости атакует обидчика
            if (t == CharacterType.MR_MEESEEKS && event instanceof EntityDamageByEntityEvent dbe
                    && dbe.getDamager() instanceof LivingEntity attacker) {
                le.setTarget(attacker);
                if (plugin.getServer().getRandom().nextDouble() < 0.3) {
                    if (attacker instanceof Player pl) {
                        MessageUtil.sendRaw(pl, plugin.getPluginConfig()
                                .randomDialogue("meeseeks_attack", pl.getName()));
                    }
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "onDamage error", ex);
        }
    }

    /** Не даём пикаль-рику кусать креатив-админов случайно и т.п. — без изменений. */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        try {
            LivingEntity dead = e.getEntity();
            CharacterType t = plugin.getEntityRegistry().typeOf(dead);
            if (t == null) return;
            plugin.getModelRegistry().release(dead);

            Player killer = dead.getKiller();
            if (killer != null) {
                var eco = plugin.getEconomyManager();
                var cfg = plugin.getPluginConfig();
                int reward = switch (t) {
                    case MR_MEESEEKS -> cfg.killRewardMeeseeks();
                    case PICKLE_RICK -> cfg.killRewardPickle();
                    default -> 0;
                };
                if (reward > 0) {
                    eco.deposit(killer, reward);
                    MessageUtil.send(killer, plugin, "&a+" + eco.format(reward)
                            + " &7за &f" + t.displayName());
                }
            }
            MessageUtil.debug(plugin, "Персонаж умер: " + t);
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "onEntityDeath error", ex);
        }
    }

    // ---------- Магазин ----------

    /**
     * Открывает GUI-магазин Рика. Товары помечаются PDC-ключом shop_entry,
     * клик по ним обрабатывается в onInventoryClick.
     */
    private void openShop(Player p) {
        var cfg = plugin.getPluginConfig();
        Inventory inv = plugin.getServer().createInventory(new ShopHolder(), 27,
                MessageUtil.color("&8Магазин Рика &7(" + plugin.getEconomyManager().getBalance(p) + " "
                        + cfg.currencyPlural() + ")"));

        put(inv, 10, plugin.getItemRegistry().create(CustomItem.PORTAL_GUN), cfg.pricePortalGun());
        put(inv, 12, plugin.getItemRegistry().create(CustomItem.MEGA_SEEDS), cfg.priceMegaSeeds());
        put(inv, 14, plugin.getItemRegistry().create(CustomItem.RICK_FLASK), cfg.priceFlask());
        put(inv, 16, plugin.getItemRegistry().create(CustomItem.PLUMBUS), cfg.pricePlumbus());

        // Питомец: снежный шар
        ItemStack pet = new ItemStack(Material.SNOWBALL);
        ItemMeta pm = pet.getItemMeta();
        if (pm != null) {
            pm.setDisplayName(MessageUtil.color("&f&lСнежный шар (питомец)"));
            pm.setLore(List.of(MessageUtil.color("&7Сядет вам на плечо. Обслуживание не требуется.")));
            pet.setItemMeta(pm);
        }
        put(inv, 22, pet, cfg.priceSnowball());

        MessageUtil.sendRaw(p, cfg.dialogue("shop_intro"));
        p.openInventory(inv);
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_TRADE, 1f, 0.8f);
    }

    private void put(Inventory inv, int slot, ItemStack item, int price) {
        if (item == null) return;
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            List<String> lore = m.getLore() == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(m.getLore());
            lore.add("");
            lore.add(MessageUtil.color("&6Цена: " + price + " " + plugin.getPluginConfig().currencyPlural()));
            m.setLore(lore);
            m.getPersistentDataContainer().set(keyShopEntry, PersistentDataType.INTEGER, price);
            // Сохраняем id товара для покупки
            String cid = plugin.getItemRegistry().customId(item);
            m.getPersistentDataContainer().set(new NamespacedKey(plugin, "shop_item_id"),
                    PersistentDataType.STRING, cid == null ? "snow_ball_pet" : cid);
            item.setItemMeta(m);
        }
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!(e.getView().getTopInventory().getHolder() instanceof ShopHolder)) return;
        e.setCancelled(true); // защита от выноса товара руками

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;

        var cdc = clicked.getItemMeta().getPersistentDataContainer();
        Integer price = cdc.get(keyShopEntry, PersistentDataType.INTEGER);
        String itemId = cdc.get(new NamespacedKey(plugin, "shop_item_id"), PersistentDataType.STRING);
        if (price == null || itemId == null) return;

        var eco = plugin.getEconomyManager();
        if (!eco.withdraw(p, price)) {
            MessageUtil.send(p, plugin, "&cНе хватает шмеклей! Нужно &f" + price
                    + "&c. Твоё: &f" + eco.getBalance(p));
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // Выдача товара
        if ("snow_ball_pet".equals(itemId)) {
            var ent = plugin.getEntityRegistry().spawn(CharacterType.SNOW_BALL,
                    p.getLocation(), p);
            if (ent == null) eco.deposit(p, price); // возврат при ошибке спавна
        } else {
            CustomItem ci = CustomItem.fromString(itemId);
            ItemStack item = ci != null ? plugin.getItemRegistry().create(ci) : null;
            if (item != null) p.getInventory().addItem(item);
            else eco.deposit(p, price);
        }
        MessageUtil.send(p, plugin, "&aПокупка совершена! &7-" + eco.format(price));
        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_YES, 1f, 1.2f);
        // Обновить заголовок с балансом
        openShopRefresh(p);
    }

    /** Пересобираем GUI, чтобы показать новый баланс. */
    private void openShopRefresh(Player p) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof ShopHolder) {
                openShop(p);
            }
        }, 2L);
    }

    /** Маркер-холдер для идентификации магазина (чтобы отличать от других GUI). */
    private static final class ShopHolder implements org.bukkit.inventory.InventoryHolder {
        @Override
        public Inventory getInventory() { return null; }
    }
}
