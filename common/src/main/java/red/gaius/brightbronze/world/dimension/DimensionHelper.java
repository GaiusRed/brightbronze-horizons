package red.gaius.brightbronze.world.dimension;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import red.gaius.brightbronze.BrightbronzeHorizons;

/**
 * Platform abstraction for dynamic dimension creation.
 * 
 * <p>Creating dimensions at runtime in MC 1.21 requires different approaches
 * on Fabric vs NeoForge. This class provides a unified interface using
 * Architectury's ExpectPlatform annotation.
 * 
 * <p>Each platform implementation handles:
 * <ul>
 *   <li>Creating the dimension with the appropriate DimensionType</li>
 *   <li>Registering the ChunkGenerator</li>
 *   <li>Adding the dimension to the server's active levels</li>
 * </ul>
 */
public class DimensionHelper {

    private DimensionHelper() {
        // Utility class
    }

    /**
     * Creates a dynamic dimension at runtime.
     * 
     * <p>This method is implemented differently on each platform:
     * <ul>
     *   <li><b>Fabric:</b> Uses Fantasy library or direct mixin access to MinecraftServer</li>
     *   <li><b>NeoForge:</b> Uses NeoForge's dimension creation events/API</li>
     * </ul>
     * 
     * @param server The Minecraft server
     * @param dimensionKey The resource key for the new dimension
     * @param dimensionTypeKey The dimension type to use (e.g., overworld type)
     * @param generator The chunk generator for this dimension
     * @return The created ServerLevel, or null if creation failed
     */
    @ExpectPlatform
    public static ServerLevel createDynamicDimension(
            MinecraftServer server,
            ResourceKey<Level> dimensionKey,
            ResourceKey<DimensionType> dimensionTypeKey,
            ChunkGenerator generator) {
        throw new AssertionError("Platform implementation not found");
    }

    /**
     * Checks if the platform supports dynamic dimension creation.
     * 
     * @return true if dynamic dimensions can be created at runtime
     */
    @ExpectPlatform
    public static boolean supportsDynamicDimensions() {
        throw new AssertionError("Platform implementation not found");
    }

    /**
     * Unloads a dynamic dimension.
     * Called when the dimension is no longer needed to free resources.
     * 
     * @param server The Minecraft server
     * @param dimensionKey The dimension to unload
     * @return true if unloading was successful
     */
    @ExpectPlatform
    public static boolean unloadDynamicDimension(
            MinecraftServer server,
            ResourceKey<Level> dimensionKey) {
        throw new AssertionError("Platform implementation not found");
    }
}
