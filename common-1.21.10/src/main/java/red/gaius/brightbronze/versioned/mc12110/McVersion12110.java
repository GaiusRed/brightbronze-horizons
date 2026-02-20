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
    private final EntityCopyHelper entityCopyHelper = new EntityCopyHelperImpl();
    private final MobSpawnHelper mobSpawnHelper = new MobSpawnHelperImpl();
    private final InteractionResultHelper interactionResultHelper = new InteractionResultHelperImpl();
    private final RegistryHelper registryHelper = new RegistryHelperImpl();
    private final SavedDataHelper savedDataHelper = new SavedDataHelperImpl();
    private final WorldGenHelper worldGenHelper = new WorldGenHelperImpl();
    private final SpawnHelper spawnHelper = new SpawnHelperImpl();
    private final LevelHelper levelHelper = new LevelHelperImpl();
    private final ChunkHelper chunkHelper = new ChunkHelperImpl();
    private final SoundHelper soundHelper = new SoundHelperImpl();
    
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
    
    @Override
    public EntityCopyHelper entityCopy() {
        return entityCopyHelper;
    }
    
    @Override
    public MobSpawnHelper mobSpawn() {
        return mobSpawnHelper;
    }
    
    @Override
    public InteractionResultHelper interaction() {
        return interactionResultHelper;
    }
    
    @Override
    public RegistryHelper registry() {
        return registryHelper;
    }
    
    @Override
    public SavedDataHelper savedData() {
        return savedDataHelper;
    }
    
    @Override
    public WorldGenHelper worldGen() {
        return worldGenHelper;
    }
    
    @Override
    public SpawnHelper spawn() {
        return spawnHelper;
    }
    
    @Override
    public LevelHelper level() {
        return levelHelper;
    }
    
    @Override
    public ChunkHelper chunk() {
        return chunkHelper;
    }
    
    @Override
    public SoundHelper sound() {
        return soundHelper;
    }
}
