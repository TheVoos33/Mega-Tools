package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server -> client: the player with this entity id just broke a block or hit something with the
 * Mega Tool. Sent to players who can see them, so the poke shows up in third person.
 */
public record MegaToolPokePayload(int entityId) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<MegaToolPokePayload> TYPE = new CustomPacketPayload.Type<>(MegaTools.id("mega_tool_poke"));
    public static final StreamCodec<ByteBuf, MegaToolPokePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MegaToolPokePayload::entityId,
            MegaToolPokePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
