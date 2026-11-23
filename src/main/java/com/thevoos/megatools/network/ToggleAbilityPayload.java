package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: a toggle hotkey was pressed. {@code bit} = ToolSettings bit = menu button id (see AbilityToggles). */
public record ToggleAbilityPayload(int bit) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ToggleAbilityPayload> TYPE = new CustomPacketPayload.Type<>(MegaTools.id("toggle_ability"));
    public static final StreamCodec<ByteBuf, ToggleAbilityPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ToggleAbilityPayload::bit,
            ToggleAbilityPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
