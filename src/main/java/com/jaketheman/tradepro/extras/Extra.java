package com.jaketheman.tradepro.extras;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.Expose;
import com.jaketheman.tradepro.TradePro;
import com.jaketheman.tradepro.trade.Trade;
import com.jaketheman.tradepro.util.ItemFactory;
import com.jaketheman.tradepro.util.MsgUtils;
import com.jaketheman.tradepro.util.Sounds;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.logging.Level;
import net.wesjd.anvilgui.AnvilGUI;

public abstract class Extra implements Listener {

  static final DecimalFormat decimalFormat = new DecimalFormat("###,##0.##");
  /** Set once if AnvilGUI cannot open on this server; extras then use the chat prompt. */
  private static volatile boolean anvilUnavailable = false;
  public final String name;
  final ItemStack icon;
  final Player player1;

  @Expose(serialize = false, deserialize = false) // Exclude from serialization
  private Conversation convo1, convo2;

  final Player player2;
  final double increment;
  final ItemStack theirIcon;
  final double taxPercent;
  public double value1 = 0, value2 = 0;
  double increment1;
  double increment2;
  @Getter
  private String displayName;
  private TradePro pl;
  private double max1;
  private double max2;
  private long lastUpdatedMax = System.currentTimeMillis();
  private String mode;
  private Trade trade;

  Extra(String name, Player player1, Player player2, TradePro pl, Trade trade) {
    pl.getServer().getPluginManager().registerEvents(this, pl);
    this.pl = pl;
    this.name = name;
    ConfigurationSection section =
            Preconditions.checkNotNull(pl.getConfig().getConfigurationSection("extras." + name));
    this.displayName = section.getString("name");
    this.player1 = player1;
    this.player2 = player2;
    this.increment = section.getDouble("increment", 1D);
    this.increment1 = increment;
    this.increment2 = increment;
    ItemFactory factory =
            new ItemFactory(section.getString("material", "PAPER"), Material.PAPER)
                    .display(section.getString("display", "&4ERROR"))
                    .customModelData(section.getInt("customModelData", 0));
    if (section.contains("lore")) factory.lore(section.getStringList("lore"));
    this.icon = factory.flag("HIDE_ATTRIBUTES").build();
    this.theirIcon =
            new ItemFactory(section.getString("material", "PAPER"), Material.PAPER)
                    .display(section.getString("theirdisplay", "&4ERROR"))
                    .customModelData(section.getInt("customModelData", 0))
                    .build();
    this.taxPercent = section.getDouble("taxpercent", 0);
    // anvil = AnvilGUI prompt (falls back to the chat prompt if this server can't open it),
    // chat = chat prompt, anything else = click to adjust
    this.mode = section.getString("mode", "anvil").toLowerCase();
    if (mode.equals("type")) {
      mode = "chat";
      section.set("mode", "chat");
      pl.saveConfig();
    }
    this.trade = trade;
  }

  public void init() {
    this.max1 = getMax(player1);
    this.max2 = getMax(player2);
    this.pl.log("'" + name + "' extra initialized. Balances: [" + max1 + ", " + max2 + "]");
  }

  public void onClick(Player player, ClickType click) {
    double offer = player1.equals(player) ? value1 : value2;
    if (mode.equals("anvil") && !anvilUnavailable && openAnvilPrompt(player)) {
      // anvil prompt is open; the amount is applied from its click handler
    } else if (mode.equals("chat") || mode.equals("anvil")) {
      trade.setCancelOnClose(player, false);
      player.closeInventory();
      Conversation convo =
              new ConversationFactory(pl)
                      .withPrefix(
                              conversationContext ->
                                      MsgUtils.color(pl.getTradeConfig().getExtrasTypePrefix()))
                      .withFirstPrompt(
                              new StringPrompt() {
                                @Override
                                public Prompt acceptInput(
                                        ConversationContext conversationContext, String input) {
                                  if (trade.isCancelled()) return null;
                                  if (input == null || input.equalsIgnoreCase("cancel")) return null;
                                  Number number;
                                  try {
                                    number = Double.parseDouble(input);
                                  } catch (NumberFormatException ignored) {
                                    player.sendMessage(pl.getTradeConfig().getExtrasTypePrefix() + pl.getTradeConfig().getExtrasTypeInvalid());
                                    return this;
                                  }
                                  if (!isValidAmount(number.doubleValue()) || number.doubleValue() < 0 || number.doubleValue() > getMax(player)) {
                                    return new StringPrompt() {
                                      @Override
                                      public Prompt acceptInput(
                                              ConversationContext conversationContext, String input) {
                                        if (trade.isCancelled()) return null;
                                        if (input == null || input.equalsIgnoreCase("cancel")) return null;
                                        Number number;
                                        try {
                                          number = Double.parseDouble(input);
                                        } catch (NumberFormatException ignored) {
                                          return this;
                                        }
                                        if (!isValidAmount(number.doubleValue()) || number.doubleValue() < 0 || number.doubleValue() > getMax(player)) {
                                          return this;
                                        }
                                        if (player1.equals(player)) {
                                          setValue1(number.doubleValue());
                                        } else if (player2.equals(player)) {
                                          setValue2(number.doubleValue());
                                        }
                                        updateMax(false);
                                        return null;
                                      }

                                      @Override
                                      public String getPromptText(ConversationContext conversationContext) {
                                        return pl.getTradeConfig()
                                                .getExtrasTypeMaximum()
                                                .replace("%BALANCE%", decimalFormat.format(getMax(player)))
                                                .replace("%EXTRA%", displayName);
                                      }
                                    };
                                  }
                                  if (player1.equals(player)) {
                                    setValue1(number.doubleValue());
                                  } else if (player2.equals(player)) {
                                    setValue2(number.doubleValue());
                                  }
                                  updateMax(false);
                                  return null;
                                }

                                @Override
                                public String getPromptText(ConversationContext conversationContext) {
                                  return pl.getTradeConfig()
                                          .getExtrasTypeEmpty()
                                          .replace("%BALANCE%", decimalFormat.format(getMax(player)))
                                          .replace("%AMOUNT%", decimalFormat.format(offer))
                                          .replace("%EXTRA%", displayName);
                                }
                              })
                      .withTimeout(30)
                      .addConversationAbandonedListener(
                              event -> {
                                if (trade.isCancelled()) return;
                                if (!event.gracefulExit()) Sounds.villagerHmm(player, 1f);
                                trade.open(player);
                                trade.updateExtras();
                                trade.setCancelOnClose(player, true);
                              })
                      .buildConversation(player);
      if (player.equals(player1)) {
        convo1 = convo;
      } else {
        convo2 = convo;
      }
      convo.begin();
    } else {
      if (click.isLeftClick()) {
        if (click.isShiftClick()) {
          if (player.equals(player1)) {
            increment1 -= increment;
          } else if (player.equals(player2)) {
            increment2 -= increment;
          }
        } else {
          if (player.equals(player1)) {
            value1 -= increment1;
          } else if (player.equals(player2)) {
            value2 -= increment2;
          }
        }
      } else if (click.isRightClick()) {
        if (click.isShiftClick()) {
          if (player.equals(player1)) {
            increment1 += increment;
          } else if (player.equals(player2)) {
            increment2 += increment;
          }
        } else {
          if (player.equals(player1)) {
            value1 += increment1;
          } else if (player.equals(player2)) {
            value2 += increment2;
          }
        }
      }
    }
    if (increment1 < 0) increment1 = 0;
    if (increment2 < 0) increment2 = 0;

    if (value1 < 0) value1 = 0;
    if (value2 < 0) value2 = 0;

    updateMax(true);
  }

  private static boolean isValidAmount(double amount) {
    return !Double.isNaN(amount) && !Double.isInfinite(amount);
  }

  /**
   * Opens the AnvilGUI amount prompt. Returns false (and remembers it) if this server version can't open it,
   * so the caller can fall back to the chat prompt instead of leaving the trade in a broken state.
   */
  private boolean openAnvilPrompt(Player player) {
    trade.setCancelOnClose(player, false);
    try {
      new AnvilGUI.Builder()
          .plugin(pl)
          .text("0")
          .title("Enter Amount")
          .itemLeft(new ItemStack(Material.PAPER))
          .onClose(stateSnapshot -> {
            if (trade.isCancelled()) return;
            trade.open(player);
            trade.updateExtras();
            trade.setCancelOnClose(player, true);
          })
          .onClick((slot, stateSnapshot) -> {
            pl.log("Anvil input click by " + player.getName() + " on slot " + slot + " text='" + stateSnapshot.getText() + "'");
            if (slot != AnvilGUI.Slot.OUTPUT) {
              return Collections.emptyList();
            }
            if (trade.isCancelled()) return Collections.singletonList(AnvilGUI.ResponseAction.close());

            double amount;
            try {
              amount = Double.parseDouble(stateSnapshot.getText().trim());
            } catch (NumberFormatException ignored) {
              player.sendMessage(pl.getTradeConfig().getExtrasTypePrefix() + pl.getTradeConfig().getExtrasTypeInvalid());
              return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("Invalid amount"));
            }
            if (!isValidAmount(amount)) {
              return Collections.singletonList(AnvilGUI.ResponseAction.replaceInputText("Invalid amount"));
            }
            if (amount < 0 || amount > getMax(player)) {
              return Collections.singletonList(
                  AnvilGUI.ResponseAction.replaceInputText("Max: " + decimalFormat.format(getMax(player))));
            }

            if (player1.equals(player)) {
              setValue1(amount);
            } else if (player2.equals(player)) {
              setValue2(amount);
            }
            updateMax(false);
            return Collections.singletonList(AnvilGUI.ResponseAction.close());
          })
          .open(player);
      return true;
    } catch (Throwable t) {
      // AnvilGUI drives server internals, so it is the one piece that can fail on a server version it has not
      // been updated for.
      anvilUnavailable = true;
      pl.getLogger().log(Level.WARNING, "Anvil amount input failed on this server version (" + t
          + "). Using the chat prompt for extras until the next restart.", t);
      trade.setCancelOnClose(player, true);
      return false;
    }
  }

  public void setValue1(double value1) {
    this.value1 = value1;
  }

  public void setValue2(double value2) {
    this.value2 = value2;
  }

  public boolean updateMax(boolean delay) {
    long now = System.currentTimeMillis();
    if (!delay || now > lastUpdatedMax + 5000) {
      max1 = getMax(player1);
      max2 = getMax(player2);
      lastUpdatedMax = now;
    }
    boolean updated = false;
    if (value1 > max1) {
      value1 = max1;
      updated = true;
    }
    if (value2 > max2) {
      value2 = max2;
      updated = true;
    }
    return updated;
  }

  public abstract double getMax(Player player);

  public abstract void onTradeEnd();

  public void onCancel() {
    if (convo1 != null && player1.isConversing()) player1.abandonConversation(convo1);
    if (convo2 != null && player2.isConversing()) player2.abandonConversation(convo2);
  }

  public ItemStack getIcon(Player player) {
    return ItemFactory.replaceInMeta(
            _getIcon(player),
            "%BALANCE%",
            decimalFormat.format(getMax(player)),
            "%EXTRA%",
            displayName);
  }

  public ItemStack getTheirIcon(Player player) {
    return ItemFactory.replaceInMeta(
            _getTheirIcon(player),
            "%BALANCE%",
            decimalFormat.format(getMax(player1.equals(player) ? player2 : player1)),
            "%EXTRA%",
            displayName);
  }

  protected abstract ItemStack _getIcon(Player player);

  protected abstract ItemStack _getTheirIcon(Player player);
}