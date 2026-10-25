package pl.duckmc.core;

import pl.duckmc.core.DataManager.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.function.IntConsumer;

public class Gui implements Listener {

    public enum Kind { SZAMAN, PERKS, ANIM, RESULT }

    public static class Holder implements InventoryHolder {
        public final Kind kind;
        public Inventory inv;

        Holder(Kind kind) {
            this.kind = kind;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private record PerkRoll(Perk perk, double gain, double applied) {}

    private static final int[] SZAMAN_BORDER_YELLOW = {0, 8, 36, 44};
    private static final int[] SZAMAN_BORDER_ORANGE = {1, 7, 9, 17, 27, 35, 37, 43};

    private final DuckCore plugin;
    private final Set<UUID> busy = new HashSet<>();
    private final Random rnd = new Random();

    public Gui(DuckCore plugin) {
        this.plugin = plugin;
    }

    // =====================================================================
    //  Helpers
    // =====================================================================

    public static ItemStack item(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Util.item(name));
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream().map(Util::item).toList());
        }
        meta.addItemFlags(ItemFlag.values());
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack item(Material m, String name, String... lore) {
        return item(m, name, Arrays.asList(lore));
    }

    private static ItemStack glow(ItemStack it) {
        ItemMeta m = it.getItemMeta();
        m.setEnchantmentGlintOverride(true);
        it.setItemMeta(m);
        return it;
    }

    private static ItemStack pane(Material m) {
        return item(m, " ");
    }

    private Inventory create(Holder h, int size, String title) {
        Inventory inv = Bukkit.createInventory(h, size, Util.mm(title));
        h.inv = inv;
        return inv;
    }

    private void fill(Inventory inv, Material m) {
        ItemStack p = pane(m);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, p);
    }

    private ItemStack withModel(ItemStack it, StoneType t) {
        int cmd = plugin.getConfig().getInt("stone-models." + t.name().toLowerCase(), 0);
        if (cmd > 0) {
            ItemMeta m = it.getItemMeta();
            m.setCustomModelData(cmd);
            it.setItemMeta(m);
        }
        return it;
    }

    private ItemStack stoneIcon(StoneType t, boolean glowing) {
        ItemStack it = withModel(item(t.item, t.stoneName()), t);
        return glowing ? glow(it) : it;
    }

    private boolean stillOpen(Player p, Holder h) {
        return p.isOnline() && p.getOpenInventory().getTopInventory().getHolder() == h;
    }

    private void click(Player p, Sound s, float pitch) {
        p.playSound(p.getLocation(), s, 0.7f, pitch);
    }

    private int cost() {
        return plugin.getConfig().getInt("draw.stone-cost", 10);
    }

    private int dropCost() {
        return plugin.getConfig().getInt("draw.drop-upgrade-cost", 5);
    }

    // =====================================================================
    //  SZAMAN
    // =====================================================================

    public void openShaman(Player p) {
        PlayerData d = plugin.data.get(p);
        Holder h = new Holder(Kind.SZAMAN);
        Inventory inv = create(h, 45, "<gradient:#a855f7:#7c3aed><bold>SZAMAN");
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);
        for (int s : SZAMAN_BORDER_YELLOW) inv.setItem(s, pane(Material.YELLOW_STAINED_GLASS_PANE));
        for (int s : SZAMAN_BORDER_ORANGE) inv.setItem(s, pane(Material.ORANGE_STAINED_GLASS_PANE));

        // koniczyna - Perk Dropu
        List<String> clover = new ArrayList<>();
        for (String line : Perk.DROP.desc) clover.add("<dark_gray>✖ " + line);
        clover.add(" ");
        clover.addAll(bonusLines(d, Perk.DROP));
        clover.add(" ");
        clover.add("<dark_gray>✖ <gray>Koszt: <white>x" + dropCost() + " " + Util.SHARD);
        clover.add(" ");
        clover.add("<green>Kliknij, aby dodać od <dark_green>" + Util.pct(plugin.getConfig().getDouble("draw.drop-min", 3))
                + "% <green>do <dark_green>" + Util.pct(plugin.getConfig().getDouble("draw.drop-max", 10)) + "% <green>bonusu!");
        inv.setItem(20, item(Material.FERN, "<#a3e635><bold>PERK DROPU", clover));

        // ksiega - Twoje perki
        inv.setItem(22, item(Material.ENCHANTED_BOOK, "<#a3ff00><bold>TWOJE PERKI",
                "<dark_gray>✖ <white>Tutaj znajdziesz swoje perki,",
                "<dark_gray>✖ <white>które możesz ulepszać!",
                " ",
                "<green>Kliknij <gray>LPM <green>aby przejść!"));

        // runa - losowanie kamienia
        inv.setItem(24, item(Material.FLINT, "<#c084fc><bold>LOSOWANIE KAMIENIA",
                "<dark_gray>✖ <gray>Koszt: <white>x" + cost() + " " + Util.SHARD,
                " ",
                "<gray>Posiadasz: <white>" + d.shards + " " + Util.SHARD,
                " ",
                "<green>Kliknij <gray>LPM <green>aby wylosować kamień!",
                "<green>Kliknij <gray>LPM + <red>SHIFT <green>aby wylosować 10 kamieni!"));

        p.openInventory(inv);
    }

    // =====================================================================
    //  TWOJE PERKI
    // =====================================================================

    private List<String> bonusLines(PlayerData d, Perk perk) {
        double own = plugin.perks.own(d, perk);
        double clan = plugin.perks.clanBonus(d);
        double tot = plugin.perks.total(d, perk);
        double co = plugin.perks.capOwn();
        double cc = plugin.perks.capClan();
        return List.of(
                "<dark_gray>↓ <green>Twój bonus: <yellow>" + Util.pct(own) + "%<gray>/<white>" + Util.pct(co) + "%",
                "<dark_gray>↓ <aqua>Bonus klanowy: <yellow>" + Util.pct(clan) + "%<gray>/<white>" + Util.pct(cc) + "%",
                "  <dark_gray>↳ <gold>Łącznie: <yellow>" + Util.pct(tot) + "%<gray>/<white>" + Util.pct(co + cc) + "%"
        );
    }

    private ItemStack perkIcon(Player p, Perk perk) {
        PlayerData d = plugin.data.get(p);
        List<String> lore = new ArrayList<>();
        for (String line : perk.desc) lore.add("<dark_gray>✖ " + line);
        lore.add(" ");
        lore.addAll(bonusLines(d, perk));
        ItemStack it = item(perk.stone.pane, perk.coloredName(), lore);
        return d.own(perk) > 0 ? glow(it) : it;
    }

    public void openPerks(Player p) {
        PlayerData d = plugin.data.get(p);
        Holder h = new Holder(Kind.PERKS);
        Inventory inv = create(h, 54, "<gradient:#a3ff00:#4ade80><bold>TWOJE PERKI");
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);

        inv.setItem(4, item(Material.PAPER, "<yellow><bold>JAK TO DZIAŁA?",
                "<dark_gray>✖ <white>Losuj <#c084fc>kamienie <white>u Szamana",
                "<dark_gray>✖ <white>Kliknij kamień na dole, aby",
                "<dark_gray>✖ <white>wylosować <green>perk <white>i jego <green>bonus",
                "<dark_gray>✖ <gray>Bonus z losowania: <yellow>"
                        + Util.pct(plugin.getConfig().getDouble("draw.perk-min", 0.1)) + "% - "
                        + Util.pct(plugin.getConfig().getDouble("draw.perk-max", 10)) + "%"));

        for (StoneType t : StoneType.values()) {
            int col = t.column();
            List<Perk> perks = t.perks();
            for (int row = 0; row < 3; row++) {
                int slot = 9 * (row + 1) + col;
                if (row < perks.size()) inv.setItem(slot, perkIcon(p, perks.get(row)));
                else inv.setItem(slot, pane(t.pane));
            }
            int have = d.stones(t);
            ItemStack stone = item(t.item, t.stoneName(),
                    "<gray>Posiadasz: <white>" + have + "x",
                    " ",
                    "<dark_gray>≡ <gray>Koszt:",
                    "<dark_gray>↪ <white>1x " + t.stoneName(),
                    " ",
                    "<green>Kliknij <gray>LPM <green>aby wylosować perk!",
                    "<green>Kliknij <gray>LPM + <red>SHIFT <green>aby wylosować 10 perków!",
                    " ",
                    "<dark_purple><bold>EPICKI");
            withModel(stone, t);
            stone.setAmount(Math.max(1, Math.min(64, have)));
            inv.setItem(36 + col, have > 0 ? glow(stone) : stone);
        }

        inv.setItem(49, item(Material.ARROW, "<red>« Wróć", "<gray>Wróć do Szamana"));
        p.openInventory(inv);
    }

    // =====================================================================
    //  KLIKNIECIA
    // =====================================================================

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Holder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getInventory()) return;
        if (busy.contains(p.getUniqueId())) return;

        int s = e.getSlot();
        boolean shift = e.isShiftClick();

        switch (h.kind) {
            case SZAMAN -> {
                if (s == 20) upgradeDrop(p);
                else if (s == 22) {
                    click(p, Sound.UI_BUTTON_CLICK, 1f);
                    openPerks(p);
                } else if (s == 24) drawStones(p, shift ? 10 : 1);
            }
            case PERKS -> {
                if (s == 49) {
                    click(p, Sound.UI_BUTTON_CLICK, 1f);
                    openShaman(p);
                    return;
                }
                for (StoneType t : StoneType.values()) {
                    if (s == 36 + t.column()) {
                        drawPerks(p, t, shift ? 10 : 1);
                        return;
                    }
                }
            }
            case RESULT -> {
                if (s == 22) {
                    click(p, Sound.UI_BUTTON_CLICK, 1f);
                    openPerks(p);
                }
            }
            default -> {
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        // animacje i tak sa tylko wizualne - wynik jest juz zapisany, wiec nic nie trzeba cofac
    }

    // =====================================================================
    //  PERK DROPU
    // =====================================================================

    private void upgradeDrop(Player p) {
        PlayerData d = plugin.data.get(p);
        int c = dropCost();
        if (d.shards < c) {
            noShards(p, c);
            return;
        }
        if (d.own(Perk.DROP) >= plugin.perks.capOwn()) {
            p.sendMessage(Util.mm(Util.PREFIX + "<red>Perk Dropu ma już maksymalny bonus!"));
            click(p, Sound.ENTITY_VILLAGER_NO, 1f);
            return;
        }
        d.shards -= c;
        double gain = plugin.perks.rollDropGain();
        double applied = plugin.perks.addBonus(d, Perk.DROP, gain);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
        p.spawnParticle(Particle.HAPPY_VILLAGER, p.getLocation().add(0, 1.2, 0), 25, 0.5, 0.5, 0.5, 0.02);
        p.sendMessage(Util.mm(Util.PREFIX + "<gray>Ulepszono " + Perk.DROP.coloredName() + " <green>+" + Util.pct(applied)
                + "% <gray>(łącznie: <yellow>" + Util.pct(plugin.perks.total(d, Perk.DROP)) + "%<gray>)"));
        openShaman(p);
    }

    private void noShards(Player p, int need) {
        click(p, Sound.ENTITY_VILLAGER_NO, 1f);
        p.sendMessage(Util.mm(Util.PREFIX + "<red>Potrzebujesz <white>x" + need + " " + Util.SHARD
                + "<red>! <gray>(Zakup w sklepie serwera)"));
    }

    // =====================================================================
    //  LOSOWANIE KAMIENI
    // =====================================================================

    private void drawStones(Player p, int count) {
        PlayerData d = plugin.data.get(p);
        long total = (long) cost() * count;
        if (d.shards < total) {
            noShards(p, (int) total);
            return;
        }
        d.shards -= total;

        List<StoneType> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            StoneType t = StoneType.values()[rnd.nextInt(StoneType.values().length)];
            d.stones.merge(t, 1, Integer::sum);
            results.add(t);
        }
        if (count == 1) animateStoneSingle(p, results.get(0));
        else animateStoneMulti(p, results);
    }

    /** Uruchamia animacje krok po kroku z rosnacym opoznieniem (efekt ruletki). */
    private void seq(Player p, Holder h, int[] delays, int i, IntConsumer step, Runnable done) {
        if (!stillOpen(p, h)) {
            done.run();
            return;
        }
        if (i >= delays.length) {
            done.run();
            return;
        }
        step.accept(i);
        Bukkit.getScheduler().runTaskLater(plugin, () -> seq(p, h, delays, i + 1, step, done), delays[i]);
    }

    private void animateStoneSingle(Player p, StoneType winner) {
        Holder h = new Holder(Kind.ANIM);
        Inventory inv = create(h, 27, "<gradient:#a855f7:#7c3aed><bold>LOSOWANIE KAMIENIA");
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(4, item(Material.END_ROD, "<yellow>▼"));
        inv.setItem(22, item(Material.END_ROD, "<yellow>▲"));
        busy.add(p.getUniqueId());
        p.openInventory(inv);

        int[] delays = {2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 5, 5, 6, 7, 8, 10, 12};
        int n = delays.length;
        StoneType[] sq = new StoneType[n + 9];
        for (int i = 0; i < sq.length; i++) sq[i] = StoneType.values()[rnd.nextInt(StoneType.values().length)];
        sq[n + 3] = winner; // srodek ostatniego kroku

        seq(p, h, delays, 0, i -> {
            for (int k = 0; k < 9; k++) {
                inv.setItem(9 + k, stoneIcon(sq[i + k], k == 4));
            }
            float pitch = 0.6f + 1.2f * ((float) i / n);
            click(p, Sound.BLOCK_NOTE_BLOCK_HAT, pitch);
        }, () -> {
            busy.remove(p.getUniqueId());
            p.sendMessage(Util.mm(Util.PREFIX + "<gray>Wylosowałeś: " + winner.stoneName()));
            if (stillOpen(p, h)) {
                ItemStack bg = pane(winner.pane);
                for (int s = 0; s < 27; s++) if (s < 9 || s > 17) inv.setItem(s, bg);
                inv.setItem(13, glow(stoneIcon(winner, true)));
                celebrate(p, winner.hex);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (stillOpen(p, h)) openShaman(p);
                }, 50L);
            }
        });
    }

    private void animateStoneMulti(Player p, List<StoneType> results) {
        Holder h = new Holder(Kind.ANIM);
        Inventory inv = create(h, 27, "<gradient:#a855f7:#7c3aed><bold>LOSOWANIE KAMIENI (x" + results.size() + ")");
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        int[] slots = {2, 3, 4, 5, 6, 11, 12, 13, 14, 15};
        for (int i = 0; i < results.size(); i++) {
            inv.setItem(slots[i], item(Material.GRAY_STAINED_GLASS_PANE, "<gray>?"));
        }
        busy.add(p.getUniqueId());
        p.openInventory(inv);

        int[] delays = new int[results.size()];
        Arrays.fill(delays, 5);

        seq(p, h, delays, 0, i -> {
            StoneType t = results.get(i);
            inv.setItem(slots[i], glow(stoneIcon(t, true)));
            click(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f + 0.08f * i);
            p.spawnParticle(Particle.END_ROD, p.getLocation().add(0, 1.5, 0), 6, 0.4, 0.4, 0.4, 0.02);
        }, () -> {
            busy.remove(p.getUniqueId());
            EnumMap<StoneType, Integer> sum = new EnumMap<>(StoneType.class);
            for (StoneType t : results) sum.merge(t, 1, Integer::sum);
            StringBuilder sb = new StringBuilder();
            sum.forEach((t, c) -> sb.append(t.tag()).append(c).append("x ").append(t.label).append("<gray>, "));
            String line = sb.length() > 2 ? sb.substring(0, sb.length() - 2) : "";
            p.sendMessage(Util.mm(Util.PREFIX + "<gray>Wylosowano kamienie: " + line));
            if (stillOpen(p, h)) {
                celebrate(p, "#c084fc");
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (stillOpen(p, h)) openShaman(p);
                }, 50L);
            }
        });
    }

    // =====================================================================
    //  LOSOWANIE PERKOW
    // =====================================================================

    private void drawPerks(Player p, StoneType t, int requested) {
        PlayerData d = plugin.data.get(p);
        int have = d.stones(t);
        int count = Math.min(requested, have);
        if (count <= 0) {
            click(p, Sound.ENTITY_VILLAGER_NO, 1f);
            p.sendMessage(Util.mm(Util.PREFIX + "<red>Nie masz kamieni: " + t.stoneName()
                    + "<red>! <gray>Wylosuj je u Szamana."));
            return;
        }
        d.stones.merge(t, -count, Integer::sum);

        List<Perk> pool = t.perks();
        List<PerkRoll> rolls = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Perk perk = pool.get(rnd.nextInt(pool.size()));
            double gain = plugin.perks.rollGain();
            double applied = plugin.perks.addBonus(d, perk, gain);
            rolls.add(new PerkRoll(perk, gain, applied));
        }
        plugin.perks.applyLife(p);

        if (count == 1) animatePerkSingle(p, t, rolls.get(0));
        else animatePerkMulti(p, t, rolls);
    }

    private void animatePerkSingle(Player p, StoneType t, PerkRoll r) {
        PlayerData d = plugin.data.get(p);
        Holder h = new Holder(Kind.ANIM);
        Inventory inv = create(h, 27, t.tag() + "<bold>LOSOWANIE PERKA");
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        busy.add(p.getUniqueId());
        p.openInventory(inv);

        List<Perk> pool = t.perks();
        int[] delays = {2, 2, 2, 2, 2, 3, 3, 3, 4, 4, 5, 6, 8, 10};
        int[] border = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26};

        seq(p, h, delays, 0, i -> {
            Perk shown = (i == delays.length - 1) ? r.perk() : pool.get(rnd.nextInt(pool.size()));
            inv.setItem(13, item(t.item, shown.coloredName(), "<gray>Losowanie..."));
            ItemStack b = pane(i % 2 == 0 ? t.pane : Material.BLACK_STAINED_GLASS_PANE);
            for (int s : border) inv.setItem(s, b);
            float pitch = 0.6f + 1.2f * ((float) i / delays.length);
            click(p, Sound.BLOCK_NOTE_BLOCK_PLING, pitch);
        }, () -> {
            busy.remove(p.getUniqueId());
            double total = d.own(r.perk());
            String note = r.applied() < r.gain() ? " <red>(limit " + Util.pct(plugin.perks.capOwn()) + "%)" : "";
            p.sendMessage(Util.mm(Util.PREFIX + "<gray>Perk: " + r.perk().coloredName() + " <green>+" + Util.pct(r.applied())
                    + "%" + note + " <gray>(masz: <yellow>" + Util.pct(total) + "%<gray>/<white>"
                    + Util.pct(plugin.perks.capOwn()) + "%<gray>)"));
            if (stillOpen(p, h)) {
                ItemStack b = pane(t.pane);
                for (int s : border) inv.setItem(s, b);
                inv.setItem(13, glow(item(t.item, r.perk().coloredName(),
                        "<green><bold>+" + Util.pct(r.applied()) + "%",
                        " ",
                        "<gray>Masz łącznie: <yellow>" + Util.pct(total) + "%")));
                celebrate(p, t.hex);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (stillOpen(p, h)) openPerks(p);
                }, 50L);
            }
        });
    }

    private void animatePerkMulti(Player p, StoneType t, List<PerkRoll> rolls) {
        PlayerData d = plugin.data.get(p);
        Holder h = new Holder(Kind.ANIM);
        Inventory inv = create(h, 27, t.tag() + "<bold>LOSOWANIE PERKÓW (x" + rolls.size() + ")");
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        busy.add(p.getUniqueId());
        p.openInventory(inv);

        List<Perk> pool = t.perks();
        int[] delays = new int[16];
        Arrays.fill(delays, 3);

        seq(p, h, delays, 0, i -> {
            Perk shown = pool.get(rnd.nextInt(pool.size()));
            inv.setItem(13, item(t.item, shown.coloredName(), "<gray>Losowanie..."));
            ItemStack b = pane(i % 2 == 0 ? t.pane : Material.BLACK_STAINED_GLASS_PANE);
            for (int s = 0; s < 27; s++) if (s != 13) inv.setItem(s, b);
            click(p, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f + 0.05f * i);
        }, () -> {
            busy.remove(p.getUniqueId());
            LinkedHashMap<Perk, double[]> sum = new LinkedHashMap<>(); // [gain applied, count]
            for (PerkRoll r : rolls) {
                double[] a = sum.computeIfAbsent(r.perk(), k -> new double[2]);
                a[0] += r.applied();
                a[1] += 1;
            }
            p.sendMessage(Util.mm(Util.PREFIX + "<gray>Wylosowano <white>" + rolls.size() + " <gray>perków:"));
            sum.forEach((perk, a) -> p.sendMessage(Util.mm("  <dark_gray>» " + perk.coloredName() + " <green>+"
                    + Util.pct(Util.round2(a[0])) + "% <gray>(" + (int) a[1] + "x, masz: <yellow>"
                    + Util.pct(d.own(perk)) + "%<gray>)")));
            if (stillOpen(p, h)) openResult(p, t, sum);
        });
    }

    private void openResult(Player p, StoneType t, Map<Perk, double[]> sum) {
        PlayerData d = plugin.data.get(p);
        Holder h = new Holder(Kind.RESULT);
        Inventory inv = create(h, 27, t.tag() + "<bold>WYNIKI LOSOWANIA");
        fill(inv, Material.BLACK_STAINED_GLASS_PANE);
        for (int s = 0; s < 27; s++) if (s < 9 || s > 17) inv.setItem(s, pane(t.pane));

        int n = sum.size();
        int[] slots = n == 1 ? new int[]{13} : n == 2 ? new int[]{12, 14} : new int[]{11, 13, 15};
        int i = 0;
        for (Map.Entry<Perk, double[]> e : sum.entrySet()) {
            if (i >= slots.length) break;
            Perk perk = e.getKey();
            double[] a = e.getValue();
            inv.setItem(slots[i++], glow(item(t.item, perk.coloredName(),
                    "<green><bold>+" + Util.pct(Util.round2(a[0])) + "%",
                    "<gray>Trafień: <white>" + (int) a[1] + "x",
                    " ",
                    "<gray>Masz łącznie: <yellow>" + Util.pct(d.own(perk)) + "%")));
        }
        inv.setItem(22, item(Material.ARROW, "<red>« Wróć", "<gray>Wróć do perków"));
        p.openInventory(inv);
        celebrate(p, t.hex);
    }

    // =====================================================================
    //  EFEKTY
    // =====================================================================

    private void celebrate(Player p, String hex) {
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.2f);
        p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1f, 1f);
        p.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1.2, 0), 40, 0.5, 0.7, 0.5, 0.3);
        try {
            Color c = Color.fromRGB(Integer.parseInt(hex.substring(1), 16));
            Firework fw = p.getWorld().spawn(p.getLocation().add(0, 1, 0), Firework.class);
            FireworkMeta meta = fw.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(c)
                    .withFade(Color.WHITE).withFlicker().build());
            fw.setFireworkMeta(meta);
            fw.setSilent(true);
            Bukkit.getScheduler().runTaskLater(plugin, fw::detonate, 1L);
        } catch (Exception ignored) {
        }
    }
}
