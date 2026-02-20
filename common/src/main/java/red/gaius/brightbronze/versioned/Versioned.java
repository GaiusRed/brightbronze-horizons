package red.gaius.brightbronze.versioned;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

/**
 * Central access point for version-specific Minecraft API implementations.
 * 
 * <p>This class provides static access to the current version's implementations.
 * The implementation must be set during mod initialization, before any content
 * is registered.
 * 
 * <p>Usage example:
 * <pre>{@code
 * // In common code:
 * ResourceLocation loc = Versioned.mc().createResourceLocation("mymod", "item");
 * Item.Properties props = Versioned.mc().items().properties(itemKey);
 * }</pre>
 * 
 * <p>Platform entrypoints initialize the correct implementation:
 * <pre>{@code
 * // In fabric-1.21.10 or neoforge-1.21.10:
 * Versioned.init(new McVersion12110());
 * BrightbronzeHorizons.init();
 * }</pre>
 */
public final class Versioned {
    
    private static McVersion mcVersion;
    
    private Versioned() {
    }
    
    /**
     * Initializes the version-specific implementation.
     * Must be called exactly once, before any mod content is registered.
     * 
     * @param version The McVersion implementation for this Minecraft version
     * @throws IllegalStateException if already initialized
     */
    @ApiStatus.Internal
    public static void init(McVersion version) {
        if (mcVersion != null) {
            throw new IllegalStateException("Versioned already initialized");
        }
        mcVersion = version;
    }
    
    /**
     * Returns the current Minecraft version's API implementation.
     * 
     * @return The McVersion instance
     * @throws IllegalStateException if not yet initialized
     */
    public static McVersion mc() {
        if (mcVersion == null) {
            throw new IllegalStateException("Versioned not initialized. " +
                    "Platform entrypoint must call Versioned.init() before BrightbronzeHorizons.init()");
        }
        return mcVersion;
    }
    
    // ===== Convenience methods for common operations =====
    
    /**
     * Creates a ResourceLocation from namespace and path.
     * Convenience method for {@code mc().createResourceLocation(namespace, path)}.
     */
    public static ResourceLocation resourceLocation(String namespace, String path) {
        return mc().createResourceLocation(namespace, path);
    }
    
    /**
     * Parses a ResourceLocation from a string.
     * Convenience method for {@code mc().parseResourceLocation(location)}.
     */
    public static ResourceLocation parseResourceLocation(String location) {
        return mc().parseResourceLocation(location);
    }
    
    /**
     * Returns the item registry helper.
     * Convenience method for {@code mc().items()}.
     */
    public static ItemRegistry items() {
        return mc().items();
    }
    
    /**
     * Returns the block registry helper.
     * Convenience method for {@code mc().blocks()}.
     */
    public static BlockRegistry blocks() {
        return mc().blocks();
    }
    
    /**
     * Returns the tool factory.
     * Convenience method for {@code mc().tools()}.
     */
    public static ToolFactory tools() {
        return mc().tools();
    }
    
    /**
     * Returns the armor factory.
     * Convenience method for {@code mc().armor()}.
     */
    public static ArmorFactory armor() {
        return mc().armor();
    }
    
    /**
     * Returns the chunk sync helper.
     * Convenience method for {@code mc().chunkSync()}.
     */
    public static ChunkSyncHelper chunkSync() {
        return mc().chunkSync();
    }
    
    /**
     * Returns the entity copy helper.
     * Convenience method for {@code mc().entityCopy()}.
     */
    public static EntityCopyHelper entityCopy() {
        return mc().entityCopy();
    }
    
    /**
     * Returns the mob spawn helper.
     * Convenience method for {@code mc().mobSpawn()}.
     */
    public static MobSpawnHelper mobSpawn() {
        return mc().mobSpawn();
    }
    
    /**
     * Returns the interaction result helper.
     * Convenience method for {@code mc().interaction()}.
     */
    public static InteractionResultHelper interaction() {
        return mc().interaction();
    }
    
    /**
     * Returns the registry helper.
     * Convenience method for {@code mc().registry()}.
     */
    public static RegistryHelper registry() {
        return mc().registry();
    }
    
    /**
     * Returns the saved data helper.
     * Convenience method for {@code mc().savedData()}.
     */
    public static SavedDataHelper savedData() {
        return mc().savedData();
    }
    
    /**
     * Returns the world generation helper.
     * Convenience method for {@code mc().worldGen()}.
     */
    public static WorldGenHelper worldGen() {
        return mc().worldGen();
    }
    
    /**
     * Returns the spawn helper.
     * Convenience method for {@code mc().spawn()}.
     */
    public static SpawnHelper spawn() {
        return mc().spawn();
    }
    
    /**
     * Returns the level helper.
     * Convenience method for {@code mc().level()}.
     */
    public static LevelHelper level() {
        return mc().level();
    }
    
    /**
     * Returns the chunk helper.
     * Convenience method for {@code mc().chunk()}.
     */
    public static ChunkHelper chunk() {
        return mc().chunk();
    }
    
    /**
     * Returns the sound helper.
     * Convenience method for {@code mc().sound()}.
     */
    public static SoundHelper sound() {
        return mc().sound();
    }
}
