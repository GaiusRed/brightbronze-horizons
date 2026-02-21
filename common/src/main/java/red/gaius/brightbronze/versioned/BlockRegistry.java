package red.gaius.brightbronze.versioned;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Version-abstracted block property and registration helpers.
 * 
 * <p>Handles the differences in BlockBehaviour.Properties between Minecraft versions,
 * particularly the setId() requirement in 1.21.10+.
 */
public interface BlockRegistry {
    
    /**
     * Creates a new BlockBehaviour.Properties with the given ID configured.
     * 
     * <p>In 1.21.10: calls {@code BlockBehaviour.Properties.of().setId(key)}
     * <p>In 1.21.1: returns {@code BlockBehaviour.Properties.of()} (no setId)
     * 
     * @param blockKey The resource key for the block
     * @return Configured BlockBehaviour.Properties
     */
    BlockBehaviour.Properties properties(ResourceKey<Block> blockKey);
    
    /**
     * Creates a ResourceKey for a block in the given namespace.
     * 
     * @param modId The mod ID (namespace)
     * @param name The block name
     * @return The resource key
     */
    ResourceKey<Block> key(String modId, String name);
}
