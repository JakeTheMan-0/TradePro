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
    //  ADD @Expose and @SerializedName to these fields, based on what you need
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
        this.amount = 1;  // Default amount
        updatePropertiesFromStack();

    }

    public ItemFactory(String parsable, Material fallback) {
        if (parsable == null) {
            this.material = fallback;
            this.stack = new ItemStack(fallback);
        } else {
            byte data = -1;
            if (parsable.contains(":")) {
                String[] split = parsable.split(":");
                data = Byte.parseByte(split[1]);
                parsable = split[0];
            }
            parsable = parsable.toUpperCase().replace(" ", "_");

            Material mat = Material.getMaterial(parsable);
            if (mat == null) {
                mat = fallback; // Use fallback if material is null
                if (mat == null) {
                    throw new IllegalArgumentException("Material cannot be null! Check your config or inputs.");
                }
                TradePro.getPlugin(TradePro.class)
                        .getLogger()
                        .warning(
                                "Unknown material ["
                                        + parsable
                                        + "]."
                                        + (Sounds.version >= 113
                                        ? " Make sure you've updated to the new 1.13 standard. Numerical item IDs are no longer supported. Using fallback: "
                                        + fallback.name()
                                        : ""));
            }

            this.material = mat;
            this.stack = new ItemStack(mat);

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
        //    Preconditions.checkNotNull(parsable, "Material cannot be null.");
        //    byte data = -1;
        //    if (parsable.contains(":")) {
        //      String[] split = parsable.split(":");
        //      data = Byte.parseByte(split[1]);
        //      parsable = split[0];
        //    }
        //    parsable = parsable.toUpperCase().replace(" ", "_");
        //    Material mat = Material.getMaterial(parsable);
        //    this.material = Preconditions.checkNotNull(mat, "Unknown material [%s]", parsable);
        //    ;
        //    this.data = data;
    }

    public ItemFactory(ItemStack stack) {
        this.stack = stack.clone();
        this.material = stack.getType();
        updatePropertiesFromStack();
    }

    public ItemFactory(ConfigurationSection yml, String key) {
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
                displayName = MsgUtils.color(meta.getDisplayName());
            }

            if (meta.hasLore()) {
                lore = meta.getLore().stream().map(MsgUtils::color).collect(Collectors.toList());
            }

            meta.setDisplayName(displayName);
            meta.setLore(lore);

            stack.setItemMeta(meta);
        }
    }

    public ItemFactory save(ConfigurationSection yml, String key) {
        ItemStack stack = this.stack.clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) meta.setDisplayName(meta.getDisplayName().replace(ChatColor.COLOR_CHAR, '&'));
            if (meta.hasLore()) meta.setLore(meta.getLore().stream().map(s -> s.replace(ChatColor.COLOR_CHAR, '&')).collect(Collectors.toList()));
        }
        yml.set(key, stack);
        return this;
    }

    static ItemStack getPlayerSkull(Player player, String displayName) {
        Material skullMaterial = Material.getMaterial(Sounds.version > 112 ? "PLAYER_HEAD" : "SKULL_ITEM");
        if (skullMaterial == null) {
            throw new IllegalArgumentException("Skull material cannot be null. Check server version and compatibility.");
        }

        ItemStack skull = new ItemStack(skullMaterial);
        if (Sounds.version < 113) skull.getData().setData((byte) 3);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        meta.setDisplayName(MsgUtils.color(displayName));
        if (Sounds.version >= 112) meta.setOwningPlayer(player);
        else meta.setOwner(player.getName());
        skull.setItemMeta(meta);
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
                if (display != null) display = display.replace(replace[i], replace[i + 1]);
                if (lore != null) {
                    int n = i;
                    lore =
                            lore.stream()
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
        this.stack = new ItemStack(this.material, amount); //Set the stack on amount, not just field
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
            if (current == null) current = new ArrayList<>();
            current.addAll(lore);
            meta.setLore(current);
            stack.setItemMeta(meta);
        }
        updatePropertiesFromStack();
        return this;
    }

    public ItemFactory flag(String flag) {
        if (Sounds.version == 17) return this;
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.valueOf(flag));
            stack.setItemMeta(meta);
        }
        updatePropertiesFromStack();
        return this;
    }

    public ItemFactory customModelData(int customModelData) {
        if (Sounds.version < 114) return this;
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            stack.setItemMeta(meta);
        }
        updatePropertiesFromStack();
        return this;
    }


    public int getAmount() {
        return amount; //Return amount field, not stack.
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