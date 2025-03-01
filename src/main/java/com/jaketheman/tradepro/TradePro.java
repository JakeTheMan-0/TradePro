//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.jaketheman.tradepro;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChain;
import co.aikar.taskchain.TaskChainFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jaketheman.tradepro.commands.TradeCommand;
import com.jaketheman.tradepro.commands.TradeProCommand;
import com.jaketheman.tradepro.config.TradeProConfig;
import com.jaketheman.tradepro.events.ExcessChestListener;
import com.jaketheman.tradepro.hooks.WorldGuardHook;
import com.jaketheman.tradepro.logging.Logs;
import com.jaketheman.tradepro.trade.InteractListener;
import com.jaketheman.tradepro.trade.Trade;
import com.jaketheman.tradepro.util.InvUtils;
import com.jaketheman.tradepro.util.PlayerUtil;
import com.jaketheman.tradepro.util.Sounds;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.hover.content.Content;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class TradePro extends JavaPlugin implements Listener {
  public ConcurrentLinkedQueue<Trade> ongoingTrades = new ConcurrentLinkedQueue();
  private TaskChainFactory taskFactory;
  private TradeProConfig tradeConfig;
  private List<Inventory> excessChests;
  private Logs logs;
  private String currentVersion;
  private String latestVersion;
  private final int SPIGET_RESOURCE_ID = 122258;
  private final String PLUGIN_NAME = "TradePro";
  private final String UPDATE_PERMISSION = "tradepro.update.notify";
  private boolean updateAvailable = false;
  private String downloadURL = "https://www.spigotmc.org/resources/tradepro-1-18-1-21-4-customizable-trading.122258/";

  public TradePro() {
  }

  public Trade getTrade(Player player) {
    for(Trade trade : this.ongoingTrades) {
      if (trade.player1.equals(player) || trade.player2.equals(player)) {
        return trade;
      }
    }

    return null;
  }

  public Trade getTrade(Player player1, Player player2) {
    for(Trade trade : this.ongoingTrades) {
      if (trade.player1.equals(player1) && trade.player2.equals(player2)) {
        return trade;
      }

      if (trade.player2.equals(player1) && trade.player1.equals(player2)) {
        return trade;
      }
    }

    return null;
  }

  public void onLoad() {
    try {
      WorldGuardHook.init();
    } catch (Throwable var2) {
      this.getLogger().info("Failed to hook into worldguard. Ignore this if you don't have worldguard.");
    }

  }

  public void onEnable() {
    if (!this.getDataFolder().exists()) {
      this.getDataFolder().mkdirs();
    }

    this.saveDefaultConfigFile("config.yml");
    this.saveDefaultConfigFile("lang.yml");
    this.saveDefaultConfigFile("gui.yml");
    int pluginId = 24810;
    new MetricsLite(this, pluginId);
    this.tradeConfig = new TradeProConfig(this);
    this.taskFactory = BukkitTaskChainFactory.create(this);
    TaskChain var10000 = this.taskFactory.newChain();
    TradeProConfig var10001 = this.tradeConfig;
    Objects.requireNonNull(var10001);
    var10000 = var10000.async(var10001::load);
    var10001 = this.tradeConfig;
    Objects.requireNonNull(var10001);
    var10000.async(var10001::save).sync(() -> {
      this.excessChests = new ArrayList();
      this.setupCommands();
      this.reload();
      if (Sounds.version > 17) {
        this.getServer().getPluginManager().registerEvents(new InteractListener(this), this);
      }

      new ExcessChestListener(this);
    }).execute();
    this.getServer().getPluginManager().registerEvents(this, this);
    this.currentVersion = this.getDescription().getVersion();
    this.startUpdateCheck();
    this.getLogger().info("TradePro has started successfully!");
    this.getServer().getScheduler().runTaskLater(this, () -> {
      for(Player player : Bukkit.getOnlinePlayers()) {
        if (player.hasPermission("tradepro.update.notify") && this.updateAvailable) {
          this.sendUpdateNotification(player);
        }
      }

    }, 100L);
  }

  public void onDisable() {
    if (this.logs != null) {
      this.logs.save();
    }

  }

  private void setupCommands() {
    this.getCommand("trade").setExecutor(new TradeCommand(this));
    this.getCommand("tradepro").setExecutor(new TradeProCommand(this));
  }

  public void reload() {
    this.tradeConfig.reload();
    if (this.logs == null && this.tradeConfig.isTradeLogs()) {
      try {
        this.logs = new Logs(this, new File(this.getDataFolder(), "logs"));
        (new BukkitRunnable() {
          public void run() {
            try {
              TradePro.this.logs.save();
            } catch (Error | Exception var2) {
              TradePro.this.getLogger().info("The trade logger crashed.");
              this.cancel();
              TradePro.this.logs = null;
            }

          }
        }).runTaskTimer(this, 6000L, 6000L);
        this.log("Initialized trade logger.");
      } catch (Error | Exception ex) {
        this.log("Failed to load trade logger.");
        ((Throwable)ex).printStackTrace();
      }
    }

    InvUtils.reloadItems(this);
  }

  private void saveDefaultConfigFile(String fileName) {
    File file = new File(this.getDataFolder(), fileName);
    if (!file.exists()) {
      this.saveResource(fileName, false);
      this.getLogger().info("Default " + fileName + " created.");
    } else {
      this.getLogger().info(fileName + " already exists, skipping creation.");
    }

  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (!this.getTradeConfig().isAllowSameIpTrade()) {
      PlayerUtil.registerIP(event.getPlayer());
    }

  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    PlayerUtil.removeIP(event.getPlayer());
  }

  public void log(String message) {
    if (this.tradeConfig.isDebugMode()) {
      this.getLogger().info(message);
    }

  }

  public Logs getLogs() {
    return this.logs;
  }

  private void startUpdateCheck() {
    this.getServer().getScheduler().runTaskAsynchronously(this, () -> {
      try {
        this.latestVersion = this.getLatestSpigetVersion();
        if (this.isNewVersionAvailable()) {
          this.updateAvailable = true;
          this.getLogger().warning("A new version of TradePro is available: " + this.latestVersion + " (Currently: " + this.currentVersion + ")");
          Bukkit.getScheduler().runTask(this, () -> {
            for(Player player : Bukkit.getOnlinePlayers()) {
              if (player.hasPermission("tradepro.update.notify")) {
                this.sendUpdateNotification(player);
              }
            }

          });
        } else {
          this.getLogger().info("Plugin is up to date.");
          this.updateAvailable = false;
        }
      } catch (InterruptedException | IOException e) {
        this.getLogger().log(Level.WARNING, "Failed to check for updates: " + ((Exception)e).getMessage());
      }

    });
  }

  private String getLatestSpigetVersion() throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.spiget.org/v2/resources/122258/versions/latest")).header("User-Agent", "TradeProUpdateChecker").build();
    HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
    if (response.statusCode() == 200) {
      String jsonResponse = (String)response.body();
      JsonParser parser = new JsonParser();
      JsonObject jsonObject = parser.parse(jsonResponse).getAsJsonObject();
      return jsonObject.get("name").getAsString();
    } else {
      throw new IOException("Spiget API request failed with status code: " + response.statusCode());
    }
  }

  private boolean isNewVersionAvailable() {
    try {
      String currentVersionCleaned = this.currentVersion.replaceAll("[^0-9.]", "");
      String latestVersionCleaned = this.latestVersion.replaceAll("[^0-9.]", "");
      String[] currentParts = currentVersionCleaned.split("\\.");
      String[] latestParts = latestVersionCleaned.split("\\.");
      int maxLength = Math.max(currentParts.length, latestParts.length);

      for(int i = 0; i < maxLength; ++i) {
        int currentPartValue = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
        int latestPartValue = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
        if (latestPartValue > currentPartValue) {
          return true;
        }

        if (latestPartValue < currentPartValue) {
          return false;
        }
      }

      return false;
    } catch (NumberFormatException var9) {
      this.getLogger().log(Level.WARNING, "Error comparing versions. Ensure version format is numeric (e.g., 1.2.3)");
      return false;
    }
  }

  private void sendUpdateNotification(Player player) {
    TextComponent message = new TextComponent(ChatColor.YELLOW + "[TradePro] " + ChatColor.RED + "A new version is available: " + this.latestVersion + ChatColor.YELLOW + " Click here to update!");
    message.setClickEvent(new ClickEvent(Action.OPEN_URL, this.downloadURL));
    message.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new Content[]{new Text("Click to visit the Spigot page.")}));
    player.spigot().sendMessage(message);
  }

  public TaskChainFactory getTaskFactory() {
    return this.taskFactory;
  }

  public TradeProConfig getTradeConfig() {
    return this.tradeConfig;
  }

  public List<Inventory> getExcessChests() {
    return this.excessChests;
  }
}
