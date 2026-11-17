package com.thevoos.megatools.block;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import com.thevoos.megatools.MegaTools;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

/** Blocks. Only the invisible tool light, which deliberately has no item. */
public final class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MegaTools.MODID);

    public static final RegistryObject<ToolLightBlock> TOOL_LIGHT = BLOCKS.register("tool_light",
            () -> new ToolLightBlock(BlockBehaviour.Properties.of()
                    .replaceable()                                   // anything can be placed into it
                    .noCollission()
                    .noOcclusion()                                   // never hides neighbouring faces
                    .noLootTable()
                    .strength(-1.0F, 3600000.8F)                     // can't be mined (it can't be targeted anyway)
                    .pushReaction(PushReaction.DESTROY)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
                    .lightLevel(state -> state.getValue(ToolLightBlock.LEVEL))));

    private ModBlocks() {}

    public static void register(IEventBus eventBus)
    {
        BLOCKS.register(eventBus);
    }
}
