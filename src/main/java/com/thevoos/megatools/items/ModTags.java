package com.thevoos.megatools.items;

import com.thevoos.megatools.MegaTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Tag keys used by the mod. The JSON lives in data/megatools/tags/block/mineable/paxel.json. */
public final class ModTags
{
    /** Pickaxe + axe + shovel + hoe mineable blocks combined. */
    public static final TagKey<Block> MINEABLE_WITH_PAXEL =
            TagKey.create(Registries.BLOCK, MegaTools.id("mineable/paxel"));

    /**
     * Blocks the Mega Tool can never break (any game mode).
     * JSON: data/megatools/tags/block/mega_tool_unbreakable.json. Other mods and datapacks can add
     * to it by shipping their own file at that same path with "replace": false.
     */
    public static final TagKey<Block> MEGA_TOOL_UNBREAKABLE =
            TagKey.create(Registries.BLOCK, MegaTools.id("mega_tool_unbreakable"));

    /**
     * Items that can ALWAYS go in wrench storage, even if they are stackable or hold other items
     * (which the default filter refuses). JSON: data/megatools/tags/item/storable_tools.json.
     * Modpacks can add entries from a datapack with "replace": false.
     */
    public static final TagKey<Item> STORABLE_TOOLS =
            TagKey.create(Registries.ITEM, MegaTools.id("storable_tools"));

    /**
     * Items accepted in the tool's light slot: torches (minecraft:torch and minecraft:soul_torch by
     * default). Modded torches can be added: data/megatools/tags/item/torches.json, "replace": false.
     */
    public static final TagKey<Item> TORCHES =
            TagKey.create(Registries.ITEM, MegaTools.id("torches"));

    private ModTags() {}
}
