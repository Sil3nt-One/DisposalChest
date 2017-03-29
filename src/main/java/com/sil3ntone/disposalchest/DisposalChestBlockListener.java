package com.sil3ntone.disposalchest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

/**
 * Created by Pratham on 3/22/2017.
 */
public class DisposalChestBlockListener implements Listener {
    public static DisposalChest plugin;

    public DisposalChestBlockListener(DisposalChest plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent e) {
        if(e.isCancelled()) {
            return;
        }
        Block b = e.getBlock();
        Player p = e.getPlayer();
        if(b.getType() == Material.WALL_SIGN) {
            org.bukkit.block.Sign s = (org.bukkit.block.Sign) b.getState();
//            System.out.println(s.getLine(0));
            if(s.getLine(0).indexOf("[Trash]") > -1) {
                Location l = b.getLocation();
                l = new Location(l.getWorld(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
                int trashChestIdx = DisposalChest.trashChestList.indexOf(new TrashChest(-1, l));
                if(trashChestIdx > -1) {
                    DatabaseQueries dbq = new DatabaseQueries(plugin);
                    dbq.removeTrashChest(b.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
                    p.sendMessage("§c" + DisposalChest.TAG + ": Chest unregistered");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onSignPlace(SignChangeEvent e) {
        if(e.isCancelled()) {
            return;
        }
        if(e.getBlock().getType() != Material.WALL_SIGN) {
            return;
        }
        Sign s = (Sign) e.getBlock().getState().getData();
        Block b = e.getBlock().getRelative(s.getAttachedFace());
        Player p = e.getPlayer();
        if(e.getLine(0).equalsIgnoreCase("trash")) {
            int playerIdxInList = DisposalChest.trashChestOwnerLimitList.indexOf(new TrashChestOwner(p.getUniqueId().toString(), -1));
            if(playerIdxInList > -1) {
                int currentNoOfTrashChests = DisposalChest.trashChestOwnerLimitList.get(playerIdxInList).getCount();
                if(currentNoOfTrashChests >= DisposalChest.perPlayerChestLimit) {
                    e.setCancelled(true);
                    p.sendMessage(String.format("§c" + DisposalChest.TAG +  ": You have exceeded limit of %d %s!", DisposalChest.perPlayerChestLimit, (DisposalChest.perPlayerChestLimit == 1 ? "chest" : "chests")));
                    return;
                }
            }
            if(b.getType() == Material.CHEST) {
                Chest c = (Chest) b.getState();
                if(!DisposalChest.isInvEmpty(c.getInventory())) {
                    e.setCancelled(true);
                    e.getBlock().breakNaturally();
                    p.sendMessage("§c" + DisposalChest.TAG +  ": Please empty the chest before placing the TRASH sign!");
                }
                else {
                    if(!trashSignRegisteredAtLoc(e.getBlock().getLocation())) {
                        e.setLine(0, "§n§o§1[Trash]");
                        DatabaseQueries dbq = new DatabaseQueries(plugin);
                        dbq.addTrashChest(p.getUniqueId().toString(), e.getBlock().getWorld().getName(), e.getBlock().getX(), e.getBlock().getY(), e.getBlock().getZ());
                        p.sendMessage("§2" + DisposalChest.TAG + ": Chest registered");
                    }
                    else {
                        e.setCancelled(true);
                        e.getBlock().breakNaturally();
                        p.sendMessage("§c" + DisposalChest.TAG + ": Chest already registered");
                    }
                }
            }
        }
    }



    public boolean trashSignRegisteredAtLoc(Location l) {
        return DisposalChest.trashChestList.indexOf(new TrashChest(-1, l)) > -1;
    }
}
