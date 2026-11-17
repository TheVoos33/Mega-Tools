package com.thevoos.megatools.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * The stored tool a Mega Tools tool last swapped to (auto-swap / GUI quick swap): its storage slot and item.
 * "Swap to last tool" (Space + R) uses it no matter what block the player looks at. The slot is tried
 * first; if the tool was moved, the first stored tool of the same item is used.
 */
public record LastSwap(int slot, Item item)
{
    /**
     * By id, and an unknown id (its mod was removed) loads as air instead of failing: a component that
     * can't be read can take the whole tool with it. Air never matches a stored tool (see ToolSwapper).
     */
    private static final Codec<Item> LENIENT_ITEM = ResourceLocation.CODEC.xmap(BuiltInRegistries.ITEM::get, BuiltInRegistries.ITEM::getKey);

    public static final Codec<LastSwap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("slot").forGetter(LastSwap::slot),
            LENIENT_ITEM.fieldOf("item").forGetter(LastSwap::item)
    ).apply(instance, LastSwap::new));

}
