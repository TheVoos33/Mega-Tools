package com.thevoos.megatools.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Server -> client: the player with this entity id just broke a block or hit something with the
 * Mega Tool. Sent to players who can see them, so the poke shows up in third person.
 */
public record MegaToolPokePayload(int entityId) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf)
    {
        buf.writeVarInt(entityId);
    }

    public static MegaToolPokePayload read(FriendlyByteBuf buf)
    {
        return new MegaToolPokePayload(buf.readVarInt());
    }
}
