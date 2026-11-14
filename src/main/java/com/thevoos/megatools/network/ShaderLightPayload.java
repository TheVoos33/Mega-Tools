package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server: whether a shader pack is active on this client. While it is, the shader lights the
 * held tool itself (MultiToolItem#getLightEmission), so the server stops placing light blocks for this
 * player (ToolLightManager). Sent on join and whenever it changes.
 */
public record ShaderLightPayload(boolean shaderActive) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ShaderLightPayload> TYPE = new CustomPacketPayload.Type<>(MegaTools.id("shader_light"));
    public static final StreamCodec<ByteBuf, ShaderLightPayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(ShaderLightPayload::new, ShaderLightPayload::shaderActive);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }
}
