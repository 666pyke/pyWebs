package org.me.pyke.pywebs.cobwebs;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.me.pyke.pywebs.PyWebs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CobwebManager {

    private final PyWebs plugin;
    private final Map<String, CobwebTier> cobwebTiers = new LinkedHashMap<>();
    private CobwebTier defaultCobweb;

    public CobwebManager(PyWebs plugin) {
        this.plugin = plugin;
        loadCobwebs();
    }

    public void loadCobwebs() {
        cobwebTiers.clear();
        defaultCobweb = null;

        FileConfiguration config = plugin.getConfig();
        ConfigurationSection section = config.getConfigurationSection("cobwebs.tiers");
        boolean legacyConfig = false;

        if (section == null) {
            section = config.getConfigurationSection("cobwebs");
            legacyConfig = true;
        }

        if (section == null) {
            plugin.getLogger().warning("[pyWebs] Missing config section: cobwebs.tiers");
            return;
        }

        String defaultKey = config.getString("cobwebs.default", "cobweb_1");
        CobwebTier defaultByConfigKey = null;

        for (String tierKey : section.getKeys(false)) {
            if (tierKey.equalsIgnoreCase("default")) {
                continue;
            }

            String path = legacyConfig ? "cobwebs." + tierKey + "." : "cobwebs.tiers." + tierKey + ".";
            String name = color(config.getString(path + "name", "&fCobweb"));
            List<String> lore = colorList(config.getStringList(path + "lore"));
            String nbtKey = config.getString(path + "nbt-key", tierKey);
            int despawnTime = Math.max(1, config.getInt(path + "despawn-time", 5));

            CobwebTier tier = new CobwebTier(name, lore, nbtKey, despawnTime);
            cobwebTiers.put(nbtKey, tier);

            if (tierKey.equalsIgnoreCase(defaultKey)) {
                defaultByConfigKey = tier;
            }
        }

        defaultCobweb = cobwebTiers.get(defaultKey);
        if (defaultCobweb == null) {
            defaultCobweb = defaultByConfigKey;
        }

        if (defaultCobweb == null && !cobwebTiers.isEmpty()) {
            plugin.getLogger().warning(color(plugin.getMessageManager().getMessage(
                    "cobweb.default-fallback",
                    "&cNo default cobweb tier found! Using the first loaded tier."
            )));
            defaultCobweb = cobwebTiers.values().iterator().next();
        }
    }

    public CobwebTier getCobwebTier(String nbtKey) {
        return cobwebTiers.get(nbtKey);
    }

    public CobwebTier getDefaultCobweb() {
        return defaultCobweb;
    }

    public List<String> getCobwebKeys() {
        return new ArrayList<>(cobwebTiers.keySet());
    }

    public ItemStack createCobwebItem(CobwebTier tier) {
        ItemStack cobwebItem = new ItemStack(Material.COBWEB);
        ItemMeta meta = cobwebItem.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(tier.getName());
            meta.setLore(tier.getLore());

            PersistentDataContainer data = meta.getPersistentDataContainer();
            data.set(new NamespacedKey(plugin, "cobweb_key"), PersistentDataType.STRING, tier.getNbtKey());

            cobwebItem.setItemMeta(meta);
        }

        return cobwebItem;
    }

    public CobwebTier getCobwebTierByTierName(String tierName) {
        return getCobwebTier(tierName);
    }

    private static List<String> colorList(List<String> lines) {
        if (lines == null) {
            return Collections.emptyList();
        }

        List<String> colored = new ArrayList<>();
        for (String line : lines) {
            colored.add(color(line));
        }
        return colored;
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    public static class CobwebTier {
        private final String name;
        private final List<String> lore;
        private final String nbtKey;
        private final int despawnTime;

        public CobwebTier(String name, List<String> lore, String nbtKey, int despawnTime) {
            this.name = name;
            this.lore = lore;
            this.nbtKey = nbtKey;
            this.despawnTime = despawnTime;
        }

        public String getName() {
            return name;
        }

        public List<String> getLore() {
            return lore;
        }

        public String getNbtKey() {
            return nbtKey;
        }

        public int getDespawnTime() {
            return despawnTime;
        }
    }
}
