package com.thevoos.megatools.network;

import net.minecraft.network.FriendlyByteBuf;

/** A Mega Tools packet: written to the buffer by write(), read back by the reader given to ModNetworking. */
public interface Payload
{
    void write(FriendlyByteBuf buf);
}
