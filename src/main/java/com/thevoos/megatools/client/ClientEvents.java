package com.thevoos.megatools.client;

import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.SwapOrigin;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.menu.ModMenus;
import com.thevoos.megatools.network.OpenToolGuiPayload;
import com.thevoos.megatools.network.SwapToolPayload;
import com.thevoos.megatools.network.SwitchPlatePayload;
import com.thevoos.megatools.tool.MegaToolMining;
import com.thevoos.megatools.tool.MegaToolPoke;
import com.thevoos.megatools.tool.ToolSwapper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Client-only event handlers (registered in MegaToolsClient). */
public final class ClientEvents
{
    private ClientEvents() {}

    /** Mod bus: connect the menu type to its screen. */
    public static void onRegisterScreens(RegisterMenuScreensEvent event)
    {
        event.register(ModMenus.TOOL_MENU.get(), ToolScreen::new);
    }

    /** Game bus: start of the client tick. Vanilla mining happens between this and Post. */
    public static void onClientTickPre(ClientTickEvent.Pre event)
    {
        MegaToolMining.setClientInTick(true);
    }

    /** Game bus: runs once per client tick. Updates auto-swap pre-selection and handles the R keys. */
    public static void onClientTick(ClientTickEvent.Post event)
    {
        MegaToolMining.setClientInTick(false); // anything after this (e.g. HUD rendering) isn't mining

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            AutoSwapClient.clear();
            return;
        }

        AutoSwapClient.update(mc);

        // Drain all click counters every tick so presses don't queue up.
        boolean openGui = false, switchPlate = false, swapTool = false;
        while (ModKeyMappings.OPEN_GUI.consumeClick()) openGui = true;
        while (ModKeyMappings.SWITCH_PLATE.consumeClick()) switchPlate = true;
        while (ModKeyMappings.SWAP_TOOL.consumeClick()) swapTool = true;

        if (mc.screen != null)
            return;

        ItemStack held = mc.player.getMainHandItem();

        // Ctrl + R : open GUI (only while holding a Mega Tools tool)
        if (openGui && ModKeyMappings.modifierMatches(ModKeyMappings.OPEN_GUI))
        {
            if (held.getItem() instanceof MultiToolItem)
                PacketDistributor.sendToServer(OpenToolGuiPayload.INSTANCE);
            return;
        }

        // Shift + R : switch enchantment plate
        if (switchPlate && ModKeyMappings.modifierMatches(ModKeyMappings.SWITCH_PLATE))
        {
            if (held.getItem() instanceof MultiToolItem multiTool && multiTool.getTier().hasEnchantPlates())
                PacketDistributor.sendToServer(SwitchPlatePayload.INSTANCE);
            return;
        }

        // R : auto-swap
        if (swapTool && ModKeyMappings.modifierMatches(ModKeyMappings.SWAP_TOOL))
            handleSwapKey(mc, held);
    }

    private static void handleSwapKey(Minecraft mc, ItemStack held)
    {
        // Holding a swapped-out tool -> swap back.
        if (held.has(ModDataComponents.SWAP_ORIGIN.get()))
        {
            PacketDistributor.sendToServer(new SwapToolPayload(ToolSwapper.SWAP_BACK));
            return;
        }
        // Holding a Mega Tools tool -> swap to the pre-selected stored tool.
        if (held.getItem() instanceof MultiToolItem multiTool && multiTool.getTier().hasAutoSwap())
        {
            int slot = AutoSwapClient.getPreselectedSlot();
            if (slot >= 0)
                PacketDistributor.sendToServer(new SwapToolPayload(slot));
            else
                mc.player.displayClientMessage(Component.translatable("message.megatools.no_tool_selected"), true);
        }
    }

    /**
     * Game bus: raw mouse input. A fresh press of the attack button while holding the Mega Tool
     * is a "new click" -> it may break a block immediately even inside the hold delay.
     */
    public static void onMouseButton(InputEvent.MouseButton.Pre event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS || mc.screen != null || mc.player == null || mc.level == null)
            return;
        if (!mc.options.keyAttack.matchesMouse(event.getButton()))
            return;
        if (!MegaToolMining.isMegaTool(mc.player.getMainHandItem()))
            return;

        MegaToolMining.markClientFreshClick(mc.level.getGameTime());
        MegaToolPoke.pressLocal(); // keep the tool turned through a quick click (see MegaToolPoke)
    }

    /**
     * Game bus: Shift + mouse wheel cycles through the pre-selected tools when more than one
     * stored tool matches the targeted block. The scroll is cancelled so the hotbar doesn't move.
     */
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || !Screen.hasShiftDown() || event.getScrollDeltaY() == 0)
            return;
        // Wheel up = previous, wheel down = next (same feel as hotbar scrolling).
        if (AutoSwapClient.cycle(event.getScrollDeltaY() > 0 ? -1 : 1))
            event.setCanceled(true);
    }

    /**
     * Game bus, HIGHEST priority: key presses inside screens, BEFORE the screen and before other mods.
     * JEI listens to this same event to show recipes on R; by running first and cancelling only
     * when a stored tool in the Mega Tools GUI is hovered, our quick swap wins there and JEI keeps
     * its R key everywhere else (including the player inventory part of our GUI).
     */
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Pre event)
    {
        if (event.getScreen() instanceof ToolScreen toolScreen
                && toolScreen.tryQuickSwap(event.getKeyCode(), event.getScanCode()))
            event.setCanceled(true); // cancelled events are not passed on to JEI
    }

    /** Game bus: hitting an entity with the Mega Tool also plays the poke (client side only). */
    public static void onAttackEntity(AttackEntityEvent event)
    {
        ItemStack held = event.getEntity().getMainHandItem();
        if (event.getEntity().level().isClientSide() && MegaToolMining.isMegaTool(held) && !MultiToolItem.isBroken(held))
            MegaToolPoke.triggerLocal();
    }

    /** Game bus: re-read the alias file on every world/server join, so edits apply without a restart. */
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event)
    {
        ToolAliases.reload();
        AutoSwapClient.clear(); // forces a fresh pre-selection scan with the new aliases
        MegaToolPoke.clearRemote();
    }

    /** Game bus: warn players that a swapped-out tool is carrying their Mega Tools tool. */
    public static void onItemTooltip(ItemTooltipEvent event)
    {
        SwapOrigin origin = event.getItemStack().get(ModDataComponents.SWAP_ORIGIN.get());
        if (origin != null)
        {
            event.getToolTip().add(Component.translatable("tooltip.megatools.swapped",
                    origin.tool().getHoverName(), ModKeyMappings.SWAP_TOOL.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.GOLD));
        }
    }
}
