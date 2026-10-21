package com.rickmorty.plugin.listeners;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import com.rickmorty.plugin.items.CustomItem;
import com.rickmorty.plugin.utils.MessageUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

/**
 * Листенер Портальной пушки и consume-предметов.
 *
 * <p>Логика (PlayerInteractEvent):</p>
 * <ol>
 *   <li>ЛКМ с пушкой — ray trace до {@code portal_gun.max_distance} блоков,
 *       точка A в PDC игрока;</li>
 *   <li>ПКМ с пушкой — точка B;</li>
 *   <li>Вход в зону портала обрабатывается таском {@link com.rickmorty.plugin.items.PortalGunManager};</li>
 *   <li>ПКМ с мега-семенами/флягой — потребление с кулдауном.</li>
 * </ol>
 */
public final class PortalGunListener implements Listener {

    private final RickAndMortyPlugin plugin;

    public PortalGunListener(RickAndMortyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        try {
            Player p = e.getPlayer();
            ItemStack hand = e.getItem();
            if (hand == null || e.getHand() != EquipmentSlot.HAND) return;

            String id = plugin.getItemRegistry().customId(hand);
            if (id == null) return;

            if (CustomItem.PORTAL_GUN.id().equals(id)) {
                handlePortalGun(p, e.getAction());
                e.setCancelled(true);
            } else if (CustomItem.MEGA_SEEDS.id().equals(id)
                    && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                handleConsumable(p, CustomItem.MEGA_SEEDS, "seeds_cd");
            } else if (CustomItem.RICK_FLASK.id().equals(id)
                    && (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                handleConsumable(p, CustomItem.RICK_FLASK, "flask_cd");
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("PortalGunListener error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void handlePortalGun(Player p, Action action) {
        int slot;
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) slot = 0;
        else if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) slot = 1;
        else return;

        // Кулдаун на игрока
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(p.getUniqueId());
        int cdMs = plugin.getPluginConfig().gunCooldownSec() * 1000;
        if (last != null && now - last < cdMs) {
            MessageUtil.send(p, plugin, "&cПорталы перегреваются! Подожди немного.");
            return;
        }
        cooldowns.put(p.getUniqueId(), now);

        // Ray trace: куда смотрит игрок
        double dist = plugin.getPluginConfig().gunMaxDistance();
        RayTraceResult hit = p.getWorld().rayTrace(p.getEyeLocation(),
                p.getEyeLocation().getDirection(), dist,
                org.bukkit.FluidCollisionMode.SOURCE_ONLY, false, 0.5, e -> false);

        org.bukkit.Location at;
        if (hit != null && hit.getHitBlock() != null) {
            // Ставим портал на грань блока, в который попал луч
            Block b = hit.getHitBlock();
            org.bukkit.block.BlockFace face = hit.getHitBlockFace();
            at = (face != null ? b.getRelative(face) : b).getLocation();
        } else {
            MessageUtil.send(p, plugin, "&cСлишком далеко или нет стены. &7Прицелься в блок.");
            return;
        }

        boolean ok = plugin.getPortalGunManager().placePortal(p, slot, at);
        if (ok) {
            String name = slot == 0 ? "&aзелёный (A)" : "&9синий (B)";
            MessageUtil.send(p, plugin, "&bПортал " + name + "&b установлен!");
            if (slot == 1 && plugin.getPortalGunManager().getPair(p) != null) {
                MessageUtil.send(p, plugin, "&7Пара готова. Ступай в любой из порталов.");
            }
        }
    }

    private final java.util.Map<java.util.UUID, Long> cooldowns = new java.util.HashMap<>();

    /** Единый обработчик consume-предметов с индивидуальным PDC-кулдауном. */
    private void handleConsumable(Player p, CustomItem ci, String cdKey) {
        var key = new org.bukkit.NamespacedKey(plugin, cdKey);
        Long last = p.getPersistentDataContainer().get(key, org.bukkit.persistence.PersistentDataType.LONG);
        long now = System.currentTimeMillis();
        int cdSec = ci == CustomItem.MEGA_SEEDS
                ? plugin.getPluginConfig().seedsCooldown() : 15;
        if (last != null && now - last < cdSec * 1000L) {
            long left = (cdSec * 1000L - (now - last)) / 1000L + 1;
            MessageUtil.send(p, plugin, "&cКулдаун ещё &f" + left + "&c сек.");
            return;
        }
        p.getPersistentDataContainer().set(key, org.bukkit.persistence.PersistentDataType.LONG, now);

        if (ci == CustomItem.MEGA_SEEDS) {
            plugin.getItemRegistry().consumeMegaSeeds(p);
        } else {
            plugin.getItemRegistry().drinkFlask(p);
        }
        // Списание предмета (стака)
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getAmount() > 1) hand.setAmount(hand.getAmount() - 1);
        else p.getInventory().setItemInMainHand(null);
    }
}
