package com.thevoos.megatools.block;

import com.thevoos.megatools.MegaTools;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Blocks. Only the invisible tool light, which deliberately has no item. */
public final class ModBlocks
{
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MegaTools.MODID);

    // registerBlock sets the block's id on its properties (required since 1.21.2).
    public static final DeferredBlock<ToolLightBlock> TOOL_LIGHT = BLOCKS.registerBlock("tool_light",
            ToolLightBlock::new, properties -> properties
                    .replaceable()                                   // anything can be placed into it
                    .noCollision()
                    .noOcclusion()                                   // never hides neighbouring faces
                    .noLootTable()
                    .strength(-1.0F, 3600000.8F)                     // can't be mined (it can't be targeted anyway)
                    .pushReaction(PushReaction.DESTROY)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .lightLevel(state -> state.getValue(ToolLightBlock.LEVEL)));

    private ModBlocks() {}

    public static void register(IEventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
