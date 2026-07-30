package org.me.pyke.pywebs.commands.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.me.pyke.pywebs.PyWebs;
import org.me.pyke.pywebs.cobwebs.CobwebManager;
import org.me.pyke.pywebs.commands.BaseSubCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CobwebSubCommand extends BaseSubCommand {

    private final PyWebs plugin;

    public CobwebSubCommand(PyWebs plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getName() {
        return "cobweb";
    }

    @Override
    public String getDescription() {
        return "Manage custom cobwebs.";
    }

    @Override
    public String getUsage() {
        return "/pywebs cobweb give <player> <nbt-key> [amount]";
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Arrays.asList("give");
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                names.add(player.getName());
            }
            return names;
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("give")) {
            return plugin.getCobwebManager().getCobwebKeys();
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("give")) {
            return Arrays.asList("1", "8", "16", "32", "64");
        }
        return new ArrayList<>();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("give")) {
            plugin.getMessageManager().send(sender, "cobweb.usage", placeholders());
            return;
        }

        give(sender, args);
    }

    private void give(CommandSender sender, String[] args) {
        if (!sender.hasPermission("pywebs.cobweb.give")) {
            plugin.getMessageManager().send(sender, "command.no-permission", placeholders());
            return;
        }

        if (args.length < 4) {
            plugin.getMessageManager().send(sender, "cobweb.usage", placeholders());
            return;
        }

        Player targetPlayer = Bukkit.getPlayer(args[2]);
        if (targetPlayer == null) {
            Map<String, String> placeholders = placeholders();
            placeholders.put("player", args[2]);
            plugin.getMessageManager().send(sender, "cobweb.player-not-found", placeholders);
            return;
        }

        CobwebManager.CobwebTier cobwebTier = plugin.getCobwebManager().getCobwebTier(args[3]);
        if (cobwebTier == null) {
            Map<String, String> placeholders = placeholders();
            placeholders.put("tier", args[3]);
            plugin.getMessageManager().send(sender, "cobweb.invalid-tier", placeholders);
            return;
        }

        Integer amount = parseAmount(sender, args);
        if (amount == null) {
            return;
        }

        ItemStack cobwebItem = plugin.getCobwebManager().createCobwebItem(cobwebTier);
        cobwebItem.setAmount(amount);
        targetPlayer.getInventory().addItem(cobwebItem);

        Map<String, String> placeholders = placeholders();
        placeholders.put("amount", String.valueOf(amount));
        placeholders.put("name", cobwebTier.getName());
        placeholders.put("player", targetPlayer.getName());
        placeholders.put("sender", sender.getName());

        plugin.getMessageManager().send(sender, "cobweb.give-success", placeholders);
        plugin.getMessageManager().send(targetPlayer, "cobweb.received", placeholders);
    }

    private Integer parseAmount(CommandSender sender, String[] args) {
        if (args.length < 5) {
            return 1;
        }

        try {
            int amount = Integer.parseInt(args[4]);
            if (amount < 1 || amount > 64) {
                plugin.getMessageManager().send(sender, "cobweb.invalid-amount", placeholders());
                return null;
            }
            return amount;
        } catch (NumberFormatException exception) {
            plugin.getMessageManager().send(sender, "cobweb.invalid-amount", placeholders());
            return null;
        }
    }

    private static Map<String, String> placeholders() {
        return new LinkedHashMap<>();
    }
}
