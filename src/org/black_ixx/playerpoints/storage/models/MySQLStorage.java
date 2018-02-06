package org.black_ixx.playerpoints.storage.models;

import lib.PatPeter.SQLibrary.MySQL;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.config.RootConfig;
import org.black_ixx.playerpoints.storage.DatabaseStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Storage handler for MySQL source.
 *
 * @author Mitsugaru
 */
public class MySQLStorage extends DatabaseStorage {
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * MYSQL reference.
     */
    private MySQL mysql;
    /**
     * Number of attempts to reconnect before completely failing an operation.
     */
    private int retryLimit = 10;
    /**
     * The table name to use.
     */
    private String tableName;
    /**
     * Current retry count.
     */
    private int retryCount = 0;
    /**
     * Skip operation flag.
     */
    private boolean skip = false;

    /**
     * Constructor.
     *
     * @param plugin - Plugin instance.
     */
    public MySQLStorage(PlayerPoints plugin) {
        super(plugin);
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if (config.debugDatabase) {
            plugin.getLogger().info("Constructor");
        }
        retryLimit = config.retryLimit;
        //setup table name and strings
        tableName = config.table;
        SetupQueries(tableName);
        //Connect
        connect();
        if (!mysql.isTable(tableName)) {
            build();
        }
    }

    @Override
    public int getPoints(String id) {
        int points = 0;
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if (id == null || id.equals("")) {
            if (config.debugDatabase) {
                plugin.getLogger().info("getPoints() - bad ID");
            }
            return points;
        }
        if (config.debugDatabase) {
            plugin.getLogger().info("getPoints(" + id + ")");
        }
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            statement = mysql.prepare(GET_POINTS);
            statement.setString(1, id);
            result = mysql.query(statement);
            if (result != null && result.next()) {
                points = result.getInt("points");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not create getter statement.", e);
            retryCount++;
            connect();
            if (!skip) {
                points = getPoints(id);
            }
        } finally {
            cleanup(result, statement);
        }
        retryCount = 0;
        if (config.debugDatabase) {
            plugin.getLogger().info("getPlayers() result - " + points);
        }
        return points;
    }

    @Override
    public boolean setPoints(String id, int points) {
        boolean value = false;
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if (id == null || id.equals("")) {
            if (config.debugDatabase) {
                plugin.getLogger().info("setPoints() - bad ID");
            }
            return value;
        }
        if (config.debugDatabase) {
            plugin.getLogger().info("setPoints(" + id + "," + points + ")");
        }
        final boolean exists = playerEntryExists(id);
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            if (exists) {
                statement = mysql.prepare(UPDATE_PLAYER);
            } else {
                statement = mysql.prepare(INSERT_PLAYER);
            }
            statement.setInt(1, points);
            statement.setString(2, id);
            result = mysql.query(statement);
            value = true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not create setter statement.", e);
            retryCount++;
            connect();
            if (!skip) {
                value = setPoints(id, points);
            }
        } finally {
            cleanup(result, statement);
        }
        retryCount = 0;
        if (config.debugDatabase) {
            plugin.getLogger().info("setPoints() result - " + value);
        }
        return value;
    }

    @Override
    public boolean playerEntryExists(String id) {
        boolean has = false;
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if (id == null || id.equals("")) {
            if (config.debugDatabase) {
                plugin.getLogger().info("playerEntryExists() - bad ID");
            }
            return has;
        }
        if (config.debugDatabase) {
            plugin.getLogger().info("playerEntryExists(" + id + ")");
        }
        PreparedStatement statement = null;
        ResultSet result = null;
        try {
            statement = mysql.prepare(GET_POINTS);
            statement.setString(1, id);
            result = mysql.query(statement);
            if (result.next()) {
                has = true;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not create player check statement.", e);
            retryCount++;
            connect();
            if (!skip) {
                has = playerEntryExists(id);
            }
        } finally {
            cleanup(result, statement);
        }
        retryCount = 0;
        if (config.debugDatabase) {
            plugin.getLogger().info("playerEntryExists() result - " + has);
        }
        return has;
    }

    @Override
    public boolean removePlayer(String id) {
        boolean deleted = false;
        if (id == null || id.equals("")) {
            return deleted;
        }
        PreparedStatement statement = null;
        ResultSet result = null;
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if (config.debugDatabase) {
            plugin.getLogger().info("removePlayers(" + id + ")");
        }
        try {
            statement = mysql.prepare(REMOVE_PLAYER);
            statement.setString(1, id);
            result = mysql.query(statement);
            deleted = true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not create player remove statement.", e);
            retryCount++;
            connect();
            if (!skip) {
                deleted = playerEntryExists(id);
            }
        } finally {
            cleanup(result, statement);
        }
        retryCount = 0;
        if (config.debugDatabase) {
            plugin.getLogger().info("renovePlayers() result - " + deleted);
        }
        return deleted;
    }

    @Override
    public void getPlayers(Consumer<List<String>> collectionConsumer) {
        ExecutorService executorService = Executors.newCachedThreadPool();
        try {
            executorService.execute(() -> {
                List<String> players = new ArrayList<>();

                RootConfig config = plugin.getModuleForClass(RootConfig.class);
                if (config.debugDatabase) {
                    plugin.getLogger().info("Attempting getPlayers()");
                }
                PreparedStatement statement = null;
                ResultSet result = null;
                try {
                    statement = mysql.prepare(GET_PLAYERS);
                    result = mysql.query(statement);

                    while (result.next()) {
                        String name = result.getString("playername");
                        if (name != null) {
                            players.add(name);
                        }
                    }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE,
                            "Could not create get players statement.", e);
                    retryCount++;
                    connect();
                    if (!skip) {
                        players.clear();
                        getPlayers(players::addAll);
                    }
                } finally {
                    cleanup(result, statement);
                }
                retryCount = 0;
                if (config.debugDatabase) {
                    plugin.getLogger().info("getPlayers() result - " + players.size());
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
            mysql.query("INSERT INTO playerpoints_log VALUES('" + playerName + "','" + SIMPLE_DATE_FORMAT.format(new Date()) + "'," + amount + ",'" + commandSender.getName() + "');");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void cachePlayerName(UUID uuid,String cacheName){
        try {
            if (!mysql.query("SELECT * FROM playerpoints_uuid_storage WHERE uuid='"+uuid+"';").next()) {
                mysql.query("INSERT INTO playerpoints_uuid_storage VALUES('"+uuid+"','"+cacheName+"');");
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public String getPlayerCacheName(UUID uuid) {
        try {
            ResultSet query = mysql.query("SELECT * FROM playerpoints_uuid_storage WHERE uuid='" + uuid + "';");
            if (query.next()){
                return query.getString("cacheName");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Bukkit.getOfflinePlayer(uuid).getName();
    }

    /**
     * Connect to MySQL database. Close existing connection if one exists.
     */
    private void connect() {
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if (mysql != null) {
            if (config.debugDatabase) {
                plugin.getLogger().info("Closing existing MySQL connection");
            }
            mysql.close();
        }
        mysql = new MySQL(plugin.getLogger(), " ", config.host,
                Integer.valueOf(config.port), config.database, config.user,
                config.password);
        if (config.debugDatabase) {
            plugin.getLogger().info("Attempting MySQL connection to " + config.user + "@" + config.host + ":" + config.port + "/" + config.database);
        }
        if (retryCount < retryLimit) {
            mysql.open();

        } else {
            plugin.getLogger().severe(
                    "Tried connecting to MySQL " + retryLimit
                            + " times and could not connect.");
            plugin.getLogger()
                    .severe("It may be in your best interest to restart the plugin / server.");
            retryCount = 0;
            skip = true;
        }
    }

    @Override
    public boolean destroy() {
        boolean success = false;
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if (config.debugDatabase) {
            plugin.getLogger().info("Dropping playerpoints table");
        }
        try {
            mysql.query(String.format("DROP TABLE %s;", tableName));
            success = true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not drop MySQL table.", e);
        }
        return success;
    }

    @Override
    public boolean build() {
        boolean success = false;
        RootConfig config = plugin.getModuleForClass(RootConfig.class);
        if (config.debugDatabase) {
            plugin.getLogger().info(String.format("Creating %s table", tableName));
        }
        try {
            mysql.query("CREATE TABLE IF NOT EXISTS playerpoints_log (\n" +
                    "  `player` varchar(255) NOT NULL DEFAULT '',\n" +
                    "  `date` varchar(255) NOT NULL DEFAULT 0,\n" +
                    "  `amount` int(11) NOT NULL DEFAULT 0,\n" +
                    "  `executor` varchar(255) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL DEFAULT ''\n" +
                    ") DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;");
            mysql.query("CREATE TABLE IF NOT EXISTS playerpoints_uuid_storage\n" +
                    "(\n" +
                    "    uuid VARCHAR(255) PRIMARY KEY,\n" +
                    "    cacheName VARCHAR(255)\n" +
                    ");");
            mysql.query(String.format("CREATE TABLE IF NOT EXISTS %s (id INT UNSIGNED NOT NULL AUTO_INCREMENT, playername varchar(36) NOT NULL, points INT NOT NULL, PRIMARY KEY(id), UNIQUE(playername));", tableName));
            success = true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE,
                    "Could not create MySQL table.", e);
        }
        return success;
    }

}
