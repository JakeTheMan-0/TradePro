package com.jaketheman.tradepro.trade;

import com.jaketheman.tradepro.TradePro;
import com.jaketheman.tradepro.events.TradeCompleteEvent;
import com.jaketheman.tradepro.extras.*;
import com.jaketheman.tradepro.extras.*;
import com.jaketheman.tradepro.logging.TradeLog;
import com.jaketheman.tradepro.util.InvUtils;
import com.jaketheman.tradepro.util.ItemFactory;
import org.bukkit.inventory.meta.ItemMeta;
import com.jaketheman.tradepro.util.Sounds;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class Trade implements Listener {

  @Getter
  public final Player player1, player2;
  private final TradePro pl = TradePro.getPlugin(TradePro.class);
  private List<Integer> mySlots, theirSlots, myExtraSlots, theirExtraSlots;
  private final List<Extra> extras = new ArrayList<>();
  private final Map<Integer, Extra> placedExtras = new HashMap<>();
  private final long startTime = System.currentTimeMillis();
  private boolean cancelOnClose1 = true, cancelOnClose2 = true;
  @Getter
  private Inventory spectatorInv, inv1, inv2;
  private boolean accept1, accept2;
  private Location location1, location2;
  private ItemStack[] accepted1, accepted2;
  private boolean forced = false;
  private BukkitTask task;
  @Getter
  private boolean cancelled;
  private EntityPickupItemEventListener entityPickupListener;

  public Trade(Player p1, Player p2) {
    player1 = p1;
    player2 = p2;
    location1 = p1.getLocation();
    location2 = p2.getLocation();
    pl.getTaskFactory()
            .newChain()
            .sync(
                    () -> {
                      inv1 = InvUtils.getTradeInventory(player1, player2);
                      inv2 = InvUtils.getTradeInventory(player2, player1);
                      spectatorInv = InvUtils.getSpectatorInventory(player1, player2);
                    })
            .async(
                    () -> {
                      if (pl.getTradeConfig().isSpectateEnabled()
                              && pl.getTradeConfig().isSpectateBroadcast())
                        Bukkit.getOnlinePlayers()
                                .forEach(
                                        p -> {
                                          if (p.hasPermission("tradepro.admin")
                                                  && !p.hasPermission("tradepro.admin.silent")) {
                                            pl.getTradeConfig()
                                                    .getSpectateMessage()
                                                    .setOnClick(
                                                            "/tradepro spectate "
                                                                    + player1.getName()
                                                                    + " "
                                                                    + player2.getName())
                                                    .send(
                                                            p,
                                                            "%PLAYER1%",
                                                            player1.getName(),
                                                            "%PLAYER2%",
                                                            player2.getName());
                                          }
                                        });
                      if (pl.getConfig().getBoolean("extras.economy.enabled", true)
                              && pl.getServer().getPluginManager().isPluginEnabled("Vault")) {
                        try {
                          if (pl.getServer()
                                  .getServicesManager()
                                  .getRegistration(Class.forName("net.milkbowl.vault.economy.Economy"))
                                  != null) {
                            extras.add(new EconomyExtra(player1, player2, pl, this));
                          }
                        } catch (Exception ignored) {
                        }
                      }
                      if (pl.getConfig().getBoolean("extras.experience.enabled", true)) {
                        extras.add(new ExperienceExtra(player1, player2, pl, this));
                      }
                      if (pl.getConfig().getBoolean("extras.playerpoints.enabled", true)
                              && pl.getServer().getPluginManager().isPluginEnabled("PlayerPoints")) {
                        extras.add(new PlayerPointsExtra(player1, player2, pl, this));
                      }
                      if (pl.getConfig().getBoolean("extras.griefprevention.enabled", true)
                              && pl.getServer().getPluginManager().isPluginEnabled("GriefPrevention")) {
                        extras.add(new GriefPreventionExtra(player1, player2, pl, this));
                      }
                      if (pl.getConfig().getBoolean("extras.enjinpoints.enabled", false)
                              && pl.getServer().getPluginManager().isPluginEnabled("EnjinMinecraftPlugin")) {
                        extras.add(new EnjinPointsExtra(player1, player2, pl, this));
                      }
                      if (pl.getConfig().getBoolean("extras.tokenenchant.enabled", true)
                              && pl.getServer().getPluginManager().isPluginEnabled("TokenEnchant")) {
                        extras.add(new TokenEnchantExtra(player1, player2, pl, this));
                      }
                      if (pl.getConfig().getBoolean("extras.tokenmanager.enabled", true)
                              && pl.getServer().getPluginManager().isPluginEnabled("TokenManager")) {
                        extras.add(new TokenManagerExtra(player1, player2, pl, this));
                      }
                      if (pl.getConfig().getBoolean("extras.beasttoken.enabled", true)
                              && pl.getServer().getPluginManager().isPluginEnabled("BeastToken")) {
                        extras.add(new BeastTokensExtra(player1, player2, pl, this));
                      }
                      if (pl.getConfig().getBoolean("extras.votingplugin.enabled", false)
                              && pl.getServer().getPluginManager().isPluginEnabled("VotingPlugin")) {
                        extras.add(new VotingPluginExtra(player1, player2, pl, this));
                      }
                    })
            .sync(
                    () -> {
                      Bukkit.getServer().getPluginManager().registerEvents(this, pl);
                      try {
                        Class.forName("org.bukkit.event.entity.EntityPickupItemEvent");
                        entityPickupListener = new EntityPickupItemEventListener(this);
                        Bukkit.getServer().getPluginManager().registerEvents(entityPickupListener, pl);
                      } catch (ClassNotFoundException ignored) {
                      }

                      this.mySlots = pl.getTradeConfig().getMySlots();
                      this.theirSlots = pl.getTradeConfig().getTheirSlots();
                      this.myExtraSlots = pl.getTradeConfig().getMyExtraSlots();
                      this.theirExtraSlots = pl.getTradeConfig().getTheirExtraSlots();

                      for (Extra extra : extras) {
                        extra.init();
                      }
                      updateExtras();

                      updateAcceptance();
                    })
            .sync(
                    () -> {
                      pl.ongoingTrades.add(this);
                      player1.openInventory(inv1);
                      player2.openInventory(inv2);
                    })
            .execute();
  }

  private static List<ItemStack> combine(ItemStack[] items) {
    List<ItemStack> result = new ArrayList<>();
    for (int i = 0; i < items.length; i++) {
      ItemStack item = items[i];
      if (item == null) continue;
      item = item.clone();
      for (int j = i + 1; j < items.length; j++) {
        ItemStack dupe = items[j];
        if (item.isSimilar(dupe)) {
          item.setAmount(item.getAmount() + dupe.getAmount());
          items[j] = null;
        }
      }
      result.add(item);
    }
    return result;
  }

  @EventHandler
  public void onDrag(InventoryDragEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) {
      return;
    }

    Player player = (Player) event.getWhoClicked();
    Inventory inv = event.getInventory();

    // Block spectator interactions
    if (inv.equals(spectatorInv)) {
      event.setCancelled(true);
      return;
    }

    // Check if this is a trade inventory
    if (inv1.getViewers().contains(player) || inv2.getViewers().contains(player)) {
      // Prevent changes if both players have accepted the trade
      if (accept1 && accept2) {
        event.setCancelled(true);
        return;
      }

      // Validate dragged slots
      for (int slot : event.getInventorySlots()) {
        if (!mySlots.contains(slot)) {
          event.setCancelled(true);
          return;
        }
      }

      // Prevent changes when trade is accepted by the current player
      if (pl.getTradeConfig().isPreventChangeOnAccept()
              && ((player.equals(player1) && accept1) || (player.equals(player2) && accept2))) {
        event.setCancelled(true);
        return;
      }

      // Trigger inventory updates and click logic
      Bukkit.getScheduler().runTaskLater(pl, this::updateInventories, 1L);
      click();
    }
  }


  @EventHandler
  public void onClick(InventoryClickEvent event) {
    if (!(event.getWhoClicked() instanceof Player)) {
      return;
    }

    Player player = (Player) event.getWhoClicked();
    Inventory inv = event.getClickedInventory();
    if (inv == null) return;

    ClickType click = event.getClick();

    // Block spectators from interacting with the trade
    if (spectatorInv.equals(inv)) {
      event.setCancelled(true);
      return;
    }

    // Verify the inventory belongs to this trade
    if (!(inv1.getViewers().contains(player) || inv2.getViewers().contains(player))) {
      return;
    }

    int slot = event.getSlot();

    // Prevent actions in the top inventory (trade inventory)
    if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
      // Cancel double-click events
      if (click.equals(ClickType.DOUBLE_CLICK)) {
        event.setCancelled(true);
        return;
      }

      // Prevent shift-clicking for invalid slots
      if (!mySlots.contains(slot) &&
              (click.equals(ClickType.SHIFT_LEFT)
                      || click.equals(ClickType.SHIFT_RIGHT))) {
        event.setCancelled(true);
        return;
      }

      // Cancel all actions if the trade is canceled
      if (cancelled) {
        event.setCancelled(true);
        return;
      }

      // Handle interactions with trade slots
      if (slot != pl.getTradeConfig().getAcceptSlot()
              && mySlots.contains(slot)
              && getExtra(slot) == null) {
        if (accept1 && accept2) {
          event.setCancelled(true);
          return;
        }

        // Prevent changes if either player has accepted the trade
        if (pl.getTradeConfig().isPreventChangeOnAccept()
                && ((player.equals(player1) && accept1) || (player.equals(player2) && accept2))) {
          event.setCancelled(true);
          event.setResult(InventoryClickEvent.Result.DENY);
          return;
        }

        // Reset trade acceptance if configured
        if (pl.getTradeConfig().isAntiscamCancelOnChange()) {
          accept1 = false;
          accept2 = false;
          updateAcceptance();
        }

        Bukkit.getScheduler().runTaskLater(pl, this::updateInventories, 1L);
        click();
      } else {
        event.setCancelled(true);
        event.setResult(InventoryClickEvent.Result.DENY);
        ItemStack item = inv.getItem(slot);
        if (item != null) {
          // Handle toggle buttons
          if (slot == pl.getTradeConfig().getAcceptSlot()) {
            if (!forced) {
              ItemStack cursor = player.getItemOnCursor();
              if (cursor == null || cursor.getType() == Material.AIR) {
                if (player.equals(player1)) {
                  accept1 = !accept1;
                } else {
                  accept2 = !accept2;
                }
              }

              updateAcceptance();
              checkAcceptance();
            }
            // Handle force trade button
          } else if (slot == 49 && player.hasPermission("tradepro.admin")) {
            forced = !forced;
            accept1 = forced;
            accept2 = forced;
            updateAcceptance();
            checkAcceptance();
            // Handle extras or cancel buttons
          } else {
            Extra extra = getExtra(slot);
            if (extra != null) {
              if (pl.getTradeConfig().isPreventChangeOnAccept()
                      && ((player.equals(player1) && accept1) || (player.equals(player2) && accept2))) {
                return;
              }
              if (task != null) {
                return;
              }
              extra.onClick(player, event.getClick());
              updateExtras();
              click();
            }
          }
        }
      }
    } else if (player.getInventory().equals(inv)) { // Bottom inventory
      if (cancelled) {
        event.setCancelled(true);
        return;
      }

      Inventory open = player.getOpenInventory().getTopInventory();

      // Handle double-click in the player's inventory
      if (click.equals(ClickType.DOUBLE_CLICK)) {
        event.setCancelled(true);
        if (accept1 && accept2) {
          return;
        }

        ItemStack item = event.getCurrentItem();
        ItemStack cursor = player.getItemOnCursor();
        if ((item == null || item.getType().equals(Material.AIR))
                && (cursor != null && !cursor.getType().equals(Material.AIR))) {
          for (int j : mySlots) {
            if (j == pl.getTradeConfig().getAcceptSlot() || getExtra(j) != null) continue;
            ItemStack i = open.getItem(j);
            if (i != null && cursor.isSimilar(i)) {
              int amount = cursor.getAmount() + i.getAmount();
              if (amount <= cursor.getMaxStackSize()) {
                open.setItem(j, null);
                cursor.setAmount(amount);
              } else {
                int remaining = amount - cursor.getMaxStackSize();
                i.setAmount(remaining);
                cursor.setAmount(cursor.getMaxStackSize());
                break;
              }
            }
          }
        }

        if (pl.getTradeConfig().isAntiscamCancelOnChange()) {
          accept1 = false;
          accept2 = false;
          updateAcceptance();
        }
        // Handle shift-click in the player's inventory
      } else if (click.name().contains("SHIFT")) {
        event.setCancelled(true);
        if (isBlocked(event.getCurrentItem())) {
          Sounds.villagerHit(player, 1);
          return;
        }

        if (accept1 && accept2) {
          return;
        }
        if (pl.getTradeConfig().isAntiscamCancelOnChange()) {
          accept1 = false;
          accept2 = false;
          updateAcceptance();
        }
        ItemStack current = event.getCurrentItem();
        if (current != null) {
          int amount = click.name().contains("LEFT") ? current.getMaxStackSize() : 1;
          player
                  .getInventory()
                  .setItem(
                          event.getSlot(),
                          putOnLeft(player.equals(player1) ? inv1 : inv2, current, amount));
          click();
        }
      }

      // Prevent changes after trade acceptance
      if (pl.getTradeConfig().isPreventChangeOnAccept()
              && ((player.equals(player1) && accept1) || (player.equals(player2) && accept2))) {
        event.setCancelled(true);
        return;
      }

      accept1 = false;
      accept2 = false;
      updateAcceptance();

      Bukkit.getScheduler().runTaskLater(pl, this::updateInventories, 1L);
    }
  }


  // plays a click sound effect to all viewers
  private void click() {
    if (pl.getTradeConfig().isSoundEffectsEnabled() && pl.getTradeConfig().isSoundOnChange()) {
      Sounds.click(player1, 2);
      Sounds.click(player2, 2);
      spectatorInv.getViewers().stream()
              .filter(Player.class::isInstance)
              .forEach(p -> Sounds.click((Player) p, 2));
    }
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    if (!(event.getPlayer().equals(player1) || event.getPlayer().equals(player2))) return;
    if (cancelled) return;
    cancelled = true;
    Player otherPlayer = event.getPlayer().equals(player1) ? player2 : player1;
    Inventory otherInv = event.getPlayer().equals(player1) ? inv2 : inv1;
    Inventory inv = event.getPlayer().equals(player1) ? inv1 : inv2;
    giveItemsOnLeft(inv, event.getPlayer());
    giveItemsOnLeft(otherInv, otherPlayer);
    pl.getTradeConfig().getCancelledMessage().send(otherPlayer, "%PLAYER%", event.getPlayer().getName());
    if (otherInv.getViewers().contains(otherPlayer)) {
      otherPlayer.closeInventory();
    }
  }

  @EventHandler
  public void onClose(InventoryCloseEvent event) {
    Inventory closed = event.getInventory();
    if (closed == null || closed.getSize() < 54) {
      return;
    }

    // I keep having issues with
    // identifying inventories so
    // trying to make sure it catches
    // all events
    if (closed.equals(inv1)
            || closed.equals(inv2)
            || inv1.getViewers().contains(event.getPlayer())
            || inv2.getViewers().contains(event.getPlayer())) {
      if ((event.getPlayer().equals(player1) && !cancelOnClose1)
              || (event.getPlayer().equals(player2) && !cancelOnClose2)) {
        return;
      }

      giveOnCursor((Player) event.getPlayer());

      // Return items to them
      giveItemsOnLeft(closed, (Player) event.getPlayer());

      Bukkit.getScheduler()
              .runTaskLater(
                      pl,
                      () -> {
                        if (inv1.getViewers().isEmpty()
                                && inv2.getViewers().isEmpty()
                                && spectatorInv.getViewers().isEmpty()) {
                          HandlerList.unregisterAll(this);
                          if (entityPickupListener != null)
                            HandlerList.unregisterAll(entityPickupListener);
                        }
                      },
                      1L);

      if (cancelled) {
        return;
      }

      cancel(false);

      pl.ongoingTrades.remove(this);
      if (task != null) {
        task.cancel();
        task = null;
      }

      pl.getTradeConfig().getCancelledMessage().send(player1, "%PLAYER%", player2.getName());
      pl.getTradeConfig().getCancelledMessage().send(player2, "%PLAYER%", player1.getName());
    } else if (closed.equals(spectatorInv)
            || spectatorInv.getViewers().contains(event.getPlayer())) {
      Bukkit.getScheduler()
              .runTaskLater(
                      pl,
                      () -> {
                        if (inv1.getViewers().isEmpty()
                                && inv2.getViewers().isEmpty()
                                && spectatorInv.getViewers().isEmpty()) {
                          HandlerList.unregisterAll(this);
                          if (entityPickupListener != null)
                            HandlerList.unregisterAll(entityPickupListener);
                        }
                      },
                      1L);
    }
  }

  @EventHandler
  public void onMove(PlayerMoveEvent event) {
    if (cancelled || event.getTo() == null) return;
    Player player = event.getPlayer();
    if (player.equals(player1) || player.equals(player2)) {
      if (event.getFrom().distanceSquared(event.getTo()) < 0.01) return;
      if (System.currentTimeMillis() < startTime + 1000) {
        return;
      }
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onInventoryPickupEvent(InventoryPickupItemEvent event) {
    if (cancelled) return;
    if (accept1 && accept2 && (event.getInventory() == inv1 || event.getInventory() == inv2)) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onInventoryInteract(InventoryInteractEvent event) {
    if ((event.getInventory() == inv1 || event.getInventory() == inv2)) {
      if (accept1 && accept2) {
        event.setCancelled(true);
        return;
      }
      if (event.getWhoClicked() == player1 || event.getWhoClicked() == player2) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onDropItem(PlayerDropItemEvent event) {
    if (cancelled) return;
    if (player1.equals(event.getPlayer()) || player2.equals(event.getPlayer())) {
      event.setCancelled(true);
      if (accept1 && accept2) {
        giveOnCursor(event.getPlayer());
      }
    }
  }

  private void giveOnCursor(Player player) {
    if (player.getItemOnCursor().getType() != Material.AIR) {
      player
              .getInventory()
              .addItem(player.getItemOnCursor())
              .forEach((i, j) -> player.getWorld().dropItemNaturally(player.getLocation(), j));
      player.setItemOnCursor(null);
    }
  }

  @EventHandler
  public void onDisable(PluginDisableEvent event) {
    if (event.getPlugin().getName().equalsIgnoreCase("TradePro")) {
      player1.closeInventory();
    }
  }

  @EventHandler
  public void onInteract(PlayerInteractAtEntityEvent event) {
    if (event.getPlayer() == player1 || event.getPlayer() == player2) {
      event.setCancelled(true);
    }
  }

  private void giveItemsOnLeft(Inventory inv, Player player) {
    List<ItemStack> dropoff = new ArrayList<>();
    for (int slot : mySlots) {
      if (slot == pl.getTradeConfig().getAcceptSlot() || getExtra(slot) != null) continue;
      ItemStack item = inv.getItem(slot);
      if (item == null || item.getType() == Material.AIR) continue;
      dropoff.addAll(player.getInventory().addItem(item).values());
      inv.setItem(slot, null);
    }
    if (!dropoff.isEmpty()) {
      int size = dropoff.size() / 9;
      if (dropoff.size() % 9 > 0) {
        size++;
      }
      size *= 9;
      Inventory excessChest =
              Bukkit.createInventory(null, size, pl.getTradeConfig().getExcessTitle());
      dropoff.forEach(excessChest::addItem);
      pl.getExcessChests().add(excessChest);
      Bukkit.getScheduler().runTaskLater(pl, () -> player.openInventory(excessChest), 1L);
    }
  }

  private List<ItemStack> getItemsOnLeft(Inventory inv) {
    List<ItemStack> items = new ArrayList<>();
    pl.getTradeConfig()
            .getMySlots()
            .forEach(
                    slot -> {
                      if (slot != pl.getTradeConfig().getAcceptSlot() && getExtra(slot) == null) {
                        ItemStack item = inv.getItem(slot);
                        if (item != null) {
                          items.add(item);
                        }
                      }
                    });
    return items;
  }

  private int getRight(int left) {
    return theirSlots.get(mySlots.indexOf(left));
  }

  private int getRightExtra(int left) {
    return theirExtraSlots.get(myExtraSlots.indexOf(left));
  }

  private void updateInventories() {
    Bukkit.getScheduler().runTask(pl, () -> {
      for (int slot : pl.getTradeConfig().getMySlots()) {
        if (getExtra(slot) == null && slot != pl.getTradeConfig().getAcceptSlot()) {
          ItemStack item1 = inv1.getItem(slot);
          ItemStack item2 = inv2.getItem(slot);

          // Process player1's items
          if (isBlocked(item1)) {
            Sounds.villagerHit(player1, 1);
            inv1.setItem(slot, null);
            player1.getInventory().addItem(item1).values().stream()
                    .findFirst()
                    .ifPresent(i -> player1.getWorld().dropItemNaturally(player1.getLocation(), i));
            pl.getTradeConfig().getErrorsBlacklistedItem().send(player1);
          } else {
            inv2.setItem(getRight(slot), item1);
            spectatorInv.setItem(slot, item1);
          }

          // Process player2's items
          if (isBlocked(item2)) {
            Sounds.villagerHit(player2, 1);
            inv2.setItem(slot, null);
            player2.getInventory().addItem(item2).values().stream()
                    .findFirst()
                    .ifPresent(i -> player2.getWorld().dropItemNaturally(player2.getLocation(), i));
            pl.getTradeConfig().getErrorsBlacklistedItem().send(player2);
          } else {
            inv1.setItem(getRight(slot), item2);
            spectatorInv.setItem(getRight(slot), item2);
          }
        }
      }

      // 🚀 Update both player inventories instantly
      player1.updateInventory();
      player2.updateInventory();
    });
  }



  public void updateExtras() {
    int slot1 = 0, slot2a = 0, slot2b = 0;
    ItemStack placeholder =
            pl.getTradeConfig().getPlaceholder().copy().replace("%PLAYER%", player2.getName()).build();
    for (int i = 0; i < myExtraSlots.size(); i++) {
      if (i >= extras.size()) {
        break;
      }
      int mySlot = myExtraSlots.get(i);
      int theirSlot = theirExtraSlots.get(i);
      inv1.setItem(mySlot, placeholder);
      inv1.setItem(theirSlot, placeholder);
      inv2.setItem(mySlot, placeholder);
      inv2.setItem(theirSlot, placeholder);
    }
    placedExtras.clear();
    for (Extra extra : extras) {
      inv1.setItem(myExtraSlots.get(slot1), extra.getIcon(player1));
      inv2.setItem(myExtraSlots.get(slot1), extra.getIcon(player2));
      placedExtras.put(myExtraSlots.get(slot1), extra);
      slot1++;
      if (extra.value1 > 0) {
        inv2.setItem(theirExtraSlots.get(slot2a), extra.getTheirIcon(player1));
        spectatorInv.setItem(myExtraSlots.get(slot2a), extra.getTheirIcon(player1));
        slot2a++;
      }
      if (extra.value2 > 0) {
        inv1.setItem(theirExtraSlots.get(slot2b), extra.getTheirIcon(player2));
        spectatorInv.setItem(theirExtraSlots.get(slot2b), extra.getTheirIcon(player2));
        slot2b++;
      }
    }
    player1.updateInventory();
    player2.updateInventory();
  }

  private void updateAcceptance() {
    if (pl.getTradeConfig().isAcceptEnabled()) {
      inv1.setItem(
              pl.getTradeConfig().getAcceptSlot(),
              accept1
                      ? pl.getTradeConfig().getCancel().build()
                      : pl.getTradeConfig().getAccept().build());
      inv1.setItem(
              pl.getTradeConfig().getTheirAcceptSlot(),
              accept2
                      ? pl.getTradeConfig().getTheirAccept().build()
                      : pl.getTradeConfig().getTheirCancel().build());
      inv2.setItem(
              pl.getTradeConfig().getAcceptSlot(),
              accept2
                      ? pl.getTradeConfig().getCancel().build()
                      : pl.getTradeConfig().getAccept().build());
      inv2.setItem(
              pl.getTradeConfig().getTheirAcceptSlot(),
              accept1
                      ? pl.getTradeConfig().getTheirAccept().build()
                      : pl.getTradeConfig().getTheirCancel().build());

      inv1.getItem(pl.getTradeConfig().getAcceptSlot())
              .setAmount(pl.getTradeConfig().getAntiscamCountdown());
      inv1.getItem(pl.getTradeConfig().getTheirAcceptSlot())
              .setAmount(pl.getTradeConfig().getAntiscamCountdown());
      inv2.getItem(pl.getTradeConfig().getAcceptSlot())
              .setAmount(pl.getTradeConfig().getAntiscamCountdown());
      inv2.getItem(pl.getTradeConfig().getTheirAcceptSlot())
              .setAmount(pl.getTradeConfig().getAntiscamCountdown());

      spectatorInv.setItem(
              4,
              accept1 && accept2
                      ? pl.getTradeConfig().getTheirAccept().build()
                      : pl.getTradeConfig().getTheirCancel().build());
    }
  }

  private void checkAcceptance() {
    if (pl.getTradeConfig().isAcceptEnabled()) {
      if (accept1 && accept2) {
        for (Extra extra : extras) {
          if (extra.updateMax(false)) {
            accept1 = false;
            accept2 = false;
            updateAcceptance();
            updateExtras();
            return;
          }
        }

        if (task != null) {
          return;
        }

        giveOnCursor(player1);
        giveOnCursor(player2);

        accepted1 = getItemsOnLeft(inv1).toArray(new ItemStack[0]);

        accepted2 = getItemsOnLeft(inv2).toArray(new ItemStack[0]);

        if (pl.getTradeConfig().isSoundEffectsEnabled() && pl.getTradeConfig().isSoundOnAccept()) {
          Sounds.pling(player1, 1);
          Sounds.pling(player2, 1);
          spectatorInv.getViewers().stream()
                  .filter(Player.class::isInstance)
                  .forEach(p -> Sounds.pling((Player) p, 1));
        }

        task =
                Bukkit.getScheduler()
                        .runTaskTimer(
                                pl,
                                () -> {
                                  int current = inv1.getItem(pl.getTradeConfig().getAcceptSlot()).getAmount();
                                  if (current > 1) {
                                    countAcceptSlots(current - 1);
                                  } else {
                                    if (task != null) {
                                      pl.ongoingTrades.remove(this);
                                      task.cancel();
                                      task = null;

                                      for (Extra extra : extras) {
                                        if (extra.updateMax(false)) {
                                          pl.getTradeConfig()
                                                  .getDiscrepancyDetected()
                                                  .send(player1, "%PLAYER%", player2.getName());
                                          pl.getTradeConfig()
                                                  .getDiscrepancyDetected()
                                                  .send(player2, "%PLAYER%", player1.getName());
                                          cancel(false);
                                          return;
                                        }
                                      }

                                      for (Extra extra : extras) {
                                        if (extra.value1 > extra.getMax(player1) || extra.value2 > extra.getMax(player2)) {
                                          pl.getTradeConfig()
                                                  .getDiscrepancyDetected()
                                                  .send(player1, "%PLAYER%", player2.getName());
                                          pl.getTradeConfig()
                                                  .getDiscrepancyDetected()
                                                  .send(player2, "%PLAYER%", player1.getName());
                                          cancel(false);
                                          return;
                                        }
                                      }

                                      if (pl.getTradeConfig().isDiscrepancyDetection()) {
                                        boolean discrepancy = false;
                                        int i = 0;
                                        for (ItemStack item : getItemsOnLeft(inv1)) {
                                          if (item == null) continue;
                                          if (accepted1.length <= i || !item.isSimilar(accepted1[i++])) {
                                            discrepancy = true;
                                            break;
                                          }
                                        }

                                        if (!discrepancy) {
                                          i = 0;
                                          for (ItemStack item : getItemsOnLeft(inv2)) {
                                            if (item == null) continue;
                                            if (accepted2.length <= i || !item.isSimilar(accepted2[i++])) {
                                              discrepancy = true;
                                            }
                                          }
                                        }

                                        if (discrepancy) {
                                          cancelled = true;
                                          pl.log(
                                                  "Found discrepancy in trade between "
                                                          + player1.getName()
                                                          + " and "
                                                          + player2.getName());
                                          pl.getTradeConfig()
                                                  .getDiscrepancyDetected()
                                                  .send(player1, "%PLAYER%", player2.getName());
                                          pl.getTradeConfig()
                                                  .getDiscrepancyDetected()
                                                  .send(player2, "%PLAYER%", player1.getName());
                                          cancel(false);
                                          return;
                                        }
                                      }

                                      for (int leftSlot : mySlots) {
                                        if (leftSlot == pl.getTradeConfig().getAcceptSlot()) continue;
                                        if (getExtra(leftSlot) != null) continue;
                                        int rightSlot = getRight(leftSlot);
                                        inv1.setItem(leftSlot, inv1.getItem(rightSlot));
                                        inv2.setItem(leftSlot, inv2.getItem(rightSlot));
                                      }

                                      for (int leftSlot : myExtraSlots) {
                                        if (leftSlot == pl.getTradeConfig().getAcceptSlot()) continue;
                                        if (getExtra(leftSlot) == null) continue;
                                        int rightSlot = getRightExtra(leftSlot);
                                        inv1.setItem(leftSlot, inv1.getItem(rightSlot));
                                        inv2.setItem(leftSlot, inv2.getItem(rightSlot));
                                      }

                                      cancel(true);

                                      for (Extra extra : extras) {
                                        extra.onTradeEnd();
                                      }

                                      if (pl.getTradeConfig().isSoundEffectsEnabled()
                                              && pl.getTradeConfig().isSoundOnComplete()) {
                                        Sounds.levelUp(player1, 1);
                                        Sounds.levelUp(player2, 1);
                                        spectatorInv.getViewers().stream()
                                                .filter(Player.class::isInstance)
                                                .map(Player.class::cast)
                                                .forEach(p -> Sounds.levelUp(p, 1));
                                      }

                                      pl.getTradeConfig()
                                              .getTradeComplete()
                                              .send(player1, "%PLAYER%", player2.getName());
                                      pl.getTradeConfig()
                                              .getTradeComplete()
                                              .send(player2, "%PLAYER%", player1.getName());

                                      if (pl.getLogs() != null) {
                                        try {
                                          TradeLog trade =
                                                  new TradeLog(
                                                          player1,
                                                          player2,
                                                          combine(accepted1).stream()
                                                                  .map(ItemFactory::new)
                                                                  .collect(Collectors.toList()),
                                                          combine(accepted2).stream()
                                                                  .map(ItemFactory::new)
                                                                  .collect(Collectors.toList()),
                                                          extras.stream()
                                                                  .filter(e -> e.value1 > 0)
                                                                  .map(e -> new TradeLog.ExtraOffer(e.name, e.value1))
                                                                  .collect(Collectors.toList()),
                                                          extras.stream()
                                                                  .filter(e -> e.value2 > 0)
                                                                  .map(e -> new TradeLog.ExtraOffer(e.name, e.value2))
                                                                  .collect(Collectors.toList()));

                                          TradeCompleteEvent completeEvent =
                                                  new TradeCompleteEvent(trade, player1, player2);
                                          Bukkit.getPluginManager().callEvent(completeEvent);

                                          pl.getLogs().log(trade);
                                        } catch (Exception ex) {
                                          pl.log("Failed to save trade log. " + ex.getMessage());
                                        }
                                      }
                                    } else {
                                      updateAcceptance();
                                    }
                                  }
                                },
                                20L,
                                20L);
      } else {
        if (task != null) {
          task.cancel();
          accept1 = false;
          accept2 = false;
          task = null;
          click();
        }
      }
    }
  }

  private Extra getExtra(int slot) {
    return placedExtras.get(slot);
  }

  private ItemStack putOnLeft(Inventory inventory, ItemStack toMove, int amountToMove) {
    int moved = 0;
    for (int slot : mySlots) {
      if (getExtra(slot) != null || slot == pl.getTradeConfig().getAcceptSlot()) continue;
      ItemStack inInventory = inventory.getItem(slot);
      if (inInventory != null
              && inInventory.isSimilar(toMove)
              && inInventory.getAmount() < inInventory.getType().getMaxStackSize()) {
        while (inInventory.getAmount() < inInventory.getType().getMaxStackSize()
                && toMove.getAmount() > 0
                && moved++ < amountToMove) {
          inInventory.setAmount(inInventory.getAmount() + 1);
          toMove.setAmount(toMove.getAmount() - 1);
        }
        if (toMove.getAmount() <= 0 || moved == amountToMove) {
          return null;
        }
      }
    }
    for (int slot : mySlots) {
      if (getExtra(slot) != null || slot == pl.getTradeConfig().getAcceptSlot()) continue;
      ItemStack i = inventory.getItem(slot);
      if (!(i == null || i.getType().equals(Material.AIR))) {
        continue;
      }
      inventory.setItem(slot, toMove);
      toMove = null;
    }
    return toMove;
  }

  boolean isBlocked(ItemStack item) {
    if (item == null || item.getType() == Material.AIR) return false;

    Material material = item.getType();
    List<String> blacklist = pl.getConfig().getStringList("blocked.blacklist");

    if (blacklist.contains(material.name().toUpperCase())) {
      return true; // No legacy material loading
    }

    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
      if (pl.getTradeConfig().isDenyNamedItems()) {
        return true;
      }


    return false;
  }


    // 🚀 Check if named items should be blocked
    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
      if (pl.getTradeConfig().isDenyNamedItems()) {
        return true;
      }
    }

    // 🚀 Regex Blocked Name/Lore Validation
    String regex = pl.getConfig().getString("blocked.regex", "");
    if (!regex.isEmpty()) {
      try {
        Pattern pattern = Pattern.compile(regex);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
          if (meta != null && meta.hasDisplayName() && pattern.matcher(meta.getDisplayName()).find()) {
          return true; // Name matches regex
          }
          if (meta.hasLore() && meta.getLore().stream().anyMatch(lore -> pattern.matcher(lore).find())) {

            return true; // Lore matches regex
          }
        }
      } catch (PatternSyntaxException ex) {
        Bukkit.getLogger().warning(ChatColor.RED + "Invalid blocked.regex in config: " + regex);
      }
    }

    // 🚀 Check Blacklisted Lore
    List<String> blockedLore = pl.getTradeConfig().getLoreBlacklist();
    if (!blockedLore.isEmpty() && item.hasItemMeta()) {
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
        List<String> lore = meta.getLore();
        if (lore != null) {
          for (String blocked : blockedLore) {
            for (String line : lore) {
              if (line.contains(blocked)) {
                return true;
              }
            }
          }
        }
      }
    }

    // 🚀 Check Blacklisted Items (Legacy & Modern)
    List<String> blocked = pl.getTradeConfig().getItemBlacklist();
    if (!blocked.isEmpty()) {
      String type = material.name();
      List<String> checks = new ArrayList<>();

      if (Sounds.version < 113) {
        byte data = item.getData().getData();
        checks.add(type + ":" + data);
        checks.add(type.replace("_", "") + ":" + data);
        checks.add(type.replace("_", " ") + ":" + data);
      }

      checks.add(type);
      checks.add(type.replace("_", ""));
      checks.add(type.replace("_", " "));

      for (String block : blocked) {
        for (String check : checks) {
          if (block.equalsIgnoreCase(check)) {
            return true;
          }
        }
      }
    }

    return false; // 🚀 This should be the last line of the method
  }


  public void open(Player player) {
    if (cancelled) {
      player.closeInventory();
      return;
    }

    if (player1.equals(player)) {
      player.openInventory(inv1);
    } else if (player2.equals(player)) {
      player.openInventory(inv2);
    }
  }

  public void setCancelOnClose(Player player, boolean cancelOnClose) {
    if (player1.equals(player)) {
      cancelOnClose1 = cancelOnClose;
    } else if (player2.equals(player)) {
      cancelOnClose2 = cancelOnClose;
    }
  }

  private void cancel(boolean success) {
    Bukkit.getScheduler().runTask(pl, () -> {
      if (inv1.getViewers().contains(player1)) player1.closeInventory();
      if (inv2.getViewers().contains(player2)) player2.closeInventory();
    });

    for (Extra extra : extras) {
      extra.onCancel();
    }
    cancelled = true;

    ItemStack acceptItem = success ? pl.getTradeConfig().getComplete().build() : pl.getTradeConfig().getCancelled().build();
    inv1.setItem(pl.getTradeConfig().getAcceptSlot(), acceptItem);
    inv2.setItem(pl.getTradeConfig().getAcceptSlot(), acceptItem);

    if (pl.getTradeConfig().isEndDisplayEnabled()) {
      new BukkitRunnable() {
        int count = pl.getTradeConfig().getEndDisplayTimer();

        @Override
        public void run() {
          if (--count <= 0) {
            player1.closeInventory();
            player2.closeInventory();
            cancel();
            cancel();
          }
        }
      }.runTaskTimer(pl, 0, 20);
    } else {
      player1.closeInventory();
      player2.closeInventory();
    }
  }



  private void countAcceptSlots(int count) {
    inv1.getItem(pl.getTradeConfig().getAcceptSlot()).setAmount(count);
    inv2.getItem(pl.getTradeConfig().getAcceptSlot()).setAmount(count);
    inv1.getItem(pl.getTradeConfig().getTheirAcceptSlot()).setAmount(count);
    inv2.getItem(pl.getTradeConfig().getTheirAcceptSlot()).setAmount(count);
    spectatorInv.getItem(4).setAmount(count);
    if (pl.getTradeConfig().isSoundEffectsEnabled() && pl.getTradeConfig().isSoundOnCountdown()) {
      Sounds.click(player1, 2);
      Sounds.click(player2, 2);
      spectatorInv.getViewers().stream()
          .filter(Player.class::isInstance)
          .forEach(p -> Sounds.click((Player) p, 2));
    }
  }
}
