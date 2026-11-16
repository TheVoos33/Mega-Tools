package com.thevoos.megatools.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Per-tool settings stored as a data component on the ItemStack.
 *
 * flags       - bit mask of enabled right-click abilities (see constants below)
 * activePlate - 0 = primary enchantment plate is loaded, 1 = secondary plate is loaded
 *
 * The bit index of each flag is also the button id used by the GUI (ToolMenu#clickMenuButton),
 * so button i toggles flag (1 << i).
 */
public record ToolSettings(int flags, int activePlate)
{
    // ---- Paxel abilities (all tools) ----
    public static final int STRIP  = 1;       // axe: strip logs / scrape / un-wax
    public static final int PATH   = 1 << 1;  // shovel: make dirt path / douse campfire
    public static final int TILL   = 1 << 2;  // hoe: till farmland
    // ---- Support Tool abilities (all tiers; only while a Support Tool is in its slot) ----
    public static final int SHEAR  = 1 << 3;  // shears: shear sheep/mooshroom/snow golem, shear-drops from leaves etc.
    public static final int BRUSH  = 1 << 4;  // brush: brush suspicious sand/gravel
    public static final int IGNITE = 1 << 5;  // flint and steel: light fires, campfires, candles
    // ---- Mega Tool only ----
    public static final int DELAYED_BREAK = 1 << 6; // ON: hold-delay between breaks. OFF: break as fast as possible
    // ---- All tools ----
    public static final int PLACE_TORCH = 1 << 7;   // right click places a torch from the torch slot (OFF by default)
    /**
     * Held-tool light switched OFF (small button under the torch slot). Stored inverted, so tools saved
     * before this button existed (and fresh tools) keep their light on.
     */
    public static final int LIGHT_OFF = 1 << 8;

    /**
     * Block breaking switched OFF: the tool breaks no blocks at all. Stored inverted like LIGHT_OFF, so
     * existing and fresh tools keep breaking blocks.
     */
    public static final int BREAK_OFF = 1 << 9;
    /** Attacking switched OFF: the tool hits no entities (no damage). Stored inverted, like BREAK_OFF. */
    public static final int ATTACK_OFF = 1 << 10;

    /** Flags enabled on a fresh tool: every toggle is ON, including Delayed Break (hold delay, see Config). */
    public static final int ALL = STRIP | PATH | TILL | SHEAR | BRUSH | IGNITE | DELAYED_BREAK;

    /** Fresh tools have every ability enabled and the primary plate loaded. */
    public static final ToolSettings DEFAULT = new ToolSettings(ALL, 0);

    /** Disk format (saved with the item). */
    public static final Codec<ToolSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("flags", ALL).forGetter(ToolSettings::flags),
            Codec.INT.optionalFieldOf("active_plate", 0).forGetter(ToolSettings::activePlate)
    ).apply(instance, ToolSettings::new));

    /** Network format (synced to clients so the GUI/tooltip can show it). */
    public static final StreamCodec<ByteBuf, ToolSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ToolSettings::flags,
            ByteBufCodecs.VAR_INT, ToolSettings::activePlate,
            ToolSettings::new);

    public boolean has(int flag)
    {
        return (flags & flag) != 0;
    }

    public ToolSettings toggle(int flag)
    {
        return new ToolSettings(flags ^ flag, activePlate);
    }

    public ToolSettings withActivePlate(int plate)
    {
        return new ToolSettings(flags, plate);
    }
}
