package com.westerosblocks.datagen.custom;

import com.westerosblocks.data.BlockDefinition;
import net.minecraft.data.client.BlockStateVariant;
import net.minecraft.data.client.VariantSettings;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a state's random texture sets into blockstate variants.
 *
 * <p>Every exporter does the same thing for each block-state it writes: walk the
 * {@code randomTextures} sets of a {@link BlockDefinition.StateVariant}, pick the model built for
 * each set, and emit one variant per set (times any random Y rotations) carrying the set's weight
 * and the block-state's rotation. This class is the one place that does it:
 *
 * <pre>{@code
 * List<Choice> choices = TextureSetVariants.choices(state,
 *         (setIdx, set) -> createNestedModelId(block, getModelName(fname, setIdx)));
 * map.register(axis, TextureSetVariants.variants(choices, Transform.y(90)));
 * }</pre>
 *
 * <p>It is also the one place that decides how a variant is written: a rotation of 0,
 * {@code uvlock: false} and {@code weight: 1} are the game's defaults and are left out.
 */
public final class TextureSetVariants {

    private TextureSetVariants() {}

    /** One random pick for a block-state: a model and the weight of the texture set it came from. */
    public record Choice(Identifier model, int weight) {
        public Choice {
            weight = Math.max(weight, 1);
        }

        public static Choice of(Identifier model) {
            return new Choice(model, 1);
        }
    }

    /** The rotation a block-state applies to its model, in degrees (0, 90, 180 or 270). */
    public record Transform(int x, int y, boolean uvlock) {
        public static final Transform NONE = new Transform(0, 0, false);

        public static Transform y(int y) {
            return new Transform(0, y, false);
        }

        public static Transform of(int x, int y) {
            return new Transform(x, y, false);
        }

        public Transform withUvlock(boolean uvlock) {
            return new Transform(x, y, uvlock);
        }

        /** This transform turned a further {@code degrees} around Y. */
        public Transform plusY(int degrees) {
            return new Transform(x, Math.floorMod(y + degrees, 360), uvlock);
        }

        public boolean isRotated() {
            return x != 0 || y != 0;
        }
    }

    /** Picks the model for one texture set, or returns null to leave the set out. */
    @FunctionalInterface
    public interface ModelForSet {
        /**
         * @param setIdx 0-based texture set index
         * @param set    the texture set; null only for the single pass a custom-model state with
         *               no texture sets gets (see {@link BlockDefinition.StateVariant#forEachTextureSet})
         */
        Identifier model(int setIdx, BlockDefinition.RandomTextureVariant set);
    }

    /** One choice per texture set of {@code state}, weighted by the set, in set order. */
    public static List<Choice> choices(BlockDefinition.StateVariant state, ModelForSet modelForSet) {
        List<Choice> choices = new ArrayList<>();
        state.forEachTextureSet((setIdx, set) -> {
            Identifier model = modelForSet.model(setIdx, set);
            if (model != null) {
                choices.add(new Choice(model, set == null ? 1 : set.getWeight()));
            }
        });
        return choices;
    }

    /** One variant per choice, all turned by {@code transform}. */
    public static List<BlockStateVariant> variants(List<Choice> choices, Transform transform) {
        return variants(choices, transform, 1);
    }

    /**
     * {@code rotations} variants per choice (1 for none, 4 for {@code rotateRandom}), each turned
     * by {@code transform} plus a further 0, 90, 180 or 270 degrees around Y.
     */
    public static List<BlockStateVariant> variants(List<Choice> choices, Transform transform, int rotations) {
        // A weight only means something next to another model; a lone model always wins.
        boolean weighted = choices.size() > 1;
        List<BlockStateVariant> variants = new ArrayList<>(choices.size() * rotations);
        for (Choice choice : choices) {
            int weight = weighted ? choice.weight() : 1;
            for (int rot = 0; rot < rotations; rot++) {
                variants.add(variant(choice.model(), transform.plusY(rot * 90), weight));
            }
        }
        return variants;
    }

    /** Shorthand for {@code variants(choices(state, modelForSet), Transform.NONE, rotations)}. */
    public static List<BlockStateVariant> variants(BlockDefinition.StateVariant state, ModelForSet modelForSet,
                                                   int rotations) {
        return variants(choices(state, modelForSet), Transform.NONE, rotations);
    }

    public static BlockStateVariant variant(Identifier model) {
        return variant(model, Transform.NONE, 1);
    }

    public static BlockStateVariant variant(Identifier model, Transform transform) {
        return variant(model, transform, 1);
    }

    /** The variant for {@code model}, writing only the settings that differ from the defaults. */
    public static BlockStateVariant variant(Identifier model, Transform transform, int weight) {
        BlockStateVariant variant = BlockStateVariant.create().put(VariantSettings.MODEL, model);
        if (transform.x() != 0) {
            variant.put(VariantSettings.X, rotation(transform.x()));
        }
        if (transform.y() != 0) {
            variant.put(VariantSettings.Y, rotation(transform.y()));
        }
        if (transform.uvlock()) {
            variant.put(VariantSettings.UVLOCK, true);
        }
        if (weight > 1) {
            variant.put(VariantSettings.WEIGHT, weight);
        }
        return variant;
    }

    /** Degrees to the rotation setting; 0, 90, 180 and 270 are the only values the game accepts. */
    public static VariantSettings.Rotation rotation(int degrees) {
        return switch (Math.floorMod(degrees, 360)) {
            case 0 -> VariantSettings.Rotation.R0;
            case 90 -> VariantSettings.Rotation.R90;
            case 180 -> VariantSettings.Rotation.R180;
            case 270 -> VariantSettings.Rotation.R270;
            default -> throw new IllegalArgumentException(
                    "Invalid rotation: " + degrees + ". Must be 0, 90, 180, or 270.");
        };
    }
}
