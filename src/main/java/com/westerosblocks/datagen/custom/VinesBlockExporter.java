package com.westerosblocks.datagen.custom;

import com.westerosblocks.block.custom.WCVinesBlock;
import com.westerosblocks.data.BlockDefinition;
import com.westerosblocks.datagen.ModModels;
import com.westerosblocks.datagen.ModTextureKey;
import net.minecraft.data.client.*;
import net.minecraft.block.Block;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Exporter for vines blocks following block-models.md patterns.
 * Generates models for multi-directional vines with side and top attachments.
 */
public class VinesBlockExporter extends BaseBlockExporter {


    private static Model getVineModel(String vineType, boolean tinted) {
        if ("u".equals(vineType)) {
            return tinted ? ModModels.VINE_TOP_TINTED : ModModels.VINE_TOP_UNTINTED;
        }
        return tinted ? ModModels.VINE_SIDE_TINTED : ModModels.VINE_SIDE_UNTINTED;
    }


    private static TextureMap createVinesTextureMap(String texture) {
        return new TextureMap()
                .put(ModTextureKey.VINES, createBlockIdentifier(texture))
                .put(TextureKey.PARTICLE, createBlockIdentifier(texture));
    }

    private static MultipartBlockStateSupplier createVinesBlockstate(Block block, List<Identifier> sideModelIds,
                                                                     List<Identifier> topModelIds, List<Integer> weights) {
        MultipartBlockStateSupplier supplier = MultipartBlockStateSupplier.create(block);

        // One entry per attachment, with the texture sets as its weighted random choices
        List<TextureSetVariants.Choice> sides = choices(sideModelIds, weights);
        List<TextureSetVariants.Choice> tops = choices(topModelIds, weights);

        // Side models face south unrotated
        supplier.with(When.create().set(Properties.SOUTH, true), TextureSetVariants.variants(sides, TextureSetVariants.Transform.y(0)));
        supplier.with(When.create().set(Properties.WEST, true), TextureSetVariants.variants(sides, TextureSetVariants.Transform.y(90)));
        supplier.with(When.create().set(Properties.NORTH, true), TextureSetVariants.variants(sides, TextureSetVariants.Transform.y(180)));
        supplier.with(When.create().set(Properties.EAST, true), TextureSetVariants.variants(sides, TextureSetVariants.Transform.y(270)));
        supplier.with(When.create().set(Properties.UP, true), TextureSetVariants.variants(tops, TextureSetVariants.Transform.NONE));
        // Down attachment is the top model flipped (180° X rotation)
        supplier.with(When.create().set(WCVinesBlock.DOWN, true), TextureSetVariants.variants(tops, TextureSetVariants.Transform.of(180, 0)));

        return supplier;
    }

    private static List<TextureSetVariants.Choice> choices(List<Identifier> modelIds, List<Integer> weights) {
        List<TextureSetVariants.Choice> choices = new ArrayList<>();
        for (int i = 0; i < modelIds.size(); i++) {
            choices.add(new TextureSetVariants.Choice(modelIds.get(i), weights.get(i)));
        }
        return choices;
    }

    public static void registerVinesBlock(BlockStateModelGenerator generator, Block block, boolean tinted,
                                         String sideTexture, String topTexture) {
        TextureMap sideTextureMap = createVinesTextureMap(sideTexture);
        TextureMap topTextureMap = createVinesTextureMap(topTexture);

        Identifier sideModelId = getVineModel("1", tinted)
                .upload(createNestedModelId(block, "base"), sideTextureMap, generator.modelCollector);
        Identifier topModelId = getVineModel("u", tinted)
                .upload(createNestedModelId(block, "top"), topTextureMap, generator.modelCollector);

        MultipartBlockStateSupplier blockstate = createVinesBlockstate(block,
                List.of(sideModelId), List.of(topModelId), List.of(1));
        generator.blockStateCollector.accept(blockstate);

        // Register item model using side texture
        TextureMap itemTextureMap = new TextureMap().put(TextureKey.LAYER0, createBlockIdentifier(sideTexture));
        Models.GENERATED.upload(ModelIds.getItemModelId(block.asItem()),
                itemTextureMap, generator.modelCollector);
    }

    public static void registerVinesBlockWithRandomTextures(BlockStateModelGenerator generator, Block block,
                                                            boolean tinted, List<BlockDefinition.TextureVariantSet> textureSets) {
        List<Identifier> sideModelIds = new ArrayList<>();
        List<Identifier> topModelIds = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();

        for (int i = 0; i < textureSets.size(); i++) {
            BlockDefinition.TextureVariantSet set = textureSets.get(i);
            String[] textures = set.getTexturesAsArray();
            TextureMap sideTextureMap = createVinesTextureMap(textures[0]);
            TextureMap topTextureMap = createVinesTextureMap(textures.length > 1 ? textures[1] : textures[0]);

            Identifier sideModelId = getVineModel("1", tinted)
                    .upload(createNestedModelId(block, "base_v" + (i + 1)), sideTextureMap, generator.modelCollector);
            Identifier topModelId = getVineModel("u", tinted)
                    .upload(createNestedModelId(block, "top_v" + (i + 1)), topTextureMap, generator.modelCollector);

            sideModelIds.add(sideModelId);
            topModelIds.add(topModelId);
            weights.add(set.weight);
        }

        MultipartBlockStateSupplier blockstate = createVinesBlockstate(block, sideModelIds, topModelIds, weights);
        generator.blockStateCollector.accept(blockstate);

        // Register item model using first side texture
        String[] firstTextures = textureSets.get(0).getTexturesAsArray();
        TextureMap itemTextureMap = new TextureMap()
                .put(TextureKey.LAYER0, createBlockIdentifier(firstTextures[0]));
        Models.GENERATED.upload(ModelIds.getItemModelId(block.asItem()),
                itemTextureMap, generator.modelCollector);
    }


    public static void registerCustomVinesBlock(BlockStateModelGenerator generator, Block block, BlockDefinition definition) {
        boolean tinted = definition.isTinted() || definition.hasColorMult();

        if (definition.hasRandomTextures()) {
            List<BlockDefinition.TextureVariantSet> textureSets = new ArrayList<>();
            for (BlockDefinition.RandomTextureVariant randomTexture : definition.getRandomTextures()) {
                List<String> textures = randomTexture.getTextures();
                int weight = randomTexture.getWeight();

                if (textures != null && textures.size() >= 2) {
                    textureSets.add(new BlockDefinition.TextureVariantSet(
                            new String[]{textures.get(0), textures.get(1)}, weight, null));
                } else if (textures != null && textures.size() == 1) {
                    String texture = textures.get(0);
                    textureSets.add(new BlockDefinition.TextureVariantSet(
                            new String[]{texture, texture}, weight, null));
                } else {
                    textureSets.add(new BlockDefinition.TextureVariantSet(
                            new String[]{"missingno", "missingno"}, weight, null));
                }
            }
            registerVinesBlockWithRandomTextures(generator, block, tinted, textureSets);
        } else if (definition.getTextures() != null && definition.getTextures().size() >= 2) {
            registerVinesBlock(generator, block, tinted,
                    definition.getTextures().get(0), definition.getTextures().get(1));
        } else if (definition.getTextures() != null && definition.getTextures().size() == 1) {
            String texture = definition.getTextures().get(0);
            registerVinesBlock(generator, block, tinted, texture, texture);
        } else {
            registerVinesBlock(generator, block, tinted, "missingno", "missingno");
        }
    }

}
