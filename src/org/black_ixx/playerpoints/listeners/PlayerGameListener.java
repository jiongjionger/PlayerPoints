package org.black_ixx.playerpoints.listeners;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.config.RootConfig;
import org.black_ixx.playerpoints.storage.StorageHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerGameListener implements Listener{
    private PlayerPoints playerPoints;

    public PlayerGameListener(PlayerPoints playerPoints) {
        this.playerPoints = playerPoints;
    }

    @EventHandler
    public void onPlayerJoinGame(PlayerJoinEvent event){
        Player player = event.getPlayer();
        playerPoints.getModuleForClass(StorageHandler.class).cachePlayerName(player.getUniqueId(),player.getName());
    }
}
