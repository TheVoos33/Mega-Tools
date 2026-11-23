package com.thevoos.megatools.tool;

import com.thevoos.megatools.MegaTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which blocks the tools light like TNT (Support Tool + Ignite toggle, right click).
 *
 * Vanilla TNT and modded TNT (Applied Energistics 2's Tiny TNT and the like) only react to the real flint and
 * steel item, so the tool lights them itself: it calls the block's onCaughtFire and removes the block, exactly
 * what flint and steel (and fire spreading onto the block) does.
 *
 * A block counts as TNT when it is a TntBlock or overrides Forge's onCaughtFire (IForgeBlock), the hook
 * fire uses to set off explosives. Its default does nothing, so a block that overrides it wants to react to
 * fire. No list of mods is needed: every explosive that fire can set off is lit by the tool too.
 */
public final class Ignitables
{
    /** Per block class: does it override onCaughtFire? Looked up once per class. */
    private static final Map<Class<?>, Boolean> CACHE = new ConcurrentHashMap<>();

    private Ignitables() {}

    public static boolean isTnt(BlockState state)
    {
        Block block = state.getBlock();
        return block instanceof TntBlock || CACHE.computeIfAbsent(block.getClass(), Ignitables::overridesOnCaughtFire);
    }

    private static boolean overridesOnCaughtFire(Class<?> blockClass)
    {
        try
        {
            Class<?> declaring = blockClass.getMethod("onCaughtFire", BlockState.class, Level.class, BlockPos.class,
                    Direction.class, LivingEntity.class).getDeclaringClass();
            // The default lives in the interface (IForgeBlock); any class that declares it overrides it.
            return !declaring.isInterface();
        }
        catch (NoSuchMethodException | SecurityException e)
        {
            MegaTools.LOGGER.debug("Could not check {} for onCaughtFire", blockClass.getName(), e);
            return false;
        }
    }
}
