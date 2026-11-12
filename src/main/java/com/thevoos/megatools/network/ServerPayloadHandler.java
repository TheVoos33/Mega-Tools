package com.thevoos.megatools.network;

import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.menu.ToolMenu;
import com.thevoos.megatools.tool.MegaToolMining;
import com.thevoos.megatools.tool.PlateHelper;
import com.thevoos.megatools.tool.ToolSwapper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringUtil;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

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

            int toolSlot = player.getInventory().selected;
            player.openMenu(
                    new SimpleMenuProvider((containerId, inventory, p) -> new ToolMenu(containerId, inventory, toolSlot), held.getHoverName()),
                    buf -> buf.writeVarInt(toolSlot)); // read by the client-side ToolMenu constructor
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
