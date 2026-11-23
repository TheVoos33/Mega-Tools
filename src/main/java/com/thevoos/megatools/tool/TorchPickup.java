package com.thevoos.megatools.tool;

import com.thevoos.megatools.component.StoredTorches;
import com.thevoos.megatools.items.MultiToolItem;
import com.thevoos.megatools.menu.ToolMenu;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Torches the player picks up go straight into the torch slot of a tool (every paxel and Mega Tool tier) that
 * already holds that kind of torch, instead of the inventory. Only what doesn't fit (the slot holds up to
 * StoredTorches.MAX) goes to the inventory. A tool with an empty torch slot is left alone: then torches are
 * picked up as usual.
 *
 * Which tool: the one in the main hand, then the off hand, then the inventory from the first slot.
 * While the player has a tool GUI open nothing is redirected (that GUI holds its own copy of the torch slot).
 */
public final class TorchPickup
{
    private TorchPickup() {}

    /** Game bus, server: fired when a player touches an item on the ground, before vanilla picks it up. */
    public static void onItemPickup(ItemEntityPickupEvent.Pre event)
    {
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
        ItemStack stack = entity.getItem();
        if (player.level().isClientSide() || event.canPickup().isFalse() || !LightSources.isTorch(stack)
                || player.containerMenu instanceof ToolMenu)
            return;
        // The same conditions vanilla checks before picking up (unless another mod forced the pickup).
        UUID target = entity.getTarget();
        if (!event.canPickup().isTrue() && (entity.hasPickUpDelay() || (target != null && !target.equals(player.getUUID()))))
            return;

        Item item = stack.getItem();
        int before = stack.getCount();
        for (ItemStack tool : toolsWithTorches(player.getInventory(), stack))
        {
            ItemStack stored = ToolData.getTorches(tool);
            int moved = Math.min(StoredTorches.MAX - stored.getCount(), stack.getCount());
            if (moved <= 0)
                continue;
            ToolData.setTorches(tool, stored.copyWithCount(stored.getCount() + moved));
            stack.shrink(moved);
            if (stack.isEmpty())
                break;
        }
        int taken = before - stack.getCount();
        if (taken == 0)
            return;

        // Like vanilla: pickup animation + sound for nearby players, statistics.
        player.take(entity, taken);
        player.awardStat(Stats.ITEM_PICKED_UP.get(item), taken);
        if (stack.isEmpty())
        {
            entity.discard();
            stack.setCount(taken);
            player.onItemPickup(entity);
            event.setCanPickup(TriState.FALSE); // nothing left for vanilla to pick up
        }
        // else: vanilla puts the rest into the inventory as usual
    }

    /** Tools whose torch slot holds this kind of torch and isn't full, in pickup order. */
    private static List<ItemStack> toolsWithTorches(Inventory inventory, ItemStack torch)
    {
        List<ItemStack> tools = new ArrayList<>();
        addIfMatching(tools, inventory.getSelected(), torch);
        addIfMatching(tools, inventory.offhand.getFirst(), torch);
        for (ItemStack stack : inventory.items)
            if (stack != inventory.getSelected())
                addIfMatching(tools, stack, torch);
        return tools;
    }

    private static void addIfMatching(List<ItemStack> tools, ItemStack tool, ItemStack torch)
    {
        if (!(tool.getItem() instanceof MultiToolItem))
            return;
        ItemStack stored = ToolData.getTorches(tool);
        if (!stored.isEmpty() && stored.getCount() < StoredTorches.MAX && ItemStack.isSameItemSameComponents(stored, torch))
            tools.add(tool);
    }
}
