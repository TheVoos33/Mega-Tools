package com.thevoos.megatools;

import net.neoforged.neoforge.common.ModConfigSpec;

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
 *   stone pickaxe 4.0, iron 6.0, diamond 8.0.  1.2 = 20% faster, 0.5 = half as fast, 2.0 = twice as fast.
 * The Mega Tool always breaks blocks instantly, so it has no mining speed setting.
 * attackSpeed is attacks per second (vanilla swords 1.6, axes 0.8-1.0, an empty hand 4.0).
 */
public final class StartupConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    //                                                     section         name             damage atk/s durability mining speed
    public static final ToolValues STONE_PAXEL   = new ToolValues(BUILDER, "stone_paxel",   "Stone Paxel",   10.0, 1.6,  655,  true);
    public static final ToolValues IRON_PAXEL    = new ToolValues(BUILDER, "iron_paxel",    "Iron Paxel",    14.0, 2.0,  1616, true);
    public static final ToolValues DIAMOND_PAXEL = new ToolValues(BUILDER, "diamond_paxel", "Diamond Paxel", 18.0, 2.5,  8171, true);
    public static final ToolValues MEGA_TOOL     = new ToolValues(BUILDER, "mega_tool",     "Mega Tool",     22.0, 3.0,  0,    false);

    static final ModConfigSpec SPEC = BUILDER.build();

    private StartupConfig() {}

    /** One [section] per tool. */
    public static final class ToolValues
    {
        public static final double DEFAULT_SPEED_MULTIPLIER = 1.2;

        public final ModConfigSpec.DoubleValue attackDamage;
        public final ModConfigSpec.DoubleValue attackSpeed;
        public final ModConfigSpec.IntValue durability;
        /** Null for the Mega Tool (instant mining). */
        public final ModConfigSpec.DoubleValue miningSpeedMultiplier;

        private ToolValues(ModConfigSpec.Builder builder, String section, String displayName,
                           double defaultDamage, double defaultAttackSpeed, int defaultDurability, boolean hasSpeed)
        {
            builder.comment(displayName + " stats (restart required after changes)").push(section);

            attackDamage = builder
                    .comment("Total attack damage, as shown in the tooltip.")
                    .defineInRange("attackDamage", defaultDamage, 1.0, 1000.0);

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
