package com.thevoos.megatools;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mega Tools configuration (config/megatools-common.toml).
 * Replaces the MDK example config (dirt block / magic number) with settings the mod actually uses.
 */
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Auto-swap matches "block namespace == stored tool namespace". Vanilla tools (shears, etc.)
    // would otherwise match every vanilla block, so by default vanilla blocks are ignored.
    public static final ModConfigSpec.BooleanValue AUTO_SWAP_IGNORE_VANILLA = BUILDER
            .comment("If true, tool auto-swap never pre-selects a tool for vanilla (minecraft:) blocks.")
            .define("autoSwapIgnoreVanilla", true);

    // Delay between block breaks while the attack button is HELD with the Mega Tool (20 ticks = 1 second).
    public static final ModConfigSpec.IntValue MEGA_TOOL_HOLD_DELAY_TICKS = BUILDER
            .comment("Ticks between block breaks while holding left click with the Mega Tool (20 = 1 second).",
                     "Only applies while the tool's \"Delayed Break\" toggle is ON. Rapid individual clicks are never delayed.")
            .defineInRange("megaToolHoldDelayTicks", 10, 1, 200); // default 10 = half a second

    static final ModConfigSpec SPEC = BUILDER.build();
}
