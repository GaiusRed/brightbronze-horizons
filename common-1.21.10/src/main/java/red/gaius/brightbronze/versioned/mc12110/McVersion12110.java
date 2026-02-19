package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.resources.ResourceLocation;
import red.gaius.brightbronze.versioned.*;

/**
 * Minecraft 1.21.10 implementation of McVersion.
 * 
 * <p>This class aggregates all version-specific implementations for MC 1.21.10.
 */
public class McVersion12110 implements McVersion {
    
    private final ItemRegistry itemRegistry = new ItemRegistryImpl();
    private final BlockRegistry blockRegistry = new BlockRegistryImpl();
    private final ToolFactory toolFactory = new ToolFactoryImpl();
    private final ArmorFactory armorFactory = new ArmorFactoryImpl();
    private final ChunkSyncHelper chunkSyncHelper = new ChunkSyncHelperImpl();
    
    @Override
    public ResourceLocation createResourceLocation(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
    
    @Override
    public ResourceLocation parseResourceLocation(String location) {
        return ResourceLocation.parse(location);
    }
    
    @Override
    public ItemRegistry items() {
        return itemRegistry;
    }
    
    @Override
    public BlockRegistry blocks() {
        return blockRegistry;
    }
    
    @Override
    public ToolFactory tools() {
        return toolFactory;
    }
    
    @Override
    public ArmorFactory armor() {
        return armorFactory;
    }
    
    @Override
    public ChunkSyncHelper chunkSync() {
        return chunkSyncHelper;
    }
}
