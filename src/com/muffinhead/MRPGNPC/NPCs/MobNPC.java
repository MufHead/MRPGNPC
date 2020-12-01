package com.muffinhead.MRPGNPC.NPCs;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.EntityHumanType;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.inventory.InventoryHolder;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.ExplodeParticle;
import cn.nukkit.level.particle.HugeExplodeSeedParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import com.muffinhead.MRPGNPC.Effects.Lightning;
import com.muffinhead.MRPGNPC.MRPGNPC;

import java.awt.im.InputContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.Boolean.getBoolean;
import static java.lang.Integer.parseInt;

public class MobNPC extends NPC{
    public MobNPC(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    public int SkillDelay = 0;

    public ConcurrentHashMap<String, Integer> skillTick = new ConcurrentHashMap<>();

    @Override
    public boolean entityBaseTick(int tickDiff) {
        checkTargetCanBeChoose();
        checkPlayerIsAttractive();
        updateDisplayName();
        bedamagedcdCheck();
        if (target == null) {
            this.target = this.getTarget();
        }
        onMove();
        if (this.target != null) {
            attackEntity(this.target);
        }
        tickSkillRun();
        skillTickUpdate();
        noHateHeal();
        SkillDelayUpdate();
        return super.entityBaseTick(tickDiff);
    }

    public void SkillDelayUpdate(){
        SkillDelay--;
    }

    public void tickSkillRun(){
        List<String> tickSkill = GetNPCSkills(this,"onTick");
        for (String skillandcondition:tickSkill){
            String condition = skillandcondition.split(":")[0];
            String skill = skillandcondition.split(":")[1];
            if (!skillTick.containsKey(condition+":"+skill)) {
                skillTick.put(condition+":"+skill, 0);
            }
            if (skillTick.get(condition+":"+skill) <= 0) {
                readSkill(skill);
                skillTick.put(condition+":"+skill, Integer.parseInt(condition.split("~")[1]));
            }
        }
    }

    public void skillTickUpdate(){
        if (this.target != null) {
            for (String s : skillTick.keySet()) {
                if (skillTick.get(s) > 0) {
                    skillTick.put(s, skillTick.get(s) - 1);
                }
            }
        }
    }

    public void noHateHeal(){
        String[] lbhealthing = nohatesheal.split(":");
        if (this.nhhealtick >= Integer.parseInt(lbhealthing[0])) {
            this.heal((float) Double.parseDouble(lbhealthing[1]));
        }
        if (this.target != null) {
            nhhealtick = 0;
        } else {
            nhhealtick++;
        }
    }


    public List<String> GetNPCSkills(MobNPC npc,String type){
        List<String> skills = npc.getSkills();
        List<String> finalskills = new ArrayList<>();
        if (skills!=null){
            for (String skillandcondition:skills){
                String prob = skillandcondition.split(":")[0];
                String condition = skillandcondition.split(":")[1];
                String skill = skillandcondition.split(":")[2];
                if (new Random().nextInt(100)+1<=Integer.parseInt(prob)) {
                    if (condition.contains(type)){
                        finalskills.add(condition+":"+skill);
                    }
                }
            }
        }
        return finalskills;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        if (source.getEntity() == this){
            if (!source.isCancelled()){
                List<String> beDamagedSkill = GetNPCSkills(this,"onBeDamaged");
                for (String skillandcondition:beDamagedSkill){
                    String condition = skillandcondition.split(":")[0];
                    String skill = skillandcondition.split(":")[1];
                    readSkill(skill);
                }
            }
        }
        return super.attack(source);
    }

    public void readSkill(String configname) {
        Config config = MRPGNPC.skillconfigs.get(configname);
        if (config!=null) {
            List<String> skillList = config.getList("Skills");
            if (skillList != null) {
                for (String skill : skillList) {
                    String[] info = skill.split(":");
                    MobNPC mob = this;
                    setReadSkill(info, mob);
                }
            }
        }
    }

    public void setReadSkill(String[] s, MobNPC mob) {
        if (this.SkillDelay<=0) {
            switch (s[0]) {
                case "Delay": {
                    int tick = parseInt(s[1]);
                    MobNPC.this.SkillDelay = tick;
                    break;
                }
                case "Damage": {
                    List<Entity> entities = MobNPC.this.getTargets(s[1].split("-"));
                    for (Entity entity:entities) {
                        if (entity != null) {
                            HashMap<EntityDamageEvent.DamageModifier, Float> damage = new HashMap<>();
                            damage.put(EntityDamageEvent.DamageModifier.BASE, (float) readEntityParameters(s[2]));
                            HashMap<Integer, Float> armorValues = new Monster.ArmorPoints();

                            float points = 0;
                            if (entity instanceof EntityHumanType) {
                                for (Item i : ((EntityHumanType) entity).getInventory().getArmorContents()) {
                                    points += armorValues.getOrDefault(i.getId(), 0f);

                                    damage.put(EntityDamageEvent.DamageModifier.ARMOR,
                                            (float) (damage.getOrDefault(EntityDamageEvent.DamageModifier.ARMOR, 0f) - Math.floor(damage.getOrDefault(EntityDamageEvent.DamageModifier.BASE, 1f) * points * 0.04)));
                                }
                                EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(mob, entity, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage);
                                event.setKnockBack(0);
                                entity.attack(event);
                            }
                        }
                    }
                    break;
                }
                case "BlowUp": {
                    List<Entity> entities = MobNPC.this.getTargets(s[1].split("-"));
                    for (Entity entity : entities) {
                        if (!(entity == null)) {
                            double strength = readEntityParameters(s[2]);
                            Vector3 vector3 = entity.subtract(mob.getPosition()).normalize();
                            if (vector3.x == 0) {
                                vector3.x = 0.1;
                            }

                            if (vector3.z == 0) {
                                vector3.z = 0.1;
                            }
                            vector3.y = strength * 0.23D;
                            double impact = strength;
                            entity.setMotion(vector3.multiply(impact));
                        }
                    }
                    break;
                }
                case "Burn": {
                    int tick = (int) readEntityParameters(s[2]);
                    List<Entity> entities = MobNPC.this.getTargets(s[1].split("-"));
                    for (Entity entity : entities) {
                        if (!(entity == null)) {
                            entity.fireTicks = tick;
                        }
                    }
                    break;
                }
                case "Message": {
                    List<Entity> entities = MobNPC.this.getTargets(s[1].split("-"));
                    String mes = s[2];
                    for (Entity entity : entities) {
                        if (!(entity == null)) {
                            if (entity instanceof Player) {
                                mes = recoverString(mes);
                                ((Player) entity).sendMessage(mes);
                            }
                        }
                    }
                    break;
                }
                case "HugeExplode":{
                    double x = readEntityParameters(s[1]);
                    double y = readEntityParameters(s[2]);
                    double z = readEntityParameters(s[3]);
                    Vector3 vector3 = new Vector3(x,y,z);
                    List<Player> players = new ArrayList<>();
                    this.level.addParticle(new ExplodeParticle(vector3),players);
                    this.level.addParticle(new HugeExplodeSeedParticle(vector3),players);
                    this.level.addLevelSoundEvent(vector3, 48);
                    break;
                }
                case "Dash": {
                    this.setMotion(new Vector3(readEntityParameters(s[1]),readEntityParameters(s[2]),readEntityParameters(s[3])));
                    break;
                }
                case "Effect": {
                    List<Entity> entities = this.getTargets(s[1].split("-"));
                    int id = Integer.parseInt(s[2]);
                    int time = (int) readEntityParameters(s[3]);
                    int level = (int) readEntityParameters(s[4]);
                    for (Entity entity : entities) {
                        Effect effect = Effect.getEffect(id);
                        effect.setAmplifier(level);
                        effect.setDuration(time);
                        entity.addEffect(effect);
                    }
                    break;
                }
                /*
                case "切换手持物品": {
                    int id = parseInt(s[1]);
                    int damage = parseInt(s[2]);
                    boolean fumo = getBoolean(s[3]);
                    Item item = Item.get(id, damage);
                    if (fumo)
                        item.addEnchantment(Enchantment.get(17));
                    mob.getInventory().setItemInHand(item);
                    break;
                }
                case "更改攻击范围": {
                    mob.setAttackrange(readEntityParameters(s[1]));
                    break;
                }
                case "刷新攻击间隔": {
                    mob.attackdelay = attackTime;
                    break;
                }
                /*
                case "更换皮肤": {
                    String skinname = s[1];
                    respawnMobNPC(mob, skinname, "geometry." + s[1]);
                    break;
                }
                case "执行命令": {
                    String cmd = recoverString(s[1]);
                    Server.getInstance().dispatchCommand(getServer().getConsoleSender(), cmd);
                    break;
                }
                case "群体雷击": {
                    double distance = readEntityParameters(s[1]);
                    for (Player p : getLevel().getPlayers().values()) {
                        if (p.distance(mob) <= distance) {
                            if (hasSpawned.containsValue(p)) {
                                if (p.level==this.level) {
                                    Lightning lightning = new Lightning(mob.chunk, Lightning.getDefaultNBT(p));
                                    lightning.spawnToAll();
                                }
                            }
                        }
                    }
                    break;
                }
                case "单体雷击": {
                    Player player = getTarget(s[1]);
                    if (!(player == null)) {
                        Lightning lightning = new Lightning(mob.chunk, Lightning.getDefaultNBT(player));
                        lightning.attack(0);
                        lightning.spawnToAll();
                    }
                    break;
                }
                case "变更伤害": {
                    double strength = readEntityParameters(s[1]);
                    mob.damage = (float) strength;
                    break;
                }
                case "变更大小": {
                    double size = readEntityParameters(s[1]);
                    setScale((float) size);
                    break;
                }
                case "变更击退": {
                    double knockback = readEntityParameters(s[1]);
                    mob.knockback = (float) knockback;
                    break;
                }
                case "变更移速": {
                    double speed = readEntityParameters(s[1]);
                    mob.speed = speed;
                    break;
                }
                case "变更攻速": {
                    int speed = Integer.parseInt(s[1]);
                    mob.setAttackdelay(speed);
                    break;
                }
                case "插入技能": {
                    List<String> skills = mob.getSkills();
                    String skill = "";
                    for (int i = 1;i<s.length;i++){
                        skill = skill+s[i];
                    }
                    skills.add(skill);
                    mob.setSkills(skills);
                    break;
                }
                case "删除技能": {
                    List<String> skills = mob.getSkills();
                    String skill = "";
                    for (int i = 1;i<s.length;i++){
                        skill = skill+s[i];
                    }
                    skills.remove(skill);
                    mob.setSkills(skills);
                    break;
                }
                case "召唤怪物": {
                    Config config = Main.mobconfigs.get(s[1]);
                    List<Double> XYZ = new ArrayList<>();
                    XYZ.add(readPlayerParameters(s[2]));
                    XYZ.add(readPlayerParameters(s[3]));
                    XYZ.add(readPlayerParameters(s[4]));
                    Main.spawnMobNPC(config, "", s[1], XYZ, mob.level, mob.getChunkX(), mob.getChunkZ(), true, mob.isFB, llspawnPlayer, roomName);
                    break;
                }

                case "回复血量": {
                    double amount = readEntityParameters(s[1]);
                    this.heal((float) amount);
                    break;
                }
                */
                /*
                case "扇形攻击":{
                    for (Entity entity:getLevel().getEntities()){
                        // if (LateUpdate(entity.getLocation(),this.getLocation())){

                        //  }
                    }
                    break;
                }

                 */
            }
        }else{
            getServer().getScheduler().scheduleDelayedTask(new Task() {
                @Override
                public void onRun(int i) {
                    setReadSkill(s,mob);
                }
            },SkillDelay);
        }
    }
}
