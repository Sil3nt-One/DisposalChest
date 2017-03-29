package com.sil3ntone.disposalchest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;

/**
 * Created by Pratham on 3/22/2017.
 */
public class DatabaseQueries {

    private DisposalChest plugin;

    public DatabaseQueries(DisposalChest plugin) {
        this.plugin = plugin;
    }


    public static boolean executeSql(String sql) {

        final String connStr = "jdbc:mysql://" + DisposalChest.mysqlhost +"/" + DisposalChest.mysqldatabase + "?" +
                "user=" + DisposalChest.mysqluser + "&password=" + DisposalChest.mysqlpass;

        Connection conn = null;

        try {
            conn = DriverManager.getConnection(connStr);

            PreparedStatement createTableStmt = conn.prepareStatement(sql);

            createTableStmt.execute();
            createTableStmt.close();
            return true;

        } catch (SQLException ex) {
            // handle any errors
            System.out.println("--------------[DisposalChest]--------------");
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("--------------[DisposalChest]--------------");
            return false;
        }
    }

    public boolean createTables() {
        return executeSql("CREATE TABLE IF NOT EXISTS" +
            "`" + DisposalChest.mysqltable_main + "`" +
            "( " +
            "`id` INT NOT NULL AUTO_INCREMENT, " +
            "`player_uuid` CHAR(36) NOT NULL, " +
            "`sign_world` VARCHAR(64) NOT NULL, " +
            "`sign_x` INT NOT NULL, " +
            "`sign_y` INT NOT NULL, " +
            "`sign_z` INT NOT NULL, " +
            "PRIMARY KEY (`id`), " +
            "CONSTRAINT Location UNIQUE (sign_x,sign_y,sign_z)" +
            ");");
    }

    public void addTrashChest(final String playerUUID, final String signWorld, final int x, final int y, final int z) {
        Bukkit.getScheduler().runTaskAsynchronously(DisposalChest.plugin, new BukkitRunnable() {
            public void run() {
                String sql = "INSERT INTO `" + DisposalChest.mysqltable_main + "`" +
                        "(player_uuid, sign_world, sign_x, sign_y, sign_z) " +
                        "values (?, ?, ?, ?, ?);";

                final String connStr = "jdbc:mysql://" + DisposalChest.mysqlhost +"/" + DisposalChest.mysqldatabase + "?" +
                        "user=" + DisposalChest.mysqluser + "&password=" + DisposalChest.mysqlpass;

                Connection conn = null;

                try {
                    conn = DriverManager.getConnection(connStr);

                    PreparedStatement sqlStmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    sqlStmt.setString(1, playerUUID);
                    sqlStmt.setString(2, signWorld);
                    sqlStmt.setInt(3, x);
                    sqlStmt.setInt(4, y);
                    sqlStmt.setInt(5, z);

                    sqlStmt.executeUpdate();
                    ResultSet rs = sqlStmt.getGeneratedKeys();
                    if (rs.next()) {
                        int id = rs.getInt(1);
                        Location l = new Location(Bukkit.getServer().getWorld(signWorld), x, y, z);
                        boolean added = DisposalChest.trashChestList.add(new TrashChest(id, l));
//                        System.out.println(added ? "added" : "not added");
                    }
                    sqlStmt.close();
                    int playerIdxInList = DisposalChest.trashChestOwnerLimitList.indexOf(new TrashChestOwner(playerUUID, -1));
                    if(playerIdxInList > -1) {
                        DisposalChest.trashChestOwnerLimitList.get(playerIdxInList).addCount();
                    }
                    else {
                        DisposalChest.trashChestOwnerLimitList.add(new TrashChestOwner(playerUUID, 1));
                    }

                } catch (SQLException ex) {
                    // handle any errors
                    System.out.println("--------------[DisposalChest]--------------");
                    System.out.println("SQLException: " + ex.getMessage());
                    System.out.println("--------------[DisposalChest]--------------");
                }
            }

        });
    }

    public void removeTrashChest(final String signWorld, final int x, final int y, final int z) {
        Bukkit.getScheduler().runTaskAsynchronously(DisposalChest.plugin, new BukkitRunnable() {
            public void run() {
                String sql = "DELETE FROM `" + DisposalChest.mysqltable_main + "`" +
                        "WHERE sign_world = ? AND sign_x = ? AND sign_y = ? AND sign_z = ?";

                final String connStr = "jdbc:mysql://" + DisposalChest.mysqlhost +"/" + DisposalChest.mysqldatabase + "?" +
                        "user=" + DisposalChest.mysqluser + "&password=" + DisposalChest.mysqlpass;

                Connection conn = null;

                try {
                    String playerUUID = getTrashChestOwnerUUID(signWorld, x, y, z);

                    conn = DriverManager.getConnection(connStr);

                    PreparedStatement sqlStmt = conn.prepareStatement(sql);
//                    sqlStmt.setInt(1, id);
                    sqlStmt.setString(1, signWorld);
                    sqlStmt.setInt(2, x);
                    sqlStmt.setInt(3, y);
                    sqlStmt.setInt(4, z);

                    sqlStmt.executeUpdate();
                    sqlStmt.close();

//                    System.out.println(signWorld);
                    World world = Bukkit.getServer().getWorld(signWorld);
                    Location l = new Location(world, x, y, z);
                    int trashChestIdx = DisposalChest.trashChestList.indexOf(new TrashChest(-1, l));
                    if(trashChestIdx > -1) {
                        DisposalChest.trashChestList.remove(trashChestIdx);
                    }
                    int playerIdxInList = DisposalChest.trashChestOwnerLimitList.indexOf(new TrashChestOwner(playerUUID, -1));
                    if(playerIdxInList > -1) {
                        DisposalChest.trashChestOwnerLimitList.get(playerIdxInList).subtractCount();
                    }
                    else {
                        System.out.println("[DisposalChest]: PlayerUUID " + playerUUID + " not found");
                    }

                } catch (SQLException ex) {
                    // handle any errors
                    System.out.println("--------------[DisposalChest]--------------");
                    System.out.println("SQLException: " + ex.getMessage());
                    System.out.println("--------------[DisposalChest]--------------");
                }
            }

        });
    }

    public void loadTrashChestsIntoMemory() {
        Bukkit.getScheduler().runTaskAsynchronously(DisposalChest.plugin, new BukkitRunnable() {
            public void run() {
                String sql = "SELECT id, player_uuid, sign_world, sign_x, sign_y, sign_z FROM `" + DisposalChest.mysqltable_main + "`;";

                final String connStr = "jdbc:mysql://" + DisposalChest.mysqlhost +"/" + DisposalChest.mysqldatabase + "?" +
                        "user=" + DisposalChest.mysqluser + "&password=" + DisposalChest.mysqlpass;

                Connection conn = null;

                try {
                    conn = DriverManager.getConnection(connStr);

                    PreparedStatement sqlStmt = conn.prepareStatement(sql);

                    ResultSet rs = sqlStmt.executeQuery();
                    DisposalChest.trashChestList.clear();
                    while(rs.next()) {
                        int id = rs.getInt("id");
                        String world = rs.getString("sign_world");
                        String playerUUID = rs.getString("player_uuid");
                        double x = (double)rs.getInt("sign_x");
                        double y = (double)rs.getInt("sign_y");
                        double z = (double)rs.getInt("sign_z");
                        Location l = new Location(plugin.getServer().getWorld(world), x, y, z);
                        DisposalChest.trashChestList.add(new TrashChest(id, l));
                        int playerIdxInList = DisposalChest.trashChestOwnerLimitList.indexOf(new TrashChestOwner(playerUUID, -1));
                        if(playerIdxInList > -1) {
                            DisposalChest.trashChestOwnerLimitList.get(playerIdxInList).addCount();
                        }
                        else {
                            DisposalChest.trashChestOwnerLimitList.add(new TrashChestOwner(playerUUID, 1));
                        }
                    }
                    sqlStmt.close();

                } catch (SQLException ex) {
                    // handle any errors
                    System.out.println("--------------[DisposalChest]--------------");
                    System.out.println("SQLException: " + ex.getMessage());
                    System.out.println("--------------[DisposalChest]--------------");
                }
            }

        });
    }

    public String getTrashChestOwnerUUID(final String signWorld, final int x, final int y, final int z) {
        String sql = "SELECT player_uuid FROM `" + DisposalChest.mysqltable_main + "` WHERE sign_world = ? AND sign_x = ? AND sign_y = ? AND sign_z = ? ";

        final String connStr = "jdbc:mysql://" + DisposalChest.mysqlhost +"/" + DisposalChest.mysqldatabase + "?" +
                "user=" + DisposalChest.mysqluser + "&password=" + DisposalChest.mysqlpass;

        Connection conn = null;

        try {
            conn = DriverManager.getConnection(connStr);

            PreparedStatement sqlStmt = conn.prepareStatement(sql);

            sqlStmt.setString(1, signWorld);
            sqlStmt.setInt(2, x);
            sqlStmt.setInt(3, y);
            sqlStmt.setInt(4, z);

            ResultSet rs = sqlStmt.executeQuery();
            if(rs.next()) {
                String playerUUID = rs.getString("player_uuid");
                return playerUUID;
            }
            sqlStmt.close();

        } catch (SQLException ex) {
            // handle any errors
            System.out.println("--------------[DisposalChest]--------------");
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("--------------[DisposalChest]--------------");
        }

        return "";
    }
}
