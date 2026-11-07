package com.westerosblocks.block.custom;

import com.westerosblocks.data.BlockDefinition;
import com.westerosblocks.utils.ModProperties;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;



public class WCCuboidNSEWBlock extends WCCuboidBlock {
    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;

    public static class Factory extends BlockFactory {
        @Override
        public Block buildBlockClass(BlockDefinition definition) {
            AbstractBlock.Settings settings = definition.makeSettings();
            ModProperties.StateProperty stateProperty = definition.buildStateProperty();
            boolean doToggleOnUse = definition.toggleOnUse();

            return StagedProperties.builder()
                    .state(stateProperty)
                    .construct(() -> new WCCuboidNSEWBlock(settings, definition, doToggleOnUse));
        }
    }

    public WCCuboidNSEWBlock(AbstractBlock.Settings settings, BlockDefinition def, boolean doToggleOnUse) {
        super(settings, def, doToggleOnUse);

        // Set default state with facing
        BlockState defbs = this.getDefaultState()
            .with(FACING, Direction.EAST)
            .with(WATERLOGGED, false);

        if (STATE != null) {
            defbs = defbs.with(STATE, STATE.defValue);
        }
        this.setDefaultState(defbs);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        Direction[] placementDirections = ctx.getPlacementDirections();
        Direction facing = Direction.EAST;

        for (Direction direction : placementDirections) {
            if (direction == Direction.EAST || direction == Direction.WEST ||
                direction == Direction.NORTH || direction == Direction.SOUTH) {
                facing = direction.getOpposite();
                break;
            }
        }

        BlockState state = this.getDefaultState()
            .with(FACING, facing)
            .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);

        if (STATE != null) {
            state = state.with(STATE, STATE.defValue);
        }

        return state;
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected Direction shapeFacing(BlockState state) {
        return state.get(FACING);
    }
}
