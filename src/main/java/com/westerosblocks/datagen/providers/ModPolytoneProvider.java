package com.westerosblocks.datagen.providers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.westerosblocks.WesterosBlocks;
import com.westerosblocks.WesterosCreativeModeTabs;
import com.westerosblocks.data.BlockDefinition;
import com.westerosblocks.data.BlockDefinitionRegistry;
import com.westerosblocks.data.ColorMapDefinition;
import com.westerosblocks.data.ColorMapEntry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.data.DataOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.DataWriter;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ModPolytoneProvider implements DataProvider {
    private final DataOutput.PathResolver blockModifiersPathResolver;
    private final DataOutput.PathResolver itemModifiersPathResolver;

    public ModPolytoneProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        // Create path resolvers for polytone/block_modifiers and polytone/item_modifiers
        this.blockModifiersPathResolver = output.getResolver(DataOutput.OutputType.RESOURCE_PACK, "polytone/block_modifiers");
        this.itemModifiersPathResolver = output.getResolver(DataOutput.OutputType.RESOURCE_PACK, "polytone/item_modifiers");
    }

    @Override
    public CompletableFuture<?> run(DataWriter writer) {
        Map<String, List<String>> blocksByColormap = new HashMap<>();
        BlockDefinitionRegistry registry = BlockDefinitionRegistry.getInstance();

        if (!registry.isInitialized()) {
            WesterosBlocks.LOGGER.warn("BlockDefinitionRegistry not initialized - skipping Polytone generation");
            return CompletableFuture.completedFuture(null);
        }

        // Blocks with several colorMults, keyed by their ordered colormap ids. Each list
        // entry maps to the tint index of the same position (colorMults[0] -> tintindex 0).
        Map<List<String>, List<String>> blocksByIndexedColormaps = new HashMap<>();

        // Group blocks by their colorMult value(s)
        for (BlockDefinition definition : registry.getAllDefinitions()) {
            // Skip test blocks (blocks in westeros_test_tab)
            if (WesterosCreativeModeTabs.isTestBlock(definition)) {
                continue;
            }

            String blockId = WesterosBlocks.MOD_ID + ":" + definition.getBlockName();

            // Multiple colorMults: one modifier with an indexed colormap. Polytone 1.21.1
            // replaces (not merges) the colormap when two modifiers target one block, so
            // listing the block under each colormap separately would tint every index the same.
            if (definition.hasColorMults()) {
                List<String> colormapIds = definition.getColorMults().stream()
                        .map(this::extractColormapId)
                        .toList();
                blocksByIndexedColormaps.computeIfAbsent(colormapIds, k -> new ArrayList<>()).add(blockId);
                continue;
            }

            // Single colorMult: one colormap for every tint index
            if (definition.hasColorMult()) {
                String colorMult = definition.getColorMult();
                String colormapId = extractColormapId(colorMult);
                blocksByColormap.computeIfAbsent(colormapId, k -> new ArrayList<>()).add(blockId);
            }
        }

        // Merge vanilla blocks from color_maps.json
        ColorMapDefinition colorMaps = registry.getColorMaps();
        int vanillaBlockCount = 0;
        if (colorMaps != null && colorMaps.hasColorMaps()) {
            for (ColorMapEntry entry : colorMaps.getColorMaps()) {
                if (entry.hasColorMult() && entry.hasBlockNames()) {
                    String colorMult = entry.getColorMult();
                    // Skip hex colors (e.g., "#FFFFFF") as they don't reference colormap textures
                    if (colorMult.startsWith("#")) {
                        WesterosBlocks.LOGGER.debug("Skipping hex color {} for blocks: {}", colorMult, entry.getBlockNames());
                        continue;
                    }
                    String colormapId = extractColormapId(colorMult);
                    List<String> blockIds = entry.getBlockNames();
                    blocksByColormap.computeIfAbsent(colormapId, k -> new ArrayList<>()).addAll(blockIds);
                    vanillaBlockCount += blockIds.size();
                }
            }
            WesterosBlocks.LOGGER.info("Added {} vanilla/external blocks from color_maps.json", vanillaBlockCount);
        }

        if (blocksByColormap.isEmpty() && blocksByIndexedColormaps.isEmpty()) {
            WesterosBlocks.LOGGER.info("No blocks with colorMult found - skipping Polytone generation");
            return CompletableFuture.completedFuture(null);
        }

        // Create list of write futures
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : blocksByColormap.entrySet()) {
            String colormapId = entry.getKey();
            writeModifiers(writer, futures, colormapId + "_tinted_blocks", entry.getValue(),
                    new JsonPrimitive(WesterosBlocks.MOD_ID + ":" + colormapId));
        }

        for (Map.Entry<List<String>, List<String>> entry : blocksByIndexedColormaps.entrySet()) {
            List<String> colormapIds = entry.getKey();
            JsonObject indexed = new JsonObject();
            for (int i = 0; i < colormapIds.size(); i++) {
                indexed.addProperty(String.valueOf(i), WesterosBlocks.MOD_ID + ":" + colormapIds.get(i));
            }
            writeModifiers(writer, futures, String.join("__", colormapIds) + "_tinted_blocks", entry.getValue(), indexed);
        }

        int fileCount = blocksByColormap.size() + blocksByIndexedColormaps.size();
        WesterosBlocks.LOGGER.info("Successfully generated {} Polytone modifier files ({} in block_modifiers, {} in item_modifiers)",
                fileCount * 2, fileCount, fileCount);

        // Return combined future of all writes
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * Writes the same modifier (targets + colormap) to block_modifiers and item_modifiers.
     */
    private void writeModifiers(DataWriter writer, List<CompletableFuture<?>> futures, String fileName,
                                List<String> blockIds, JsonElement colormap) {
        // Sort block IDs for consistent output
        Collections.sort(blockIds);

        JsonObject json = new JsonObject();
        JsonArray targetsArray = new JsonArray();
        blockIds.forEach(targetsArray::add);
        json.add("targets", targetsArray);
        json.add("colormap", colormap);

        Identifier fileId = Identifier.of(WesterosBlocks.MOD_ID, fileName);
        futures.add(DataProvider.writeToPath(writer, json, blockModifiersPathResolver.resolveJson(fileId)));
        futures.add(DataProvider.writeToPath(writer, json, itemModifiersPathResolver.resolveJson(fileId)));
    }

    /**
     * Extracts the colormap identifier from a colorMult value
     * E.g., "textures/colormap/sand" -> "sand", "birch" -> "birch"
     */
    private String extractColormapId(String colorMult) {
        if (colorMult.contains("/")) {
            String[] parts = colorMult.split("/");
            return parts[parts.length - 1];
        }
        return colorMult;
    }

    @Override
    public String getName() {
        return "Polytone Block & Item Modifiers";
    }
}
