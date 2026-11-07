package com.westerosblocks.block.custom;

import com.westerosblocks.block.shape.Box;
import com.westerosblocks.block.shape.FacingShapes;
import com.westerosblocks.data.BlockDefinition;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class WCCuboidNSEWStackBlock extends WCCuboidBlock implements WCBlockDef {
    public static final DirectionProperty FACING = DirectionProperty.of("facing", Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH);
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    protected final boolean allowHalfBreak;
    // Each half's outline comes from its stack element's bounding box, authored facing east.
    private final FacingShapes lowerShapes;
    private final FacingShapes upperShapes;

    public static class Factory extends BlockFactory {
        @Override
        public Block buildBlockClass(BlockDefinition definition) {
            AbstractBlock.Settings settings = definition.makeSettings().nonOpaque();
            boolean doAllowHalfBreak = definition.isAllowHalfBreak();

            return new WCCuboidNSEWStackBlock(settings, definition, doAllowHalfBreak);
        }
    }

    public WCCuboidNSEWStackBlock(AbstractBlock.Settings settings, BlockDefinition def, boolean doAllowHalfBreak) {
        super(settings, def, false);
        this.allowHalfBreak = doAllowHalfBreak;

        boolean hasStack = def != null && def.hasStackElements() && def.getStack().size() >= 2;
        this.lowerShapes = shapesFor(hasStack ? def.getStack().get(0) : null);
        this.upperShapes = shapesFor(hasStack ? def.getStack().get(1) : null);

        this.setDefaultState(this.getDefaultState()
            .with(WATERLOGGED, false)
            .with(HALF, DoubleBlockHalf.LOWER)
            .with(FACING, Direction.EAST));
    }

    private static FacingShapes shapesFor(BlockDefinition.StackElement element) {
        if (element != null && element.hasBoundingBox()) {
            return FacingShapes.of(Direction.EAST, Box.of(element.getBoundingBox()));
        }
        return FacingShapes.uniform(VoxelShapes.fullCube());
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(FACING, HALF);
    }

    @Override
    protected VoxelShape shapeFor(BlockState state) {
        FacingShapes half = state.get(HALF) == DoubleBlockHalf.LOWER ? lowerShapes : upperShapes;
        return half.get(state.get(FACING));
    }

    @Override
    public VoxelShape getCullingShape(BlockState state, net.minecraft.world.BlockView world, BlockPos pos) {
        return shapeFor(state);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos blockpos = ctx.getBlockPos();
        if (blockpos.getY() < ctx.getWorld().getTopY() && ctx.getWorld().getBlockState(blockpos.up()).canReplace(ctx)) {
            FluidState fluidstate = ctx.getWorld().getFluidState(ctx.getBlockPos());
            Direction[] adirection = ctx.getPlacementDirections();
            Direction dir = Direction.EAST; // Default
            for (Direction d : adirection) {
                if (d == Direction.EAST || d == Direction.WEST || d == Direction.NORTH || d == Direction.SOUTH) {
                    dir = d;
                    break;
                }
            }
            return this.getDefaultState()
                .with(FACING, dir)
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(WATERLOGGED, fluidstate.isIn(FluidTags.WATER));
        } else {
            return null;
        }
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (allowHalfBreak) {
            return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        }

        DoubleBlockHalf doubleblockhalf = state.get(HALF);
        if (direction.getAxis() != Direction.Axis.Y
                || doubleblockhalf == DoubleBlockHalf.LOWER != (direction == Direction.UP)
                || neighborState.isOf(this) && neighborState.get(HALF) != doubleblockhalf) {
            return doubleblockhalf == DoubleBlockHalf.LOWER
                    && direction == Direction.DOWN
                    && !state.canPlaceAt(world, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        } else {
            return Blocks.AIR.getDefaultState();
        }
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        BlockPos above = pos.up();
        FluidState fluidstate = world.getFluidState(above);
        world.setBlockState(above, this.getDefaultState()
            .with(FACING, state.get(FACING))
            .with(HALF, DoubleBlockHalf.UPPER)
            .with(WATERLOGGED, fluidstate.isIn(FluidTags.WATER)), 3);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (this.allowHalfBreak || (state.get(HALF) != DoubleBlockHalf.UPPER)) {
            return super.canPlaceAt(state, world, pos);
        } else {
            BlockState blockstate = world.getBlockState(pos.down());
            if (state.getBlock() != this) {
                return super.canPlaceAt(state, world, pos);
            }
            return blockstate.isOf(this) && blockstate.get(HALF) == DoubleBlockHalf.LOWER;
        }
    }

    @Override
    public BlockDefinition getDefinition() {
        return this.def;
    }
}
