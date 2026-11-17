package com.thevoos.megatools.network;

import net.minecraft.network.FriendlyByteBuf;

/** Client -> server: Ctrl + R was pressed while holding a Mega Tools tool. Open its GUI. */
public record OpenToolGuiPayload() implements Payload
{
    public static final OpenToolGuiPayload INSTANCE = new OpenToolGuiPayload();

    @Override
    public void write(FriendlyByteBuf buf)
    {
        // no data
    }
}
