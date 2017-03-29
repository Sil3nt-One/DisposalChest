package com.sil3ntone.disposalchest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javafx.scene.chart.PieChart;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Pratham on 3/22/2017.
 */
public class DisposalChest extends JavaPlugin {

    public static final String TAG = "[DisposalChest]";
    public static final int perPlayerChestLimit = 2;

    public static DisposalChest plugin;
    public static String mysqlhost;
    public static String mysqluser;
    public static String mysqlpass;
    public static String mysqldatabase;
    public static String mysqltable_main;
    private static float tps;

    public static int minChestClearTime;
    public static int maxChestClearTime;
    public static int minTPS;
    public static int maxTPS;
    public static int minChestClearBatchSize;

    File configFile;
    FileConfiguration config;

    public static List<TrashChest> trashChestList;
    public static List<TrashChestOwner> trashChestOwnerLimitList;
    public static int trashChestPos;



    @Override
    public void onDisable() {
        getServer().getScheduler().cancelTasks(this);
    }

    @Override
    public void onEnable() {

        plugin = this;

        configFile = new File(getDataFolder(), "config.yml");
        try {
            firstRun();
        } catch (Exception e) {
            e.printStackTrace();
        }

        trashChestPos = 0;

        config = new YamlConfiguration();
        loadConfig();

        Connection conn = null;
        trashChestList = new ArrayList<TrashChest>();
        trashChestOwnerLimitList = new ArrayList<TrashChestOwner>();

        DatabaseQueries dbq = new DatabaseQueries(plugin);
        if(!dbq.createTables()) {
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        dbq.loadTrashChestsIntoMemory();

        new DisposalChestBlockListener(this);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new BukkitRunnable()
        {
            int sec, second;
            int ticks;
            float tps;

            public void run()
            {
                sec = (int)(System.currentTimeMillis() / 1000);

                if(second == sec)
                {
                    ticks++;
                }
                else
                {
                    second = sec;
                    tps = (tps == 0 ? ticks : ((tps + ticks) / 2));
                    ticks = 1;

//                    System.out.print("TPS = " + tps);
                    DisposalChest.tps = tps;
                }
            }
        }, 0, 1);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new BukkitRunnable()
        {
            public void run()
            {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                    public void run() {
                        clearTrashChests();
                    }
                });
            }
        }, 100, 200);

        this.getCommand("trashchest").setExecutor(new DisposalChestCommandExecutor(this));
    }

    private void clearTrashChests() {
//        System.out.println("tps " + this.tps + "  < minTPS " + this.minTPS);
        if(DisposalChest.tps < DisposalChest.minTPS) {
            return;
        }

        int chestListSize = trashChestList.size();

        if(chestListSize < 1) {
            return;
        }

        int limitTPS = Math.min(DisposalChest.maxTPS, Math.round(DisposalChest.tps));
        int timeToClearAllChests = Math.round(DisposalChest.maxChestClearTime - ((DisposalChest.maxChestClearTime - DisposalChest.minChestClearTime) * (DisposalChest.maxTPS - limitTPS) /  (DisposalChest.maxTPS - DisposalChest.minTPS) ));

//        System.out.println("chestListSize: " + chestListSize);
//        System.out.println("timeToClearAllChests: " + timeToClearAllChests);

        int chestBatchSize = chestListSize / timeToClearAllChests * 10 ;
        chestBatchSize = Math.max(DisposalChest.minChestClearBatchSize, chestBatchSize);

//        System.out.println("chestBatchSize: " + chestBatchSize);

        for(int i = 0; i < chestBatchSize; i++) {
            TrashChest tc = trashChestList.get(trashChestPos);
            Location signLoc = (Location) tc.getLoc();
            boolean chunkLoaded = plugin.getServer().getWorld(signLoc.getWorld().getName()).isChunkLoaded(signLoc.getBlockX() >> 4, signLoc.getBlockZ() >> 4);

            if(chunkLoaded) {
                Block signBlock = plugin.getServer().getWorld(signLoc.getWorld().getName()).getBlockAt(signLoc);
                if(signBlock.getType() == Material.WALL_SIGN) {
                    org.bukkit.material.Sign s = (org.bukkit.material.Sign) signBlock.getState().getData();
                    Block chestBlock = signBlock.getRelative(s.getAttachedFace());
                    if(chestBlock.getType() == Material.CHEST) {
                        Chest c = (Chest) chestBlock.getState();
                        if(!isInvEmpty(c.getInventory())) {
                            c.getInventory().clear();
//                            System.out.println("Clearing Inv : " + chestBlock.getX() + "," + chestBlock.getY() + "," + chestBlock.getZ());
                        }
                    }
                }
                else {
                    // If not a wall_sign delete from MySQL
                    DatabaseQueries dbq = new DatabaseQueries(plugin);
                    dbq.removeTrashChest(signLoc.getWorld().getName(), signLoc.getBlockX(), signLoc.getBlockY(), signLoc.getBlockZ());
                    System.out.println("Removing non-existing trash chest (sign missing) @ " + signLoc.getWorld().getName() + " - " + signLoc.getBlockX() + "," + signLoc.getBlockY() + "," +  signLoc.getBlockZ());
                }
            }

//            System.out.println("-----------");

            DisposalChest.trashChestPos++;
            if(DisposalChest.trashChestPos >= chestListSize) {
                DisposalChest.trashChestPos = 0;
                break;
            }
        }
    }

    private void firstRun() throws Exception {
        if(!configFile.exists()){
            configFile.getParentFile().mkdirs();
            copy(getResource("config.yml"), configFile);
        }
    }

    private void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        try {
            config.load(configFile);

            DisposalChest.mysqlhost = config.getString("mysqlhost");
            DisposalChest.mysqluser = config.getString("mysqluser");
            DisposalChest.mysqlpass = config.getString("mysqlpass");
            DisposalChest.mysqlpass = mysqlpass == null ? "" : mysqlpass;
            DisposalChest.mysqldatabase = config.getString("mysqldatabase");
            DisposalChest.mysqltable_main = config.getString("mysqltable_main");

            DisposalChest.minChestClearTime = config.getInt("minChestClearTime");
            DisposalChest.maxChestClearTime = config.getInt("maxChestClearTime");
            DisposalChest.minTPS = config.getInt("minTPS");
            DisposalChest.maxTPS = config.getInt("maxTPS");
            DisposalChest.minChestClearBatchSize = config.getInt("minChestClearBatchSize");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isInvEmpty(Inventory inv) {
        for(ItemStack item : inv.getContents()) {
            if(item != null) {
                return false;
            }
        }
        return true;
    }
}
