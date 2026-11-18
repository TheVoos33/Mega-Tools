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
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/**
 * Mega Tool mining rules:
 *  - Any breakable block breaks in one hit (hardness doesn't matter; hardness -1 blocks like bedrock
 *    stay unbreakable, vanilla never asks for a speed for those).
 *  - Flying / swimming / underwater penalties are ignored, and so is whatever other mods do to the
 *    mining speed (we override the FINAL speed, see below).
 *  - While left click is HELD, a delay (Config: megaToolHoldDelayTicks) separates breaks. That delay
 *    is the "Delayed Break" toggle (ON by default); turning it OFF removes the delay.
 *  - Every NEW click breaks a block immediately.
 *  - Zero-hardness blocks (grass, flowers, torches...) follow the same delay. Their speed can't be 0
 *    (see finalDigSpeed), so on the client the delay is applied in onLeftClickBlock instead.
 *  - Blocks in the #megatools:not_mineable tag are mined like with an empty hand (no instant break).
 *
 * WHO DECIDES: the client. It knows about held vs. fresh clicks and is the side that shows the break
 * (sound, particles). When it decides a block breaks now, it sends MegaToolBreakPayload right before
 * vanilla's dig packet; the server then grants exactly that one break. The server never runs its own
 * timer, so both sides always break the same block at the same moment (no silent, late breaks).
 *
 * HOW: Player#getDigSpeed runs on both sides whenever vanilla computes dig progress. PlayerMixin
 * replaces its RESULT (finalDigSpeed below): a huge speed when a break is permitted (vanilla then
 * finishes the block in the same call chain) and 0 while the hold delay runs.
 * This is deliberately not a PlayerEvent.BreakSpeed listener. Mods that remove the "mining while not
 * on the ground" penalty (Draconic Evolution's mining stabilizer, Advanced AE's armor, Apothic's Stable
 * Footing...) listen to that event too and set the speed again from the ORIGINAL speed, which threw
 * away our value whenever they ran after us: the Mega Tool then mined slowly, or broke blocks during
 * the hold delay. The method's return value is computed after every listener, so nothing can undo it.
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
    //  Final dig speed (both sides)
    // =====================================================================================

    /**
     * Called by PlayerMixin with the speed Player#getDigSpeed is about to return (after vanilla's
     * penalties and every BreakSpeed listener). Returns the speed to use instead, or {@code speed}
     * unchanged when the held item isn't one of our tools.
     */
    public static float finalDigSpeed(Player player, BlockState state, @Nullable BlockPos pos, float speed)
    {
        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof MultiToolItem multiTool))
            return speed;
        if (speed < 0.0F)
            return speed; // another mod cancelled BreakSpeed (NeoForge then returns -1): the block stays protected
        // Broken (1 durability left) or block breaking switched off: no progress at all, on every tier.
        if (MultiToolItem.isBroken(held) || ToolData.hasFlag(held, ToolSettings.BREAK_OFF))
            return 0.0F;
        if (!multiTool.getTier().isMega())
            return speed; // paxels mine at their normal speed

        if (state.is(ModTags.NOT_MINEABLE))
            return speed; // not the right tool: mines like an empty hand, no instant break or hold delay
        if (player.getAbilities().instabuild)
            return speed; // creative already breaks instantly

        boolean client = player.level().isClientSide();
        // Zero-hardness blocks (short grass, flowers...): vanilla computes progress as speed / hardness,
        // so speed 0 gives 0 / 0 = NaN. On the client that NaN sticks in the dig progress and the block
        // can never break again, so these blocks must never get speed 0.
        boolean zeroHardness = isZeroHardness(player, state, Optional.ofNullable(pos));

        if (!client)
        {
            // Server: only the break the client just announced (or no delay at all). The server checks
            // each dig packet on its own, so for zero hardness -1 is safe: -1 / 0 = -Infinity, never >= 1.
            boolean granted = !ToolData.hasFlag(held, ToolSettings.DELAYED_BREAK)
                    || player.level().getGameTime() <= SERVER_PERMIT_UNTIL.getOrDefault(player.getUUID(), NEVER);
            return granted ? INSTANT_SPEED : zeroHardness ? -1.0F : 0.0F;
        }

        if (zeroHardness)
            return INSTANT_SPEED; // client: the delay for these blocks is applied in onLeftClickBlock, before vanilla asks for the speed

        return clientPermitsBreak(player, held) ? INSTANT_SPEED : 0.0F;
    }

    /**
     * Client: may a block break right now? If it may and this is real mining, the break is recorded
     * (the hold delay starts), the jab plays and the server is told to grant it.
     */
    private static boolean clientPermitsBreak(Player player, ItemStack held)
    {
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
        return permitted;
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

    /**
     * Client, on joining a world: the timers use the world's game time, and the new world's time can be
     * lower than the old one's, which made the hold delay behave oddly until the first break.
     */
    public static void resetClient()
    {
        clientLastBreak = NEVER;
        clientFreshUntil = NEVER;
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

    /**
     * Both sides: cancelling stops a tagged block from even starting to break (also in creative).
     * Client: applies the hold delay to zero-hardness blocks. Vanilla fires this event right before it
     * asks for the dig progress; useItem = FALSE makes it skip the mining (no break) for this tick.
     */
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (!isMegaTool(held))
            return;

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!event.getLevel().isClientSide() || player.getAbilities().instabuild
                || MultiToolItem.isBroken(held) || ToolData.hasFlag(held, ToolSettings.BREAK_OFF))
            return; // broken / breaking switched off: DisabledToolHandler cancels the click
        PlayerInteractEvent.LeftClickBlock.Action action = event.getAction();
        if (action != PlayerInteractEvent.LeftClickBlock.Action.START && action != PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD)
            return;
        if (event.getUseItem().isFalse() || state.is(ModTags.NOT_MINEABLE)
                || !isZeroHardness(player, state, Optional.of(event.getPos())))
            return;

        if (!clientPermitsBreak(player, held))
            event.setUseItem(TriState.FALSE);
    }

    /** Server: a block is about to be broken. */
    public static void onBlockBroken(BlockEvent.BreakEvent event)
    {
        Player player = event.getPlayer();
        if (player == null || !isMegaTool(player.getMainHandItem()))
            return;
        SERVER_PERMIT_UNTIL.remove(player.getUUID()); // the granted break is used up
        broadcastPoke(player);
    }

    /** Server: a Mega Tool hit also shows the poke to other players. */
    public static void onAttackEntity(AttackEntityEvent event)
    {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (!player.level().isClientSide() && isMegaTool(held) && !MultiToolItem.isBroken(held))
            broadcastPoke(player); // not reached when the hit was cancelled (DisabledToolHandler runs first)
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

    /** Game time starts over in the next world: a permit left from this one must not carry over. */
    public static void onServerStopped(ServerStoppedEvent event)
    {
        SERVER_PERMIT_UNTIL.clear();
    }
}
