package com.thevoos.megatools.component;

/**
 * The Mega Tools data stored on item stacks. In 1.21 these are data components; Minecraft 1.20.1 has none,
 * so each one is a ModComponent: saved in the item's NBT with the same codec (and synced with it).
 * Use as COMPONENT.get(stack), COMPONENT.set(stack, value), ...
 */
public final class ModDataComponents
{
    /** Ability toggles + which enchantment plate is loaded. */
    public static final ModComponent<ToolSettings> TOOL_SETTINGS = new ModComponent<>("tool_settings", ToolSettings.CODEC);

    /** Wrench storage contents (9 / 18 / 27 slots depending on tier; older tools saved 8 / 16 / 24). Reuses vanilla's container format. */
    public static final ModComponent<ItemContainerContents> TOOL_STORAGE = new ModComponent<>("tool_storage", LenientContents.CODEC);

    /** The two enchantment plate slots (index 0 = primary, 1 = secondary). */
    public static final ModComponent<ItemContainerContents> ENCHANTMENT_PLATES = new ModComponent<>("enchantment_plates", LenientContents.CODEC);

    /**
     * The Support Tool slot (one item; every tier). While a Support Tool is in it, the
     * tool gets the shears, brush and flint and steel abilities (see ToolData#hasSupportTool).
     */
    public static final ModComponent<ItemContainerContents> SUPPORT_TOOL = new ModComponent<>("support_tool", LenientContents.CODEC);

    /** The torches in the light slot (type + count, up to 256). Present on every tier. */
    public static final ModComponent<StoredTorches> TORCHES = new ModComponent<>("torches", StoredTorches.CODEC);

    /**
     * LEGACY: the old single-item light slot. Still registered so tools saved before the torch slot
     * load correctly; MultiToolItem#inventoryTick moves its torch into TORCHES (anything else goes back
     * to the player) and removes it.
     */
    public static final ModComponent<ItemContainerContents> TOOL_LIGHT = new ModComponent<>("tool_light", LenientContents.CODEC);

    /** The stored tool this Mega Tools tool last swapped to (for "Swap to last tool"). */
    public static final ModComponent<LastSwap> LAST_SWAP = new ModComponent<>("last_swap", LastSwap.CODEC);

    /** Custom swap rules set in the Swap Rules GUI: which blocks pre-select which stored tool. Per tool stack. */
    public static final ModComponent<SwapRules> SWAP_RULES = new ModComponent<>("swap_rules", SwapRules.CODEC);

    /**
     * The default rules (client file config/megatools-default-rules.json) that were already copied into this
     * tool's SWAP_RULES. A default rule is copied once per tool; after that it is an ordinary rule the player
     * can change or remove, and this list keeps a removed one from coming back.
     */
    public static final ModComponent<SwapRules> APPLIED_DEFAULT_RULES = new ModComponent<>("applied_default_rules", SwapRules.APPLIED_CODEC);

    /** Put on a tool that was auto-swapped into the hand; holds the Mega Tools tool to restore. */
    public static final ModComponent<SwapOrigin> SWAP_ORIGIN = new ModComponent<>("swap_origin", SwapOrigin.CODEC);

    private ModDataComponents() {}
}
