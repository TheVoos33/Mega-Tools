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
    // registerItem / registerSimpleItem set the item's id on its properties (required since 1.21.2).
    public static final DeferredItem<MultiToolItem> STONE_PAXEL = ITEMS.registerItem("stone_paxel",
            properties -> new MultiToolItem(ToolTier.STONE, properties));
    public static final DeferredItem<MultiToolItem> IRON_PAXEL = ITEMS.registerItem("iron_paxel",
            properties -> new MultiToolItem(ToolTier.IRON, properties));
    public static final DeferredItem<MultiToolItem> DIAMOND_PAXEL = ITEMS.registerItem("diamond_paxel",
            properties -> new MultiToolItem(ToolTier.DIAMOND, properties));
    public static final DeferredItem<MultiToolItem> MEGA_TOOL = ITEMS.registerItem("mega_tool",
            properties -> new MultiToolItem(ToolTier.MEGA, properties));

    // ---- Crafting ingredients ----
    public static final DeferredItem<Item> IRON_HEAD = ITEMS.registerSimpleItem("iron_head");
    public static final DeferredItem<Item> DIAMOND_HEAD = ITEMS.registerSimpleItem("diamond_head");
    public static final DeferredItem<Item> MEGA_TOOL_HEAD = ITEMS.registerSimpleItem("mega_tool_head",
            properties -> properties.fireResistant().rarity(Rarity.RARE));

    // ---- Upgrade item ----
    public static final DeferredItem<EnchantmentPlateItem> ENCHANTMENT_PLATE = ITEMS.registerItem("enchantment_plate",
            EnchantmentPlateItem::new);

    /** Shears + brush + flint and steel; adds those abilities to a tool when placed in its Support Tool slot. */
    public static final DeferredItem<SupportToolItem> SUPPORT_TOOL = ITEMS.registerItem("support_tool",
            SupportToolItem::new);

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
