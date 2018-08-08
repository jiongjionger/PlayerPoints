package org.black_ixx.playerpoints.commands;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.models.Flag;
import org.black_ixx.playerpoints.services.CommandHandler;
import org.black_ixx.playerpoints.storage.StorageHandler;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.EnumMap;

public class InitCommand extends CommandHandler {
    /**
     * Constructor.
     *
     * @param plugin - Root plugin.
     * @param cmd
     */
    public InitCommand(PlayerPoints plugin) {
        super(plugin, "init");
    }

    @Override
    public boolean noArgs(CommandSender sender, Command command, String label, EnumMap<Flag, String> info) {
        if (!sender.isOp()) {
            return false;
        }
        plugin.getModuleForClass(StorageHandler.class).build();
        sender.sendMessage("执行点券初始化成功!");
        return false;
    }

    @Override
    public boolean unknownCommand(CommandSender sender, Command command, String label, String[] args, EnumMap<Flag, String> info) {
        return false;
    }
}
