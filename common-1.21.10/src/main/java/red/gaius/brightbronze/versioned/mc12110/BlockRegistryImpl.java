package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import red.gaius.brightbronze.versioned.BlockRegistry;

/**
 * Minecraft 1.21.10 implementation of BlockRegistry.
 * 
 * <p>In 1.21.10, BlockBehaviour.Properties requires setId() to be called before construction.
 */
public class BlockRegistryImpl implements BlockRegistry {
    
    @Override
    public BlockBehaviour.Properties properties(ResourceKey<Block> blockKey) {
        return BlockBehaviour.Properties.of().setId(blockKey);
    }
    
    @Override
    public ResourceKey<Block> key(String modId, String name) {
        return ResourceKey.create(Registries.BLOCK,
                ResourceLocation.fromNamespaceAndPath(modId, name));
    }
}
