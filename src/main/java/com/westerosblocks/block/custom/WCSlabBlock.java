package com.westerosblocks.block.custom;

import com.westerosblocks.data.BlockDefinition;
import com.westerosblocks.utils.ModProperties;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WCSlabBlock extends SlabBlock implements WCBlockDef {
    protected BlockDefinition def;
    protected boolean toggleOnUse = false;

    protected boolean connectState;
    public static final IntProperty CONNECTSTATE = ModProperties.CONNECTSTATE;

    protected ModProperties.StateProperty STATE;

    public static class Factory extends BlockFactory {
        @Override
        public Block buildBlockClass(BlockDefinition definition) {
            AbstractBlock.Settings settings = definition.makeSettings();

            ModProperties.StateProperty stateProperty = definition.buildStateProperty();
            boolean doConnectState = definition.isConnectState();
            boolean doToggleOnUse = definition.toggleOnUse();
            boolean doAddStates = (stateProperty != null);

            return StagedProperties.builder()
                    .addIf(doConnectState, CONNECTSTATE)
                    .state(stateProperty)
                    .construct(() -> new WCSlabBlock(settings, definition, doConnectState, doToggleOnUse, doAddStates));
        }
    }

    public WCSlabBlock(Settings settings, BlockDefinition def, boolean doConnectState, boolean doToggleOnUse, boolean doAddStates) {
        super(settings);
        this.def = def;

        if (doToggleOnUse) {
            toggleOnUse = true;
        }

        BlockState defbs = this.getDefaultState();
        this.connectState = doConnectState;
        if (this.connectState) {
            defbs = defbs.with(CONNECTSTATE, 0);
        }
        if (doAddStates && STATE != null) {
            defbs = defbs.with(STATE, STATE.defValue);
        }
        this.setDefaultState(defbs);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        return toggleOnUse ? CreativeToggle.cycle(state, world, pos, player, STATE) : ActionResult.PASS;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        STATE = StagedProperties.appendTo(builder).state();
    }

    public BlockDefinition getDefinition() {
        return def;
    }
}
