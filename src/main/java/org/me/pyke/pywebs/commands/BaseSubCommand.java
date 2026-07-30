package org.me.pyke.pywebs.commands;

import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public abstract class BaseSubCommand {
    public abstract String getName();

    public abstract String getDescription();

    public abstract String getUsage();

    public List<String> getAliases() {
        return Collections.emptyList();
    }

    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    public abstract void execute(CommandSender sender, String[] args);
}
