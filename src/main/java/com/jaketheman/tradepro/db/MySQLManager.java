package com.jaketheman.tradepro.db;

import com.jaketheman.tradepro.TradePro;
import com.jaketheman.tradepro.logging.TradeLog;
import com.google.gson.Gson;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class MySQLManager {
    private final TradePro plugin;
    private Connection connection;
    private final Gson gson = new Gson();

    public MySQLManager(TradePro plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        if (!plugin.getTradeConfig().isDatabaseEnabled()) return false;

        String host = plugin.getTradeConfig().getDatabaseHost();
        int port = plugin.getTradeConfig().getDatabasePort();
        String database = plugin.getTradeConfig().getDatabaseName();
        String username = plugin.getTradeConfig().getDatabaseUsername();
        String password = plugin.getTradeConfig().getDatabasePassword();

        try {
            if (connection != null && !connection.isClosed()) return true;

            // Replit uses PostgreSQL, but we'll support both.
            // If the host contains 'replit', we can assume it might be a Postgres connection,
            // but for a Spigot plugin, MySQL is the standard.
            // However, to make it work on Replit, we should check for PostgreSQL driver too.

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database;
            if (host.contains("pooler.neon.tech") || host.contains("postgres")) {
                Class.forName("org.postgresql.Driver");
                url = "jdbc:postgresql://" + host + ":" + port + "/" + database;
            } else {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }

            connection = DriverManager.getConnection(url, username, password);
            setupTable();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not connect to MySQL database!", e);
            return false;
        }
    }

    private void setupTable() throws SQLException {
        String prefix = plugin.getTradeConfig().getDatabaseTablePrefix();
        String idColumn = "id INT AUTO_INCREMENT PRIMARY KEY";
        if (connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("PostgreSQL")) {
            idColumn = "id SERIAL PRIMARY KEY";
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + prefix + "logs (" +
                    idColumn + "," +
                    "time VARCHAR(255)," +
                    "player1_uuid VARCHAR(36)," +
                    "player1_name VARCHAR(16)," +
                    "player2_uuid VARCHAR(36)," +
                    "player2_name VARCHAR(16)," +
                    "log_json TEXT" +
                    ")");
        }
    }

    public void logTrade(TradeLog log) {
        if (!connect()) return;

        String prefix = plugin.getTradeConfig().getDatabaseTablePrefix();
        String query = "INSERT INTO " + prefix + "logs (time, player1_uuid, player1_name, player2_uuid, player2_name, log_json) VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, log.getTime().toString());
            statement.setString(2, log.getPlayer1().getUniqueId().toString());
            statement.setString(3, log.getPlayer1().getLastKnownName());
            statement.setString(4, log.getPlayer2().getUniqueId().toString());
            statement.setString(5, log.getPlayer2().getLastKnownName());
            statement.setString(6, gson.toJson(log));
            statement.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not log trade to MySQL!", e);
        }
    }

    public List<TradeLog> getRecentLogs(int limit) {
        List<TradeLog> logs = new ArrayList<>();
        if (!connect()) return logs;

        String prefix = plugin.getTradeConfig().getDatabaseTablePrefix();
        String query = "SELECT log_json FROM " + prefix + "logs ORDER BY id DESC LIMIT ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    String json = rs.getString("log_json");
                    logs.add(plugin.getLogs().getGson().fromJson(json, TradeLog.class));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not fetch logs from MySQL!", e);
        }
        return logs;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
