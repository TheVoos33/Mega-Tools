package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: R was pressed.
 * slot >= 0 : swap the Mega Tools tool for the stored tool in that storage slot (the client's pre-selection).
 * slot = -1 : swap back to the Mega Tools tool (ToolSwapper.SWAP_BACK).
 * The server re-validates everything.
 */
public record SwapToolPayload(int slot) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<SwapToolPayload> TYPE = new CustomPacketPayload.Type<>(MegaTools.id("swap_tool"));
    public static final StreamCodec<ByteBuf, SwapToolPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SwapToolPayload::slot,
            SwapToolPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
