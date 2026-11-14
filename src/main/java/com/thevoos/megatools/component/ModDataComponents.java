package com.thevoos.megatools.component;

import com.thevoos.megatools.MegaTools;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Custom data components (1.21 replacement for item NBT).
 * persistent(...)          -> saved with the item
 * networkSynchronized(...) -> sent to clients (needed for GUI, tooltips and the HUD icon)
 */
public final class ModDataComponents
{
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MegaTools.MODID);

    /** Ability toggles + which enchantment plate is loaded. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolSettings>> TOOL_SETTINGS =
            COMPONENTS.register("tool_settings", () -> DataComponentType.<ToolSettings>builder()
                    .persistent(ToolSettings.CODEC)
                    .networkSynchronized(ToolSettings.STREAM_CODEC)
                    .build());

    /** Wrench storage contents (9 / 18 / 27 slots depending on tier; older tools saved 8 / 16 / 24). Reuses vanilla's container format. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> TOOL_STORAGE =
            COMPONENTS.register("tool_storage", () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(LenientContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .build());

    /** The two enchantment plate slots (index 0 = primary, 1 = secondary). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> ENCHANTMENT_PLATES =
            COMPONENTS.register("enchantment_plates", () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(LenientContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .build());

    /**
     * The Support Tool slot (one item; every tier). While a Support Tool is in it, the
     * tool gets the shears, brush and flint and steel abilities (see ToolData#hasSupportTool).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> SUPPORT_TOOL =
            COMPONENTS.register("support_tool", () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(LenientContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .build());

    /** The torches in the light slot (type + count, up to 256). Present on every tier. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StoredTorches>> TORCHES =
            COMPONENTS.register("torches", () -> DataComponentType.<StoredTorches>builder()
                    .persistent(StoredTorches.CODEC)
                    .networkSynchronized(StoredTorches.STREAM_CODEC)
                    .build());

    /**
     * LEGACY: the old single-item light slot. Still registered so tools saved before the torch slot
     * load correctly; MultiToolItem#inventoryTick moves its torch into TORCHES (anything else goes back
     * to the player) and removes it.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> TOOL_LIGHT =
            COMPONENTS.register("tool_light", () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(LenientContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .build());

    /** The stored tool this Mega Tools tool last swapped to (for "Swap to last tool"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LastSwap>> LAST_SWAP =
            COMPONENTS.register("last_swap", () -> DataComponentType.<LastSwap>builder()
                    .persistent(LastSwap.CODEC)
                    .networkSynchronized(LastSwap.STREAM_CODEC)
                    .build());

    /** Custom swap rules set in the Swap Rules GUI: which blocks pre-select which stored tool. Per tool stack. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SwapRules>> SWAP_RULES =
            COMPONENTS.register("swap_rules", () -> DataComponentType.<SwapRules>builder()
                    .persistent(SwapRules.CODEC)
                    .networkSynchronized(SwapRules.STREAM_CODEC)
                    .build());

    /**
     * The default rules (client file config/megatools-default-rules.json) that were already copied into this
     * tool's SWAP_RULES. A default rule is copied once per tool; after that it is an ordinary rule the player
     * can change or remove, and this list keeps a removed one from coming back.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SwapRules>> APPLIED_DEFAULT_RULES =
            COMPONENTS.register("applied_default_rules", () -> DataComponentType.<SwapRules>builder()
                    .persistent(SwapRules.APPLIED_CODEC)
                    .networkSynchronized(SwapRules.APPLIED_STREAM_CODEC)
                    .build());

    /** Put on a tool that was auto-swapped into the hand; holds the Mega Tools tool to restore. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SwapOrigin>> SWAP_ORIGIN =
            COMPONENTS.register("swap_origin", () -> DataComponentType.<SwapOrigin>builder()
                    .persistent(SwapOrigin.CODEC)
                    .networkSynchronized(SwapOrigin.STREAM_CODEC)
                    .build());

    private ModDataComponents() {}

    public static void register(IEventBus eventBus)
    {
        COMPONENTS.register(eventBus);
    }
}
