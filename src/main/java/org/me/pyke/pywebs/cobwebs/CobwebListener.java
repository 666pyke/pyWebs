package org.me.pyke.pywebs.cobwebs;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.me.pyke.pywebs.PyWebs;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CobwebListener implements Listener {

    private final PyWebs plugin;
    private final CobwebManager cobwebManager;
    private final NamespacedKey cobwebKey;
    private final Map<Location, ActiveCobweb> activeCobwebs = new HashMap<>();
    private final Set<String> disabledWorlds = new HashSet<>();
    private BukkitTask cobwebTicker;
    private boolean removeActiveCobwebsOnDisable;
    private boolean blockFireworksInCobweb;
    private boolean cobwebPlacedMessageEnabled;
    private boolean progressDisplayEnabled;
    private String progressStart;
    private String progressMiddle;
    private String progressEnd;

    public CobwebListener(PyWebs plugin, CobwebManager cobwebManager) {
        this.plugin = plugin;
        this.cobwebManager = cobwebManager;
        this.cobwebKey = new NamespacedKey(plugin, "cobweb_key");
        reloadSettings();
    }

    public void reloadSettings() {
        FileConfiguration config = plugin.getConfig();

        disabledWorlds.clear();
        List<String> worlds = config.getStringList("settings.disabled-worlds");
        for (String world : worlds) {
            disabledWorlds.add(world.toLowerCase(Locale.ROOT));
        }

        removeActiveCobwebsOnDisable = config.getBoolean("settings.remove-active-cobwebs-on-disable", true);
        blockFireworksInCobweb = config.getBoolean("settings.block-fireworks-in-cobweb", true);
        cobwebPlacedMessageEnabled = config.getBoolean("settings.messages.cobweb-placed", true);
        progressDisplayEnabled = config.getBoolean("settings.progress-display.enabled", true);
        progressStart = color(config.getString("settings.progress-display.start", "&a||||||||||||||||||||||||||"));
        progressMiddle = color(config.getString("settings.progress-display.middle", "&e||||||||||||||||||||||||||"));
        progressEnd = color(config.getString("settings.progress-display.end", "&c||||||||||||||||||||||||||"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCobwebPlace(BlockPlaceEvent event) {
        if (isDisabledWorld(event.getBlock().getWorld().getName())) {
            return;
        }

        ItemStack itemInHand = event.getItemInHand();
        if (itemInHand.getType() != Material.COBWEB) {
            return;
        }

        CobwebManager.CobwebTier cobwebTier = getTier(itemInHand);
        if (cobwebTier == null) {
            plugin.getMessageManager().send(event.getPlayer(), "cobweb.tier-not-recognized", new HashMap<String, String>());
            event.setCancelled(true);
            return;
        }

        Location blockLocation = event.getBlock().getLocation();
        removeActiveCobweb(blockLocation, false);

        TextDisplay display = null;
        if (progressDisplayEnabled) {
            display = createTextDisplay(blockLocation.clone().add(0.5, 1, 0.5));
        }

        ActiveCobweb activeCobweb = new ActiveCobweb(display, cobwebTier.getDespawnTime());
        activeCobwebs.put(blockLocation, activeCobweb);
        startCobwebTicker();

        if (cobwebPlacedMessageEnabled) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("time", String.valueOf(cobwebTier.getDespawnTime()));
            plugin.getMessageManager().send(event.getPlayer(), "cobweb.placed", placeholders);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Location blockLocation = event.getBlock().getLocation();
        if (event.getBlock().getType() == Material.COBWEB) {
            removeActiveCobweb(blockLocation, true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFireworkUse(PlayerInteractEvent event) {
        if (!blockFireworksInCobweb || isDisabledWorld(event.getPlayer().getWorld().getName())) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.FIREWORK_ROCKET) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.isGliding() || !isInCobweb(player)) {
            return;
        }

        event.setCancelled(true);
        String message = plugin.getMessageManager().getMessage(
                "cobweb.firework-blocked",
                "&cYou cannot use fireworks while stuck in a cobweb!"
        );
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(message));
    }

    public void cleanupCobwebsAndDisplays() {
        for (Map.Entry<Location, ActiveCobweb> entry : new HashMap<>(activeCobwebs).entrySet()) {
            Location location = entry.getKey();
            ActiveCobweb activeCobweb = entry.getValue();

            if (removeActiveCobwebsOnDisable && location.getBlock().getType() == Material.COBWEB) {
                location.getBlock().setType(Material.AIR);
            }

            removeDisplay(activeCobweb.getDisplay());
        }
        activeCobwebs.clear();
        stopCobwebTicker();
    }

    private CobwebManager.CobwebTier getTier(ItemStack itemStack) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return cobwebManager.getDefaultCobweb();
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        String nbtKey = data.get(cobwebKey, PersistentDataType.STRING);

        return nbtKey == null ? cobwebManager.getDefaultCobweb() : cobwebManager.getCobwebTier(nbtKey);
    }

    private void startCobwebTicker() {
        if (cobwebTicker != null && !cobwebTicker.isCancelled()) {
            return;
        }

        cobwebTicker = new BukkitRunnable() {
            @Override
            public void run() {
                tickCobwebs();
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void stopCobwebTicker() {
        if (cobwebTicker != null) {
            cobwebTicker.cancel();
            cobwebTicker = null;
        }
    }

    private void tickCobwebs() {
        if (activeCobwebs.isEmpty()) {
            stopCobwebTicker();
            return;
        }

        Iterator<Map.Entry<Location, ActiveCobweb>> iterator = activeCobwebs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Location, ActiveCobweb> entry = iterator.next();
            Location blockLocation = entry.getKey();
            ActiveCobweb activeCobweb = entry.getValue();

            activeCobweb.tick();

            if (activeCobweb.isExpired()) {
                if (blockLocation.getBlock().getType() == Material.COBWEB) {
                    blockLocation.getBlock().setType(Material.AIR);
                }
                removeDisplay(activeCobweb.getDisplay());
                iterator.remove();
                continue;
            }

            updateDisplay(activeCobweb);
        }

        if (activeCobwebs.isEmpty()) {
            stopCobwebTicker();
        }
    }

    private void updateDisplay(ActiveCobweb activeCobweb) {
        TextDisplay display = activeCobweb.getDisplay();
        if (display == null || display.isDead()) {
            return;
        }

        String progressBar = progressStart;
        if (activeCobweb.getElapsed() >= activeCobweb.getWarningTime() * 2) {
            progressBar = progressEnd;
        } else if (activeCobweb.getElapsed() >= activeCobweb.getWarningTime()) {
            progressBar = progressMiddle;
        }

        display.setCustomName(progressBar);
    }

    private TextDisplay createTextDisplay(Location location) {
        return Objects.requireNonNull(location.getWorld()).spawn(location, TextDisplay.class, entity -> {
            entity.setCustomName(progressStart);
            entity.setCustomNameVisible(true);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setGravity(false);
            entity.setSilent(true);
        });
    }

    private void removeActiveCobweb(Location blockLocation, boolean stopTickerIfEmpty) {
        ActiveCobweb activeCobweb = activeCobwebs.remove(blockLocation);
        if (activeCobweb == null) {
            return;
        }

        removeDisplay(activeCobweb.getDisplay());

        if (stopTickerIfEmpty && activeCobwebs.isEmpty()) {
            stopCobwebTicker();
        }
    }

    private void removeDisplay(TextDisplay display) {
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }

    private boolean isInCobweb(Player player) {
        Location feet = player.getLocation();
        Location body = player.getLocation().clone().add(0, 1, 0);

        return feet.getBlock().getType() == Material.COBWEB
                || body.getBlock().getType() == Material.COBWEB;
    }

    private boolean isDisabledWorld(String worldName) {
        return disabledWorlds.contains(worldName.toLowerCase(Locale.ROOT));
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private static final class ActiveCobweb {
        private final TextDisplay display;
        private final int totalTime;
        private final int warningTime;
        private int elapsed;

        private ActiveCobweb(TextDisplay display, int totalTime) {
            this.display = display;
            this.totalTime = totalTime;
            this.warningTime = Math.max(1, totalTime / 3);
        }

        private TextDisplay getDisplay() {
            return display;
        }

        private int getElapsed() {
            return elapsed;
        }

        private int getWarningTime() {
            return warningTime;
        }

        private void tick() {
            elapsed++;
        }

        private boolean isExpired() {
            return elapsed >= totalTime;
        }
    }
}
