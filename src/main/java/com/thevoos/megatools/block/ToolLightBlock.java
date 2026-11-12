package com.thevoos.megatools.block;

import com.mojang.serialization.MapCodec;
import com.thevoos.megatools.tool.ToolLightManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible light placed at the player's position while they hold a Mega Tools tool with a light
 * source in it (see ToolLightManager). It has no shape, no collision, no drops and no item, can't be
 * targeted, never blocks placement, and can be waterlogged so it also works underwater.
 *
 * It removes itself: every CHECK_TICKS it asks ToolLightManager whether a player's light still
 * uses this position, and turns back into air/water if not. Scheduled block ticks are saved with
 * the chunk, so a light left behind (crash, unloaded chunk) cleans up when the chunk loads again.
 */
public class ToolLightBlock extends Block implements SimpleWaterloggedBlock
{
    public static final MapCodec<ToolLightBlock> CODEC = simpleCodec(ToolLightBlock::new);
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;        // 0-15
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final int CHECK_TICKS = 40; // 2 seconds

    public ToolLightBlock(Properties properties)
    {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 14).setValue(WATERLOGGED, false));
    }

    @Override
    public MapCodec<? extends Block> codec()
    {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(LEVEL, WATERLOGGED);
    }

    // ---- invisible and untargetable ----

    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return Shapes.empty();
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos)
    {
        return state.getFluidState().isEmpty();
    }

    // ---- water support ----

    @Override
    public FluidState getFluidState(BlockState state)
    {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        if (state.getValue(WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return state;
    }

    // ---- self cleaning ----

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston)
    {
        if (!level.isClientSide())
            level.scheduleTick(pos, this, CHECK_TICKS);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        if (ToolLightManager.isActiveLight(level, pos))
            level.scheduleTick(pos, this, CHECK_TICKS);
        else
            level.setBlock(pos, emptyState(state), ToolLightManager.SET_FLAGS);
    }

    /** What the block turns back into: water if it was placed in water, otherwise air. */
    public static BlockState emptyState(BlockState lightState)
    {
        return lightState.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }
}
