package com.muffinhead.MRPGNPC;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.entity.item.EntityItem;
import cn.nukkit.item.Item;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import com.muffinhead.MRPGNPC.Events.MobNPCBeAttack;
import com.muffinhead.MRPGNPC.NPCs.MobNPC;
import com.muffinhead.MRPGNPC.NPCs.NPC;
import com.muffinhead.MRPGNPC.Tasks.AutoSpawn;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static cn.nukkit.utils.Utils.readFile;

public class MRPGNPC extends PluginBase {
    public static MRPGNPC mrpgnpc;
    //public static Map<String, CompoundTag> tagMap = new HashMap<>();
    public static ConcurrentHashMap<String, Config> mobconfigs = new ConcurrentHashMap<String, Config>();
    public static ConcurrentHashMap<String, Config> pointconfigs = new ConcurrentHashMap<String, Config>();
    public static ConcurrentHashMap<String, Skin> skins = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        getServer().getLogger().info("MRPGNPC is enable!The author is MuffinHead.");
        getServer().getPluginManager().registerEvents(new MobNPCBeAttack(),this);
        mrpgnpc = this;
        saveResource("GreenCross/skin.png", "/Skins/GreenCross/skin.png", false);
        saveResource("GreenCross/geometry.json", "/Skins/GreenCross/geometry.json", false);
        checkMobs();
        checkPoints();
        getServer().getScheduler().scheduleDelayedRepeatingTask(new AutoSpawn(),1,1);
        try {
            checkSkins();
        } catch (IOException e) {
            getServer().getLogger().alert("Skins check wrong！！");
        }
    }

    @Override
    public void onDisable() {
        for (Level level :getServer().getLevels().values()){
            for (Entity entity:level.getEntities()){
                if (entity instanceof MobNPC){
                    entity.kill();
                }
            }
        }
    }

    //command part
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mrn")) {
            if (args.length <= 0) return false;
            switch (args[0]) {
                case "clear":{
                    if (args.length <= 1) return false;
                    switch (args[1]) {
                        case "mobs":{
                            for (Level level:getServer().getLevels().values()){
                                for (Entity entity:level.getEntities()){
                                    if (entity instanceof MobNPC) {
                                        entity.kill();
                                    }
                                }
                            }
                        }
                        case "drops": {
                            for (Level level:getServer().getLevels().values()){
                                for (Entity entity:level.getEntities()){
                                    if (entity instanceof EntityItem) {
                                        entity.kill();
                                    }
                                }
                            }
                        }
                    }
                }
                case "mob": {
                    if (args.length <= 1) return false;
                    switch (args[1]) {
                        case "create": {
                            if (args.length >= 3) {
                                File mobFile = getMobFolder().resolve(args[2] + ".yml").toFile();
                                if (mobFile.exists()) {
                                    sender.sendMessage("§cThis mobfile is already exist!");
                                    return true;
                                }
                                Config config = createMobConfig(mobFile.getPath());
                                config.save();
                                sender.sendMessage("§aThe new mobfile was created successfully!");
                                return true;
                            }
                        }
                        case "delete": {
                            if (args.length >= 3) {
                                File mobFile = getMobFolder().resolve(args[2] + ".yml").toFile();
                                if (!mobFile.exists()) {
                                    sender.sendMessage("§cCan't find the file");

                                } else {
                                    mobFile.deleteOnExit();
                                    sender.sendMessage("§aThe mobfile was deleted successfully!");
                                }
                                return true;
                            }
                        }
                        case "spawn": {
                            NPC npc = spawnNPC(sender, args);
                            if (npc!=null){
                                npc.spawnToAll();
                            }
                            return true;
                        }
                    }
                    return false;
                }
                case "point": {
                    if (args.length <= 1) return false;
                    switch (args[1]) {
                        case "create": {
                            if (sender instanceof Player) {
                                if (args.length >= 3) {
                                    File pointFile = getPointFolder().resolve(args[2] + ".yml").toFile();
                                    if (pointFile.exists()) {
                                        sender.sendMessage("§cThis pointfile is already exist!");
                                        return true;
                                    }
                                    Config config = createPointConfig(pointFile.getPath(), ((Player) sender));
                                    config.save();
                                    sender.sendMessage("§aThe new pointfile was created successfully!");
                                    return true;
                                }
                            }
                        }
                        case "delete": {
                            if (args.length >= 3) {
                                File pointFile = getPointFolder().resolve(args[2] + ".yml").toFile();
                                if (!pointFile.exists()) {
                                    sender.sendMessage("§cCan't find the file");
                                } else {
                                    pointFile.deleteOnExit();
                                    sender.sendMessage("§aThe pointfile was deleted successfully!");
                                }
                                return true;
                            }
                        }
                    }
                }
                case "reload":{
                    checkMobs();
                    checkPoints();
                    sender.sendMessage("§aThe mob&point files was reload successfully!");
                    return true;
                }

                //put on the back burner
                case "skill": {

                }
            }
        }
        return super.onCommand(sender, command, label, args);
    }
//command part


    public Path getMobFolder() {
        return getDataFolder().toPath().resolve("Mobs");
    }

    public Path getPointFolder() {
        return getDataFolder().toPath().resolve("Points");
    }

    public Config createMobConfig(String configPath) {
        Config config = new Config(configPath, Config.YAML);
        config.set("DisplayName", "Mob");
        config.set("MaxHealth", 40);
        config.set("Size", 1.0);
        config.set("MovementSpeed", 1.0);
        config.set("Damage", 3.0);
        config.set("KnockBack", 0.1);
        config.set("DefenseFormula", "source.damage");
        config.set("AttackDelay", 30);
        config.set("DamageDelay", 0);
        config.set("BedamagedDelay", 0);
        config.set("AttackRange", 1.2);
        config.set("HitRange",0.15);
        config.set("HateRange", 15.0);
        config.set("HitRange",0.15);
        config.set("NoHatesHeal", "200:1.0");
        config.set("CanBeKnockBack", false);
        config.set("DeathCommands", new ArrayList<>());
        config.set("Skin", "GreenCross");
        config.set("ItemInHand", "267:0");
        config.set("BeDamagedBlockParticleID", "152:0");
        config.set("ActiveAttackCreature",new ArrayList<>());
        config.set("Drops", new ArrayList<>());
        config.set("Camp", "Example");
        return config;
    }

    /*
    mobfilename-respawntick-1timespawnamount-maxamount-spawnlimit
     */
    public Config createPointConfig(String configPath,Player player) {
        Config config = new Config(configPath, Config.YAML);
        config.set("PointName", "A");
        config.set("PointPosition",player.getX()+":"+player.getY()+":"+player.getZ()+":"+player.getLevel().getName());
        config.set("SpawnList", new ArrayList<>());
        return config;
    }
    public MobNPC spawnNPC(CommandSender sender, String mobfile, Position position,String mobFeature) {
        String[] args = new String[7];
        args[0] = "";
        args[1] = "";
        args[2] = mobfile;
        args[3] = String.valueOf(position.x);
        args[4] = String.valueOf(position.y);
        args[5] = String.valueOf(position.z);
        args[6] = position.getLevel().getName();
        MobNPC npc = spawnNPC(sender,args);
        npc.setMobFeature(mobFeature);
        return npc;
    }

    public MobNPC spawnNPC(CommandSender sender, String[] args) {
        if (args.length < 7) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("The console need type the coordinates");
                return null;
            }else{
                Config config = mobconfigs.get(args[2]);
                if (config!=null) {
                    MobNPC npc = new MobNPC(((Player) sender).getChunk(), NPC.getDefaultNBT(((Player) sender).getTargetBlock(3)));
                    npc.setDisplayName(config.getString("DisplayName"));
                    npc.setMaxHealth(config.getInt("MaxHealth"));
                    npc.setHealth(npc.getMaxHealth());
                    npc.setScale((float) config.getDouble("Size"));
                    npc.setSpeed(config.getDouble("MovementSpeed"));
                    npc.setDamage(config.getDouble("Damage"));
                    npc.setKnockback(config.getDouble("Knockback"));
                    npc.setDefenseformula(config.getString("DefenseFormula"));
                    npc.setAttackdelay(config.getInt("AttackDelay"));
                    npc.setDamagedelay(config.getInt("DamageDelay"));
                    npc.setBedamageddelay(config.getInt("BedamagedDelay"));
                    npc.setAttackrange(config.getDouble("AttackRange"));
                    npc.setHaterange(config.getDouble("HateRange"));
                    npc.setNohatesheal(config.getString("NoHatesHeal"));
                    npc.setHitrange(config.getDouble("HitRange"));
                    npc.setCanbeknockback(config.getBoolean("CanBeKnockback"));
                    npc.setDeathcommands(config.getList("DeathCommands"));
                    npc.setCamp(config.getString("Camp"));
                    npc.getInventory().setItemInHand(getItemByString(config.getString("ItemInHand")));
                    npc.setBeDamagedblockparticle(config.getString("BeDamagedBlockParticleID"));
                    npc.setActiveattackcreature(config.getList("ActiveAttackCreature"));
                    npc.setDrops(config.getList("Drops"));
                    Skin skin = skins.get(config.getString("Skin"));
                    npc.setSkin(skin);
                    return npc;
                }
            }
        }else{
            Config config = mobconfigs.get(args[2]);
            if (config!=null) {
                Position position = new Position(Double.parseDouble(args[3]), Double.parseDouble(args[4]), Double.parseDouble(args[5]), getServer().getLevelByName(args[6]));
                MobNPC npc = new MobNPC(position.getChunk(), NPC.getDefaultNBT(position));
                npc.setDisplayName(config.getString("DisplayName"));
                npc.setMaxHealth(config.getInt("MaxHealth"));
                npc.setHealth(npc.getMaxHealth());
                npc.setScale((float) config.getDouble("Size"));
                npc.setSpeed(config.getDouble("MovementSpeed"));
                npc.setDamage(config.getDouble("Damage"));
                npc.setKnockback(config.getDouble("Knockback"));
                npc.setDefenseformula(config.getString("DefenseFormula"));
                npc.setAttackdelay(config.getInt("AttackDelay"));
                npc.setDamagedelay(config.getInt("DamageDelay"));
                npc.setBedamageddelay(config.getInt("BedamagedDelay"));
                npc.setAttackrange(config.getDouble("AttackRange"));
                npc.setHaterange(config.getDouble("HateRange"));
                npc.setNohatesheal(config.getString("NoHatesHeal"));
                npc.setHitrange(config.getDouble("HitRange"));
                npc.setCanbeknockback(config.getBoolean("CanBeKnockback"));
                npc.setDeathcommands(config.getList("DeathCommands"));
                npc.setCamp(config.getString("Camp"));
                npc.getInventory().setItemInHand(getItemByString(config.getString("ItemInHand")));
                npc.setBeDamagedblockparticle(config.getString("BeDamagedBlockParticleID"));
                npc.setActiveattackcreature(config.getList("ActiveAttackCreature"));
                npc.setDrops(config.getList("Drops"));
                Skin skin = skins.get(config.getString("Skin"));
                npc.setSkin(skin);
                return npc;
            }
        }
        return null;
    }


    public Item getItemByString(String s){
        Item item = Item.get(Integer.parseInt(s.split(":")[0]));
        item.setDamage(Integer.parseInt(s.split(":")[1]));
        return item;
    }
    public void checkSkins() throws IOException {
        Path skinPath = getDataFolder().toPath().resolve("Skins");
        File skinsFolder = new File(skinPath.toString());
        if (!skinsFolder.exists()) {
            skinsFolder.mkdirs();
        }
        for (File skinFolder : Objects.requireNonNull(skinsFolder.listFiles())) {
            Skin skin = new Skin();
            /*CompoundTag tag = getSkinTag(skinFolder.getName());
              tagMap.put(skinFolder.getName(), tag);*/
            Path skinpath = skinFolder.toPath().resolve("skin.png");
            Path geometrypath = skinFolder.toPath().resolve("geometry.json");
            BufferedImage skindata = null;
            String skingeometry = "";
            try {
                skindata = ImageIO.read(skinpath.toFile());
                skingeometry = new String(Files.readAllBytes(geometrypath), StandardCharsets.UTF_8);
                if (skindata != null) {
                    skin.setSkinData(skindata);
                    skin.setGeometryData(skingeometry);
                    skin.setGeometryName("geometry." + skinFolder.getName());
                    skin.setSkinId(skinFolder.getName());
                }
                skins.put(skinFolder.getName(), skin);
            } catch (IOException event) {
                System.out.println("skin not exist");
            }
        }
        File[] files = skinsFolder.listFiles();
        for (File skinthings : files) {
            // CompoundTag tag = getSkinTag(skinthings.getName());
            //tagMap.put(skinthings.getName(), tag);
        }
    }
    public void checkMobs(){
        File mobsFolder = getMobFolder().toFile();
        if (!mobsFolder.exists()) {
            mobsFolder.mkdirs();
        }
        for (File mobfile : Objects.requireNonNull(mobsFolder.listFiles())) {
            Config config = new Config(mobfile.getPath());
            mobconfigs.put(mobfile.getName().replace(".yml", ""), config);
        }
    }
    public void checkPoints(){
        File pointsFolder = getPointFolder().toFile();
        if (!pointsFolder.exists()) {
            pointsFolder.mkdirs();
        }
        for (File pointfile : Objects.requireNonNull(pointsFolder.listFiles())) {
            Config config = new Config(pointfile.getPath());
            pointconfigs.put(pointfile.getName().replace(".yml", ""), config);
        }
    }
    public static CompoundTag getSkinTag(String skinName) throws IOException {
        Skin skin = new Skin();
        BufferedImage skindata = null;
        try {
            skindata = ImageIO.read(new File(mrpgnpc.getDataFolder() + "/Skins/" + skinName + "/skin.png"));
        } catch (IOException var19) {
            System.out.println("model not exist");
        }

        if (skindata != null) {
            skin.setSkinData(skindata);
            skin.setSkinId(skinName);
        }
        Map<String, Object> skinJson = (new Config(mrpgnpc.getDataFolder() + "/Skins/" + skinName + "/geometry.json", Config.JSON)).getAll();
        String geometryName = null;
        for (Map.Entry<String, Object> entry1 : skinJson.entrySet()) {
            if (geometryName == null) {
                geometryName = entry1.getKey();
            }
        }
        skin.setGeometryName(geometryName);
        skin.setGeometryData(readFile(new File(mrpgnpc.getDataFolder() + "/Skins/" + skinName + "/geometry.json")));
        CompoundTag skinTag = new CompoundTag()
                .putByteArray("Data", skin.getSkinData().data)
                .putInt("SkinImageWidth", skin.getSkinData().width)
                .putInt("SkinImageHeight", skin.getSkinData().height)
                .putString("ModelId", skin.getSkinId())
                .putString("CapeId", skin.getCapeId())
                .putByteArray("CapeData", skin.getCapeData().data)
                .putInt("CapeImageWidth", skin.getCapeData().width)
                .putInt("CapeImageHeight", skin.getCapeData().height)
                .putByteArray("SkinResourcePatch", skin.getSkinResourcePatch().getBytes(StandardCharsets.UTF_8))
                .putByteArray("GeometryData", skin.getGeometryData().getBytes(StandardCharsets.UTF_8))
                .putByteArray("AnimationData", skin.getAnimationData().getBytes(StandardCharsets.UTF_8))
                .putBoolean("PremiumSkin", skin.isPremium())
                .putBoolean("PersonaSkin", skin.isPersona())
                .putBoolean("CapeOnClassicSkin", skin.isCapeOnClassic());
        return skinTag;
    }
}
