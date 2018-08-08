package org.black_ixx.playerpoints.commands;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.config.LocalizeConfig;
import org.black_ixx.playerpoints.config.LocalizeNode;
import org.black_ixx.playerpoints.models.Flag;
import org.black_ixx.playerpoints.permissions.PermissionHandler;
import org.black_ixx.playerpoints.permissions.PermissionNode;
import org.black_ixx.playerpoints.services.CommandHandler;
import org.black_ixx.playerpoints.storage.StorageHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles the leader board commands.
 *
 * @author Mitsugaru
 */
public class LeadCommand extends CommandHandler {

    /**
     * Entires per page limit.
     */
    private static final int LIMIT = 10;

    /**
     * Current page the player is viewing.
     */
    private final Map<String, Integer> page = new HashMap<>();

    /**
     * Constructor.
     *
     * @param plugin - Plugin instance.
     */
    public LeadCommand(PlayerPoints plugin) {
        super(plugin, "lead");
    }

    @Override
    public boolean noArgs(CommandSender sender, Command command, String label,
                          EnumMap<Flag, String> info) {
        // Check permissions
        if (!PermissionHandler.has(sender, PermissionNode.LEAD)) {
            info.put(Flag.EXTRA, PermissionNode.LEAD.getNode());
            final String permMessage = LocalizeConfig.parseString(
                    LocalizeNode.PERMISSION_DENY, info);
            if (!permMessage.isEmpty()) {
                sender.sendMessage(permMessage);
            }
            return true;
        }
        StorageHandler moduleForClass = plugin.getModuleForClass(StorageHandler.class);
        moduleForClass.getPlayers(strings -> {
            Integer pageCurrent = page.getOrDefault(sender.getName(), 1);
            if (pageCurrent < 1) {
                pageCurrent = 1;
            }
            sender.sendMessage(ChatColor.BLUE + "=== " + ChatColor.GRAY
                    + PlayerPoints.TAG + " Points Leaders " + ChatColor.BLUE + "=== "
                    + ChatColor.GRAY + pageCurrent + ":" + strings.size());
            int i = pageCurrent * 10;
            if (i > strings.size()) {
                i = strings.size();
            }
            strings.subList((pageCurrent - 1) * 10, i).forEach(string -> {
                sender.sendMessage(ChatColor.AQUA + "" + (strings.indexOf(string) + 1) + ". "
                        + ChatColor.GRAY + moduleForClass.getPlayerCacheName(UUID.fromString(string)) + ChatColor.WHITE
                        + " - " + ChatColor.GOLD + plugin.getAPI().look(UUID.fromString(string)));
            });
        });
        return true;
    }

    @Override
    public boolean unknownCommand(CommandSender sender, Command command,
                                  String label, String[] args, EnumMap<Flag, String> info) {
        String com = args[0];

        int current = 0;
        if (page.containsKey(sender.getName())) {
            current = page.get(sender.getName());
        }

        boolean valid = false;

        if (com.equalsIgnoreCase("prev")) {
            page.put(sender.getName(), --current);
            noArgs(sender, command, label, info);
            valid = true;
        } else if (com.equals("next")) {
            page.put(sender.getName(), ++current);
            noArgs(sender, command, label, info);
            valid = true;
        } else {
            try {
                current = Integer.parseInt(com);
                page.put(sender.getName(), current - 1);
                noArgs(sender, command, label, info);
                valid = true;
            } catch (NumberFormatException e) {
                // Handle notification later
            }
        }

        // Handle invalid input
        if (!valid) {
            info.put(Flag.EXTRA, args[0]);
            sender.sendMessage(LocalizeConfig.parseString(
                    LocalizeNode.COMMAND_LEAD, info));
        }

        return true;
    }

}
