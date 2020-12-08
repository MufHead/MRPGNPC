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
import cn.nukkit.network.protocol.AnimatePacket;
import cn.nukkit.network.protocol.PlayerActionPacket;
import cn.nukkit.network.protocol.PlayerSkinPacket;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import com.muffinhead.MRPGNPC.Effects.Lightning;
import com.muffinhead.MRPGNPC.MRPGNPC;

import java.awt.im.InputContext;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
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
        healthSkillRun();
        skillTickUpdate();
        noHateHeal();
        SkillDelayUpdate();
        return super.entityBaseTick(tickDiff);
    }

    public void SkillDelayUpdate(){
        SkillDelay--;
    }
    ////////SkillsRun
    public void tickSkillRun(){
        List<String> tickSkill = GetNPCSkills(this,"onTick");
        for (String skillandcondition:tickSkill){
            String condition = skillandcondition.split(":")[1];
            String skill = skillandcondition.split(":")[2];
            if (!skillTick.containsKey(condition+":"+skill)) {
                skillTick.put(condition+":"+skill, 0);
            }
            if (skillTick.get(condition+":"+skill) <= 0) {
                readSkill(skill);
                skillTick.put(condition+":"+skill, Integer.parseInt(condition.split("~")[1]));
            }
        }
    }
    public void damageSkillRun(){
        List<String> damageSkill = GetNPCSkills(this,"onDamage");
        for (String skillandcondition:damageSkill){
            String condition = skillandcondition.split(":")[1];
            String skill = skillandcondition.split(":")[2];
            readSkill(skill);
        }
    }
    public void beDamagedSkillRun(){
        List<String> beDamagedSkill = GetNPCSkills(this,"onBeDamaged");
        for (String skillandcondition:beDamagedSkill){
            String condition = skillandcondition.split(":")[1];
            String skill = skillandcondition.split(":")[2];
            readSkill(skill);
        }
    }
    public void attackingSkillRun(){
        List<String> attackingSkill = GetNPCSkills(this,"onAttack");
        for (String skillandcondition:attackingSkill){
            String condition = skillandcondition.split(":")[1];
            String skill = skillandcondition.split(":")[2];
            readSkill(skill);
        }
    }
    public void healthSkillRun() {
        List<String> healthSkill = GetNPCSkills(this, "onHealth");
        for (String skillandcondition : healthSkill) {
            String condition = skillandcondition.split(":")[1];
            String skill = skillandcondition.split(":")[2];
            double health = Double.parseDouble(getReplacedNumber(condition));
            String symbol = getReplacedText(condition.split("~")[1]);
            switch (symbol) {
                case "＜":
                case "<":
                    if (this.getHealth() < health) {
                        readSkill(skill);
                        this.getSkills().remove(skillandcondition);
                    }
                    break;
                case "＜=":
                case "<=":
                    if (this.getHealth() <= health) {
                        readSkill(skill);
                        this.getSkills().remove(skillandcondition);
                    }
                    break;
                case "＞":
                case ">":
                    if (this.getHealth() > health) {
                        readSkill(skill);
                        this.getSkills().remove(skillandcondition);
                    }
                    break;
                case "＞=":
                case ">=":
                    if (this.getHealth() >= health) {
                        readSkill(skill);
                        this.getSkills().remove(skillandcondition);
                    }
                    break;
            }
        }
    }
    ////////SkillsRun
    public void skillTickUpdate(){
        if (this.target != null) {
            for (String s : skillTick.keySet()) {
                if (skillTick.get(s) > 0) {
                    skillTick.put(s, skillTick.get(s) - 1);
                }else{
                    skillTick.put(skinname,0);
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
                        finalskills.add(prob+":"+condition+":"+skill);
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
                beDamagedSkillRun();
            }
        }
        if (source instanceof EntityDamageByEntityEvent){
            if (((EntityDamageByEntityEvent) source).getDamager()==this){
                damageSkillRun();
            }
        }
        return super.attack(source);
    }

    @Override
    public void attackEntity(Entity target) {
        super.attackEntity(target);
        attackingSkillRun();
    }

    public void readSkill(String configname) {
        Config config = MRPGNPC.skillconfigs.get(configname);
        if (config!=null) {
            List<String> skillList = config.getList("Skills");
            if (skillList != null) {
                for (String skill : skillList) {
                    String[] info = skill.split(":");
                    MobNPC mob = this;
                    if (info.length == 2) {
                        if (info[1].contains("lastDamager")||info[1].contains("damager.name")) {
                            getServer().getScheduler().scheduleDelayedTask(new Task() {
                                @Override
                                public void onRun(int i) {
                                    setReadSkill(info, mob);
                                }
                            }, 1);
                        } else {
                            setReadSkill(info, mob);
                        }
                    } else if (info.length >= 3) {
                        if (info[1].contains("lastDamager")||info[2].contains("damager.name")){
                            getServer().getScheduler().scheduleDelayedTask(new Task() {
                                @Override
                                public void onRun(int i) {
                                    setReadSkill(info, mob);
                                }
                            }, 1);
                        }
                    } else {
                        setReadSkill(info, mob);
                    }
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
                case "ChangeItemInHand": {
                    int id = parseInt(s[1]);
                    int damage = parseInt(s[2]);
                    boolean enchant = getBoolean(s[3]);
                    Item item = Item.get(id, damage);
                    if (enchant) {
                        item.addEnchantment(Enchantment.get(17));
                    }
                    mob.getInventory().setItemInHand(item);
                    break;
                }
                case "ChangeAttackRange": {
                    mob.setAttackrange(readEntityParameters(s[1]));
                    break;
                }
                case "RefreshAttackDelay": {
                    mob.attackdelay = attackTime;
                    break;
                }
                case "ChangeSkin": {
                    String skinname = s[1];
                    mob.setSkin(MRPGNPC.skins.get(skinname));
                    sendSkinChangePacket(mob);
                    break;
                }
                case "RunCommand": {
                    if (s[1].contains("damager.name")) {
                        getServer().getScheduler().scheduleDelayedTask(new Task() {
                            @Override
                            public void onRun(int i) {
                                String cmd = recoverString(s[1]);
                                Server.getInstance().dispatchCommand(getServer().getConsoleSender(), cmd);
                            }
                        },1);
                    }
                    break;
                }
                case "Lightning": {
                    List<Entity> entities = this.getTargets(s[1].split("-"));
                    for (Entity entity:entities) {
                        if (!(entity == null)) {
                            Lightning lightning = new Lightning(mob.chunk, Lightning.getDefaultNBT(entity));
                            lightning.attack(0);
                            lightning.spawnToAll();
                        }
                    }
                    break;
                }
                case "ChangeDamage": {
                    double strength = readEntityParameters(s[1]);
                    mob.setDamage((float) strength);
                    break;
                }
                case "ChangeSize": {
                    double size = readEntityParameters(s[1]);
                    mob.setScale((float) size);
                    break;
                }
                case "ChangeKnockback": {
                    double knockback = readEntityParameters(s[1]);
                    mob.setKnockback((float) knockback);
                    break;
                }
                case "ChangeMovementSpeed": {
                    double speed = readEntityParameters(s[1]);
                    mob.setSpeed(speed);
                    break;
                }
                case "ChangeAttackdDelay": {
                    int speed = Integer.parseInt(s[1]);
                    mob.setAttackdelay(speed);
                    break;
                }
                case "InsertSkill": {
                    List<String> skills = mob.getSkills();
                    String skill = "";
                    for (int i = 1;i<s.length;i++){
                        skill = skill+s[i];
                    }
                    skills.add(skill);
                    mob.setSkills(skills);
                    break;
                }
                case "RemoveSkill": {
                    List<String> skills = mob.getSkills();
                    String skill = "";
                    for (int i = 1;i<s.length;i++){
                        skill = skill+s[i];
                    }
                    skills.remove(skill);
                    mob.setSkills(skills);
                    break;
                }
                case "Heal": {
                    double amount = readEntityParameters(s[1]);
                    this.heal((float) amount);
                    break;
                }
                case "Action":{
                    broadcastEntityEvent(Integer.parseInt(s[1]));
                    break;
                }
                /*
                case "SummonMob": {
                    Config config = MRPGNPC.mobconfigs.get(s[1]);
                    List<Double> XYZ = new ArrayList<>();
                    XYZ.add(readEntityParameters(s[2]));
                    XYZ.add(readEntityParameters(s[3]));
                    XYZ.add(readEntityParameters(s[4]));
                    MRPGNPC.mrpgnpc.spawnNPC(config, "", s[1], );
                    break;
                }

                */
                /*
                case "SectorAttack":{
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
    public static void sendSkinChangePacket(EntityHuman player) {
        PlayerSkinPacket pk = new PlayerSkinPacket();
        pk.newSkinName = "new";
        pk.oldSkinName = "old";
        pk.uuid = player.getUniqueId();
        pk.skin = player.getSkin();
        for (Map.Entry<UUID, Player> entry : Server.getInstance().getOnlinePlayers().entrySet()) {
            entry.getValue().dataPacket(pk);
        }
        //Server.getInstance().updatePlayerListData(player.getUniqueId(), player.getId(), player.getName(), player.getSkin(), player.getRawUniqueId());
    }
    public static void sendSkinChangePacket(EntityHuman entityHuman,Player player) {
        PlayerSkinPacket pk = new PlayerSkinPacket();
        pk.newSkinName = "new";
        pk.oldSkinName = "old";
        pk.uuid = entityHuman.getUniqueId();
        pk.skin = entityHuman.getSkin();
        player.dataPacket(pk);
        //Server.getInstance().updatePlayerListData(player.getUniqueId(), player.getId(), player.getName(), player.getSkin(), player.getRawUniqueId());
    }
    public static String getReplacedNumber(String num) {
        num = num.replaceAll("[^\\d.]", "");
        return num;
    }
    public static String getReplacedText(String num) {
        num = num.replaceAll("\\d+","").replaceAll("\\.","");
        return num;
    }
}
