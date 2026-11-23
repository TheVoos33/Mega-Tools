package com.thevoos.megatools.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;

/**
 * Who may edit the "Tool Stats" (startup config) in the mod's config menu.
 *
 * Tool stats are baked into the items when the game starts, and each side (client / server) reads its
 * own config/megatools-startup.toml. So:
 *  - Only from inside a world the player hosts (singleplayer or open to LAN) with cheats on, or as an
 *    operator there (permission level 2). Without that, changing the stats would be a cheat.
 *  - Never while connected to a remote server: the server's own file decides there, and changing the
 *    client's file would only make the two disagree (wrong durability, ghost blocks while mining).
 *    Server operators edit the server's file instead. ToolStatsCheck warns about mismatches on join.
 *  - The main menu has no world, so no cheat permission either: locked there too.
 * Everything else in the config menu (e.g. the Mega Tool hold delay) works as before.
 *
 * The startup file's configVersion ("File format") is hidden from the menu; it stays in the file.
 */
public final class ToolStatsAccess
{
    private ToolStatsAccess() {}

    /** Section screen factory for NeoForge's ConfigurationScreen. */
    public static Screen section(ConfigurationScreen parent, ModConfig.Type type, ModConfig config, Component title)
    {
        Component reason = type == ModConfig.Type.STARTUP ? lockReason(Minecraft.getInstance()) : null;
        if (reason != null)
            return new AlertScreen(() -> Minecraft.getInstance().setScreen(parent),
                    Component.translatable("gui.megatools.tool_stats_locked.title"), reason);
        // configVersion ("File format") stays in megatools-startup.toml, where StartupConfig#update uses it,
        // but isn't shown in the menu, so nobody changes it by accident.
        return new ConfigurationScreen.ConfigurationSectionScreen(parent, type, config, title,
                (context, key, element) -> "configVersion".equals(key) ? null : element);
    }

    /** Why the tool stats can't be edited right now, or null if they can. */
    private static Component lockReason(Minecraft mc)
    {
        if (mc.level == null || mc.player == null)
            return Component.translatable("gui.megatools.tool_stats_locked.no_world");
        if (!mc.hasSingleplayerServer())
            return Component.translatable("gui.megatools.tool_stats_locked.server");
        if (!mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            return Component.translatable("gui.megatools.tool_stats_locked.no_permission");
        return null;
    }
}
