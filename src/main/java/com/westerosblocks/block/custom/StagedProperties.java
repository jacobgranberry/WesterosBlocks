package com.westerosblocks.block.custom;

import com.westerosblocks.utils.ModProperties;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * Hands definition-driven block-state properties to {@link Block#appendProperties}.
 *
 * <p>Minecraft calls {@code appendProperties} from inside the {@link Block} constructor, before
 * the subclass constructor body runs, so a block cannot read its own fields there. Which
 * properties a WesterosBlocks block has depends on its {@code BlockDefinition}, so a factory
 * stages them here and constructs the block inside {@link #construct}:
 *
 * <pre>{@code
 * return StagedProperties.builder()
 *         .addIf(doUnconnect, UNCONNECT)
 *         .state(definition.buildStateProperty())
 *         .construct(() -> new WCFooBlock(settings, definition, ...));
 * }</pre>
 *
 * and the block's {@code appendProperties} calls {@link #appendTo} once, keeping the returned
 * object to read back the {@code state} property.
 *
 * <p>Staging is thread-local and scoped to the {@code construct} call, so nothing staged for one
 * block can leak into the next one, even if a constructor throws. This replaces the static
 * {@code tempSTATE}/{@code tempUNCONNECT}/... fields carried over from the 1.18.2 Forge code.
 */
public final class StagedProperties {
    private static final ThreadLocal<StagedProperties> PENDING = new ThreadLocal<>();

    /** Returned by {@link #appendTo} when a block is constructed outside {@link #construct}. */
    public static final StagedProperties NONE = new StagedProperties(Collections.emptyList(), null);

    private final List<Property<?>> properties;
    private final ModProperties.StateProperty state;

    private StagedProperties(List<Property<?>> properties, ModProperties.StateProperty state) {
        this.properties = properties;
        this.state = state;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Adds the staged properties, in the order they were staged, to {@code builder}. Call it
     * exactly once per block, from the {@code appendProperties} of the class that owns the
     * properties. Returns {@link #NONE} when nothing is staged.
     */
    public static StagedProperties appendTo(StateManager.Builder<Block, BlockState> builder) {
        StagedProperties staged = PENDING.get();
        if (staged == null) {
            return NONE;
        }
        // Consume it, so a second call in the same construction cannot add properties twice.
        PENDING.remove();
        for (Property<?> property : staged.properties) {
            builder.add(property);
        }
        return staged;
    }

    /** The definition's {@code state} property, or null when the definition has no states. */
    public ModProperties.StateProperty state() {
        return state;
    }

    /** True when {@code property} was staged for this block. */
    public boolean has(Property<?> property) {
        return properties.contains(property);
    }

    public static final class Builder {
        private final List<Property<?>> properties = new ArrayList<>();
        private ModProperties.StateProperty state;

        private Builder() {}

        /** Stages {@code property}. Properties are added to the block in staging order. */
        public Builder add(Property<?> property) {
            if (property != null) {
                properties.add(property);
            }
            return this;
        }

        public Builder addIf(boolean condition, Property<?> property) {
            return condition ? add(property) : this;
        }

        /** Stages the definition's {@code state} property; null means none. */
        public Builder state(ModProperties.StateProperty stateProperty) {
            if (stateProperty != null) {
                this.state = stateProperty;
                properties.add(stateProperty);
            }
            return this;
        }

        /** Runs {@code constructor} with these properties staged for its {@code appendProperties}. */
        public <B extends Block> B construct(Supplier<B> constructor) {
            StagedProperties previous = PENDING.get();
            PENDING.set(new StagedProperties(List.copyOf(properties), state));
            try {
                return constructor.get();
            } finally {
                if (previous == null) {
                    PENDING.remove();
                } else {
                    PENDING.set(previous);
                }
            }
        }
    }
}
