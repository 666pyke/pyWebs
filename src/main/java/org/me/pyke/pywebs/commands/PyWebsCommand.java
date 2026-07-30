package org.me.pyke.pywebs.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.me.pyke.pywebs.PyWebs;
import org.me.pyke.pywebs.commands.subcommands.CobwebSubCommand;
import org.me.pyke.pywebs.commands.subcommands.ReloadSubCommand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PyWebsCommand implements CommandExecutor, TabCompleter {

    private final PyWebs plugin;
    private final Map<String, BaseSubCommand> subCommands = new LinkedHashMap<>();

    public PyWebsCommand(PyWebs plugin) {
        this.plugin = plugin;
        register(new CobwebSubCommand(plugin));
        register(new ReloadSubCommand(plugin));
    }

    private void register(BaseSubCommand subCommand) {
        subCommands.put(subCommand.getName().toLowerCase(Locale.ROOT), subCommand);
        for (String alias : subCommand.getAliases()) {
            subCommands.put(alias.toLowerCase(Locale.ROOT), subCommand);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pywebs.use")) {
            plugin.getMessageManager().send(sender, "command.no-permission", placeholders());
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        BaseSubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            plugin.getMessageManager().send(sender, "command.unknown", placeholders());
            sendUsage(sender);
            return true;
        }

        try {
            subCommand.execute(sender, args);
        } catch (Exception exception) {
            Map<String, String> placeholders = placeholders();
            placeholders.put("error", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
            plugin.getMessageManager().send(sender, "command.error", placeholders);
            exception.printStackTrace();
        }
        return true;
    }

    private void sendUsage(CommandSender sender) {
        plugin.getMessageManager().sendList(sender, "command.usage", placeholders());
        plugin.getMessageManager().sendRaw(sender, "", placeholders());
        plugin.getMessageManager().sendRaw(sender, "&7&omade with <3 by 666pyke", placeholders());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pywebs.use")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return filter(primaryCommandNames(), args[0]);
        }

        BaseSubCommand subCommand = subCommands.get(args[0].toLowerCase(Locale.ROOT));
        if (subCommand == null) {
            return new ArrayList<>();
        }

        return filter(subCommand.tabComplete(sender, args), args[args.length - 1]);
    }

    private List<String> primaryCommandNames() {
        List<String> names = new ArrayList<>();
        for (BaseSubCommand subCommand : subCommands.values()) {
            if (!names.contains(subCommand.getName())) {
                names.add(subCommand.getName());
            }
        }
        return names;
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }

    private static Map<String, String> placeholders() {
        return new LinkedHashMap<>();
    }
}
