/*
 * Multiverse 2 Copyright (c) the Multiverse Team 2011.
 * Multiverse 2 is licensed under the BSD License.
 * For more information please check the README.md file included
 * with this project.
 */

package com.onarandombox.MultiverseSignPortals.utils;

import com.onarandombox.MultiverseSignPortals.MultiverseSignPortals;
import com.onarandombox.MultiverseSignPortals.exceptions.MoreThanOneSignFoundException;
import com.onarandombox.utils.LocationManipulation;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

enum Axis {
    X, Z
}

public class PortalDetector {
    private MultiverseSignPortals plugin;

    public PortalDetector(MultiverseSignPortals plugin) {
        this.plugin = plugin;
    }

    public String getNotchPortalDestination(Player p) throws MoreThanOneSignFoundException, NoMultiverseSignFoundException {
        // Determine corner, should be 1 of 4
        Location l = p.getLocation();
        Block block = l.getBlock();
        Location portalStart;
        Location portalEnd;
        List<Sign> foundSigns = null;
        if (block.getType() == Material.PORTAL) {
            // We found the bottom 2: ##
            if (block.getRelative(1, 0, 0).getType() == Material.PORTAL) {
                portalEnd = block.getRelative(1, 0, 0).getLocation();
                portalStart = block.getRelative(0, 2, 0).getLocation();
                foundSigns = this.checkBlocksOutside(l.getWorld().getBlockAt(portalStart), l.getWorld().getBlockAt(portalEnd), Axis.X);
                this.plugin.log(Level.FINER, "Found normal X");

            } else if (block.getRelative(-1, 0, 0).getType() == Material.PORTAL) {
                portalEnd = block.getLocation();
                portalStart = block.getRelative(-1, 2, 0).getLocation();
                foundSigns = this.checkBlocksOutside(l.getWorld().getBlockAt(portalStart), l.getWorld().getBlockAt(portalEnd), Axis.X);
                this.plugin.log(Level.FINER, "Found inverse X");
            } else if (block.getRelative(0, 0, 1).getType() == Material.PORTAL) {
                portalEnd = block.getRelative(0, 0, 1).getLocation();
                portalStart = block.getRelative(0, 2, 0).getLocation();
                foundSigns = this.checkBlocksOutside(l.getWorld().getBlockAt(portalStart), l.getWorld().getBlockAt(portalEnd), Axis.Z);
                this.plugin.log(Level.FINER, "Found normal Z");

            } else if (block.getRelative(0, 0, -1).getType() == Material.PORTAL) {
                portalEnd = block.getLocation();
                portalStart = block.getRelative(0, 2, -1).getLocation();
                foundSigns = this.checkBlocksOutside(l.getWorld().getBlockAt(portalStart), l.getWorld().getBlockAt(portalEnd), Axis.Z);
                this.plugin.log(Level.FINER, "Found inverse Z");
            }
        }
        if (foundSigns != null) {
            this.plugin.log(Level.FINE, "Woo! Notch Portal!");
            return processSigns(foundSigns, p);
        } else {
            this.plugin.log(Level.FINE, ":( No Notch Portal Here...");
        }
        return null;
    }

    /**
     * Iterate through the signs and return the text if only one is found.
     *
     * @param foundSigns
     *
     * @return
     *
     * @throws MoreThanOneSignFoundException
     * @throws NoMultiverseSignFoundException
     */
    private String processSigns(List<Sign> foundSigns, Player player) throws MoreThanOneSignFoundException, NoMultiverseSignFoundException {
        Sign foundSign = null;
        Sign legacySign = null;
        Sign normalSign = null;
        for (Sign s : foundSigns) {
            if (this.getSignStatus(s) == SignStatus.NetherPortalSign) {
                if (foundSign != null) {
                    throw new MoreThanOneSignFoundException();
                }
                foundSign = s;
            } else if (foundSign == null && this.getSignStatus(s) == SignStatus.Legacy) {
                // Found an old sign
                if (legacySign != null) {
                    throw new MoreThanOneSignFoundException();
                }
                legacySign = s;
            } else if (foundSign == null && this.getSignStatus(s) == SignStatus.SignPortal) {
                // Found a normal signPortal
                if (normalSign != null) {
                    throw new MoreThanOneSignFoundException();
                }
                normalSign = s;
            }
        }
        if (foundSign == null && legacySign == null && normalSign == null) {
            throw new NoMultiverseSignFoundException();
        }
        if (foundSign != null) {
            this.invalidateOtherSigns(foundSign, foundSigns);
            return foundSign.getLine(1);
        }
        if (legacySign != null) {
            if (this.plugin.getCore().getPermissions().hasPermission(player, "multiverse.signportal.validate", true)) {
                this.plugin.log(Level.FINE, "Migrating Legacy Sign");
                legacySign.setLine(0, SignTools.setColor(legacySign.getLine(0), ChatColor.DARK_BLUE));
                legacySign.update(true);
                this.invalidateOtherSigns(legacySign, foundSigns);
                return legacySign.getLine(1);
            }
        }
        if (normalSign != null) {
            this.plugin.log(Level.FINE, "Migrating Normal Sign");
            normalSign.setLine(0, SignTools.setColor(normalSign.getLine(0), ChatColor.DARK_BLUE));
            normalSign.update(true);
            return normalSign.getLine(1);
        }
        throw new NoMultiverseSignFoundException();
    }

    private void invalidateOtherSigns(Sign sign, List<Sign> foundSigns) {
        for (Sign s : foundSigns) {
            if (!s.equals(sign)) {
                s.setLine(0, SignTools.setColor(s.getLine(0), ChatColor.DARK_RED));
                s.update(true);
            }
        }
    }

    public String processSign(Sign sign) {
        if (SignTools.isMVSign(sign.getLine(0), ChatColor.DARK_GREEN)) {
            this.plugin.log(Level.FINER, "Found a MV Sign");
            return sign.getLine(1);
        }
        return null;
    }

    public SignStatus getSignStatus(Sign sign) {
        if (SignTools.isMVSign(sign.getLine(0), ChatColor.DARK_GREEN)) {
            this.plugin.log(Level.FINER, "Found a MV Sign (Sign Portal)");
            return SignStatus.SignPortal;
        }
        if (SignTools.isMVSign(sign.getLine(0), ChatColor.DARK_BLUE)) {
            this.plugin.log(Level.FINER, "Found a MV Sign (Nether Portal that has a Sign)");
            return SignStatus.NetherPortalSign;
        }
        if (SignTools.isMVSign(sign.getLine(0), ChatColor.DARK_RED)) {
            this.plugin.log(Level.FINER, "Found a MV Sign (Disabled)");
            return SignStatus.Disabled;
        }
        if (SignTools.isMVSign(sign.getLine(0), null)) {
            this.plugin.log(Level.FINER, "Found a MV Sign (Legacy)");
            return SignStatus.Legacy;
        }
        return SignStatus.NotASignPortal;
    }

    /**
     * Check to see if the obsidian blocks are in place for a notch portal, since MV-Portals can make portals out of
     * anything.
     *
     * @param top    The top left of the portal
     * @param bottom The bottom right of the portal
     * @param a
     *
     * @return
     */
    private List<Sign> checkBlocksOutside(Block top, Block bottom, Axis a) {
        int xM = (a == Axis.X) ? 1 : 0;
        int zM = (a == Axis.Z) ? 1 : 0;
        // Check the top 2
        if (top.getRelative(0, 1, 0).getType() != Material.OBSIDIAN) {
            return null;
        }
        if (top.getRelative(1 * xM, 1, 1 * zM).getType() != Material.OBSIDIAN) {
            return null;
        }
        this.plugin.log(Level.FINER, "Found top 2");
        // Check the bottom 2
        if (bottom.getRelative(0, -1, 0).getType() != Material.OBSIDIAN) {
            return null;
        }
        if (bottom.getRelative(-1 * xM, -1, -1 * zM).getType() != Material.OBSIDIAN) {
            return null;
        }
        this.plugin.log(Level.FINER, "Found bottom 2");
        // Check the Left 3
        if (top.getRelative(-1 * xM, 0, -1 * zM).getType() != Material.OBSIDIAN) {
            return null;
        }
        if (top.getRelative(-1 * xM, -1, -1 * zM).getType() != Material.OBSIDIAN) {
            return null;
        }
        if (top.getRelative(-1 * xM, -2, -1 * zM).getType() != Material.OBSIDIAN) {
            return null;
        }
        this.plugin.log(Level.FINER, "Found left 3");
        // Check the Right 3
        if (bottom.getRelative(1 * xM, 0, 1 * zM).getType() != Material.OBSIDIAN) {
            return null;
        }
        if (bottom.getRelative(1 * xM, 1, 1 * zM).getType() != Material.OBSIDIAN) {
            return null;
        }
        if (bottom.getRelative(1 * xM, 2, 1 * zM).getType() != Material.OBSIDIAN) {
            return null;
        }
        this.plugin.log(Level.FINER, "Found right 3");
        Block topper = top.getRelative(-1 - (xM * 1), 1, -1 - (zM * 1));
        Block bottomer = bottom.getRelative(1 + (xM * 1), -1, 1 + (zM * 1));
        return this.checkZoneForSigns(topper, bottomer);
    }

    private List<Sign> checkZoneForSigns(Block topper, Block bottomer) {
        Location looking = new Location(topper.getWorld(), 0, 0, 0);
        List<Sign> signs = new ArrayList<Sign>();
        for (int x = topper.getX(); x <= bottomer.getX(); x++) {
            looking.setX(x);
            for (int y = bottomer.getY(); y <= topper.getY(); y++) {
                looking.setY(y);
                for (int z = topper.getZ(); z <= bottomer.getZ(); z++) {
                    looking.setZ(z);
                    this.plugin.log(Level.FINEST, "Looking for sign at " + LocationManipulation.strCoordsRaw(looking));
                    Material isASign = topper.getWorld().getBlockAt(looking).getType();
                    if (isASign == Material.WALL_SIGN || isASign == Material.SIGN_POST) {
                        this.plugin.log(Level.FINER, "WOO Found one! " + LocationManipulation.strCoordsRaw(looking));
                        signs.add((Sign) topper.getWorld().getBlockAt(looking).getState());
                    }
                }
            }
        }
        return signs;
    }

}
