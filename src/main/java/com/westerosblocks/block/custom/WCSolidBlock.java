package com.westerosblocks.block.custom;

import com.westerosblocks.data.BlockDefinition;
import com.westerosblocks.utils.ModProperties;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class WCSolidBlock extends Block implements WCBlockDef {
    protected BlockDefinition def;
    protected final VoxelShape collisionBox;
    protected boolean toggleOnUse = false;

    protected boolean connectState;
    public static final IntProperty CONNECTSTATE = ModProperties.CONNECTSTATE;

    protected ModProperties.StateProperty STATE;

    public boolean symmetrical;
    public static final BooleanProperty SYMMETRICAL = ModProperties.SYMMETRICAL;

    public static class Factory extends BlockFactory {
        @Override
        public Block buildBlockClass(BlockDefinition definition) {
            AbstractBlock.Settings settings = definition.makeSettings();
            ModProperties.StateProperty stateProperty = definition.buildStateProperty();

            boolean doConnectState = definition.isConnectState();
            boolean doToggleOnUse = definition.toggleOnUse();
            boolean doSymmetrical = definition.isSymmetrical();
            boolean doAddStates = (stateProperty != null);

            return StagedProperties.builder()
                    .addIf(doConnectState, CONNECTSTATE)
                    .addIf(doSymmetrical, SYMMETRICAL)
                    .state(stateProperty)
                    .construct(() -> new WCSolidBlock(settings, definition, doConnectState, doToggleOnUse, doAddStates, doSymmetrical));
        }
    }

    public WCSolidBlock(AbstractBlock.Settings settings, BlockDefinition def, boolean connectedState,
            boolean doToggleOnUse, boolean addStates, boolean doSymmetrical) {
        super(settings);
        this.def = def;
        this.collisionBox = def.makeCollisionBoxShape();

        if (doToggleOnUse) {
            toggleOnUse = true;
        }

        BlockState defbs = this.getDefaultState();
        this.connectState = connectedState;
        this.symmetrical = doSymmetrical;
        if (this.connectState) {
            defbs = defbs.with(CONNECTSTATE, 0);
        }
        if (this.symmetrical) {
            defbs = defbs.with(SYMMETRICAL, symmetrical);
        }
        if (addStates && STATE != null) {
            defbs = defbs.with(STATE, STATE.defValue);
        }
        this.setDefaultState(defbs);
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        STATE = StagedProperties.appendTo(builder).state();
        super.appendProperties(builder);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState bs = super.getPlacementState(ctx);
        if (connectState && bs != null && bs.contains(CONNECTSTATE)) {
            bs = bs.with(CONNECTSTATE, 0);
        }
        return bs;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        return toggleOnUse ? CreativeToggle.cycle(state, world, pos, player, STATE) : ActionResult.PASS;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return collisionBox;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return collisionBox;
    }

    // Ports 1.18.2 getBlockSupportShape(): the sturdy-face shape walls/fences/panes
    // test for connection. supportBoxes were unused in the 1.18.2 definitions, so
    // this is always the collision box.
    @Override
    public VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
        return collisionBox;
    }

    // Ports 1.18.2 getVisualShape(): non-opaque solids don't clip the camera.
    @Override
    public VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (def.isNonOpaque()) {
            return VoxelShapes.empty();
        }
        return collisionBox;
    }

    public BlockDefinition getDefinition() {
        return def;
    }

    @Override
    public boolean isSideInvisible(BlockState state, BlockState stateFrom, Direction direction) {
        if (def.isNonOpaque()) {
            return stateFrom.isOf(this) || super.isSideInvisible(state, stateFrom, direction);
        }
        return false;
    }

    @Override
    public VoxelShape getCullingShape(BlockState state, BlockView world, BlockPos pos) {
        if (def.isNonOpaque()) {
            return VoxelShapes.empty();
        }
        return VoxelShapes.fullCube();
    }

    @Override
    public float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return (def.getLightOpacity() != null && def.getLightOpacity() == 0) ? 1.0F : 0.2F;
    }

    @Override
    public boolean isTransparent(BlockState state, BlockView world, BlockPos pos) {
        return def.isNonOpaque();
    }
}
