package com.thevoos.megatools.network;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Client -> server: whether a shader pack is active on this client. While it is, the shader lights the
 * held tool itself (MultiToolItem#getLightEmission), so the server stops placing light blocks for this
 * player (ToolLightManager). Sent on join and whenever it changes.
 */
public record ShaderLightPayload(boolean shaderActive) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf)
    {
        buf.writeBoolean(shaderActive);
    }

    public static ShaderLightPayload read(FriendlyByteBuf buf)
    {
        return new ShaderLightPayload(buf.readBoolean());
    }
}
