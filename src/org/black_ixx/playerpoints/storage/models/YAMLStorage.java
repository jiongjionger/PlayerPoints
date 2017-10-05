package org.black_ixx.playerpoints.storage.models;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.storage.IStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Object that handles points storage from a file config source.
 *
 * @author Mitsugaru
 */
public class YAMLStorage implements IStorage {
    protected static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Points section string.
     */
    private static final String POINTS_SECTION = "Points.";
    /**
     * Plugin reference.
     */
    private PlayerPoints plugin;
    /**
     * File reference.
     */
    private File file;
    /**
     * Yaml config.
     */
    private YamlConfiguration config;

    private File logFile;

    /**
     * Constructor.
     *
     * @param pp - Player points plugin instance.
     */
    public YAMLStorage(PlayerPoints pp) {
        plugin = pp;
        file = new File(plugin.getDataFolder().getAbsolutePath()
                + "/storage.yml");
        config = YamlConfiguration.loadConfiguration(file);
        this.logFile = new File(plugin.getDataFolder(), "points.log");
        save();
    }

    /**
     * Save the config data.
     */
    public void save() {
        // Set config
        try {
            // Save the file
            config.save(file);
        } catch (IOException e1) {
            plugin.getLogger().warning(
                    "File I/O Exception on saving storage.yml");
            e1.printStackTrace();
        }
    }

    /**
     * Reload the config file.
     */
    public void reload() {
        try {
            config.load(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean setPoints(String id, int points) {
        config.set(POINTS_SECTION + id, points);
        save();
        return true;
    }

    @Override
    public int getPoints(String id) {
        int points = config.getInt(POINTS_SECTION + id, 0);
        return points;
    }

    @Override
    public boolean playerEntryExists(String id) {
        return config.contains(POINTS_SECTION + id);
    }

    @Override
    public boolean removePlayer(String id) {
        config.set(POINTS_SECTION + id, null);
        return true;
    }

    @Override
    public void getPlayers(Consumer<List<String>> collectionConsumer) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        try {
            executorService
                    .execute(() -> {
                        List<String> players = new ArrayList<>();

                        if (config.isConfigurationSection("Points")) {
                            players.addAll(config.getConfigurationSection("Points").getKeys(false));
                        }
                        collectionConsumer.accept(players);
                    });
        } finally {
            executorService.shutdown();
        }
    }

    @Override
    public void logPlayerPointsChange(String playerName, CommandSender commandSender, int amount) {
        try {
            new FileWriter(logFile, true).write("[" + SIMPLE_DATE_FORMAT.format(new Date()) + "] " + playerName + " point change amount " + amount + " by " + commandSender.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean destroy() {
        Collection<String> sections = config.getKeys(false);
        for (String section : sections) {
            config.set(section, null);
        }
        return true;
    }

    @Override
    public boolean build() {
        boolean success = false;
        try {
            success = file.createNewFile();
            success = logFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create storage file!", e);
        }
        return success;
    }

}
