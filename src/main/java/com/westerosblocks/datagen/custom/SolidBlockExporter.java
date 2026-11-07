package com.westerosblocks.datagen.custom;

import com.westerosblocks.datagen.ModModels;
import com.westerosblocks.datagen.ModTextureKey;
import com.westerosblocks.utils.ModProperties;
import net.minecraft.block.Block;
import net.minecraft.data.client.*;
import net.minecraft.util.Identifier;
import com.westerosblocks.datagen.ModTextureMap;
import com.westerosblocks.block.custom.WCSolidBlock;
import com.westerosblocks.data.BlockDefinition;

import java.util.ArrayList;
import java.util.List;

public class SolidBlockExporter extends BaseBlockExporter {

    private static boolean isSymmetrical(Block block) {
        return (block instanceof WCSolidBlock) && ((WCSolidBlock) block).symmetrical;
    }

    public static void registerCustomSolidBlock(BlockStateModelGenerator generator, Block block, BlockDefinition definition) {
        var states = definition.getStates();
        if (states == null || states.isEmpty()) {
            return;
        }

        boolean hasSymmetrical = isSymmetrical(block);
        boolean hasRotateRandom = definition.hasRotateRandom();

        // Generate blockstates
        generateBlockState(generator, block, states, hasSymmetrical, hasRotateRandom);

        boolean isTinted = definition.isTinted() || definition.hasColorMult();
        for (BlockDefinition.StateVariant state : states) {
            String fname = getStateIdOrBase(state.getStateID());
            boolean isOverlay = state.hasOverlayTextures();

            state.forEachTextureSet((setIdx, set) -> {
                if (hasSymmetrical) {
                    generateSolidModel(generator, block, getModelName(fname, setIdx, true),
                        state, setIdx, isTinted, isOverlay, true);
                    generateSolidModel(generator, block, getModelName(fname, setIdx, false),
                        state, setIdx, isTinted, isOverlay, false);
                } else {
                    generateSolidModel(generator, block, getModelName(fname, setIdx),
                        state, setIdx, isTinted, isOverlay, false);
                }
            });
        }

        // Item model
        String firstName = getStateIdOrBase(states.get(0).getStateID());
        Identifier itemModelId = hasSymmetrical
            ? createNestedModelId(block, getModelName(firstName, 0, true))
            : createNestedModelId(block, getModelName(firstName, 0));
        registerParentedItemModel(generator, block, itemModelId);
    }


    private static String getModelName(String fname, int setIdx, boolean symmetrical) {
        String dir = symmetrical ? "symmetrical" : "asymmetrical";
        return dir + "/" + getModelName(fname, setIdx);
    }

    private static void generateBlockState(BlockStateModelGenerator generator, Block block,
            List<BlockDefinition.StateVariant> states, boolean hasSymmetrical, boolean hasRotateRandom) {

        ModProperties.StateProperty stateProperty = getStateProperty(block);
        boolean hasMultipleStates = stateProperty != null && states.size() > 1;
        int rotationCount = hasRotateRandom ? 4 : 1;

        if (hasSymmetrical && hasMultipleStates) {
            var map = BlockStateVariantMap.create(WCSolidBlock.SYMMETRICAL, stateProperty);
            for (BlockDefinition.StateVariant state : states) {
                map.register(true, state.getStateID(), TextureSetVariants.variants(solidChoices(block, state, true), TextureSetVariants.Transform.NONE, rotationCount));
                map.register(false, state.getStateID(), TextureSetVariants.variants(solidChoices(block, state, false), TextureSetVariants.Transform.NONE, rotationCount));
            }
            generator.blockStateCollector.accept(VariantsBlockStateSupplier.create(block).coordinate(map));
        } else if (hasMultipleStates) {
            var map = BlockStateVariantMap.create(stateProperty);
            for (BlockDefinition.StateVariant state : states) {
                map.register(state.getStateID(), TextureSetVariants.variants(solidChoices(block, state, null), TextureSetVariants.Transform.NONE, rotationCount));
            }
            generator.blockStateCollector.accept(VariantsBlockStateSupplier.create(block).coordinate(map));
        } else if (hasSymmetrical) {
            List<TextureSetVariants.Choice> sym = new ArrayList<>();
            List<TextureSetVariants.Choice> asym = new ArrayList<>();
            for (BlockDefinition.StateVariant state : states) {
                sym.addAll(solidChoices(block, state, true));
                asym.addAll(solidChoices(block, state, false));
            }
            var map = BlockStateVariantMap.create(WCSolidBlock.SYMMETRICAL)
                    .register(true, TextureSetVariants.variants(sym, TextureSetVariants.Transform.NONE, rotationCount))
                    .register(false, TextureSetVariants.variants(asym, TextureSetVariants.Transform.NONE, rotationCount));
            generator.blockStateCollector.accept(VariantsBlockStateSupplier.create(block).coordinate(map));
        } else {
            List<TextureSetVariants.Choice> choices = new ArrayList<>();
            for (BlockDefinition.StateVariant state : states) {
                choices.addAll(solidChoices(block, state, null));
            }
            List<BlockStateVariant> variants = TextureSetVariants.variants(choices, TextureSetVariants.Transform.NONE, rotationCount);
            generator.blockStateCollector.accept(
                    VariantsBlockStateSupplier.create(block, variants.toArray(new BlockStateVariant[0])));
        }
    }

    /** The model choices for one state. {@code symmetrical} is null for non-symmetrical blocks. */
    private static List<TextureSetVariants.Choice> solidChoices(Block block, BlockDefinition.StateVariant state,
            Boolean symmetrical) {
        String fname = getStateIdOrBase(state.getStateID());
        return TextureSetVariants.choices(state, (setIdx, set) -> {
            if (set == null) return null;
            String modelName = (symmetrical == null)
                    ? getModelName(fname, setIdx)
                    : getModelName(fname, setIdx, symmetrical);
            return createNestedModelId(block, modelName);
        });
    }

    private static void generateSolidModel(BlockStateModelGenerator generator, Block block, String modelName,
            BlockDefinition.StateVariant state, int setIdx, boolean isTinted, boolean isOverlay, boolean isSymmetrical) {

        BlockDefinition.RandomTextureVariant set = state.getRandomTextureSet(setIdx);
        if (set == null) return;
        Identifier modelId = createNestedModelId(block, modelName);

        if (isOverlay) {
            generateOverlayModel(generator, modelId, set, state.getOverlayTextures(), isTinted, isSymmetrical);
        } else if (set.getTextureCount() > 1 || isTinted) {
            generateCubeModel(generator, modelId, set, isTinted, isSymmetrical);
        } else {
            generateCubeAllModel(generator, modelId, set);
        }
    }

    private static void generateCubeAllModel(BlockStateModelGenerator generator, Identifier modelId,
            BlockDefinition.RandomTextureVariant set) {
        TextureMap textureMap = new TextureMap()
            .put(TextureKey.ALL, createBlockIdentifier(set.getTextureByIndex(0)));
        Models.CUBE_ALL.upload(modelId, textureMap, generator.modelCollector);
    }

    private static void generateCubeModel(BlockStateModelGenerator generator, Identifier modelId,
            BlockDefinition.RandomTextureVariant set, boolean isTinted, boolean isSymmetrical) {

        String down = set.getTextureByIndex(0);
        String up = set.getTextureByIndex(1);
        String north = set.getTextureByIndex(2);
        String south = set.getTextureByIndex(3);
        String west = isSymmetrical ? set.getTextureByIndex(4) : set.getTextureByIndex(6);
        String east = isSymmetrical ? set.getTextureByIndex(5) : set.getTextureByIndex(7);

        String[] textures = fillTextureArray(new String[]{down, up, north, south, west, east});
        TextureMap textureMap = ModTextureMap.customAllSides(textures);

        if (isTinted) {
            ModModels.CUBE_TINTED.upload(modelId, textureMap, generator.modelCollector);
        } else {
            Models.CUBE.upload(modelId, textureMap, generator.modelCollector);
        }
    }

    private static void generateOverlayModel(BlockStateModelGenerator generator, Identifier modelId,
            BlockDefinition.RandomTextureVariant set, List<String> overlayTextures, boolean isTinted, boolean isSymmetrical) {

        String down = set.getTextureByIndex(0);
        String up = set.getTextureByIndex(1);
        String north = set.getTextureByIndex(2);
        String south = set.getTextureByIndex(3);
        String west = isSymmetrical ? set.getTextureByIndex(4) : set.getTextureByIndex(6);
        String east = isSymmetrical ? set.getTextureByIndex(5) : set.getTextureByIndex(7);

        String[] textures = fillTextureArray(new String[]{down, up, north, south, west, east});
        TextureMap textureMap = ModTextureMap.customAllSides(textures);

        if (overlayTextures != null && !overlayTextures.isEmpty()) {
            textureMap.put(ModTextureKey.DOWN_OVERLAY, createBlockIdentifier(getOverlayTextureByIndex(overlayTextures, 0)));
            textureMap.put(ModTextureKey.UP_OVERLAY, createBlockIdentifier(getOverlayTextureByIndex(overlayTextures, 1)));
            textureMap.put(ModTextureKey.NORTH_OVERLAY, createBlockIdentifier(getOverlayTextureByIndex(overlayTextures, 2)));
            textureMap.put(ModTextureKey.SOUTH_OVERLAY, createBlockIdentifier(getOverlayTextureByIndex(overlayTextures, 3)));

            int westIdx = isSymmetrical ? 4 : 6;
            int eastIdx = isSymmetrical ? 5 : 7;
            textureMap.put(ModTextureKey.WEST_OVERLAY, createBlockIdentifier(getOverlayTextureByIndex(overlayTextures, westIdx)));
            textureMap.put(ModTextureKey.EAST_OVERLAY, createBlockIdentifier(getOverlayTextureByIndex(overlayTextures, eastIdx)));
        }

        Model overlayModel = isTinted ? ModModels.CUBE_OVERLAY_TINTED : ModModels.CUBE_OVERLAY_UNTINTED;
        overlayModel.upload(modelId, textureMap, generator.modelCollector);
    }
}
