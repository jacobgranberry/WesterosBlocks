package com.westerosblocks.datagen.providers;

import com.westerosblocks.WesterosBlocks;
import com.westerosblocks.block.BlockType;
import com.westerosblocks.block.BlockTypes;
import com.westerosblocks.block.ModBlocks;

import com.westerosblocks.data.BlockDefinition;
import com.westerosblocks.data.BlockDefinitionRegistry;
import com.westerosblocks.datagen.custom.*;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.block.Block;
import net.minecraft.data.client.BlockStateModelGenerator;
import net.minecraft.data.client.ItemModelGenerator;

import java.util.Map;

public class ModModelProvider extends FabricModelProvider {

    @FunctionalInterface
    private interface BlockExporter {
        void export(BlockStateModelGenerator generator, Block block, BlockDefinition definition);
    }

    @FunctionalInterface
    private interface ItemModelExporter {
        void export(ItemModelGenerator generator, Block block, BlockDefinition definition);
    }

    /**
     * Dispatch for item-model generation. Block types absent from this map keep their
     * item-model registration in the block pass (status quo). As exporters are migrated
     * to the Fabric-idiomatic split, add an entry here and remove the item-model upload
     * from the exporter's block-pass method.
     */
    private static final Map<BlockType, ItemModelExporter> ITEM_EXPORTERS = Map.ofEntries(
        Map.entry(BlockTypes.HALF_DOOR, HalfDoorBlockExporter::registerCustomHalfDoorItemModel)
    );

    private static final Map<BlockType, BlockExporter> EXPORTERS = Map.ofEntries(
        Map.entry(BlockTypes.SOLID, SolidBlockExporter::registerCustomSolidBlock),
        Map.entry(BlockTypes.SAND, SolidBlockExporter::registerCustomSolidBlock),
        Map.entry(BlockTypes.SOUL_SAND, SolidBlockExporter::registerCustomSolidBlock),
        Map.entry(BlockTypes.DOOR, DoorBlockExporter::registerCustomDoorBlock),
        Map.entry(BlockTypes.TRAPDOOR, TrapDoorBlockExporter::registerCustomTrapDoorBlock),
        Map.entry(BlockTypes.LOG, LogBlockExporter::registerCustomLogBlock),
        Map.entry(BlockTypes.FLOWER_POT, FlowerPotBlockExporter::registerCustomFlowerPotBlock),
        Map.entry(BlockTypes.WEB, CrossBlockExporter::registerCustomCrossBlock),
        Map.entry(BlockTypes.PLANT, CrossBlockExporter::registerCustomCrossBlock),
        Map.entry(BlockTypes.SLAB, SlabBlockExporter::registerCustomSlabBlock),
        Map.entry(BlockTypes.HALF_DOOR, HalfDoorBlockExporter::registerCustomHalfDoorBlock),
        Map.entry(BlockTypes.FIRE, FireBlockExporter::registerCustomFireBlock),
        Map.entry(BlockTypes.LADDER, LadderBlockExporter::registerCustomLadderBlock),
        Map.entry(BlockTypes.ORIENTED_LADDER, OrientedLadderBlockExporter::registerCustomOrientedLadderBlock),
        Map.entry(BlockTypes.VINES, VinesBlockExporter::registerCustomVinesBlock),
        Map.entry(BlockTypes.PANE, PaneBlockExporter::registerCustomPaneBlock),
        Map.entry(BlockTypes.FENCE, FenceBlockExporter::registerCustomFenceBlock),
        Map.entry(BlockTypes.FENCE_GATE, FenceGateBlockExporter::registerCustomFenceGateBlock),
        Map.entry(BlockTypes.LEAVES, LeavesBlockExporter::registerCustomLeavesBlock),
        Map.entry(BlockTypes.BED, BedBlockExporter::registerCustomBedBlock),
        Map.entry(BlockTypes.TABLE, TableBlockExporter::registerCustomTableBlock),
        Map.entry(BlockTypes.BENCH, BenchBlockExporter::registerCustomBenchBlock),
        Map.entry(BlockTypes.CROP, CropBlockExporter::registerCustomCropBlock),
        Map.entry(BlockTypes.TORCH, TorchBlockExporter::registerTorchBlockFromDefinition),
        Map.entry(BlockTypes.FAN, FanBlockExporter::registerFanBlockFromDefinition),
        Map.entry(BlockTypes.RAIL, RailBlockExporter::registerRailBlockFromDefinition),
        Map.entry(BlockTypes.FURNACE, FurnaceBlockExporter::registerCustomFurnaceBlock),
        Map.entry(BlockTypes.WALL, WallBlockExporter::registerCustomWallBlock),
        Map.entry(BlockTypes.CUBOID, CuboidBlockExporter::registerCustomCuboidBlock),
        Map.entry(BlockTypes.BEACON, CuboidBlockExporter::registerCustomCuboidBlock),
        Map.entry(BlockTypes.CUBOID_NSEW, CuboidNSEWBlockExporter::registerCustomCuboidNSEWBlock),
        Map.entry(BlockTypes.CUBOID_NSEW_STACK, CuboidNSEWStackBlockExporter::registerCustomCuboidNSEWStackBlock),
        Map.entry(BlockTypes.CUBOID_NE, CuboidNEBlockExporter::registerCustomCuboidNEBlock),
        Map.entry(BlockTypes.CUBOID_NSEWUD, CuboidNSEWUDBlockExporter::registerCustomCuboidNSEWUDBlock),
        Map.entry(BlockTypes.CUBOID_16WAY, Cuboid16WayBlockExporter::registerCustomCuboid16WayBlock),
        Map.entry(BlockTypes.STAIR, StairBlockExporter::registerCustomStairBlock),
        Map.entry(BlockTypes.LAYER, LayerBlockExporter::registerCustomLayerBlock),
        Map.entry(BlockTypes.PARTICLE, ParticleEmitterExporter::registerCustomParticleEmitterBlock),
        Map.entry(BlockTypes.CHAIR, ChairBlockExporter::registerChairBlock),
        Map.entry(BlockTypes.MOUNTED, MountedBlockExporter::registerMountedBlock),
        Map.entry(BlockTypes.MOUNTED_SLAB, MountedSlabBlockExporter::registerMountedSlabBlock),
        Map.entry(BlockTypes.FLOWERBED, FlowerbedBlockExporter::registerCustomFlowerbedBlock),
        Map.entry(BlockTypes.AWNING, AwningBlockExporter::registerCustomAwningBlock),
        Map.entry(BlockTypes.BIG_DOOR, BigDoorBlockExporter::registerCustomBigDoorBlock),
        Map.entry(BlockTypes.TALL_DOOR, TallDoorBlockExporter::registerCustomTallDoorBlock),
        Map.entry(BlockTypes.BUNTING, BuntingBlockExporter::registerBuntingBlock),
        Map.entry(BlockTypes.BALCONY, BalconyBlockExporter::registerCustomBalconyBlock),
        Map.entry(BlockTypes.ARROW_SLIT, ArrowSlitBlockExporter::registerCustomArrowSlitBlock),
        Map.entry(BlockTypes.MOUNTED_MIRROR, MountedMirrorBlockExporter::registerMountedMirrorBlock)
    );

    static {
        // Every block type needs a model exporter; fail datagen loudly instead of silently
        // shipping blocks without blockstates.
        for (BlockType type : BlockTypes.all()) {
            if (!EXPORTERS.containsKey(type)) {
                throw new IllegalStateException("No model exporter for block type '" + type.id() + "'");
            }
        }
    }

    private final FabricDataOutput output;

    public ModModelProvider(FabricDataOutput output) {
        super(output);
        this.output = output;
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator bsmg) {
        generateModelsFromDefinitions(bsmg);
    }

    private void generateModelsFromDefinitions(BlockStateModelGenerator bsmg) {
        BlockDefinitionRegistry registry = BlockDefinitionRegistry.getInstance();

        if (!registry.isInitialized()) {
            return;
        }

        // Generate models for all block definitions
        for (BlockDefinition definition : registry.getAllDefinitions()) {
            Block block = ModBlocks.getAutoRegisteredBlock(definition.getBlockName());
            if (block != null) {
                generateModelFromDefinition(bsmg, block, definition);
            }
        }
    }

    /**
     * Generates models for a block from its definition using the appropriate exporter
     */
    private void generateModelFromDefinition(BlockStateModelGenerator bsmg, Block block, BlockDefinition definition) {
        BlockType type = BlockTypes.of(definition);
        BlockExporter exporter = type == null ? null : EXPORTERS.get(type);
        if (exporter != null) {
            try {
                exporter.export(bsmg, block, definition);
            } catch (Exception e) {
                WesterosBlocks.LOGGER.error("Error generating model for block '{}': {}",
                        definition.getBlockName(), e.getMessage());
            }
        } else {
            WesterosBlocks.LOGGER.warn("Unsupported block type '{}' for model generation: {}",
                    definition.getBlockType(), definition.getBlockName());
        }
    }

    @Override
    public void generateItemModels(ItemModelGenerator itemModelGenerator) {
        BlockDefinitionRegistry registry = BlockDefinitionRegistry.getInstance();
        if (!registry.isInitialized()) {
            return;
        }

        for (BlockDefinition definition : registry.getAllDefinitions()) {
            Block block = ModBlocks.getAutoRegisteredBlock(definition.getBlockName());
            if (block == null) continue;

            BlockType type = BlockTypes.of(definition);
            ItemModelExporter exporter = type == null ? null : ITEM_EXPORTERS.get(type);
            if (exporter == null) continue;

            try {
                exporter.export(itemModelGenerator, block, definition);
            } catch (Exception e) {
                WesterosBlocks.LOGGER.error("Error generating item model for block '{}': {}",
                        definition.getBlockName(), e.getMessage());
            }
        }
    }
}
