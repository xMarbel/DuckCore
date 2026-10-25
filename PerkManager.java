package pl.duckmc.core;

import pl.duckmc.core.DataManager.PlayerData;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PerkManager {

    private final DuckCore plugin;
    private final NamespacedKey lifeKey;
    /** aktywne efekty na graczach: perk -> (gracz -> do kiedy ms) */
    private final Map<Perk, Map<UUID, Long>> timers = new EnumMap<>(Perk.class);

    public PerkManager(DuckCore plugin) {
        this.plugin = plugin;
        this.lifeKey = new NamespacedKey(plugin, "perk_life");
    }

    // ---------- bonusy ----------

    public double capOwn() {
        return plugin.getConfig().getDouble("caps.own", 70.0);
    }

    public double capClan() {
        return plugin.getConfig().getDouble("caps.clan", 20.0);
    }

    public double own(PlayerData d, Perk p) {
        return d.own(p);
    }

    public double clanBonus(PlayerData d) {
        return plugin.clans.bonus(plugin.clans.of(d));
    }

    public double total(PlayerData d, Perk p) {
        return Math.min(capOwn() + capClan(), own(d, p) + clanBonus(d));
    }

    /** Losuje, czy perk zadzialal (szansa = laczny bonus w %). */
    public boolean roll(PlayerData d, Perk p) {
        double t = total(d, p);
        return t > 0 && ThreadLocalRandom.current().nextDouble() * 100.0 < t;
    }

    // ---------- losowanie wartosci ----------

    /** Bonus z losowania perka: 0.10% - 10%, czesciej male wartosci. */
    public double rollGain() {
        double min = plugin.getConfig().getDouble("draw.perk-min", 0.10);
        double max = plugin.getConfig().getDouble("draw.perk-max", 10.0);
        double curve = plugin.getConfig().getDouble("draw.perk-curve", 3.0);
        double r = Math.pow(ThreadLocalRandom.current().nextDouble(), curve);
        return Util.round2(min + (max - min) * r);
    }

    /** Bonus z ulepszenia Perku Dropu: rowno od 3% do 10%. */
    public double rollDropGain() {
        double min = plugin.getConfig().getDouble("draw.drop-min", 3.0);
        double max = plugin.getConfig().getDouble("draw.drop-max", 10.0);
        return Util.round2(min + (max - min) * ThreadLocalRandom.current().nextDouble());
    }

    /** Dodaje bonus do perka z uwzglednieniem limitu. Zwraca faktycznie dodana wartosc. */
    public double addBonus(PlayerData d, Perk p, double gain) {
        double old = d.own(p);
        double nw = Util.round2(Math.min(capOwn(), old + gain));
        d.perks.put(p, nw);
        return Util.round2(nw - old);
    }

    // ---------- efekty czasowe (debuffy na przeciwniku) ----------

    public void mark(Perk perk, Player target, long millis) {
        timers.computeIfAbsent(perk, k -> new HashMap<>()).put(target.getUniqueId(), System.currentTimeMillis() + millis);
    }

    public boolean active(Perk perk, Player target) {
        Map<UUID, Long> m = timers.get(perk);
        if (m == null) return false;
        Long until = m.get(target.getUniqueId());
        if (until == null) return false;
        if (until < System.currentTimeMillis()) {
            m.remove(target.getUniqueId());
            return false;
        }
        return true;
    }

    public void clear(Player p) {
        for (Map<UUID, Long> m : timers.values()) m.remove(p.getUniqueId());
    }

    // ---------- Perk Zycia: maksymalne zdrowie ----------

    public void applyLife(Player p) {
        AttributeInstance inst = p.getAttribute(Attribute.MAX_HEALTH);
        if (inst == null) return;
        inst.removeModifier(lifeKey);
        PlayerData d = plugin.data.get(p);
        double bonusPct = total(d, Perk.LIFE);
        if (bonusPct <= 0) return;
        // bazowe 20 HP * bonus%
        double extra = 20.0 * bonusPct / 100.0;
        inst.addModifier(new AttributeModifier(lifeKey, extra, AttributeModifier.Operation.ADD_NUMBER));
    }
}
