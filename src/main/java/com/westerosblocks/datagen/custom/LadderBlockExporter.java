package com.westerosblocks.datagen.custom;

import com.westerosblocks.WesterosBlocks;
import com.westerosblocks.data.BlockDefinition;
import net.minecraft.data.client.*;
import net.minecraft.block.Block;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Exporter for ladder blocks following block-models.md patterns.
 * Generates models for directional ladder blocks with optional random textures.
 */
public class LadderBlockExporter extends BaseBlockExporter {

    private static Model createLadderModel(boolean tinted) {
        return createTintedModel(tinted, "ladder", TextureKey.TEXTURE, TextureKey.PARTICLE);
    }


    private static TextureMap createLadderTextureMap(String texture) {
        return new TextureMap()
                .put(TextureKey.TEXTURE, createBlockIdentifier(texture))
                .put(TextureKey.PARTICLE, createBlockIdentifier(texture));
    }

    private static VariantsBlockStateSupplier createLadderBlockstate(Block block, List<TextureSetVariants.Choice> models) {
        BlockStateVariantMap.SingleProperty<Direction> variantMap = BlockStateVariantMap.create(Properties.HORIZONTAL_FACING);

        for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            variantMap.register(direction, TextureSetVariants.variants(models,
                    TextureSetVariants.Transform.y(getRotationForDirection(direction))));
        }

        return VariantsBlockStateSupplier.create(block).coordinate(variantMap);
    }

    // ========================================
    // Public Registration Methods (block-models.md 5.5)
    // ========================================

    public static void registerLadderBlock(BlockStateModelGenerator generator, Block block, boolean tinted, String texture) {
        TextureMap textureMap = createLadderTextureMap(texture);
        Identifier modelId = uploadModel(createLadderModel(tinted), block, "base", textureMap, generator.modelCollector);

        VariantsBlockStateSupplier blockstate = createLadderBlockstate(block, List.of(TextureSetVariants.Choice.of(modelId)));
        generator.blockStateCollector.accept(blockstate);
        registerParentedItemModel(generator, block, modelId);
    }

    public static void registerLadderBlockWithRandomTextures(BlockStateModelGenerator generator, Block block,
                                                             boolean tinted, List<BlockDefinition.TextureVariantSet> textureSets) {
        List<TextureSetVariants.Choice> models = new ArrayList<>();

        for (int i = 0; i < textureSets.size(); i++) {
            BlockDefinition.TextureVariantSet set = textureSets.get(i);
            TextureMap textureMap = createLadderTextureMap(set.getFirstTexture());
            Identifier modelId = uploadModel(createLadderModel(tinted), block, "base_v" + (i + 1),
                                            textureMap, generator.modelCollector);
            models.add(new TextureSetVariants.Choice(modelId, set.weight));
        }

        VariantsBlockStateSupplier blockstate = createLadderBlockstate(block, models);
        generator.blockStateCollector.accept(blockstate);
        registerParentedItemModel(generator, block, models.get(0).model());
    }

    /**
     * Registers a ladder block using custom model references (no model generation).
     * For ladders with pre-existing custom models.
     */
    public static void registerLadderBlockCustomModel(BlockStateModelGenerator generator, Block block, String blockName, int variantCount) {
        List<TextureSetVariants.Choice> models = new ArrayList<>();
        for (int i = 0; i < variantCount; i++) {
            models.add(TextureSetVariants.Choice.of(WesterosBlocks.id("block/custom/" + blockName + "/base_v" + (i + 1))));
        }

        VariantsBlockStateSupplier blockstate = createLadderBlockstate(block, models);
        generator.blockStateCollector.accept(blockstate);
        generator.registerParentedItemModel(block, models.get(0).model());
    }

    // ========================================
    // BlockDefinition Integration (block-models.md 5.6)
    // ========================================

    public static void registerCustomLadderBlock(BlockStateModelGenerator generator, Block block, BlockDefinition definition) {
        boolean tinted = definition.isTinted() || definition.hasColorMult();
        boolean isCustomModel = definition.hasCustomModel();

        if (definition.hasRandomTextures()) {
            // If custom model, just reference existing models
            if (isCustomModel) {
                registerLadderBlockCustomModel(generator, block, definition.getBlockName(), definition.getRandomTextures().size());
                return;
            }

            registerLadderBlockWithRandomTextures(generator, block, tinted, extractTextureVariantSets(definition));
        } else if (definition.getTextures() != null && !definition.getTextures().isEmpty()) {
            // Single texture case
            if (isCustomModel) {
                registerLadderBlockCustomModel(generator, block, definition.getBlockName(), 1);
            } else {
                registerLadderBlock(generator, block, tinted, definition.getTextures().get(0));
            }
        } else {
            // No textures
            if (isCustomModel) {
                registerLadderBlockCustomModel(generator, block, definition.getBlockName(), 1);
            } else {
                registerLadderBlock(generator, block, tinted, "missingno");
            }
        }
    }

}
