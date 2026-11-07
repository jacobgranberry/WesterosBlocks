package com.westerosblocks.block.custom;

import com.westerosblocks.block.shape.Box;
import com.westerosblocks.block.shape.FacingShapes;
import com.westerosblocks.data.BlockDefinition;
import net.minecraft.block.*;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

import java.util.List;

public class WCMountedBlock extends Block implements WCBlockDef {
    protected BlockDefinition def;
    private final boolean allowUnsupported;

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    /** Thin panel against the wall, for blocks that define no geometry. */
    private static final FacingShapes DEFAULT_SHAPES = FacingShapes.of(Direction.EAST, Box.pixels(0, 0, 0, 2, 16, 16));

    private final FacingShapes shapes;

    public WCMountedBlock(Settings settings, BlockDefinition def, FacingShapes shapes, boolean allowUnsupported) {
        super(settings);
        this.def = def;
        this.allowUnsupported = allowUnsupported;
        this.shapes = shapes;
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH));
    }

    public static class Factory extends BlockFactory {
        @Override
        public Block buildBlockClass(BlockDefinition definition) {
            AbstractBlock.Settings settings = definition.makeSettings();
            FacingShapes shapes = createShapes(definition);
            boolean allowUnsupported = definition.isAllowUnsupported();
            return new WCMountedBlock(settings, definition, shapes, allowUnsupported);
        }

        /**
         * Cuboids come first, then the bounding box, then a thin panel against the wall.
         * The bounding box is authored facing east, the panel's side being the wall.
         *
         * <p>The cuboid outline has always been built as if the cuboids were authored facing
         * south (the NORTH outline is the cuboids turned half way round), although the custom
         * models they describe face north at 0 degrees. Kept as is until a block that uses
         * cuboids is checked in-game.
         */
        private static FacingShapes createShapes(BlockDefinition definition) {
            List<BlockDefinition.CuboidElement> cuboids = definition.getCuboids();
            if (cuboids != null && !cuboids.isEmpty()) {
                return FacingShapes.of(Direction.SOUTH, Box.of(cuboids));
            }
            if (definition.hasBoundingBox()) {
                return FacingShapes.of(Direction.EAST, Box.of(definition.getBoundingBox()));
            }
            return DEFAULT_SHAPES;
        }
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = this.getDefaultState();
        WorldView world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();

        // Try each horizontal direction to find a valid wall
        for (Direction direction : ctx.getPlacementDirections()) {
            if (direction.getAxis().isHorizontal()) {
                // Block faces opposite of the clicked direction (away from wall)
                Direction opposite = direction.getOpposite();
                state = state.with(FACING, opposite);
                if (state.canPlaceAt(world, pos)) {
                    return state;
                }
            }
        }

        return null; // Cannot place if no valid wall found
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (this.allowUnsupported) {
            return true;
        }

        Direction facing = state.get(FACING);
        Direction attachmentDir = facing.getOpposite();
        BlockPos attachPos = pos.offset(attachmentDir);
        BlockState attachState = world.getBlockState(attachPos);

        // Check if the block behind is a solid full square face
        return attachState.isSideSolidFullSquare(world, attachPos, facing);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                 WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        // Check if the supporting wall was removed
        if (direction.getOpposite() == state.get(FACING) && !this.canPlaceAt(state, world, pos)) {
            return Blocks.AIR.getDefaultState();
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapes.get(state.get(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapes.get(state.get(FACING));
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    /**
     * Gets the BlockDefinition for this block.
     * @return BlockDefinition if block was created from JSON, null if created programmatically
     */
    public BlockDefinition getDefinition() {
        return def;
    }
}
