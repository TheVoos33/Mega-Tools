package com.thevoos.megatools.tool;

import com.thevoos.megatools.Config;
import com.thevoos.megatools.component.ToolSettings;
import com.thevoos.megatools.items.ModTags;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.network.MegaToolBreakPayload;
import com.thevoos.megatools.network.MegaToolPokePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Mega Tool mining rules:
 *  - Any breakable block breaks in one hit (hardness doesn't matter; hardness -1 blocks like bedrock
 *    stay unbreakable, vanilla never asks for a speed for those).
 *  - Flying / swimming / underwater penalties are ignored (we override the FINAL speed).
 *  - While left click is HELD, a delay (Config: megaToolHoldDelayTicks) separates breaks. That delay
 *    is the "Delayed Break" toggle (ON by default); turning it OFF removes the delay.
 *  - Every NEW click breaks a block immediately.
 *  - Zero-hardness blocks (grass, flowers, torches...) always break instantly, like with any tool.
 *  - Blocks in the #megatools:mega_tool_unbreakable tag can never be broken with the Mega Tool.
 *
 * WHO DECIDES: the client. It knows about held vs. fresh clicks and is the side that shows the break
 * (sound, particles). When it decides a block breaks now, it sends MegaToolBreakPayload right before
 * vanilla's dig packet; the server then grants exactly that one break. The server never runs its own
 * timer, so both sides always break the same block at the same moment (no silent, late breaks).
 *
 * HOW: PlayerEvent.BreakSpeed fires on both sides whenever vanilla computes dig progress. We return a
 * huge speed when a break is permitted (vanilla then finishes the block in the same call chain) and 0
 * while the hold delay runs.
 */
public final class MegaToolMining
{
    private static final float INSTANT_SPEED = 1.0E7F;
    private static final long NEVER = Long.MIN_VALUE / 4;
    /** How long (server ticks) a break granted by MegaToolBreakPayload stays valid. */
    private static final int SERVER_PERMIT_TICKS = 5;

    // ---- client-side state (only touched on the client thread) ----
    private static long clientLastBreak = NEVER;
    private static long clientFreshUntil = NEVER;
    /** Set by MegaToolsClient: is the attack button held? (This class can't touch client classes.) */
    private static BooleanSupplier clientAttackDown = () -> false;
    /**
     * True only while the client game tick runs. Vanilla mining always happens inside the tick; HUD
     * mods like Jade ask BreakSpeed while drawing the screen. Only in-tick calls are real mining, so
     * only they may use up a click, start the delay, play the jab or tell the server to break.
     */
    private static boolean clientInTick;

    // ---- server-side state per player: break granted by the client until this game tick ----
    private static final Map<UUID, Long> SERVER_PERMIT_UNTIL = new HashMap<>();

    private MegaToolMining() {}

    public static void setClientInTick(boolean inTick) { clientInTick = inTick; }

    public static void setClientAttackDownSupplier(BooleanSupplier supplier) { clientAttackDown = supplier; }

    public static boolean isMegaTool(ItemStack stack)
    {
        return stack.getItem() instanceof MultiToolItem multiTool && multiTool.getTier().isMega();
    }

    // =====================================================================================
    //  Break speed (both sides)
    // =====================================================================================

    public static void onBreakSpeed(PlayerEvent.BreakSpeed event)
    {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (!isMegaTool(held) || MultiToolItem.isBroken(held))
            return; // a broken tool is handled by BrokenToolHandler (no mining at all)

        BlockState state = event.getState();
        if (state.is(ModTags.MEGA_TOOL_UNBREAKABLE))
        {
            event.setNewSpeed(0.0F); // tagged blocks: no progress at all
            return;
        }
        if (player.getAbilities().instabuild)
            return; // creative already breaks instantly

        boolean client = player.level().isClientSide();

        // Zero-hardness blocks (short grass, flowers...) break instantly with any tool. They must never
        // get speed 0: vanilla computes progress as speed / hardness, and 0 / 0 is NaN, which made such
        // blocks impossible to break while the delay was running. They don't use up the delay.
        if (isZeroHardness(player, state, event.getPosition()))
        {
            if (client && isClientMining(player))
                MegaToolPoke.triggerLocal();
            event.setNewSpeed(INSTANT_SPEED);
            return;
        }

        if (!client)
        {
            // Server: only the break the client just announced (or no delay at all).
            boolean granted = !ToolData.hasFlag(held, ToolSettings.DELAYED_BREAK)
                    || player.level().getGameTime() <= SERVER_PERMIT_UNTIL.getOrDefault(player.getUUID(), NEVER);
            event.setNewSpeed(granted ? INSTANT_SPEED : 0.0F);
            return;
        }

        // ---------------- client: decide ----------------
        boolean delayed = ToolData.hasFlag(held, ToolSettings.DELAYED_BREAK);
        long now = player.level().getGameTime();
        int delay = Config.MEGA_TOOL_HOLD_DELAY_TICKS.get();
        boolean permitted = !delayed
                || now <= clientFreshUntil
                || now - clientLastBreak >= delay
                || now == clientLastBreak; // vanilla may ask twice in the tick it breaks

        if (permitted && isClientMining(player) && now != clientLastBreak)
        {
            // A permitted speed finishes the block in this same call chain: record it, animate it, and
            // tell the server BEFORE vanilla sends its dig packet (packets arrive in order).
            clientLastBreak = now;
            clientFreshUntil = NEVER; // a click permits ONE immediate break
            MegaToolPoke.triggerLocal();
            if (delayed)
                PacketDistributor.sendToServer(MegaToolBreakPayload.INSTANCE);
        }
        event.setNewSpeed(permitted ? INSTANT_SPEED : 0.0F);
    }

    private static boolean isZeroHardness(Player player, BlockState state, Optional<BlockPos> pos)
    {
        return pos.isPresent() && state.getDestroySpeed(player.level(), pos.get()) == 0.0F;
    }

    /** Real mining on this client: inside the game tick, with the button held or right after a click. */
    private static boolean isClientMining(Player player)
    {
        return clientInTick
                && (clientAttackDown.getAsBoolean() || player.level().getGameTime() <= clientFreshUntil);
    }

    /** Client: the attack button was freshly pressed with a Mega Tool. */
    public static void markClientFreshClick(long gameTime)
    {
        clientFreshUntil = gameTime + 2;
    }

    /** Server: MegaToolBreakPayload arrived; grant the break that follows it. */
    public static void grantServerBreak(Player player)
    {
        SERVER_PERMIT_UNTIL.put(player.getUUID(), player.level().getGameTime() + SERVER_PERMIT_TICKS);
    }

    // =====================================================================================
    //  Other events
    // =====================================================================================

    /** Both sides: cancelling stops a tagged block from even starting to break (also in creative). */
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        if (isMegaTool(event.getEntity().getMainHandItem())
                && event.getLevel().getBlockState(event.getPos()).is(ModTags.MEGA_TOOL_UNBREAKABLE))
            event.setCanceled(true);
    }

    /** Server: a block is about to be broken. */
    public static void onBlockBroken(BlockEvent.BreakEvent event)
    {
        Player player = event.getPlayer();
        if (player == null || !isMegaTool(player.getMainHandItem()))
            return;
        if (event.getState().is(ModTags.MEGA_TOOL_UNBREAKABLE))
        {
            event.setCanceled(true); // final server-side guarantee for the tag
            return;
        }
        SERVER_PERMIT_UNTIL.remove(player.getUUID()); // the granted break is used up
        broadcastPoke(player);
    }

    /** Server: a Mega Tool hit also shows the poke to other players. */
    public static void onAttackEntity(AttackEntityEvent event)
    {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && isMegaTool(player.getMainHandItem()) && !MultiToolItem.isBroken(player.getMainHandItem()))
            broadcastPoke(player);
    }

    /** Tell everyone who can see this player to play the poke (third person). The player animates locally. */
    private static void broadcastPoke(Player player)
    {
        PacketDistributor.sendToPlayersTrackingEntity(player, new MegaToolPokePayload(player.getId()));
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event)
    {
        SERVER_PERMIT_UNTIL.remove(event.getEntity().getUUID());
    }
}
