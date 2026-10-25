package pl.duckmc.core;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public class DuckCore extends JavaPlugin {

    private static DuckCore instance;

    public DataManager data;
    public ClanManager clans;
    public PerkManager perks;
    public Gui gui;
    public DuckScoreboard board;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        data = new DataManager(this);
        clans = new ClanManager(this);
        perks = new PerkManager(this);
        gui = new Gui(this);
        board = new DuckScoreboard(this);

        Bukkit.getPluginManager().registerEvents(gui, this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);

        Commands cmds = new Commands(this);
        for (String name : new String[]{"szaman", "odlamki", "klan", "duck"}) {
            getCommand(name).setExecutor(cmds);
            getCommand(name).setTabCompleter(cmds);
        }

        board.start();

        // autozapis co 5 minut
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            data.save();
            clans.save();
        }, 6000L, 6000L);

        // /reload - gracze juz na serwerze
        for (Player p : Bukkit.getOnlinePlayers()) {
            data.get(p);
            perks.applyLife(p);
        }
        getLogger().info("DuckCore wlaczony.");
    }

    @Override
    public void onDisable() {
        if (data != null) data.save();
        if (clans != null) clans.save();
    }

    // =====================================================================
    //  API dla innych pluginow / generatorow
    // =====================================================================

    public static DuckCore get() {
        return instance;
    }

    /** Mnoznik zarobku z generatorow: 1.0 + (laczny bonus Perku Dropu / 100). */
    public static double getGeneratorMultiplier(Player p) {
        DuckCore c = instance;
        if (c == null) return 1.0;
        return 1.0 + c.perks.total(c.data.get(p), Perk.DROP) / 100.0;
    }

    /** Czy gracz ma aktualnie zablokowana eventowke (Perk Blokady Eventowki przeciwnika). */
    public static boolean isEventBlocked(Player p) {
        DuckCore c = instance;
        return c != null && c.perks.active(Perk.EVENT_BLOCK, p);
    }

    public static void addShards(UUID id, long amount) {
        DuckCore c = instance;
        if (c == null) return;
        DataManager.PlayerData d = c.data.get(id);
        if (d != null) d.shards = Math.max(0, d.shards + amount);
    }
}
