package com.westerosblocks.datagen.custom;

import net.minecraft.block.Block;
import net.minecraft.data.client.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import com.westerosblocks.datagen.ModModels;
import com.westerosblocks.datagen.ModTextureKey;
import com.westerosblocks.block.custom.WCPaneBlock;
import com.westerosblocks.data.BlockDefinition;

import java.util.ArrayList;
import java.util.List;

public class PaneBlockExporter extends BaseBlockExporter {

    private static Model getPaneSideModel(boolean isBars) {
        return isBars ? ModModels.PANE_SIDE_BARS : ModModels.PANE_SIDE;
    }

    public static void registerPaneBlockWithVariants(BlockStateModelGenerator generator, Block block,
                                                     List<String[]> textureVariants, List<Integer> weights) {
        WCPaneBlock paneBlock = (WCPaneBlock) block;
        boolean isBars = paneBlock.isBarsModel();

        List<Identifier> postModelIds = new ArrayList<>();
        List<Identifier> sideModelIds = new ArrayList<>();
        List<Identifier> nosideModelIds = new ArrayList<>();

        // Generate models for each texture variant
        for (int i = 0; i < textureVariants.size(); i++) {
            String[] textures = textureVariants.get(i);
            String sideTexture = textures[0];
            String capTexture = textures.length > 1 ? textures[1] : textures[0];

            TextureMap paneTextureMap = new TextureMap()
                    .put(TextureKey.SIDE, createBlockIdentifier(sideTexture))
                    .put(ModTextureKey.CAP, createBlockIdentifier(capTexture))
                    .put(TextureKey.PARTICLE, createBlockIdentifier(sideTexture));

            // Upload models with _v1, _v2, etc. suffix
            String suffix = "_v" + (i + 1);

            if (!isBars) {
                Identifier postModelId = ModModels.PANE_POST.upload(
                        createNestedModelId(block, "post" + suffix), paneTextureMap, generator.modelCollector);
                postModelIds.add(postModelId);

                Identifier nosideModelId = ModModels.PANE_NOSIDE.upload(
                        createNestedModelId(block, "noside" + suffix), paneTextureMap, generator.modelCollector);
                nosideModelIds.add(nosideModelId);
            }

            Identifier sideModelId = getPaneSideModel(isBars).upload(
                    createNestedModelId(block, "side" + suffix), paneTextureMap, generator.modelCollector);
            sideModelIds.add(sideModelId);
        }

        // Create multipart blockstate
        MultipartBlockStateSupplier supplier = createPaneBlockState(block, paneBlock,
                postModelIds, sideModelIds, nosideModelIds, weights);
        generator.blockStateCollector.accept(supplier);

        // Register item model (using first texture variant)
        String[] firstTextures = textureVariants.get(0);
        registerSimpleItemModel(generator, block, createBlockIdentifier(firstTextures[0]));
    }

    private static MultipartBlockStateSupplier createPaneBlockState(Block block, WCPaneBlock paneBlock,
            List<Identifier> postModelIds, List<Identifier> sideModelIds,
            List<Identifier> nosideModelIds, List<Integer> weights) {

        MultipartBlockStateSupplier supplier = MultipartBlockStateSupplier.create(block);
        boolean isBars = paneBlock.isBarsModel();
        boolean isLegacy = paneBlock.isLegacyModel();

        // One entry per part, with the texture sets as its weighted random choices
        List<TextureSetVariants.Choice> posts = choices(postModelIds, weights);
        List<TextureSetVariants.Choice> sides = choices(sideModelIds, weights);
        List<TextureSetVariants.Choice> nosides = choices(nosideModelIds, weights);
        Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

        // Post model (always present for non-bars models)
        if (!isBars) {
            supplier = supplier.with(TextureSetVariants.variants(posts, TextureSetVariants.Transform.NONE));
        }

        // Add side connections for all 4 directions with proper rotation and uvlock
        for (Direction direction : directions) {
            addPaneSideVariant(supplier, sides, direction, isBars, isLegacy);
        }

        // No-side connections for non-bars models (all 4 directions)
        if (!isBars) {
            for (Direction direction : directions) {
                addPaneNosideVariant(supplier, nosides, direction);
            }
        }

        return supplier;
    }

    /**
     * Adds a pane side variant for a specific direction.
     * For bars and legacy models, uses OR logic: direction=true OR all_directions=false
     * (matches the 1.18.2 PaneBlockModelExport, which applied this for is_legacy || is_bars).
     */
    private static void addPaneSideVariant(MultipartBlockStateSupplier supplier, List<TextureSetVariants.Choice> sides,
                                           Direction direction, boolean isBars, boolean isLegacy) {
        List<BlockStateVariant> sideVariants = TextureSetVariants.variants(sides, sidesTransform(direction));

        // Create condition
        When condition;
        if (isBars || isLegacy) {
            // For bars and legacy: direction=true OR all_directions=false
            When directionTrue = switch (direction) {
                case NORTH -> When.create().set(Properties.NORTH, true);
                case EAST -> When.create().set(Properties.EAST, true);
                case SOUTH -> When.create().set(Properties.SOUTH, true);
                case WEST -> When.create().set(Properties.WEST, true);
                default -> throw new IllegalArgumentException("Invalid direction: " + direction);
            };

            When allFalse = When.create()
                    .set(Properties.NORTH, false)
                    .set(Properties.EAST, false)
                    .set(Properties.SOUTH, false)
                    .set(Properties.WEST, false);

            condition = When.anyOf(directionTrue, allFalse);
        } else {
            // For regular panes: just direction=true
            condition = switch (direction) {
                case NORTH -> When.create().set(Properties.NORTH, true);
                case EAST -> When.create().set(Properties.EAST, true);
                case SOUTH -> When.create().set(Properties.SOUTH, true);
                case WEST -> When.create().set(Properties.WEST, true);
                default -> throw new IllegalArgumentException("Invalid direction: " + direction);
            };
        }

        supplier.with(condition, sideVariants);
    }

    /**
     * Adds a pane noside variant for a specific direction (non-bars models only).
     */
    private static void addPaneNosideVariant(MultipartBlockStateSupplier supplier, List<TextureSetVariants.Choice> nosides,
                                             Direction direction) {
        List<BlockStateVariant> nosideVariants = TextureSetVariants.variants(nosides, sidesTransform(direction));

        // Condition: direction=false
        When condition = switch (direction) {
            case NORTH -> When.create().set(Properties.NORTH, false);
            case EAST -> When.create().set(Properties.EAST, false);
            case SOUTH -> When.create().set(Properties.SOUTH, false);
            case WEST -> When.create().set(Properties.WEST, false);
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        };

        supplier.with(condition, nosideVariants);
    }

    /** Side and no-side parts are modelled facing north, turned to {@code direction}, with uvlock. */
    private static TextureSetVariants.Transform sidesTransform(Direction direction) {
        return TextureSetVariants.Transform.y(getRotationForDirection(direction)).withUvlock(true);
    }

    private static List<TextureSetVariants.Choice> choices(List<Identifier> modelIds, List<Integer> weights) {
        List<TextureSetVariants.Choice> choices = new ArrayList<>();
        for (int i = 0; i < modelIds.size(); i++) {
            choices.add(new TextureSetVariants.Choice(modelIds.get(i), weights.get(i)));
        }
        return choices;
    }

    public static void registerCustomPaneBlock(BlockStateModelGenerator generator, Block block, BlockDefinition definition) {
        // After doInit(), states is ALWAYS non-empty (at least synthetic base state exists)
        var states = definition.getStates();

        if (states == null || states.isEmpty()) {
            throw new IllegalStateException("Block definition states should never be null/empty after doInit() for block: " + getBlockName(block));
        }

        // Extract all random texture variants from the first state
        BlockDefinition.StateVariant state = states.get(0);
        List<String[]> textureVariants = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();

        int textureSetCount = state.getRandomTextureSetCount();
        if (textureSetCount > 0) {
            // Multiple texture variants (random textures)
            for (int i = 0; i < textureSetCount; i++) {
                BlockDefinition.RandomTextureVariant set = state.getRandomTextureSet(i);
                if (set != null && set.getTextureCount() > 0) {
                    // Pane blocks need side texture and cap texture
                    // texture[0] = side, texture[1] = cap (if present, otherwise use side for both)
                    String[] textures = new String[2];
                    textures[0] = set.getTextureByIndex(0);  // side texture
                    textures[1] = set.getTextureCount() > 1 ? set.getTextureByIndex(1) : textures[0];  // cap texture

                    textureVariants.add(textures);
                    weights.add(set.getWeight());
                } else {
                    // Fallback for empty texture set
                    textureVariants.add(new String[]{"missingno", "missingno"});
                    weights.add(1);
                }
            }
        } else {
            // No random textures, use single texture
            textureVariants.add(new String[]{"missingno", "missingno"});
            weights.add(1);
        }

        registerPaneBlockWithVariants(generator, block, textureVariants, weights);
    }
}