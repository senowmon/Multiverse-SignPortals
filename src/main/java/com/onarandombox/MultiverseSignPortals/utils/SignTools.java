/*
 * Multiverse 2 Copyright (c) the Multiverse Team 2011.
 * Multiverse 2 is licensed under the BSD License.
 * For more information please check the README.md file included
 * with this project.
 */

package com.onarandombox.MultiverseSignPortals.utils;

import org.bukkit.ChatColor;

public class SignTools {
    public static boolean isMVSign(String test, ChatColor color) {
        if (color == null) {
            return test.equalsIgnoreCase("[multiverse]") || test.equalsIgnoreCase("[mv]");
        }
        return test.equalsIgnoreCase(color + "[multiverse]") || test.equalsIgnoreCase(color + "[mv]");
    }

    public static String setColor(String line, ChatColor color) {
        return color + line.substring(line.indexOf("["), line.length());
    }
}
