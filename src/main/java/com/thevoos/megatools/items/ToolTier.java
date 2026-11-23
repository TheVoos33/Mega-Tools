package com.thevoos.megatools.items;

import com.thevoos.megatools.StartupConfig;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Feature set of each Mega Tools tool, plus where its stats come from.
 *
 * Attack damage, durability and mining speed are read from config/megatools-startup.toml
 * (StartupConfig) when the item is registered. The defaults there are:
 *   Durability = sum of the vanilla tools it replaces (1.21.1 values):
 *     STONE   : 5 stone tools  x 131                                              =  655
 *     IRON    : 5 iron tools   x 250 (1250) + brush 64 + flint&steel 64 + shears 238  = 1616
 *     DIAMOND : 5 diamond tools x 1561 (7805) + brush 64 + flint&steel 64 + shears 238 = 8171
 *     NETHERITE: 5 netherite tools x 2031                                         = 10155
 *     MEGA    : 0 = unbreakable (every Mega Tool tier)
 *   (The shears/brush/flint and steel abilities come from a Support Tool in the tool's Support Tool
 *   slot, on every tier; the iron/diamond default durability still includes those tools.)
 *   Mining speed = vanilla tier speed (stone 4.0, iron 6.0, diamond 8.0, netherite 9.0) x miningSpeedMultiplier (1.2).
 *   Attack damage = the TOTAL shown in the tooltip (10 / 14 / 18 / 22 paxels, 28 Mega Tool,
 *   38 / 48 / 60 for the Allthemodium / Vibranium / Unobtainium Mega Tools).
 *   Attack speed = attacks per second shown in the tooltip (1.6 / 2.0 / 2.5 / 2.5 paxels, 3.0 Mega Tools).
 * The Mega Tools always mine instantly (MegaToolMining), so their base speed here hardly matters.
 *
 * Mega Tool tiers: MEGA plus three upgrades that only exist when the Allthemodium mod is installed
 * (see ModItems#ALLTHEMODIUM). They are the same tool with more attack damage; the difference is which
 * Allthemodium ores they can break (see #tooWeakFor).
 */
public enum ToolTier
{
    //      config section               vanilla speed storage support plates incorrect-for-drops tag             GUI accent   repair item
    STONE  (StartupConfig.STONE_PAXEL,   4.0F,  0,      true,   true,  BlockTags.INCORRECT_FOR_STONE_TOOL,     0xFF5E5E5E, () -> Ingredient.of(ItemTags.STONE_TOOL_MATERIALS)),
    IRON   (StartupConfig.IRON_PAXEL,    6.0F,  9,      true,   true,  BlockTags.INCORRECT_FOR_IRON_TOOL,      0xFF6B7782, () -> Ingredient.of(Items.IRON_INGOT)),
    DIAMOND(StartupConfig.DIAMOND_PAXEL, 8.0F,  18,     true,   true,  BlockTags.INCORRECT_FOR_DIAMOND_TOOL,   0xFF1D8A86, () -> Ingredient.of(Items.DIAMOND)),
    NETHERITE(StartupConfig.NETHERITE_PAXEL, 9.0F, 27,  true,   true,  BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 0xFF4A3B3D, () -> Ingredient.of(Items.NETHERITE_INGOT)),
    MEGA   (StartupConfig.MEGA_TOOL,     12.0F, 36,     true,   true,  BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 0xFF3E2050, () -> Ingredient.EMPTY),
    MEGA_ALLTHEMODIUM(StartupConfig.MEGA_TOOL_ALLTHEMODIUM, 12.0F, 36, true, true, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 0xFF9A4A1C, () -> Ingredient.EMPTY),
    MEGA_VIBRANIUM   (StartupConfig.MEGA_TOOL_VIBRANIUM,    12.0F, 36, true, true, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 0xFF116165, () -> Ingredient.EMPTY),
    MEGA_UNOBTAINIUM (StartupConfig.MEGA_TOOL_UNOBTAINIUM,  12.0F, 36, true, true, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 0xFF422992, () -> Ingredient.EMPTY);

    private final StartupConfig.ToolValues config;
    private final float vanillaSpeed;
    private final int storageSlots;
    private final boolean supportTools;
    private final boolean enchantPlates;
    private final TagKey<Block> incorrectBlocksForDrops;
    private final int accentColor;
    private final Supplier<Ingredient> repairIngredient;

    ToolTier(StartupConfig.ToolValues config, float vanillaSpeed, int storageSlots,
             boolean supportTools, boolean enchantPlates, TagKey<Block> incorrectBlocksForDrops,
             int accentColor, Supplier<Ingredient> repairIngredient)
    {
        this.config = config;
        this.vanillaSpeed = vanillaSpeed;
        this.storageSlots = storageSlots;
        this.supportTools = supportTools;
        this.enchantPlates = enchantPlates;
        this.incorrectBlocksForDrops = incorrectBlocksForDrops;
        this.accentColor = accentColor;
        this.repairIngredient = repairIngredient;
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
    public Ingredient repairIngredient() { return repairIngredient.get(); }
    /** Every Mega Tool tier: instant mining, Delayed Break, poke animation, unbreakable by default. */
    public boolean isMega()
    {
        return this == MEGA || this == MEGA_ALLTHEMODIUM || this == MEGA_VIBRANIUM || this == MEGA_UNOBTAINIUM;
    }

    /**
     * Blocks this Mega Tool tier can't break at all (empty unless Allthemodium is installed):
     *   MEGA              - #megatools:needs_allthemodium_mega_tool (vibranium and unobtainium ores)
     *   MEGA_ALLTHEMODIUM - #megatools:needs_vibranium_mega_tool (unobtainium ores)
     *   MEGA_VIBRANIUM / MEGA_UNOBTAINIUM - nothing: they break every block.
     * Paxels are not affected (they follow the normal tool tiers).
     */
    public boolean tooWeakFor(BlockState state)
    {
        return switch (this)
        {
            case MEGA -> state.is(ModTags.NEEDS_ALLTHEMODIUM_MEGA_TOOL);
            case MEGA_ALLTHEMODIUM -> state.is(ModTags.NEEDS_VIBRANIUM_MEGA_TOOL);
            default -> false;
        };
    }
}
