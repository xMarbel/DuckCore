package pl.duckmc.core;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataManager {

    public static class PlayerData {
        public final UUID id;
        public String name;
        public long shards = 0;
        public final EnumMap<StoneType, Integer> stones = new EnumMap<>(StoneType.class);
        public final EnumMap<Perk, Double> perks = new EnumMap<>(Perk.class);
        public int kills = 0;
        public int deaths = 0;
        public int ranking;
        public double balance = 0;
        public String rank = "Gracz";
        public String patron = "-";
        public String clan = null;

        public PlayerData(UUID id, String name, int startRanking) {
            this.id = id;
            this.name = name;
            this.ranking = startRanking;
        }

        public int stones(StoneType t) {
            return stones.getOrDefault(t, 0);
        }

        public double own(Perk p) {
            return perks.getOrDefault(p, 0.0);
        }
    }

    private final DuckCore plugin;
    private final File file;
    private final Map<UUID, PlayerData> map = new HashMap<>();

    // cache pozycji w rankingu
    private Map<UUID, Integer> positions = new HashMap<>();
    private long positionsAt = 0;

    public DataManager(DuckCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        load();
    }

    private int startRanking() {
        return plugin.getConfig().getInt("ranking.start", 1000);
    }

    public PlayerData get(Player p) {
        PlayerData d = map.computeIfAbsent(p.getUniqueId(), k -> new PlayerData(k, p.getName(), startRanking()));
        d.name = p.getName();
        return d;
    }

    public PlayerData get(UUID id) {
        return map.get(id);
    }

    public Collection<PlayerData> all() {
        return map.values();
    }

    public PlayerData byName(String name) {
        for (PlayerData d : map.values()) {
            if (d.name != null && d.name.equalsIgnoreCase(name)) return d;
        }
        return null;
    }

    /** Pozycja w rankingu (1 = najlepszy). Cache odswiezany co 5 s. */
    public int position(UUID id) {
        long now = System.currentTimeMillis();
        if (now - positionsAt > 5000) refreshPositions();
        return positions.getOrDefault(id, map.size());
    }

    public void refreshPositions() {
        List<PlayerData> list = new ArrayList<>(map.values());
        list.sort((a, b) -> Integer.compare(b.ranking, a.ranking));
        Map<UUID, Integer> np = new HashMap<>();
        for (int i = 0; i < list.size(); i++) np.put(list.get(i).id, i + 1);
        positions = np;
        positionsAt = System.currentTimeMillis();
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("players");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            UUID id;
            try {
                id = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            PlayerData d = new PlayerData(id, s.getString("name", "?"), startRanking());
            d.shards = s.getLong("shards", 0);
            d.kills = s.getInt("kills", 0);
            d.deaths = s.getInt("deaths", 0);
            d.ranking = s.getInt("ranking", startRanking());
            d.balance = s.getDouble("balance", 0);
            d.rank = s.getString("rank", "Gracz");
            d.patron = s.getString("patron", "-");
            d.clan = s.getString("clan", null);
            ConfigurationSection st = s.getConfigurationSection("stones");
            if (st != null) {
                for (String k : st.getKeys(false)) {
                    try {
                        d.stones.put(StoneType.valueOf(k), st.getInt(k));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            ConfigurationSection pk = s.getConfigurationSection("perks");
            if (pk != null) {
                for (String k : pk.getKeys(false)) {
                    try {
                        d.perks.put(Perk.valueOf(k), pk.getDouble(k));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            map.put(id, d);
        }
        refreshPositions();
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (PlayerData d : map.values()) {
            String p = "players." + d.id + ".";
            y.set(p + "name", d.name);
            y.set(p + "shards", d.shards);
            y.set(p + "kills", d.kills);
            y.set(p + "deaths", d.deaths);
            y.set(p + "ranking", d.ranking);
            y.set(p + "balance", d.balance);
            y.set(p + "rank", d.rank);
            y.set(p + "patron", d.patron);
            y.set(p + "clan", d.clan);
            for (Map.Entry<StoneType, Integer> e : d.stones.entrySet()) {
                if (e.getValue() > 0) y.set(p + "stones." + e.getKey().name(), e.getValue());
            }
            for (Map.Entry<Perk, Double> e : d.perks.entrySet()) {
                if (e.getValue() > 0) y.set(p + "perks." + e.getKey().name(), e.getValue());
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException ex) {
            Bukkit.getLogger().severe("[DuckCore] Nie udalo sie zapisac players.yml: " + ex.getMessage());
        }
    }
}
