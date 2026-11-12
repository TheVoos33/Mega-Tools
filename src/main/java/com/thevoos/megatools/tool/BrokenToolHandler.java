package com.thevoos.megatools.tool;

import com.thevoos.megatools.items.MultiToolItem;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Makes a broken tool (1 durability left, see MultiToolItem#isBroken) unusable for mining and combat.
 * Right-click abilities are blocked inside MultiToolItem itself. The tool's GUI (Ctrl + R), storage,
 * plates and auto-swap keep working, so stored items can always be taken out.
 */
public final class BrokenToolHandler
{
    private BrokenToolHandler() {}

    /** Both sides: cancelling stops the block from even starting to break (no dig packet is sent). */
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        Player player = event.getEntity();
        if (MultiToolItem.isBroken(player.getMainHandItem()))
        {
            event.setCanceled(true);
            MultiToolItem.showBrokenMessage(player);
        }
    }

    /** Both sides, LOWEST priority so it overrides the Mega Tool's instant-break speed: no progress. */
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event)
    {
        if (MultiToolItem.isBroken(event.getEntity().getMainHandItem()))
            event.setNewSpeed(0.0F);
    }

    /** Both sides: a broken tool can't hit anything. */
    public static void onAttackEntity(AttackEntityEvent event)
    {
        Player player = event.getEntity();
        if (MultiToolItem.isBroken(player.getMainHandItem()))
        {
            event.setCanceled(true);
            MultiToolItem.showBrokenMessage(player);
        }
    }
}
