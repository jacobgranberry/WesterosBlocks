package com.westerosblocks.datagen.custom;

import com.westerosblocks.data.BlockDefinition;
import com.westerosblocks.utils.ModProperties;
import net.minecraft.block.Block;
import net.minecraft.data.client.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cuboid16WayBlockExporter extends CuboidBlockExporter {

    private static final String[] MODEL_SUFFIXES = {"", "_rotn22", "_rotn45", "_rot22"};
    private static final Float[] MODEL_ROTATIONS = {null, -22.5f, -45f, 22.5f};

    public static void registerCustomCuboid16WayBlock(BlockStateModelGenerator generator, Block block, BlockDefinition definition) {
        var states = definition.getStates();

        if (states == null || states.isEmpty()) {
            throw new IllegalStateException("Block definition states should never be null/empty after doInit() for block: " + getBlockName(block));
        }

        boolean hasMultipleStates = definition.getStateCount() > 1;

        if (definition.hasCustomModel() && !hasMultipleStates) {
            registerCustomModelCuboid16WayBlock(generator, block, definition);
            return;
        }

        // Collect model sets: stateId -> rotationSuffix -> List<TextureSetVariants.Choice>
        Map<String, Map<String, List<TextureSetVariants.Choice>>> stateRotationModelSets = new HashMap<>();
        Identifier firstModel = null;

        for (BlockDefinition.StateVariant state : states) {
            String stateId = state.getStateID();
            if (stateId == null) stateId = "base";

            Map<String, List<TextureSetVariants.Choice>> rotationModelSets = new HashMap<>();
            String namePrefix = hasMultipleStates ? stateId : "base";

            for (int rotIdx = 0; rotIdx < MODEL_SUFFIXES.length; rotIdx++) {
                String suffix = MODEL_SUFFIXES[rotIdx];
                Float modelRotation = MODEL_ROTATIONS[rotIdx];
                List<TextureSetVariants.Choice> modelSets = TextureSetVariants.choices(state, (setIdx, set) -> {
                    String variantName = namePrefix + suffix + "_v" + (setIdx + 1);
                    if (state.isCustomModel()) {
                        return createCustomModelId(block, variantName);
                    }
                    if (set == null || set.getTextureCount() == 0) return null;
                    return CuboidModelBuilder.createCuboidModel(generator, block, definition,
                            new ArrayList<>(set.getTextures()), setIdx, variantName, modelRotation);
                });

                if (!modelSets.isEmpty()) {
                    rotationModelSets.put(suffix, modelSets);
                    if (firstModel == null) firstModel = modelSets.get(0).model();
                }
            }

            if (!rotationModelSets.isEmpty()) {
                stateRotationModelSets.put(stateId, rotationModelSets);
            }
        }

        if (stateRotationModelSets.isEmpty()) {
            registerFallbackCuboid16WayBlock(generator, block, definition);
            return;
        }

        ModProperties.StateProperty stateProperty = getStateProperty(block);
        if (hasMultipleStates && stateProperty != null) {
            generateBlockStateWithStates(generator, block, stateRotationModelSets, stateProperty);
        } else {
            Map<String, List<TextureSetVariants.Choice>> rotationModelSets = stateRotationModelSets.values().iterator().next();
            generateBlockState(generator, block, rotationModelSets);
        }

        if (firstModel != null) {
            registerParentedItemModel(generator, block, firstModel);
        }
    }

    private static void registerCustomModelCuboid16WayBlock(BlockStateModelGenerator generator, Block block, BlockDefinition definition) {
        Map<String, List<TextureSetVariants.Choice>> rotationModelSets = new HashMap<>();

        for (int i = 0; i < MODEL_SUFFIXES.length; i++) {
            Identifier modelId = createCustomModelId(block, "base" + MODEL_SUFFIXES[i] + "_v1");
            rotationModelSets.put(MODEL_SUFFIXES[i], List.of(TextureSetVariants.Choice.of(modelId)));
        }

        generateBlockState(generator, block, rotationModelSets);
        registerParentedItemModel(generator, block, rotationModelSets.get("").get(0).model());
    }

    private static void registerFallbackCuboid16WayBlock(BlockStateModelGenerator generator, Block block, BlockDefinition definition) {
        List<String> fallbackTextures = List.of("missing");
        Map<String, List<TextureSetVariants.Choice>> rotationModelSets = new HashMap<>();

        for (int i = 0; i < MODEL_SUFFIXES.length; i++) {
            String modelName = "base" + MODEL_SUFFIXES[i] + "_v1";
            Identifier modelId = CuboidModelBuilder.createCuboidModel(generator, block, definition, fallbackTextures, 0, modelName, MODEL_ROTATIONS[i]);
            rotationModelSets.put(MODEL_SUFFIXES[i], List.of(TextureSetVariants.Choice.of(modelId)));
        }

        generateBlockState(generator, block, rotationModelSets);
        registerParentedItemModel(generator, block, rotationModelSets.get("").get(0).model());
    }

    // Single-state: SingleProperty<Integer> on ROTATION
    private static void generateBlockState(BlockStateModelGenerator generator, Block block,
                                          Map<String, List<TextureSetVariants.Choice>> rotationModelSets) {
        BlockStateVariantMap.SingleProperty<Integer> variantMap =
            BlockStateVariantMap.create(Properties.ROTATION);

        for (int rotation = 0; rotation < 16; rotation++) {
            int modelIndex = rotation % 4;
            List<TextureSetVariants.Choice> modelSets = rotationModelSets.get(MODEL_SUFFIXES[modelIndex]);
            int yRotation = (90 * (((rotation + 1) % 16) / 4)) % 360;

            List<BlockStateVariant> variants = buildRotationVariants(modelSets, yRotation);
            variantMap.register(rotation, variants);
        }

        generator.blockStateCollector.accept(
            VariantsBlockStateSupplier.create(block).coordinate(variantMap)
        );
    }

    // Multi-state: DoubleProperty<Integer, String> on ROTATION + STATE
    private static void generateBlockStateWithStates(BlockStateModelGenerator generator, Block block,
                                                     Map<String, Map<String, List<TextureSetVariants.Choice>>> stateRotationModelSets,
                                                     ModProperties.StateProperty stateProperty) {
        BlockStateVariantMap.DoubleProperty<Integer, String> variantMap =
            BlockStateVariantMap.create(Properties.ROTATION, stateProperty);

        for (Map.Entry<String, Map<String, List<TextureSetVariants.Choice>>> stateEntry : stateRotationModelSets.entrySet()) {
            String stateId = stateEntry.getKey();
            Map<String, List<TextureSetVariants.Choice>> rotationModelSets = stateEntry.getValue();

            for (int rotation = 0; rotation < 16; rotation++) {
                int modelIndex = rotation % 4;
                List<TextureSetVariants.Choice> modelSets = rotationModelSets.get(MODEL_SUFFIXES[modelIndex]);
                int yRotation = (90 * (((rotation + 1) % 16) / 4)) % 360;

                if (modelSets == null || modelSets.isEmpty()) continue;

                List<BlockStateVariant> variants = buildRotationVariants(modelSets, yRotation);
                variantMap.register(rotation, stateId, variants);
            }
        }

        generator.blockStateCollector.accept(
            VariantsBlockStateSupplier.create(block).coordinate(variantMap)
        );
    }

    private static List<BlockStateVariant> buildRotationVariants(List<TextureSetVariants.Choice> modelSets, int yRotation) {
        return TextureSetVariants.variants(modelSets, TextureSetVariants.Transform.y(yRotation));
    }
}
