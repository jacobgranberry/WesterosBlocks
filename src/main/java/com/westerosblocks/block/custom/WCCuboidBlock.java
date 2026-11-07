package com.westerosblocks.block.custom;

import com.westerosblocks.block.shape.FacingShapes;
import com.westerosblocks.data.BlockDefinition;
import com.westerosblocks.utils.ModProperties;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

import java.util.List;

public class WCCuboidBlock extends Block implements Waterloggable, WCBlockDef {
    protected BlockDefinition def;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public ModProperties.StateProperty STATE;
    protected boolean toggleOnUse = false;
    /** Outline for each definition state, in state order, by facing. */
    protected final List<FacingShapes> shapesByState;

    public static class Factory extends BlockFactory {
        @Override
        public Block buildBlockClass(BlockDefinition definition) {
            ModProperties.StateProperty stateProperty = definition.buildStateProperty();
            AbstractBlock.Settings settings = definition.applyStateLuminance(definition.makeSettings(), stateProperty);
            boolean doToggleOnUse = definition.toggleOnUse();

            return StagedProperties.builder()
                    .state(stateProperty)
                    .construct(() -> new WCCuboidBlock(settings, definition, doToggleOnUse));
        }
    }

    public WCCuboidBlock(AbstractBlock.Settings settings, BlockDefinition def, boolean doToggleOnUse) {
        super(settings);
        this.def = def;
        this.toggleOnUse = doToggleOnUse;
        // Cuboid models are authored facing east; subclasses pick the facing per state.
        this.shapesByState = FacingShapes.forStates(def, Direction.EAST);

        // Set default state
        BlockState defbs = this.getDefaultState().with(WATERLOGGED, false);
        if (STATE != null) {
            defbs = defbs.with(STATE, STATE.defValue);
        }
        this.setDefaultState(defbs);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
        STATE = StagedProperties.appendTo(builder).state();
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        BlockState bs = this.getDefaultState().with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);
        if (STATE != null) {
            bs = bs.with(STATE, STATE.defValue);
        }
        return bs;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
            WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (this.toggleOnUse) {
            ActionResult toggled = CreativeToggle.cycle(state, world, pos, player, STATE);
            if (toggled != ActionResult.PASS) {
                return toggled;
            }
        }
        return ActionResult.PASS;
    }

    /** Index of the definition state {@code state} shows. */
    protected int stateIndex(BlockState state) {
        return STATE != null ? STATE.getIndex(state.get(STATE)) : 0;
    }

    /**
     * Facing whose shape {@code state} uses. Cuboid models are authored facing east, so
     * blocks without a facing use the authored shape.
     */
    protected Direction shapeFacing(BlockState state) {
        return Direction.EAST;
    }

    protected VoxelShape shapeFor(BlockState state) {
        return shapesByState.get(stateIndex(state)).get(shapeFacing(state));
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeFor(state);
    }

    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.empty();
    }

    /**
     * Ports 1.18.2's getBlockSupportShape override. The sides shape is what walls,
     * fences, panes and redstone read via isSideSolidFullSquare/isFaceSturdy to decide
     * whether a neighbor presents a sturdy face. Cuboid blocks expose an empty support
     * shape so decorative cuboids like banners are not connected to; a non-empty
     * default would reintroduce that bug.
     */
    @Override
    public VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
        return VoxelShapes.empty();
    }

    public BlockDefinition getDefinition() {
        return def;
    }
}
