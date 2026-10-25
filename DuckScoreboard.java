package pl.duckmc.core;

import pl.duckmc.core.ClanManager.Clan;
import pl.duckmc.core.DataManager.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Scoreboard DuckMC. Bez: Kanal, Esencje, Srebrne, Zlote, play.duckmc.pl.
 * Z: Smoczy Odlamek + animowany niebieski footer Dc.duckmc.pl.
 */
public class DuckScoreboard {

    private static final int LINES = 13;

    private final DuckCore plugin;
    private final Map<UUID, String[]> cache = new HashMap<>();
    private int tick = 0;

    public DuckScoreboard(DuckCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tick++;
            for (Player p : Bukkit.getOnlinePlayers()) update(p);
        }, 20L, 2L);
    }

    public void remove(Player p) {
        cache.remove(p.getUniqueId());
    }

    private void update(Player p) {
        Scoreboard board = p.getScoreboard();
        Objective obj = board.getObjective("duck");
        if (obj == null) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            obj = board.registerNewObjective("duck", Criteria.DUMMY, Util.mm("DUCKMC.PL"));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            for (int i = 0; i < LINES; i++) {
                Team t = board.registerNewTeam("l" + i);
                t.addEntry(entry(i));
                obj.getScore(entry(i)).setScore(LINES - i);
            }
            p.setScoreboard(board);
            cache.remove(p.getUniqueId());
        }

        // tytul z plynacym gradientem
        double phase = Math.sin(tick / 20.0);
        obj.displayName(Util.mm("<bold><gradient:#ff9a00:#ff3d77:#a855f7:"
                + String.format(Locale.US, "%.2f", phase) + ">DUCKMC.PL</gradient></bold>"));

        String[] now = buildLines(p);
        String[] old = cache.computeIfAbsent(p.getUniqueId(), k -> new String[LINES]);
        for (int i = 0; i < LINES; i++) {
            if (!now[i].equals(old[i])) {
                board.getTeam("l" + i).prefix(Util.mm(now[i]));
                old[i] = now[i];
            }
        }
    }

    private String[] buildLines(Player p) {
        PlayerData d = plugin.data.get(p);
        Clan clan = plugin.clans.of(d);

        long ticks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);
        long minutes = ticks / 20 / 60;
        String playtime = (minutes / 60) + "h, " + (minutes % 60) + "min";

        String patron = d.patron == null || d.patron.equals("-") ? "<gray>-" : "<gold>⚡ <yellow>" + d.patron;
        String clanTag = clan == null ? "<gray>-" : "<green>" + clan.tag;
        String points = clan == null ? "<gray>-" : "<yellow>brakuje " + plugin.clans.missingToNext(clan) + " pkt";

        String[] l = new String[LINES];
        l[0] = " ";
        l[1] = "<gray>▪ <white>Nick: <yellow>" + p.getName();
        l[2] = "<gray>▪ <white>Ranga: <gray>[<white>" + d.rank + "<gray>]";
        l[3] = "<gray>▪ <white>Patron: " + patron;
        l[4] = "<gray>▪ <white>Stan konta: <green>" + Util.money(d.balance);
        l[5] = "<gray>▪ <red>⚔ " + d.kills + " <dark_gray>| <white>☠ <gray>" + d.deaths;
        l[6] = "<gray>▪ <white>Ranking: <gold>" + d.ranking + " <aqua>#" + plugin.data.position(p.getUniqueId());
        l[7] = "<gray>▪ <white>Czas gry: <aqua>" + playtime;
        l[8] = "<gray>▪ <white>Smoczy Odłamek: <gradient:#7b2ff7:#f107a3>" + d.shards + "</gradient>";
        l[9] = "<gray>▪ <white>Twój klan: " + clanTag;
        l[10] = "<gray>▪ <white>Punkty: " + points;
        l[11] = "  ";

        double ph = Math.sin(tick / 15.0);
        l[12] = "<bold><gradient:#00c6ff:#0072ff:#00e5ff:" + String.format(Locale.US, "%.2f", ph)
                + ">Dc.duckmc.pl</gradient></bold>";
        return l;
    }

    private static String entry(int i) {
        return "§" + Integer.toHexString(i) + "§r";
    }
}
