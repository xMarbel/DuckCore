package pl.duckmc.core;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

/** 5 kamieni perkow. Kolejnosc = kolejnosc kolumn w menu "Twoje perki". */
public enum StoneType {
    SILA("Siły", "#ff4d4d", Material.RED_STAINED_GLASS_PANE, Material.REDSTONE),
    ODPORNOSC("Odporności", "#38bdf8", Material.BLUE_STAINED_GLASS_PANE, Material.LAPIS_LAZULI),
    ZRECZNOSC("Zręczności", "#facc15", Material.YELLOW_STAINED_GLASS_PANE, Material.GLOWSTONE_DUST),
    INTELIGENCJA("Inteligencji", "#4ade80", Material.LIME_STAINED_GLASS_PANE, Material.EMERALD),
    MAGIA("Magii", "#d946ef", Material.PURPLE_STAINED_GLASS_PANE, Material.AMETHYST_SHARD);

    public final String label;
    public final String hex;
    public final Material pane;
    public final Material item;

    StoneType(String label, String hex, Material pane, Material item) {
        this.label = label;
        this.hex = hex;
        this.pane = pane;
        this.item = item;
    }

    /** Tag MiniMessage z kolorem, np. "<#ff4d4d>". */
    public String tag() {
        return "<" + hex + ">";
    }

    public String stoneName() {
        return tag() + "Kamień " + label;
    }

    /** Kolumna w menu perkow (0,2,4,6,8). */
    public int column() {
        return ordinal() * 2;
    }

    public List<Perk> perks() {
        return Arrays.stream(Perk.values()).filter(p -> p.stone == this).toList();
    }
}
