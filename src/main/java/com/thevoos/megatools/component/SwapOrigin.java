package com.thevoos.megatools.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Attached to a tool that was pulled OUT of a Mega Tools tool's wrench storage by auto-swap (R key).
 *
 * tool - the complete Mega Tools tool (with its remaining storage, plates, settings) that was in hand
 * slot - the storage slot the swapped tool came from, so it goes back to the same place
 *
 * Keeping the original tool INSIDE the swapped tool means nothing can be lost: if the player drops
 * the swapped tool, dies, or stores it in a chest, the Mega Tools tool travels with it and pressing R
 * while holding it restores it. (Breakage is handled by ToolSwapper#onItemDestroyed.)
 *
 * Note: never mutate {@link #tool()} - always copy() it first, data components must be immutable.
 */
public record SwapOrigin(ItemStack tool, int slot)
{
    public static final Codec<SwapOrigin> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("tool").forGetter(SwapOrigin::tool),
            Codec.INT.fieldOf("slot").forGetter(SwapOrigin::slot)
    ).apply(instance, SwapOrigin::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwapOrigin> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, SwapOrigin::tool,
            ByteBufCodecs.VAR_INT, SwapOrigin::slot,
            SwapOrigin::new);

    // ItemStack has identity equality, so equals/hashCode are overridden to compare contents.
    // Component equality matters for stack merging and inventory syncing.
    @Override
    public boolean equals(Object o)
    {
        return o instanceof SwapOrigin other && slot == other.slot && ItemStack.matches(tool, other.tool);
    }

    @Override
    public int hashCode()
    {
        return 31 * ItemStack.hashItemAndComponents(tool) + slot;
    }
}
