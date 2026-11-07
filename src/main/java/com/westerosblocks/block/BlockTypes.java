package com.westerosblocks.block;

import com.westerosblocks.WesterosBlocks;
import com.westerosblocks.block.BlockType.Trait;
import com.westerosblocks.block.custom.*;
import com.westerosblocks.data.BlockDefinition;
import net.minecraft.block.Block;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The single list of block types this mod supports. Adding a type means adding one entry here
 * (plus its model exporter in {@code ModModelProvider}, which datagen checks is present).
 */
public final class BlockTypes {
    private static final Map<String, BlockType> BY_ID = new LinkedHashMap<>();

    /** Mod tag for multi-block doors; also seeded from block_tags.json. */
    public static final TagKey<Block> BIG_DOOR_TAG = TagKey.of(RegistryKeys.BLOCK, WesterosBlocks.id("big_door"));

    public static final BlockType SOLID = register(BlockType.builder("solid", new WCSolidBlock.Factory()));
    public static final BlockType DOOR = register(BlockType.builder("door", new WCDoorBlock.Factory())
            .tag(BlockTags.DOORS));
    public static final BlockType HALF_DOOR = register(BlockType.builder("halfdoor", new WCHalfDoorBlock.Factory())
            .tag(BlockTags.DOORS));
    public static final BlockType LOG = register(BlockType.builder("log", new WCLogBlock.Factory())
            .tag(BlockTags.LOGS));
    public static final BlockType PLANT = register(BlockType.builder("plant", new WCPlantBlock.Factory())
            .tag(BlockTags.FLOWERS)
            .traits(Trait.INSUBSTANTIAL, Trait.VEGETATION));
    public static final BlockType FLOWER_POT = register(BlockType.builder("flowerpot", new WCFlowerPotBlock.Factory())
            .tag(BlockTags.FLOWER_POTS));
    public static final BlockType WEB = register(BlockType.builder("web", new WCWebBlock.Factory())
            .traits(Trait.INSUBSTANTIAL));
    public static final BlockType SLAB = register(BlockType.builder("slab", new WCSlabBlock.Factory())
            .tag(BlockTags.SLABS));
    public static final BlockType FIRE = register(BlockType.builder("fire", new WCFireBlock.Factory())
            .tag(BlockTags.FIRE)
            .traits(Trait.INSUBSTANTIAL));
    public static final BlockType LADDER = register(BlockType.builder("ladder", new WCLadderBlock.Factory())
            .tag(BlockTags.CLIMBABLE, BlockTypes::isClimbable));
    public static final BlockType ORIENTED_LADDER = register(BlockType.builder("oriented_ladder", new WCOrientedLadderBlock.Factory())
            .tag(BlockTags.CLIMBABLE, BlockTypes::isClimbable));
    public static final BlockType VINES = register(BlockType.builder("vines", new WCVinesBlock.Factory())
            .traits(Trait.INSUBSTANTIAL, Trait.VEGETATION));
    public static final BlockType PANE = register(BlockType.builder("pane", new WCPaneBlock.Factory()));
    public static final BlockType FENCE = register(BlockType.builder("fence", new WCFenceBlock.Factory())
            .tag(BlockTags.FENCES)
            .tag(BlockTags.WOODEN_FENCES, definition -> "wood".equalsIgnoreCase(definition.getSoundGroup())));
    public static final BlockType FENCE_GATE = register(BlockType.builder("fencegate", new WCFenceGateBlock.Factory())
            .tag(BlockTags.FENCE_GATES));
    public static final BlockType TRAPDOOR = register(BlockType.builder("trapdoor", new WCTrapDoorBlock.Factory())
            .tag(BlockTags.TRAPDOORS));
    public static final BlockType LEAVES = register(BlockType.builder("leaves", new WCLeavesBlock.Factory())
            .tag(BlockTags.LEAVES)
            .traits(Trait.VEGETATION, Trait.TREE_RELATED));
    public static final BlockType BED = register(BlockType.builder("bed", new WCBedBlock.Factory())
            .tag(BlockTags.BEDS));
    public static final BlockType CROP = register(BlockType.builder("crop", new WCCropBlock.Factory())
            .tag(BlockTags.CROPS)
            .traits(Trait.INSUBSTANTIAL, Trait.VEGETATION));
    public static final BlockType TORCH = register(BlockType.builder("torch", new WCTorchBlock.Factory())
            .companion(BlockTypes::wallVariantName)
            .tag(BlockTags.WALL_POST_OVERRIDE)
            .companionTag(BlockTags.WALL_POST_OVERRIDE)
            .traits(Trait.INSUBSTANTIAL));
    public static final BlockType FAN = register(BlockType.builder("fan", new WCFanBlock.Factory())
            .companion(BlockTypes::wallVariantName)
            .tag(BlockTags.CORALS)
            .companionTag(BlockTags.WALL_CORALS)
            .traits(Trait.INSUBSTANTIAL));
    public static final BlockType RAIL = register(BlockType.builder("rail", new WCRailBlock.Factory())
            .tag(BlockTags.RAILS)
            .traits(Trait.INSUBSTANTIAL));
    public static final BlockType FURNACE = register(BlockType.builder("furnace", new WCFurnaceBlock.Factory()));
    public static final BlockType WALL = register(BlockType.builder("wall", new WCWallBlock.Factory())
            .tag(BlockTags.WALLS));
    public static final BlockType STAIR = register(BlockType.builder("stair", new WCStairBlock.Factory())
            .tag(BlockTags.STAIRS));
    public static final BlockType CUBOID = register(BlockType.builder("cuboid", new WCCuboidBlock.Factory()));
    public static final BlockType CUBOID_NSEW = register(BlockType.builder("cuboid-nsew", new WCCuboidNSEWBlock.Factory()));
    public static final BlockType CUBOID_NSEW_STACK = register(BlockType.builder("cuboid-nsew-stack", new WCCuboidNSEWStackBlock.Factory()));
    public static final BlockType CUBOID_NE = register(BlockType.builder("cuboid-ne", new WCCuboidNEBlock.Factory()));
    public static final BlockType CUBOID_NSEWUD = register(BlockType.builder("cuboid-nsewud", new WCCuboidNSEWUDBlock.Factory()));
    public static final BlockType CUBOID_16WAY = register(BlockType.builder("cuboid-16way", new WCCuboid16WayBlock.Factory()));
    public static final BlockType LAYER = register(BlockType.builder("layer", new WCLayerBlock.Factory()));
    public static final BlockType BEACON = register(BlockType.builder("beacon", new WCBeaconBlock.Factory()));
    public static final BlockType SAND = register(BlockType.builder("sand", new WCSandBlock.Factory())
            .tag(BlockTags.SAND));
    public static final BlockType SOUL_SAND = register(BlockType.builder("soul-sand", new WCSoulSandBlock.Factory()));
    public static final BlockType PARTICLE = register(BlockType.builder("particle", new WCParticleEmitterBlock.Factory())
            .traits(Trait.NO_BLOCK_TEXTURES));
    public static final BlockType TABLE = register(BlockType.builder("table", new WCTableBlock.Factory()));
    public static final BlockType CHAIR = register(BlockType.builder("chair", new WCChairBlock.Factory()));
    public static final BlockType BENCH = register(BlockType.builder("bench", new WCBenchBlock.Factory()));
    public static final BlockType ARROW_SLIT = register(BlockType.builder("arrow-slit", new WCArrowSlitBlock.Factory()));
    public static final BlockType FLOWERBED = register(BlockType.builder("flowerbed", new WCFlowerbedBlock.Factory()));
    public static final BlockType MOUNTED = register(BlockType.builder("mounted", new WCMountedBlock.Factory()));
    public static final BlockType MOUNTED_SLAB = register(BlockType.builder("mounted_slab", new WCMountedSlabBlock.Factory()));
    public static final BlockType AWNING = register(BlockType.builder("awning", new WCAwningBlock.Factory()));
    public static final BlockType BIG_DOOR = register(BlockType.builder("bigdoor", new WCBigDoorBlock.Factory())
            .tag(BIG_DOOR_TAG));
    public static final BlockType TALL_DOOR = register(BlockType.builder("talldoor", new WCTallDoorBlock.Factory()));
    public static final BlockType BUNTING = register(BlockType.builder("bunting", new WCBuntingBlock.Factory())
            .companion(blockName -> blockName + "_ceiling"));
    public static final BlockType BALCONY = register(BlockType.builder("balcony", new WCBalconyBlock.Factory()));
    public static final BlockType MOUNTED_MIRROR = register(BlockType.builder("mounted_mirror", new WCMountedMirrorBlock.Factory()));

    private BlockTypes() {}

    private static BlockType register(BlockType.Builder builder) {
        BlockType type = builder.build();
        if (BY_ID.putIfAbsent(type.id(), type) != null) {
            throw new IllegalStateException("Duplicate block type '" + type.id() + "'");
        }
        return type;
    }

    private static String wallVariantName(String blockName) {
        return "wall_" + blockName;
    }

    private static boolean isClimbable(BlockDefinition definition) {
        return !definition.isNoClimb();
    }

    /** The type with this {@code blockType} id (case-insensitive), or null if there is none. */
    public static BlockType get(String id) {
        return id == null ? null : BY_ID.get(id.toLowerCase(Locale.ROOT));
    }

    /** The type of {@code definition}, or null if its {@code blockType} is unknown. */
    public static BlockType of(BlockDefinition definition) {
        return get(definition.getBlockType());
    }

    public static boolean has(BlockDefinition definition, Trait trait) {
        BlockType type = of(definition);
        return type != null && type.has(trait);
    }

    public static Collection<BlockType> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }
}
