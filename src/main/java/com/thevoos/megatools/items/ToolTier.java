package com.thevoos.megatools.items;

import com.thevoos.megatools.StartupConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.function.Consumer;

/**
 * Feature set of each Mega Tools tool, plus where its stats come from.
 *
 * Attack damage, durability and mining speed are read from config/megatools-startup.toml
 * (StartupConfig) when the item is registered. The defaults there are:
 *   Durability = sum of the vanilla tools it replaces (1.21.1 values):
 *     STONE   : 5 stone tools  x 131                                              =  655
 *     IRON    : 5 iron tools   x 250 (1250) + brush 64 + flint&steel 64 + shears 238  = 1616
 *     DIAMOND : 5 diamond tools x 1561 (7805) + brush 64 + flint&steel 64 + shears 238 = 8171
 *     MEGA    : 0 = unbreakable
 *   (The shears/brush/flint and steel abilities come from a Support Tool in the tool's Support Tool
 *   slot, on every tier; the iron/diamond default durability still includes those tools.)
 *   Mining speed = vanilla tier speed (stone 4.0, iron 6.0, diamond 8.0) x miningSpeedMultiplier (1.2).
 *   Attack damage = the TOTAL shown in the tooltip (10 / 14 / 18 / 22).
 *   Attack speed = attacks per second shown in the tooltip (1.6 / 2.0 / 2.5 / 3.0).
 * The Mega Tool always mines instantly (MegaToolMining), so its base speed here hardly matters.
 */
public enum ToolTier
{
    //      config section               vanilla speed storage support plates incorrect-for-drops tag             GUI accent   anvil repair
    STONE  (StartupConfig.STONE_PAXEL,   4.0F,  0,      true,   true,  BlockTags.INCORRECT_FOR_STONE_TOOL,     0xFF5E5E5E, p -> p.repairable(ItemTags.STONE_TOOL_MATERIALS)),
    IRON   (StartupConfig.IRON_PAXEL,    6.0F,  9,      true,   true,  BlockTags.INCORRECT_FOR_IRON_TOOL,      0xFF6B7782, p -> p.repairable(Items.IRON_INGOT)),
    DIAMOND(StartupConfig.DIAMOND_PAXEL, 8.0F,  18,     true,   true,  BlockTags.INCORRECT_FOR_DIAMOND_TOOL,   0xFF1D8A86, p -> p.repairable(Items.DIAMOND)),
    MEGA   (StartupConfig.MEGA_TOOL,     12.0F, 27,     true,   true,  BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 0xFF3E2050, p -> {});

    private final StartupConfig.ToolValues config;
    private final float vanillaSpeed;
    private final int storageSlots;
    private final boolean supportTools;
    private final boolean enchantPlates;
    private final TagKey<Block> incorrectBlocksForDrops;
    private final int accentColor;
    private final Consumer<Item.Properties> repairItem;

    ToolTier(StartupConfig.ToolValues config, float vanillaSpeed, int storageSlots,
             boolean supportTools, boolean enchantPlates, TagKey<Block> incorrectBlocksForDrops,
             int accentColor, Consumer<Item.Properties> repairItem)
    {
        this.config = config;
        this.vanillaSpeed = vanillaSpeed;
        this.storageSlots = storageSlots;
        this.supportTools = supportTools;
        this.enchantPlates = enchantPlates;
        this.incorrectBlocksForDrops = incorrectBlocksForDrops;
        this.accentColor = accentColor;
        this.repairItem = repairItem;
    }

    // ---- From config/megatools-startup.toml. Only call these once configs are loaded
    //      (item registration or later); they are read when each item is constructed. ----

    /** Max durability; 0 = unbreakable (Mega Tool default). */
    public int durability() { return config.durability.get(); }

    /** Vanilla tier speed x the configured multiplier. */
    public float miningSpeed()
    {
        double multiplier = config.miningSpeedMultiplier != null
                ? config.miningSpeedMultiplier.get()
                : StartupConfig.ToolValues.DEFAULT_SPEED_MULTIPLIER;
        return (float) (vanillaSpeed * multiplier);
    }

    /** Total attack damage shown in the tooltip. */
    public float attackDamage() { return config.attackDamage.get().floatValue(); }

    /** Attacks per second shown in the tooltip (MultiToolItem subtracts the player's base of 4.0). */
    public float attackSpeed() { return config.attackSpeed.get().floatValue(); }

    // ---- Fixed per tier ----

    public int storageSlots() { return storageSlots; }
    /**
     * Has a Support Tool slot. The shears + brush + flint and steel abilities (and their GUI buttons)
     * are only active while a Support Tool is in it: see ToolData#hasSupportTool.
     */
    public boolean supportTools() { return supportTools; }
    /**
     * Primary/secondary enchantment plate slots and Shift+R switching (every tier). The tools themselves
     * can't be enchanted; their enchantments always come from the loaded plate.
     */
    public boolean hasEnchantPlates() { return enchantPlates; }
    /**
     * Whether this tier has the GUI toggle for ToolSettings bit {@code bit} (button id = bit):
     * 0-2 strip/path/till (all), 3-5 shear/brush/ignite (all tiers; shown while a Support Tool is in),
     * 6 delayed break (mega), 7 place torch (all), 8 light on/off (all; stored inverted as LIGHT_OFF),
     * 9 break blocks / 10 attack (all; stored inverted as BREAK_OFF / ATTACK_OFF).
     */
    public boolean hasToggle(int bit)
    {
        return switch (bit)
        {
            case 0, 1, 2, 7, 8, 9, 10 -> true;
            case 3, 4, 5 -> supportTools;
            case 6 -> isMega();
            default -> false;
        };
    }

    /**
     * Rows of toggle buttons: one row for Break / Attack, the 3-wide grid (1 or 2 rows) and one row for
     * Delayed Break / Place Torch.
     */
    public int toggleRows() { return 1 + (supportTools ? 2 : 1) + 1; }
    /** Auto-swap only makes sense when there is wrench storage to swap from. */
    public boolean hasAutoSwap() { return storageSlots > 0; }
    public TagKey<Block> incorrectBlocksForDrops() { return incorrectBlocksForDrops; }
    /** Colour of the title bar in the GUI, so every tool's GUI looks different. */
    public int accentColor() { return accentColor; }
    /** Anvil repair material (the Mega Tool has none). */
    public void applyRepairItem(Item.Properties properties) { repairItem.accept(properties); }
    public boolean isMega() { return this == MEGA; }
}
