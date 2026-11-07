package com.westerosblocks.block.custom;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The "toggleOnUse" behaviour shared by WesterosBlocks blocks: a creative-mode player with an
 * empty main hand right-clicks a block to step one of its properties to the next value.
 */
public final class CreativeToggle {
    /** World event 1006: the click sound vanilla plays for toggling doors and similar blocks. */
    private static final int TOGGLE_SOUND_EVENT = 1006;

    private CreativeToggle() {}

    /** True when {@code player} may toggle a block: creative mode, empty main hand. */
    public static boolean canToggle(PlayerEntity player) {
        return player.isCreative() && player.getMainHandStack().isEmpty();
    }

    /**
     * Cycles {@code property} to its next value when the block has it and the player may toggle.
     * Returns {@link ActionResult#PASS} when nothing changed, so callers can fall through.
     */
    public static ActionResult cycle(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                     Property<?> property) {
        if (property == null || !state.contains(property) || !canToggle(player)) {
            return ActionResult.PASS;
        }
        return apply(state.cycle(property), world, pos, player);
    }

    /** Writes {@code newState}, plays the toggle sound, and returns the usual success result. */
    public static ActionResult apply(BlockState newState, World world, BlockPos pos, PlayerEntity player) {
        world.setBlockState(pos, newState, Block.NOTIFY_ALL);
        world.syncWorldEvent(player, TOGGLE_SOUND_EVENT, pos, 0);
        return ActionResult.success(world.isClient);
    }
}
