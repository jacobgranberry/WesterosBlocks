package com.westerosblocks.block.custom;

import com.westerosblocks.block.shape.Box;
import com.westerosblocks.block.shape.FacingShapes;
import com.westerosblocks.data.BlockDefinition;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class WCMountedSlabBlock extends Block implements WCBlockDef {
    protected BlockDefinition def;
    private final boolean allowUnsupported;

    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<BlockHalf> HALF = Properties.BLOCK_HALF;

    /** Thin panel against the wall, for blocks that define no bounding box. */
    private static final Box DEFAULT_BOX = Box.pixels(0, 0, 0, 2, 8, 16);

    private final FacingShapes bottomShapes;
    private final FacingShapes topShapes;

    public WCMountedSlabBlock(Settings settings, BlockDefinition def, boolean allowUnsupported, boolean defaultTop) {
        super(settings);
        this.def = def;
        this.allowUnsupported = allowUnsupported;
        // The bounding box is authored facing east in the bottom half; the top half is the
        // same box moved up to the top of the block.
        Box box = def.hasBoundingBox() ? Box.of(def.getBoundingBox()) : DEFAULT_BOX;
        this.bottomShapes = FacingShapes.of(Direction.EAST, box);
        this.topShapes = FacingShapes.of(Direction.EAST, box.atTop());
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(HALF, defaultTop ? BlockHalf.TOP : BlockHalf.BOTTOM));
    }

    public static class Factory extends BlockFactory {
        @Override
        public Block buildBlockClass(BlockDefinition definition) {
            AbstractBlock.Settings settings = definition.makeSettings();
            boolean allowUnsupported = definition.isAllowUnsupported();
            boolean defaultTop = definition.isMountedSlabDefaultTop();
            return new WCMountedSlabBlock(settings, definition, allowUnsupported, defaultTop);
        }
    }

    private VoxelShape shapeFor(BlockState state) {
        return (state.get(HALF) == BlockHalf.TOP ? topShapes : bottomShapes).get(state.get(FACING));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState state = this.getDefaultState();
        WorldView world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();

        // Find a valid wall-facing direction
        for (Direction direction : ctx.getPlacementDirections()) {
            if (direction.getAxis().isHorizontal()) {
                Direction opposite = direction.getOpposite();
                state = state.with(FACING, opposite);
                if (state.canPlaceAt(world, pos)) {
                    // Determine top/bottom half from hit position
                    Direction clickedFace = ctx.getSide();
                    BlockHalf half = (clickedFace == Direction.DOWN ||
                            (clickedFace != Direction.UP && ctx.getHitPos().y - pos.getY() > 0.5))
                            ? BlockHalf.TOP : BlockHalf.BOTTOM;
                    return state.with(HALF, half);
                }
            }
        }

        return null;
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

        return attachState.isSideSolidFullSquare(world, attachPos, facing);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                 WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction.getOpposite() == state.get(FACING) && !this.canPlaceAt(state, world, pos)) {
            return Blocks.AIR.getDefaultState();
        }

        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeFor(state);
    }

    // The collision/outline shape is a full-footprint slab, but the visible model is only a thin
    // brace. Returning an empty culling shape stops the block from occluding its neighbors' faces
    // (which made the block above a top-half brace, or below a bottom-half brace, appear transparent).
    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.empty();
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    public BlockDefinition getDefinition() {
        return def;
    }
}
