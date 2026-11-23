package com.thevoos.megatools.command;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.thevoos.megatools.GuideCompat;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.thevoos.megatools.Config;
import com.thevoos.megatools.MegaTools;
import net.minecraft.ChatFormatting;
import com.thevoos.megatools.items.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code /megatools} command tree.
 *
 * <ul>
 *   <li>{@code /megatools kit} - admins only (permission level 3). Gives the running player every
 *       Mega Tools tool, 64 torches, a Fortune III book, a Silk Touch book, two Enchantment Plates and
 *       one of each vanilla diamond tool. Anything that doesn't fit in the inventory is dropped at
 *       the player's feet.</li>
 *   <li>{@code /megatools held_light [true|false]} - admins only, like kit. Shows or switches the held tool light
 *       (Config#HELD_LIGHT) for the whole server and saves it to the server's megatools-common.toml.</li>
 * </ul>
 */
public final class MegaToolsCommand
{
    private MegaToolsCommand() {}

    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal(MegaTools.MODID)
                .then(Commands.literal("kit")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .executes(MegaToolsCommand::giveKit))
                .then(Commands.literal("held_light")
                        .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                        .executes(MegaToolsCommand::showHeldLight)
                        .then(Commands.argument("on", BoolArgumentType.bool())
                                .executes(ctx ->
                                {
                                    Config.HELD_LIGHT.set(BoolArgumentType.getBool(ctx, "on"));
                                    Config.HELD_LIGHT.save();
                                    boolean on = Config.HELD_LIGHT.get();
                                    MegaTools.LOGGER.info("Held tool light switched {} by {}", on ? "on" : "off", ctx.getSource().getTextName());
                                    ctx.getSource().sendSuccess(() -> heldLightMessage(), true); // also told to other operators
                                    return Command.SINGLE_SUCCESS;
                                }))));
    }

    /** The server's heldLight (light blocks around players holding a tool with torches), for every player. */
    private static int showHeldLight(CommandContext<CommandSourceStack> ctx)
    {
        ctx.getSource().sendSuccess(MegaToolsCommand::heldLightMessage, false);
        return Command.SINGLE_SUCCESS;
    }

    private static Component heldLightMessage()
    {
        boolean on = Config.HELD_LIGHT.get();
        return Component.translatable("command.megatools.held_light",
                Component.translatable(on ? "options.on" : "options.off").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    private static int giveKit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();

        List<ItemStack> kit = buildKit(player);
        for (ItemStack stack : kit)
        {
            // add() shrinks the stack by whatever fit; drop the rest so nothing is lost
            if (!player.getInventory().add(stack) && !stack.isEmpty())
                player.drop(stack, false);
        }

        MegaTools.LOGGER.info("Gave the Mega Tools kit to {}", player.getGameProfile().name());
        source.sendSuccess(() -> Component.translatable("command.megatools.kit.success", player.getDisplayName()), true);
        return Command.SINGLE_SUCCESS;
    }

    private static List<ItemStack> buildKit(ServerPlayer player)
    {
        List<ItemStack> kit = new ArrayList<>();

        // Mega Tools tools
        kit.add(new ItemStack(ModItems.STONE_PAXEL.get()));
        kit.add(new ItemStack(ModItems.IRON_PAXEL.get()));
        kit.add(new ItemStack(ModItems.DIAMOND_PAXEL.get()));
        kit.add(new ItemStack(ModItems.NETHERITE_PAXEL.get()));
        for (Item megaTool : ModItems.megaTools())
            kit.add(new ItemStack(megaTool));

        // Support Tools (shears + brush + flint and steel), one per tool
        kit.add(new ItemStack(ModItems.SUPPORT_TOOL.get()));
        kit.add(new ItemStack(ModItems.SUPPORT_TOOL.get()));
        kit.add(new ItemStack(ModItems.SUPPORT_TOOL.get()));
        kit.add(new ItemStack(ModItems.SUPPORT_TOOL.get()));
        kit.add(new ItemStack(ModItems.SUPPORT_TOOL.get()));

        // Light source for the tools' torch slot
        kit.add(new ItemStack(Items.TORCH, 64));

        // Books to put on the plates in an anvil
        kit.add(enchantedBook(player, Enchantments.FORTUNE, 3));
        kit.add(enchantedBook(player, Enchantments.SILK_TOUCH, 1));

        // Plates stack to 1, so two separate stacks
        kit.add(new ItemStack(ModItems.ENCHANTMENT_PLATE.get()));
        kit.add(new ItemStack(ModItems.ENCHANTMENT_PLATE.get()));

        // The in-game guide book, only when GuideME is installed (built from its id: no dependency on GuideME)
        if (GuideCompat.isLoaded())
        {
            JsonObject guide = new JsonObject();
            guide.addProperty("id", GuideCompat.MOD_ID + ":guide");
            JsonObject components = new JsonObject();
            components.addProperty(GuideCompat.MOD_ID + ":guide_id", GuideCompat.GUIDE.toString());
            guide.add("components", components);
            ItemStack.CODEC.parse(player.registryAccess().createSerializationContext(JsonOps.INSTANCE), guide)
                    .resultOrPartial(error -> MegaTools.LOGGER.warn("Couldn't create the guide book: {}", error))
                    .ifPresent(kit::add);
        }

        // Vanilla diamond tools
        kit.add(new ItemStack(Items.DIAMOND_SWORD));
        kit.add(new ItemStack(Items.DIAMOND_PICKAXE));
        kit.add(new ItemStack(Items.DIAMOND_AXE));
        kit.add(new ItemStack(Items.DIAMOND_SHOVEL));
        kit.add(new ItemStack(Items.DIAMOND_HOE));

        return kit;
    }

    /** Enchantments are data-driven in 1.21, so they are looked up from the server's registries. */
    private static ItemStack enchantedBook(ServerPlayer player, ResourceKey<Enchantment> key, int level)
    {
        Holder<Enchantment> enchantment = player.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(key);
        return EnchantmentHelper.createBook(new EnchantmentInstance(enchantment, level));
    }
}
