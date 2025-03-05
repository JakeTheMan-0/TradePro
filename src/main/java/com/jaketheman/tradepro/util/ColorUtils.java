package com.jaketheman.tradepro.util;

public class ColorUtils {

    public static String convertMinecraftToHTMLColor(String text) {
        if (text == null) return "";
        StringBuilder html = new StringBuilder();
        boolean bold = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '§' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                i++;

                switch (code) {
                    case '0': html.append("</span><span style='color:#000000;'>"); break; // Black
                    case '1': html.append("</span><span style='color:#0000AA;'>"); break; // Dark Blue
                    case '2': html.append("</span><span style='color:#00AA00;'>"); break; // Dark Green
                    case '3': html.append("</span><span style='color:#00AAAA;'>"); break; // Dark Aqua
                    case '4': html.append("</span><span style='color:#AA0000;'>"); break; // Dark Red
                    case '5': html.append("</span><span style='color:#AA00AA;'>"); break; // Dark Purple
                    case '6': html.append("</span><span style='color:#FFAA00;'>"); break; // Gold
                    case '7': html.append("</span><span style='color:#AAAAAA;'>"); break; // Gray
                    case '8': html.append("</span><span style='color:#555555;'>"); break; // Dark Gray
                    case '9': html.append("</span><span style='color:#5555FF;'>"); break; // Blue
                    case 'a': html.append("</span><span style='color:#55FF55;'>"); break; // Green
                    case 'b': html.append("</span><span style='color:#55FFFF;'>"); break; // Aqua
                    case 'c': html.append("</span><span style='color:#FF5555;'>"); break; // Red
                    case 'd': html.append("</span><span style='color:#FF55FF;'>"); break; // Light Purple
                    case 'e': html.append("</span><span style='color:#FFFF55;'>"); break; // Yellow
                    case 'f': html.append("</span><span style='color:#FFFFFF;'>"); break; // White
                    case 'k': html.append("</span>"); break; // Obfuscated (skip)
                    case 'l': bold = true; html.append("</span><b>"); break; // Bold
                    case 'm': html.append("</span><strike>"); break; // Strikethrough
                    case 'n': html.append("</span><u>"); break; // Underline
                    case 'o': html.append("</span><i>"); break; // Italic
                    case 'r': bold = false; html.append("</span>"); break; // Reset
                    case 'x': // Support for 1.16+ hex colors (§x§R§R§G§G§B§B)
                        if (i + 6 < text.length()) {
                            String hexColor = "#" + text.substring(i + 1, i + 7);
                            try {

                                Integer.parseInt(text.substring(i + 1, i + 7), 16);
                                html.append("</span><span style='color:").append(hexColor).append(";'>");
                                i += 6;
                            } catch (NumberFormatException e) {
                                html.append("</span><span>");
                            }

                        } else {
                            html.append("</span><span>");
                        }
                        break;

                    default:
                        html.append("</span><span>");
                        break;
                }
            } else {
                html.append(c);
            }
        }
        if (bold) {
            html.append("</b>");
        }
        html.append("</span>");
        return html.toString();
    }
}