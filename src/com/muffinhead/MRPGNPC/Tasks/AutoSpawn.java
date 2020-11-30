package com.muffinhead.MRPGNPC.Tasks;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Position;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import com.muffinhead.MRPGNPC.MRPGNPC;
import com.muffinhead.MRPGNPC.NPCs.MobNPC;

import javax.swing.*;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class AutoSpawn extends Task {
    protected ConcurrentHashMap<String,Integer> spawnTick = new ConcurrentHashMap<>();

    @Override
    public void onRun(int i) {
        //spawntick
        for (String s:spawnTick.keySet()){
            spawnTick.put(s,spawnTick.get(s)+1);
        }

        //spawn
        for (Config config:MRPGNPC.pointconfigs.values()){
            //spawnpoint position
            Position position = new Position();
            position.x = Double.parseDouble(config.getString("PointPosition").split(":")[0]);
            position.y = Double.parseDouble(config.getString("PointPosition").split(":")[1]);
            position.z = Double.parseDouble(config.getString("PointPosition").split(":")[2]);
            position.level = MRPGNPC.mrpgnpc.getServer().getLevelByName(config.getString("PointPosition").split(":")[3]);
            //mobs spawnlist and limit
            List<String> spawnlist = config.getList("SpawnList");
            for (String spawns:spawnlist){
     /*
    mobfilename-respawntick-1timespawnamount-maxamount-spawnlimit
     */
                String mobFeature = spawns+":"+config.getString("PointName");
                String mobfile = spawns.split(":")[0];
                int respawntick = Integer.parseInt(spawns.split(":")[1]);
                int amountOneTime = Integer.parseInt(spawns.split(":")[2]);
                int maxamount = Integer.parseInt(spawns.split(":")[3]);
                boolean canSpawn = true;
                if (!spawnTick.containsKey(mobFeature)) {
                    spawnTick.put(mobFeature, 0);
                }
                if (spawns.split(":").length>=5) {
                    String spawnlimit = spawns.split(":")[4];
                    for (String limit : spawnlimit.split("-")) {
                        String condition = limit;
                        String function = "";
                        if (limit.contains("~")) {
                            condition = limit.split("~")[0];
                            function = limit.split("~")[1];
                        }
                        switch (condition) {
                            case "atDay": {
                                if (!position.getLevel().isDaytime()) {
                                    canSpawn = false;
                                }
                                break;
                            }
                            case "atNight": {
                                if (position.getLevel().isDaytime()) {
                                    canSpawn = false;
                                }
                                break;
                            }
                            case "playersNearby": {
                                boolean nearby = false;
                                for (Player player : position.getLevel().getPlayers().values()) {
                                    if (player.distance(position) <= Double.parseDouble(function)) {
                                        nearby = true;
                                    }
                                }
                                canSpawn = nearby;
                                break;
                            }
                            case "noOneNearby": {
                                boolean nearby = false;
                                for (Player player : position.getLevel().getPlayers().values()) {
                                    if (player.distance(position) <= Double.parseDouble(function)) {
                                        nearby = true;
                                    }
                                }
                                canSpawn = !nearby;
                                break;
                            }
                            default:
                        }
                    }
                }
                //respawntick limit
                if (spawnTick.get(mobFeature)<respawntick){
                    canSpawn = false;
                }
                //respawntick limit


                //maxmob limit
                int mobamount = 0;
                for (Entity entity:position.getLevel().getEntities()){
                    if (entity instanceof MobNPC) {
                        if (((MobNPC) entity).getMobFeature() != null) {
                            if (((MobNPC) entity).getMobFeature().equals(mobFeature)) {
                                mobamount++;
                            }
                        }
                    }
                }
                if (mobamount>=maxamount){
                    canSpawn = false;
                }
                //maxmob limit
                if (canSpawn){
                    int spawnamount = amountOneTime;
                    if (mobamount+amountOneTime>maxamount){
                        spawnamount = maxamount-mobamount;
                    }
                    for (int t = 0;t<spawnamount;t++) {
                        MobNPC npc = MRPGNPC.mrpgnpc.spawnNPC(MRPGNPC.mrpgnpc.getServer().getConsoleSender(), mobfile, position, mobFeature);
                        npc.spawnToAll();
                    }
                    spawnTick.put(mobFeature,0);
                }
            }
        }
    }
}
