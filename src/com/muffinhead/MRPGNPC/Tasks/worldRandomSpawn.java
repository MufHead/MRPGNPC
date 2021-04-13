package com.muffinhead.MRPGNPC.Tasks;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.math.Vector3;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import com.muffinhead.MRPGNPC.MRPGNPC;
import com.muffinhead.MRPGNPC.NPCs.MobNPC;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class worldRandomSpawn extends Task {
    protected ConcurrentHashMap<String, Integer> spawnTick = new ConcurrentHashMap<>();
    @Override
    public void onRun(int i) {
        for (String levelname : MRPGNPC.worldSpawnConfig.getAll().keySet()) {
     /*
    mobid-plimit-wlimit
     */
            List<String> spawnlist = MRPGNPC.worldSpawnConfig.getList(levelname);
            Level level = Server.getInstance().getLevelByName(levelname);
            for (String spawns : spawnlist) {
                for (Player player : level.getPlayers().values()) {
                    if (player.isOnGround()) {
                        String mobfile = spawns.split(":")[0];
                        int maxamount = Integer.parseInt(spawns.split(":")[1]);
                        int maxamountDistance = Integer.parseInt(spawns.split(":")[2]);
                        int spawnDistance = Integer.parseInt(spawns.split(":")[3]);
                        double disappearDistance = Double.parseDouble(spawns.split(":")[4]);
                        int cooldown = Integer.parseInt(spawns.split(":")[5]);
                        int pro = Integer.parseInt(spawns.split(":")[6]);
                        Location location = getSafeLoc(level, spawnDistance, player);

                        if (location != null) {

                            if (!spawnTick.containsKey(levelname + ":" + mobfile + ":" + player.getName())) {
                                spawnTick.put(levelname + ":" + mobfile + ":" + player.getName(), 0);
                            }
                            int amount = 0;
                            for (Entity entity : player.getLevel().getEntities()) {
                                if (entity instanceof MobNPC) {
                                    if (((MobNPC) entity).getMobFeature().split(":")[0].equals("Random")) {
                                        if (((MobNPC) entity).getMobFeature().split(":")[1].equals(mobfile)) {
                                            if (entity.distance(player) <= maxamountDistance) {
                                                amount++;
                                            }
                                        }
                                    }
                                }
                            }
                            String mobFeature = "Random"+":"+mobfile+":"+disappearDistance;
                            List<Double> XYZ = new ArrayList<>();
                            XYZ.add(location.x);
                            XYZ.add(location.y);
                            XYZ.add(location.z);
                            if (spawnTick.get(levelname + ":" + mobfile + ":" + player.getName()) <= 0) {
                                if (amount < maxamount) {
                                    if (new Random().nextInt(10001) <= pro) {
                                        MobNPC npc = MRPGNPC.mrpgnpc.spawnNPC(Server.getInstance().getConsoleSender(),mobfile,location,mobFeature);
                                        npc.spawnToAll();
                                    }
                                    spawnTick.put(levelname + ":" + mobfile + ":" + player.getName(), cooldown);
                                }
                            }
                        }
                    }
                }
            }
        }
        for (String key : spawnTick.keySet()) {
            if (spawnTick.get(key)>0) {
                spawnTick.put(key, spawnTick.get(key) - 1);
            }else{
                spawnTick.remove(key);
            }
        }
    }

    public Location getSafeLoc(Level level,int distance,Player player) {
        Location location = player.getLocation().clone();
        double xx = new Random().nextDouble()+new Random().nextInt(distance);
        double zz = new Random().nextDouble()+new Random().nextInt(distance);
        if (new Random().nextBoolean()) {
            xx = -xx;
        }
        if (new Random().nextBoolean()) {
            zz = -zz;
        }
        location.x+=xx;
        location.z+=zz;
        if (level.getBlock(new Vector3(location.x,location.y,location.z)).getId() != Block.AIR){
            for (;(level.getBlock(new Vector3(location.x,location.y,location.z)).getId() != Block.AIR||!level.getBlock(new Vector3(location.x,location.y,location.z)).canPassThrough())&&location.y<=location.y+distance;){
                location.y++;
            }
        }
        if (level.getBlock(new Vector3(location.x,location.y,location.z)).getId() != Block.AIR){
            for (;(level.getBlock(new Vector3(location.x,location.y,location.z)).getId() != Block.AIR||!level.getBlock(new Vector3(location.x,location.y,location.z)).canPassThrough())&&location.y<=location.y-distance;){
                location.y--;
            }
        }
        if (level.getBlock(new Vector3(location.x,location.y,location.z)).getId() != Block.AIR||!level.getBlock(new Vector3(location.x,location.y,location.z)).canPassThrough()){
            return getSafeLoc(level,distance,player);
        }
        for (;(level.getBlock(new Vector3(location.x,location.y-1,location.z)).getId() == Block.AIR||level.getBlock(new Vector3(location.x,location.y,location.z)).canPassThrough())&&location.distance(player)<=distance;){
            location.y--;
        }
        if (level.getBlock(new Vector3(location.x,location.y-1,location.z)).getId()== Block.AIR){
            return null;
        }
        if (!location.getChunk().isLoaded()){
            return null;
        }


        return location;
    }
}
