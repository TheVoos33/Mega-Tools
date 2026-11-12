package com.thevoos.megatools.tool;

import com.thevoos.megatools.block.ModBlocks;
import com.thevoos.megatools.block.ToolLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Held-tool lighting (server side).
 *
 * Every tick, for each player holding a Mega Tools tool (either hand) with a light source in its
 * light slot, an invisible ToolLightBlock with that light level is kept at the player's head
 * (or feet, or just above the head if those are occupied). When the player moves, the old light is
 * removed and a new one placed; when the tool is put away, the light source is taken out, or the
 * player dies / leaves / changes dimension, the light is removed.
 *
 * Lights only ever replace air or still water (waterlogged), so no block is ever destroyed, and they
 * are placed without neighbour updates so observers and redstone don't react to a passing player.
 * Because the light is a real block, every player sees it and no client mod is needed.
 */
public final class ToolLightManager
{
    /** Send to clients, but no neighbour/shape updates (observers, redstone, falling blocks ignore it). */
    public static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private record ActiveLight(ResourceKey<Level> dimension, BlockPos pos, int level) {}

    private static final Map<UUID, ActiveLight> ACTIVE = new HashMap<>();

    private ToolLightManager() {}

    // =====================================================================================
    //  Per-tick update
    // =====================================================================================

    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        ActiveLight current = ACTIVE.get(player.getUUID());
        int level = player.isAlive() && !player.isSpectator() ? LightSources.heldLevel(player) : 0;
        if (level <= 0)
        {
            removeLight(player.server, player.getUUID());
            return;
        }

        ServerLevel world = player.serverLevel();
        BlockPos target = findTarget(world, player, current);
        if (current != null && current.dimension() == world.dimension()
                && current.pos().equals(target) && current.level() == level)
            return; // nothing changed this tick

        removeLight(player.server, player.getUUID());
        if (target != null)
        {
            boolean inWater = world.getBlockState(target).is(Blocks.WATER);
            world.setBlock(target, ModBlocks.TOOL_LIGHT.get().defaultBlockState()
                    .setValue(ToolLightBlock.LEVEL, level)
                    .setValue(ToolLightBlock.WATERLOGGED, inWater), SET_FLAGS);
            ACTIVE.put(player.getUUID(), new ActiveLight(world.dimension(), target, level));
        }
    }

    /** Head first (like a held torch), then feet, then above the head. Null if none is free. */
    private static BlockPos findTarget(ServerLevel world, ServerPlayer player, ActiveLight current)
    {
        BlockPos head = BlockPos.containing(player.getEyePosition());
        BlockPos[] candidates = {head, player.blockPosition(), head.above()};
        for (BlockPos pos : candidates)
        {
            // Our own current light counts as free, so a standing player keeps the same spot.
            if (current != null && current.dimension() == world.dimension() && current.pos().equals(pos)
                    && world.getBlockState(pos).is(ModBlocks.TOOL_LIGHT.get()))
                return pos;
            if (canPlaceAt(world.getBlockState(pos)))
                return pos;
        }
        return null;
    }

    /** Only air or a still water source; never replaces anything else. */
    private static boolean canPlaceAt(BlockState state)
    {
        return state.isAir() || (state.is(Blocks.WATER) && state.getFluidState().isSource());
    }

    // =====================================================================================
    //  Removal
    // =====================================================================================

    private static void removeLight(MinecraftServer server, UUID playerId)
    {
        ActiveLight light = ACTIVE.remove(playerId);
        if (light == null || server == null)
            return;
        // Another player may be standing in the same spot with their own light: leave it to them.
        if (ACTIVE.values().stream().anyMatch(other -> other.dimension() == light.dimension() && other.pos().equals(light.pos())))
            return;

        ServerLevel world = server.getLevel(light.dimension());
        if (world == null || !world.isLoaded(light.pos()))
            return; // unloaded: the block's own scheduled tick removes it when the chunk loads
        BlockState state = world.getBlockState(light.pos());
        if (state.is(ModBlocks.TOOL_LIGHT.get()))
            world.setBlock(light.pos(), ToolLightBlock.emptyState(state), SET_FLAGS);
    }

    /** Asked by ToolLightBlock's self-check: is any player's light still at this position? */
    public static boolean isActiveLight(ServerLevel world, BlockPos pos)
    {
        return ACTIVE.values().stream().anyMatch(light -> light.dimension() == world.dimension() && light.pos().equals(pos));
    }

    // =====================================================================================
    //  Cleanup events
    // =====================================================================================

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            removeLight(player.server, player.getUUID());
    }

    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            removeLight(player.server, player.getUUID());
    }

    public static void onDeath(LivingDeathEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            removeLight(player.server, player.getUUID());
    }

    /** Remove every light before the world saves, and forget them (the map is static). */
    public static void onServerStopping(ServerStoppingEvent event)
    {
        for (UUID id : ACTIVE.keySet().toArray(new UUID[0]))
            removeLight(event.getServer(), id);
        ACTIVE.clear();
    }
}
