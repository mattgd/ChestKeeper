package com.koletar.jj.chestkeeper;

import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * @author jjkoletar
 */
public class Phrases {
    private static Phrases instance;
    private ResourceBundle phrases;

    private Phrases() {}

    public static Phrases getInstance() {
        if (instance == null) {
            instance = new Phrases();
        }
        return instance;
    }

    public void initialize(Locale l) {
        phrases = ResourceBundle.getBundle("phrases", l);
    }

    public static String phrase(String key, Object... replacements) {
        if (getInstance() == null) {
            return "";
        }
        if (getInstance().phrases == null) {
            return "\u00A74Phrase Error! Did you /reload? Don't!";
        }
        if (!getInstance().phrases.containsKey(key)) {
            Logger.getLogger("Minecraft").warning("[ChestKeeper] Unknown phrase key! '" + key + "'");
            return "";
        }
        String format = getInstance().phrases.getString(key);
        for (int i = 0; i < replacements.length; i++) {
            format = format.replace("%" + i + "%", findName(replacements[i]));
        }
        format = format.replace("&", "\u00A7").replace("\u00A7\u00A7", "&");
        return format;
    }

    public static String findName(Object o) {
        if (o instanceof Player) {
            return ((Player) o).getName();
        } else if (o instanceof World) {
            return ((World) o).getName();
        } else if (o instanceof ConsoleCommandSender) {
            return phrase("console");
        } else if (o instanceof CKUser) {
            return ((CKUser) o).getUsername();
        } else if (o instanceof CKChest) {
            return ((CKChest) o).getTitle();
        }
        return o.toString();
    }
}