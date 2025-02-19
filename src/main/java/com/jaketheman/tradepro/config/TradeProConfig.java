package com.jaketheman.tradepro.config;

import com.jaketheman.tradepro.TradePro;
import com.jaketheman.tradepro.util.ItemFactory;
import com.jaketheman.tradepro.util.MsgUtils;
import com.jaketheman.tradepro.util.Sounds;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public class TradeProConfig {

  private TradePro plugin;

  private File configFile;
  private FileConfiguration config;

  private File langFile;
  private FileConfiguration lang;

  private File guiFile;
  private FileConfiguration gui;

  // Config values from config.yml
  private List<String> aliases;
  private boolean tradeCompatMode = true;

  private boolean tradeLogs;
  private boolean allowSameIpTrade;

  private boolean permissionsRequired;
  private String sendPermission, acceptPermission;

  private int requestCooldownSeconds;
  private boolean allowTradeInCreative, allowShiftRightClick;

  private List<String> itemBlacklist;
  private boolean denyNamedItems;
  private List<String> loreBlacklist;

  private String action;

  private double sameWorldRange, crossWorldRange;
  private boolean allowCrossWorld;
  private List<String> blockedWorlds;

  private int antiscamCountdown;
  private boolean antiscamCancelOnChange, preventChangeOnAccept, discrepancyDetection;

  private boolean endDisplayEnabled;
  private int endDisplayTimer;

  private boolean spectateEnabled, spectateBroadcast;

  // Config values from gui.yml
  private String guiTitle;
  private String spectatorTitle;

  private List<Integer> mySlots, theirSlots;
  private List<Integer> myExtraSlots, theirExtraSlots;
  private ItemFactory force,
          accept,
          cancel,
          complete,
          cancelled,
          theirAccept,
          theirCancel,
          separator,
          placeholder;
  private int forceSlot, acceptSlot, theirAcceptSlot;
  private boolean forceEnabled, acceptEnabled, headEnabled;
  private String headDisplayName;

  // Extras settings (from config.yml)
  private String extrasTypePrefix,
          extrasTypeEmpty,
          extrasTypeValid,
          extrasTypeInvalid,
          extrasTypeMaximum;
  private boolean factionsAllowTradeInEnemyTerritory, worldguardTradingFlag;
  private boolean soundEffectsEnabled,
          soundOnChange,
          soundOnAccept,
          soundOnComplete,
          soundOnCountdown;

  private boolean excessChest;
  private String excessTitle;

  private boolean debugMode;

  // Language messages (from lang.yml)
  private ConfigMessage requestSent;
  private ConfigMessage requestReceived;

  private ConfigMessage tradingDisabled;
  private ConfigMessage tradingEnabled;

  private ConfigMessage errorsCreative, errorsCreativeThem, errorsSameIp;
  private ConfigMessage errorsBlockedWorld, errorsSameWorldRange, errorsCrossWorldRange, errorsNoCrossWorld;
  private ConfigMessage acceptSender, acceptReceiver;
  private ConfigMessage cancelledMessage, expired;
  private ConfigMessage errorsWaitForExpire,
          errorsPlayerNotFound,
          errorsSelfTrade,
          errorsInvalidUsage,
          errorsTradingDisabled;
  private ConfigMessage errorsNoPermsAccept,
          errorsNoPermsSend,
          errorsNoPermsReceive,
          errorsNoPermsAdmin;
  private ConfigMessage tradeComplete, forcedTrade;
  private ConfigMessage theyDenied, youDenied;

  private ConfigMessage spectateMessage;
  private ConfigMessage discrepancyDetected;

  private ConfigMessage adminConfigReloaded,
          adminInvalidPlayers,
          adminForcedTrade,
          adminPlayersOnly,
          adminNoTrade;
  private ConfigMessage factionsEnemyTerritory, worldguardTradingNotAllowed;

  public TradeProConfig(TradePro plugin) {
    this.plugin = plugin;
  }

  /**
   * Saves the current in-memory configurations to disk.
   */
  public void save() {
    try {
      config.save(configFile);
      lang.save(langFile);
      gui.save(guiFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Reloads all configuration files (config.yml, lang.yml, gui.yml) from disk
   * and loads all values into memory. This method does not update or reset the files.
   */
  public void reload() {
    load();

    // Load values from config.yml
    aliases = config.getStringList("aliases");

    excessChest = config.getBoolean("excess-chest.enabled", true);
    excessTitle = MsgUtils.color(config.getString("excess-chest.title", "&7Your inventory is full!"));

    tradeLogs = config.getBoolean("trade-logs", false);
    allowSameIpTrade = config.getBoolean("allow-same-ip-trade", true);

    permissionsRequired = config.getBoolean("permissions.required", config.getBoolean("permissionrequired", false));
    sendPermission = config.getString("permissions.send", config.getString("permissionnode", "tradepro.send"));
    acceptPermission = config.getString("permissions.accept", "tradepro.accept");

    requestCooldownSeconds = config.getInt("requestcooldownseconds", 20);
    allowTradeInCreative = config.getBoolean("allow-trade-in-creative", false);
    allowShiftRightClick = config.getBoolean("allow-shift-right-click");

    itemBlacklist = config.getStringList("blocked.blacklist");
    denyNamedItems = config.getBoolean("blocked.named-items", false);
    loreBlacklist = config.getStringList("blocked.lore");

    action = config.getString("action", "crouchrightclick");

    sameWorldRange = config.getDouble("ranges.sameworld", 10.0);
    crossWorldRange = config.getDouble("ranges.crossworld", 0.0);
    allowCrossWorld = config.getBoolean("ranges.allowcrossworld", false);
    blockedWorlds = config.getStringList("ranges.blocked-worlds");

    antiscamCountdown = config.getInt("antiscam.countdown", 10);
    antiscamCancelOnChange = config.getBoolean("antiscam.cancelonchange", true);
    preventChangeOnAccept = config.getBoolean("antiscam.preventchangeonaccept", true);
    discrepancyDetection = config.getBoolean("antiscam.discrepancy-detection", true);

    endDisplayEnabled = config.getBoolean("end-display.enabled", true);
    endDisplayTimer = config.getInt("end-display.timer", 0);

    spectateEnabled = config.getBoolean("spectate.enabled", true);
    spectateBroadcast = config.getBoolean("spectate.broadcast", true);

    // Load values from gui.yml
    guiTitle = MsgUtils.color(gui.getString("title", "Your Items <|     |> Their Items"));
    spectatorTitle = MsgUtils.color(gui.getString("spectator-title", "Player 1 <|          |> Player 2"));

    mySlots = gui.getStringList("my-slots").stream()
            .map(Integer::valueOf)
            .collect(Collectors.toList());
    theirSlots = gui.getStringList("their-slots").stream()
            .map(Integer::valueOf)
            .collect(Collectors.toList());

    myExtraSlots = gui.getStringList("my-extra-slots").stream()
            .map(Integer::valueOf)
            .collect(Collectors.toList());
    theirExtraSlots = gui.getStringList("their-extra-slots").stream()
            .map(Integer::valueOf)
            .collect(Collectors.toList());

    extrasTypePrefix = MsgUtils.color(config.getString("extras.type.prefix", "&6&l!!&6> "));
    extrasTypeEmpty = MsgUtils.color(config.getString("extras.type.empty", "&ePlease enter your %EXTRA% offer, or type 'cancel'."));
    extrasTypeValid = MsgUtils.color(config.getString("extras.type.valid", "&aClick output slot to submit offer."));
    extrasTypeInvalid = MsgUtils.color(config.getString("extras.type.invalid", "&cInvalid amount entered!"));
    extrasTypeMaximum = MsgUtils.color(config.getString("extras.type.maximum", "&cYou have %BALANCE% %EXTRA%"));

    factionsAllowTradeInEnemyTerritory = config.getBoolean("hooks.factions.allow-trades-in-enemy-territory", false);
    worldguardTradingFlag = config.getBoolean("hooks.worldguard.trading-flag", true);

    soundEffectsEnabled = config.getBoolean("soundeffects.enabled", true);
    soundOnChange = config.getBoolean("soundeffects.onchange", true);
    soundOnAccept = config.getBoolean("soundeffects.onaccept", true);
    soundOnComplete = config.getBoolean("soundeffects.oncomplete", true);
    soundOnCountdown = config.getBoolean("soundeffects.oncountdown", true);

    debugMode = config.getBoolean("debug-mode", false);

    // Load values from lang.yml (construct messages)
    requestSent = new ConfigMessage(lang, "request.sent", "&6&l(!) &r&6You sent a trade request to &e%PLAYER%");
    requestReceived = new ConfigMessage(lang, "request.received", "&6&l(!) &r&6You received a trade request from &e%PLAYER%%NEWLINE%&6&l(!) &r&6Type &e/trade %PLAYER% &6to begin trading");

    tradingEnabled = new ConfigMessage(lang, "enabled", "&6&l(!) &r&6You &aenabled &6trade requests from other players!");
    tradingDisabled = new ConfigMessage(lang, "disabled", "&6&l(!) &r&6You &cdisabled &6trade requests from other players!");

    errorsCreative = new ConfigMessage(lang, "errors.creative", "&4&l(!) &r&4You can't trade in creative mode!");
    errorsCreativeThem = new ConfigMessage(lang, "errors.creative-them", "&4&l(!) &r&4That player is in creative mode!");
    errorsSameIp = new ConfigMessage(lang, "errors.same-ip", "&4&l(!) &4Players aren't allowed to trade on same IP!");
    errorsBlockedWorld = new ConfigMessage(lang, "errors.blocked-world", "&4&l(!) &r&4You can't trade in this world.");
    errorsSameWorldRange = new ConfigMessage(lang, "errors.within-range.same-world", "&4&l(!) &r&4You must be within %AMOUNT% blocks of a player to trade with them");
    errorsCrossWorldRange = new ConfigMessage(lang, "errors.within-range.cross-world", "&4&l(!) &r&4You must be within %AMOUNT% blocks of a player%NEWLINE%&4&l(!) &r&4in a different world to trade with them!");
    errorsNoCrossWorld = new ConfigMessage(lang, "errors.no-cross-world", "&4&l(!) &r&4You must be in the same world as a player to trade with them!");
    acceptSender = new ConfigMessage(lang, "accept.sender", "&6&l(!) &r&e%PLAYER% &6accepted your trade request");
    acceptReceiver = new ConfigMessage(lang, "accept.receiver", "&6&l(!) &r&6You accepted &e%PLAYER%'s &6trade request");
    cancelledMessage = new ConfigMessage(lang, "cancelled", "&4&l(!) &r&4The trade was cancelled");
    expired = new ConfigMessage(lang, "expired", "&4&l(!) &r&4Your last trade request expired");
    errorsWaitForExpire = new ConfigMessage(lang, "errors.wait-for-expire", "&4&l(!) &r&4You still have an active trade request%NEWLINE%&4&l(!) &r&4It will expire shortly");
    errorsPlayerNotFound = new ConfigMessage(lang, "errors.player-not-found", "&4&l(!) &r&4Could not find specified player");
    errorsSelfTrade = new ConfigMessage(lang, "errors.self-trade", "&4&l(!) &r&4You cannot trade with yourself");
    errorsInvalidUsage = new ConfigMessage(lang, "errors.invalid-usage", "&4&l(!) &r&4Invalid arguments. Usage: %NEWLINE%    &c- /trade <player name>%NEWLINE%    &c- /trade deny");
    errorsTradingDisabled = new ConfigMessage(lang, "errors.trading-disabled", "&4&l(!) &r&4That player has trading toggled off.");
    errorsNoPermsAccept = new ConfigMessage(lang, "errors.no-perms.accept", "&4&l(!) &r&4You do not have permission to trade");
    errorsNoPermsSend = new ConfigMessage(lang, "errors.no-perms.send", "&4&l(!) &r&4You do not have permission to send a trade");
    errorsNoPermsReceive = new ConfigMessage(lang, "errors.no-perms.receive", "&4&l(!) &r&4That player does not have permission to accept a trade");
    errorsNoPermsAdmin = new ConfigMessage(lang, "errors.no-perms.admin", "&4&l(!) &r&4You do not have permission to use this command");
    tradeComplete = new ConfigMessage(lang, "trade-complete", "&6&l(!) &r&6The trade was successful!");
    forcedTrade = new ConfigMessage(lang, "forced-trade", "&6&l(!) &r&6You've been forced into a trade with &e%PLAYER%");
    theyDenied = new ConfigMessage(lang, "denied.them", "&4&l(!) &r&4Your trade request to &c%PLAYER% &4was denied");
    youDenied = new ConfigMessage(lang, "denied.you", "&4&l(!) &r&4Any recent incoming trade requests have been denied.");
    spectateMessage = new ConfigMessage(lang, "spectate", "&6&l(!) &e%PLAYER1% &6and &e%PLAYER2% &6have started a trade %NEWLINE%&6&l(!) &6Type &e/tradepro spectate %PLAYER1% %PLAYER2% &6to spectate");
    discrepancyDetected = new ConfigMessage(lang, "antiscam.discrepancy", "&4&l(!) &r&4A discrepancy was detected in the traded items.%NEWLINE%&4&l(!) &4The trade has been cancelled.");
    adminConfigReloaded = new ConfigMessage(lang, "admin.configs-reloaded", "&6&l(!) &6Configs reloaded!");
    adminInvalidPlayers = new ConfigMessage(lang, "admin.invalid-players", "&4&l(!) &4Invalid players!");
    adminForcedTrade = new ConfigMessage(lang, "admin.forced-trade", "&6&l(!) &6You forced a trade between &e%PLAYER1% &6and &e%PLAYER2%");
    adminPlayersOnly = new ConfigMessage(lang, "admin.players-only", "&4&l(!) &4This command is for players only.");
    adminNoTrade = new ConfigMessage(lang, "admin.no-trade", "&4&l(!) &4This command is for players only.");
    factionsEnemyTerritory = new ConfigMessage(lang, "hooks.factions.enemy-territory", "&4&l(!) &4You can't trade in enemy territory!");
    worldguardTradingNotAllowed = new ConfigMessage(lang, "hooks.worldguard.trading-not-allowed", "&4&l(!) &4You can't trade in this area.");

    // Load values from gui.yml for items and slots
    headEnabled = gui.getBoolean("head.enabled", true);
    headDisplayName = gui.getString("head.display-name", "&7You are trading with: &3&l%PLAYER%");

    acceptEnabled = gui.getBoolean("accept.enabled", true);
    acceptSlot = gui.getInt("accept.my-slot", 0);
    theirAcceptSlot = gui.getInt("accept.their-slot", 8);

    // Load/initialize item icons using ItemFactory
    accept = new ItemFactory(gui, "accept.my-icon");
    if (accept.getStack() == null) {
      Material material = Material.matchMaterial("RED_STAINED_GLASS_PANE");
      if (material == null) {
        Bukkit.getLogger().warning("[TradePro] Invalid Material: RED_STAINED_GLASS_PANE. Falling back to PAPER.");
        material = Material.PAPER;
      }
      accept = new ItemFactory(material)
              .display("&aClick to Accept")
              .lore(Arrays.asList(
                      "&7If either of you change",
                      "&7your offers during the countdown,",
                      "&7you will have to accept",
                      "&7the trade again."))
              .flag("HIDE_ATTRIBUTES")
              .damage((short) 14)
              .save(gui, "accept.my-icon");
    }

    cancel = new ItemFactory(gui, "accept.my-cancel");
    if (cancel.getStack() == null) {
      Material material = Material.matchMaterial(Sounds.version > 112 ? "GREEN_STAINED_GLASS_PANE" : "LEGACY_STAINED_GLASS_PANE");
      if (material == null) {
        material = Material.matchMaterial("STAINED_GLASS_PANE");
      }
      if (material == null) {
        material = Material.PAPER;
      }
      cancel = new ItemFactory(material)
              .display("&cClick to Cancel")
              .flag("HIDE_ATTRIBUTES")
              .damage((short) (Sounds.version > 112 ? 5 : 13))
              .save(gui, "accept.my-cancel");
    }

    complete = new ItemFactory(gui, "complete");
    if (complete.getStack() == null) {
      Material material = Material.matchMaterial(Sounds.version > 112 ? "GREEN_STAINED_GLASS_PANE" : "LEGACY_STAINED_GLASS_PANE");
      if (material == null) {
        material = Material.matchMaterial("STAINED_GLASS_PANE");
      }
      if (material == null) {
        material = Material.PAPER;
      }
      complete = new ItemFactory(material)
              .display("&aTrade Complete!")
              .lore(Arrays.asList(
                      "&fReview the final agreement and",
                      "&fclose the trade window",
                      "&fto retrieve your items!"))
              .flag("HIDE_ATTRIBUTES")
              .damage((short) (Sounds.version > 112 ? 5 : 13))
              .save(gui, "complete");
    }

    cancelled = new ItemFactory(gui, "cancelled");
    if (cancelled.getStack() == null) {
      cancelled = new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "RED_STAINED_GLASS_PANE" : "STAINED_GLASS_PANE"))
              .display("&cTrade Cancelled!")
              .lore(Arrays.asList(
                      "&7Your trading partner cancelled",
                      "&7the trade. Close the window",
                      "&7to retrieve your items."))
              .flag("HIDE_ATTRIBUTES")
              .damage((short) 14)
              .save(gui, "cancelled");
    }

    theirAccept = new ItemFactory(gui, "accept.their-icon");
    if (theirAccept.getStack() == null) {
      theirAccept = new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "GREEN_STAINED_GLASS_PANE" : "STAINED_GLASS_PANE"))
              .display("&aThey've accepted your offer.")
              .lore(Arrays.asList(
                      "&7If you're satisfied with the",
                      "&7trade as shown right now,",
                      "&7click your accept button!"))
              .flag("HIDE_ATTRIBUTES")
              .damage((short) 13)
              .save(gui, "accept.their-icon");
    }

    theirCancel = new ItemFactory(gui, "accept.their-cancel");
    if (theirCancel.getStack() == null) {
      theirCancel = new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "RED_STAINED_GLASS_PANE" : "STAINED_GLASS_PANE"))
              .display("&aYour partner is still considering.")
              .lore(Arrays.asList(
                      "&7Click your accept button to",
                      "&7signal that you like the trade",
                      "&7as it is now, or wait",
                      "&7for them to offer more!"))
              .flag("HIDE_ATTRIBUTES")
              .damage((short) 14)
              .save(gui, "accept.their-cancel");
    }

    separator = new ItemFactory(gui, "separator");
    if (separator.getStack() == null) {
      Material material = Material.matchMaterial(Sounds.version > 112 ? "BLACK_STAINED_GLASS_PANE" : "LEGACY_STAINED_GLASS_PANE");
      if (material == null) {
        material = Material.matchMaterial("STAINED_GLASS_PANE");
      }
      if (material == null) {
        material = Material.BARRIER;
      }
      separator = new ItemFactory(material)
              .display(" ")
              .flag("HIDE_ATTRIBUTES")
              .damage((short) (Sounds.version > 112 ? 15 : 0))
              .save(gui, "separator");
    }

    placeholder = new ItemFactory(gui, "placeholder");
    if (placeholder.getStack() == null) {
      placeholder = new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "BLACK_STAINED_GLASS_PANE" : "STAINED_GLASS_PANE"))
              .display(" ")
              .flag("HIDE_ATTRIBUTES")
              .damage((short) 15)
              .save(gui, "placeholder");
    }

    forceEnabled = gui.getBoolean("force.enabled", true);
    forceSlot = gui.getInt("force.slot", 49);
    force = new ItemFactory(gui, "force.icon");
    if (force.getStack() == null) {
      force = new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "CLOCK" : "WATCH"))
              .display("&4&lForce Trade")
              .lore(Arrays.asList(
                      "&7Click to force the trade",
                      "&7to countdown and accept as",
                      "&7it stands now."))
              .flag("HIDE_ATTRIBUTES")
              .save(gui, "force.icon");
    }
  }

  /**
   * Loads the configuration files from disk.
   * If a file does not exist, it is created and populated with default values.
   */
  public void load() {
    Sounds.loadSounds();

    configFile = new File(plugin.getDataFolder(), "config.yml");
    loadConfig();

    langFile = new File(plugin.getDataFolder(), "lang.yml");
    loadLang();

    guiFile = new File(plugin.getDataFolder(), "gui.yml");
    loadGui();

    save();
  }

  public void loadConfig() {
    if (configFile.exists()) {
      config = YamlConfiguration.loadConfiguration(configFile);
    } else {
      plugin.getDataFolder().mkdirs();
      try {
        configFile.createNewFile();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      config = YamlConfiguration.loadConfiguration(configFile);

      // Set default values for config.yml (new version with all features)
      config.set("aliases", Collections.singletonList("tradepro"));
      config.set("trade-command-compatible-mode", false);

      config.set("excess-chest.enabled", true);
      config.set("excess-chest.title", "&7Your inventory is full!");

      config.set("trade-logs", false);
      config.set("allow-same-ip-trade", true);

      config.set("permissions.required", false);
      config.set("permissions.send", "tradepro.send");
      config.set("permissions.accept", "tradepro.accept");

      config.set("requestcooldownseconds", 20);
      config.set("allow-trade-in-creative", false);
      config.set("allow-shift-right-click", true);

      config.set("blocked.blacklist", Arrays.asList("bedrock", "monster_egg"));
      config.set("blocked.named-items", false);
      config.set("blocked.lore", Collections.singletonList("EXAMPLE_BLOCKED_LORE"));
      config.set("blocked.regex", "");

      config.set("action", "crouchrightclick");

      config.set("ranges.sameworld", 10.0);
      config.set("ranges.crossworld", 0.0);
      config.set("ranges.allowcrossworld", false);
      config.set("ranges.blocked-worlds",
              Arrays.asList("ThisWorldDoesntExistButItsBlocked", "NeitherDoesThisOneButItIsToo"));

      config.set("antiscam.countdown", 10);
      config.set("antiscam.cancelonchange", true);
      config.set("antiscam.preventchangeonaccept", true);
      config.set("antiscam.discrepancy-detection", true);

      config.set("end-display.enabled", true);
      config.set("end-display.timer", 0);

      config.set("spectate.enabled", true);
      config.set("spectate.broadcast", true);

      config.set("extras.type.prefix", "&6&l!!&6> ");
      config.set("extras.type.empty", "&ePlease enter your %EXTRA% offer, or type 'cancel'.");
      config.set("extras.type.valid", "&aClick output slot to submit offer.");
      config.set("extras.type.invalid", "&cInvalid amount entered!");
      config.set("extras.type.maximum", "&cYou have %BALANCE% %EXTRA%");

      // Economy extras
      config.set("extras.economy.enabled", true);
      config.set("extras.economy.name", "money");
      config.set("extras.economy.material", "gold_ingot");
      config.set("extras.economy.display", "&eYour money offer is &6%AMOUNT%");
      config.set("extras.economy.theirdisplay", "&eTheir money offer is &6%AMOUNT%");
      config.set("extras.economy.lore", Arrays.asList("&fClick to edit your offer!", "&fYou have %BALANCE% money."));
      config.set("extras.economy.customModelData", 0);
      config.set("extras.economy.increment", 10.0);
      config.set("extras.economy.taxpercent", 0);
      config.set("extras.economy.mode", "chat");

      // Experience extras
      config.set("extras.experience.enabled", true);
      config.set("extras.experience.name", "experience points");
      config.set("extras.experience.material", Sounds.version < 113 ? "exp_bottle" : "experience_bottle");
      config.set("extras.experience.display", "&aYour XP offer is &2%AMOUNT% &c(%LEVELS% levels)");
      config.set("extras.experience.theirdisplay", "&aTheir XP offer is &2%AMOUNT% &a(+%LEVELS% levels)");
      config.set("extras.experience.lore", Arrays.asList("&fClick to edit your offer!", "&fYou have %BALANCE% XP."));
      config.set("extras.experience.customModelData", 0);
      config.set("extras.experience.increment", 5);
      config.set("extras.experience.taxpercent", 0);
      config.set("extras.experience.mode", "chat");
      config.set("extras.experience.levelMode", false);

      // PlayerPoints extras
      config.set("extras.playerpoints.enabled", true);
      config.set("extras.playerpoints.name", "player points");
      config.set("extras.playerpoints.material", "diamond");
      config.set("extras.playerpoints.display", "&bYour PlayerPoints offer is &3%AMOUNT%");
      config.set("extras.playerpoints.theirdisplay", "&bTheir PlayerPoints offer is &3%AMOUNT%");
      config.set("extras.playerpoints.lore", Arrays.asList("&fClick to edit your offer!", "&fYou have %BALANCE% player points."));
      config.set("extras.playerpoints.customModelData", 0);
      config.set("extras.playerpoints.increment", 5);
      config.set("extras.playerpoints.taxpercent", 0);
      config.set("extras.playerpoints.mode", "chat");

      // GriefPrevention extras
      config.set("extras.griefprevention.enabled", true);
      config.set("extras.griefprevention.name", "grief prevention");
      config.set("extras.griefprevention.material", Sounds.version > 112 ? "golden_shovel" : "gold_spade");
      config.set("extras.griefprevention.display", "&eYour GriefPrevention offer is &6%AMOUNT%");
      config.set("extras.griefprevention.theirdisplay", "&eTheir GriefPrevention offer is &6%AMOUNT%");
      config.set("extras.griefprevention.lore", Arrays.asList("&fClick to edit your offer!", "&fYou have %BALANCE% protection blocks."));
      config.set("extras.griefprevention.customModelData", 0);
      config.set("extras.griefprevention.increment", 1);
      config.set("extras.griefprevention.taxperecent", 0);
      config.set("extras.griefprevention.mode", "chat");

      // EnjinPoints extras
      config.set("extras.enjinpoints.enabled", false);
      config.set("extras.enjinpoints.name", "enjin points");
      config.set("extras.enjinpoints.material", "emerald");
      config.set("extras.enjinpoints.display", "&eYour EnjinPoints offer is &6%AMOUNT%");
      config.set("extras.enjinpoints.theirdisplay", "&eTheir EnjinPoints offer is &6%AMOUNT%");
      config.set("extras.enjinpoints.lore", Arrays.asList("&fClick to edit your offer!", "&fYou have %BALANCE% enjin points."));
      config.set("extras.enjinpoints.customModelData", 0);
      config.set("extras.enjinpoints.increment", 1);
      config.set("extras.enjinpoints.taxpercent", 0);
      config.set("extras.enjinpoints.mode", "chat");

      // TokenEnchant extras
      config.set("extras.tokenenchant.enabled", true);
      config.set("extras.tokenenchant.name", "token enchant points");
      config.set("extras.tokenenchant.material", "enchanted_book");
      config.set("extras.tokenenchant.display", "&eYour TokenEnchant tokens offer is &6%AMOUNT%");
      config.set("extras.tokenenchant.theirdisplay", "Their TokenEnchants tokens offer is &6%AMOUNT%");
      config.set("extras.tokenenchant.lore", Arrays.asList("&fClick to edit your offer!", "&fYou have %BALANCE% enchant tokens."));
      config.set("extras.tokenenchant.customModelData", 0);
      config.set("extras.tokenenchant.increment", 1);
      config.set("extras.tokenenchant.taxpercent", 0);
      config.set("extras.tokenenchant.mode", "chat");

      // TokenManager extras
      config.set("extras.tokenmanager.enabled", true);
      config.set("extras.tokenmanager.name", "tokens");
      config.set("extras.tokenmanager.material", "emerald");
      config.set("extras.tokenmanager.display", "&eYour tokens offer is &6%AMOUNT%");
      config.set("extras.tokenmanager.theirdisplay", "&eTheir tokens offer is &6%AMOUNT%");
      config.set("extras.tokenmanager.lore", Arrays.asList("&fClick to edit your offer!", "&fYou have %BALANCE% tokens."));
      config.set("extras.tokenmanager.customModelData", 0);
      config.set("extras.tokenmanager.increment", 1);
      config.set("extras.tokenmanager.taxpercent", 0);
      config.set("extras.tokenmanager.mode", "chat");

      // BeastTokens extras
      config.set("extras.beasttokens.enabled", true);
      config.set("extras.beasttokens.name", "tokens");
      config.set("extras.beasttokens.material", "emerald");
      config.set("extras.beasttokens.display", "&eYour tokens offer is &6%AMOUNT%");
      config.set("extras.beasttokens.theirdisplay", "&eTheir tokens offer is &6%AMOUNT%");
      config.set("extras.beasttokens.lore", Arrays.asList("&fClick to edit your offer!", "&fYou have %BALANCE% tokens."));
      config.set("extras.beasttokens.customModelData", 0);
      config.set("extras.beasttokens.increment", 1);
      config.set("extras.beasttokens.taxpercent", 0);
      config.set("extras.beasttokens.mode", "chat");

      // VotingPlugin extras
      config.set("extras.votingplugin.name", "vote points");
      config.set("extras.votingplugin.enabled", false);
      config.set("extras.votingplugin.material", "sunflower");
      config.set("extras.votingplugin.display", "&7Your current vote points offer is &b%AMOUNT%");
      config.set("extras.votingplugin.theirdisplay", "&7Their current vote points offer is &b%AMOUNT%");
      config.set("extras.votingplugin.lore", Arrays.asList("&fClick to edit your offer!"));
      config.set("extras.votingplugin.taxpercent", 0);

      config.set("hooks.factions.allow-trades-in-enemy-territory", false);
      config.set("hooks.worldguard.trading-flag", true);

      config.set("soundeffects.enabled", true);
      config.set("soundeffects.onchange", true);
      config.set("soundeffects.onaccept", true);
      config.set("soundeffects.oncomplete", true);
      config.set("soundeffects.oncountdown", true);

      config.set("debug-mode", false);

      // Mark the config version as the current version (3.85)
      config.set("configversion", 3.85);
    }
  }

  public void loadLang() {
    if (langFile.exists()) {
      lang = YamlConfiguration.loadConfiguration(langFile);
    } else {
      try {
        langFile.createNewFile();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
      lang = YamlConfiguration.loadConfiguration(langFile);

      lang.set("request.sent", "&6&l(!) &r&6You sent a trade request to &e%PLAYER%");
      lang.set("request.received.text", "&6&l(!) &r&6You received a trade request from &e%PLAYER%%NEWLINE%&6&l(!) &r&6Type &e/trade %PLAYER% &6to begin trading");
      lang.set("request.received.hover", "&6&lClick here to trade with &e&l%PLAYER%");
      lang.set("enabled", "&6&l(!) &r&6You &aenabled &6trade requests from other players!");
      lang.set("disabled", "&6&l(!) &r&6You &cdisabled &6trade requests from other players!");
      lang.set("errors.creative", "&4&l(!) &r&4You can't trade in creative mode!");
      lang.set("errors.creative-them", "&4&l(!) &r&4That player is in creative mode!");
      lang.set("errors.within-range.same-world", "&4&l(!) &r&4You must be within %AMOUNT% blocks of a player to trade with them");
      lang.set("errors.within-range.cross-world", "&4&l(!) &r&4You must be within %AMOUNT% blocks of a player%NEWLINE%&4&l(!) &r&4in a different world to trade with them!");
      lang.set("errors.no-cross-world", "&4&l(!) &r&4You must be in the same world as a player to trade with them!");
      lang.set("errors.same-ip", "&4&l(!) &4Players aren't allowed to trade on same IP!");
      lang.set("accept.sender", "&6&l(!) &r&e%PLAYER% &6accepted your trade request");
      lang.set("accept.receiver", "&6&l(!) &r&6You accepted &e%PLAYER%'s &6trade request");
      lang.set("cancelled", "&4&l(!) &r&4The trade was cancelled");
      lang.set("expired", "&4&l(!) &r&4Your last trade request expired");
      lang.set("errors.wait-for-expire", "&4&l(!) &r&4You still have an active trade request%NEWLINE%&4&l(!) &r&4It will expire shortly");
      lang.set("errors.player-not-found", "&4&l(!) &r&4Could not find specified player");
      lang.set("errors.self-trade", "&4&l(!) &r&4You cannot trade with yourself");
      lang.set("errors.blocked-world", "&4&l(!) &r&4You can't trade in this world.");
      lang.set("errors.invalid-usage", "&4&l(!) &r&4Invalid arguments. Usage: %NEWLINE%    &c- /trade <player name>%NEWLINE%    &c- /trade deny");
      lang.set("errors.trading-disabled", "&4&l(!) &r&4That player has trading toggled off.");
      lang.set("errors.no-perms.accept", "&4&l(!) &r&4You do not have permission to trade");
      lang.set("errors.no-perms.send", "&4&l(!) &r&4You do not have permission to send a trade");
      lang.set("errors.no-perms.receive", "&4&l(!) &r&4That player does not have permission to accept a trade");
      lang.set("errors.no-perms.admin", "&4&l(!) &r&4You do not have permission to use this command");
      lang.set("trade-complete", "&6&l(!) &r&6The trade was successful!");
      lang.set("forced-trade", "&6&l(!) &r&6You've been forced into a trade with &e%PLAYER%");
      lang.set("denied.them", "&4&l(!) &r&4Your trade request to &c%PLAYER% &4was denied");
      lang.set("denied.you", "&4&l(!) &r&4Any recent incoming trade requests have been denied.");
      lang.set("spectate.text", "&6&l(!) &e%PLAYER1% &6and &e%PLAYER2% &6have started a trade %NEWLINE%&6&l(!) &6Type &e/tradepro spectate %PLAYER1% %PLAYER2% &6to spectate");
      lang.set("spectate.hover", "&6&lClick here to spectate this trade");
      lang.set("antiscam.discrepancy", "&4&l(!) &r&4A discrepancy was detected in the traded items.%NEWLINE%&4&l(!) &4The trade has been cancelled.");
      lang.set("admin.configs-reloaded", "&6&l(!) &6Configs reloaded!");
      lang.set("admin.invalid-players", "&4&l(!) &4Invalid players!");
      lang.set("admin.forced-trade", "&6&l(!) &6You forced a trade between &e%PLAYER1% &6and &e%PLAYER2%");
      lang.set("admin.players-only", "&4&l(!) &4This command is for players only.");
      lang.set("admin.no-trade", "&4&l(!) &4No trade was found with those arguments.");
      lang.set("hooks.factions.enemy-territory", "&4&l(!) &4You can't trade in enemy territory!");
      lang.set("hooks.worldguard.trading-not-allowed", "&4&l(!) &4You can't trade in this area.");
    }
  }

  public void loadGui() {
    if (guiFile.exists()) {
      gui = YamlConfiguration.loadConfiguration(guiFile);
    } else {
      try {
        guiFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
      gui = YamlConfiguration.loadConfiguration(guiFile);

      gui.set("title", "Your Items <|     |> Their Items");
      gui.set("spectator-title", "Player 1 <|         |> Player 2");

      gui.set("my-slots",
              Stream.of(1, 2, 3, 9, 10, 11, 12, 18, 19, 20, 21, 27, 28, 29, 30, 36, 37, 38, 39, 45, 46, 47, 48)
                      .map(i -> Integer.toString(i))
                      .collect(Collectors.toList()));
      gui.set("their-slots",
              Stream.of(5, 6, 7, 14, 15, 16, 17, 23, 24, 25, 26, 32, 33, 34, 35, 41, 42, 43, 44, 50, 51, 52, 53)
                      .map(i -> Integer.toString(i))
                      .collect(Collectors.toList()));

      gui.set("my-extra-slots", Arrays.asList("45", "46", "47", "48", "39", "38", "37", "36", "27", "28", "29", "30"));
      gui.set("their-extra-slots", Arrays.asList("50", "51", "52", "53", "44", "43", "42", "41", "32", "33", "34", "35"));

      gui.set("head.enabled", true);
      gui.set("head.display-name", "&7You are trading with: &3&l%PLAYER%");

      gui.set("force.enabled", true);
      gui.set("force.slot", 49);
      new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "CLOCK" : "WATCH"))
              .display("&4&lForce Trade")
              .lore(Arrays.asList("&7Click to force the trade", "&7to countdown and accept as", "&7it stands now."))
              .flag("HIDE_ATTRIBUTES")
              .save(gui, "force.icon");

      gui.set("accept.enabled", true);
      gui.set("accept.my-slot", 0);
      gui.set("accept.their-slot", 8);
      new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "RED_STAINED_GLASS_PANE" : "STAINED_GLASS_PANE"))
              .display("&aClick to Accept")
              .lore(Arrays.asList(
                      "&7If either of you change",
                      "&7your offers during the countdown,",
                      "&7you will have to accept",
                      "&7the trade again."))
              .flag("HIDE_ATTRIBUTES")
              .damage((short) 14)
              .save(gui, "accept.my-icon");

      new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "GREEN_STAINED_GLASS_PANE" : "STAINED_GLASS_PANE"))
              .display("&cClick to Cancel")
              .flag("HIDE_ATTRIBUTES")
              .damage((short) 13)
              .save(gui, "accept.my-cancel");

      new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "GREEN_STAINED_GLASS_PANE" : "STAINED_GLASS_PANE"))
              .display("&aTrade Complete!")
              .lore(Arrays.asList(
                      "&fReview the final agreement and",
                      "&fclose the trade window",
                      "&fto retrieve your items!"))
              .flag("HIDE_ATTRIBUTES")
              .damage((short) 13)
              .save(gui, "complete");

      new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "RED_STAINED_GLASS_PANE" : "STAINED_GLASS_PANE"))
              .display("&cTrade Cancelled!")
              .lore(Arrays.asList(
                      "&7Your trading partner cancelled",
                      "&7the trade. Close the window",
                      "&7to retrieve your items."))
              .flag("HIDE_ATTRIBUTES")
              .damage((short) 14)
              .save(gui, "cancelled");

      new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "GREEN_STAINED_GLASS_PANE" : "STAINED_GLASS_PANE"))
              .display("&aThey've accepted your offer.")
              .lore(Arrays.asList(
                      "&7If you're satisfied with the",
                      "&7trade as shown right now,",
                      "&7click your accept button!"))
              .flag("HIDE_ATTRIBUTES")
              .damage((short) 13)
              .save(gui, "accept.their-icon");

      new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "RED_STAINED_GLASS_PANE" : "STAINED_GLASS_PANE"))
              .display("&aYour partner is still considering.")
              .lore(Arrays.asList(
                      "&7Click your accept button to",
                      "&7signal that you like the trade",
                      "&7as it is now, or wait",
                      "&7for them to offer more!"))
              .flag("HIDE_ATTRIBUTES")
              .damage((short) 14)
              .save(gui, "accept.their-cancel");

      new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "BLACK_STAINED_GLASS_PANE" : "STAINED_GLASS_PANE"))
              .display(" ")
              .flag("HIDE_ATTRIBUTES")
              .damage((short) 15)
              .save(gui, "separator");

      new ItemFactory(Material.getMaterial(Sounds.version > 112 ? "BLACK_STAINED_GLASS_PANE" : "STAINED_GLASS_PANE"))
              .display(" ")
              .flag("HIDE_ATTRIBUTES")
              .damage((short) 15)
              .save(gui, "placeholder");
    }
  }
}
