package com.thevoos.megatools.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Client -> server: "my Mega Tool breaks the block I'm digging, now". Sent by MegaToolMining right
 * before vanilla's dig packet, so the server grants exactly that break and both sides stay in sync.
 */
public record MegaToolBreakPayload() implements Payload
{
    public static final MegaToolBreakPayload INSTANCE = new MegaToolBreakPayload();

    @Override
    public void write(FriendlyByteBuf buf)
    {
        // no data
    }
}
