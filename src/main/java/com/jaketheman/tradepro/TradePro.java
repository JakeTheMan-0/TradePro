package com.jaketheman.tradepro;

import co.aikar.taskchain.BukkitTaskChainFactory;
import co.aikar.taskchain.TaskChainFactory;
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
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TradePro extends JavaPlugin implements Listener {

  public ConcurrentLinkedQueue<Trade> ongoingTrades = new ConcurrentLinkedQueue<>();
  @Getter private TaskChainFactory taskFactory;

  @Getter private TradeProConfig tradeConfig;

  @Getter private List<Inventory> excessChests;

  private Logs logs;

  public Trade getTrade(Player player) {
    for (Trade trade : ongoingTrades) {
      if (trade.player1.equals(player) || trade.player2.equals(player)) return trade;
    }
    return null;
  }

  public Trade getTrade(Player player1, Player player2) {
    for (Trade trade : ongoingTrades) {
      if (trade.player1.equals(player1) && trade.player2.equals(player2)) return trade;
      if (trade.player2.equals(player1) && trade.player1.equals(player2)) return trade;
    }
    return null;
  }

  @Override
  public void onLoad() {
    try {
      WorldGuardHook.init();
    } catch (Throwable ignored) {
      getLogger().info("Failed to hook into worldguard. Ignore this if you don't have worldguard.");
    }
  }

  @Override
  public void onEnable() {
    // Ensure the plugin folder exists
    if (!getDataFolder().exists()) {
      getDataFolder().mkdirs();
    }

    // Save default configuration files
    saveDefaultConfigFile("config.yml");
    saveDefaultConfigFile("lang.yml");
    saveDefaultConfigFile("gui.yml");

    // Initialize configuration and task chain
    tradeConfig = new TradeProConfig(this);
    taskFactory = BukkitTaskChainFactory.create(this);
    taskFactory.newChain()
            .async(tradeConfig::load)
            // Removed tradeConfig::update since update() no longer exists.
            .async(tradeConfig::save)
            .sync(() -> {
              excessChests = new ArrayList<>();
              setupCommands();
              reload();
              if (Sounds.version > 17) {
                getServer().getPluginManager().registerEvents(new InteractListener(this), this);
              }
              new ExcessChestListener(this);
            })
            .execute();
    getServer().getPluginManager().registerEvents(this, this);

    getLogger().info("TradePro has started successfully!");
  }

  @Override
  public void onDisable() {
    if (logs != null) {
      logs.save();
    }
  }

  private void setupCommands() {
    getCommand("trade").setExecutor(new TradeCommand(this));
    getCommand("tradepro").setExecutor(new TradeProCommand(this));
  }

  public void reload() {
    tradeConfig.reload();
    if (logs == null && tradeConfig.isTradeLogs()) {
      try {
        logs = new Logs(this, new File(getDataFolder(), "logs"));
        new BukkitRunnable() {
          @Override
          public void run() {
            try {
              logs.save();
            } catch (Exception | Error ex) {
              getLogger().info("The trade logger crashed.");
              cancel();
              logs = null;
            }
          }
        }.runTaskTimer(this, 5 * 60 * 20, 5 * 60 * 20);
        log("Initialized trade logger.");
      } catch (Exception | Error ex) {
        log("Failed to load trade logger.");
        ex.printStackTrace();
      }
    }
    InvUtils.reloadItems(this);
  }

  private void saveDefaultConfigFile(String fileName) {
    File file = new File(getDataFolder(), fileName);
    if (!file.exists()) {
      saveResource(fileName, false);
      getLogger().info("Default " + fileName + " created.");
    } else {
      getLogger().info(fileName + " already exists, skipping creation.");
    }
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (!getTradeConfig().isAllowSameIpTrade()) {
      PlayerUtil.registerIP(event.getPlayer());
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    PlayerUtil.removeIP(event.getPlayer());
  }

  public void log(String message) {
    if (tradeConfig.isDebugMode()) {
      getLogger().info(message);
    }
  }

  public Logs getLogs() {
    return logs;
  }
}
