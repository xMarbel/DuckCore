package pl.duckmc.core;

/** 12 perkow z kamieni + Perk Dropu (ulepszany Smoczymi Odlamkami u Szamana). */
public enum Perk {
    // ---- Kamien Sily ----
    LIFESTEAL(StoneType.SILA, "Perk Lifesteal", "#ff4d4d",
            "<white>że każde <red>trafienie <white>ma szansę",
            "<white>na <red>przywrócenie zdrowia"),
    EXTRA_HIT(StoneType.SILA, "Perk Extra Hit", "#ff7a00",
            "<white>że każde <red>trafienie <white>ma szansę",
            "<white>na <red>podwójne uderzenie"),
    HIGHLIGHT(StoneType.SILA, "Perk Podświetlenia", "#facc15",
            "<white>że każde <red>trafienie <white>ma szansę",
            "<white>na <aqua>oznaczenie przeciwnika<white>,",
            "<white>które zwiększa <red>zadawane obrażenia"),

    // ---- Kamien Odpornosci ----
    RESISTANCE(StoneType.ODPORNOSC, "Perk Odporności", "#7dd3fc",
            "<white>że masz szansę na otrzymanie",
            "<white>efektu <light_purple>Resistance"),
    HIT_BLOCK(StoneType.ODPORNOSC, "Perk Blokady Uderzenia", "#38bdf8",
            "<white>że masz szansę <aqua>zablokować",
            "<white>atak <red>przeciwnika"),
    LIFE(StoneType.ODPORNOSC, "Perk Życia", "#f87171",
            "<white>że Twoje <red>maksymalne zdrowie",
            "<white>zostaje <green>zwiększone"),

    // ---- Kamien Zrecznosci ----
    ROOT(StoneType.ZRECZNOSC, "Perk Spętania", "#fde047",
            "<white>że każde <red>trafienie <white>ma szansę",
            "<white>na zablokowanie <aqua>skakania"),
    DURABILITY(StoneType.ZRECZNOSC, "Perk Wytrzymałości", "#fbbf24",
            "<white>że Twoje <aqua>uzbrojenie",
            "<white>ma większą <green>wytrzymałość"),

    // ---- Kamien Inteligencji ----
    BLOCK_BREAK(StoneType.INTELIGENCJA, "Perk Zablokowania Niszczenia Bloków", "#4ade80",
            "<white>że każde <red>trafienie <white>ma szansę",
            "<white>na zablokowanie przeciwnikowi",
            "<white>możliwości <blue>niszczenia bloków"),
    FIREWORK(StoneType.INTELIGENCJA, "Perk Blokady Fajerwerki", "#22c55e",
            "<white>że każde <red>trafienie <white>ma szansę",
            "<white>na zablokowanie przeciwnikowi",
            "<white>możliwości używania <light_purple>fajerwerek"),

    // ---- Kamien Magii ----
    REFLECT(StoneType.MAGIA, "Perk Odbicia", "#c084fc",
            "<white>że masz szansę na <light_purple>odbicie",
            "<red>negatywnego efektu perka",
            "<white>w stronę <red>przeciwnika"),
    EVENT_BLOCK(StoneType.MAGIA, "Perk Blokady Eventówki", "#e879f9",
            "<white>że masz szansę na <light_purple>zablokowanie",
            "<gold>eventówki <white>przeciwnika"),

    // ---- Perk Dropu (bez kamienia) ----
    DROP(null, "Perk Dropu", "#a3e635",
            "<white>że zwiększasz <green>zarobek <white>podczas",
            "<white>kopania <gold>generatorów");

    public final StoneType stone;
    public final String display;
    public final String hex;
    public final String[] desc;

    Perk(StoneType stone, String display, String hex, String... desc) {
        this.stone = stone;
        this.display = display;
        this.hex = hex;
        this.desc = desc;
    }

    public String tag() {
        return "<" + hex + ">";
    }

    /** Nazwa z kolorem, np. "<#ff4d4d>Perk Lifesteal". */
    public String coloredName() {
        return tag() + display;
    }

    /** Czy to perk, ktory dziala jako negatywny efekt na przeciwniku (podlega odbiciu). */
    public boolean isDebuff() {
        return this == HIGHLIGHT || this == ROOT || this == BLOCK_BREAK
                || this == FIREWORK || this == EVENT_BLOCK;
    }
}
