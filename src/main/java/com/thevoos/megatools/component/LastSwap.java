package com.thevoos.megatools.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
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
    private static final Codec<Item> LENIENT_ITEM = Identifier.CODEC.xmap(BuiltInRegistries.ITEM::getValue, BuiltInRegistries.ITEM::getKey);

    public static final Codec<LastSwap> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("slot").forGetter(LastSwap::slot),
            LENIENT_ITEM.fieldOf("item").forGetter(LastSwap::item)
    ).apply(instance, LastSwap::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, LastSwap> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LastSwap::slot,
            ByteBufCodecs.registry(Registries.ITEM), LastSwap::item,
            LastSwap::new);
}
