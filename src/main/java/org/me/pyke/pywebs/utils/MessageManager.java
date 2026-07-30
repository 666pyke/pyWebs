package org.me.pyke.pywebs.utils;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MessageManager {

    private final JavaPlugin plugin;
    private final File messageFile;
    private FileConfiguration messagesConfig;
    private final Map<String, String> messagesCache = new HashMap<>();
    private final Map<String, List<String>> messagesListCache = new HashMap<>();

    public MessageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messageFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messageFile.exists()) {
            try {
                if (messageFile.getParentFile() != null) {
                    messageFile.getParentFile().mkdirs();
                }
                plugin.saveResource("messages.yml", false);
            } catch (Exception exception) {
                plugin.getLogger().severe("Could not create messages.yml!");
                exception.printStackTrace();
            }
        }

        loadMessages();
    }

    public void loadMessages() {
        messagesConfig = YamlConfiguration.loadConfiguration(messageFile);
        messagesCache.clear();
        messagesListCache.clear();

        for (String key : messagesConfig.getKeys(true)) {
            if (messagesConfig.isString(key)) {
                messagesCache.put(key, Lang.color(Objects.requireNonNull(messagesConfig.getString(key))));
            } else if (messagesConfig.isList(key)) {
                List<String> list = new ArrayList<>(messagesConfig.getStringList(key));
                list.replaceAll(Lang::color);
                messagesListCache.put(key, list);
            }
        }

        plugin.getLogger().info("[pyWebs] Messages reloaded. Total: " + messagesCache.size() + " keys.");
    }

    public String getMessage(String key, String defaultValue) {
        return messagesCache.getOrDefault(key, Lang.color(defaultValue));
    }

    public List<String> getMessageList(String key, List<String> defaultValue) {
        return messagesListCache.getOrDefault(key, defaultValue);
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sendRaw(sender, getMessage(key, ""), placeholders);
    }

    public void sendList(CommandSender sender, String key, Map<String, String> placeholders) {
        List<String> messages = getMessageList(key, Collections.emptyList());
        for (String message : messages) {
            sendRaw(sender, message, placeholders);
        }
    }

    public void sendRaw(CommandSender sender, String message, Map<String, String> placeholders) {
        sender.sendMessage(Lang.placeholders(Lang.color(message), placeholders));
    }

    public void saveMessages() {
        try {
            messagesConfig.save(messageFile);
        } catch (IOException exception) {
            plugin.getLogger().severe("[pyWebs] Could not save messages.yml!");
            exception.printStackTrace();
        }
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

}
