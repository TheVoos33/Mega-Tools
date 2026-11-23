package com.thevoos.megatools.tool;

import com.thevoos.megatools.component.ToolSettings;
import com.thevoos.megatools.items.MultiToolItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Stops a tool from breaking blocks and/or attacking, for two reasons:
 *  - it is broken (1 durability left, see MultiToolItem#isBroken): no mining and no combat until it is
 *    repaired;
 *  - the player switched the Break or Attack button off in the tool GUI (ToolSettings#BREAK_OFF /
 *    ATTACK_OFF). These apply in every game mode, creative included. Attack only covers mobs and
 *    players: item frames, boats, minecarts and the like can still be hit. Break only covers blocks that
 *    take more than one hit with an empty hand: torches, crops, sugar cane, flowers... (no hardness at all)
 *    still break, so the player doesn't have to switch tools for them.
 * A Mega Tool also can't break the Allthemodium ores its tier is too weak for (ToolTier#tooWeakFor); the
 * server refuses those breaks here, the client side is in MegaToolMining.
 * Right-click abilities are not affected by the two buttons (they have their own); for a broken tool
 * they are blocked inside MultiToolItem itself. The tool's GUI (Ctrl + R), storage, plates and auto-swap
 * always keep working, so stored items can always be taken out.
 *
 * The mining speed is forced to 0 as well, in MegaToolMining#finalDigSpeed.
 */
public final class DisabledToolHandler
{
    private DisabledToolHandler() {}

    /** Both sides: stops the block from even starting to break. */
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event)
    {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (MultiToolItem.isBroken(held))
        {
            event.setCanceled(true);
            MultiToolItem.showBrokenMessage(player);
        }
        else if (isSwitchedOff(held, ToolSettings.BREAK_OFF)
                && !breaksInstantlyByHand(event.getLevel(), event.getPos(), event.getLevel().getBlockState(event.getPos())))
        {
            // Survival: only the mining is skipped. The block still gets its left click, which other
            // mods use (taking items out of drawers, AE2 parts...). Creative breaks on the click itself,
            // so there the whole click is cancelled.
            if (player.getAbilities().instabuild)
                event.setCanceled(true);
            else
                event.setUseItem(TriState.FALSE);
            showMessage(player, "message.megatools.break_disabled");
        }
    }

    /** Server: final guarantee, also for breaks that don't start with a left click (other mods' area mining...). */
    public static void onBlockBroken(BlockEvent.BreakEvent event)
    {
        Player player = event.getPlayer();
        if (player == null)
            return;
        ItemStack held = player.getMainHandItem();
        if (MultiToolItem.isBroken(held))
            event.setCanceled(true);
        else if (isSwitchedOff(held, ToolSettings.BREAK_OFF) && !breaksInstantlyByHand(event.getLevel(), event.getPos(), event.getState()))
            event.setCanceled(true);
        else if (held.getItem() instanceof MultiToolItem tool && !player.getAbilities().instabuild
                && tool.getTier().tooWeakFor(event.getState()))
            event.setCanceled(true);
    }

    /**
     * Blocks an empty hand breaks in one hit: no hardness at all (torches, crops, sugar cane, flowers, short
     * grass, redstone dust...). These still break while the Break button is off.
     */
    public static boolean breaksInstantlyByHand(BlockGetter level, BlockPos pos, BlockState state)
    {
        return state.getDestroySpeed(level, pos) == 0.0F;
    }

    /** Both sides: no hit at all on a mob or player, so no damage, knockback, sweep or durability loss. */
    public static void onAttackEntity(AttackEntityEvent event)
    {
        Player player = event.getEntity();
        ItemStack held = player.getMainHandItem();
        if (MultiToolItem.isBroken(held))
        {
            event.setCanceled(true);
            MultiToolItem.showBrokenMessage(player);
        }
        else if (isSwitchedOff(held, ToolSettings.ATTACK_OFF) && isAttackable(event.getTarget()))
        {
            event.setCanceled(true);
            showMessage(player, "message.megatools.attack_disabled");
        }
    }

    /**
     * What the Attack button protects: mobs and players. Everything else a left click "attacks" is an
     * object that is hit to be picked up or removed (item frames, paintings, boats, minecarts, armor
     * stands, end crystals...), and stays hittable.
     */
    private static boolean isAttackable(Entity target)
    {
        return target instanceof LivingEntity && !(target instanceof ArmorStand);
    }

    private static boolean isSwitchedOff(ItemStack stack, int flag)
    {
        return stack.getItem() instanceof MultiToolItem && ToolData.hasFlag(stack, flag);
    }

    /** Action-bar hint (client side, no spam to the server). */
    private static void showMessage(Player player, String key)
    {
        if (player.level().isClientSide())
            player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.YELLOW), true);
    }
}
