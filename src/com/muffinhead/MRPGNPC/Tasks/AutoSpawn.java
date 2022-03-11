package com.muffinhead.MRPGNPC.Tasks;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import com.muffinhead.MRPGNPC.MRPGNPC;
import com.muffinhead.MRPGNPC.NPCs.MobNPC;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class AutoSpawn extends Task {
    protected ConcurrentHashMap<String,Integer> spawnTick = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String,Integer> mobAmount = new ConcurrentHashMap<>();
    @Override
    public void onRun(int i) {
        //spawntick
        for (String s:spawnTick.keySet()){
            spawnTick.put(s,spawnTick.get(s)+1);
        }

        //spawn
        for (Config config:MRPGNPC.pointconfigs.values()) {
            //spawnpoint position

            if (!MRPGNPC.mrpgnpc.getServer().loadLevel(config.getString("PointPosition").split(":")[3])) {
                System.out.println("level " + config.getString("PointPosition").split(":")[3] + " is not exist");
            } else {
            }
            Location location = new Location();
            location.x = Double.parseDouble(config.getString("PointPosition").split(":")[0])-0.1;
            location.y = Double.parseDouble(config.getString("PointPosition").split(":")[1]);
            location.z = Double.parseDouble(config.getString("PointPosition").split(":")[2])-0.1;
            location.level = MRPGNPC.mrpgnpc.getServer().getLevelByName(config.getString("PointPosition").split(":")[3]);
            if (config.getString("PointPosition").split(":").length > 4) {
                location.yaw = Double.parseDouble(config.getString("PointPosition").split(":")[4]);
                location.pitch = Double.parseDouble(config.getString("PointPosition").split(":")[5]);
            }
            //mobs spawnlist and limit
            List<String> spawnlist = config.getList("SpawnList");
            if(location.getChunk() == null){
                if(!location.level.isSpawnChunk((int) location.x, (int) location.z)){
                }
            }else {
                //System.out.println(location.getChunk());
                if (!location.getChunk().isGenerated()) {
                    location.getChunk().setGenerated();
                }
            }
            for (String spawns : spawnlist) {
     /*
    mobfilename-respawntick-1timespawnamount-maxamount-spawnlimit
      */
                String pointname = config.getString("PointName");
                String mobFeature = spawns + ":" + pointname;
                String mobfile = spawns.split(":")[0];
                int respawntick = Integer.parseInt(spawns.split(":")[1]);
                int amountOneTime = Integer.parseInt(spawns.split(":")[2]);
                int maxamount = Integer.parseInt(spawns.split(":")[3]);
                boolean canSpawn = true;
                if (!spawnTick.containsKey(mobFeature)) {
                    spawnTick.put(mobFeature, 0);
                }
                if (spawns.split(":").length >= 5) {
                    String spawnlimit = spawns.split(":")[4];
                    if (spawnlimit.contains("-")) {
                        for (String limit : spawnlimit.split("-")) {
                            canSpawn = onCheckSpawnLimit(limit, location);
                        }
                    } else {
                        canSpawn = onCheckSpawnLimit(spawnlimit, location);
                    }
                }
                //respawntick limit
                if (spawnTick.get(mobFeature) < respawntick) {
                    canSpawn = false;
                }
                //respawntick limit


                //maxmob limit
                if (MRPGNPC.mrpgnpc.getServer().isLevelLoaded(config.getString("PointPosition").split(":")[3])) {
                    MRPGNPC.mrpgnpc.getServer().loadLevel(config.getString("PointPosition").split(":")[3]);
                }
                mobAmount.put(pointname, 0);
                for (Entity entity : location.level.getEntities()) {
                    if (entity instanceof MobNPC) {
                        if (((MobNPC) entity).getMobFeature() != null) {
                            if (((MobNPC) entity).getMobFeature().equals(mobFeature)) {
                                mobAmount.put(pointname, mobAmount.get(pointname) + 1);
                            }
                        }
                    }
                }
                if (mobAmount.get(pointname) >= maxamount) {
                    canSpawn = false;
                }
                //maxmob limit
                if (canSpawn) {
                    int spawnamount = amountOneTime;
                    if (mobAmount.get(pointname) + amountOneTime > maxamount) {
                        spawnamount = maxamount - mobAmount.get(pointname);
                    }
                    for (int t = 0; t < spawnamount; t++) {
                        //try {
                        if(location.getChunk() != null) {
                            MobNPC npc = MRPGNPC.mrpgnpc.spawnNPC(MRPGNPC.mrpgnpc.getServer().getConsoleSender(), mobfile, location, mobFeature);
                            npc.spawnToAll();
                        }
                        // } catch (Exception e) {
                        /*
                            System.out.println("Spawn Wrong！");
                            System.out.println("MobFile"+mobfile);
                            System.out.println("Location"+location);
                            System.out.println("MobFeature"+mobFeature);
                        }
                         */
                    }
                    spawnTick.put(mobFeature, 0);
                }
            }
        }
    }
    public boolean onCheckSpawnLimit(String limit,Location location){
        boolean canSpawn = true;
        String condition = limit;
        String function = "";
        if (limit.contains("~")) {
            condition = limit.split("~")[0];
            function = limit.split("~")[1];
        }
        switch (condition) {
            case "atDay": {
                if (location.getLevel().getTime()>=13800&&location.getLevel().getTime()<24000) {
                    canSpawn = false;
                }
                break;
            }
            case "atNight": {
                if (location.getLevel().getTime()>=0&&location.getLevel().getTime()<13800) {
                    canSpawn = false;
                }
                break;
            }
            case "playersNearby": {
                boolean nearby = false;
                for (Player player : location.getLevel().getPlayers().values()) {
                    if (player.distance(location) <= Double.parseDouble(function)) {
                        nearby = true;
                    }
                }
                canSpawn = nearby;
                break;
            }
            case "noOneNearby": {
                boolean nearby = false;
                for (Player player : location.getLevel().getPlayers().values()) {
                    if (player.distance(location) <= Double.parseDouble(function)) {
                        nearby = true;
                    }
                }
                canSpawn = !nearby;
                break;
            }
            default:
        }
        return canSpawn;
    }
}
