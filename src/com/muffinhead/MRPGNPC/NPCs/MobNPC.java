package com.muffinhead.MRPGNPC.NPCs;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.CommandSender;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.EntityHumanType;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Location;
import cn.nukkit.level.ParticleEffect;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.ExplodeParticle;
import cn.nukkit.level.particle.HugeExplodeSeedParticle;
import cn.nukkit.math.Vector3;
import cn.nukkit.math.Vector3f;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.PlayerSkinPacket;
import cn.nukkit.network.protocol.SpawnParticleEffectPacket;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import com.muffinhead.MRPGNPC.Effects.Bullet;
import com.muffinhead.MRPGNPC.Effects.Lightning;
import com.muffinhead.MRPGNPC.MRPGNPC;

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

    public boolean isStop() {
        if (!status.containsKey("Stop")) {
            return false;
        }else{
            status.put("Stop",Integer.parseInt(status.get("Stop").toString())-1);
            if (Integer.parseInt(status.get("Stop").toString())<=0){
                status.remove("Stop");
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        if (!isStop()) {
            if (target == null) {
                this.target = this.getTarget();
            }
            onMove();
            if (this.target != null) {
                attackEntity(this.target);
            }
        }
        tickSkillRun();
        healthSkillRun();
        skillTickUpdate();
        noHateHeal();
        SkillDelayUpdate();
        checkTargetCanBeChoose();
        checkPlayerIsAttractive();
        updateDisplayName();
        bedamagedcdCheck();
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
                skillTick.put(condition+":"+skill, Integer.parseInt(condition.split("~")[1]));
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
                        skills.remove(skillandcondition);
                    }
                    break;
                case "＜=":
                case "<=":
                    if (this.getHealth() <= health) {
                        readSkill(skill);
                        skills.remove(skillandcondition);
                    }
                    break;
                case "＞":
                case ">":
                    if (this.getHealth() > health) {
                        readSkill(skill);
                        skills.remove(skillandcondition);
                    }
                    break;
                case "＞=":
                case ">=":
                    if (this.getHealth() >= health) {
                        readSkill(skill);
                        skills.remove(skillandcondition);
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
        try {
            String[] lbhealthing = nohatesheal.split(":");
            if (this.nhhealtick >= Integer.parseInt(lbhealthing[0])) {
                this.heal((float) Double.parseDouble(lbhealthing[1]));
            }
            if (this.target != null) {
                nhhealtick = 0;
            } else {
                nhhealtick++;
            }
        } catch (NumberFormatException ignored) {

        }
    }


    public List<String> GetNPCSkills(MobNPC npc,String type){
        List<String> finalskills = new ArrayList<>();
        if (npc.skills!=null){
            for (String skillandcondition:npc.skills){
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
        if (!isStop()) {
            if (source.getEntity() == this) {
                if (!source.isCancelled()) {
                    beDamagedSkillRun();
                }
            }
            if (source instanceof EntityDamageByEntityEvent) {
                if (((EntityDamageByEntityEvent) source).getDamager() == this) {
                    damageSkillRun();
                }
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
                        }else{
                            setReadSkill(info, mob);
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
                case "Title": {
                    List<Entity> entities = MobNPC.this.getTargets(s[1].split("-"));
                    String title = s[2];
                    String subTitle = s[3];
                    for (Entity entity : entities) {
                        if (!(entity == null)) {
                            if (entity instanceof Player) {
                                title = recoverString(title);
                                subTitle = recoverString(subTitle);
                                ((Player) entity).sendTitle(title,subTitle);
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
                case "setAttackDelay": {
                    mob.attackdelay = Integer.parseInt(s[1]);
                    break;
                }
                case "ChangeSkin": {
                    String skinname = s[1];
                    mob.setSkin(MRPGNPC.skins.get(skinname));
                    sendSkinChangePacket(mob);
                    break;
                }
                case "RunCommand": {
                    if (s[1].equalsIgnoreCase("true")) {
                        getServer().getScheduler().scheduleDelayedTask(new Task() {
                            @Override
                            public void onRun(int i) {
                                String cmd = recoverString(s[2]);
                                CommandSender sender = MRPGNPC.mrpgnpc.getServer().getConsoleSender();
                                if (mob.getLastDamageCause() instanceof EntityDamageByEntityEvent){
                                    if (((EntityDamageByEntityEvent) mob.getLastDamageCause()).getDamager() instanceof CommandSender) {
                                        sender = (CommandSender) ((EntityDamageByEntityEvent) mob.getLastDamageCause()).getDamager();
                                    }
                                }
                                Server.getInstance().dispatchCommand(sender, cmd);
                            }
                        }, 1);
                    }else{
                        String cmd = recoverString(s[2]);
                        CommandSender sender = MRPGNPC.mrpgnpc.getServer().getConsoleSender();
                        Server.getInstance().dispatchCommand(sender, cmd);
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
                case "ChangeDefenseFormula": {
                    String defenseFormula = s[1];
                    this.defenseformula = defenseFormula;
                    break;
                }
                case "InsertSkill": {
                    String skill = "";
                    for (int i = 1;i<s.length;i++){
                        skill = skill+s[i];
                    }
                    skills.add(skill);
                    mob.setSkills(skills);
                    break;
                }
                case "RemoveSkill": {
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
                case "CheckStatus":{
                    String statusName = s[1];
                    String skillName1 = s[2];
                    String skillName2 = s[3];
                    if (status.containsKey(statusName)){
                        if (!skillName1.equals("")) {
                            readSkill(skillName1);
                        }
                    }else{
                        if (!skillName2.equals("")) {
                            readSkill(skillName2);
                        }
                    }
                    break;
                }
                case "Shoot":{
                    //entityId-startPosX-startPosY-startPosZ-motionX-motionY-motionZ-bulletDamage-bulletKnockback-speed-maxDistance-bulletSize
                    //-Math.sin(npc.yaw / 180.0D * 3.14) * Math.cos(npc.pitch / 180.0D * 3.14)
                    //-Math.sin(npc.pitch / 180.0D * 3.14)
                    // Math.cos(npc.yaw / 180.0D * 3.14) * Math.cos(npc.pitch / 180.0D * 3.14)
                    Vector3 startpos = new Vector3(readEntityParameters(s[2]),readEntityParameters(s[3]),readEntityParameters(s[4]));
                    int entityId = Integer.parseInt(s[1]);
                    Vector3 motion = new Vector3(readEntityParameters(s[5]),readEntityParameters(s[6]),readEntityParameters(s[7])).multiply(Double.parseDouble(s[10]));
                    Bullet bullet = new Bullet(this.getChunk(),Bullet.getDefaultNBT(startpos),mob,entityId,motion,mob.yaw,mob.pitch);
                    bullet.damage = (float) readEntityParameters(s[8]);
                    bullet.knockback = (float) readEntityParameters(s[9]);
                    bullet.MaxDistance = readEntityParameters(s[11]);
                    if (s.length>=13){
                        bullet.scale = (float) readEntityParameters(s[12]);
                    }
                    bullet.spawnToAll();
                    break;
                }
                case "SetSpawn":{
                    this.spawnPosition = new Location(Double.parseDouble(s[1]),Double.parseDouble(s[2]),Double.parseDouble(s[3]),Double.parseDouble(s[4]),Double.parseDouble(s[5]),getServer().getLevelByName(s[6]));
                    break;
                }
                case "Shield":{
                    //amount-canHurtHealth-willStopAct-shieldBreakStatusTick
                    ConcurrentHashMap<String,Object> shield = new ConcurrentHashMap<>();
                    shield.put("Value",Float.parseFloat(s[1]));
                    shield.put("canHurtHealth",s[2]);
                    shield.put("willStopAct",s[3]);
                    shield.put("shieldBreak",s[4]);
                    status.put("Shield",shield);
                    break;
                }
                case "TornadoParticle":{
                    List<Entity> entities = MobNPC.this.getTargets(s[1].split("-"));
                    String identifier = s[2];
                    double turns = Double.parseDouble(s[3]);
                    double startY = readEntityParameters(s[4]);
                    double ys = readEntityParameters(s[5]);
                    double addRadius = readEntityParameters(s[6]);
                    /*
                    for (double radius = 0, y = startY, degree = 0; degree < 360 * turns; degree+=10, y += ys, radius += addRadius) {
                        double radians = Math.toRadians(degree);
                        double x = radius * Math.cos(radians);
                        double z = radius * Math.sin(radians);
                        Vector3f vector = new Vector3f();
                        vector.x = (float) this.x;
                        vector.y = (float) this.y;
                        vector.z = (float) this.z;
                        vector.add((float) x, (float)y, (float)z);
                        SpawnParticleEffectPacket pk = new SpawnParticleEffectPacket();
                        pk.identifier = "minecraft:"+identifier;
                        pk.position = vector;
                        pk.dimensionId = this.getLevel().getDimension();
                        vector.subtract((float) x, (float)y, (float)z);
                        Server.getInstance().getScheduler().scheduleDelayedTask(MRPGNPC.mrpgnpc,new Task() {
                            @Override
                            public void onRun(int i) {
                                for (Entity entity:entities){
                                    if (entity instanceof Player){
                                        ((Player) entity).dataPacket(pk);
                                    }
                                }
                            }
                        },10);
                    }*/

                    for (int degree = 0; degree < 360; degree++) {
                        double radians = Math.toRadians(degree);
                        double x = Math.cos(radians);
                        double y = Math.sin(radians);
                        Vector3f vector = new Vector3f();
                        vector.x = (float) this.x;
                        vector.y = (float) this.y;
                        vector.z = (float) this.z;
                        vector.add((float) x, 0, (float) y);
                        SpawnParticleEffectPacket pk = new SpawnParticleEffectPacket();
                        pk.identifier = "minecraft:"+identifier;
                        pk.position = vector;
                        pk.dimensionId = this.getLevel().getDimension();
                        vector.subtract((float) x, 0, (float) y);
                        Server.getInstance().getScheduler().scheduleDelayedTask(MRPGNPC.mrpgnpc,new Task() {
                            @Override
                            public void onRun(int i) {
                                for (Entity entity:entities){
                                    if (entity instanceof Player){
                                        ((Player) entity).dataPacket(pk);
                                    }
                                }
                            }
                        },10);
                    }
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
    public static String getReplacedNumber(String num) {
        num = num.replaceAll("[^\\d.]", "");
        return num;
    }
    public static String getReplacedText(String num) {
        num = num.replaceAll("\\d+","").replaceAll("\\.","");
        return num;
    }

    @Override
    public void despawnFrom(Player player) {
        super.despawnFrom(player);
    }

    @Override
    public void close() {
        super.close();
    }
}
