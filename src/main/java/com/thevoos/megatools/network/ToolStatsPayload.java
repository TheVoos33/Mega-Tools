package com.thevoos.megatools.network;

import com.thevoos.megatools.MegaTools;
import com.thevoos.megatools.StartupConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> client on login: the server's tool stats (config/megatools-startup.toml). Startup configs
 * aren't synced (the items are built before a server is joined), so a client with different stats
 * would show wrong durability and mine at a different speed than the server (ghost blocks). The client
 * compares and warns the player if they differ. Uses only common classes, so it is safe on servers.
 */
public record ToolStatsPayload(List<Double> stats) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<ToolStatsPayload> TYPE = new CustomPacketPayload.Type<>(MegaTools.id("tool_stats"));
    public static final StreamCodec<ByteBuf, ToolStatsPayload> STREAM_CODEC =
            ByteBufCodecs.DOUBLE.apply(ByteBufCodecs.list()).map(ToolStatsPayload::new, ToolStatsPayload::stats);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return TYPE;
    }

    /** This side's stats, in a fixed order. */
    public static ToolStatsPayload local()
    {
        List<Double> stats = new ArrayList<>();
        for (StartupConfig.ToolValues tool : StartupConfig.ALL)
        {
            stats.add(tool.attackDamage.get());
            stats.add(tool.attackSpeed.get());
            stats.add((double) tool.durability.get());
            stats.add(tool.miningSpeedMultiplier != null ? tool.miningSpeedMultiplier.get() : -1.0);
        }
        return new ToolStatsPayload(stats);
    }

    /** Server: send our stats to every player who joins. */
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            PacketDistributor.sendToPlayer(player, local());
    }

    /** Client: warn if the server's stats differ from ours. */
    public static void handle(ToolStatsPayload server, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            List<Double> mine = local().stats();
            boolean same = mine.size() == server.stats().size();
            for (int i = 0; same && i < mine.size(); i++)
                same = Math.abs(mine.get(i) - server.stats().get(i)) < 1.0E-6;
            if (!same)
                context.player().displayClientMessage(
                        Component.translatable("message.megatools.tool_stats_mismatch").withStyle(ChatFormatting.YELLOW), false);
        });
    }
}
