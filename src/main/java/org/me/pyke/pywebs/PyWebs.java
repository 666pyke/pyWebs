package org.me.pyke.pywebs;

import org.bukkit.command.PluginCommand;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.me.pyke.pywebs.cobwebs.CobwebListener;
import org.me.pyke.pywebs.cobwebs.CobwebManager;
import org.me.pyke.pywebs.commands.PyWebsCommand;
import org.me.pyke.pywebs.utils.MessageManager;

public final class PyWebs extends JavaPlugin {

    private CobwebManager cobwebManager;
    private CobwebListener cobwebListener;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messageManager = new MessageManager(this);
        cobwebManager = new CobwebManager(this);
        cobwebListener = new CobwebListener(this, cobwebManager);

        getServer().getPluginManager().registerEvents(cobwebListener, this);

        if (getConfig().getBoolean("settings.metrics.enabled", true)) {
            new Metrics(this, 33003);
        }

        PluginCommand command = getCommand("pywebs");
        if (command != null) {
            PyWebsCommand pyWebsCommand = new PyWebsCommand(this);
            command.setExecutor(pyWebsCommand);
            command.setTabCompleter(pyWebsCommand);
        }
    }

    @Override
    public void onDisable() {
        if (cobwebListener != null) {
            cobwebListener.cleanupCobwebsAndDisplays();
        }
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public CobwebManager getCobwebManager() {
        return cobwebManager;
    }

    public CobwebListener getCobwebListener() {
        return cobwebListener;
    }
}
