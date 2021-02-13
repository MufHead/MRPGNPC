package com.muffinhead.MRPGNPC.NPCs;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockAir;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.entity.item.EntityItem;
import cn.nukkit.entity.projectile.EntityProjectile;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDeathEvent;
import cn.nukkit.event.player.PlayerDeathEvent;
import cn.nukkit.event.player.PlayerToggleSneakEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.DestroyBlockParticle;
import cn.nukkit.level.particle.Particle;
import cn.nukkit.math.*;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.AddPlayerPacket;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.network.protocol.PlayerSkinPacket;
import cn.nukkit.network.protocol.SetEntityLinkPacket;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.Task;
import com.muffinhead.MRPGNPC.MRPGNPC;
import com.muffinhead.mdungeon.DataPacketLimit;
import com.muffinhead.mdungeon.MDungeon;
import com.muffinhead.mdungeon.Room;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.awt.im.InputContext;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Integer.parseInt;


public class NPC extends EntityHuman {
    protected boolean isAttacking = false;

    protected String camp = "Example";

    protected Position spawnPosition;

    protected String geometryName = "";

    protected String mobFeature = "";

    protected String skinname = "";

    private double frontX;

    private double frontY;

    private double frontZ;

    protected double speed = 1.0;

    protected Entity target = null;

    protected double attackrange = 1.0;

    public int nhhealtick = 0;

    protected double hitrange = 0.2;

    protected double damage = 3.0;

    protected String displayName = "";

    protected double knockback = 0.1;

    protected String defenseformula = "source.damage";

    protected int attackdelay = 30;

    protected int attackdelayed = 0;

    protected int damagedelay = 0;

    protected ConcurrentHashMap<Entity, Integer> bedamageCD = new ConcurrentHashMap<>();

    protected int bedamageddelay = 0;

    protected double haterange = 15.0;

    protected String nohatesheal = "200:1.0";

    protected boolean canbeknockback = false;

    protected List<String> deathcommands = new ArrayList<>();

    protected String bedamagedblockparticle = "152:0";

    protected List<String> drops = new ArrayList<>();

    protected ConcurrentHashMap<Entity, Integer> cantAttractiveTarget = new ConcurrentHashMap<>();

    protected ConcurrentHashMap<Entity, Float> damagePool = new ConcurrentHashMap<>();

    protected ConcurrentHashMap<Entity, Float> hatePool = new ConcurrentHashMap<>();

    protected List<String> activeattackcreature = new ArrayList<>();

    protected List<String> unattractivecreature = new ArrayList<>();

    public List<String> skills = new ArrayList<>();

    protected boolean enableBox = true;

    public ConcurrentHashMap<String,Object> status = new ConcurrentHashMap<>();


    public NPC(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt.putCompound("Skin", new CompoundTag()));
        nbt
                .putString("ModelID", this.skin.getSkinId())
                .putString("ModelId", this.skin.getSkinId())
                .putString("GeometryName", this.geometryName)
                .putByteArray("GeometryData", this.skin.getGeometryData().getBytes(StandardCharsets.UTF_8));
        spawnPosition = getPosition();
//teleport to avoid invisible bug
        this.teleport(new Vector3(this.x + 0.1, this.y, this.z + 0.1));

        this.teleport(new Vector3(this.x - 0.1, this.y, this.z - 0.1));
    }

    public void setSkin(Skin skin) {
        this.skin = skin;
    }

    //Draw on the mob plugin for detection jumps
    protected boolean checkJump(double dx, double dz) {
        if (this.frontY == (getGravity() * 2.0F))
            return this.level.getBlock(new Vector3(NukkitMath.floorDouble(this.x), (int) this.y,
                    NukkitMath.floorDouble(this.z))) instanceof cn.nukkit.block.BlockLiquid;
        if (this.level.getBlock(new Vector3(NukkitMath.floorDouble(this.x), (int) (this.y + 0.8D),
                NukkitMath.floorDouble(this.z))) instanceof cn.nukkit.block.BlockLiquid) {
            this.frontY = (getGravity() * 2.0F);
            return true;
        }
        Block that = getLevel().getBlock(new Vector3(NukkitMath.floorDouble(this.x + dx), (int) this.y, NukkitMath.floorDouble(this.z + dz)));
        if (getDirection() == null)
            return false;
        Block block = that.getSide(getHorizontalFacing());
        if (!block.canPassThrough() && block.up().canPassThrough() && that.up(2).canPassThrough()) {
            if (block instanceof cn.nukkit.block.BlockFence || block instanceof cn.nukkit.block.BlockFenceGate) {
                this.frontY = getGravity();
            } else if (this.frontY <= (getGravity() * 4.0F)) {
                this.frontY = (getGravity() * 4.0F);
            } else if (block instanceof cn.nukkit.block.BlockSlab || block instanceof cn.nukkit.block.BlockStairs) {
                this.frontY = (getGravity() * 4.0F);
            } else if (this.frontY <= (getGravity() * 8.0F)) {
                this.frontY = (getGravity() * 8.0F);
            } else {
                this.frontY += getGravity() * 0.25D;
            }
            return true;
        }
        return false;
    }

    public void moveTowards(Position target) {
        double speed = this.speed;
        if (this.hasEffect(2)) {
            speed = speed - speed * (this.getEffect(2).getAmplifier() + 1) * 0.15;
        }
        if (this.hasEffect(1)) {
            speed = speed + speed * (this.getEffect(1).getAmplifier() + 1) * 0.15;
        }
        if (!this.onGround) {
            speed /= 2;
        }
        if (speed < 0) {
            speed = 0;
        }
        Double x = 0d;
        Double y = 0d;
        Double z = 0d;

        if (this.spawnPosition != null) {
            x = this.x - this.spawnPosition.x;
            y = this.y - this.spawnPosition.y;
            z = this.z - this.spawnPosition.z;
        }
        if (target != null) {
            x = this.x - target.x;
            y = this.y - target.y;
            z = this.z - target.z;
        }
        double yaw = Math.asin(x / Math.sqrt(x * x + z * z)) / Math.PI * 180.0D;
        double asin = Math.asin(y / Math.sqrt(x * x + z * z + y * y)) / Math.PI * 180.0D;
        long pitch;
        pitch = Math.round(asin);
        if (!(z > 0.0D)) {
        } else {
            yaw = -yaw + 180.0D;
        }
        double frontYaw = ((yaw + 90.0D) * Math.PI) / 180.0D;
        setRotation(yaw, pitch);


        frontX = speed * 0.15D * Math.cos(frontYaw);
        frontZ = speed * 0.15D * Math.sin(frontYaw);


        boolean isjump = true;
        isjump = checkJump(frontX, frontZ);

        if (!isjump) {
            if (this.onGround) {
                this.frontY = 0.0D;
            } else if (this.motionY > (-getGravity() * 4.0F)) {
                if (!(this.level.getBlock(new Vector3(NukkitMath.floorDouble(this.x), (int) (this.y + 0.8D), NukkitMath.floorDouble(this.z))) instanceof cn.nukkit.block.BlockLiquid)) {
                    this.frontY -= (getGravity() * 1F);
                }
            } else {
                this.frontY -= getGravity();
            }
        }
        if (target != null) {
            if (this.isAttacking) {
                this.move(0.0D, this.frontY, 0.0D);
            } else {
                if (distance((Vector3) target) < this.attackrange) {
                    this.move(0.0D, this.frontY, 0.0D);
                } else {
                    this.move(frontX, this.frontY, frontZ);
                }
            }
        }
        if (enableBox) {
            for (Entity entity : this.getLevel().getEntities()) {
                if (!(entity instanceof EntityProjectile||entity instanceof EntityItem)) {
                    if (entity instanceof MobNPC&&!((MobNPC) entity).enableBox) {

                    }else {
                        if (this.distance(entity) <= 0.4 * this.scale) {
                            double x1 = this.x;
                            double z1 = this.z;
                            double x2 = entity.x;
                            double z2 = entity.z;
                            double xx = x1 - x2;
                            double zz = z1 - z2;
                            this.frontX = xx;
                            this.frontZ = zz;
                            this.move(this.frontX, frontY, this.frontZ);
                        }
                    }
                }
            }
        }
    }

    //entity everytick do

    //displaynameupdate
    public void updateDisplayName() {
        if (this.displayName != null) {
            String name = this.displayName
                    .replace("{Maxhealth}", this.getMaxHealth() + "")
                    .replace("{Health}", this.getHealth() + "")
                    .replace("{Damage}", this.damage + "");
            this.setNameTag(name);
        }
    }

    //
    public void checkPlayerIsAttractive() {
        if (cantAttractiveTarget != null) {
            for (Entity entity : cantAttractiveTarget.keySet()) {
                if (cantAttractiveTarget.get(entity) > 0) {
                    cantAttractiveTarget.put(entity, cantAttractiveTarget.get(entity) - 1);
                } else {
                    cantAttractiveTarget.remove(entity);
                }
            }
        }
    }

    //
    public void onMove() {
        if (this.target != null) {
            if (!checkIsWallFront()) {
                moveTowards(this.target.getPosition());
            } else {
                this.hatePool.remove(target);
                cantAttractiveTarget.put(target, (int) Math.round(20 * this.distance(spawnPosition) / speed));
                this.target = null;
            }
        } else {
            this.moveTowards(this.spawnPosition);
        }
    }

    //
    public void checkTargetCanBeChoose() {
        if (this.target != null && this.target instanceof Player) {
            if (!((Player) this.target).isOnline()) {
                this.target = null;
            }
        }
        if (this.target != null && !this.target.isAlive()) {
            this.target = null;
        }
        if (this.target != null && !this.level.getName().equals(this.target.getLevel().getName())) {
            this.target = null;
        }
        if (this.target != null && this.target.distance((Vector3) this) > this.haterange) {
            this.target = null;
        }

    }

    public boolean checkIsWallFront() {
        List<Block> blocks = new ArrayList<>();
        for (Block block : blocksAround) {
            if (this.getDirection() != null) {
                switch (this.getDirection()) {
                    case NORTH: {
                        if (block.z < this.z) {
                            if (block.y + 1 > this.y) {
                                if (block.x <= this.x + 0.05 || block.x >= this.x - 0.05) {
                                    blocks.add(block);
                                }
                            }
                        }
                        break;
                    }
                    case SOUTH: {
                        if (block.z > this.z) {
                            if (block.y + 1 > this.y) {
                                if (block.x <= this.x + 0.05 || block.x >= this.x - 0.05) {
                                    blocks.add(block);
                                }
                            }
                        }
                        break;
                    }
                    case WEST: {
                        if (block.x < this.x) {
                            if (block.y + 1 > this.y) {
                                if (block.z <= this.z + 0.05 || block.z >= this.z - 0.05) {
                                    blocks.add(block);
                                }
                            }
                        }
                        break;
                    }
                    case EAST: {
                        if (block.x > this.x) {
                            if (block.y + 1 > this.y) {
                                if (block.z <= this.z + 0.05 || block.z >= this.z - 0.05) {
                                    blocks.add(block);
                                }
                            }
                        }

                        break;
                    }
                }
            }
        }
        boolean hasWall = true;
        for (Block block : blocks) {
            if (block instanceof BlockAir) {
                if (hasWall) {
                    hasWall = false;
                }
            }
        }
        if (blocks.isEmpty()) {
            hasWall = false;
        }
        return hasWall;
    }

    public static List<Entity> getMaxValueList(ConcurrentHashMap<Entity, Float> map) {
        double max = 0.0f;
        String temp = "";
        double value = 0.0f;
        List<Entity> list = new ArrayList<>();
        for (Entity entity : map.keySet()) {
            value = map.get(entity);
            if (max < value) {
                max = value;
                list.clear();
                list.add(entity);
            } else if (max == value) {
                list.add(entity);
            }
        }
        return list;
    }

    public static Entity getMaxValue(ConcurrentHashMap<Entity, Float> map) {
        double max = -100000000000000000000000000000000000000.0F;
        Entity temp = null;
        double value = 0.0D;
        for (Entity key : map.keySet()) {
            value = map.get(key);
            if (max < value) {
                max = value;
                temp = key;
            }
        }
        return temp;
    }

    public static Entity getMinValue(ConcurrentHashMap<Entity, Float> map) {
        float min = 100000000000000000000000000000000000000.0F;
        Entity temp = null;
        float value = 0.0F;
        for (Entity key : map.keySet()) {
            value = map.get(key);
            if (min > value) {
                min = value;
                temp = key;
            }
        }
        return temp;
    }

    @Override
    public boolean setMotion(Vector3 motion) {
        this.frontY += motion.getY();
        this.move(motion.x,
                frontY,
                motion.z);
        return true;
    }

    public Entity getTarget() {
        List<Entity> entities = new ArrayList<>();
        for (Entity entity : getLevel().getEntities()) {
            if (entity.distance(this) <= this.haterange) {
                if (activeattackcreature != null) {
                    simplifyTarget(entities, activeattackcreature, entity);
                }
            }
        }
        for (Entity entity : entities) {
            hatePool.put(entity, 0.0f);
        }
        ConcurrentHashMap<Entity, Float> map = new ConcurrentHashMap<>();
        if (hatePool != null) {
            map = new ConcurrentHashMap<Entity, Float>(hatePool);
        }
        removeUnattractiveCreature(map, unattractivecreature);
        if (!map.isEmpty()) {
            for (Map.Entry<Entity, Float> s : map.entrySet()) {
                ///////////////////
                if (!map.isEmpty()) {
                    if (s.getKey() == null) {
                        map.remove(s.getKey());
                    }
                }
                if (cantAttractiveTarget.containsKey(s.getKey())) {
                    if (map.containsKey(s.getKey())) {
                        map.remove(s.getKey());
                    }
                }
                if (s.getKey() != null) {
                    if (!map.isEmpty()) {
                        if (map.containsKey(s.getKey())) {
                            if (s.getKey() instanceof Player) {
                                if (!((Player) s.getKey()).isOnline()) {
                                    map.remove(s.getKey());
                                }
                            }
                        }
                    }
                }
                if (!map.isEmpty()) {
                    if (map.containsKey(s.getKey())) {
                        if (s.getKey().level != this.level) {
                            map.remove(s.getKey());
                        }
                    }
                }
                if (!map.isEmpty()) {
                    if (map.containsKey(s.getKey())) {
                        if (s.getKey().distance(this) >= this.haterange) {
                            map.remove(s.getKey());
                        }
                    }
                }
                if (!map.isEmpty()) {
                    if (map.containsKey(s.getKey())) {
                        if (!s.getKey().isAlive()) {
                            map.remove(s.getKey());
                        }
                    }
                }
                if (!map.isEmpty()) {
                    if (map.containsKey(s.getKey())) {
                        for (Effect effect : s.getKey().getEffects().values()) {
                            if (effect.getId() == 14) {
                                map.remove(s.getKey());
                            }
                        }
                    }
                }
            }
        }
        if (!map.isEmpty()) {
            Entity entity = null;


            //向Map中添加数据
            //.....
            //转换
            ArrayList<Map.Entry<Entity, Float>> arrayList = new ArrayList<Map.Entry<Entity, Float>>(map.entrySet());
            //排序
            Collections.sort(arrayList, new Comparator<Map.Entry<Entity, Float>>() {
                public int compare(Map.Entry<Entity, Float> map1,
                                   Map.Entry<Entity, Float> map2) {
                    return ((map2.getValue() - map1.getValue() == 0) ? 0
                            : (map2.getValue() - map1.getValue() > 0) ? 1
                            : -1);
                }
            });
            ConcurrentHashMap<Entity, Float> firstHateMap = new ConcurrentHashMap<>();
            for (Map.Entry<Entity, Float> entry : arrayList) {
                firstHateMap.put(entry.getKey(), entry.getValue());
            }
            List<Entity> firstHatePool = getMaxValueList(firstHateMap);
//get random target
            Collections.shuffle(firstHatePool);

            if (!firstHatePool.isEmpty()) {
                entity = firstHatePool.get(0);
            }
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    public List<Entity> getTargets(String[] s) {
        return getTargets(s[0], s.length < 2 ? 0 : Double.parseDouble(s[2]), s.length < 3 ? this.getHaterange() : Integer.parseInt(s[2]), s.length < 4 ? this.level.getEntities().length : Integer.parseInt(s[3]), null);
    }

    public List<Entity> getTargets(String type,double figure,double distance, int amountlimit, List<String> creaturetype) {
        List<Entity> entities = new ArrayList<>();
        for (Entity entity : getLevel().getEntities()) {
            if (entity.distance(this) <= distance) {
                if (creaturetype != null) {
                    simplifyTarget(entities, creaturetype, entity);
                } else {
                    simplifyTarget(entities, activeattackcreature, entity);
                }
            }
        }
        switch (type) {
            case "mostDamage": {
                ConcurrentHashMap<Entity, Float> damagepool = new ConcurrentHashMap<>();
                for (Entity entity : entities) {
                    if (this.damagePool.containsKey(entity)) {
                        damagepool.put(entity, this.damagePool.get(entity));
                    }
                }
                Entity entity = getMaxValue(damagepool);
                entities = new ArrayList<>();
                entities.add(entity);
                break;
            }
            case "mosthate": {
                ConcurrentHashMap<Entity, Float> hatepool = new ConcurrentHashMap<>();
                for (Entity entity : entities) {
                    if (this.hatePool.containsKey(entity)) {
                        hatepool.put(entity, this.hatePool.get(entity));
                    }
                }
                Entity entity = getMaxValue(hatepool);
                entities = new ArrayList<>();
                entities.add(entity);
                break;
            }
            case "nearest": {
                ConcurrentHashMap<Entity, Float> entityDistance = new ConcurrentHashMap<>();
                for (Entity entity : entities) {
                    entityDistance.put(entity, (float) entity.distance(this));
                }
                Entity entity = getMinValue(entityDistance);
                entities = new ArrayList<>();
                entities.add(entity);
                break;
            }
            case "farest": {
                ConcurrentHashMap<Entity, Float> entityDistance = new ConcurrentHashMap<>();
                for (Entity entity : entities) {
                    entityDistance.put(entity, (float) entity.distance(this));
                }
                Entity entity = getMaxValue(entityDistance);
                entities = new ArrayList<>();
                entities.add(entity);
                break;
            }
            case "lastDamager": {
                if (this.getLastDamageCause() != null) {
                    if (this.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                        if (((EntityDamageByEntityEvent) this.getLastDamageCause()).getDamager() instanceof Player) {
                            entities = new ArrayList<>();
                            entities.add(((EntityDamageByEntityEvent) this.getLastDamageCause()).getDamager());
                        }
                    }
                }
                break;
            }
            case "distance":{
                for (Entity entity:this.getLevel().getEntities()){
                    if (entity.distance(this)<=distance){
                        if (entity!=this) {
                            entities.add(entity);
                        }
                    }
                }
                break;
            }
            case "hate":{
                for (Entity entity:entities) {
                    if (this.getHatePool().containsKey(entity)) {
                        if (this.getHatePool().get(entity) >= figure) {
                            entities.add(entity);
                        }
                    }
                }
                break;
            }
            case "damage":{
                for (Entity entity:entities) {
                    if (this.getDamagePool().containsKey(entity)) {
                        if (this.getDamagePool().get(entity) >= figure) {
                            entities.add(entity);
                        }
                    }
                    if (creaturetype != null) {
                        removeUnattractiveCreature(entities,creaturetype);
                    } else {
                        removeUnattractiveCreature(entities,activeattackcreature);
                    }
                }
                break;
            }
            case "all": {
                break;
            }
        }
        if (entities.size() > amountlimit) {
            Collections.shuffle(entities);
            for (int i = amountlimit; i < entities.size(); i++) {
                entities.remove(i);
            }
        }
        return entities;
    }
    public void removeUnattractiveCreature(List<Entity> entities, List<String> unattractiveCreature) {
        for (Entity entity : entities) {
            if (unattractiveCreature != null) {
                for (String targettype : unattractiveCreature) {
//Multicharacteristic EntityChoose
                    if (targettype.contains("|")) {
                        boolean canBeChoose = true;
                        String[] types = targettype.split("\\|");
                        for (String type : types) {
                            String t = type;
                            String function = "";
                            if (type.contains(":")) {
                                t = type.split(":")[0];
                                function = type.split(":")[1];
                            }
                            switch (t) {
                                case "Player": {
                                    if (!(entity instanceof Player)) {
                                        canBeChoose = false;
                                    }
                                    break;
                                }
                                case "Mob": {
                                    if (!(entity instanceof MobNPC)) {
                                        canBeChoose = false;
                                    }
                                    break;
                                }
                                case "Camp": {
                                    if (entity instanceof MobNPC) {
                                        if (!((MobNPC) entity).getCamp().equals(function)) {
                                            canBeChoose = false;
                                        }
                                    } else {
                                        canBeChoose = false;
                                    }
                                    break;
                                }
                                case "Point": {
                                    if (entity instanceof MobNPC) {
                                        if (!((MobNPC) entity).getMobFeature().split(":")[((MobNPC) entity).getMobFeature().split(":").length - 1].equals(function)) {
                                            canBeChoose = false;
                                        }
                                    } else {
                                        canBeChoose = false;
                                    }
                                    break;
                                }
                                case "MDungeon": {
                                    if (getServer().getPluginManager().getPlugin("MDungeon")!=null) {
                                        if (this.mobFeature.split(":")[0].equals("MDungeon")) {
                                            Long roomid = Long.valueOf(this.mobFeature.split(":")[this.mobFeature.split(":").length - 1]);
                                            if (entity instanceof Player){
                                                if (DataPacketLimit.limitPlayers.containsKey(entity.getId())){
                                                    if (DataPacketLimit.limitPlayers.get(entity.getId())!=roomid){
                                                        canBeChoose = false;
                                                    }
                                                }
                                            }else{
                                                if (DataPacketLimit.limitEntities.containsKey(entity.getId())){
                                                    if (DataPacketLimit.limitEntities.get(entity.getId())!=roomid){
                                                        canBeChoose = false;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                                case "NotMDungeon": {
                                    if (getServer().getPluginManager().getPlugin("MDungeon")!=null) {
                                        if (this.mobFeature.split(":")[0].equals("MDungeon")) {
                                            Long roomid = Long.valueOf(this.mobFeature.split(":")[this.mobFeature.split(":").length - 1]);
                                            if (entity instanceof Player){
                                                if (DataPacketLimit.limitPlayers.containsKey(entity.getId())){
                                                    if (DataPacketLimit.limitPlayers.get(entity.getId())==roomid){
                                                        canBeChoose = false;
                                                    }
                                                }else{
                                                    canBeChoose = false;
                                                }
                                            }else{
                                                if (DataPacketLimit.limitEntities.containsKey(entity.getId())){
                                                    if (DataPacketLimit.limitEntities.get(entity.getId())==roomid){
                                                        canBeChoose = false;
                                                    }
                                                }else{
                                                    canBeChoose = false;
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        if (canBeChoose) {
                            entities.remove(entity);
                        }
//Single characteristic EntityChoose
                    } else {
                        String type = targettype;
                        String function = "";
                        if (targettype.contains(":")) {
                            type = targettype.split(":")[0];
                            function = targettype.split(":")[1];
                        }
                        switch (type) {
                            case "Player": {
                                if (entity instanceof Player) {
                                    entities.remove(entity);
                                }
                                break;
                            }
                            case "Mob": {
                                if (entity instanceof MobNPC) {
                                    entities.remove(entity);
                                }
                                break;
                            }
                            case "Camp": {
                                if (entity instanceof MobNPC) {
                                    if (((MobNPC) entity).getCamp().equals(function)) {
                                        entities.remove(entity);
                                    }
                                }
                                break;
                            }
                            case "Point": {
                                if (entity instanceof MobNPC) {
                                    if (((MobNPC) entity).getMobFeature().split(":")[((MobNPC) entity).getMobFeature().split(":").length - 1].equals(function)) {
                                        entities.remove(entity);
                                    }
                                }
                                break;
                            }
                            case "MDungeon": {
                                if (getServer().getPluginManager().getPlugin("MDungeon")!=null) {
                                    if (this.mobFeature.split(":")[0].equals("MDungeon")) {
                                        Long roomid = Long.valueOf(this.mobFeature.split(":")[this.mobFeature.split(":").length - 1]);
                                        if (entity instanceof Player){
                                            if (DataPacketLimit.limitPlayers.containsKey(entity.getId())){
                                                if (DataPacketLimit.limitPlayers.get(entity.getId())==roomid){
                                                    entities.remove(entity);
                                                }
                                            }
                                        }else{
                                            if (DataPacketLimit.limitEntities.containsKey(entity.getId())){
                                                if (DataPacketLimit.limitEntities.get(entity.getId())==roomid){
                                                    entities.remove(entity);
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            case "NotMDungeon": {
                                if (getServer().getPluginManager().getPlugin("MDungeon")!=null) {
                                    if (this.mobFeature.split(":")[0].equals("MDungeon")) {
                                        Long roomid = Long.valueOf(this.mobFeature.split(":")[this.mobFeature.split(":").length - 1]);
                                        if (entity instanceof Player){
                                            if (DataPacketLimit.limitPlayers.containsKey(entity.getId())){
                                                if (DataPacketLimit.limitPlayers.get(entity.getId())!=roomid){
                                                    entities.remove(entity);
                                                }
                                            }else{
                                                entities.remove(entity);
                                            }
                                        }else{
                                            if (DataPacketLimit.limitEntities.containsKey(entity.getId())){
                                                if (DataPacketLimit.limitEntities.get(entity.getId())!=roomid){
                                                    entities.remove(entity);
                                                }
                                            }else{
                                                entities.remove(entity);
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    public void removeUnattractiveCreature(ConcurrentHashMap<Entity, Float> map, List<String> unattractiveCreature) {
        for (Entity entity : map.keySet()) {
            if (unattractiveCreature != null) {
                for (String targettype : unattractiveCreature) {
//Multicharacteristic EntityChoose
                    if (targettype.contains("|")) {
                        boolean canBeChoose = true;
                        String[] types = targettype.split("\\|");
                        for (String type : types) {
                            String t = type;
                            String function = "";
                            if (type.contains(":")) {
                                t = type.split(":")[0];
                                function = type.split(":")[1];
                            }
                            switch (t) {
                                case "Player": {
                                    if (!(entity instanceof Player)) {
                                        canBeChoose = false;
                                    }
                                    break;
                                }
                                case "Mob": {
                                    if (!(entity instanceof MobNPC)) {
                                        canBeChoose = false;
                                    }
                                    break;
                                }
                                case "Camp": {
                                    if (entity instanceof MobNPC) {
                                        if (!((MobNPC) entity).getCamp().equals(function)) {
                                            canBeChoose = false;
                                        }
                                    } else {
                                        canBeChoose = false;
                                    }
                                    break;
                                }
                                case "Point": {
                                    if (entity instanceof MobNPC) {
                                        if (!((MobNPC) entity).getMobFeature().split(":")[((MobNPC) entity).getMobFeature().split(":").length - 1].equals(function)) {
                                            canBeChoose = false;
                                        }
                                    } else {
                                        canBeChoose = false;
                                    }
                                    break;
                                }
                                case "MDungeon": {
                                    if (getServer().getPluginManager().getPlugin("MDungeon")!=null) {
                                        if (this.mobFeature.split(":")[0].equals("MDungeon")) {
                                            Long roomid = Long.valueOf(this.mobFeature.split(":")[this.mobFeature.split(":").length - 1]);
                                            if (entity instanceof Player){
                                                if (DataPacketLimit.limitPlayers.containsKey(entity.getId())){
                                                    if (DataPacketLimit.limitPlayers.get(entity.getId())!=roomid){
                                                        canBeChoose = false;
                                                    }
                                                }
                                            }else{
                                                if (DataPacketLimit.limitEntities.containsKey(entity.getId())){
                                                    if (DataPacketLimit.limitEntities.get(entity.getId())!=roomid){
                                                        canBeChoose = false;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                                case "NotMDungeon": {
                                    if (getServer().getPluginManager().getPlugin("MDungeon")!=null) {
                                        if (this.mobFeature.split(":")[0].equals("MDungeon")) {
                                            Long roomid = Long.valueOf(this.mobFeature.split(":")[this.mobFeature.split(":").length - 1]);
                                            if (entity instanceof Player){
                                                if (DataPacketLimit.limitPlayers.containsKey(entity.getId())){
                                                    if (DataPacketLimit.limitPlayers.get(entity.getId())!=roomid){
                                                        map.remove(entity);
                                                    }
                                                }else{
                                                    map.remove(entity);
                                                }
                                            }else{
                                                if (DataPacketLimit.limitEntities.containsKey(entity.getId())){
                                                    if (DataPacketLimit.limitEntities.get(entity.getId())!=roomid){
                                                        map.remove(entity);
                                                    }
                                                }else{
                                                    map.remove(entity);
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                        if (canBeChoose) {
                            map.remove(entity);
                        }
//Single characteristic EntityChoose
                    } else {
                        String type = targettype;
                        String function = "";
                        if (targettype.contains(":")) {
                            type = targettype.split(":")[0];
                            function = targettype.split(":")[1];
                        }
                        switch (type) {
                            case "Player": {
                                if (entity instanceof Player) {
                                    map.remove(entity);
                                }
                                break;
                            }
                            case "Mob": {
                                if (entity instanceof MobNPC) {
                                    map.remove(entity);
                                }
                                break;
                            }
                            case "Camp": {
                                if (entity instanceof MobNPC) {
                                    if (((MobNPC) entity).getCamp().equals(function)) {
                                        map.remove(entity);
                                    }
                                }
                                break;
                            }
                            case "Point": {
                                if (entity instanceof MobNPC) {
                                    if (((MobNPC) entity).getMobFeature().split(":")[((MobNPC) entity).getMobFeature().split(":").length - 1].equals(function)) {
                                        map.remove(entity);
                                    }
                                }
                                break;
                            }
                            case "MDungeon": {
                                if (getServer().getPluginManager().getPlugin("MDungeon")!=null) {
                                    if (this.mobFeature.split(":")[0].equals("MDungeon")) {
                                        Long roomid = Long.valueOf(this.mobFeature.split(":")[this.mobFeature.split(":").length - 1]);
                                        if (entity instanceof Player){
                                            if (DataPacketLimit.limitPlayers.containsKey(entity.getId())){
                                                if (DataPacketLimit.limitPlayers.get(entity.getId())==roomid){
                                                    map.remove(entity);
                                                }
                                            }
                                        }else{
                                            if (DataPacketLimit.limitEntities.containsKey(entity.getId())){
                                                if (DataPacketLimit.limitEntities.get(entity.getId())==roomid){
                                                    map.remove(entity);
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            case "NotMDungeon": {
                                if (getServer().getPluginManager().getPlugin("MDungeon")!=null) {
                                    if (this.mobFeature.split(":")[0].equals("MDungeon")) {
                                        Long roomid = Long.valueOf(this.mobFeature.split(":")[this.mobFeature.split(":").length - 1]);
                                        if (entity instanceof Player){
                                            if (DataPacketLimit.limitPlayers.containsKey(entity.getId())){
                                                if (DataPacketLimit.limitPlayers.get(entity.getId())!=roomid){
                                                    map.remove(entity);
                                                }
                                            }else{
                                                map.remove(entity);
                                            }
                                        }else{
                                            if (DataPacketLimit.limitEntities.containsKey(entity.getId())){
                                                if (DataPacketLimit.limitEntities.get(entity.getId())!=roomid){
                                                    map.remove(entity);
                                                }
                                            }else{
                                                map.remove(entity);
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    public void simplifyTarget(List<Entity> entities, List<String> creaturetype, Entity entity) {
        if (entity != this) {
            for (String targettype : creaturetype) {
//Multicharacteristic EntityChoose
                if (targettype.contains("|")) {
                    boolean canBeChoose = true;
                    String[] types = targettype.split("\\|");
                    for (String type : types) {
                        String t = type;
                        String function = "";
                        if (type.contains(":")) {
                            t = type.split(":")[0];
                            function = type.split(":")[1];
                        }
                        switch (t) {
                            case "Player": {
                                if (!(entity instanceof Player)) {
                                    canBeChoose = false;
                                }
                                break;
                            }
                            case "Mob": {
                                if (!(entity instanceof MobNPC)) {
                                    canBeChoose = false;
                                }
                                break;
                            }
                            case "Camp": {
                                if (entity instanceof MobNPC) {
                                    if (!((MobNPC) entity).getCamp().equals(function)) {
                                        canBeChoose = false;
                                    }
                                } else {
                                    canBeChoose = false;
                                }
                                break;
                            }
                            case "Point": {
                                if (entity instanceof MobNPC) {
                                    if (!((MobNPC) entity).getMobFeature().split(":")[((MobNPC) entity).getMobFeature().split(":").length - 1].equals(function)) {
                                        canBeChoose = false;
                                    }
                                } else {
                                    canBeChoose = false;
                                }
                                break;
                            }
                            case "MDungeon": {
                                if (getServer().getPluginManager().getPlugin("MDungeon")!=null) {
                                    if (this.mobFeature.split(":")[0].equals("MDungeon")) {
                                        Long roomid = Long.valueOf(this.mobFeature.split(":")[this.mobFeature.split(":").length - 1]);
                                        if (entity instanceof Player){
                                            if (DataPacketLimit.limitPlayers.containsKey(entity.getId())){
                                                if (DataPacketLimit.limitPlayers.get(entity.getId())!=roomid){
                                                    canBeChoose = false;
                                                }
                                            }
                                        }else{
                                            if (DataPacketLimit.limitEntities.containsKey(entity.getId())){
                                                if (DataPacketLimit.limitEntities.get(entity.getId())!=roomid){
                                                    canBeChoose = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            case "NotMDungeon": {
                                if (getServer().getPluginManager().getPlugin("MDungeon")!=null) {
                                    if (this.mobFeature.split(":")[0].equals("MDungeon")) {
                                        Long roomid = Long.valueOf(this.mobFeature.split(":")[this.mobFeature.split(":").length - 1]);
                                        if (entity instanceof Player){
                                            if (DataPacketLimit.limitPlayers.containsKey(entity.getId())){
                                                if (DataPacketLimit.limitPlayers.get(entity.getId())==roomid){
                                                    canBeChoose = false;
                                                }
                                            }
                                        }else{
                                            if (DataPacketLimit.limitEntities.containsKey(entity.getId())){
                                                if (DataPacketLimit.limitEntities.get(entity.getId())==roomid){
                                                    canBeChoose = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                    if (canBeChoose) {
                        if (!entities.contains(entity)) {
                            entities.add(entity);
                        }
                    }
//Single characteristic EntityChoose
                } else {
                    String type = targettype;
                    String function = "";
                    if (targettype.contains(":")) {
                        type = targettype.split(":")[0];
                        function = targettype.split(":")[1];
                    }
                    switch (type) {
                        case "Player": {
                            if (entity instanceof Player) {
                                if (!entities.contains(entity)) {
                                    entities.add(entity);
                                }
                            }
                            break;
                        }
                        case "Mob": {
                            if (entity instanceof MobNPC) {
                                if (!entities.contains(entity)) {
                                    entities.add(entity);
                                }
                            }
                            break;
                        }
                        case "Camp": {
                            if (entity instanceof MobNPC) {
                                if (((MobNPC) entity).getCamp().equals(function)) {
                                    entities.add(entity);
                                }
                            }
                            break;
                        }
                        case "Point": {
                            if (entity instanceof MobNPC) {
                                if (((MobNPC) entity).getMobFeature().split(":")[((MobNPC) entity).getMobFeature().split(":").length - 1].equals(function)) {
                                    entities.add(entity);
                                }
                            }
                            break;
                        }
                        case "MDungeon": {
                            if (getServer().getPluginManager().getPlugin("MDungeon")!=null) {
                                if (this.mobFeature.split(":")[0].equals("MDungeon")) {
                                    Long roomid = Long.valueOf(this.mobFeature.split(":")[this.mobFeature.split(":").length - 1]);
                                    if (entity instanceof Player){
                                        if (DataPacketLimit.limitPlayers.containsKey(entity.getId())){
                                            if (DataPacketLimit.limitPlayers.get(entity.getId())==roomid){
                                                entities.add(entity);
                                            }
                                        }
                                    }else{
                                        if (DataPacketLimit.limitEntities.containsKey(entity.getId())){
                                            if (DataPacketLimit.limitEntities.get(entity.getId())==roomid){
                                                entities.add(entity);
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                        case "NotMDungeon": {
                            if (getServer().getPluginManager().getPlugin("MDungeon")!=null) {
                                if (this.mobFeature.split(":")[0].equals("MDungeon")) {
                                    Long roomid = Long.valueOf(this.mobFeature.split(":")[this.mobFeature.split(":").length - 1]);
                                    if (entity instanceof Player){
                                        if (DataPacketLimit.limitPlayers.containsKey(entity.getId())){
                                            if (DataPacketLimit.limitPlayers.get(entity.getId())!=roomid){
                                                entities.add(entity);
                                            }
                                        }
                                    }else{
                                        if (DataPacketLimit.limitEntities.containsKey(entity.getId())){
                                            if (DataPacketLimit.limitEntities.get(entity.getId())!=roomid){
                                                entities.add(entity);
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    //Setting method


    public void setCamp(String camp) {
        this.camp = camp;
    }

    public String getCamp() {
        return camp;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setAttackdelay(int attackdelay) {
        this.attackdelay = attackdelay;
    }

    public void setAttackrange(double attackrange) {
        this.attackrange = attackrange;
    }

    public void setBeDamagedblockparticle(String beattackblockparticle) {
        this.bedamagedblockparticle = beattackblockparticle;
    }

    public String getBeDamagedblockparticle() {
        return bedamagedblockparticle;
    }

    public void setBedamageddelay(int bedamageddelay) {
        this.bedamageddelay = bedamageddelay;
    }

    public int getBedamageddelay() {
        return bedamageddelay;
    }

    public void setBedamageCD(ConcurrentHashMap<Entity, Integer> bedamageCD) {
        this.bedamageCD = bedamageCD;
    }

    public ConcurrentHashMap<Entity, Integer> getBedamageCD() {
        return bedamageCD;
    }

    public void setCanbeknockback(boolean canbeknockback) {
        this.canbeknockback = canbeknockback;
    }

    public boolean getCanbeknockback() {
        return canbeknockback;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public void setDamagedelay(int damagedelay) {
        this.damagedelay = damagedelay;
    }

    public void setDamagePool(ConcurrentHashMap<Entity, Float> damagePool) {
        this.damagePool = damagePool;
    }

    public ConcurrentHashMap<Entity, Float> getDamagePool() {
        return damagePool;
    }

    public void setHatePool(ConcurrentHashMap<Entity, Float> hatePool) {
        this.hatePool = hatePool;
    }

    public ConcurrentHashMap<Entity, Float> getHatePool() {
        return hatePool;
    }

    public void setDeathcommands(List<String> deathcommands) {
        this.deathcommands = deathcommands;
    }

    public List<String> getDeathcommands() {
        return deathcommands;
    }

    public void setDefenseformula(String defenseformula) {
        this.defenseformula = defenseformula;
    }

    public String getDefenseformula() {
        return defenseformula;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setDrops(List<String> drops) {
        this.drops = drops;
    }

    public void setHaterange(double haterange) {
        this.haterange = haterange;
    }

    public double getHaterange() {
        return haterange;
    }

    public void setNohatesheal(String nohatesheal) {
        this.nohatesheal = nohatesheal;
    }

    public void setKnockback(double knockback) {
        this.knockback = knockback;
    }

    public void setSkinname(String skinname) {
        this.skinname = skinname;
    }

    public String getSkinname() {
        return skinname;
    }

    public void setActiveattackcreature(List<String> activeattackcreature) {
        this.activeattackcreature = activeattackcreature;
    }

    public List<String> getActiveattackcreature() {
        return activeattackcreature;
    }

    public List<String> getUnattractivecreature() {
        return unattractivecreature;
    }

    public void setUnattractivecreature(List<String> unattractivecreature) {
        this.unattractivecreature = unattractivecreature;
    }

    public void setHitrange(double hitrange) {
        this.hitrange = hitrange;
    }

    public void setMobFeature(String mobPoint) {
        this.mobFeature = mobPoint;
    }

    public String getMobFeature() {
        return mobFeature;
    }

    public boolean isAttacking() {
        return isAttacking;
    }

    public ConcurrentHashMap<Entity, Integer> getCantAttractiveTarget() {
        return cantAttractiveTarget;
    }

    public void setCantAttractiveTarget(ConcurrentHashMap<Entity, Integer> cantAttractiveTarget) {
        this.cantAttractiveTarget = cantAttractiveTarget;
    }

    public void setGeometryName(String geometryName) {
        this.geometryName = geometryName;
    }

    public void setEnableBox(boolean enableBox) {
        this.enableBox = enableBox;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }
/*
    @Override
    public void spawnToAll() {
        if (this.chunk != null && !this.closed) {
            Iterator var1 = this.level.getChunkPlayers(this.chunk.getX(), this.chunk.getZ()).values().iterator();
            while(var1.hasNext()) {
                Player player = (Player)var1.next();
                if (player.isOnline()) {
                    this.spawnTo(player);
                }
            }
        }
    }

    public void spawnTo(Player player) {
        if (!this.hasSpawned.containsKey(player.getLoaderId()) && this.chunk != null && player.usedChunks.containsKey(Level.chunkHash(this.chunk.getX(), this.chunk.getZ()))) {
            this.hasSpawned.put(player.getLoaderId(), player);
            player.dataPacket(this.createAddEntityPacket());
        }

        if (this.riding != null) {
            this.riding.spawnTo(player);
            SetEntityLinkPacket pkk = new SetEntityLinkPacket();
            pkk.vehicleUniqueId = this.riding.getId();
            pkk.riderUniqueId = this.getId();
            pkk.type = 1;
            pkk.immediate = 1;
            player.dataPacket(pkk);
        }
    }

 */

    //attack method
    public void attackEntity(Entity target) {
        attackdelayed++;
        boolean canAttack = true;
        if (this.hasEffect(15)) {
            if (new Random().nextInt(101) <= 50) {
                canAttack = false;
            }
        }
        if (this.attackdelayed >= this.attackdelay) {
            if (distance(target)/target.scale <= this.attackrange) {
                Position position = target.getPosition().clone();
                float knockback = (float) this.knockback;
                HashMap<EntityDamageEvent.DamageModifier, Float> damage = new HashMap<>();
                damage.put(EntityDamageEvent.DamageModifier.BASE, (float) this.damage);
                if (target != null) {
                    if (target instanceof Player) {
                        HashMap<Integer, Float> armorValues = new Monster.ArmorPoints();
                        float points = 0;
                        if (((Player) target).getInventory() != null) {
                            if (((Player) target).getInventory().getArmorContents() != null) {
                                for (Item i : ((Player) target).getInventory().getArmorContents()) {
                                    points += armorValues.getOrDefault(i.getId(), 0f);
                                }

                                damage.put(EntityDamageEvent.DamageModifier.ARMOR, (float) (damage.getOrDefault(EntityDamageEvent.DamageModifier.ARMOR, 0f) - Math.floor(damage.getOrDefault(EntityDamageEvent.DamageModifier.BASE, 1f) * points * 0.04)));
                            }
                        }
                    }
                    NPC npc = this;
                    boolean finalCanAttack = canAttack;
                    if (!isAttacking) {
                        getServer().getScheduler().scheduleDelayedTask(new Task() {
                            @Override
                            public void onRun(int i) {
                                for (Entity entity : getLevel().getEntities()) {
                                    if (entity != npc) {
                                        if (hatePool.containsKey(entity)) {
                                            if (entity.distanceSquared(position) <= npc.hitrange) {
                                                if (finalCanAttack) {
                                                    if (npc.isAlive()) {
                                                        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(npc, entity, EntityDamageEvent.DamageCause.ENTITY_ATTACK, damage);
                                                        event.setKnockBack(knockback);
                                                        entity.attack(event);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                broadcastEntityEvent(4);
                                npc.attackdelayed = 0;
                                isAttacking = false;
                            }
                        }, this.damagedelay, true);
                    }
                    this.isAttacking = true;
                }
            }
        }
    }


    public void broadcastEntityEvent(int eventId) {
        broadcastEntityEvent(eventId, 0);
    }

    public void broadcastEntityEvent(int eventId, int eventData) {
        EntityEventPacket packet = new EntityEventPacket();
        packet.eid = this.id;
        packet.event = eventId;
        packet.data = eventData;
        for (Player player : getViewers().values()) {
            if (player.isOnline())
                player.dataPacket(packet);
        }
    }

    public void beattackparticle() {
        String[] iddamage = getBeDamagedblockparticle().split(":");
        int id = Integer.parseInt(iddamage[0]);
        int damage = Integer.parseInt(iddamage[1]);
        Block block = Block.get(id, damage);
        DestroyBlockParticle particle = new DestroyBlockParticle(this.getPosition(), block);
        this.getLevel().addParticle(particle);
    }

    public void dropItem(Vector3 source, Item item, Vector3 motion, boolean dropAround, int delay) {
        if (motion == null) {
            if (dropAround) {
                float f = ThreadLocalRandom.current().nextFloat() * 0.5F;
                float f1 = ThreadLocalRandom.current().nextFloat() * 6.2831855F;
                motion = new Vector3((double) (-MathHelper.sin(f1) * f), 0.20000000298023224D, (double) (MathHelper.cos(f1) * f));
            } else {
                motion = new Vector3((new Random()).nextDouble() * 0.2D - 0.1D, 0.2D, (new Random()).nextDouble() * 0.2D - 0.1D);
            }
        }

        CompoundTag itemTag = NBTIO.putItemHelper(item);
        itemTag.setName("Item");
        if (item.getId() > 0 && item.getCount() > 0) {
            EntityItem itemEntity = (EntityItem) Entity.createEntity("Item", this.getLevel().getChunk((int) source.getX() >> 4, (int) source.getZ() >> 4, true), (new CompoundTag()).putList((new ListTag("Pos")).add(new DoubleTag("", source.getX())).add(new DoubleTag("", source.getY())).add(new DoubleTag("", source.getZ()))).putList((new ListTag("Motion")).add(new DoubleTag("", motion.x)).add(new DoubleTag("", motion.y)).add(new DoubleTag("", motion.z))).putList((new ListTag("Rotation")).add(new FloatTag("", (new Random()).nextFloat() * 360.0F)).add(new FloatTag("", 0.0F))).putShort("Health", 5).putCompound("Item", itemTag).putShort("PickupDelay", delay), new Object[0]);
            if (itemEntity != null) {
                //IF you need you can change the method type :)
                itemEntity.spawnToAll();
            }
        }

    }

    @Override
    public Item[] getDrops() {
        return new Item[0];
    }

    public void Drop() {
        for (String probAndDrops : drops) {
            int probability = Integer.parseInt(probAndDrops.split(":")[0]);
            if (new Random().nextInt(101) <= probability) {
                String items = probAndDrops.split(":")[1];
                if (items.contains("||")) {
                    String[] itemgroup = items.split("\\|\\|");
                    String item = itemgroup[new Random().nextInt(itemgroup.length)];
                    if (item.contains("&&")) {
                        for (String i : item.split("&&")) {
                            dropItems(i, this);
                        }
                    } else {
                        dropItems(item, this);
                    }
                } else {
                    if (items.contains("&&")) {
                        for (String item : items.split("&&")) {
                            dropItems(item, this);
                        }
                    } else {
                        dropItems(items, this);
                    }
                }
            }
        }
    }

    public void bedamagedcdCheck() {
        if (bedamageCD != null) {
            for (Entity entity : bedamageCD.keySet()) {
                if (bedamageCD.get(entity) > 0) {
                    bedamageCD.put(entity, bedamageCD.get(entity) - 1);
                } else {
                    bedamageCD.remove(entity);
                }
            }
        }
    }

    public void dropItems(String itemiddamageamount, NPC npc) {
        int id = Integer.parseInt(itemiddamageamount.split("-")[0]);
        int damage = Integer.parseInt(itemiddamageamount.split("-")[1]);
        int amount = Integer.parseInt(itemiddamageamount.split("-")[2]);
        Item item = Item.get(id, damage, amount);
        dropItem(npc, item, null, false, 2);
    }

    //js formula
    public double readEntityParameters(String s) {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine se = manager.getEngineByName("javascript");
        s = recoverString(s);
        try {
            Double d = Double.valueOf(se.eval(s).toString());
            return d;
        } catch (ScriptException e) {
            System.out.println(s);
            try {
                System.out.println(se.eval(s).toString());
            } catch (ScriptException scriptException) {
                scriptException.printStackTrace();
            }
            System.out.println("算式错误");
        }
        return 0;
    }

    public String recoverString(String s) {
        if (target != null) {
            s = s.replaceAll("target\\.name", target.getName());
        }
        if (target != null) {
            s = s.replaceAll("target\\.health", target.getHealth() + "");
        }
        if (target != null) {
            s = s.replaceAll("target\\.x", target.x + "");
        } else {
            s = s.replaceAll("target\\.x", this.x + "");
        }
        if (target != null) {
            s = s.replaceAll("target\\.y", target.y + "");
        } else {
            s = s.replaceAll("target\\.y", this.y + "");
        }
        if (target != null) {
            s = s.replaceAll("target\\.z", target.z + "");
        } else {
            s = s.replaceAll("target\\.z", this.z + "");
        }
        if (this.getLastDamageCause() != null) {
            if (this.getLastDamageCause().getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                if (((EntityDamageByEntityEvent) this.getLastDamageCause()).getDamager() instanceof Player) {
                    s = s.replaceAll("damager\\.name", ((EntityDamageByEntityEvent) this.getLastDamageCause()).getDamager().getName());
                }
            }
        }

        s = s
                .replaceAll("npc\\.yaw", this.yaw + "")
                .replaceAll("npc\\.pitch", this.pitch + "")
                .replaceAll("npc\\.x", this.x + "")
                .replaceAll("npc\\.y", this.y + "")
                .replaceAll("npc\\.z", this.z + "")
                .replaceAll("npc\\.health", this.getHealth() + "")
                .replaceAll("npc\\.damage", this.damage + "");

        return s;
    }
    public void spawnTo(Player player) {
        if (player.isLoaderActive()) {
            if (Long.valueOf(this.getId()) == null) {
                this.id = Entity.entityCount++;
            }
            if (!this.hasSpawned.containsKey(player.getLoaderId())) {
                this.hasSpawned.put(player.getLoaderId(), player);
                if (this.skin == null) {
                    this.skin = MRPGNPC.skins.get(skinname);
                }
                PlayerSkinPacket packet = new PlayerSkinPacket();
                packet.uuid = this.uuid;
                packet.newSkinName = "";
                packet.oldSkinName = "";
                packet.skin = this.skin;
                AddPlayerPacket pk = new AddPlayerPacket();
                pk.uuid = this.getUniqueId();
                pk.username = this.getName();
                pk.entityUniqueId = this.getId();
                pk.entityRuntimeId = this.getId();
                pk.x = (float) this.x;
                pk.y = (float) this.y;
                pk.z = (float) this.z;
                pk.speedX = (float) this.motionX;
                pk.speedY = (float) this.motionY;
                pk.speedZ = (float) this.motionZ;
                pk.yaw = (float) this.yaw;
                pk.pitch = (float) this.pitch;
                pk.item = this.getInventory().getItemInHand();
                pk.metadata = this.dataProperties;
                player.dataPacket(packet);
                player.dataPacket(pk);
                this.inventory.sendArmorContents(player);
                this.offhandInventory.sendContents(player);
                if (this.riding != null) {
                    SetEntityLinkPacket pkk = new SetEntityLinkPacket();
                    pkk.vehicleUniqueId = this.riding.getId();
                    pkk.riderUniqueId = this.getId();
                    pkk.type = 1;
                    pkk.immediate = 1;
                    player.dataPacket(pkk);
                }
                this.server.removePlayerListData(this.getUniqueId(), new Player[]{player});
            }
        }
    }
}
