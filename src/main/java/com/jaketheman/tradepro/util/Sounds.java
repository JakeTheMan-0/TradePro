package com.jaketheman.tradepro.util;

import org.bukkit.entity.Player;
import com.cryptomorin.xseries.XSound;

public class Sounds {

  public static void loadSounds() {
    // Sounds loaded on use
  }

  public static void pling(Player player, float v1) {
    play(player, XSound.BLOCK_NOTE_BLOCK_PLING, v1);
  }

  public static void click(Player player, float v1) {
    play(player, XSound.UI_BUTTON_CLICK, v1);
  }

  public static void levelUp(Player player, float v1) {
    play(player, XSound.ENTITY_PLAYER_LEVELUP, v1);
  }

  public static void villagerHit(Player player, float v1) {
    play(player, XSound.ENTITY_VILLAGER_HURT, v1);
  }

  public static void villagerHmm(Player player, float v1) {
    play(player, XSound.ENTITY_VILLAGER_AMBIENT, v1);
  }

  private static void play(Player player, XSound xsound, float pitch) {
    try {
      // XSeries resolves the right Sound for the running server. Sound.valueOf is deliberately not used:
      // Sound is an enum on older servers and an interface on newer ones, so one compiled call can't link on both.
      org.bukkit.Sound sound = xsound.parseSound();
      if (sound != null) {
        player.playSound(player.getLocation(), sound, 1.0f, pitch);
      }
    } catch (Throwable ignored) {
      // cosmetic only; never let a missing sound break a trade
    }
  }
}
