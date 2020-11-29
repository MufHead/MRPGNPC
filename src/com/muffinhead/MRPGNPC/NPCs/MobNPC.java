package com.muffinhead.MRPGNPC.NPCs;

import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

public class MobNPC extends NPC{
    public MobNPC(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public boolean entityBaseTick(int tickDiff) {
        checkTargetCanBeChoose();
        checkPlayerIsAttractive();
        updateDisplayName();
        if (target == null) {
            this.target = this.getTarget();
        }
        onMove();
        if (this.target != null) {
            attackEntity(this.target);
        }
        return super.entityBaseTick(tickDiff);
    }
}
