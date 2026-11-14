package com.thevoos.megatools.tool;

import com.thevoos.megatools.Config;
import com.thevoos.megatools.block.ModBlocks;
import com.thevoos.megatools.block.ToolLightBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Held-tool lighting (server side).
 *
 * While a player holds a Mega Tools tool (either hand) with torches in its torch slot, ONE invisible
 * ToolLightBlock at full brightness (15, whatever the torch) follows them:
 *
 *  - At the head (like a held torch). If the head cell is taken (door, ladder, vines, tall grass...)
 *    the feet, the cell above the head, then the cells around head and feet are tried, the one in
 *    front of the player first, so the light never disappears in doorways or on ladders.
 *
 * Smoother movement: vanilla block light can only sit on whole blocks, so a moving light always
 * steps. Two things keep it from flickering:
 *  - Hysteresis: the light stays where it is until the eyes are clearly (CELL_HYSTERESIS) inside
 *    another block, so moving along a block edge doesn't make it jump back and forth. Every light move
 *    is a light-engine update on the server and on every client nearby, so this also saves work.
 *  - Overlap: a light that is no longer needed stays lit for FADE_TICKS after its replacement was
 *    placed, so there is never a tick with neither light (no dark flash).
 *
 * Dense vegetation (grass, flowers, leaves): when every spot near the head is taken, the current light
 * is kept as long as it is within KEEP_REACH of the head, instead of disappearing until a free spot
 * turns up; this used to make the light blink on and off while walking through tall grass.
 *
 * Shaders: players whose client reports an active shader pack (ShaderLightPayload) get no light block;
 * the shader's handheld light lights them smoothly instead (MultiToolItem#getLightEmission). Other
 * players then see no light around them, since shaders only light the local player's own hand.
 *
 * Freshly loaded chunks: a client applies the light data of chunks it just received a little later, in a
 * queue. A light that changes in that time (right after logging in, or after a teleport) is changed in a
 * chunk whose light isn't applied yet, so the client skips it and then applies the older data from the
 * chunk packet: a lit spot without a light block stays behind until something else updates it. So when a
 * light changes near chunks a player received less than RESYNC_WINDOW ago, the light of those chunks is
 * sent to that player again once the server has worked it out (see noteLightChange).
 *
 * Lights only ever replace air or still water (waterlogged), so no block is ever destroyed, and they
 * are placed without neighbour updates so observers and redstone don't react to a passing player.
 * Because the light is a real block, every player sees it and no client mod is needed.
 */
public final class ToolLightManager
{
    /** Send to clients, but no neighbour/shape updates (observers, redstone, falling blocks ignore it). */
    public static final int SET_FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    /** Every tool light uses the maximum light level. */
    public static final int LIGHT_LEVEL = 15;
    /** How long a replaced light stays on after its successor was placed. */
    private static final int FADE_TICKS = 4;
    /** The light only moves once the eyes are this far into another block. */
    private static final double CELL_HYSTERESIS = 0.3;
    /** When no spot near the head is free, the current light is kept while it is this close (blocks). */
    private static final int KEEP_REACH = 2;
    /** Light changes this long after a chunk was sent to a player are re-sent to them (ticks). */
    private static final int RESYNC_WINDOW = 300;
    /** Changes are collected this long before the light is re-sent (ticks). */
    private static final int RESYNC_DELAY = 5;
    /** The inside of a Nether portal frame is at most this wide and this high (vanilla PortalShape). */
    private static final int PORTAL_SIZE = 21;
    /** A level 15 light reaches this far (blocks). */
    private static final int LIGHT_REACH = LIGHT_LEVEL - 1;

    private record LightKey(ResourceKey<Level> dimension, BlockPos pos) {}

    /** The light one player currently uses (null = none, e.g. nothing free nearby). */
    private static final class PlayerLight
    {
        final ResourceKey<Level> dimension;
        BlockPos pos;

        PlayerLight(ResourceKey<Level> dimension)
        {
            this.dimension = dimension;
        }

        boolean uses(LightKey key)
        {
            return dimension == key.dimension() && key.pos().equals(pos);
        }
    }

    private static final Map<UUID, PlayerLight> PLAYERS = new HashMap<>();
    /** Lights no longer used by anyone, removed once the server tick reaches the stored value. */
    private static final Map<LightKey, Long> FADING = new HashMap<>();
    /** Players whose shader pack lights their held tool (no light block for them). */
    private static final Set<UUID> SHADER_LIGHT = new HashSet<>();
    /** Per player: chunks sent within the last RESYNC_WINDOW ticks (chunk -> server tick it was sent). */
    private static final Map<UUID, Long2LongOpenHashMap> RECENT_CHUNKS = new HashMap<>();
    /** Per player: chunks whose block light is re-sent soon. */
    private static final Map<UUID, Long2ObjectOpenHashMap<Resync>> RESYNC = new HashMap<>();

    /** Block light sections of one chunk to re-send to one player. */
    private static final class Resync
    {
        final ResourceKey<Level> dimension;
        final BitSet sections = new BitSet();
        long due;

        Resync(ResourceKey<Level> dimension)
        {
            this.dimension = dimension;
        }
    }

    private ToolLightManager() {}

    // =====================================================================================
    //  Per-tick update
    // =====================================================================================

    public static void onPlayerTick(PlayerTickEvent.Post event)
    {
        // Fake players (machines that "use" items) never get lights: nothing would ever remove them.
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer)
            return;

        // Runs for every player every tick: the cheap held-item check comes first.
        boolean lit = Config.HELD_LIGHT.get() && LightSources.isHoldingLight(player) && player.isAlive() && !player.isSpectator()
                && !SHADER_LIGHT.contains(player.getUUID());
        if (!lit)
        {
            if (!PLAYERS.isEmpty())
                release(player.server, PLAYERS.remove(player.getUUID()));
            return;
        }

        ServerLevel world = player.serverLevel();
        PlayerLight light = PLAYERS.get(player.getUUID());
        if (light == null || light.dimension != world.dimension())
        {
            // New dimension: start over (the old light fades out).
            release(player.server, light);
            light = new PlayerLight(world.dimension());
            PLAYERS.put(player.getUUID(), light);
        }

        BlockPos next = findPosition(world, player, player.getEyePosition(), light.pos);
        if (!Objects.equals(next, light.pos))
        {
            BlockPos old = light.pos;
            light.pos = next;
            if (next != null)
                place(world, next);   // the new light first...
            if (old != null)
                fadeOut(player.server, new LightKey(world.dimension(), old), player.server.getTickCount()); // ...then the old one fades
        }
    }

    /** Server tick: turn faded-out lights back into air/water. */
    public static void onServerTick(ServerTickEvent.Post event)
    {
        MinecraftServer server = event.getServer();
        if (!RECENT_CHUNKS.isEmpty() || !RESYNC.isEmpty())
            tickResync(server);
        if (FADING.isEmpty())
            return;
        long now = server.getTickCount();
        Iterator<Map.Entry<LightKey, Long>> it = FADING.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<LightKey, Long> entry = it.next();
            if (entry.getValue() > now)
                continue;
            it.remove();
            if (!isInUse(entry.getKey()))
                clearBlock(server, entry.getKey());
        }
    }

    // =====================================================================================
    //  Choosing the position
    // =====================================================================================

    /**
     * Head first (like a held torch), then feet, then above the head, then the cells around head and
     * feet (in front of the player first). Null if none is free.
     */
    private static BlockPos findPosition(ServerLevel world, ServerPlayer player, Vec3 eye, BlockPos current)
    {
        // Hysteresis: keep the current light while the eyes are still (almost) in its block.
        if (current != null && nearCell(current, eye, CELL_HYSTERESIS) && isToolLight(world.getBlockState(current)))
            return current;

        BlockPos head = BlockPos.containing(eye);
        if (isFree(world, head))
            return head;
        BlockPos feet = player.blockPosition();
        if (isFree(world, feet))
            return feet;
        BlockPos above = head.above();
        if (isFree(world, above))
            return above;
        Direction facing = player.getDirection();
        for (BlockPos center : new BlockPos[]{head, feet})
            for (int i = 0; i < 4; i++)
            {
                BlockPos pos = center.relative(around(facing, i));
                if (isFree(world, pos))
                    return pos;
            }

        // Nothing free (tall grass, flowers, leaves all around): keep the current light while it is still
        // close, rather than switching it off until a free spot turns up.
        if (current != null && isToolLight(world.getBlockState(current))
                && Math.max(Math.abs(current.getX() - head.getX()),
                            Math.max(Math.abs(current.getY() - head.getY()), Math.abs(current.getZ() - head.getZ()))) <= KEEP_REACH)
            return current;
        return null;
    }

    /** The 4 horizontal directions, the one the player faces first: facing, behind, right, left. */
    private static Direction around(Direction facing, int index)
    {
        return switch (index)
        {
            case 0 -> facing;
            case 1 -> facing.getOpposite();
            case 2 -> facing.getClockWise();
            default -> facing.getCounterClockWise();
        };
    }

    /** True if the eyes are inside {@code cell} grown by {@code margin} on every side (no allocation). */
    private static boolean nearCell(BlockPos cell, Vec3 eye, double margin)
    {
        return eye.x >= cell.getX() - margin && eye.x < cell.getX() + 1 + margin
                && eye.y >= cell.getY() - margin && eye.y < cell.getY() + 1 + margin
                && eye.z >= cell.getZ() - margin && eye.z < cell.getZ() + 1 + margin;
    }

    /** Air, a still water source, or an existing tool light (ours, fading, or another player's). */
    private static boolean isFree(ServerLevel world, BlockPos pos)
    {
        BlockState state = world.getBlockState(pos);
        return state.isAir() || isToolLight(state) || (state.is(Blocks.WATER) && state.getFluidState().isSource());
    }

    private static boolean isToolLight(BlockState state)
    {
        return state.is(ModBlocks.TOOL_LIGHT.get());
    }

    // =====================================================================================
    //  Placing / removing
    // =====================================================================================

    private static void place(ServerLevel world, BlockPos pos)
    {
        if (!FADING.isEmpty())
            FADING.remove(new LightKey(world.dimension(), pos)); // reused before it faded out
        BlockState state = world.getBlockState(pos);
        if (isToolLight(state))
        {
            if (state.getValue(ToolLightBlock.LEVEL) != LIGHT_LEVEL)
            {
                world.setBlock(pos, state.setValue(ToolLightBlock.LEVEL, LIGHT_LEVEL), SET_FLAGS);
                noteLightChange(world, pos);
            }
            return;
        }
        boolean inWater = state.is(Blocks.WATER);
        world.setBlock(pos, ModBlocks.TOOL_LIGHT.get().defaultBlockState()
                .setValue(ToolLightBlock.LEVEL, LIGHT_LEVEL)
                .setValue(ToolLightBlock.WATERLOGGED, inWater), SET_FLAGS);
        noteLightChange(world, pos);
    }

    /** No longer needed by this player: fade out unless someone else still uses it. */
    private static void fadeOut(MinecraftServer server, LightKey key, long now)
    {
        if (server != null && !isInUse(key))
            FADING.put(key, now + FADE_TICKS);
    }

    /**
     * The player leaves (logout, death, dimension change): remove the light right away instead of fading.
     * A fade needs further server ticks; when a singleplayer world is closed, players are logged out
     * after the last tick (and after ServerStoppingEvent), so a fading light was saved with the world.
     */
    private static void removeNow(MinecraftServer server, PlayerLight light)
    {
        if (light == null || server == null || light.pos == null)
            return;
        LightKey key = new LightKey(light.dimension, light.pos);
        light.pos = null;
        FADING.remove(key);
        if (!isInUse(key))
            clearBlock(server, key);
    }

    /** The player stops using their light (tool put away): it fades out. */
    private static void release(MinecraftServer server, PlayerLight light)
    {
        if (light == null || server == null || light.pos == null)
            return;
        BlockPos pos = light.pos;
        light.pos = null; // so isInUse no longer counts this player's light
        fadeOut(server, new LightKey(light.dimension, pos), server.getTickCount());
    }

    private static void clearBlock(MinecraftServer server, LightKey key)
    {
        ServerLevel world = server.getLevel(key.dimension());
        if (world == null || !world.isLoaded(key.pos()))
            return; // unloaded: the block's own scheduled tick removes it when the chunk loads
        BlockState state = world.getBlockState(key.pos());
        if (isToolLight(state))
        {
            world.setBlock(key.pos(), ToolLightBlock.emptyState(state), SET_FLAGS);
            noteLightChange(world, key.pos());
        }
    }

    /**
     * A fire was placed by a player (flint and steel, fire charge, the tool's Ignite ability). Fire inside an
     * obsidian frame makes a Nether portal only if the whole inside of the frame is AIR, and a tool light is
     * not air: standing in the frame with a lit tool, the portal never appeared. So a light that sits INSIDE
     * the frame the fire was placed in is removed first; lights anywhere else are left alone. NeoForge fires
     * this event before the fire looks for its portal frame; the player's light comes back on their next tick
     * (by then the frame is filled with portal blocks, so it is placed next to it).
     */
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event)
    {
        if (PLAYERS.isEmpty() && FADING.isEmpty())
            return;
        if (!(event.getLevel() instanceof ServerLevel world) || !(event.getPlacedBlock().getBlock() instanceof BaseFireBlock))
            return;
        BlockPos fire = event.getPos();

        // Cheap check first: is any light in one of the two upright planes a portal through the fire could be in?
        boolean alongX = false, alongZ = false;
        for (PlayerLight light : PLAYERS.values())
            if (light.dimension == world.dimension() && light.pos != null && inPortalReach(light.pos, fire))
            {
                alongX |= light.pos.getZ() == fire.getZ();
                alongZ |= light.pos.getX() == fire.getX();
            }
        for (LightKey key : FADING.keySet())
            if (key.dimension() == world.dimension() && inPortalReach(key.pos(), fire))
            {
                alongX |= key.pos().getZ() == fire.getZ();
                alongZ |= key.pos().getX() == fire.getX();
            }
        if (!alongX && !alongZ)
            return;

        Set<BlockPos> inFrame = new HashSet<>();
        if (alongX)
            lightsInFrame(world, fire, Direction.Axis.X, inFrame);
        if (alongZ)
            lightsInFrame(world, fire, Direction.Axis.Z, inFrame);
        if (inFrame.isEmpty())
            return;

        for (PlayerLight light : PLAYERS.values())
            if (light.dimension == world.dimension() && light.pos != null && inFrame.contains(light.pos))
                light.pos = null; // placed again on the player's next tick
        FADING.keySet().removeIf(key -> key.dimension() == world.dimension() && inFrame.contains(key.pos()));
        for (BlockPos pos : inFrame)
        {
            BlockState state = world.getBlockState(pos);
            world.setBlock(pos, ToolLightBlock.emptyState(state), SET_FLAGS);
            noteLightChange(world, pos);
        }
    }

    /** Same plane-independent box test: a portal's inside is at most PORTAL_SIZE wide and high. */
    private static boolean inPortalReach(BlockPos pos, BlockPos fire)
    {
        return Math.abs(pos.getX() - fire.getX()) < PORTAL_SIZE
                && Math.abs(pos.getY() - fire.getY()) < PORTAL_SIZE
                && Math.abs(pos.getZ() - fire.getZ()) < PORTAL_SIZE;
    }

    /**
     * Collects the tool lights inside the frame around {@code fire}, for a portal standing along {@code axis}.
     *
     * The inside of a frame is the area of open cells (air, fire, tool lights) connected to the fire in that
     * upright plane. It is walked cell by cell; as soon as the area is wider or higher than a portal can be,
     * the fire is not in a frame (open ground, a cave) and nothing is collected.
     */
    private static void lightsInFrame(ServerLevel world, BlockPos fire, Direction.Axis axis, Set<BlockPos> result)
    {
        Direction side = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction[] steps = {Direction.UP, Direction.DOWN, side, side.getOpposite()};
        int fireAlong = fire.get(axis);
        int minAlong = fireAlong, maxAlong = fireAlong, minY = fire.getY(), maxY = fire.getY();

        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> lights = new ArrayList<>();
        visited.add(fire);
        queue.add(fire);
        while (!queue.isEmpty())
        {
            BlockPos pos = queue.poll();
            for (Direction step : steps)
            {
                BlockPos next = pos.relative(step);
                if (visited.contains(next))
                    continue;
                BlockState state = world.getBlockState(next);
                boolean light = isToolLight(state);
                if (!light && !state.isAir() && !state.is(BlockTags.FIRE) && !state.is(Blocks.NETHER_PORTAL))
                    continue; // frame (or anything else solid): the area ends here
                minAlong = Math.min(minAlong, next.get(axis));
                maxAlong = Math.max(maxAlong, next.get(axis));
                minY = Math.min(minY, next.getY());
                maxY = Math.max(maxY, next.getY());
                if (maxAlong - minAlong >= PORTAL_SIZE || maxY - minY >= PORTAL_SIZE)
                    return; // too big for a portal: not a frame
                visited.add(next);
                queue.add(next);
                if (light)
                    lights.add(next);
            }
        }
        result.addAll(lights);
    }

    private static boolean isInUse(LightKey key)
    {
        for (PlayerLight light : PLAYERS.values())
            if (light.uses(key))
                return true;
        return false;
    }

    /** Asked by ToolLightBlock's self-check: is this light still used or fading out? */
    public static boolean isActiveLight(ServerLevel world, BlockPos pos)
    {
        LightKey key = new LightKey(world.dimension(), pos);
        return FADING.containsKey(key) || isInUse(key);
    }

    // =====================================================================================
    //  Re-sending light to clients that are still loading chunks
    // =====================================================================================

    /** Server: a chunk was sent to a player. */
    public static void onChunkSent(ChunkWatchEvent.Sent event)
    {
        RECENT_CHUNKS.computeIfAbsent(event.getPlayer().getUUID(), id -> new Long2LongOpenHashMap())
                .put(event.getPos().toLong(), event.getLevel().getServer().getTickCount());
    }

    /** Server: a player no longer has a chunk (out of range or other dimension). */
    public static void onChunkUnwatch(ChunkWatchEvent.UnWatch event)
    {
        UUID id = event.getPlayer().getUUID();
        long chunk = event.getPos().toLong();
        Long2LongOpenHashMap recent = RECENT_CHUNKS.get(id);
        if (recent != null)
            recent.remove(chunk);
        Long2ObjectOpenHashMap<Resync> pending = RESYNC.get(id);
        if (pending != null)
            pending.remove(chunk);
    }

    /**
     * A light at {@code pos} was placed, changed or removed. Players who received one of the chunks it
     * reaches less than RESYNC_WINDOW ago get the block light of those chunks again shortly (see class doc).
     */
    public static void noteLightChange(ServerLevel world, BlockPos pos)
    {
        if (RECENT_CHUNKS.isEmpty())
            return;
        MinecraftServer server = world.getServer();
        long now = server.getTickCount();
        int minX = SectionPos.blockToSectionCoord(pos.getX() - LIGHT_REACH), maxX = SectionPos.blockToSectionCoord(pos.getX() + LIGHT_REACH);
        int minZ = SectionPos.blockToSectionCoord(pos.getZ() - LIGHT_REACH), maxZ = SectionPos.blockToSectionCoord(pos.getZ() + LIGHT_REACH);

        for (Map.Entry<UUID, Long2LongOpenHashMap> entry : RECENT_CHUNKS.entrySet())
        {
            if (!anyRecent(entry.getValue(), minX, maxX, minZ, maxZ, now))
                continue;
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || player.serverLevel() != world)
                continue;

            LevelLightEngine engine = world.getLightEngine();
            int minIndex = Math.max(0, SectionPos.blockToSectionCoord(pos.getY() - LIGHT_REACH) - engine.getMinLightSection());
            int maxIndex = Math.min(engine.getLightSectionCount() - 1,
                    SectionPos.blockToSectionCoord(pos.getY() + LIGHT_REACH) - engine.getMinLightSection());
            if (minIndex > maxIndex)
                continue;
            Long2ObjectOpenHashMap<Resync> pending = RESYNC.computeIfAbsent(entry.getKey(), id -> new Long2ObjectOpenHashMap<>());
            for (int x = minX; x <= maxX; x++)
                for (int z = minZ; z <= maxZ; z++)
                {
                    if (!player.getChunkTrackingView().contains(x, z))
                        continue;
                    Resync resync = pending.computeIfAbsent(ChunkPos.asLong(x, z), c -> new Resync(world.dimension()));
                    resync.sections.set(minIndex, maxIndex + 1);
                    resync.due = now + RESYNC_DELAY; // later changes push it back, so the last one is included
                }
        }
    }

    private static boolean anyRecent(Long2LongOpenHashMap recent, int minX, int maxX, int minZ, int maxZ, long now)
    {
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++)
            {
                long sent = recent.getOrDefault(ChunkPos.asLong(x, z), Long.MIN_VALUE);
                if (sent != Long.MIN_VALUE && now - sent <= RESYNC_WINDOW)
                    return true;
            }
        return false;
    }

    /** Server tick: forget old chunk sends and send the light that is due. */
    private static void tickResync(MinecraftServer server)
    {
        long now = server.getTickCount();
        if (now % 20 == 0)
        {
            RECENT_CHUNKS.values().forEach(recent -> recent.long2LongEntrySet().removeIf(e -> now - e.getLongValue() > RESYNC_WINDOW));
            RECENT_CHUNKS.values().removeIf(Long2LongOpenHashMap::isEmpty);
        }

        Iterator<Map.Entry<UUID, Long2ObjectOpenHashMap<Resync>>> players = RESYNC.entrySet().iterator();
        while (players.hasNext())
        {
            Map.Entry<UUID, Long2ObjectOpenHashMap<Resync>> entry = players.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null)
            {
                players.remove();
                continue;
            }
            var chunks = entry.getValue().long2ObjectEntrySet().iterator();
            while (chunks.hasNext())
            {
                var chunk = chunks.next();
                Resync resync = chunk.getValue();
                if (resync.due > now)
                    continue;
                ChunkPos pos = new ChunkPos(chunk.getLongKey()); // read before remove(): the entry is invalid after it
                chunks.remove();
                ServerLevel world = server.getLevel(resync.dimension);
                if (world == null || player.serverLevel() != world)
                    continue;
                // The server works out light on its own thread: send once the light changes queued so far are done.
                world.getChunkSource().getLightEngine().waitForPendingTasks(pos.x, pos.z).thenRunAsync(() ->
                {
                    if (!player.hasDisconnected() && player.serverLevel() == world
                            && player.getChunkTrackingView().contains(pos))
                        player.connection.send(new ClientboundLightUpdatePacket(pos, world.getLightEngine(), new BitSet(), resync.sections));
                }, server);
            }
            if (entry.getValue().isEmpty())
                players.remove();
        }
    }

    // =====================================================================================
    //  Cleanup events
    // =====================================================================================

    /** Server: the player's client reports whether a shader pack lights the held tool (ShaderLightPayload). */
    public static void setShaderLight(ServerPlayer player, boolean shaderActive)
    {
        if (shaderActive)
            SHADER_LIGHT.add(player.getUUID());
        else
            SHADER_LIGHT.remove(player.getUUID());
        // onPlayerTick removes or re-places this player's light on the next tick.
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        SHADER_LIGHT.remove(event.getEntity().getUUID());
        RECENT_CHUNKS.remove(event.getEntity().getUUID());
        RESYNC.remove(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player)
            removeNow(player.server, PLAYERS.remove(player.getUUID()));
    }

    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            removeNow(player.server, PLAYERS.remove(player.getUUID()));
    }

    public static void onDeath(LivingDeathEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer player)
            removeNow(player.server, PLAYERS.remove(player.getUUID()));
    }

    /**
     * After the server has fully stopped: forget everything. In singleplayer the JVM (and these static maps)
     * outlives the world; a stale entry kept a saved light "in use" in the next world that was opened.
     */
    public static void onServerStopped(ServerStoppedEvent event)
    {
        PLAYERS.clear();
        FADING.clear();
        SHADER_LIGHT.clear();
        RECENT_CHUNKS.clear();
        RESYNC.clear();
    }

    /** Remove every light right away before the world saves, and forget them (the maps are static). */
    public static void onServerStopping(ServerStoppingEvent event)
    {
        MinecraftServer server = event.getServer();
        List<LightKey> all = new ArrayList<>(FADING.keySet());
        for (PlayerLight light : PLAYERS.values())
            if (light.pos != null)
                all.add(new LightKey(light.dimension, light.pos));
        PLAYERS.clear();
        SHADER_LIGHT.clear();
        FADING.clear();
        for (LightKey key : all)
            clearBlock(server, key);
    }
}
