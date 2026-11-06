package com.westerosblocks.block;

import com.westerosblocks.block.custom.BlockFactory;
import com.westerosblocks.data.BlockDefinition;
import net.minecraft.block.Block;
import net.minecraft.registry.tag.TagKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Everything the mod knows about one {@code blockType} value from the definition JSON: how to
 * build the block, which tags it belongs to, which companion block it registers, and how
 * exporters classify it. All types are declared in {@link BlockTypes}; model generation is
 * keyed by these same objects in {@code ModModelProvider}.
 */
public final class BlockType {

    /** Behaviour flags that other modules switch on instead of comparing type strings. */
    public enum Trait {
        /** Easily replaced by WorldPainter: no real body (plants, torches, fire, ...). */
        INSUBSTANTIAL,
        /** Removed by WorldPainter's "remove vegetation" merge. */
        VEGETATION,
        /** Removed by WorldPainter's "remove trees" merge. */
        TREE_RELATED,
        /** Has no block textures of its own, so the loader skips the texture check. */
        NO_BLOCK_TEXTURES
    }

    /** Which block a {@link TagRule} tags: the definition's own block, or its companion. */
    public enum TagTarget { BLOCK, COMPANION }

    /** Puts the block (or its companion) in {@code tag} when {@code when} holds for its definition. */
    public record TagRule(TagKey<Block> tag, TagTarget target, Predicate<BlockDefinition> when) {}

    private final String id;
    private final BlockFactory factory;
    private final UnaryOperator<String> companionName;
    private final List<TagRule> tagRules;
    private final Set<Trait> traits;

    private BlockType(Builder builder) {
        this.id = builder.id;
        this.factory = builder.factory;
        this.companionName = builder.companionName;
        this.tagRules = List.copyOf(builder.tagRules);
        this.traits = builder.traits.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(builder.traits));
    }

    static Builder builder(String id, BlockFactory factory) {
        return new Builder(id, factory);
    }

    /** The {@code blockType} string used in the definition JSON. */
    public String id() {
        return id;
    }

    /** Builds the block. A factory may also register the type's companion block. */
    public Block create(BlockDefinition definition) {
        return factory.buildBlockClass(definition);
    }

    /**
     * Registry name of the item-less block this type registers next to {@code blockName}
     * (the wall variant of a torch, the ceiling variant of a bunting), or null if it has none.
     */
    public String companionName(String blockName) {
        return companionName == null ? null : companionName.apply(blockName);
    }

    public List<TagRule> tagRules() {
        return tagRules;
    }

    public boolean has(Trait trait) {
        return traits.contains(trait);
    }

    @Override
    public String toString() {
        return id;
    }

    static final class Builder {
        private final String id;
        private final BlockFactory factory;
        private UnaryOperator<String> companionName;
        private final List<TagRule> tagRules = new ArrayList<>();
        private final Set<Trait> traits = EnumSet.noneOf(Trait.class);

        private Builder(String id, BlockFactory factory) {
            this.id = id;
            this.factory = factory;
        }

        Builder companion(UnaryOperator<String> nameFromBlockName) {
            this.companionName = nameFromBlockName;
            return this;
        }

        Builder tag(TagKey<Block> tag) {
            return tag(tag, TagTarget.BLOCK, definition -> true);
        }

        Builder tag(TagKey<Block> tag, Predicate<BlockDefinition> when) {
            return tag(tag, TagTarget.BLOCK, when);
        }

        Builder companionTag(TagKey<Block> tag) {
            return tag(tag, TagTarget.COMPANION, definition -> true);
        }

        private Builder tag(TagKey<Block> tag, TagTarget target, Predicate<BlockDefinition> when) {
            tagRules.add(new TagRule(tag, target, when));
            return this;
        }

        Builder traits(Trait... traits) {
            this.traits.addAll(List.of(traits));
            return this;
        }

        BlockType build() {
            return new BlockType(this);
        }
    }
}
