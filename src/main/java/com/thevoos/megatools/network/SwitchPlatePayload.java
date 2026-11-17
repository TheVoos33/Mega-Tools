package com.thevoos.megatools.network;

import net.minecraft.network.FriendlyByteBuf;

/** Client -> server: Shift + R was pressed. Toggle between the primary and secondary enchantment plate. */
public record SwitchPlatePayload() implements Payload
{
    public static final SwitchPlatePayload INSTANCE = new SwitchPlatePayload();

    @Override
    public void write(FriendlyByteBuf buf)
    {
        // no data
    }
}
