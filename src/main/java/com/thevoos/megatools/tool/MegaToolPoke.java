package com.thevoos.megatools.tool;

import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * State of the Mega Tool "poke" animation (client side; no client-only imports, so common code can
 * trigger it).
 *
 * Two independent parts:
 *  - TILT (0 = upright, 1 = turned ~90 degrees forward). It turns toward 1 while the player holds
 *    left click on a block, or while a jab is queued/playing, and back to 0 otherwise.
 *  - JAB: one forward-and-back prod. Each real block break (or hit) marks one jab as PENDING; it
 *    starts on the exact frame the tilt reaches 1, never earlier, so it is always turn first, THEN
 *    prod, whatever direction the player looks. While left click stays held the tool stays tilted,
 *    and every following break just plays another jab.
 *
 * RIGHT-CLICK swings (till, strip, path, shear, ignite, place torch...): the first-person renderer ignores
 * vanilla's arm swing so held mining doesn't chop up and down, which also hid the swing of right-click
 * abilities. A swing that starts right after the use key was pressed is marked as a "use swing" and
 * the renderer plays vanilla's normal tool swing for it (see isUseSwing).
 *
 * The local player is driven by input (attack key held). Other players are driven by
 * MegaToolPokePayload, sent by the server whenever they break a block or hit something; they stay
 * tilted for REMOTE_HOLD_MS after each poke, so they keep the pose while holding left click.
 */
public final class MegaToolPoke
{
    /** Time to turn 90 degrees. */
    public static final long TILT_MS = 120;
    /** Time for one jab out and back. */
    public static final long THRUST_MS = 220;
    /** With Delayed Break off, blocks can break every tick: a running jab must be this far along
     *  before another one is queued, so it reads as a steady prod instead of jitter. */
    private static final float RESTART_AT = 0.6F;
    /**
     * After a click, the local tool stays turned at least this long even if the button is already
     * released. Long enough for the full turn + one jab, so a quick click always plays turn -> jab ->
     * return, and rapid clicking keeps it turned (like holding) instead of bobbing up and down.
     */
    private static final long CLICK_HOLD_MS = TILT_MS + THRUST_MS + 80;
    /** Other players stay tilted this long after their last poke (longer than the default hold delay). */
    private static final long REMOTE_HOLD_MS = 700;
    private static final long NEVER = Long.MIN_VALUE / 4;
    /** Other players' states are forgotten after this long without a poke (left, out of view...). */
    private static final long REMOTE_FORGET_MS = 30_000;
    private static long lastPrune;

    /** Animation state of one player. */
    public static final class State
    {
        private float tilt;
        private long lastUpdate = -1;
        private long thrustStart = NEVER;
        private boolean jabPending;
        private long lastPoke = NEVER;
        private long holdUntil = NEVER;

        /** Raw 0..1 tilt (renderers apply their own easing). */
        public float tilt()
        {
            return tilt;
        }

        /** 0..1..0 jab offset right now (0 when no jab is playing or it is still waiting for the tilt). */
        public float thrust()
        {
            float progress = thrustProgress(Util.getMillis());
            return progress < 0.0F ? 0.0F : (float) Math.sin(Math.PI * progress);
        }

        private float thrustProgress(long now)
        {
            if (thrustStart == NEVER)
                return -1.0F;
            long elapsed = now - thrustStart;
            return elapsed >= THRUST_MS ? -1.0F : elapsed / (float) THRUST_MS;
        }

        private void trigger(long now)
        {
            if (jabPending)
                return; // one is already waiting for the tilt
            float running = thrustProgress(now);
            if (running >= 0.0F && running < RESTART_AT)
                return; // let the current jab finish
            jabPending = true; // starts in update() once the tool is fully turned
        }

        private void update(long now, boolean wantTilt)
        {
            long dt = lastUpdate < 0 ? 0 : Math.min(now - lastUpdate, 100);
            lastUpdate = now;
            boolean jabPlaying = thrustProgress(now) >= 0.0F;
            float target = wantTilt || jabPending || jabPlaying || now < holdUntil ? 1.0F : 0.0F;
            float step = dt / (float) TILT_MS;
            tilt = target > tilt ? Math.min(target, tilt + step) : Math.max(target, tilt - step);

            // Turn first, THEN prod: a pending jab begins only once the turn is complete.
            if (jabPending && tilt >= 1.0F)
            {
                jabPending = false;
                thrustStart = now;
            }
        }
    }

    /** A swing starting this soon after the use key counts as a right-click swing. */
    private static final long USE_SWING_WINDOW_MS = 150;
    private static long useInputAt = NEVER;
    private static boolean useSwing;

    private static final State LOCAL = new State();
    private static final Map<Integer, State> REMOTE = new HashMap<>();

    private MegaToolPoke() {}

    // ---- Local player ----

    /** A block broke (or an entity was hit) with the Mega Tool: queue one jab. */
    public static void triggerLocal()
    {
        LOCAL.trigger(Util.getMillis());
    }

    /** The attack button was just pressed with the Mega Tool: stay turned for at least CLICK_HOLD_MS. */
    public static void pressLocal()
    {
        LOCAL.holdUntil = Util.getMillis() + CLICK_HOLD_MS;
    }

    /** Advance and return the local state. wantTilt = left click is held with the Mega Tool. */
    public static State updateLocal(boolean wantTilt)
    {
        LOCAL.update(Util.getMillis(), wantTilt);
        return LOCAL;
    }

    // ---- Local right-click swing ----

    /** The use key (right click) was just processed with the Mega Tool in the main hand. */
    public static void markUseInput()
    {
        useInputAt = Util.getMillis();
    }

    /** The attack key was processed: any swing from now on belongs to mining / hitting. */
    public static void markAttackInput()
    {
        useSwing = false;
    }

    /** The local player started a main-hand swing with the Mega Tool (MultiToolItem#onEntitySwing). */
    public static void onLocalSwing()
    {
        useSwing = Util.getMillis() - useInputAt <= USE_SWING_WINDOW_MS;
    }

    /** True while the current local swing comes from a right-click ability. */
    public static boolean isUseSwing()
    {
        return useSwing;
    }

    /** Current local state without advancing it (used while rendering the held item). */
    public static State peekLocal()
    {
        return LOCAL;
    }

    // ---- Other players (third person) ----

    /** Called from MegaToolPokePayload: another player broke a block or hit something. */
    public static void triggerRemote(int entityId)
    {
        long now = Util.getMillis();
        // Players who logged out or moved out of view are never drawn again, so updateRemote can't
        // forget them; drop states that haven't poked in a while (at most every few seconds).
        if (now - lastPrune > 5_000)
        {
            lastPrune = now;
            REMOTE.values().removeIf(s -> now - s.lastPoke > REMOTE_FORGET_MS);
        }
        State state = REMOTE.computeIfAbsent(entityId, id -> new State());
        state.lastPoke = now;
        state.trigger(now);
    }

    /** Advance and return another player's state, or null if they haven't poked recently. */
    public static State updateRemote(int entityId)
    {
        State state = REMOTE.get(entityId);
        if (state == null)
            return null;
        long now = Util.getMillis();
        state.update(now, now - state.lastPoke < REMOTE_HOLD_MS);
        if (state.tilt <= 0.0F && now - state.lastPoke > 5_000)
            REMOTE.remove(entityId); // idle for a while: forget it
        return state;
    }

    /** Another player's current state without advancing it, or null. */
    public static State peekRemote(int entityId)
    {
        return REMOTE.get(entityId);
    }

    /** On joining a world: entity ids from the old world are meaningless. */
    public static void clearRemote()
    {
        REMOTE.clear();
    }
}
