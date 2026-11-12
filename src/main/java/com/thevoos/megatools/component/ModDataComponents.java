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

    /** Wrench storage contents (8 / 16 / 24 slots depending on tier). Reuses vanilla's container format. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> TOOL_STORAGE =
            COMPONENTS.register("tool_storage", () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
                    .build());

    /** The two enchantment plate slots (index 0 = primary, 1 = secondary). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> ENCHANTMENT_PLATES =
            COMPONENTS.register("enchantment_plates", () -> DataComponentType.<ItemContainerContents>builder()
                    .persistent(ItemContainerContents.CODEC)
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
                    .persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC)
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
