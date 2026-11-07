package com.westerosblocks.block.custom;

import com.westerosblocks.data.BlockDefinition;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.WoodType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import com.westerosblocks.utils.ModWoodType;

public class WCFenceGateBlock extends FenceGateBlock implements WCBlockDef {
    protected BlockDefinition def;
    private final boolean locked;

    public static class Factory extends BlockFactory {
        @Override
        public Block buildBlockClass(BlockDefinition definition) {
            AbstractBlock.Settings settings = definition.makeSettings();
            String woodTypeString = definition.getWoodType() != null ? definition.getWoodType() : "oak";
            WoodType woodType = ModWoodType.getWoodType(woodTypeString);
            boolean locked = definition.isLocked();

            return new WCFenceGateBlock(woodType, settings, definition, locked);
        }
    }

    public WCFenceGateBlock(WoodType type, Settings settings, BlockDefinition def, boolean locked) {
        super(type, settings);
        this.def = def;
        this.locked = locked;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        // Locked gates only open for a creative player with an empty hand.
        if (this.locked && !CreativeToggle.canToggle(player)) {
            return ActionResult.PASS;
        }
        return super.onUse(state, world, pos, player, hit);
    }

    public BlockDefinition getDefinition() {
        return def;
    }
}
