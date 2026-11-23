package com.thevoos.megatools.items;

import com.thevoos.megatools.MegaTools;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    public static final DeferredItem<MultiToolItem> NETHERITE_PAXEL = ITEMS.registerItem("netherite_paxel",
            properties -> new MultiToolItem(ToolTier.NETHERITE, properties));
    public static final DeferredItem<MultiToolItem> MEGA_TOOL = ITEMS.registerItem("mega_tool",
            properties -> new MultiToolItem(ToolTier.MEGA, properties));

    // ---- Mega Tool upgrades: only registered when the Allthemodium mod is installed, otherwise these
    //      items don't exist at all (null here). Made in a smithing table, see data/megatools/recipe/. ----
    public static final boolean ALLTHEMODIUM = ModList.get().isLoaded("allthemodium");
    @Nullable public static final DeferredItem<MultiToolItem> MEGA_TOOL_ALLTHEMODIUM = registerIfAllthemodium("mega_tool_allthemodium", ToolTier.MEGA_ALLTHEMODIUM);
    @Nullable public static final DeferredItem<MultiToolItem> MEGA_TOOL_VIBRANIUM = registerIfAllthemodium("mega_tool_vibranium", ToolTier.MEGA_VIBRANIUM);
    @Nullable public static final DeferredItem<MultiToolItem> MEGA_TOOL_UNOBTAINIUM = registerIfAllthemodium("mega_tool_unobtainium", ToolTier.MEGA_UNOBTAINIUM);

    // ---- Crafting ingredients ----
    public static final DeferredItem<Item> IRON_HEAD = ITEMS.registerSimpleItem("iron_head");
    public static final DeferredItem<Item> DIAMOND_HEAD = ITEMS.registerSimpleItem("diamond_head");
    public static final DeferredItem<Item> NETHERITE_HEAD = ITEMS.registerSimpleItem("netherite_head",
            properties -> properties.fireResistant());
    /** Nether star, dragon head, sculk shrieker and dragon's breath; upgrades the Netherite Paxel to the Mega Tool in a smithing table. */
    public static final DeferredItem<Item> MEGA_TOOL_CORE = ITEMS.registerSimpleItem("mega_tool_core",
            properties -> properties.fireResistant().rarity(Rarity.EPIC));

    // ---- Upgrade item ----
    public static final DeferredItem<EnchantmentPlateItem> ENCHANTMENT_PLATE = ITEMS.registerItem("enchantment_plate",
            EnchantmentPlateItem::new);

    /** Shears + brush + flint and steel; adds those abilities to a tool when placed in its Support Tool slot. */
    public static final DeferredItem<SupportToolItem> SUPPORT_TOOL = ITEMS.registerItem("support_tool",
            SupportToolItem::new);

    @Nullable
    private static DeferredItem<MultiToolItem> registerIfAllthemodium(String name, ToolTier tier)
    {
        return ALLTHEMODIUM ? ITEMS.registerItem(name, properties -> new MultiToolItem(tier, properties)) : null;
    }

    /** Every Mega Tool item that exists in this game: the Mega Tool, plus its upgrades with Allthemodium. */
    public static List<Item> megaTools()
    {
        List<Item> items = new ArrayList<>();
        items.add(MEGA_TOOL.get());
        for (DeferredItem<MultiToolItem> upgrade : Arrays.asList(MEGA_TOOL_ALLTHEMODIUM, MEGA_TOOL_VIBRANIUM, MEGA_TOOL_UNOBTAINIUM))
            if (upgrade != null)
                items.add(upgrade.get());
        return items;
    }

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
