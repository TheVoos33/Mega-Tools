package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the player edited the title (tool name) in the GUI. Blank = reset to default name. */
public record RenameToolPayload(String name) implements CustomPacketPayload
{
    public static final int MAX_LENGTH = 50; // same limit as the anvil

    public static final CustomPacketPayload.Type<RenameToolPayload> TYPE = new CustomPacketPayload.Type<>(MegaTools.id("rename_tool"));
    public static final StreamCodec<ByteBuf, RenameToolPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), RenameToolPayload::name,
            RenameToolPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
