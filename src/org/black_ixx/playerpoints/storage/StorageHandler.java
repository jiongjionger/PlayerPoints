package org.black_ixx.playerpoints.storage;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.config.RootConfig;
import org.black_ixx.playerpoints.services.IModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Storage handler for getting / setting info between YAML, SQLite, and MYSQL.
 */
public class StorageHandler implements IStorage, IModule {
    /**
     * Current storage of player points information.
     */
    IStorage storage;
    /**
     * Plugin instance.
     */
    private PlayerPoints plugin;
    /**
     * Generator for storage objects.
     */
    private StorageGenerator generator;

    /**
     * Constructor.
     *
     * @param plugin - PlayerPoints plugin instance.
     */
    public StorageHandler(PlayerPoints plugin) {
        this.plugin = plugin;

    }

    @Override
    public int getPoints(String name) {
        return storage.getPoints(name);
    }

    @Override
    public boolean setPoints(String name, int points) {
        return storage.setPoints(name, points);
    }

    @Override
    public boolean playerEntryExists(String id) {
        return storage.playerEntryExists(id);
    }

    @Override
    public boolean removePlayer(String id) {
        return storage.removePlayer(id);
    }


    @Override
    public void logPlayerPointsChange(String playerName, CommandSender commandSender, int amount) {
        if (plugin.getModuleForClass(RootConfig.class).isLogEnable) {
            this.storage.logPlayerPointsChange(playerName, commandSender, amount);
        }
    }

    @Override
    public void cachePlayerName(UUID uuid, String cacheName) {
        if (plugin.getModuleForClass(RootConfig.class).isCachePlayerNameEnable) {
            this.storage.cachePlayerName(uuid, cacheName);
        }
    }

    @Override
    public String getPlayerCacheName(UUID uuid) {
        if (plugin.getModuleForClass(RootConfig.class).isCachePlayerNameEnable) {
            return this.storage.getPlayerCacheName(uuid);
        }
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    @Override
    public UUID getPlayerCacheUUID(String name) {
        if (plugin.getModuleForClass(RootConfig.class).isCachePlayerNameEnable) {
            return this.storage.getPlayerCacheUUID(name);
        }
        return Bukkit.getOfflinePlayer(name).getUniqueId();
    }

    @Override
    public void starting() {
        generator = new StorageGenerator(plugin);
        storage = generator.createStorageHandlerForType(plugin
                .getModuleForClass(RootConfig.class).getStorageType());
    }

    @Override
    public void closing() {
    }

    @Override
    public boolean destroy() {
        return storage.destroy();
    }

    @Override
    public boolean build() {
        return storage.build();
    }

    @Override
    public void getPlayers(Consumer<List<String>> collectionConsumer) {
        this.storage.getPlayers(collectionConsumer);
    }

}
