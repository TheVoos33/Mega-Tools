package com.thevoos.megatools.items;

import net.minecraftforge.registries.ForgeRegistries;
import com.thevoos.megatools.MegaTools;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Item registry. Registry names are unchanged from your original file, so your existing
 * models (models/item/<name>.json) and lang entries keep working.
 */
public class ModItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MegaTools.MODID);

    // ---- Tools (all share MultiToolItem; ToolTier decides stats and features) ----
    public static final RegistryObject<MultiToolItem> STONE_PAXEL = ITEMS.register("stone_paxel",
            () -> new MultiToolItem(ToolTier.STONE, new Item.Properties()));
    public static final RegistryObject<MultiToolItem> IRON_PAXEL = ITEMS.register("iron_paxel",
            () -> new MultiToolItem(ToolTier.IRON, new Item.Properties()));
    public static final RegistryObject<MultiToolItem> DIAMOND_PAXEL = ITEMS.register("diamond_paxel",
            () -> new MultiToolItem(ToolTier.DIAMOND, new Item.Properties()));
    public static final RegistryObject<MultiToolItem> NETHERITE_PAXEL = ITEMS.register("netherite_paxel",
            () -> new MultiToolItem(ToolTier.NETHERITE, new Item.Properties()));
    public static final RegistryObject<MultiToolItem> MEGA_TOOL = ITEMS.register("mega_tool",
            () -> new MultiToolItem(ToolTier.MEGA, new Item.Properties()));

    // ---- Mega Tool upgrades: only registered when the Allthemodium mod is installed, otherwise these
    //      items don't exist at all (null here). Made in a smithing table, see data/megatools/recipes/. ----
    public static final boolean ALLTHEMODIUM = ModList.get().isLoaded("allthemodium");
    @Nullable public static final RegistryObject<MultiToolItem> MEGA_TOOL_ALLTHEMODIUM = registerIfAllthemodium("mega_tool_allthemodium", ToolTier.MEGA_ALLTHEMODIUM);
    @Nullable public static final RegistryObject<MultiToolItem> MEGA_TOOL_VIBRANIUM = registerIfAllthemodium("mega_tool_vibranium", ToolTier.MEGA_VIBRANIUM);
    @Nullable public static final RegistryObject<MultiToolItem> MEGA_TOOL_UNOBTAINIUM = registerIfAllthemodium("mega_tool_unobtainium", ToolTier.MEGA_UNOBTAINIUM);

    // ---- Crafting ingredients ----
    public static final RegistryObject<Item> IRON_HEAD = ITEMS.register("iron_head",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DIAMOND_HEAD = ITEMS.register("diamond_head",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> NETHERITE_HEAD = ITEMS.register("netherite_head",
            () -> new Item(new Item.Properties().fireResistant()));
    /** Nether star, dragon head, sculk shrieker and dragon's breath; upgrades the Netherite Paxel to the Mega Tool in a smithing table. */
    public static final RegistryObject<Item> MEGA_TOOL_CORE = ITEMS.register("mega_tool_core",
            () -> new Item(new Item.Properties().fireResistant().rarity(Rarity.EPIC)));

    // ---- Upgrade item ----
    public static final RegistryObject<EnchantmentPlateItem> ENCHANTMENT_PLATE = ITEMS.register("enchantment_plate",
            () -> new EnchantmentPlateItem(new Item.Properties()));

    /** Shears + brush + flint and steel; adds those abilities to a tool when placed in its Support Tool slot. */
    public static final RegistryObject<SupportToolItem> SUPPORT_TOOL = ITEMS.register("support_tool",
            () -> new SupportToolItem(new Item.Properties()));

    @Nullable
    private static RegistryObject<MultiToolItem> registerIfAllthemodium(String name, ToolTier tier)
    {
        return ALLTHEMODIUM ? ITEMS.register(name, () -> new MultiToolItem(tier, new Item.Properties())) : null;
    }

    /** Every Mega Tool item that exists in this game: the Mega Tool, plus its upgrades with Allthemodium. */
    public static List<Item> megaTools()
    {
        List<Item> items = new ArrayList<>();
        items.add(MEGA_TOOL.get());
        for (RegistryObject<MultiToolItem> upgrade : Arrays.asList(MEGA_TOOL_ALLTHEMODIUM, MEGA_TOOL_VIBRANIUM, MEGA_TOOL_UNOBTAINIUM))
            if (upgrade != null)
                items.add(upgrade.get());
        return items;
    }

    public static void register(IEventBus eventBus)
    {
        ITEMS.register(eventBus);
    }
}
