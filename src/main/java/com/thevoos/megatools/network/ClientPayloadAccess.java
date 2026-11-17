package com.thevoos.megatools.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Client-only access, kept in its own class so the dedicated server never loads client classes. */
final class ClientPayloadAccess
{
    private ClientPayloadAccess() {}

    static Player player()
    {
        return Minecraft.getInstance().player;
    }
}
