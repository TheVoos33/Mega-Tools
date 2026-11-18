package com.thevoos.megatools.items;

import com.thevoos.megatools.MegaTools;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/** Tag keys used by the mod. The JSON lives in data/megatools/tags/blocks/mineable/paxel.json. */
public final class ModTags
{
    /** Pickaxe + axe + shovel + hoe mineable blocks combined. */
    public static final TagKey<Block> MINEABLE_WITH_PAXEL =
            TagKey.create(Registries.BLOCK, MegaTools.id("mineable/paxel"));

    /**
     * Blocks the Mega Tool is NOT the right tool for. Everything else counts as mined with the right tool.
     * On these blocks the Mega Tool works like an empty hand: normal hand speed (no instant mining) and
     * only the drops a hand gets. Empty by default; it includes the old #megatools:mega_tool_unbreakable tag
     * (removed in v1.2) as an optional entry, so datapacks that still fill that tag keep working.
     * JSON: data/megatools/tags/blocks/not_mineable.json, "replace": false to add blocks.
     */
    public static final TagKey<Block> NOT_MINEABLE =
            TagKey.create(Registries.BLOCK, MegaTools.id("not_mineable"));

    /**
     * The only thing tool storage checks: an item is accepted if and only if it has this tag.
     * JSON: data/megatools/tags/items/storable_item.json, which pulls in other mods' tool tags (wrenches,
     * shears, screwdrivers, crowbars...). Mods and datapacks can add entries by shipping their own file
     * at that path with "replace": false. The items and tags listed in
     * config/megatools-storage-whitelist.json are added to it when tags load (see StorageWhitelist).
     */
    public static final TagKey<Item> STORABLE_ITEM =
            TagKey.create(Registries.ITEM, MegaTools.id("storable_item"));

    /**
     * Items accepted in the tool's light slot: torches (minecraft:torch and minecraft:soul_torch by
     * default). Modded torches can be added: data/megatools/tags/items/torches.json, "replace": false.
     */
    public static final TagKey<Item> TORCHES =
            TagKey.create(Registries.ITEM, MegaTools.id("torches"));

    private ModTags() {}
}
