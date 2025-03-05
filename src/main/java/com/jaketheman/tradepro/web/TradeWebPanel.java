package com.jaketheman.tradepro.web;

import spark.Request;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import static spark.Spark.*;
import com.google.gson.Gson;
import com.jaketheman.tradepro.TradePro;
import com.jaketheman.tradepro.logging.TradeLog;
import org.bukkit.Material;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import com.jaketheman.tradepro.util.ItemFactory;
import com.jaketheman.tradepro.util.ColorUtils;

public class TradeWebPanel {

        private final TradePro plugin;
        private final Gson gson = new Gson();

        public TradeWebPanel(TradePro plugin) {
            this.plugin = plugin;
        }

        public void startWebServer() {
            if (!plugin.getTradeConfig().isWebPanelEnabled()) {
                plugin.getLogger().info("Web panel is disabled in config.yml");
                return;
            }

            int port = plugin.getTradeConfig().getWebPanelPort();
            port(port);
            plugin.getLogger().info("Starting web server on port " + port);

            // Serve static files (CSS, etc.) from a "public" directory in your plugin folder
            staticFiles.location("/public");

            get("/", (req, res) -> {
                res.type("text/html");
                return generateTradeListHTML(req);
            });

            exception(Exception.class, (exception, request, response) -> {
                plugin.getLogger().log(Level.SEVERE, "Exception in web server", exception);
                response.status(500);
                response.body("<h1>Internal Server Error</h1><p>An error occurred while processing the request. Check the server logs for details.</p>");
            });
        }


        private String generateTradeListHTML(Request req) {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>Trade Logs</title>\n");
            html.append("    <link href=\"https://fonts.googleapis.com/css2?family=Roboto:wght@400;500;700&display=swap\" rel=\"stylesheet\">\n");
            html.append("    <link rel=\"stylesheet\" href=\"/style.css\">\n"); // Link to your CSS file
            html.append("</head>\n");
            html.append("<body>\n");
            html.append("    <div class=\"container\">\n");
            html.append("        <h1>Trade Logs</h1>\n");

            // Add the search form
            html.append("<form action=\"/\" method=\"GET\">\n");
            html.append("  <input type=\"text\" name=\"search\" placeholder=\"Search by Player Name\">\n");
            html.append("  <button type=\"submit\">Search</button>\n");
            html.append("</form>\n");
            html.append("<br>\n"); // Add spacing after the form

            html.append("        <table class=\"trade-table\">\n");
            html.append("            <thead>\n");
            html.append("                <tr>\n");
            html.append("                    <th>Time</th>\n");
            html.append("                    <th>Player 1</th>\n");
            html.append("                    <th>Player 2</th>\n");
            html.append("                    <th>Player 1 Items</th>\n");
            html.append("                    <th>Player 2 Items</th>\n");
            html.append("                </tr>\n");
            html.append("            </thead>\n");
            html.append("            <tbody>\n");

            // 1. Get the logs folder
            File logsFolder = new File(plugin.getDataFolder(), "logs");
            if (!logsFolder.exists() || !logsFolder.isDirectory()) {
                plugin.getLogger().warning("Logs folder not found: " + logsFolder.getAbsolutePath());
                return "Error: Logs folder not found.  Check server logs";  // Return an error message
            }

            // 2. List all session folders (YYYY-MM-DD_HH-mm-ss)
            File[] sessionFolders = logsFolder.listFiles(File::isDirectory);

            if (sessionFolders == null || sessionFolders.length == 0) {
                plugin.getLogger().info("No session folders found in logs directory.");
                return "No trade logs found.";  // Return a message if there are no logs
            }

            Arrays.sort(sessionFolders, (a, b) -> b.getName().compareTo(a.getName()));


            // Get search query from request
            String searchQuery = req.queryParams("search");
            File[] tradeLogFiles;
            List<File> filteredTradeLogFiles = new ArrayList<>();

            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String lowerCaseSearchQuery = searchQuery.trim().toLowerCase();
                plugin.getLogger().info("Search query received: " + searchQuery);  // Debugging log

                // Filter trade log files based on search query
                for (File sessionFolder : sessionFolders) {
                    File[] logFiles = sessionFolder.listFiles(pathname -> pathname.getName().toLowerCase().endsWith(".json"));
                    if (logFiles == null || logFiles.length == 0) {
                        plugin.getLogger().info("No trade logs found in session folder: " + sessionFolder.getName());
                        continue; // Skip to the next session folder
                    }
                    for (File tradeLogFile : logFiles) {
                        try (FileReader reader = new FileReader(tradeLogFile)) {
                            TradeLog log = gson.fromJson(reader, TradeLog.class);
                            if (log.getPlayer1().getLastKnownName().toLowerCase().contains(lowerCaseSearchQuery) ||
                                    log.getPlayer2().getLastKnownName().toLowerCase().contains(lowerCaseSearchQuery)) {
                                filteredTradeLogFiles.add(tradeLogFile);
                            }
                        } catch (IOException e) {
                            plugin.getLogger().log(Level.WARNING, "Failed to read trade log file during search: " + tradeLogFile.getAbsolutePath(), e);
                            continue;  // Skip to the next file
                        }
                    }
                }
                tradeLogFiles = filteredTradeLogFiles.toArray(new File[0]);
            }
            else {
                List<File> allFiles = new ArrayList<>();
                for (File sessionFolder : sessionFolders) {
                    File[] logFiles = sessionFolder.listFiles(pathname -> pathname.getName().toLowerCase().endsWith(".json"));
                    if (logFiles != null) {
                        allFiles.addAll(Arrays.asList(logFiles));
                    }
                }
                tradeLogFiles = allFiles.toArray(new File[0]);
            }
            if (filteredTradeLogFiles.size() == 0 && searchQuery != null && !searchQuery.trim().isEmpty())
            {
                return "Search results for " + searchQuery + " returned no results!";
            }
            Arrays.sort(tradeLogFiles, (a, b) -> b.getName().compareTo(a.getName()));

            for (File tradeLogFile : tradeLogFiles) {
                try (FileReader reader = new FileReader(tradeLogFile)) {
                    TradeLog log = gson.fromJson(reader, TradeLog.class);

                    // Format item names to display in the web panel
                    List<String> player1ItemNames = new ArrayList<>();
                    for (ItemFactory itemFactory : log.getPlayer1Items()) {
                        if (itemFactory != null) {
                            String itemName = itemFactory.getDisplayName() != null ?  ColorUtils.convertMinecraftToHTMLColor(itemFactory.getDisplayName()) :  ColorUtils.convertMinecraftToHTMLColor(itemFactory.getMaterial().toString());
                            List<String> lore = itemFactory.getLore();
                            int amount = itemFactory.getAmount();

                            StringBuilder itemDescription = new StringBuilder();
                            itemDescription.append(amount).append(" ").append(itemName);
                            if (lore != null && !lore.isEmpty()) {
                                List<String> coloredLore = lore.stream()
                                        .map(ColorUtils::convertMinecraftToHTMLColor)
                                        .collect(Collectors.toList());
                                itemDescription.append("<br>").append("<i>").append(String.join("<br>", coloredLore)).append("</i>");
                            }
                            player1ItemNames.add(itemDescription.toString());
                        } else {
                            player1ItemNames.add("INVALID ITEM");
                        }
                    }

                    List<String> player2ItemNames = new ArrayList<>();
                    for (ItemFactory itemFactory : log.getPlayer2Items()) {
                        if (itemFactory != null) {
                            String itemName = itemFactory.getDisplayName() != null ?  ColorUtils.convertMinecraftToHTMLColor(itemFactory.getDisplayName()) :  ColorUtils.convertMinecraftToHTMLColor(itemFactory.getMaterial().toString());
                            List<String> lore = itemFactory.getLore();
                            int amount = itemFactory.getAmount();

                            StringBuilder itemDescription = new StringBuilder();
                            itemDescription.append(amount).append(" ").append(itemName);
                            if (lore != null && !lore.isEmpty()) {
                                List<String> coloredLore = lore.stream()
                                        .map(ColorUtils::convertMinecraftToHTMLColor)
                                        .collect(Collectors.toList());
                                itemDescription.append("<br>").append("<i>").append(String.join("<br>", coloredLore)).append("</i>");
                            }
                            player2ItemNames.add(itemDescription.toString());
                        } else {
                            player2ItemNames.add("INVALID ITEM");
                        }
                    }

                    // Get player names and create image URLs
                    String player1Name = log.getPlayer1().getLastKnownName();
                    String player2Name = log.getPlayer2().getLastKnownName();
                    String player1HeadURL = "https://minotar.net/helm/" + player1Name + "/32.png";  //Size of player heads can also be configured, if admins prefer
                    String player2HeadURL = "https://minotar.net/helm/" + player2Name + "/32.png";


                    // Append data row to HTML table
                    html.append("<tr>\n");
                    html.append("   <td>").append(log.getTime()).append("</td>\n");
                    html.append("   <td><img src='").append(player1HeadURL).append("' alt='").append(player1Name).append(" Head' class='player-head'>").append(player1Name).append("</td>\n");
                    html.append("   <td><img src='").append(player2HeadURL).append("' alt='").append(player2Name).append(" Head' class='player-head'>").append(player2Name).append("</td>\n");
                    html.append("   <td>").append(String.join("<br>", player1ItemNames)).append("</td>\n");
                    html.append("   <td>").append(String.join("<br>", player2ItemNames)).append("</td>\n");
                    html.append("</tr>\n");

                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to read trade log file: " + tradeLogFile.getAbsolutePath(), e);
                    html.append("<tr><td colspan='5'>Error reading log file: ").append(tradeLogFile.getName()).append("</td></tr>\n");
                }
            }

            html.append("            </tbody>\n");
            html.append("        </table>\n");
            html.append("    </div>\n");
            html.append("</body>\n");
            html.append("</html>\n");

            return html.toString();
        }

}