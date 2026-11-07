package com.westerosblocks.datagen.custom;

import net.minecraft.block.Block;
import net.minecraft.data.client.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

import com.westerosblocks.WesterosBlocks;
import com.westerosblocks.data.BlockDefinition;
import com.westerosblocks.datagen.ModTextureMap;
import com.westerosblocks.utils.ModProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import com.google.gson.JsonElement;

/**
 * Base class providing shared utilities for all block exporters.
 * Centralizes common functionality to eliminate code duplication and ensure consistency.
 *
 * @see <a href="block-models.md">block-models.md sections 5.2-5.6</a>
 */
public abstract class BaseBlockExporter {

    /**
     * Extracts the block name from the block's registry key.
     * Handles both formatted registry keys and plain block strings.
     *
     * @param block The block to extract the name from
     * @return The block name without namespace or Block{} wrapper
     */
    protected static String getBlockName(Block block) {
        String blockString = block.toString();
        if (blockString.contains(":")) {
            return blockString.split(":")[1].replace("}", "");
        }
        return blockString.toLowerCase().replace("block{", "").replace("}", "");
    }

    /**
     * Creates a nested model identifier following the pattern: {@code block/blockName/variant}.
     * This is the standard pattern for organizing block models by block type.
     *
     * @param block The block to create the identifier for
     * @param variant The model variant (e.g., "bottom", "top", "open", "horizontal")
     * @return The nested model identifier in format {@code westerosblocks:block/blockName/variant}
     */
    protected static Identifier createNestedModelId(Block block, String variant) {
        String blockName = getBlockName(block);
        String modelPath = "block/" + blockName + "/" + variant;
        return WesterosBlocks.id(modelPath);
    }

    /**
     * Creates a default nested model identifier using the block name as variant.
     * 
     * @param block The block
     * @return The nested model identifier
     */
    protected static Identifier createNestedModelId(Block block) {
        return createNestedModelId(block, getBlockName(block));
    }

    /**
     * Creates an identifier for block textures, handling namespaces properly.
     * Automatically prepends {@code "block/"} if no namespace is provided.
     *
     * @param texturePath The texture path (can include namespace like "westerosblocks:block/white_door")
     * @return The identifier for the block texture
     * @throws IllegalArgumentException if texturePath is null or empty
     */
    protected static Identifier createBlockIdentifier(String texturePath) {
        if (texturePath == null || texturePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Texture path cannot be null or empty");
        }
        
        // If the texture path includes a namespace
        int colon = texturePath.indexOf(':');
        if (colon >= 0) {
            return Identifier.of(texturePath.substring(0, colon), texturePath.substring(colon + 1));
        }
        // No namespace, use mod ID and prepend "block/"
        return WesterosBlocks.id("block/" + texturePath);
    }

    /**
     * Creates a model identifier for a block with optional variant.
     * 
     * @param block The block
     * @param variant The model variant (e.g., "bottom", "top", "open")
     * @return The model identifier
     */
    protected static Identifier createModelId(Block block, String variant) {
        String blockName = getBlockName(block);
        String modelPath = variant != null && !variant.isEmpty() 
            ? "block/" + blockName + "/" + variant
            : "block/" + blockName + "/" + blockName;
        return WesterosBlocks.id(modelPath);
    }

    /**
     * Creates a model identifier for a block without variant.
     * 
     * @param block The block
     * @return The model identifier
     */
    protected static Identifier createModelId(Block block) {
        return createModelId(block, null);
    }

    /**
     * Validates that texture paths array meets minimum requirements.
     * Ensures data integrity before texture processing.
     *
     * @param texturePaths The texture paths to validate
     * @param minRequired The minimum number of textures required
     * @throws IllegalArgumentException if validation fails with descriptive error message
     */
    protected static void validateTexturePaths(String[] texturePaths, int minRequired) {
        if (texturePaths == null || texturePaths.length < minRequired) {
            throw new IllegalArgumentException(
                "At least " + minRequired + " texture path(s) required, got " + 
                (texturePaths == null ? 0 : texturePaths.length));
        }
    }

    /**
     * Fills texture array to the specified target size.
     * Missing slots are filled with the last provided texture.
     *
     * @param texturePaths The input texture paths
     * @param targetSize The desired array size
     * @return Array of exactly targetSize texture paths
     * @throws IllegalArgumentException if texturePaths is null or empty
     */
    protected static String[] fillTextureArray(String[] texturePaths, int targetSize) {
        validateTexturePaths(texturePaths, 1);

        String[] filledTextures = new String[targetSize];
        for (int i = 0; i < targetSize; i++) {
            if (i < texturePaths.length) {
                filledTextures[i] = texturePaths[i];
            } else {
                filledTextures[i] = texturePaths[texturePaths.length - 1];
            }
        }
        return filledTextures;
    }

    /**
     * Fills texture array to ensure exactly 6 textures for cube models.
     * Shorthand for {@code fillTextureArray(texturePaths, 6)}.
     *
     * @param texturePaths The input texture paths (1-6 textures)
     * @return Array of exactly 6 texture paths
     * @throws IllegalArgumentException if texturePaths is null or empty
     */
    protected static String[] fillTextureArray(String[] texturePaths) {
        return fillTextureArray(texturePaths, 6);
    }

    /**
     * Creates a block state variant without rotation (0° rotation).
     * Follows block-models.md section 5.4: Custom BlockStateSupplier Method.
     *
     * @param modelId The model identifier to use for this variant
     * @return The block state variant with MODEL setting
     */
    protected static BlockStateVariant createVariant(Identifier modelId) {
        return TextureSetVariants.variant(modelId);
    }

    /**
     * Creates a block state variant with Y-axis rotation (left out when 0).
     * Follows block-models.md section 5.4: Custom BlockStateSupplier Method.
     *
     * @param modelId The model identifier to use for this variant
     * @param rotation The Y-axis rotation in degrees (must be 0, 90, 180, or 270)
     * @return The block state variant with MODEL and Y rotation settings
     * @throws IllegalArgumentException if rotation is not 0, 90, 180, or 270
     */
    protected static BlockStateVariant createVariant(Identifier modelId, int rotation) {
        return TextureSetVariants.variant(modelId, TextureSetVariants.Transform.y(rotation));
    }

    /**
     * Creates a simple blockstate supplier with a single model variant.
     * Follows block-models.md section 3.1: Simple Cube All pattern.

     * @param block The block to create the blockstate for
     * @param modelId The model identifier to use
     * @return The blockstate supplier with a single variant
     */
    protected static VariantsBlockStateSupplier createSimpleBlockState(Block block, Identifier modelId) {
        return VariantsBlockStateSupplier.create(block, createVariant(modelId));
    }

    /**
     * Creates a Model with automatic tinted/untinted path resolution.
     * Centralizes the common pattern of path selection based on tinting.
     *
     * @param tinted Whether the model should use tinted textures
     * @param modelPath The model path (without tinted/untinted prefix)
     * @param textureKeys The texture keys this model requires
     * @return The configured Model instance
     */
    protected static Model createTintedModel(boolean tinted, String modelPath, TextureKey... textureKeys) {
        String tintPath = tinted ? "block/tinted/" : "block/untinted/";
        String fullPath = tintPath + modelPath;
        return new Model(Optional.of(WesterosBlocks.id(fullPath)), Optional.empty(), textureKeys);
    }

    /**
     * Uploads a model with automatic nested model ID creation.
     * Combines model.upload() with createNestedModelId() in one call.
     *
     * @param model The model to upload
     * @param block The block this model belongs to
     * @param variant The model variant name
     * @param textureMap The texture map for this model
     * @param modelCollector The model collector to upload to
     * @return The identifier of the uploaded model
     */
    protected static Identifier uploadModel(Model model, Block block, String variant,
                                           TextureMap textureMap, BiConsumer<Identifier, Supplier<JsonElement>> modelCollector) {
        return model.upload(createNestedModelId(block, variant), textureMap, modelCollector);
    }


    /**
     * Creates a variant carrying only a Y-axis rotation (no model). Used by exporters that
     * supply the model via a separate blockstate coordinate (e.g. beam, mounted_mirror).
     * The rotation is omitted entirely when {@code rotation == 0}.
     *
     * @param rotation Y-axis rotation in degrees (0, 90, 180, or 270)
     */
    protected static BlockStateVariant rotationOnlyVariant(int rotation) {
        BlockStateVariant variant = BlockStateVariant.create();
        if (rotation != 0) {
            variant.put(VariantSettings.Y, TextureSetVariants.rotation(rotation));
        }
        return variant;
    }

    /**
     * Converts degrees to VariantSettings.Rotation enum.
     *
     * @param degrees Rotation in degrees (0, 90, 180, or 270)
     * @return The corresponding Rotation enum value
     * @throws IllegalArgumentException if degrees is not 0, 90, 180, or 270
     */
    protected static VariantSettings.Rotation toYRotation(int degrees) {
        return TextureSetVariants.rotation(degrees);
    }

    /**
     * Converts a Direction enum to rotation degrees for Y-axis rotation.
     * Standard mapping where model faces north at 0° rotation.
     *
     * @param direction The direction
     * @return Rotation in degrees (0, 90, 180, or 270)
     * @throws IllegalArgumentException if direction is not horizontal
     */
    protected static int getRotationForDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> 0;
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> throw new IllegalArgumentException("Direction must be horizontal: " + direction);
        };
    }

    /**
     * Converts a Direction to rotation degrees for south-default models.
     * Models that face south at 0° rotation (bench, table, chair).
     *
     * @param direction The direction
     * @return Rotation in degrees (0, 90, 180, or 270)
     */
    protected static int getFacingSouthDefaultRotation(Direction direction) {
        return switch (direction) {
            case NORTH -> 180;
            case SOUTH -> 0;
            case WEST -> 90;
            case EAST -> 270;
            default -> 0;
        };
    }

    /**
     * Gets the StateProperty from a block's state manager.
     *
     * @param block The block to inspect
     * @return The StateProperty, or null if not found
     */
    protected static ModProperties.StateProperty getStateProperty(Block block) {
        for (var property : block.getStateManager().getProperties()) {
            if (property instanceof ModProperties.StateProperty sp && "state".equals(property.getName()))
                return sp;
        }
        return null;
    }

    /**
     * Checks if a block has a "state" property.
     *
     * @param block The block to check
     * @return true if the block has a state property
     */
    protected static boolean hasStateProperty(Block block) {
        for (var property : block.getStateManager().getProperties()) {
            if (property.getName().equals("state"))
                return true;
        }
        return false;
    }

    /**
     * Creates a model identifier for custom (hand-authored) models.
     * Path format: {@code block/custom/blockName/variant}
     *
     * @param block The block
     * @param variant The model variant
     * @return The custom model identifier
     */
    protected static Identifier createCustomModelId(Block block, String variant) {
        String blockName = getBlockName(block);
        return WesterosBlocks.id("block/custom/" + blockName + "/" + variant);
    }

    /**
     * Creates a model identifier for generated models.
     * Path format: {@code block/blockName/variant}
     * Equivalent to {@link #createNestedModelId(Block, String)}.
     *
     * @param block The block
     * @param variant The model variant
     * @return The generated model identifier
     */
    protected static Identifier createGeneratedModelId(Block block, String variant) {
        return createNestedModelId(block, variant);
    }

    /**
     * Returns a model name with variant index suffix.
     * Example: {@code getModelName("base", 0)} returns {@code "base_v1"}.
     *
     * @param baseName The base name (e.g., state ID or "base")
     * @param variantIndex The 0-based variant index
     * @return The model name with suffix
     */
    protected static String getModelName(String baseName, int variantIndex) {
        return baseName + "_v" + (variantIndex + 1);
    }

    /**
     * Returns the stateID or "base" if null.
     *
     * @param stateID The state ID, possibly null
     * @return The state ID or "base"
     */
    protected static String getStateIdOrBase(String stateID) {
        return stateID == null ? "base" : stateID;
    }

    /**
     * The model choices for one state, one per texture set, named
     * {@code idForModel + "_v" + (setIdx+1)}. Custom-model states point at their hand-authored
     * models, others at generated ones.
     */
    protected static List<TextureSetVariants.Choice> stateChoices(
            Block block, BlockDefinition.StateVariant state, String idForModel) {
        return TextureSetVariants.choices(state, (setIdx, set) -> state.isCustomModel()
                ? createCustomModelId(block, getModelName(idForModel, setIdx))
                : createNestedModelId(block, getModelName(idForModel, setIdx)));
    }

    /**
     * Creates a VariantsBlockStateSupplier with optionally-rotated weighted variants.
     *
     * @param block The block
     * @param modelIds List of model identifiers
     * @param weights List of weights (null for uniform weight)
     * @param rotateRandom Whether to add 0/90/180/270° rotation variants
     * @return The block state supplier
     */
    protected static VariantsBlockStateSupplier createRotatedVariantsBlockState(
            Block block, List<Identifier> modelIds, List<Integer> weights, boolean rotateRandom) {
        List<TextureSetVariants.Choice> choices = new ArrayList<>();
        for (int i = 0; i < modelIds.size(); i++) {
            choices.add(new TextureSetVariants.Choice(modelIds.get(i), weights == null ? 1 : weights.get(i)));
        }
        List<BlockStateVariant> variants = TextureSetVariants.variants(
                choices, TextureSetVariants.Transform.NONE, rotateRandom ? 4 : 1);
        return VariantsBlockStateSupplier.create(block, variants.toArray(new BlockStateVariant[0]));
    }

    /**
     * Extracts texture variant sets from a BlockDefinition's random textures.
     * Handles the common pattern of converting RandomTextureVariant list to TextureVariantSet list,
     * merging overlay textures from the definition when present.
     *
     * @param definition The block definition with random textures
     * @return List of TextureVariantSets ready for use by registration methods
     */
    protected static List<BlockDefinition.TextureVariantSet> extractTextureVariantSets(BlockDefinition definition) {
        List<BlockDefinition.TextureVariantSet> sets = new ArrayList<>();
        boolean overlay = definition.hasOverlayTextures();

        for (BlockDefinition.RandomTextureVariant rv : definition.getRandomTextures()) {
            List<String> textures = rv.getTextures();
            int weight = rv.getWeight();

            if (textures != null && !textures.isEmpty()) {
                String[] textureArray = textures.toArray(new String[0]);
                String[] overlayArray = null;
                if (overlay && definition.getOverlayTextures() != null
                        && definition.getOverlayTextures().size() >= textures.size()) {
                    overlayArray = definition.getOverlayTextures().subList(0, textures.size()).toArray(new String[0]);
                }
                sets.add(new BlockDefinition.TextureVariantSet(textureArray, weight, overlayArray));
            } else {
                sets.add(new BlockDefinition.TextureVariantSet(new String[]{"missingno"}, weight, null));
            }
        }
        return sets;
    }

    /**
     * Gets an overlay texture by index, clamping to the last available texture.
     *
     * @param overlayTextures The overlay texture list
     * @param index The desired index
     * @return The texture at the index, or the last texture if index exceeds size, or null if empty
     */
    protected static String getOverlayTextureByIndex(List<String> overlayTextures, int index) {
        if (overlayTextures == null || overlayTextures.isEmpty()) {
            return null;
        }
        if (index >= overlayTextures.size()) {
            index = overlayTextures.size() - 1;
        }
        return overlayTextures.get(index);
    }

    /**
     * Creates a texture map for fence/wall blocks, with optional overlay textures.
     *
     * @param textures Array of 3 textures (top, bottom, side)
     * @param overlayTextures Array of 3 overlay textures, or null
     * @return The texture map
     */
    protected static TextureMap createFenceWallTextureMap(String[] textures, String[] overlayTextures) {
        if (overlayTextures != null) {
            return ModTextureMap.fenceWallOverlayTextures(
                    textures[0], textures[1], textures[2],
                    overlayTextures[0], overlayTextures[1], overlayTextures[2]);
        }
        return ModTextureMap.fenceWallTextures(textures[0], textures[1], textures[2]);
    }

    protected static void registerSimpleItemModel(BlockStateModelGenerator generator, Block block, Identifier textureId) {
        TextureMap itemTextureMap = TextureMap.layer0(textureId);
        Models.GENERATED.upload(ModelIds.getItemModelId(block.asItem()), itemTextureMap, generator.modelCollector);
    }

    protected static void registerParentedItemModel(BlockStateModelGenerator generator, Block block, Identifier modelId) {
        generator.registerParentedItemModel(block, modelId);
    }

    /** Item-pass variant of {@link #registerSimpleItemModel(BlockStateModelGenerator, Block, Identifier)}. */
    protected static void registerSimpleItemModel(ItemModelGenerator generator, Block block, Identifier textureId) {
        Models.GENERATED.upload(ModelIds.getItemModelId(block.asItem()),
                TextureMap.layer0(textureId), generator.writer);
    }

    /** Item-pass variant of {@link #registerParentedItemModel(BlockStateModelGenerator, Block, Identifier)}. Writes a plain {@code {"parent": modelId}} JSON. */
    protected static void registerParentedItemModel(ItemModelGenerator generator, Block block, Identifier modelId) {
        generator.writer.accept(ModelIds.getItemModelId(block.asItem()), () -> {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            json.addProperty("parent", modelId.toString());
            return json;
        });
    }
}