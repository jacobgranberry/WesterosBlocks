package com.westerosblocks.block.shape;

import com.westerosblocks.data.BlockDefinition;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.List;

/**
 * An axis-aligned box in block units (0..1 on each axis), the units block definitions are
 * authored in. Boxes are turned about the block centre to produce the other facings of a
 * block; see {@link FacingShapes}.
 */
public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

    /** A box given in pixels (0..16), the units {@code Block.createCuboidShape} takes. */
    public static Box pixels(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return new Box(minX / 16, minY / 16, minZ / 16, maxX / 16, maxY / 16, maxZ / 16);
    }

    public static Box of(BlockDefinition.BoundingBox box) {
        return new Box(box.getXMin(), box.getYMin(), box.getZMin(), box.getXMax(), box.getYMax(), box.getZMax());
    }

    public static Box of(BlockDefinition.CuboidElement cuboid) {
        return new Box(cuboid.getXMin(), cuboid.getYMin(), cuboid.getZMin(),
                cuboid.getXMax(), cuboid.getYMax(), cuboid.getZMax());
    }

    public static List<Box> of(List<? extends BlockDefinition.CuboidElement> cuboids) {
        return cuboids.stream().map(Box::of).toList();
    }

    /**
     * Turns the box clockwise, seen from above, by {@code quarterTurns} quarter turns about
     * the block centre. One turn takes an east-facing box to south, the same as a blockstate
     * {@code "y": 90} and {@code CuboidRotation.ROTY90}.
     */
    public Box turnClockwise(int quarterTurns) {
        Box box = this;
        for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
            box = new Box(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY, box.maxX);
        }
        return box;
    }

    /** Tips an east-facing box so it faces up ({@code CuboidRotation.ROTZ270}). */
    public Box tipUp() {
        return new Box(1 - maxY, minX, minZ, 1 - minY, maxX, maxZ);
    }

    /** Tips an east-facing box so it faces down ({@code CuboidRotation.ROTZ90}). */
    public Box tipDown() {
        return new Box(minY, 1 - maxX, minZ, maxY, 1 - minX, maxZ);
    }

    /** The same box moved up so its top sits at the top of the block. */
    public Box atTop() {
        return new Box(minX, 1 - (maxY - minY), minZ, maxX, 1, maxZ);
    }

    public VoxelShape toShape() {
        return VoxelShapes.cuboid(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** The union of {@code boxes}, or an empty shape when there are none. */
    public static VoxelShape union(List<Box> boxes) {
        VoxelShape shape = VoxelShapes.empty();
        for (Box box : boxes) {
            shape = VoxelShapes.union(shape, box.toShape());
        }
        return shape;
    }
}
