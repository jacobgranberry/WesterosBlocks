package com.westerosblocks.block.custom;

import com.westerosblocks.data.BlockDefinition;
import com.westerosblocks.utils.ModProperties;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;



public class WCCuboid16WayBlock extends WCCuboidBlock {
    public static final IntProperty ROTATION = Properties.ROTATION;

    public static class Factory extends BlockFactory {
        @Override
        public Block buildBlockClass(BlockDefinition definition) {
            AbstractBlock.Settings settings = definition.makeSettings();
            ModProperties.StateProperty stateProperty = definition.buildStateProperty();
            boolean doToggleOnUse = definition.toggleOnUse();

            return StagedProperties.builder()
                    .state(stateProperty)
                    .construct(() -> new WCCuboid16WayBlock(settings, definition, doToggleOnUse));
        }
    }

    public WCCuboid16WayBlock(AbstractBlock.Settings settings, BlockDefinition def, boolean doToggleOnUse) {
        super(settings, def, doToggleOnUse);

        // Set default state with rotation 0
        BlockState defbs = this.getDefaultState()
            .with(ROTATION, 0)
            .with(WATERLOGGED, false);

        if (STATE != null) {
            defbs = defbs.with(STATE, STATE.defValue);
        }
        this.setDefaultState(defbs);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(ROTATION);
    }

    /**
     * Shapes only follow whole quarter turns: rotations 0-3 use the east shape, 4-7 south,
     * 8-11 west and 12-15 north. The 22.5 degree steps between are model-only.
     */
    @Override
    protected Direction shapeFacing(BlockState state) {
        Direction facing = Direction.EAST;
        for (int i = 0; i < state.get(ROTATION) / 4; i++) {
            facing = facing.rotateYClockwise();
        }
        return facing;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
        int rotation = MathHelper.floor((double)(ctx.getPlayerYaw() * 16.0F / 360.0F) + 0.5D) & 15;

        BlockState state = this.getDefaultState()
            .with(ROTATION, rotation)
            .with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER);

        if (STATE != null) {
            state = state.with(STATE, STATE.defValue);
        }

        return state;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (!this.toggleOnUse) {
            return ActionResult.PASS;
        }
        // Cycle the definition's states first; blocks without states rotate instead.
        ActionResult toggled = CreativeToggle.cycle(state, world, pos, player, STATE);
        if (toggled != ActionResult.PASS) {
            return toggled;
        }
        return CreativeToggle.cycle(state, world, pos, player, ROTATION);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        int currentRotation = state.get(ROTATION);
        int rotationSteps = switch (rotation) {
            case CLOCKWISE_90 -> 4;  // 90° = 4 steps of 22.5°
            case CLOCKWISE_180 -> 8; // 180° = 8 steps
            case COUNTERCLOCKWISE_90 -> 12; // 270° = 12 steps
            default -> 0;
        };

        int newRotation = (currentRotation + rotationSteps) & 15; // Keep in range 0-15
        return state.with(ROTATION, newRotation);
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        int rotation = state.get(ROTATION);

        int mirroredRotation = switch (mirror) {
            case FRONT_BACK -> (16 - rotation) & 15;
            case LEFT_RIGHT -> (32 - rotation) & 15;
            default -> rotation;
        };

        return state.with(ROTATION, mirroredRotation);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }
}
