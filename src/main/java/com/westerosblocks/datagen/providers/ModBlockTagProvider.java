package com.westerosblocks.datagen.providers;
import com.westerosblocks.WesterosBlocks;
import com.westerosblocks.WesterosCreativeModeTabs;
import com.westerosblocks.data.BlockDefinition;
import com.westerosblocks.data.BlockDefinitionRegistry;
import com.westerosblocks.data.BlockTagDefinition;
import com.westerosblocks.data.BlockTagEntry;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.westerosblocks.block.BlockType;
import com.westerosblocks.block.BlockTypes;
import com.westerosblocks.block.ModBlocks;

public class ModBlockTagProvider extends FabricTagProvider<Block> {

    public ModBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, RegistryKeys.BLOCK, registriesFuture);
    }

    /**
     * Check if a block definition is a test block (should be excluded from production)
     */
    private boolean isTestBlock(BlockDefinition definition) {
        return WesterosCreativeModeTabs.isTestBlock(definition);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        BlockDefinitionRegistry registry = BlockDefinitionRegistry.getInstance();

        // Initialize custom tag builders and seed with external block IDs from block_tags.json
        Map<String, FabricTagProvider<Block>.FabricTagBuilder> customTagBuilders = new HashMap<>();
        BlockTagDefinition blockTagDef = registry.getBlockTags();
        if (blockTagDef != null) {
            for (BlockTagEntry entry : blockTagDef.getBlockTags()) {
                TagKey<Block> tagKey = TagKey.of(RegistryKeys.BLOCK, WesterosBlocks.id(entry.getCustomTag()));
                FabricTagProvider<Block>.FabricTagBuilder builder = getOrCreateTagBuilder(tagKey);
                for (String blockId : entry.getBlockNames()) {
                    builder.addOptional(Identifier.of(blockId));
                }
                customTagBuilders.put(entry.getCustomTag(), builder);
            }
        }

        // Single loop through all definitions
        for (BlockDefinition definition : registry.getAllDefinitions()) {
            // Skip test blocks
            if (isTestBlock(definition)) {
                continue;
            }

            String blockName = definition.getBlockName();
            Block block = ModBlocks.getAutoRegisteredBlock(blockName);

            // Skip if block wasn't registered
            if (block == null) {
                continue;
            }

            // Tags each block type declares in BlockTypes.
            BlockType type = BlockTypes.of(definition);
            if (type != null) {
                for (BlockType.TagRule rule : type.tagRules()) {
                    if (!rule.when().test(definition)) {
                        continue;
                    }
                    Block target = block;
                    if (rule.target() == BlockType.TagTarget.COMPANION) {
                        target = ModBlocks.getAutoRegisteredBlock(type.companionName(blockName));
                        if (target == null) {
                            continue;
                        }
                    }
                    getOrCreateTagBuilder(rule.tag()).add(target);
                }
            }

            // Blocks that opt in via the hasClimb option (e.g. steep ladders, which are
            // cuboid-nsew rather than the "ladder" type) join the climbable tag.
            if (definition.hasClimb()) {
                getOrCreateTagBuilder(BlockTags.CLIMBABLE).add(block);
            }

            // Add to custom tags declared in block definitions (e.g., polished_stone_ctm, wool_ctm)
            List<String> customTags = definition.getCustomTags();
            if (customTags != null) {
                for (String tag : customTags) {
                    FabricTagProvider<Block>.FabricTagBuilder builder = customTagBuilders.computeIfAbsent(
                        tag, t -> getOrCreateTagBuilder(TagKey.of(RegistryKeys.BLOCK, WesterosBlocks.id(t)))
                    );
                    builder.add(block);
                }
            }
        }
    }
}