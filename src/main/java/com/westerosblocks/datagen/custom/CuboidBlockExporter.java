package com.westerosblocks.datagen.custom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.block.Block;
import net.minecraft.data.client.*;
import net.minecraft.util.Identifier;
import com.westerosblocks.data.BlockDefinition;

import java.util.*;

public class CuboidBlockExporter extends BaseBlockExporter {

    public static void registerCustomCuboidBlock(BlockStateModelGenerator generator, Block block, BlockDefinition definition) {
        List<BlockDefinition.StateVariant> states = definition.getStates();
        if (states == null || states.isEmpty()) {
            throw new IllegalStateException("Block definition states should never be null/empty after doInit() for block: " + getBlockName(block));
        }

        int rotationCount = definition.hasRotateRandom() ? 4 : 1;
        boolean hasMultipleStates = definition.getStateCount() > 1;

        CuboidBlockExporter exporter = new CuboidBlockExporter();
        Map<String, List<TextureSetVariants.Choice>> stateModelMap = exporter.generateModelsReturnMap(generator, block, definition);

        if (stateModelMap.isEmpty()) {
            Identifier fallback = createNestedModelId(block, getModelName("base", 0));
            stateModelMap.put("base", List.of(TextureSetVariants.Choice.of(fallback)));
        }

        generateBlockState(generator, block, states, stateModelMap, hasMultipleStates, rotationCount);

        registerParentedItemModel(generator, block, stateModelMap.values().iterator().next().get(0).model());
    }

    private static void generateBlockState(BlockStateModelGenerator generator, Block block,
                                          List<BlockDefinition.StateVariant> states,
                                          Map<String, List<TextureSetVariants.Choice>> stateModelMap,
                                          boolean hasMultipleStates, int rotationCount) {
        if (hasMultipleStates && hasStateProperty(block)) {
            BlockStateVariantMap.SingleProperty<String> stateMap =
                BlockStateVariantMap.create(getStateProperty(block));

            for (BlockDefinition.StateVariant state : states) {
                String stateId = getStateIdOrBase(state.getStateID());
                List<TextureSetVariants.Choice> choices = stateModelMap.get(stateId);
                if (choices == null || choices.isEmpty()) continue;
                stateMap.register(stateId, TextureSetVariants.variants(choices, TextureSetVariants.Transform.NONE, rotationCount));
            }

            generator.blockStateCollector.accept(
                VariantsBlockStateSupplier.create(block).coordinate(stateMap));
        } else {
            List<TextureSetVariants.Choice> choices = stateModelMap.values().iterator().next();
            List<BlockStateVariant> variants = TextureSetVariants.variants(choices, TextureSetVariants.Transform.NONE, rotationCount);

            generator.blockStateCollector.accept(
                VariantsBlockStateSupplier.create(block, variants.toArray(new BlockStateVariant[0])));
        }
    }

    /**
     * Returns the cuboids to use for model generation. Subclasses can override
     * to transform cuboids (e.g., rotate for NSEWUD blocks). Returns null to
     * use the default cuboids from the definition.
     */
    protected List<BlockDefinition.CuboidElement> getModelCuboids(BlockDefinition definition) {
        return null;
    }

    /**
     * Generates model files for every state × texture-set combination and returns
     * them keyed by state ID. Single-state blocks use {@code "base"} as the model
     * name prefix regardless of the state's actual ID; multi-state blocks use the
     * state ID. Uses {@link LinkedHashMap} so iteration order matches definition
     * order (needed for deterministic item-model selection).
     */
    protected Map<String, List<TextureSetVariants.Choice>> generateModelsReturnMap(BlockStateModelGenerator generator, Block block, BlockDefinition definition) {
        List<BlockDefinition.StateVariant> states = definition.getStates();
        boolean hasMultipleStates = definition.getStateCount() > 1;

        Map<String, List<TextureSetVariants.Choice>> stateModelMap = new LinkedHashMap<>();

        for (BlockDefinition.StateVariant state : states) {
            String stateId = getStateIdOrBase(state.getStateID());
            String modelNamePrefix = hasMultipleStates ? stateId : "base";

            List<TextureSetVariants.Choice> choices = TextureSetVariants.choices(state, (setIdx, set) -> {
                String variantName = getModelName(modelNamePrefix, setIdx);

                if (state.isCustomModel()) {
                    return createCustomModelId(block, variantName);
                }
                if (set == null || set.getTextureCount() == 0) {
                    return null;
                }

                List<String> textureList = new ArrayList<>(set.getTextures());

                Identifier modelId;
                if (CuboidModelBuilder.hasCuboids(definition) || state.hasCuboids()) {
                    Float rotation = state.getRotYOffset() != null
                            ? state.getRotYOffset().floatValue()
                            : null;
                    modelId = CuboidModelBuilder.createCuboidModel(generator, block, definition,
                            textureList, setIdx, variantName, rotation, getModelCuboids(definition));
                } else {
                    TextureMap textureMap = CuboidModelBuilder.standardCubeTextureMap(textureList);
                    Model parent = textureList.size() == 1 ? Models.CUBE_ALL : Models.CUBE;
                    modelId = parent.upload(createGeneratedModelId(block, variantName),
                            textureMap, generator.modelCollector);
                }
                return modelId;
            });

            if (!choices.isEmpty()) {
                stateModelMap.put(stateId, choices);
            }
        }

        return stateModelMap;
    }

    /**
     * Configuration for a single facing direction's rotation.
     */
    protected record FacingRotation(String facingValue, int yRot, int xRot) {
        FacingRotation(String facingValue, int yRot) {
            this(facingValue, yRot, 0);
        }
    }

    /**
     * Provider that returns facing rotations per state.
     * Allows per-state customization (e.g., different conventions for custom vs generated models).
     */
    @FunctionalInterface
    protected interface FacingRotationProvider {
        FacingRotation[] getRotations(BlockDefinition.StateVariant state);
    }

    /**
     * Shared blockstate generator for all directional cuboid exporters.
     * Builds facing variant JSON from a rotation configuration, handling:
     * - Single and multi-state blocks
     * - Weighted random texture variants
     * - Per-state rotYOffset
     * - X-axis rotation (for UP/DOWN directions)
     */
    protected static BlockStateSupplier generateFacingBlockState(
            Block block,
            Map<String, List<TextureSetVariants.Choice>> stateModelMap,
            List<BlockDefinition.StateVariant> states,
            boolean hasMultipleStates,
            FacingRotationProvider rotationProvider) {
        return new BlockStateSupplier() {
            @Override
            public Block getBlock() {
                return block;
            }

            @Override
            public JsonElement get() {
                JsonObject json = new JsonObject();
                JsonObject variants = new JsonObject();

                for (BlockDefinition.StateVariant state : states) {
                    String stateId = state.getStateID() != null ? state.getStateID() : "base";
                    List<TextureSetVariants.Choice> choices = stateModelMap.get(stateId);
                    if (choices == null || choices.isEmpty()) continue;

                    int rotYOffset = state.getRotYOffset() != null ? state.getRotYOffset() : 0;

                    for (FacingRotation fr : rotationProvider.getRotations(state)) {
                        String key = hasMultipleStates
                                ? "facing=" + fr.facingValue() + ",state=" + stateId
                                : "facing=" + fr.facingValue();
                        TextureSetVariants.Transform transform =
                                TextureSetVariants.Transform.of(fr.xRot(), fr.yRot()).plusY(rotYOffset);
                        variants.add(key, BlockStateVariant.toJson(TextureSetVariants.variants(choices, transform)));
                    }
                }

                json.add("variants", variants);
                return json;
            }
        };
    }
}
