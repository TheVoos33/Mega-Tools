package com.thevoos.megatools.network;

import net.minecraft.network.FriendlyByteBuf;

/** Client -> server: a toggle hotkey was pressed. {@code bit} = ToolSettings bit = menu button id (see AbilityToggles). */
public record ToggleAbilityPayload(int bit) implements Payload
{
    @Override
    public void write(FriendlyByteBuf buf)
    {
        buf.writeVarInt(bit);
    }

    public static ToggleAbilityPayload read(FriendlyByteBuf buf)
    {
        return new ToggleAbilityPayload(buf.readVarInt());
    }
}
