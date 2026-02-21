package red.gaius.brightbronze.versioned;

import net.minecraft.resources.ResourceLocation;

/**
 * Provides version-specific Minecraft API implementations.
 * 
 * <p>This interface abstracts APIs that differ between Minecraft versions.
 * Implementations are provided in version-specific subprojects (e.g., common-1.21.10).
 * 
 * <p>The implementation is initialized by platform entrypoints before any mod
 * content is registered.
 */
public interface McVersion {
    
    /**
     * Creates a ResourceLocation from namespace and path.
     * 
     * <p>In 1.21.10: {@code ResourceLocation.fromNamespaceAndPath(ns, path)}
     * <p>In 1.21.1: {@code new ResourceLocation(ns, path)}
     */
    ResourceLocation createResourceLocation(String namespace, String path);
    
    /**
     * Parses a ResourceLocation from a string (namespace:path format).
     * 
     * <p>In 1.21.10: {@code ResourceLocation.parse(string)}
     * <p>In 1.21.1: {@code new ResourceLocation(string)}
     */
    ResourceLocation parseResourceLocation(String location);
    
    /**
     * Returns the item registry helper for this Minecraft version.
     */
    ItemRegistry items();
    
    /**
     * Returns the block registry helper for this Minecraft version.
     */
    BlockRegistry blocks();
    
    /**
     * Returns the tool factory for this Minecraft version.
     */
    ToolFactory tools();
    
    /**
     * Returns the armor factory for this Minecraft version.
     */
    ArmorFactory armor();
    
    /**
     * Returns the chunk sync helper for this Minecraft version.
     */
    ChunkSyncHelper chunkSync();
    
    /**
     * Returns the entity copy helper for this Minecraft version.
     */
    EntityCopyHelper entityCopy();
    
    /**
     * Returns the mob spawn helper for this Minecraft version.
     */
    MobSpawnHelper mobSpawn();
    
    /**
     * Returns the interaction result helper for this Minecraft version.
     */
    InteractionResultHelper interaction();
    
    /**
     * Returns the registry helper for this Minecraft version.
     */
    RegistryHelper registry();
    
    /**
     * Returns the saved data helper for this Minecraft version.
     */
    SavedDataHelper savedData();
    
    /**
     * Returns the world generation helper for this Minecraft version.
     */
    WorldGenHelper worldGen();
    
    /**
     * Returns the spawn helper for this Minecraft version.
     */
    SpawnHelper spawn();
    
    /**
     * Returns the level helper for this Minecraft version.
     */
    LevelHelper level();
    
    /**
     * Returns the chunk helper for this Minecraft version.
     */
    ChunkHelper chunk();
    
    /**
     * Returns the sound helper for this Minecraft version.
     */
    SoundHelper sound();
}
