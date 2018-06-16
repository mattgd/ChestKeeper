package com.koletar.jj.chestkeeper;

import java.util.Collection;

/**
 * @author jjkoletar
 */
public class StringTools {
    public static String buildList(Object[] items, String prefix, String suffix) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            sb.append(prefix);
            sb.append(Phrases.findName(items[i]));
            if (i < items.length - 1) {
                sb.append(suffix);
            }
        }
        return sb.toString();
    }

    public static String buildList(Collection<?> items, String prefix, String suffix) {
        return buildList(items.toArray(), prefix, suffix);
    }
}
