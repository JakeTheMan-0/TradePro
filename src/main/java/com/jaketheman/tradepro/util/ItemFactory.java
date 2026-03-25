package com.jaketheman.tradepro.util;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.jaketheman.tradepro.TradePro;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.meta.Damageable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor // Add no-argument constructor
public class ItemFactory {

    // *********************************************************************
    // ADD @Expose and @SerializedName to these fields, based on what you need
    // *********************************************************************

    @Expose
    @SerializedName("material")
    private Material material;

    @Expose
    @SerializedName("amount")
    @Getter
    private int amount;

    @Expose
    @SerializedName("displayName")
    private String displayName;

    @Expose
    @SerializedName("lore")
    private List<String> lore;

    // Add more fields for other properties you want to serialize:
    // - Enchantments
    // - Item flags
    // - Custom model data
    // - Etc.

    @Getter
    private ItemStack stack;

    // *********************************************************************
    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public ItemFactory(Material material) {
        this.material = material;
        this.stack = new ItemStack(material);
        this.amount = 1; // Default amount
        updatePropertiesFromStack();

    }

    public ItemFactory(String parsable, Material fallback) {
        if (parsable == null) {
            this.material = fallback;
            this.stack = new ItemStack(fallback);
        } else {
            parsable = parsable.toUpperCase().replace(" ", "_");
            java.util.Optional<com.cryptomorin.xseries.XMaterial> xmatOpt;
            try {
                xmatOpt = com.cryptomorin.xseries.XMaterial.matchXMaterial(parsable);
            } catch (IllegalArgumentException ignored) {
                xmatOpt = java.util.Optional.empty();
            }

            if (!xmatOpt.isPresent()) {
                this.material = fallback;
                this.stack = new ItemStack(fallback);
                TradePro.getPlugin(TradePro.class).getLogger()
                        .warning("Unknown material [" + parsable + "]. Using fallback: " + fallback.name());
            } else {
                ItemStack parsedStack = xmatOpt.get().parseItem();
                if (parsedStack == null)
                    parsedStack = new ItemStack(fallback); // Failsafe
                this.stack = parsedStack;
                this.material = parsedStack.getType();
            }
        }
        this.amount = 1;
        updatePropertiesFromStack();
    }

    private void updatePropertiesFromStack() {
        this.amount = stack.getAmount();
        if (stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta.hasDisplayName()) {
                this.displayName = meta.getDisplayName();
            } else {
                this.displayName = null;
            }
            if (meta.hasLore()) {
                this.lore = meta.getLore();
            } else {
                this.lore = null;
            }

            // Extract other properties as needed
        } else {
            this.displayName = null;
            this.lore = null;
        }
    }

    public ItemFactory damage(int damage) {
        if (stack.getItemMeta() instanceof Damageable) {
            Damageable damageable = (Damageable) stack.getItemMeta();
            damageable.setDamage(damage);
            stack.setItemMeta((ItemMeta) damageable);
        }
        updatePropertiesFromStack();
        return this;
    }

    public ItemFactory(String parsable) {
        this(parsable, Material.PAPER);
        // Preconditions.checkNotNull(parsable, "Material cannot be null.");
        // byte data = -1;
        // if (parsable.contains(":")) {
        // String[] split = parsable.split(":");
        // data = Byte.parseByte(split[1]);
        // parsable = split[0];
        // }
        // parsable = parsable.toUpperCase().replace(" ", "_");
        // Material mat = Material.getMaterial(parsable);
        // this.material = Preconditions.checkNotNull(mat, "Unknown material [%s]",
        // parsable);
        // ;
        // this.data = data;
    }

    public ItemFactory(ItemStack stack) {
        this.stack = stack.clone();
        this.material = stack.getType();
        updatePropertiesFromStack();
    }

    public ItemFactory(ConfigurationSection yml, String key) {
        if (!yml.contains(key)) {
            this.stack = null;
            return;
        }

        if (yml.isItemStack(key)) {
            this.stack = yml.getItemStack(key);
            if (stack == null || stack.getType() == Material.AIR) {
                throw new IllegalArgumentException("Invalid item stack or material in config for key: " + key);
            }
            this.material = stack.getType();
            updatePropertiesFromStack();

            if (stack.hasItemMeta()) {
                ItemMeta meta = stack.getItemMeta();
                String displayName = null;
                List<String> lore = null;

                if (meta.hasDisplayName()) {
                    displayName = MsgUtils.cleanLegacyJson(meta.getDisplayName());
                    displayName = MsgUtils.color(displayName);
                }

                if (meta.hasLore()) {
                    lore = meta.getLore().stream().map(MsgUtils::cleanLegacyJson).map(MsgUtils::color)
                            .collect(Collectors.toList());
                }

                meta.setDisplayName(displayName);
                meta.setLore(lore);

                stack.setItemMeta(meta);
            }
        } else {
            // Handle new format or legacy map format (failed deserialization)
            String matName = yml.getString(key + ".material");
            if (matName == null) {
                // Try legacy "type" field if it failed deserialization
                matName = yml.getString(key + ".type", "PAPER");
            }
            java.util.Optional<com.cryptomorin.xseries.XMaterial> xmatOpt;
            try {
                xmatOpt = com.cryptomorin.xseries.XMaterial.matchXMaterial(matName);
            } catch (IllegalArgumentException ignored) {
                xmatOpt = java.util.Optional.empty();
            }
            if (xmatOpt.isPresent()) {
                this.stack = xmatOpt.get().parseItem();
                if (this.stack == null)
                    this.stack = new ItemStack(Material.PAPER);
            } else {
                Material m = Material.matchMaterial(matName);
                if (m == null)
                    m = Material.PAPER;
                this.stack = new ItemStack(m);
            }
            this.material = this.stack.getType();

            this.amount = yml.getInt(key + ".amount", 1);
            this.stack.setAmount(this.amount);

            ItemMeta meta = this.stack.getItemMeta();
            if (meta != null) {
                if (yml.contains(key + ".name")) {
                    meta.setDisplayName(MsgUtils.color(MsgUtils.cleanLegacyJson(yml.getString(key + ".name"))));
                } else if (yml.contains(key + ".meta.display-name")) { // Legacy meta
                    meta.setDisplayName(
                            MsgUtils.color(MsgUtils.cleanLegacyJson(yml.getString(key + ".meta.display-name"))));
                }

                if (yml.contains(key + ".lore")) {
                    meta.setLore(yml.getStringList(key + ".lore").stream().map(MsgUtils::cleanLegacyJson)
                            .map(MsgUtils::color).collect(Collectors.toList()));
                } else if (yml.contains(key + ".meta.lore")) {
                    meta.setLore(yml.getStringList(key + ".meta.lore").stream().map(MsgUtils::cleanLegacyJson)
                            .map(MsgUtils::color).collect(Collectors.toList()));
                }

                if (yml.contains(key + ".custom-model-data")) {
                    try {
                        meta.setCustomModelData(yml.getInt(key + ".custom-model-data"));
                    } catch (NoSuchMethodError ignored) {
                    }
                } else if (yml.contains(key + ".meta.custom-model-data")) {
                    try {
                        meta.setCustomModelData(yml.getInt(key + ".meta.custom-model-data"));
                    } catch (NoSuchMethodError ignored) {
                    }
                }

                if (yml.contains(key + ".flags")) {
                    for (String flag : yml.getStringList(key + ".flags")) {
                        try {
                            meta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf(flag));
                        } catch (Exception ignored) {
                        }
                    }
                } else if (yml.contains(key + ".meta.ItemFlags")) {
                    for (String flag : yml.getStringList(key + ".meta.ItemFlags")) {
                        try {
                            meta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf(flag));
                        } catch (Exception ignored) {
                        }
                    }
                }
                this.stack.setItemMeta(meta);
            }
            updatePropertiesFromStack();
        }
    }

    public ItemFactory save(ConfigurationSection yml, String key) {
        yml.set(key, null); // Clear old format
        yml.set(key + ".material", this.material.name());
        yml.set(key + ".amount", this.amount);

        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                yml.set(key + ".name", meta.getDisplayName().replace(ChatColor.COLOR_CHAR, '&'));
            }
            if (meta.hasLore()) {
                yml.set(key + ".lore", meta.getLore().stream().map(s -> s.replace(ChatColor.COLOR_CHAR, '&'))
                        .collect(Collectors.toList()));
            }
            try {
                if (meta.hasCustomModelData()) {
                    yml.set(key + ".custom-model-data", meta.getCustomModelData());
                }
            } catch (NoSuchMethodError ignored) {
            }

            if (!meta.getItemFlags().isEmpty()) {
                yml.set(key + ".flags", meta.getItemFlags().stream().map(Enum::name).collect(Collectors.toList()));
            }
        }
        return this;
    }

    static ItemStack getPlayerSkull(Player player, String displayName) {
        ItemStack skull = com.cryptomorin.xseries.XMaterial.PLAYER_HEAD.parseItem();
        if (skull == null)
            skull = new ItemStack(Material.PAPER); // Failsafe
        if (skull.getItemMeta() instanceof SkullMeta) {
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setDisplayName(MsgUtils.color(displayName));
            try {
                meta.setOwningPlayer(player);
            } catch (NoSuchMethodError e) {
                meta.setOwner(player.getName());
            }
            skull.setItemMeta(meta);
        }
        return skull;
    }

    public static ItemStack replaceInMeta(ItemStack item, String... replace) {
        item = item.clone();
        if (!item.hasItemMeta()) {
            return item;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            try {
                for (int i = 0; i < replace.length - 1; i += 2) {
                    String toReplace = replace[i];
                    String replaceWith = replace[i + 1];
                    if (meta.hasDisplayName()) {
                        meta.setDisplayName(meta.getDisplayName().replace(toReplace, replaceWith));
                    }
                    if (meta.hasLore()) {
                        List<String> lore = meta.getLore();
                        assert lore != null;
                        for (int j = 0; j < lore.size(); j++) {
                            lore.set(j, lore.get(j).replace(toReplace, replaceWith));
                        }
                        meta.setLore(lore);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    public ItemFactory replace(String... replace) {
        if (stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            String display = meta.getDisplayName();
            List<String> lore = meta.getLore();
            for (int i = 0; i < replace.length - 1; i += 2) {
                if (display != null)
                    display = display.replace(replace[i], replace[i + 1]);
                if (lore != null) {
                    int n = i;
                    lore = lore.stream()
                            .map(str -> str.replace(replace[n], replace[n + 1]))
                            .collect(Collectors.toList());
                }
            }
            meta.setDisplayName(display);
            meta.setLore(lore);
            stack.setItemMeta(meta);
        }
        updatePropertiesFromStack();
        return this;
    }

    public ItemStack build() {
        return stack.clone();
    }

    public ItemFactory copy() {
        return new ItemFactory(stack);
    }

    public ItemFactory amount(int amount) {
        this.stack = new ItemStack(this.material, amount); // Set the stack on amount, not just field
        this.amount = amount;
        updatePropertiesFromStack();
        return this;
    }

    public ItemFactory display(String display) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (display.contains("%NEWLINE%")) {
                String[] split = display.split("%NEWLINE%");
                display = split[0];
                List<String> lore = new ArrayList<>();
                for (int i = 1; i < split.length; i++) {
                    lore.add(split[i]);
                }
                this.lore(lore);
            }
            meta.setDisplayName(MsgUtils.color(display));
            stack.setItemMeta(meta);
        }
        updatePropertiesFromStack();
        return this;
    }

    public ItemFactory lore(List<String> lore) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            for (int i = 0; i < lore.size(); i++) {
                String line = lore.get(i);
                if (line != null) {
                    line = MsgUtils.color(line);
                    lore.set(i, line);
                }
            }
            List<String> current = meta.getLore();
            if (current == null)
                current = new ArrayList<>();
            current.addAll(lore);
            meta.setLore(current);
            stack.setItemMeta(meta);
        }
        updatePropertiesFromStack();
        return this;
    }

    public ItemFactory flag(String flag) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            try {
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf(flag));
                stack.setItemMeta(meta);
            } catch (Throwable ignored) {
            } // Gracefully ignore on older versions
        }
        updatePropertiesFromStack();
        return this;
    }

    public ItemFactory customModelData(int customModelData) {
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            try {
                meta.setCustomModelData(customModelData);
                stack.setItemMeta(meta);
            } catch (NoSuchMethodError ignored) {
            } // Graceful fail < 1.14
        }
        updatePropertiesFromStack();
        return this;
    }

    public int getAmount() {
        return amount; // Return amount field, not stack.
    }

    @Override
    public String toString() {
        return "ItemFactory{" +
                "material=" + material +
                ", amount=" + amount +
                ", displayName='" + displayName + '\'' +
                ", lore=" + lore +
                ", stack=" + stack +
                '}';
    }
}