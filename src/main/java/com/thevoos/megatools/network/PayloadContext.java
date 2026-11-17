package com.thevoos.megatools.network;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Nullable;

/** The few things packet handlers need from Forge's NetworkEvent.Context. */
public record PayloadContext(NetworkEvent.Context context)
{
    /** Runs the task on the main thread (the server thread, or the client thread for packets to the client). */
    public void enqueueWork(Runnable task)
    {
        context.enqueueWork(task);
    }

    /** The sender on the server; the local player on the client. */
    public @Nullable Player player()
    {
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT)
            return ClientPayloadAccess.player();
        return context.getSender();
    }
}
