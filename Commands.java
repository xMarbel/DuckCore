package pl.duckmc.core;

import pl.duckmc.core.ClanManager.Clan;
import pl.duckmc.core.DataManager.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class Commands implements CommandExecutor, TabCompleter {

    private final DuckCore plugin;

    public Commands(DuckCore plugin) {
        this.plugin = plugin;
    }

    private void msg(CommandSender s, String text) {
        s.sendMessage(Util.mm(Util.PREFIX + text));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        switch (cmd.getName().toLowerCase()) {
            case "szaman" -> {
                if (!(sender instanceof Player p)) return true;
                plugin.gui.openShaman(p);
            }
            case "odlamki" -> {
                if (!(sender instanceof Player p)) return true;
                PlayerData d = plugin.data.get(p);
                msg(p, "<gray>Masz <white>" + d.shards + " " + Util.SHARD);
                for (StoneType t : StoneType.values()) {
                    if (d.stones(t) > 0) p.sendMessage(Util.mm("  <dark_gray>» " + t.stoneName() + " <gray>x" + d.stones(t)));
                }
            }
            case "klan" -> {
                if (!(sender instanceof Player p)) return true;
                clan(p, a);
            }
            case "duck" -> admin(sender, a);
            default -> {
            }
        }
        return true;
    }

    // =====================================================================
    //  KLAN
    // =====================================================================

    private void clan(Player p, String[] a) {
        PlayerData d = plugin.data.get(p);
        Clan own = plugin.clans.of(d);
        if (a.length == 0) {
            msg(p, "<gray>/klan <white>stworz <TAG> <dark_gray>| <white>zapros <gracz> <dark_gray>| <white>dolacz <TAG>");
            msg(p, "<gray>/klan <white>opusc <dark_gray>| <white>usun <dark_gray>| <white>info [TAG] <dark_gray>| <white>top");
            return;
        }
        switch (a[0].toLowerCase()) {
            case "stworz" -> {
                if (a.length < 2) {
                    msg(p, "<red>Użycie: /klan stworz <TAG>");
                    return;
                }
                if (own != null) {
                    msg(p, "<red>Już jesteś w klanie!");
                    return;
                }
                String tag = a[1].toUpperCase();
                if (!tag.matches("[A-Z0-9]{2,4}")) {
                    msg(p, "<red>Tag musi mieć 2-4 znaki (litery/cyfry).");
                    return;
                }
                if (plugin.clans.get(tag) != null) {
                    msg(p, "<red>Klan o takim tagu już istnieje!");
                    return;
                }
                plugin.clans.create(tag, p.getUniqueId());
                d.clan = tag;
                msg(p, "<green>Stworzono klan <white>" + tag + "<green>!");
            }
            case "zapros" -> {
                if (own == null || !own.owner.equals(p.getUniqueId())) {
                    msg(p, "<red>Tylko lider klanu może zapraszać!");
                    return;
                }
                if (a.length < 2) {
                    msg(p, "<red>Użycie: /klan zapros <gracz>");
                    return;
                }
                Player t = Bukkit.getPlayerExact(a[1]);
                if (t == null) {
                    msg(p, "<red>Gracz offline.");
                    return;
                }
                if (plugin.clans.of(plugin.data.get(t)) != null) {
                    msg(p, "<red>Ten gracz jest już w klanie.");
                    return;
                }
                if (own.members.size() >= plugin.getConfig().getInt("clan.max-members", 10)) {
                    msg(p, "<red>Klan jest pełny!");
                    return;
                }
                plugin.clans.invites.put(t.getUniqueId(), own.tag);
                msg(p, "<green>Zaproszono <white>" + t.getName() + "<green>.");
                msg(t, "<yellow>" + p.getName() + " <gray>zaprasza Cię do klanu <white>" + own.tag
                        + "<gray>. Wpisz <green>/klan dolacz " + own.tag);
            }
            case "dolacz" -> {
                if (a.length < 2) {
                    msg(p, "<red>Użycie: /klan dolacz <TAG>");
                    return;
                }
                if (own != null) {
                    msg(p, "<red>Już jesteś w klanie!");
                    return;
                }
                String tag = a[1].toUpperCase();
                Clan c = plugin.clans.get(tag);
                if (c == null || !tag.equals(plugin.clans.invites.get(p.getUniqueId()))) {
                    msg(p, "<red>Nie masz zaproszenia do tego klanu.");
                    return;
                }
                if (c.members.size() >= plugin.getConfig().getInt("clan.max-members", 10)) {
                    msg(p, "<red>Klan jest pełny!");
                    return;
                }
                c.members.add(p.getUniqueId());
                d.clan = c.tag;
                plugin.clans.invites.remove(p.getUniqueId());
                msg(p, "<green>Dołączyłeś do klanu <white>" + c.tag + "<green>!");
                plugin.perks.applyLife(p);
            }
            case "opusc" -> {
                if (own == null) {
                    msg(p, "<red>Nie jesteś w klanie.");
                    return;
                }
                if (own.owner.equals(p.getUniqueId())) {
                    msg(p, "<red>Lider nie może opuścić klanu. Użyj <white>/klan usun<red>.");
                    return;
                }
                own.members.remove(p.getUniqueId());
                d.clan = null;
                msg(p, "<gray>Opuściłeś klan <white>" + own.tag);
                plugin.perks.applyLife(p);
            }
            case "usun" -> {
                if (own == null || !own.owner.equals(p.getUniqueId())) {
                    msg(p, "<red>Tylko lider może usunąć klan.");
                    return;
                }
                String tag = own.tag;
                plugin.clans.delete(own);
                for (Player o : Bukkit.getOnlinePlayers()) plugin.perks.applyLife(o);
                msg(p, "<gray>Usunięto klan <white>" + tag);
            }
            case "info" -> {
                Clan c = a.length >= 2 ? plugin.clans.get(a[1]) : own;
                if (c == null) {
                    msg(p, "<red>Nie znaleziono klanu.");
                    return;
                }
                OfflinePlayerName names = new OfflinePlayerName();
                StringBuilder sb = new StringBuilder();
                for (UUID m : c.members) sb.append(names.of(m)).append(", ");
                String members = sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "-";
                msg(p, "<gold>Klan <white>" + c.tag + " <dark_gray>| <gray>Poziom: <white>" + plugin.clans.level(c)
                        + " <dark_gray>| <gray>Punkty: <white>" + c.points);
                p.sendMessage(Util.mm("  <gray>Bonus klanowy: <aqua>" + Util.pct(plugin.clans.bonus(c)) + "%"));
                p.sendMessage(Util.mm("  <gray>Lider: <yellow>" + names.of(c.owner)));
                p.sendMessage(Util.mm("  <gray>Członkowie (" + c.members.size() + "): <white>" + members));
            }
            case "top" -> {
                List<Clan> list = new ArrayList<>(plugin.clans.all());
                list.sort((x, y) -> Integer.compare(y.points, x.points));
                msg(p, "<gold>Top klany:");
                for (int i = 0; i < Math.min(10, list.size()); i++) {
                    Clan c = list.get(i);
                    p.sendMessage(Util.mm("  <yellow>" + (i + 1) + ". <white>" + c.tag + " <gray>- " + c.points
                            + " pkt (lvl " + plugin.clans.level(c) + ")"));
                }
            }
            default -> msg(p, "<red>Nieznana podkomenda. Wpisz /klan");
        }
    }

    private class OfflinePlayerName {
        String of(UUID id) {
            PlayerData pd = plugin.data.get(id);
            return pd != null && pd.name != null ? pd.name : id.toString().substring(0, 6);
        }
    }

    // =====================================================================
    //  ADMIN
    // =====================================================================

    private void admin(CommandSender s, String[] a) {
        if (!s.hasPermission("duckcore.admin")) {
            msg(s, "<red>Brak uprawnień.");
            return;
        }
        if (a.length < 1) {
            msg(s, "<gray>/duck <white>odlamki|kamien|ranga|patron|kasa|reload");
            return;
        }
        if (a[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            msg(s, "<green>Przeładowano config.");
            return;
        }
        if (a.length < 3) {
            msg(s, "<red>Użycie: /duck " + a[0] + " <gracz> <wartość>");
            return;
        }
        PlayerData d = plugin.data.byName(a[1]);
        if (d == null) {
            msg(s, "<red>Nie znaleziono gracza (musi być kiedyś na serwerze).");
            return;
        }
        switch (a[0].toLowerCase()) {
            case "odlamki" -> {
                try {
                    d.shards = Math.max(0, d.shards + Long.parseLong(a[2]));
                    msg(s, "<green>" + d.name + " ma teraz <white>" + d.shards + " " + Util.SHARD);
                } catch (NumberFormatException ex) {
                    msg(s, "<red>Podaj liczbę.");
                }
            }
            case "kamien" -> {
                if (a.length < 4) {
                    msg(s, "<red>Użycie: /duck kamien <gracz> <typ> <ilość>");
                    return;
                }
                try {
                    StoneType t = StoneType.valueOf(a[2].toUpperCase());
                    int n = Integer.parseInt(a[3]);
                    d.stones.merge(t, n, Integer::sum);
                    msg(s, "<green>Dodano <white>" + n + "x " + t.stoneName() + " <green>dla " + d.name);
                } catch (IllegalArgumentException ex) {
                    msg(s, "<red>Typy: " + Arrays.toString(StoneType.values()));
                }
            }
            case "ranga" -> {
                d.rank = String.join(" ", Arrays.copyOfRange(a, 2, a.length));
                msg(s, "<green>Ustawiono rangę.");
            }
            case "patron" -> {
                d.patron = String.join(" ", Arrays.copyOfRange(a, 2, a.length));
                msg(s, "<green>Ustawiono patrona.");
            }
            case "kasa" -> {
                try {
                    d.balance += Double.parseDouble(a[2]);
                    msg(s, "<green>Stan konta: <white>" + Util.money(d.balance));
                } catch (NumberFormatException ex) {
                    msg(s, "<red>Podaj liczbę.");
                }
            }
            default -> msg(s, "<red>Nieznana podkomenda.");
        }
    }

    // =====================================================================
    //  TAB
    // =====================================================================

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        String n = cmd.getName().toLowerCase();
        if (n.equals("klan") && a.length == 1) {
            out.addAll(List.of("stworz", "zapros", "dolacz", "opusc", "usun", "info", "top"));
        } else if (n.equals("klan") && a.length == 2 && a[0].equalsIgnoreCase("zapros")) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        } else if (n.equals("duck") && s.hasPermission("duckcore.admin")) {
            if (a.length == 1) out.addAll(List.of("odlamki", "kamien", "ranga", "patron", "kasa", "reload"));
            else if (a.length == 2) for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
            else if (a.length == 3 && a[0].equalsIgnoreCase("kamien"))
                for (StoneType t : StoneType.values()) out.add(t.name().toLowerCase());
        }
        String last = a.length == 0 ? "" : a[a.length - 1].toLowerCase();
        out.removeIf(x -> !x.toLowerCase().startsWith(last));
        return out;
    }
}
