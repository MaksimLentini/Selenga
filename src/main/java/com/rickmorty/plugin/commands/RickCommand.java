package com.rickmorty.plugin.commands;

import com.rickmorty.plugin.core.RickAndMortyPlugin;
import com.rickmorty.plugin.entities.CharacterType;
import com.rickmorty.plugin.items.CustomItem;
import com.rickmorty.plugin.utils.EconomyManager;
import com.rickmorty.plugin.utils.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Обработчик и таб-автодополнение команды /rick.
 *
 * <p>Подкоманды:</p>
 * <ul>
 *   <li>{@code /rick spawn <rick|morty|meeseeks|pickle|snowball>} — спавн персонажа [admin]</li>
 *   <li>{@code /rick portal} — телепорт в измерение (Citadel/случайная локация)</li>
 *   <li>{@code /rick give <portalgun|megaseeds|plumbus|flask>} — выдать предмет [admin]</li>
 *   <li>{@code /rick balance} — показать шмекли</li>
 *   <li>{@code /rick claim} — ежедневный бонус шмеклей</li>
 *   <li>{@code /rick reload} — перезагрузить конфиг [admin]</li>
 *   <li>{@code /rick help} — справка</li>
 * </ul>
 */
public final class RickCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS =
            List.of("spawn", "portal", "give", "balance", "claim", "reload", "help");
    private static final List<String> SPAWN_TYPES =
            List.of("rick", "morty", "meeseeks", "pickle", "snowball");
    private static final List<String> GIVE_ITEMS =
            List.of("portalgun", "megaseeds", "plumbus", "flask");

    private final RickAndMortyPlugin plugin;

    public RickCommand(RickAndMortyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        try {
            if (args.length == 0) { printHelp(sender); return true; }
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "spawn"   -> handleSpawn(sender, args);
                case "portal"  -> handlePortal(sender);
                case "give"    -> handleGive(sender, args);
                case "balance" -> handleBalance(sender);
                case "claim"   -> handleClaim(sender);
                case "reload"  -> handleReload(sender);
                case "help"    -> printHelp(sender);
                default        -> MessageUtil.send(sender, plugin, "&cНеизвестная подкоманда. &7/rick help");
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("Ошибка команды /rick: " + ex.getMessage());
            ex.printStackTrace();
            MessageUtil.send(sender, plugin, "&cВнутренняя ошибка. Сообщите админу.");
        }
        return true;
    }

    // ---------- Подкоманды ----------

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rickandmorty.admin")) {
            deny(sender); return;
        }
        if (!(sender instanceof Player p)) {
            MessageUtil.send(sender, plugin, "&cСпавн возможен только игроком (нужна локация).");
            return;
        }
        if (args.length < 2) {
            MessageUtil.send(sender, plugin, "&cИспользование: /rick spawn <" + String.join("|", SPAWN_TYPES) + ">");
            return;
        }
        CharacterType type = CharacterType.fromString(args[1]);
        if (type == null) {
            MessageUtil.send(sender, plugin, "&cНеизвестный персонаж: &f" + args[1]);
            return;
        }
        Location loc = p.getLocation().add(0, 0, 2);
        var ent = plugin.getEntityRegistry().spawn(type, loc, p);
        if (ent != null) {
            MessageUtil.send(p, plugin, "&aЗаспавнен &f" + type.displayName() + "&a!");
        } else {
            MessageUtil.send(p, plugin, "&cНе удалось заспавнить персонажа (см. консоль).");
        }
    }

    /**
     * /rick portal — режим из конфига:
     * CITADEL — телепорт в отдельный мир (создаётся при первом вызове),
     * RANDOM_OFFSET — случайная точка на offset блоков от текущей.
     */
    private void handlePortal(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            MessageUtil.send(sender, plugin, "&cТолько для игроков."); return;
        }
        var cfg = plugin.getPluginConfig();
        if ("CITADEL".equals(cfg.portalMode())) {
            World citadel = Bukkit.getWorld(cfg.citadelWorld());
            if (citadel == null) {
                MessageUtil.send(p, plugin, "&bЦитадель ещё не построена. Создаю измерение...");
                WorldCreator wc = new WorldCreator(cfg.citadelWorld())
                        .environment(World.Environment.NORMAL)
                        .generator(new VoidChunkGenerator());
                citadel = wc.createWorld();
                if (citadel == null) {
                    MessageUtil.send(p, plugin, "&cНе удалось создать мир Цитадели.");
                    return;
                }
                citadel.setTime(6000);
                citadelStyle(citadel);
            }
            Location spawn = citadel.getSpawnLocation();
            // Безопасная точка: ищем воздух над платформой спавна
            p.teleport(spawn.add(ThreadLocalRandom.current().nextInt(-10, 11), 3,
                    ThreadLocalRandom.current().nextInt(-10, 11)));
            MessageUtil.send(p, plugin, "&a*вжух* Вы в Цитадели. Не трогайте ничего красного.");
        } else {
            int off = cfg.portalRandomOffset();
            ThreadLocalRandom r = ThreadLocalRandom.current();
            Location dest = p.getLocation().add(
                    r.nextInt(-off, off + 1), 0, r.nextInt(-off, off + 1));
            dest.setY(Math.max(64, p.getWorld().getHighestBlockYAt(dest)));
            p.teleport(dest);
            MessageUtil.send(p, plugin, "&aПортал перенёс вас на &f" + off + "&a блоков&7. Geez!");
        }
    }

    private void citadelStyle(World w) {
        w.setStorm(false);
        w.setFullTime(6000); // вечный полдень Цитадели
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rickandmorty.admin")) { deny(sender); return; }
        if (args.length < 2) {
            MessageUtil.send(sender, plugin,
                    "&cИспользование: /rick give <" + String.join("|", GIVE_ITEMS) + ">");
            return;
        }
        CustomItem ci = CustomItem.fromString(args[1]);
        if (ci == null) {
            MessageUtil.send(sender, plugin, "&cНеизвестный предмет: &f" + args[1]);
            return;
        }
        Player target = (args.length >= 3 && sender.hasPermission("rickandmorty.admin"))
                ? Bukkit.getPlayerExact(args[2]) : (sender instanceof Player p ? p : null);
        if (target == null) {
            MessageUtil.send(sender, plugin, "&cИгрок не найден: " + args[2]);
            return;
        }
        var item = plugin.getItemRegistry().create(ci);
        if (item != null) {
            target.getInventory().addItem(item);
            MessageUtil.send(sender, plugin, "&aВыдано: &f" + ci.displayName() + " &7-> " + target.getName());
        }
    }

    private void handleBalance(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            MessageUtil.send(sender, plugin, "&cТолько для игроков."); return;
        }
        EconomyManager eco = plugin.getEconomyManager();
        MessageUtil.send(p, plugin, "&fБаланс: " + eco.format(eco.getBalance(p)));
    }

    private void handleClaim(CommandSender sender) {
        if (!(sender instanceof Player p)) {
            MessageUtil.send(sender, plugin, "&cТолько для игроков."); return;
        }
        if (plugin.getEconomyManager().claimDaily(p)) {
            MessageUtil.send(p, plugin, "&aПолучен дневной бонус: "
                    + plugin.getEconomyManager().format(plugin.getPluginConfig().dailyBonus()));
        } else {
            MessageUtil.send(p, plugin, "&cБонус уже забран. Возвращайся завтра, Jerry.");
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("rickandmorty.admin")) { deny(sender); return; }
        plugin.reloadPlugin();
        MessageUtil.send(sender, plugin, "&aКонфиг перезагружен. Я всё починил. Наверное.");
    }

    private void deny(CommandSender sender) {
        MessageUtil.send(sender, plugin, "&cНедостаточно прав (rickandmorty.admin).");
    }

    private void printHelp(CommandSender sender) {
        MessageUtil.sendRaw(sender, "&8&m----------------------------------");
        MessageUtil.sendRaw(sender, "&b&lRick and Morty &7— команды:");
        MessageUtil.sendRaw(sender, " &e/rick spawn <rick|morty|meeseeks|pickle|snowball> &7- спавн [admin]");
        MessageUtil.sendRaw(sender, " &e/rick portal &7- портальнуть в другое измерение");
        MessageUtil.sendRaw(sender, " &e/rick give <portalgun|megaseeds|plumbus|flask> [player] &7[admin]");
        MessageUtil.sendRaw(sender, " &e/rick balance &7- ваши шмекли");
        MessageUtil.sendRaw(sender, " &e/rick claim &7- забрать дневной бонус");
        MessageUtil.sendRaw(sender, " &e/rick reload &7- перезагрузить конфиг [admin]");
        MessageUtil.sendRaw(sender, " &8&m----------------------------------");
    }

    // ---------- TabComplete ----------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUBS) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2) {
            List<String> pool = switch (args[0].toLowerCase(Locale.ROOT)) {
                case "spawn" -> SPAWN_TYPES;
                case "give" -> GIVE_ITEMS;
                default -> List.of();
            };
            for (String s : pool) if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    out.add(p.getName());
                }
            }
        }
        return out;
    }
}
