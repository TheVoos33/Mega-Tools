package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: "my Mega Tool breaks the block I'm digging, now". Sent by MegaToolMining right
 * before vanilla's dig packet, so the server grants exactly that break and both sides stay in sync.
 */
public record MegaToolBreakPayload() implements CustomPacketPayload
{
    public static final MegaToolBreakPayload INSTANCE = new MegaToolBreakPayload();
    public static final CustomPacketPayload.Type<MegaToolBreakPayload> TYPE = new CustomPacketPayload.Type<>(MegaTools.id("mega_tool_break"));
    public static final StreamCodec<ByteBuf, MegaToolBreakPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
