package com.thevoos.megatools.network;

import net.minecraft.network.FriendlyByteBuf;

/** Client -> server: the player edited the title (tool name) in the GUI. Blank = reset to default name. */
public record RenameToolPayload(String name) implements Payload
{
    public static final int MAX_LENGTH = 50; // same limit as the anvil

    @Override
    public void write(FriendlyByteBuf buf)
    {
        buf.writeUtf(name, 256);
    }

    public static RenameToolPayload read(FriendlyByteBuf buf)
    {
        return new RenameToolPayload(buf.readUtf(256));
    }
}
