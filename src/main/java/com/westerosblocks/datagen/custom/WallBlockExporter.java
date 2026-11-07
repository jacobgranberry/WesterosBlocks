package com.westerosblocks.datagen.custom;

import com.westerosblocks.data.BlockDefinition;
import com.westerosblocks.datagen.ModModels;
import com.westerosblocks.utils.ModProperties;
import net.minecraft.block.enums.WallShape;
import net.minecraft.data.client.*;
import net.minecraft.block.Block;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WallBlockExporter extends BaseBlockExporter {

    private static Model getWallPostModel(boolean tinted, boolean overlay, boolean isHedge) {
        if (isHedge) return ModModels.HEDGE_POST_TINTED;
        if (overlay) return tinted ? ModModels.WALL_POST_OVERLAY_TINTED : ModModels.WALL_POST_OVERLAY_UNTINTED;
        return tinted ? ModModels.WALL_POST_TINTED : ModModels.WALL_POST_UNTINTED;
    }

    private static Model getWallSideModel(boolean tinted, boolean overlay, boolean isShort, boolean isHedge) {
        if (isHedge) return ModModels.HEDGE_SIDE_TINTED;
        if (isShort) {
            if (overlay) return tinted ? ModModels.WALL_SIDE_SHORT_OVERLAY_TINTED : ModModels.WALL_SIDE_SHORT_OVERLAY_UNTINTED;
            return tinted ? ModModels.WALL_SIDE_SHORT_TINTED : ModModels.WALL_SIDE_SHORT_UNTINTED;
        }
        if (overlay) return tinted ? ModModels.WALL_SIDE_OVERLAY_TINTED : ModModels.WALL_SIDE_OVERLAY_UNTINTED;
        return tinted ? ModModels.WALL_SIDE_TINTED : ModModels.WALL_SIDE_UNTINTED;
    }

    private static Model getWallSideTallModel(boolean tinted, boolean overlay, boolean isHedge) {
        if (isHedge) return ModModels.HEDGE_SIDE_TALL_TINTED;
        if (overlay) return tinted ? ModModels.WALL_SIDE_TALL_OVERLAY_TINTED : ModModels.WALL_SIDE_TALL_OVERLAY_UNTINTED;
        return tinted ? ModModels.WALL_SIDE_TALL_TINTED : ModModels.WALL_SIDE_TALL_UNTINTED;
    }

    private static Model getWallInventoryModel(boolean tinted, boolean isHedge) {
        if (isHedge) return ModModels.HEDGE_INVENTORY_TINTED;
        return tinted ? ModModels.WALL_INVENTORY_TINTED : ModModels.WALL_INVENTORY_UNTINTED;
    }

    private record WallModelSet(List<Identifier> postModelIds, List<Identifier> sideModelIds,
                                List<Identifier> tallModelIds, List<Integer> weights) {}

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Creates multipart blockstate supplier for wall blocks without states.
     */
    private static MultipartBlockStateSupplier createWallVariants(Block block, List<Identifier> postModelIds,
                                                                    List<Identifier> sideModelIds, List<Identifier> tallModelIds,
                                                                    List<Integer> weights) {
        MultipartBlockStateSupplier supplier = MultipartBlockStateSupplier.create(block);
        addWallEntries(supplier, postModelIds, sideModelIds, tallModelIds, weights, null, null);
        return supplier;
    }

    /**
     * Creates multipart blockstate supplier for wall blocks with state property.
     */
    private static MultipartBlockStateSupplier createWallVariantsWithStates(Block block,
            Map<String, WallModelSet> stateModels, ModProperties.StateProperty stateProperty) {
        MultipartBlockStateSupplier supplier = MultipartBlockStateSupplier.create(block);

        for (Map.Entry<String, WallModelSet> entry : stateModels.entrySet()) {
            String stateId = entry.getKey();
            WallModelSet models = entry.getValue();
            addWallEntries(supplier, models.postModelIds, models.sideModelIds, models.tallModelIds,
                    models.weights, stateProperty, stateId);
        }

        return supplier;
    }

    /**
     * Adds wall post and side entries to the multipart supplier, one entry per part with the
     * texture sets as its weighted random choices.
     * If stateProperty and stateId are non-null, adds state condition to when clauses.
     */
    private static void addWallEntries(MultipartBlockStateSupplier supplier, List<Identifier> postModelIds,
            List<Identifier> sideModelIds, List<Identifier> tallModelIds, List<Integer> weights,
            ModProperties.StateProperty stateProperty, String stateId) {
        boolean hasState = stateProperty != null && stateId != null;

        When.PropertyCondition postCondition = When.create().set(Properties.UP, true);
        if (hasState) {
            postCondition.set(stateProperty, stateId);
        }
        supplier.with(postCondition, TextureSetVariants.variants(
                choices(postModelIds, weights), TextureSetVariants.Transform.NONE));

        List<TextureSetVariants.Choice> lowSides = choices(sideModelIds, weights);
        List<TextureSetVariants.Choice> tallSides = choices(tallModelIds, weights);
        for (Direction direction : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            TextureSetVariants.Transform transform =
                    TextureSetVariants.Transform.y(getRotationForDirection(direction)).withUvlock(true);
            addSideEntry(supplier, direction, WallShape.LOW, lowSides, transform, hasState ? stateProperty : null, stateId);
            addSideEntry(supplier, direction, WallShape.TALL, tallSides, transform, hasState ? stateProperty : null, stateId);
        }
    }

    private static void addSideEntry(MultipartBlockStateSupplier supplier, Direction direction, WallShape shape,
                                     List<TextureSetVariants.Choice> sides, TextureSetVariants.Transform transform,
                                     ModProperties.StateProperty stateProperty, String stateId) {
        When.PropertyCondition condition = When.create().set(switch (direction) {
            case EAST -> Properties.EAST_WALL_SHAPE;
            case SOUTH -> Properties.SOUTH_WALL_SHAPE;
            case WEST -> Properties.WEST_WALL_SHAPE;
            default -> Properties.NORTH_WALL_SHAPE;
        }, shape);
        if (stateProperty != null) {
            condition.set(stateProperty, stateId);
        }
        supplier.with(condition, TextureSetVariants.variants(sides, transform));
    }

    private static List<TextureSetVariants.Choice> choices(List<Identifier> modelIds, List<Integer> weights) {
        List<TextureSetVariants.Choice> choices = new ArrayList<>();
        for (int i = 0; i < modelIds.size(); i++) {
            choices.add(new TextureSetVariants.Choice(modelIds.get(i), weights.get(i)));
        }
        return choices;
    }

    /**
     * Uploads wall post, side, and tall models for a given set of random textures.
     */
    private static WallModelSet uploadWallModels(BlockStateModelGenerator generator, Block block,
            boolean tinted, boolean overlay, boolean isShort, boolean isHedge, String modelPrefix,
            List<BlockDefinition.RandomTextureVariant> randomSets, List<String> overlayTextures) {
        List<Identifier> postModelIds = new ArrayList<>();
        List<Identifier> sideModelIds = new ArrayList<>();
        List<Identifier> tallModelIds = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();

        for (int i = 0; i < randomSets.size(); i++) {
            BlockDefinition.RandomTextureVariant set = randomSets.get(i);
            String[] textures = new String[set.getTextureCount()];
            for (int t = 0; t < set.getTextureCount(); t++) {
                textures[t] = set.getTextureByIndex(t);
            }
            String[] expandedTextures = fillTextureArray(textures, 3);

            String[] expandedOverlays = null;
            if (overlay && overlayTextures != null && !overlayTextures.isEmpty()) {
                expandedOverlays = fillTextureArray(overlayTextures.toArray(new String[0]), 3);
            }

            TextureMap textureMap = createFenceWallTextureMap(expandedTextures, expandedOverlays);

            String postName = modelPrefix + "post_v" + (i + 1);
            String sideName = modelPrefix + "side_v" + (i + 1);
            String tallName = modelPrefix + "side_tall_v" + (i + 1);

            Identifier postModelId = getWallPostModel(tinted, overlay, isHedge)
                    .upload(createNestedModelId(block, postName), textureMap, generator.modelCollector);
            Identifier sideModelId = getWallSideModel(tinted, overlay, isShort, isHedge)
                    .upload(createNestedModelId(block, sideName), textureMap, generator.modelCollector);
            Identifier tallModelId = getWallSideTallModel(tinted, overlay, isHedge)
                    .upload(createNestedModelId(block, tallName), textureMap, generator.modelCollector);

            postModelIds.add(postModelId);
            sideModelIds.add(sideModelId);
            tallModelIds.add(tallModelId);
            weights.add(set.getWeight());
        }

        return new WallModelSet(postModelIds, sideModelIds, tallModelIds, weights);
    }

    // ========================================
    // Public Registration Methods
    // ========================================

    public static void registerCustomWallBlock(BlockStateModelGenerator generator, Block block, BlockDefinition definition) {
        boolean tinted = definition.isTinted() || definition.hasColorMult();
        boolean isShort = "short".equals(definition.getWallSize());
        boolean isHedge = "hedge".equals(definition.getWallModel());

        var states = definition.getStates();
        ModProperties.StateProperty stateProperty = getStateProperty(block);
        boolean hasMultipleStates = stateProperty != null && states != null && states.size() > 1;

        if (hasMultipleStates) {
            // Multi-state wall: generate per-state models and state-aware blockstate
            Map<String, WallModelSet> stateModels = new LinkedHashMap<>();
            String firstSideTexture = null;

            for (BlockDefinition.StateVariant state : states) {
                String stateId = getStateIdOrBase(state.getStateID());
                boolean stateOverlay = state.hasOverlayTextures();
                String modelPrefix = stateId + "/";

                // After doInit(), randomTextures is always populated
                List<BlockDefinition.RandomTextureVariant> randomSets = new ArrayList<>();
                for (int i = 0; i < state.getRandomTextureSetCount(); i++) {
                    randomSets.add(state.getRandomTextureSet(i));
                }

                if (randomSets.isEmpty()) continue;

                WallModelSet modelSet = uploadWallModels(generator, block, tinted, stateOverlay, isShort, isHedge,
                        modelPrefix, randomSets, state.getOverlayTextures());
                stateModels.put(state.getStateID(), modelSet);

                // Capture first state's side texture for item model
                if (firstSideTexture == null) {
                    BlockDefinition.RandomTextureVariant firstSet = randomSets.get(0);
                    String[] textures = new String[firstSet.getTextureCount()];
                    for (int t = 0; t < firstSet.getTextureCount(); t++) {
                        textures[t] = firstSet.getTextureByIndex(t);
                    }
                    String[] expanded = fillTextureArray(textures, 3);
                    firstSideTexture = expanded[2]; // side texture
                }
            }

            if (!stateModels.isEmpty()) {
                MultipartBlockStateSupplier blockstate = createWallVariantsWithStates(block, stateModels, stateProperty);
                generator.blockStateCollector.accept(blockstate);

                // Item model from first state's side texture
                String[] expandedItemTextures = fillTextureArray(new String[]{firstSideTexture}, 3);
                TextureMap itemTextureMap = createFenceWallTextureMap(expandedItemTextures, null);
                Identifier itemModelId = ModelIds.getItemModelId(block.asItem());
                getWallInventoryModel(tinted, isHedge).upload(itemModelId, itemTextureMap, generator.modelCollector);
            }
        } else {
            // Single-state wall: existing behavior
            boolean overlay = definition.hasOverlayTextures();
            if (definition.hasRandomTextures()) {
                registerWallBlockWithRandomTextures(generator, block, tinted, overlay, isShort, isHedge, extractTextureVariantSets(definition));
            } else {
                List<String> textureList = definition.getTextures();
                if (textureList != null && !textureList.isEmpty()) {
                    String[] textures = textureList.toArray(new String[0]);
                    String[] overlays = overlay && definition.getOverlayTextures() != null
                            ? definition.getOverlayTextures().toArray(new String[0])
                            : null;
                    registerWallBlock(generator, block, tinted, overlay, isShort, isHedge, textures, overlays);
                } else {
                    registerWallBlock(generator, block, tinted, overlay, isShort, isHedge, new String[]{"missingno"}, null);
                }
            }
        }
    }

    private static void registerWallBlock(BlockStateModelGenerator generator, Block block, boolean tinted,
                                        boolean overlay, boolean isShort, boolean isHedge,
                                        String[] textures, String[] overlayTextures) {
        String[] expandedTextures = fillTextureArray(textures, 3);
        String[] expandedOverlays = overlay && overlayTextures != null ? fillTextureArray(overlayTextures, 3) : null;

        TextureMap textureMap = createFenceWallTextureMap(expandedTextures, expandedOverlays);

        Identifier postModelId = getWallPostModel(tinted, overlay, isHedge)
                .upload(createNestedModelId(block, "post"), textureMap, generator.modelCollector);
        Identifier sideModelId = getWallSideModel(tinted, overlay, isShort, isHedge)
                .upload(createNestedModelId(block, "side"), textureMap, generator.modelCollector);
        Identifier tallModelId = getWallSideTallModel(tinted, overlay, isHedge)
                .upload(createNestedModelId(block, "side_tall"), textureMap, generator.modelCollector);

        MultipartBlockStateSupplier blockstate = createWallVariants(block,
                List.of(postModelId), List.of(sideModelId), List.of(tallModelId), List.of(1));
        generator.blockStateCollector.accept(blockstate);

        Identifier itemModelId = ModelIds.getItemModelId(block.asItem());
        getWallInventoryModel(tinted, isHedge).upload(itemModelId, textureMap, generator.modelCollector);
    }

    private static void registerWallBlockWithRandomTextures(BlockStateModelGenerator generator, Block block, boolean tinted,
                                                           boolean overlay, boolean isShort, boolean isHedge,
                                                           List<BlockDefinition.TextureVariantSet> textureSets) {
        List<Identifier> postModelIds = new ArrayList<>();
        List<Identifier> sideModelIds = new ArrayList<>();
        List<Identifier> tallModelIds = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();

        for (int i = 0; i < textureSets.size(); i++) {
            BlockDefinition.TextureVariantSet set = textureSets.get(i);
            String[] expandedTextures = fillTextureArray(set.getTexturesAsArray(), 3);
            String[] expandedOverlays = overlay && set.hasOverlay() ? fillTextureArray(set.getOverlayTexturesAsArray(), 3) : null;

            TextureMap textureMap = createFenceWallTextureMap(expandedTextures, expandedOverlays);

            Identifier postModelId = getWallPostModel(tinted, overlay, isHedge)
                    .upload(createNestedModelId(block, "post_v" + (i + 1)), textureMap, generator.modelCollector);
            Identifier sideModelId = getWallSideModel(tinted, overlay, isShort, isHedge)
                    .upload(createNestedModelId(block, "side_v" + (i + 1)), textureMap, generator.modelCollector);
            Identifier tallModelId = getWallSideTallModel(tinted, overlay, isHedge)
                    .upload(createNestedModelId(block, "side_tall_v" + (i + 1)), textureMap, generator.modelCollector);

            postModelIds.add(postModelId);
            sideModelIds.add(sideModelId);
            tallModelIds.add(tallModelId);
            weights.add(set.weight);
        }

        MultipartBlockStateSupplier blockstate = createWallVariants(block, postModelIds, sideModelIds, tallModelIds, weights);
        generator.blockStateCollector.accept(blockstate);

        BlockDefinition.TextureVariantSet firstSet = textureSets.get(0);
        String[] expandedTextures = fillTextureArray(firstSet.getTexturesAsArray(), 3);
        String[] expandedOverlays = overlay && firstSet.hasOverlay() ? fillTextureArray(firstSet.getOverlayTexturesAsArray(), 3) : null;

        TextureMap itemTextureMap = createFenceWallTextureMap(expandedTextures, expandedOverlays);
        Identifier itemModelId = ModelIds.getItemModelId(block.asItem());
        getWallInventoryModel(tinted, isHedge).upload(itemModelId, itemTextureMap, generator.modelCollector);
    }
}
