package com.jaketheman.tradepro.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.bukkit.ChatColor.COLOR_CHAR;

public class MsgUtils {

  private static boolean clickableMessages;

  static {
    try {
      clickableMessages = Class.forName("net.md_5.bungee.api.chat.TextComponent") != null;
    } catch (ClassNotFoundException ignored) {
      clickableMessages = false;
    }
  }

  public static void send(Player player, String onHover, String onClick, String[] messages) {
    if (clickableMessages) {
      MsgUtils1_8.send(player, onHover, onClick, messages);
    } else {
      send(player, messages);
    }
  }

  public static void send(Player player, String onHover, String onClick, String message) {
    if (message.contains("%NEWLINE%")) {
      send(player, onHover, onClick, message.split("%NEWLINE%"));
    } else {
      send(player, onHover, onClick, new String[] { message });
    }
  }

  public static void send(CommandSender sender, String[] messages) {
    for (String message : messages) {
      send(sender, message);
    }
  }

  public static void send(CommandSender sender, String message) {
    sender.sendMessage(color(message));
  }

  public static final String startTag = "&\\{";
  public static final String endTag = "}";

  public static String color(String string) {
    final Pattern hexPattern = Pattern.compile(startTag + "([A-Fa-f0-9]{6})" + endTag);
    Matcher matcher = hexPattern.matcher(string);
    StringBuffer buffer = new StringBuffer(string.length() + 4 * 8);
    while (matcher.find()) {
      String group = matcher.group(1);
      matcher.appendReplacement(buffer, COLOR_CHAR + "x"
          + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
          + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
          + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5));
    }
    return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
  }

  public static String cleanLegacyJson(String string) {
    if (string == null || string.isEmpty() || (!string.startsWith("{\"") && !string.startsWith("[{\""))) {
      return string;
    }
    try {
      JsonParser parser = new JsonParser();
      JsonElement element = parser.parse(string);
      StringBuilder builder = new StringBuilder();
      parseJsonElement(element, builder);
      return builder.length() > 0 ? color(builder.toString()) : string;
    } catch (Exception e) {
      return string;
    }
  }

  private static void parseJsonElement(JsonElement element, StringBuilder builder) {
    if (element.isJsonObject()) {
      JsonObject obj = element.getAsJsonObject();
      if (obj.has("color")) {
        try {
          String colorName = obj.get("color").getAsString().toUpperCase();
          if (colorName.equals("OBFUSCATED") || colorName.equals("MAGIC")) {
            builder.append(ChatColor.MAGIC.toString());
          } else {
            builder.append(ChatColor.valueOf(colorName).toString());
          }
        } catch (Exception ignored) {
        }
      }
      if (obj.has("bold") && obj.get("bold").getAsBoolean())
        builder.append(ChatColor.BOLD.toString());
      if (obj.has("italic") && obj.get("italic").getAsBoolean())
        builder.append(ChatColor.ITALIC.toString());
      if (obj.has("underlined") && obj.get("underlined").getAsBoolean())
        builder.append(ChatColor.UNDERLINE.toString());
      if (obj.has("strikethrough") && obj.get("strikethrough").getAsBoolean())
        builder.append(ChatColor.STRIKETHROUGH.toString());
      if (obj.has("obfuscated") && obj.get("obfuscated").getAsBoolean())
        builder.append(ChatColor.MAGIC.toString());

      if (obj.has("text")) {
        builder.append(obj.get("text").getAsString());
      }
      if (obj.has("extra")) {
        parseJsonElement(obj.get("extra"), builder);
      }
    } else if (element.isJsonArray()) {
      for (JsonElement arrElem : element.getAsJsonArray()) {
        parseJsonElement(arrElem, builder);
      }
    }
  }

}
