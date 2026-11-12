package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: Shift + R was pressed. Toggle between the primary and secondary enchantment plate. */
public record SwitchPlatePayload() implements CustomPacketPayload
{
    public static final SwitchPlatePayload INSTANCE = new SwitchPlatePayload();
    public static final CustomPacketPayload.Type<SwitchPlatePayload> TYPE = new CustomPacketPayload.Type<>(MegaTools.id("switch_plate"));
    public static final StreamCodec<ByteBuf, SwitchPlatePayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
