package com.sil3ntone.disposalchest;

import org.bukkit.Location;

/**
 * Created by Pratham on 3/28/2017.
 */
public class TrashChest {
    private int ID;
    private Location loc;

    public TrashChest(int ID, Location loc) {
        this.ID = ID;
        this.loc = loc;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public Location getLoc() {
        return loc;
    }

    public void setLoc(Location loc) {
        this.loc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        Location that = ((TrashChest)o).getLoc();

        return getLoc() != null ? getLoc().equals(that) : that == null;
    }

    @Override
    public int hashCode() {
        return getLoc() != null ? getLoc().hashCode() : 0;
    }
}
