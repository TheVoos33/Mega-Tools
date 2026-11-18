package com.thevoos.megatools.items;

import com.thevoos.megatools.MegaTools;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registry. Registry names are unchanged from your original file, so your existing
 * models (models/item/<name>.json) and lang entries keep working.
 */
public class ModItems
{
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MegaTools.MODID);

    // ---- Tools (all share MultiToolItem; ToolTier decides stats and features) ----
    public static final DeferredItem<MultiToolItem> STONE_PAXEL = ITEMS.register("stone_paxel",
            () -> new MultiToolItem(ToolTier.STONE, new Item.Properties()));
    public static final DeferredItem<MultiToolItem> IRON_PAXEL = ITEMS.register("iron_paxel",
            () -> new MultiToolItem(ToolTier.IRON, new Item.Properties()));
    public static final DeferredItem<MultiToolItem> DIAMOND_PAXEL = ITEMS.register("diamond_paxel",
            () -> new MultiToolItem(ToolTier.DIAMOND, new Item.Properties()));
    public static final DeferredItem<MultiToolItem> MEGA_TOOL = ITEMS.register("mega_tool",
            () -> new MultiToolItem(ToolTier.MEGA, new Item.Properties()));

    // ---- Crafting ingredients ----
    public static final DeferredItem<Item> IRON_HEAD = ITEMS.register("iron_head",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> DIAMOND_HEAD = ITEMS.register("diamond_head",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> MEGA_TOOL_HEAD = ITEMS.register("mega_tool_head",
            () -> new Item(new Item.Properties().fireResistant().rarity(Rarity.RARE)));

    // ---- Upgrade item ----
    public static final DeferredItem<EnchantmentPlateItem> ENCHANTMENT_PLATE = ITEMS.register("enchantment_plate",
            () -> new EnchantmentPlateItem(new Item.Properties()));

    /** Shears + brush + flint and steel; adds those abilities to a tool when placed in its Support Tool slot. */
    public static final DeferredItem<SupportToolItem> SUPPORT_TOOL = ITEMS.register("support_tool",
            () -> new SupportToolItem(new Item.Properties()));

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
