package com.thevoos.megatools.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Client -> server: R was pressed.
 * slot >= 0 : swap the Mega Tools tool for the stored tool in that storage slot (the client's pre-selection).
 * slot = -1 : swap back to the Mega Tools tool (ToolSwapper.SWAP_BACK).
 * The server re-validates everything.
 */
public record SwapToolPayload(int slot) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf)
    {
        buf.writeVarInt(slot);
    }

    public static SwapToolPayload read(FriendlyByteBuf buf)
    {
        return new SwapToolPayload(buf.readVarInt());
    }
}
