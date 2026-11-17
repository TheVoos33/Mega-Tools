package com.thevoos.megatools;

import com.thevoos.megatools.menu.MenuStyle;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Mega Tools configuration (config/megatools-common.toml).
 * Replaces the MDK example config (dirt block / magic number) with settings the mod actually uses.
 */
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // Delay between block breaks while the attack button is HELD with the Mega Tool (20 ticks = 1 second).
    public static final ForgeConfigSpec.IntValue MEGA_TOOL_HOLD_DELAY_TICKS = BUILDER
            .comment("Ticks between block breaks while holding left click with the Mega Tool (20 = 1 second).",
                     "Only applies while the tool's \"Delayed Break\" toggle is ON. Rapid individual clicks are never delayed.")
            .defineInRange("megaToolHoldDelayTicks", 10, 1, 200); // default 10 = half a second

    // Server switch for the light blocks that follow players holding a tool with torches (ToolLightManager).
    public static final ForgeConfigSpec.BooleanValue HELD_LIGHT = BUILDER
            .comment("Whether a held tool with torches in its torch slot lights the area around the player.",
                     "The light is an invisible block that moves with the player; every move is a light update for the",
                     "server and for nearby players. Large servers can switch it off here (read by the server).",
                     "Shader packs with handheld lighting still light the player's own surroundings.")
            .define("heldLight", true);

    // Which of the two tool menus this player uses. ASK = show the choice the next time the menu opens.
    public static final ForgeConfigSpec.EnumValue<MenuStyle> MENU_STYLE = BUILDER
            .comment("Which tool menu to show: CLASSIC (wide, buttons with text) or ICONS (compact, buttons with item icons).",
                     "ASK shows both the next time a tool menu is opened and lets you pick one.",
                     "A setting of your own game: a server ignores it, every player picks their own.")
            .defineEnum("menuStyle", MenuStyle.ASK);

    static final ForgeConfigSpec SPEC = BUILDER.build();
}
