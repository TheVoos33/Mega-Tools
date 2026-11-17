package com.thevoos.megatools.network;

import net.minecraft.network.FriendlyByteBuf;

/** Client -> server: the "Custom Rule" button of the tool GUI was clicked. Open the held tool's Swap Rules GUI. */
public record OpenSwapRulesPayload() implements Payload
{
    public static final OpenSwapRulesPayload INSTANCE = new OpenSwapRulesPayload();

    @Override
    public void write(FriendlyByteBuf buf)
    {
        // no data
    }
}
