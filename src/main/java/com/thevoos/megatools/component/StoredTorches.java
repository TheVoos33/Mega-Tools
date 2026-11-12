package com.thevoos.megatools.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * The torches inside a tool's light slot: which torch (a 1-count stack) and how many (1-256).
 *
 * Stored as type + count instead of a normal ItemStack because item stacks can't be saved with more
 * than 99 items, and the slot holds up to 256.
 */
public record StoredTorches(ItemStack type, int count)
{
    public static final int MAX = 256;

    public static final Codec<StoredTorches> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("type").forGetter(StoredTorches::type),
            Codec.intRange(1, MAX).fieldOf("count").forGetter(StoredTorches::count)
    ).apply(instance, StoredTorches::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StoredTorches> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, StoredTorches::type,
            ByteBufCodecs.VAR_INT, StoredTorches::count,
            StoredTorches::new);

    /** The torches as one (possibly over-sized, up to 256) stack, e.g. for the GUI slot. */
    public ItemStack asStack()
    {
        return type.copyWithCount(count);
    }

    // ItemStack has identity equality; compare contents (needed for syncing and stack merging).
    @Override
    public boolean equals(Object o)
    {
        return o instanceof StoredTorches other && count == other.count && ItemStack.isSameItemSameComponents(type, other.type);
    }

    @Override
    public int hashCode()
    {
        return 31 * ItemStack.hashItemAndComponents(type) + count;
    }
}
