package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import red.gaius.brightbronze.versioned.BlockRegistry;

/**
 * Minecraft 1.21.1 implementation of BlockRegistry.
 * 
 * <p>In 1.21.1, BlockBehaviour.Properties does NOT have setId() - the ID is set
 * during registration, not during property creation.
 */
public class BlockRegistryImpl implements BlockRegistry {
    
    @Override
    public BlockBehaviour.Properties properties(ResourceKey<Block> blockKey) {
        // In 1.21.1, we don't need to set the ID on properties
        return BlockBehaviour.Properties.of();
    }
    
    @Override
    public ResourceKey<Block> key(String modId, String name) {
        return ResourceKey.create(Registries.BLOCK,
                ResourceLocation.parse(modId + ":" + name));
    }
}
