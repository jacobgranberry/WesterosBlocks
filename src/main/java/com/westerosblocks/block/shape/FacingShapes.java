package com.westerosblocks.block.shape;

import com.westerosblocks.data.BlockDefinition;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A block's outline for every facing, built from geometry authored for one facing.
 *
 * <p>Horizontal facings turn the authored boxes clockwise about the block centre, one quarter
 * turn per step from the authored facing (east to south to west to north), which is how
 * blockstate {@code "y"} rotations turn the models. {@code UP} and {@code DOWN} first turn the
 * geometry to face east, then tip it up or down, matching {@code cuboid-nsewud} models.
 *
 * <p>Every shape is built once, when the block is constructed.
 */
public final class FacingShapes {
    private final Map<Direction, VoxelShape> byFacing;

    private FacingShapes(Map<Direction, VoxelShape> byFacing) {
        this.byFacing = byFacing;
    }

    /** Shapes for {@code boxes} authored facing {@code authored}. */
    public static FacingShapes of(Direction authored, List<Box> boxes) {
        if (!authored.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("Geometry must be authored for a horizontal facing, not " + authored);
        }
        Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.values()) {
            byFacing.put(facing, Box.union(boxes.stream().map(box -> orient(box, authored, facing)).toList()));
        }
        return new FacingShapes(byFacing);
    }

    public static FacingShapes of(Direction authored, Box box) {
        return of(authored, List.of(box));
    }

    /** The same shape for every facing. */
    public static FacingShapes uniform(VoxelShape shape) {
        Map<Direction, VoxelShape> byFacing = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.values()) {
            byFacing.put(facing, shape);
        }
        return new FacingShapes(byFacing);
    }

    /**
     * Shapes for one definition state, authored facing {@code authored}: the state's cuboids,
     * which after loading already fall back to the block's cuboids and to either bounding box.
     * A state with no geometry at all is a full cube.
     */
    public static FacingShapes forState(BlockDefinition.StateVariant state, Direction authored) {
        List<? extends BlockDefinition.CuboidElement> cuboids = state.getCuboids();
        if (cuboids == null || cuboids.isEmpty()) {
            return uniform(VoxelShapes.fullCube());
        }
        return of(authored, Box.of(cuboids));
    }

    /** Shapes for every state of {@code definition}, in state order. */
    public static List<FacingShapes> forStates(BlockDefinition definition, Direction authored) {
        return definition.getStates().stream().map(state -> forState(state, authored)).toList();
    }

    public VoxelShape get(Direction facing) {
        return byFacing.get(facing);
    }

    private static Box orient(Box box, Direction authored, Direction facing) {
        if (facing == Direction.UP) {
            return turn(box, authored, Direction.EAST).tipUp();
        }
        if (facing == Direction.DOWN) {
            return turn(box, authored, Direction.EAST).tipDown();
        }
        return turn(box, authored, facing);
    }

    private static Box turn(Box box, Direction from, Direction to) {
        return box.turnClockwise(to.getHorizontal() - from.getHorizontal());
    }
}
