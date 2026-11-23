package com.thevoos.megatools.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.thevoos.megatools.Config;
import com.thevoos.megatools.MegaTools;
import com.thevoos.megatools.menu.MenuStyle;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

/**
 * Client commands for the player's own settings in config/megatools-common.toml, the same values as the
 * config screen. They run in the player's own game: no operator or cheats needed, on any server.
 *
 * <ul>
 *   <li>{@code /megatools animation true|false} - the Mega Tool poke animation (false = vanilla swing)</li>
 *   <li>{@code /megatools delayed_break 1-200} - ticks between breaks while left click is held (Delayed Break on)</li>
 *   <li>{@code /megatools menu classic|compact} - which tool menu opens</li>
 * </ul>
 * Without a value each one shows the current setting. Commands the client doesn't know (/megatools kit and
 * /megatools held_light, which are server commands, see MegaToolsCommand) are
 * passed on to the server by NeoForge.
 */
public final class ClientCommands
{
    private ClientCommands() {}

    /** Game bus. */
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal(MegaTools.MODID)
                .then(Commands.literal("animation")
                        .executes(ctx -> showAnimation(ctx))
                        .then(Commands.argument("poke", BoolArgumentType.bool())
                                .executes(ctx ->
                                {
                                    boolean poke = BoolArgumentType.getBool(ctx, "poke");
                                    Config.MEGA_TOOL_ANIMATION.set(poke ? Config.MegaToolAnimation.POKE : Config.MegaToolAnimation.VANILLA);
                                    Config.MEGA_TOOL_ANIMATION.save();
                                    return showAnimation(ctx);
                                })))
                .then(Commands.literal("delayed_break")
                        .executes(ctx -> showDelay(ctx))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 200))
                                .executes(ctx ->
                                {
                                    Config.MEGA_TOOL_HOLD_DELAY_TICKS.set(IntegerArgumentType.getInteger(ctx, "ticks"));
                                    Config.MEGA_TOOL_HOLD_DELAY_TICKS.save();
                                    return showDelay(ctx);
                                })))
                .then(Commands.literal("menu")
                        .executes(ctx -> showMenu(ctx))
                        .then(Commands.literal("classic").executes(ctx -> setMenu(ctx, MenuStyle.CLASSIC)))
                        .then(Commands.literal("compact").executes(ctx -> setMenu(ctx, MenuStyle.ICONS)))));
    }

    private static int showAnimation(CommandContext<CommandSourceStack> ctx)
    {
        return send(ctx, Component.translatable("command.megatools.animation", onOff(Config.pokeAnimation())));
    }

    private static int showDelay(CommandContext<CommandSourceStack> ctx)
    {
        int ticks = Config.MEGA_TOOL_HOLD_DELAY_TICKS.get();
        return send(ctx, Component.translatable("command.megatools.delayed_break",
                Component.literal(String.valueOf(ticks)).withStyle(ChatFormatting.GOLD), String.format("%.2f", ticks / 20.0)));
    }

    private static int setMenu(CommandContext<CommandSourceStack> ctx, MenuStyle style)
    {
        Config.MENU_STYLE.set(style);
        Config.MENU_STYLE.save();
        return showMenu(ctx);
    }

    private static int showMenu(CommandContext<CommandSourceStack> ctx)
    {
        String key = switch (Config.MENU_STYLE.get())
        {
            case CLASSIC -> "command.megatools.menu.classic";
            case ICONS -> "command.megatools.menu.compact";
            case ASK -> "command.megatools.menu.ask";
        };
        return send(ctx, Component.translatable("command.megatools.menu",
                Component.translatable(key).withStyle(ChatFormatting.GOLD)));
    }

    /** Green "ON" / red "OFF" (vanilla's option words). */
    private static Component onOff(boolean on)
    {
        return Component.translatable(on ? "options.on" : "options.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static int send(CommandContext<CommandSourceStack> ctx, Component message)
    {
        ctx.getSource().sendSuccess(() -> message, false);
        return Command.SINGLE_SUCCESS;
    }
}
