package com.thevoos.megatools.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.PacketDistributor;

/** Sending packets (same calls as NeoForge's PacketDistributor, on the Mega Tools channel). */
public final class ModPackets
{
    private ModPackets() {}

    public static void sendToServer(Payload payload)
    {
        ModNetworking.CHANNEL.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, Payload payload)
    {
        ModNetworking.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), payload);
    }

    public static void sendToPlayersTrackingEntity(Entity entity, Payload payload)
    {
        ModNetworking.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), payload);
    }
}
