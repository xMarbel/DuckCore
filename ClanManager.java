package pl.duckmc.core;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ClanManager {

    public static class Clan {
        public final String tag;
        public UUID owner;
        public final Set<UUID> members = new LinkedHashSet<>();
        public int points = 0;

        public Clan(String tag, UUID owner) {
            this.tag = tag;
            this.owner = owner;
            this.members.add(owner);
        }
    }

    private final DuckCore plugin;
    private final File file;
    private final Map<String, Clan> clans = new LinkedHashMap<>();
    /** zaproszenia: gracz -> tag klanu */
    public final Map<UUID, String> invites = new HashMap<>();

    public ClanManager(DuckCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "clans.yml");
        load();
    }

    public Clan get(String tag) {
        return tag == null ? null : clans.get(tag.toUpperCase());
    }

    public Clan of(DataManager.PlayerData d) {
        return d.clan == null ? null : clans.get(d.clan);
    }

    public Collection<Clan> all() {
        return clans.values();
    }

    public int level(Clan c) {
        int per = Math.max(1, plugin.getConfig().getInt("clan.points-per-level", 100));
        return 1 + c.points / per;
    }

    /** Ile punktow brakuje do kolejnego poziomu. */
    public int missingToNext(Clan c) {
        int per = Math.max(1, plugin.getConfig().getInt("clan.points-per-level", 100));
        return per - (c.points % per);
    }

    /** Bonus klanowy (%) - taki sam dla kazdego perka. */
    public double bonus(Clan c) {
        if (c == null) return 0;
        double per = plugin.getConfig().getDouble("clan.bonus-per-level", 0.5);
        double cap = plugin.getConfig().getDouble("caps.clan", 20.0);
        return Math.min(cap, per * level(c));
    }

    public Clan create(String tag, UUID owner) {
        Clan c = new Clan(tag.toUpperCase(), owner);
        clans.put(c.tag, c);
        return c;
    }

    public void delete(Clan c) {
        for (UUID m : c.members) {
            DataManager.PlayerData d = plugin.data.get(m);
            if (d != null) d.clan = null;
        }
        clans.remove(c.tag);
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("clans");
        if (sec == null) return;
        for (String tag : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(tag);
            if (s == null) continue;
            try {
                Clan c = new Clan(tag, UUID.fromString(s.getString("owner", "")));
                c.points = s.getInt("points", 0);
                for (String m : s.getStringList("members")) {
                    try {
                        c.members.add(UUID.fromString(m));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                clans.put(tag, c);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Clan c : clans.values()) {
            String p = "clans." + c.tag + ".";
            y.set(p + "owner", c.owner.toString());
            y.set(p + "points", c.points);
            y.set(p + "members", c.members.stream().map(UUID::toString).toList());
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            Bukkit.getLogger().severe("[DuckCore] Nie udalo sie zapisac clans.yml: " + ex.getMessage());
        }
    }
}
