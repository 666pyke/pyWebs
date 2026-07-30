package org.me.pyke.pywebs.commands.subcommands;

import org.bukkit.command.CommandSender;
import org.me.pyke.pywebs.PyWebs;
import org.me.pyke.pywebs.commands.BaseSubCommand;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ReloadSubCommand extends BaseSubCommand {

    private final PyWebs plugin;

    public ReloadSubCommand(PyWebs plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reload pyWebs configuration.";
    }

    @Override
    public String getUsage() {
        return "/pywebs reload";
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pywebs.reload")) {
            plugin.getMessageManager().send(sender, "command.no-permission", placeholders());
            return;
        }

        plugin.reloadConfig();
        plugin.getMessageManager().loadMessages();
        plugin.getCobwebManager().loadCobwebs();
        plugin.getCobwebListener().reloadSettings();

        plugin.getMessageManager().send(sender, "command.reload.success", placeholders());
    }

    private static Map<String, String> placeholders() {
        return new LinkedHashMap<>();
    }
}
