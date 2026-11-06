package com.westerosblocks.block;

import com.westerosblocks.WesterosBlocks;
import com.westerosblocks.WesterosCreativeModeTabs;
import com.westerosblocks.block.custom.*;
import com.westerosblocks.data.BlockDefinition;
import com.westerosblocks.data.BlockDefinitionRegistry;

import net.fabricmc.loader.api.FabricLoader;
import com.westerosblocks.item.custom.WCBlockItem;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ModBlocks {
    private static final Map<String, Block> AUTO_REGISTERED_BLOCKS = new HashMap<>();

    private static void registerBlocksFromDefinitions() {
        try {
            BlockDefinitionRegistry registry = BlockDefinitionRegistry.getInstance();

            if (!registry.isInitialized()) {
                WesterosBlocks.LOGGER.warn("BlockDefinitionRegistry not initialized - skipping automatic registration");
                return;
            }

            WesterosBlocks.LOGGER.info("Starting automatic block registration from JSON definitions...");
            int registeredCount = 0;
            int skippedCount = 0;

            boolean isDevelopment = FabricLoader.getInstance().isDevelopmentEnvironment();

            for (BlockDefinition definition : registry.getAllDefinitions()) {
                try {
                    // Skip test blocks in production (blocks in westeros_test_tab)
                    if (!isDevelopment && WesterosCreativeModeTabs.isTestBlock(definition)) {
                        skippedCount++;
                        WesterosBlocks.LOGGER.debug("Skipped test block (production): {}", definition.getBlockName());
                        continue;
                    }

                    Block block = createBlockFromDefinition(definition);
                    if (block != null) {
                        Block registeredBlock = registerBlock(definition.getBlockName(), block);
                        AUTO_REGISTERED_BLOCKS.put(definition.getBlockName(), registeredBlock);
                        registeredCount++;

                        WesterosBlocks.LOGGER.debug("Auto-registered block: {} ({})",
                                definition.getBlockName(), definition.getBlockType());
                    } else {
                        skippedCount++;
                        WesterosBlocks.LOGGER.warn("Skipped unsupported block type '{}' for block '{}'",
                                definition.getBlockType(), definition.getBlockName());
                    }
                } catch (Exception e) {
                    skippedCount++;
                    WesterosBlocks.LOGGER.error("Failed to register block '{}': {}",
                            definition.getBlockName(), e.getMessage());
                }
            }

            WesterosBlocks.LOGGER.info("Automatic block registration complete: {} registered, {} skipped",
                    registeredCount, skippedCount);

        } catch (Exception e) {
            WesterosBlocks.LOGGER.error("Error during automatic block registration", e);
        }
    }

    /**
     * Creates a Block instance from a BlockDefinition using its {@link BlockType}.
     */
    private static Block createBlockFromDefinition(BlockDefinition definition) {
        BlockType type = BlockTypes.of(definition);
        if (type == null) {
            WesterosBlocks.LOGGER.warn("No block type '{}' for block '{}'",
                    definition.getBlockType(), definition.getBlockName());
            return null;
        }
        try {
            return type.create(definition);
        } catch (Exception e) {
            WesterosBlocks.LOGGER.error("Error creating block from definition: {}", definition.getBlockName(), e);
            return null;
        }
    }

    public static Block getAutoRegisteredBlock(String blockName) {
        return AUTO_REGISTERED_BLOCKS.get(blockName);
    }

    public static Map<String, Block> getAllAutoRegisteredBlocks() {
        return new HashMap<>(AUTO_REGISTERED_BLOCKS);
    }

    private static void registerAutoBlocksToCreativeTabs() {
        try {
            BlockDefinitionRegistry registry = BlockDefinitionRegistry.getInstance();

            if (!registry.isInitialized()) {
                WesterosBlocks.LOGGER.warn("BlockDefinitionRegistry not initialized - skipping creative tab registration");
                return;
            }

            // Group blocks by creative tab. LinkedHashMap preserves JSON declaration
            // order so each tab lists blocks in the order their definitions were loaded
            // (file-walk order, then JSON array order within each file) rather than
            // alphabetically by label.
            Map<String, List<Block>> blocksByTab = new LinkedHashMap<>();

            for (BlockDefinition definition : registry.getAllDefinitions()) {
                String creativeTab = definition.getCreativeTab();
                String blockName = definition.getBlockName();

                Block block = AUTO_REGISTERED_BLOCKS.get(blockName);

                if (block != null && creativeTab != null && !creativeTab.isEmpty()) {
                    blocksByTab.computeIfAbsent(creativeTab, k -> new ArrayList<>())
                               .add(block);
                }
            }

            int totalRegistered = 0;
            for (Map.Entry<String, List<Block>> entry : blocksByTab.entrySet()) {
                String tabName = entry.getKey();
                List<Block> blocks = entry.getValue();

                if (!blocks.isEmpty()) {
                    WesterosCreativeModeTabs.addToTab(tabName, blocks.toArray(new Block[0]));

                    totalRegistered += blocks.size();
                    WesterosBlocks.LOGGER.info("Added {} blocks to creative tab '{}'", blocks.size(), tabName);
                }
            }

            WesterosBlocks.LOGGER.info("Successfully registered {} auto-blocks to {} creative tabs",
                    totalRegistered, blocksByTab.size());

        } catch (Exception e) {
            WesterosBlocks.LOGGER.error("Error registering auto-blocks to creative tabs", e);
        }
    }

    public static void registerModBlocks() {
        WesterosBlocks.LOGGER.info("Registering Mod Blocks for " + WesterosBlocks.MOD_ID);

        // Build and register all blocks from JSON definitions, then group them into
        // creative tabs. Called explicitly (and in order) from WesterosBlocks.onInitialize()
        // after the BlockDefinitionRegistry has been initialized — no static-init side effects.
        registerBlocksFromDefinitions();
        registerAutoBlocksToCreativeTabs();
    }

    public static Block registerBlock(String name, Block block) {
        registerBlockItem(name, block);
        return Registry.register(Registries.BLOCK, Identifier.of(WesterosBlocks.MOD_ID, name), block);
    }

    private static void registerBlockItem(String name, Block block) {
        Registry.register(Registries.ITEM, Identifier.of(WesterosBlocks.MOD_ID, name),
                new WCBlockItem(block, new Item.Settings()));
    }

    /**
     * Registers a block without creating a BlockItem (used for wall torches, wall fans, etc.)
     * Made public so factories can register wall variants internally.
     * Also adds the block to AUTO_REGISTERED_BLOCKS for tracking.
     */
    public static Block registerBlockWithoutItem(String name, Block block) {
        Block registered = Registry.register(Registries.BLOCK, Identifier.of(WesterosBlocks.MOD_ID, name), block);
        AUTO_REGISTERED_BLOCKS.put(name, registered);
        return registered;
    }
}
