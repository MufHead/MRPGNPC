package com.muffinhead.MRPGNPC.Effects;

import cn.nukkit.block.Block;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.weather.EntityLightning;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.AxisAlignedBB;
import cn.nukkit.nbt.tag.CompoundTag;

import java.util.concurrent.ThreadLocalRandom;

public class Lightning extends EntityLightning {
    public Lightning(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }
    protected void initEntity() {
        super.initEntity();
        this.setHealth(4.0F);
        this.setMaxHealth(4);
        this.state = 2;
        this.liveTime = ThreadLocalRandom.current().nextInt(3) + 1;
        if (this.isEffect && this.level.gameRules.getBoolean(GameRule.DO_FIRE_TICK) && this.server.getDifficulty() >= 2) {
        }
    }


    public boolean onUpdate(int currentTick) {
        if (this.closed) {
            return false;
        } else {
            int tickDiff = currentTick - this.lastUpdate;
            if (tickDiff <= 0 && !this.justCreated) {
                return true;
            } else {
                this.lastUpdate = currentTick;
                this.entityBaseTick(tickDiff);
                if (this.state == 2) {
                    this.level.addLevelSoundEvent(this, 47);
                    this.level.addLevelSoundEvent(this, 48);
                }

                --this.state;
                if (this.state < 0) {
                    if (this.liveTime == 0) {
                        this.close();
                        return false;
                    }

                    if (this.state < -ThreadLocalRandom.current().nextInt(10)) {
                        --this.liveTime;
                        this.state = 1;
                        if (this.isEffect && this.level.gameRules.getBoolean(GameRule.DO_FIRE_TICK)) {
                            Block block = this.getLevelBlock();
                            if (block.getId() == 0 || block.getId() == 31) {

                            }
                        }
                    }
                }

                if (this.state >= 0 && this.isEffect) {
                    AxisAlignedBB bb = this.getBoundingBox().grow(3.0D, 3.0D, 3.0D);
                    bb.setMaxX(bb.getMaxX() + 6.0D);
                    Entity[] var9 = this.level.getCollidingEntities(bb, this);
                    int var10 = var9.length;

                    for(int var6 = 0; var6 < var10; ++var6) {
                        Entity entity = var9[var6];
                        entity.onStruckByLightning(this);
                    }
                }

                return true;
            }
        }
    }
}
