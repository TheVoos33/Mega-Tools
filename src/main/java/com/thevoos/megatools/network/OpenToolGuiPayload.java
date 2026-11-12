package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: Ctrl + R was pressed while holding a Mega Tools tool. Open its GUI. */
public record OpenToolGuiPayload() implements CustomPacketPayload
{
    public static final OpenToolGuiPayload INSTANCE = new OpenToolGuiPayload();
    public static final CustomPacketPayload.Type<OpenToolGuiPayload> TYPE = new CustomPacketPayload.Type<>(MegaTools.id("open_tool_gui"));
    public static final StreamCodec<ByteBuf, OpenToolGuiPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
