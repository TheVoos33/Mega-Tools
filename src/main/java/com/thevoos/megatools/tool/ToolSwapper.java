package com.thevoos.megatools.tool;

import com.thevoos.megatools.component.LastSwap;
import com.thevoos.megatools.component.ModDataComponents;
import com.thevoos.megatools.component.SwapOrigin;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.menu.ToolMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;

/**
 * Tool auto-swap (R key), executed on the server.
 * <p>
 * Swap OUT : the pre-selected stored tool is removed from wrench storage and put in the main hand.
 * The Mega Tools tool is saved INSIDE that tool (SwapOrigin component).
 * Swap BACK: pressing R while holding a tool with a SwapOrigin puts the Mega Tools tool back in the
 * hand and returns the (possibly damaged) tool to its storage slot.
 */
public final class ToolSwapper
{
    /**
     * Slot value the client sends when it wants to swap back.
     */
    public static final int SWAP_BACK = -1;
    /** Slot value for "swap to the last tool swapped to" (Space + R), whatever block is targeted. */
    public static final int SWAP_LAST = -2;

    private ToolSwapper()
    {
    }

    public static void handleSwap(ServerPlayer player, int requestedSlot)
    {
        // The tool GUI holds a copy of the storage. The client closes it before sending this, but a
        // modified client could swap and take the same stored tool out of the still-open GUI in one tick.
        if (player.containerMenu instanceof ToolMenu)
            player.closeContainer();

        ItemStack held = player.getMainHandItem();

        SwapOrigin origin = held.get(ModDataComponents.SWAP_ORIGIN.get());
        if (origin != null)
        {
            swapBack(player, held, origin);
            return;
        }
        if (requestedSlot == SWAP_BACK)
            return;

        if (!(held.getItem() instanceof MultiToolItem multiTool) || !multiTool.getTier().hasAutoSwap())
            return;

        int size = multiTool.getTier().storageSlots();
        NonNullList<ItemStack> storage = ToolData.getStorage(held, size);
        if (requestedSlot == SWAP_LAST)
        {
            requestedSlot = findLastSwapped(held, storage);
            if (requestedSlot < 0)
            {
                LastSwap last = held.get(ModDataComponents.LAST_SWAP.get());
                player.displayClientMessage(last == null || last.item() == Items.AIR
                        ? Component.translatable("message.megatools.no_last_tool")
                        : Component.translatable("message.megatools.last_tool_gone", last.item().getDescription()), true);
                return;
            }
        }
        if (requestedSlot < 0 || requestedSlot >= size)
            return; // never trust slot indices from the client

        ItemStack stored = storage.get(requestedSlot);
        if (stored.isEmpty())
            return;

        // Mega Tools tool with that slot emptied...
        ItemStack megaTool = held.copy();
        storage.set(requestedSlot, ItemStack.EMPTY);
        ToolData.setStorage(megaTool, storage);
        megaTool.set(ModDataComponents.LAST_SWAP.get(), new LastSwap(requestedSlot, stored.getItem()));

        // ...travels inside the swapped-out tool.
        ItemStack swappedOut = stored.copy();
        swappedOut.set(ModDataComponents.SWAP_ORIGIN.get(), new SwapOrigin(megaTool, requestedSlot));
        player.setItemInHand(InteractionHand.MAIN_HAND, swappedOut);

        playSwapSound(player);
    }

    private static void swapBack(ServerPlayer player, ItemStack held, SwapOrigin origin)
    {
        ItemStack megaTool = origin.tool().copy();
        ItemStack returned = held.copy();
        returned.remove(ModDataComponents.SWAP_ORIGIN.get());

        if (megaTool.getItem() instanceof MultiToolItem multiTool)
        {
            int size = multiTool.getTier().storageSlots();
            NonNullList<ItemStack> storage = ToolData.getStorage(megaTool, size);

            // Original slot if still free, otherwise the first free slot.
            int target = origin.slot() >= 0 && origin.slot() < size && storage.get(origin.slot()).isEmpty()
                    ? origin.slot() : firstEmpty(storage);
            if (target >= 0)
            {
                storage.set(target, returned);
                ToolData.setStorage(megaTool, storage);
                returned = ItemStack.EMPTY;
            }
        }

        player.setItemInHand(InteractionHand.MAIN_HAND, megaTool);
        giveOrDrop(player, returned); // only if storage was somehow full
        playSwapSound(player);
    }

    /**
     * Safety net: if the swapped-out tool breaks from durability loss, give the Mega Tools tool back
     * instead of losing it with the broken item.
     */
    public static void onItemDestroyed(PlayerDestroyItemEvent event)
    {
        SwapOrigin origin = event.getOriginal().get(ModDataComponents.SWAP_ORIGIN.get());
        if (origin == null)
            return;

        Player player = event.getEntity();
        ItemStack megaTool = origin.tool().copy();
        if (player.getMainHandItem().isEmpty())
            player.setItemInHand(InteractionHand.MAIN_HAND, megaTool);
        else
            giveOrDrop(player, megaTool);

        player.displayClientMessage(Component.translatable("message.megatools.swap_restored", megaTool.getHoverName()), true);
    }

    /** Storage slot of the last swapped-to tool: its old slot if it's still there, else the first of that item. */
    private static int findLastSwapped(ItemStack tool, NonNullList<ItemStack> storage)
    {
        LastSwap last = tool.get(ModDataComponents.LAST_SWAP.get());
        if (last == null || last.item() == Items.AIR) // air: the tool's mod was removed (empty slots "are" air)
            return -1;
        if (last.slot() >= 0 && last.slot() < storage.size() && storage.get(last.slot()).is(last.item()))
            return last.slot();
        for (int i = 0; i < storage.size(); i++)
            if (storage.get(i).is(last.item()))
                return i;
        return -1;
    }

    private static int firstEmpty(NonNullList<ItemStack> items)
    {
        for (int i = 0; i < items.size(); i++)
            if (items.get(i).isEmpty()) return i;
        return -1;
    }

    private static void giveOrDrop(Player player, ItemStack stack)
    {
        if (!stack.isEmpty() && !player.getInventory().add(stack))
            player.drop(stack, false);
    }

    private static void playSwapSound(Player player)
    {
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.5F, 1.4F);
    }
}