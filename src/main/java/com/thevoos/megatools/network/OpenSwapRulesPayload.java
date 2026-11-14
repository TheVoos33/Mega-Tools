package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the "Custom Rule" button of the tool GUI was clicked. Open the held tool's Swap Rules GUI. */
public record OpenSwapRulesPayload() implements CustomPacketPayload
{
    public static final OpenSwapRulesPayload INSTANCE = new OpenSwapRulesPayload();
    public static final CustomPacketPayload.Type<OpenSwapRulesPayload> TYPE = new CustomPacketPayload.Type<>(MegaTools.id("open_swap_rules"));
    public static final StreamCodec<ByteBuf, OpenSwapRulesPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
