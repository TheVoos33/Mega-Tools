package com.thevoos.megatools.network;

import com.thevoos.megatools.component.SwapRules;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.menu.SwapRulesMenu;
import com.thevoos.megatools.menu.ToolMenu;
import com.thevoos.megatools.tool.MegaToolMining;
import com.thevoos.megatools.tool.PlateHelper;
import com.thevoos.megatools.tool.ToolData;
import com.thevoos.megatools.tool.ToolLightManager;
import com.thevoos.megatools.tool.ToolSwapper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Server-side handling of the client packets. enqueueWork() makes sure game state is
 * only touched on the main server thread.
 */
public final class ServerPayloadHandler
{
    private ServerPayloadHandler() {}

    /** Ctrl + R -> open the GUI of the held Mega Tools tool. */
    public static void handleOpenGui(OpenToolGuiPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player))
                return;
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof MultiToolItem))
                return;

            closeOpenMenuQuietly(player); // coming back from the Swap Rules GUI
            int toolSlot = player.getInventory().selected;
            player.openMenu(
                    new SimpleMenuProvider((containerId, inventory, p) -> new ToolMenu(containerId, inventory, toolSlot), held.getHoverName()),
                    buf -> buf.writeVarInt(toolSlot)); // read by the client-side ToolMenu constructor
        });
    }

    /**
     * Closes the player's open menu on the server only. openMenu() would do it too, but it also tells the
     * client to close its screen first: for a moment no screen is open, and the game moves the mouse
     * pointer to the middle of the window. Switching between the tool GUI and the Swap Rules GUI should
     * leave the pointer where it is, so the client just replaces one screen with the other.
     */
    private static void closeOpenMenuQuietly(ServerPlayer player)
    {
        if (player.containerMenu != player.inventoryMenu)
            player.doCloseContainer(); // returns an item on the cursor to the inventory
    }

    /** "Custom Rule" button of the tool GUI -> open the Swap Rules GUI of the held tool (replaces the tool GUI). */
    public static void handleOpenSwapRules(OpenSwapRulesPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player))
                return;
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof MultiToolItem multiTool) || !multiTool.getTier().hasAutoSwap())
                return;

            closeOpenMenuQuietly(player); // coming from the tool GUI
            int toolSlot = player.getInventory().selected;
            player.openMenu(
                    new SimpleMenuProvider((containerId, inventory, p) -> new SwapRulesMenu(containerId, inventory, toolSlot),
                            Component.translatable("gui.megatools.swap_rules")),
                    buf -> buf.writeVarInt(toolSlot)); // read by SwapRulesMenu#fromNetwork
        });
    }

    /** Swap Rules GUI -> store the new rules on the held tool. Synced back to the client with the item. */
    public static void handleSetSwapRules(SetSwapRulesPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player))
                return;
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof MultiToolItem multiTool) || !multiTool.getTier().hasAutoSwap())
                return;
            ToolData.setSwapRules(held, payload.rules().sanitized()); // never trust sizes from the client
        });
    }

    /**
     * Default rules of the player's own config file for the held tool: remember them on the tool and add
     * the ones it hasn't received before as ordinary rules (see ApplyDefaultRulesPayload).
     */
    public static void handleApplyDefaultRules(ApplyDefaultRulesPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player))
                return;
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof MultiToolItem multiTool) || !multiTool.getTier().hasAutoSwap())
                return;

            Set<SwapRules.Rule> applied = new LinkedHashSet<>(ToolData.getAppliedDefaults(held).rules());
            boolean anyNew = false;
            for (SwapRules.Rule rule : payload.applied().rules())
                if (applied.size() < SwapRules.MAX_APPLIED && applied.add(rule))
                    anyNew = true;
            if (!anyNew)
                return; // sent twice, or the list is full: never add a default a second time
            ToolData.setAppliedDefaults(held, new SwapRules(List.copyOf(applied)));
            ToolData.setSwapRules(held, ToolData.getSwapRules(held).withAll(payload.add()));
        });
    }

    /** The client's shader pack lights the held tool itself: stop/resume placing light blocks for this player. */
    public static void handleShaderLight(ShaderLightPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (context.player() instanceof ServerPlayer player)
                ToolLightManager.setShaderLight(player, payload.shaderActive());
        });
    }

    /** Shift + R -> switch primary/secondary enchantment plate. */
    public static void handleSwitchPlate(SwitchPlatePayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (context.player() instanceof ServerPlayer player)
                PlateHelper.switchPlate(player);
        });
    }

    /** R -> swap to the pre-selected stored tool, or back to the Mega Tools tool. */
    public static void handleSwapTool(SwapToolPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (context.player() instanceof ServerPlayer player)
                ToolSwapper.handleSwap(player, payload.slot());
        });
    }

    /** Title edited in the GUI -> rename the held tool (blank or default name removes the custom name). */
    public static void handleRename(RenameToolPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (!(context.player() instanceof ServerPlayer player))
                return;
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof MultiToolItem))
                return;

            String name = StringUtil.filterText(payload.name()).trim();   // strips illegal characters
            if (name.length() > RenameToolPayload.MAX_LENGTH)
                name = name.substring(0, RenameToolPayload.MAX_LENGTH);

            String defaultName = held.getItem().getName(held).getString();
            if (name.isEmpty() || name.equals(defaultName))
                held.remove(DataComponents.CUSTOM_NAME);
            else
                held.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        });
    }

    /** The client's Mega Tool is breaking a block right now (see MegaToolMining). */
    public static void handleMegaToolBreak(MegaToolBreakPayload payload, IPayloadContext context)
    {
        context.enqueueWork(() ->
        {
            if (context.player() instanceof ServerPlayer player && MegaToolMining.isMegaTool(player.getMainHandItem()))
                MegaToolMining.grantServerBreak(player);
        });
    }
}
