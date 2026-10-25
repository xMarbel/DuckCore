package pl.duckmc.core;

import pl.duckmc.core.ClanManager.Clan;
import pl.duckmc.core.DataManager.PlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CombatListener implements Listener {

    private final DuckCore plugin;
    /** gracze, ktorzy wlasnie zadaja "extra hit" - blokuje petle perkow */
    private final Set<UUID> extraGuard = new HashSet<>();
    /** anti-farm: "zabojca:ofiara" -> kiedy ostatnio */
    private final Map<String, Long> lastKill = new HashMap<>();

    public CombatListener(DuckCore plugin) {
        this.plugin = plugin;
    }

    private PerkManager perks() {
        return plugin.perks;
    }

    private static Player attackerOf(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        return null;
    }

    // =====================================================================
    //  WALKA - PERKI
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Player attacker = attackerOf(e.getDamager());
        if (attacker == null || attacker.equals(victim)) return;
        if (extraGuard.contains(attacker.getUniqueId())) return;

        PlayerData ad = plugin.data.get(attacker);
        PlayerData vd = plugin.data.get(victim);

        // Perk Blokady Uderzenia - ofiara blokuje atak
        if (perks().roll(vd, Perk.HIT_BLOCK)) {
            e.setCancelled(true);
            victim.sendActionBar(Util.mm("<aqua><bold>ZABLOKOWANO ATAK!"));
            attacker.sendActionBar(Util.mm("<red>Przeciwnik zablokował Twój atak!"));
            victim.playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
            return;
        }

        // Perk Podswietlenia - oznaczony przeciwnik dostaje wiecej obrazen
        if (perks().active(Perk.HIGHLIGHT, victim)) {
            e.setDamage(e.getDamage() * 1.25);
        }

        // Perk Lifesteal
        if (perks().roll(ad, Perk.LIFESTEAL)) {
            double max = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
            attacker.setHealth(Math.min(max, attacker.getHealth() + 2.0));
            attacker.spawnParticle(Particle.HEART, attacker.getLocation().add(0, 1.8, 0), 4, 0.3, 0.2, 0.3, 0);
            attacker.sendActionBar(Util.mm("<red>♥ Lifesteal!"));
        }

        // Negatywne efekty na przeciwniku (podlegaja Perkowi Odbicia)
        debuff(attacker, victim, ad, vd, Perk.HIGHLIGHT);
        debuff(attacker, victim, ad, vd, Perk.ROOT);
        debuff(attacker, victim, ad, vd, Perk.BLOCK_BREAK);
        debuff(attacker, victim, ad, vd, Perk.FIREWORK);
        debuff(attacker, victim, ad, vd, Perk.EVENT_BLOCK);

        // Perk Odpornosci
        if (perks().roll(vd, Perk.RESISTANCE)) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 0));
            victim.sendActionBar(Util.mm("<aqua>Odporność!"));
        }

        // Perk Extra Hit - podwojne uderzenie
        if (perks().roll(ad, Perk.EXTRA_HIT)) {
            final double dmg = e.getFinalDamage();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!victim.isOnline() || victim.isDead() || !attacker.isOnline()) return;
                extraGuard.add(attacker.getUniqueId());
                try {
                    victim.setNoDamageTicks(0);
                    victim.damage(dmg, attacker);
                } finally {
                    extraGuard.remove(attacker.getUniqueId());
                }
                victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.1);
                attacker.sendActionBar(Util.mm("<gold><bold>EXTRA HIT!"));
            }, 2L);
        }
    }

    private void debuff(Player attacker, Player victim, PlayerData ad, PlayerData vd, Perk perk) {
        if (!perks().roll(ad, perk)) return;
        Player target = victim;
        // Perk Odbicia - odbija negatywny efekt w strone atakujacego
        if (perks().roll(vd, Perk.REFLECT)) {
            target = attacker;
            victim.sendActionBar(Util.mm("<light_purple><bold>ODBITO EFEKT!"));
            attacker.sendActionBar(Util.mm("<red>Przeciwnik odbił Twój efekt!"));
            victim.playSound(victim.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
        }
        applyDebuff(perk, target);
    }

    private void applyDebuff(Perk perk, Player t) {
        switch (perk) {
            case HIGHLIGHT -> {
                perks().mark(perk, t, 4000);
                t.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 80, 0));
                t.sendActionBar(Util.mm("<yellow>Zostałeś oznaczony! <red>Otrzymujesz więcej obrażeń"));
            }
            case ROOT -> {
                t.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 250, false, false));
                t.sendActionBar(Util.mm("<yellow>Zostałeś spętany! <gray>Nie możesz skakać"));
            }
            case BLOCK_BREAK -> {
                perks().mark(perk, t, 5000);
                t.sendActionBar(Util.mm("<green>Nie możesz niszczyć bloków przez 5s!"));
            }
            case FIREWORK -> {
                perks().mark(perk, t, 5000);
                t.sendActionBar(Util.mm("<green>Nie możesz używać fajerwerek przez 5s!"));
            }
            case EVENT_BLOCK -> {
                perks().mark(perk, t, 8000);
                t.sendActionBar(Util.mm("<light_purple>Twoja eventówka została zablokowana!"));
            }
            default -> {
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (perks().active(Perk.BLOCK_BREAK, e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Util.mm("<red>Niszczenie bloków jest zablokowane!"));
        }
    }

    @EventHandler(ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack it = e.getItem();
        if (it == null || it.getType() != Material.FIREWORK_ROCKET) return;
        if (perks().active(Perk.FIREWORK, e.getPlayer())) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(Util.mm("<red>Fajerwerki są zablokowane!"));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent e) {
        String n = e.getItem().getType().name();
        boolean armor = n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE")
                || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS");
        if (!armor) return;
        if (perks().roll(plugin.data.get(e.getPlayer()), Perk.DURABILITY)) e.setCancelled(true);
    }

    // =====================================================================
    //  ZABOJSTWO / RANKING
    // =====================================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        PlayerData vd = plugin.data.get(victim);
        vd.deaths++;
        perks().clear(victim);

        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;
        PlayerData kd = plugin.data.get(killer);

        // ---- punkty rankingu ----
        int diff = vd.ranking - kd.ranking;
        int gain = (int) Math.max(5, Math.min(50, 20 + diff / 20.0));
        int loss = Math.max(3, (int) Math.round(gain * 0.7));

        String key = killer.getUniqueId() + ":" + victim.getUniqueId();
        long now = System.currentTimeMillis();
        long window = plugin.getConfig().getLong("ranking.anti-farm-seconds", 300) * 1000L;
        Long last = lastKill.get(key);
        boolean farm = last != null && now - last < window;
        lastKill.put(key, now);
        if (farm) {
            gain = 0;
            loss = 0;
        }

        kd.kills++;
        kd.ranking += gain;
        vd.ranking = Math.max(0, vd.ranking - loss);
        plugin.data.refreshPositions();

        // punkty klanu
        Clan kc = plugin.clans.of(kd);
        if (kc != null && !farm) kc.points += plugin.getConfig().getInt("clan.points-per-kill", 5);

        // ---- wiadomosci ----
        e.deathMessage(null);
        int pos = plugin.data.position(killer.getUniqueId());

        String sub = "<white>" + victim.getName() + " <green>+" + gain + " <gray>(<yellow>" + kd.ranking
                + "pkt <dark_gray>| <red>✝" + kd.kills + " <dark_gray>| <white>Pozycja: <aqua>#" + pos + "<gray>)";
        killer.showTitle(Title.title(
                Util.mm("<gradient:#ffb300:#ff6a00><bold>ZABÓJSTWO"),
                Util.mm(sub),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1400), Duration.ofMillis(300))));
        killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.6f);

        String line = "<red>☠ <white>" + victim.getName() + " <gray>zginął z rąk <yellow>" + killer.getName()
                + " <green>(+" + gain + " pkt)";
        Component centered = Util.center(line);
        Bukkit.getServer().sendMessage(centered);

        victim.sendMessage(Util.center("<red>Straciłeś <white>" + loss + " <red>pkt rankingu <gray>(" + vd.ranking + " pkt)"));
        if (farm) {
            killer.sendMessage(Util.center("<gray>Anti-farm: <red>brak punktów za zabicie tego samego gracza"));
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> perks().applyLife(e.getPlayer()), 2L);
    }

    // =====================================================================
    //  JOIN / QUIT
    // =====================================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.data.get(p); // tworzy dane
        perks().applyLife(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        perks().clear(e.getPlayer());
        plugin.board.remove(e.getPlayer());
    }
}
