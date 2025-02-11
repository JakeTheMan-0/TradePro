package com.jaketheman.tradepro.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.logging.Level;

public class Sounds {

  public static final int version;
  private static Sound pling;
  private static Sound click;
  private static Sound levelUp;
  private static Sound villagerHit;
  private static Sound villagerHmm;

  // ✅ Safe version detection (Works on Bukkit, Spigot, Paper)
  static {
    version = detectVersion();
  }

  private static int detectVersion() {
    try {
      String versionString = Bukkit.getBukkitVersion().split("-")[0]; // Example: "1.21-R0.1-SNAPSHOT"
      String[] parts = versionString.split("\\.");
      int major = Integer.parseInt(parts[0]);
      int minor = Integer.parseInt(parts[1]);
      int detectedVersion = major * 100 + minor; // Converts 1.21 → 121
      Bukkit.getLogger().info(ChatColor.GREEN + "[TradePro] Detected Minecraft version: " + detectedVersion);
      return detectedVersion;
    } catch (Exception e) {
      Bukkit.getLogger().log(Level.SEVERE, "[TradePro] Failed to detect server version!", e);
      return -1; // Fallback
    }
  }

  // Load sounds based on detected version
  public static void loadSounds() {
    if (version == -1) {
      Bukkit.getLogger().warning("[TradePro] Version detection failed. Sounds will not be loaded.");
      return;
    }

    try {
      if (version < 109) { // Pre-1.9
        pling = safeValueOf("NOTE_PLING");
        click = safeValueOf("CLICK");
        levelUp = safeValueOf("LEVEL_UP");
        villagerHit = safeValueOf("VILLAGER_HIT");
        villagerHmm = safeValueOf("VILLAGER_IDLE");
      } else if (version < 113) { // 1.9 - 1.12
        pling = safeValueOf("BLOCK_NOTE_PLING");
        click = safeValueOf("UI_BUTTON_CLICK");
        levelUp = safeValueOf("ENTITY_PLAYER_LEVELUP");
        villagerHit = safeValueOf("ENTITY_VILLAGER_HURT");
        villagerHmm = safeValueOf("ENTITY_VILLAGER_AMBIENT");
      } else { // 1.13+
        pling = safeValueOf("BLOCK_NOTE_BLOCK_PLING");
        click = safeValueOf("UI_BUTTON_CLICK");
        levelUp = safeValueOf("ENTITY_PLAYER_LEVELUP");
        villagerHit = safeValueOf("ENTITY_VILLAGER_HURT");
        villagerHmm = safeValueOf("ENTITY_VILLAGER_AMBIENT");
      }
      Bukkit.getLogger().info(ChatColor.GREEN + "[TradePro] Sounds loaded successfully!");
    } catch (Exception ex) {
      Bukkit.getLogger().log(Level.SEVERE, "[TradePro] Unable to load sounds! Sound effects disabled.", ex);
    }
  }

  // ✅ Helper method to check if sound exists
  private static Sound safeValueOf(String soundName) {
    try {
      return Sound.valueOf(soundName);
    } catch (IllegalArgumentException ex) {
      Bukkit.getLogger().warning("[TradePro] Sound not found: " + soundName);
      return null;
    }
  }

  // ✅ Play sounds safely
  public static void pling(Player player, float v1) {
    if (pling != null) player.playSound(player.getEyeLocation(), pling, 1, v1);
  }

  public static void click(Player player, float v1) {
    if (click != null) player.playSound(player.getEyeLocation(), click, 1, v1);
  }

  public static void levelUp(Player player, float v1) {
    if (levelUp != null) player.playSound(player.getEyeLocation(), levelUp, 1, v1);
  }

  public static void villagerHit(Player player, float v1) {
    if (villagerHit != null) player.playSound(player.getEyeLocation(), villagerHit, 1, v1);
  }

  public static void villagerHmm(Player player, float v1) {
    if (villagerHmm != null) player.playSound(player.getEyeLocation(), villagerHmm, 1, v1);
  }
}
