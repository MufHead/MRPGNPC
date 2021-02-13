package com.muffinhead.MRPGNPC.Events;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByChildEntityEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.event.level.ChunkUnloadEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.math.Vector3;
import com.muffinhead.MRPGNPC.Effects.Bullet;
import com.muffinhead.MRPGNPC.NPCs.MobNPC;
import com.muffinhead.MRPGNPC.NPCs.NPC;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MobNPCBeAttack implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDamaged(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof MobNPC) {
            //particle
            if (!event.isCancelled()){
                ((MobNPC) entity).beattackparticle();
            }
            //particle
            //attackcooldown 0
            //attackcooldown 0
            //hatepool damagepool
            if (event instanceof EntityDamageByEntityEvent) {
                event.setAttackCooldown(0);
                Entity damager = ((EntityDamageByEntityEvent) event).getDamager();
                //cant attractive target can't damage npc
                if (((MobNPC) entity).getCantAttractiveTarget().containsKey(damager)) {
                    event.setCancelled();
                }
                //defense
                float damage = event.getFinalDamage();
                damage = (float) ((MobNPC) entity).readEntityParameters(((MobNPC) entity).getDefenseformula().replaceAll("source\\.damage", damage + ""));
                event.setDamage(damage);
                //defense
                //checkbedamagedcd
                onCheckCanEntityAttack((EntityDamageByEntityEvent) event);
                //checkbedamagedcd

                //shield
                if (onCheckShield(((MobNPC) entity))){
                    if (!checkShieldWillHurtHealth(((MobNPC) entity))){
                        event.setCancelled();
                    }
                    onReduceShield(((MobNPC) entity),event);
                }
                //shield
                //cant attractive target can't damage npc
                if (!event.isCancelled()) {
                    ConcurrentHashMap<Entity, Float> damagepool = ((MobNPC) entity).getDamagePool();
                    ConcurrentHashMap<Entity, Float> hatepool = ((MobNPC) entity).getHatePool();
                    //
                    if (damagepool.containsKey(damager)) {
                        damagepool.put(damager, damagepool.get(damager) + event.getDamage());
                    } else {
                        damagepool.put(damager, event.getDamage());
                    }
                    if (hatepool.containsKey(damager)) {
                        hatepool.put(damager, hatepool.get(damager) + event.getDamage());
                    } else {
                        hatepool.put(damager, event.getDamage());
                    }
                    //
                    ((MobNPC) entity).setHatePool(hatepool);
                    ((MobNPC) entity).setDamagePool(damagepool);
                    KnockBackNPC((MobNPC) entity,(EntityDamageByEntityEvent) event);
                }
            }
            if (event.getCause() == EntityDamageEvent.DamageCause.MAGIC){
                event.setAttackCooldown(0);
                //prevent mob's in poison or some effect that player can't hurt mobnpc.
            }
        }
    }

    public boolean onCheckShield(MobNPC mobNPC){
        if (mobNPC.status.containsKey("Shield")){
            float value = Float.parseFloat(((ConcurrentHashMap)mobNPC.status.get("Shield")).get("Value").toString());
            return true;
        }else{
            return false;
        }
    }

    public static void onReduceShield(MobNPC mobNPC,EntityDamageEvent event){
        float value = Float.parseFloat(((ConcurrentHashMap)mobNPC.status.get("Shield")).get("Value").toString());
        ((ConcurrentHashMap)mobNPC.status.get("Shield")).put("Value",value-event.getFinalDamage());
        value = Float.parseFloat(((ConcurrentHashMap)mobNPC.status.get("Shield")).get("Value").toString());
        if (value<=0){
            int willStopActTick = Integer.parseInt(((ConcurrentHashMap)mobNPC.status.get("Shield")).get("willStopAct").toString());
            mobNPC.status.put("Stop",willStopActTick);
            mobNPC.status.put("ShieldBreak",((ConcurrentHashMap)mobNPC.status.get("Shield")).get("shieldBreak"));
            mobNPC.status.remove("Shield");
        }
    }

    public boolean checkShieldWillHurtHealth(MobNPC mobNPC){
        return Boolean.parseBoolean((((ConcurrentHashMap)mobNPC.status.get("Shield")).get("canHurtHealth").toString()));
    }

    public void KnockBackNPC(MobNPC npc,EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (npc.getCanbeknockback()) {
            double frontYaw = ((damager.yaw + 90.0D) * Math.PI) / 180.0D;
            double frontX = event.getKnockBack() * 5 * Math.cos(frontYaw);
            double frontZ = event.getKnockBack() * 5 * Math.sin(frontYaw);
            double frontY = event.getKnockBack() * 2;
            npc.setMotion(new Vector3(frontX, frontY, frontZ));
        }else{
            event.setKnockBack(0);
        }
    }

    public void onCheckCanEntityAttack(EntityDamageByEntityEvent event){
        Entity damager = event.getDamager();
        Entity npc = event.getEntity();
        if (npc instanceof MobNPC) {
            ConcurrentHashMap<Entity, Integer> bedamagedcd = ((MobNPC) npc).getBedamageCD();
            if (!bedamagedcd.containsKey(damager)){
                bedamagedcd.put(damager,((MobNPC) npc).getBedamageddelay());
                ((MobNPC) npc).setBedamageCD(bedamagedcd);
            }else{
                event.setCancelled();
            }
        }
    }


    @EventHandler
    public void onKilled(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof MobNPC) {
            List<String> deathcommands = ((MobNPC) entity).getDeathcommands();
            commands(deathcommands, (MobNPC) entity);
            ((MobNPC) entity).Drop();
        }
    }

    public void commands(List<String> deathcommands, MobNPC npc) {
        for (String probAndCommands : deathcommands) {
            int probability = Integer.parseInt(probAndCommands.split(":")[0]);
            if (new Random().nextInt(101) <= probability) {
                String commands = probAndCommands.split(":")[1];
                if (commands.contains("||")) {
                    String[] commandgroup = commands.split("\\|\\|");
                    String command = commandgroup[new Random().nextInt(commandgroup.length)];
                    if (command.contains("&&")) {
                        for (String c : command.split("&&")) {
                            runcommand(c, npc);
                        }
                    }else{
                        runcommand(command, npc);
                    }
                }else{
                    if (commands.contains("&&")) {
                        for (String command : commands.split("&&")) {
                            runcommand(command, npc);
                        }
                    }else{
                        runcommand(commands, npc);
                    }
                }
            }
        }
    }

    public void runcommand(String command, MobNPC npc) {
        String finalCommand = command;
        List<Entity> damagers = NPC.getMaxValueList(npc.getDamagePool());
        damagers.removeIf(entity -> !(entity instanceof Player));
        List<Entity> haters = NPC.getMaxValueList(npc.getHatePool());
        haters.removeIf(entity -> !(entity instanceof Player));
        ConsoleCommandSender sender = Server.getInstance().getConsoleSender();
        boolean isMulti = false;

        //damager
        if (!damagers.isEmpty()) {
            Collections.shuffle(damagers);
            if (Server.getInstance().getPlayer(damagers.get(0).getName()) != null) {
                finalCommand = finalCommand.replaceAll("\\{damager\\.name}", damagers.get(0).getName());
            } else {
                return;
            }
        } else {
            return;
        }
        //damager

        //hater
        if (!haters.isEmpty()) {
            Collections.shuffle(haters);
            if (Server.getInstance().getPlayer(haters.get(0).getName()) != null) {
                finalCommand = finalCommand.replaceAll("\\{hater\\.name}", haters.get(0).getName());
            } else {
                return;
            }
        } else {
            return;
        }
        //hater

        if (finalCommand.contains("{killer.")) {
            if (npc.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
                Entity killer = ((EntityDamageByEntityEvent) npc.getLastDamageCause()).getDamager();
                if (killer instanceof Player) {
                    finalCommand = finalCommand.replaceAll("\\{killer\\.name}", killer.getName());
                } else {
                    return;
                }
            } else {
                return;
            }
        }
        if (command.contains("all.")) {
            isMulti = true;
        }
        //runCommand part
        if (isMulti) {
            if (finalCommand.contains("{all.damagers.name}")) {
                for (Entity player : npc.getDamagePool().keySet()) {
                    if (player instanceof Player) {
                        Server.getInstance().dispatchCommand(sender, finalCommand.replaceAll("\\{all\\.damagers\\.name}", player.getName()));
                    }
                }
            }
            if (finalCommand.contains("{all.haters.name}")) {
                for (Entity player : npc.getHatePool().keySet()) {
                    if (player instanceof Player) {
                        Server.getInstance().dispatchCommand(sender, finalCommand.replaceAll("\\{all\\.haters\\.name}", player.getName()));
                    }
                }
            }
        }else{
            Server.getInstance().dispatchCommand(sender, finalCommand);
        }
    }
    @EventHandler
    public void onDamage(EntityDamageByChildEntityEvent event){
        Entity bullet = event.getChild();
        if (bullet instanceof Bullet){
            event.setKnockBack(((Bullet) bullet).knockback);
            event.setDamage(((Bullet) bullet).damage);
        }
    }
    @EventHandler
    public void onUnChunk(ChunkUnloadEvent paramChunkUnloadEvent) {
        for (Map.Entry localEntry : paramChunkUnloadEvent.getChunk().getEntities().entrySet()) {
            Entity localEntity = (Entity)localEntry.getValue();
            if (localEntity instanceof NPC) {
                paramChunkUnloadEvent.setCancelled(true);
            }
        }
    }
}
