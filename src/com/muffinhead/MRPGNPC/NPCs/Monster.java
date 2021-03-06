package com.muffinhead.MRPGNPC.NPCs;

import cn.nukkit.event.Listener;
import cn.nukkit.item.Item;

import java.util.HashMap;
//copy mobplugin
public interface Monster {
    final class ArmorPoints extends HashMap<Integer, Float> implements Listener {
        {
            put(Item.LEATHER_CAP, 1f);
            put(Item.LEATHER_TUNIC, 3f);
            put(Item.LEATHER_PANTS, 2f);
            put(Item.LEATHER_BOOTS, 1f);
            put(Item.CHAIN_HELMET, 1f);
            put(Item.CHAIN_CHESTPLATE, 5f);
            put(Item.CHAIN_LEGGINGS, 4f);
            put(Item.CHAIN_BOOTS, 1f);
            put(Item.GOLD_HELMET, 1f);
            put(Item.GOLD_CHESTPLATE, 5f);
            put(Item.GOLD_LEGGINGS, 3f);
            put(Item.GOLD_BOOTS, 1f);
            put(Item.IRON_HELMET, 2f);
            put(Item.IRON_CHESTPLATE, 6f);
            put(Item.IRON_LEGGINGS, 5f);
            put(Item.IRON_BOOTS, 2f);
            put(Item.DIAMOND_HELMET, 3f);
            put(Item.DIAMOND_CHESTPLATE, 8f);
            put(Item.DIAMOND_LEGGINGS, 6f);
            put(Item.DIAMOND_BOOTS, 3f);
        }
    }
}
