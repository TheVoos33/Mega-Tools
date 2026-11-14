package com.thevoos.megatools;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Mega Tools configuration (config/megatools-common.toml).
 * Replaces the MDK example config (dirt block / magic number) with settings the mod actually uses.
 */
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Delay between block breaks while the attack button is HELD with the Mega Tool (20 ticks = 1 second).
    public static final ModConfigSpec.IntValue MEGA_TOOL_HOLD_DELAY_TICKS = BUILDER
            .comment("Ticks between block breaks while holding left click with the Mega Tool (20 = 1 second).",
                     "Only applies while the tool's \"Delayed Break\" toggle is ON. Rapid individual clicks are never delayed.")
            .defineInRange("megaToolHoldDelayTicks", 10, 1, 200); // default 10 = half a second

    // Server switch for the light blocks that follow players holding a tool with torches (ToolLightManager).
    public static final ModConfigSpec.BooleanValue HELD_LIGHT = BUILDER
            .comment("Whether a held tool with torches in its torch slot lights the area around the player.",
                     "The light is an invisible block that moves with the player; every move is a light update for the",
                     "server and for nearby players. Large servers can switch it off here (read by the server).",
                     "Shader packs with handheld lighting still light the player's own surroundings.")
            .define("heldLight", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}
