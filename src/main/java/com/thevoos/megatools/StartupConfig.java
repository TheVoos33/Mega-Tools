package com.thevoos.megatools;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * Tool stats for modpack developers: config/megatools-startup.toml.
 *
 * This is a STARTUP config (not COMMON) because durability, attack damage and mining speed are
 * baked into each item when it is registered, which happens before COMMON configs load. Startup
 * configs load immediately, so the values are ready in time. Consequences:
 *  - Changes need a game/server restart.
 *  - Startup configs are not synced over the network: ship the same file to clients and the
 *    server (modpacks do this automatically through their config folder).
 *
 * miningSpeedMultiplier is relative to the matching vanilla tier:
 *   stone pickaxe 4.0, iron 6.0, diamond 8.0, netherite 9.0.  1.2 = 20% faster, 0.5 = half as fast, 2.0 = twice as fast.
 * The Mega Tools always break blocks instantly, so they have no mining speed setting.
 * attackSpeed is attacks per second (vanilla swords 1.6, axes 0.8-1.0, an empty hand 4.0).
 */
public final class StartupConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * Format of this file. 1: Mega Tools 1.0 - 1.2.  2: early Mega Tools 1.3 test builds.
     * 3: Mega Tools 1.3 (Mega Tool attack damage 22 -> 28, Mega Tool attack speed 3.0 -> 3.5,
     * Netherite Paxel attack speed 2.5 -> 3.0).
     */
    private static final int VERSION = 3;
    private static final double OLD_MEGA_TOOL_DAMAGE = 22.0;
    private static final double OLD_MEGA_TOOL_SPEED = 3.0;
    private static final double OLD_NETHERITE_PAXEL_SPEED = 2.5;

    public static final ModConfigSpec.IntValue CONFIG_VERSION = BUILDER
            .comment("Format of this file, used to update it after a Mega Tools update. Don't change it.")
            .defineInRange("configVersion", 1, 1, Integer.MAX_VALUE);

    //                                                     section         name             damage atk/s durability mining speed
    public static final ToolValues STONE_PAXEL   = new ToolValues(BUILDER, "stone_paxel",   "Stone Paxel",   10.0, 1.6,  655,  true);
    public static final ToolValues IRON_PAXEL    = new ToolValues(BUILDER, "iron_paxel",    "Iron Paxel",    14.0, 2.0,  1616, true);
    public static final ToolValues DIAMOND_PAXEL = new ToolValues(BUILDER, "diamond_paxel", "Diamond Paxel", 18.0, 2.5,  8171, true);
    public static final ToolValues NETHERITE_PAXEL = new ToolValues(BUILDER, "netherite_paxel", "Netherite Paxel", 22.0, 3.0, 10155, true);
    public static final ToolValues MEGA_TOOL     = new ToolValues(BUILDER, "mega_tool",     "Mega Tool",     28.0, 3.5,  0,    false);
    // Only used when the Allthemodium mod is installed (the items don't exist otherwise).
    public static final ToolValues MEGA_TOOL_ALLTHEMODIUM = new ToolValues(BUILDER, "mega_tool_allthemodium",
            "Mega Tool - Allthemodium (only with the Allthemodium mod)", 38.0, 3.5, 0, false);
    public static final ToolValues MEGA_TOOL_VIBRANIUM = new ToolValues(BUILDER, "mega_tool_vibranium",
            "Mega Tool - Vibranium (only with the Allthemodium mod)", 48.0, 3.5, 0, false);
    public static final ToolValues MEGA_TOOL_UNOBTAINIUM = new ToolValues(BUILDER, "mega_tool_unobtainium",
            "Mega Tool - Unobtainium (only with the Allthemodium mod)", 60.0, 3.5, 0, false);

    /** Every tool, in a fixed order (ToolStatsPayload compares the server's and the client's values). */
    public static final List<ToolValues> ALL = List.of(STONE_PAXEL, IRON_PAXEL, DIAMOND_PAXEL, NETHERITE_PAXEL,
            MEGA_TOOL, MEGA_TOOL_ALLTHEMODIUM, MEGA_TOOL_VIBRANIUM, MEGA_TOOL_UNOBTAINIUM);

    static final ModConfigSpec SPEC = BUILDER.build();

    private StartupConfig() {}

    /**
     * Called right after the file is loaded. Values that are still an old default are raised to the new
     * default; any other value was set on purpose and is kept:
     *  - files from Mega Tools 1.2 and older: Mega Tool attack damage 22 -> 28,
     *  - files from 1.2 and older or early 1.3 test builds: attack speed of the Mega Tools 3.0 -> 3.5 and of
     *    the Netherite Paxel 2.5 -> 3.0.
     * New tools (Netherite Paxel...) are added to the file with their defaults by the loader.
     */
    static void update()
    {
        int version = CONFIG_VERSION.get();
        if (version >= VERSION)
            return;
        if (version < 2)
            raiseOldDefault(MEGA_TOOL.attackDamage, OLD_MEGA_TOOL_DAMAGE, "Mega Tool attack damage");
        raiseOldDefault(NETHERITE_PAXEL.attackSpeed, OLD_NETHERITE_PAXEL_SPEED, "Netherite Paxel attack speed");
        for (ToolValues mega : List.of(MEGA_TOOL, MEGA_TOOL_ALLTHEMODIUM, MEGA_TOOL_VIBRANIUM, MEGA_TOOL_UNOBTAINIUM))
            raiseOldDefault(mega.attackSpeed, OLD_MEGA_TOOL_SPEED, "attack speed of " + mega.name);
        CONFIG_VERSION.set(VERSION);
        SPEC.save();
    }

    private static void raiseOldDefault(ModConfigSpec.DoubleValue value, double oldDefault, String what)
    {
        if (value.get() != oldDefault)
            return;
        value.set(value.getDefault());
        MegaTools.LOGGER.info("megatools-startup.toml: {} raised from {} to {} (new default)", what, oldDefault, value.getDefault());
    }

    /** One [section] per tool. */
    public static final class ToolValues
    {
        public static final double DEFAULT_SPEED_MULTIPLIER = 1.2;

        /** Section name in the file. */
        public final String name;
        public final ModConfigSpec.DoubleValue attackDamage;
        public final ModConfigSpec.DoubleValue attackSpeed;
        public final ModConfigSpec.IntValue durability;
        /** Null for the Mega Tool (instant mining). */
        public final ModConfigSpec.DoubleValue miningSpeedMultiplier;

        private ToolValues(ModConfigSpec.Builder builder, String section, String displayName,
                           double defaultDamage, double defaultAttackSpeed, int defaultDurability, boolean hasSpeed)
        {
            this.name = section;
            builder.comment(displayName + " stats (restart required after changes)").push(section);

            attackDamage = builder
                    .comment("Total attack damage, as shown in the tooltip.")
                    .defineInRange("attackDamage", defaultDamage, 1.0, 10_000.0);

            attackSpeed = builder
                    .comment("Attacks per second, as shown in the tooltip. 1.6 = sword speed,",
                             "0.8 = slow like an axe, 4.0 = no cooldown (like an empty hand).")
                    .defineInRange("attackSpeed", defaultAttackSpeed, 0.1, 20.0);

            if (hasSpeed)
            {
                durability = builder
                        .comment("Maximum durability. Default = combined durability of the vanilla tools it replaces.")
                        .defineInRange("durability", defaultDurability, 1, 1_000_000);
                miningSpeedMultiplier = builder
                        .comment("Mining speed relative to the vanilla tool of the same tier.",
                                 "1.2 = 20% faster (default), 0.5 = half as fast, 2.0 = twice as fast, 0 = cannot mine.")
                        .defineInRange("miningSpeedMultiplier", DEFAULT_SPEED_MULTIPLIER, 0.0, 2.0);
            }
            else
            {
                durability = builder
                        .comment("Maximum durability. 0 = unbreakable (default).")
                        .defineInRange("durability", defaultDurability, 0, 1_000_000);
                miningSpeedMultiplier = null;
            }

            builder.pop();
        }
    }
}
