package com.koletar.jj.chestkeeper;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jjkoletar
 */
public class CKChest implements ConfigurationSerializable {
    private static final int SMALL_CHEST_SIZE = 27;
    private static final int LARGE_CHEST_SIZE = 54;
    private ItemStack[] contents;
    private Inventory inventory;
    private boolean modified;
    private String title;

    public CKChest(String title, boolean isLargeChest) {
        contents = new ItemStack[isLargeChest ? LARGE_CHEST_SIZE : SMALL_CHEST_SIZE];
        this.title = title;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> me = new HashMap<>();
        for (int i = 0; i < contents.length; i++) {
            me.put(String.valueOf(i), contents[i]);
        }
        me.put("_title", title);
        return me;
    }

    public Inventory getInventory(int magic) {
        if (inventory == null) {
            String invTitle = title + makeMagic(magic);
            if (invTitle.length() > 32) {
                invTitle = invTitle.substring(0, 32);
                if (invTitle.endsWith("\u00A7")) {
                    invTitle = invTitle.substring(0, invTitle.length() - 1);
                }
            }
            inventory = Bukkit.createInventory(null, contents.length, invTitle);
            ChestKeeper.trace("Title is: " + title + makeMagic(magic));
        }
        if (modified) {
            return inventory;
        }
        inventory.setContents(contents);
        modified = true;
        return inventory;
    }

    public boolean save() {
        if (inventory == null || inventory.getViewers().size() > 1) {
            return false;
        }
        if (!modified) {
            return true;
        }
        contents = inventory.getContents();
        modified = false;
        return true;
    }

    public boolean isModified() {
        return modified;
    }

    public void kick() {
        if (inventory != null) {
            for (HumanEntity he : inventory.getViewers()) {
                he.closeInventory();
            }
        }
    }

    public void empty() {
        if (inventory != null) {
            if (modified) {
                inventory.clear();
            } else {
                contents = new ItemStack[contents.length];
                modified = true;
            }
        }
    }

    private static String makeMagic(int magic) {
        StringBuilder sb = new StringBuilder();
        char[] digits = String.valueOf(magic).toCharArray();
        for (char digit : digits) {
            sb.append("\u00A7");
            if (digit == '-') {
                sb.append('f');
            } else {
                sb.append(digit);
            }
        }
        return sb.toString();
    }

    public void setName(String name) {
        kick();
        save();
        this.title = name;
        inventory = null;
    }

    public boolean isLargeChest() {
        return contents.length == LARGE_CHEST_SIZE;
    }

    public boolean upgrade() {
        if (contents.length == LARGE_CHEST_SIZE) {
            return false;
        }
        kick();
        save();
        ItemStack[] newContents = new ItemStack[LARGE_CHEST_SIZE];
        for (int i = 0; i < contents.length; i++) {
            newContents[i] = contents[i] == null ? null : contents[i].clone();
        }
        contents = newContents;
        inventory = null;
        return true;
    }

    public String getTitle() {
        return title;
    }

    protected void setItems(ItemStack[] in) {
        contents = new ItemStack[contents.length];
        for (int i = 0; i < in.length && i < contents.length; i++) {
            contents[i] = in[i];
        }
    }
}
