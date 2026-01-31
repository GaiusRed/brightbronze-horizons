package red.gaius.brightbronze.world.dimension;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.registry.ModDimensions;
import red.gaius.brightbronze.world.gen.SingleBiomeChunkGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages source dimensions for biome-coherent chunk generation.
 * 
 * <p>Source dimensions are created lazily on demand when a chunk spawner requests
 * terrain from a specific biome. Each biome gets its own dedicated dimension that
 * generates terrain using the overworld generator but forces all chunks to use
 * that single biome.
 * 
 * <p>This ensures that when a player activates a Coal-tier chunk spawner for a plains
 * biome, the spawned chunk will have plains-appropriate terrain: grass, flowers, oak trees, etc.
 * 
 * <p><b>Note:</b> In MC 1.21, dynamic dimension creation at runtime is complex and
 * requires careful handling of registry freezing. This implementation provides
 * a foundation that may need platform-specific enhancements (mixins/access wideners)
 * for full functionality.
 */
public class SourceDimensionManager {

    private static final Map<ResourceLocation, ResourceKey<Level>> activeDimensions = new HashMap<>();

    private SourceDimensionManager() {
        // Utility class
    }

    /**
     * Gets or creates a source dimension for the specified biome.
     * 
     * <p>If the dimension already exists (either from a previous call or from saved data),
     * the existing dimension is returned. Otherwise, a new dimension is dynamically created
     * with a {@link SingleBiomeChunkGenerator} that wraps the overworld generator.
     * 
     * <p><b>Important:</b> In MC 1.21, dynamic dimension creation may not be fully supported
     * without mixins. For now, this method will return the overworld if the source dimension
     * doesn't exist, to allow development to continue.
     * 
     * @param server The Minecraft server
     * @param biomeId The biome's resource location (e.g., "minecraft:plains")
     * @return The server level for this biome's source dimension, or overworld as fallback
     */
    public static ServerLevel getOrCreateSourceDimension(MinecraftServer server, ResourceLocation biomeId) {
        ResourceKey<Level> dimensionKey = ModDimensions.getSourceDimensionKey(biomeId);
        
        // Check if the dimension already exists
        ServerLevel existingLevel = server.getLevel(dimensionKey);
        if (existingLevel != null) {
            activeDimensions.put(biomeId, dimensionKey);
            return existingLevel;
        }

        // In MC 1.21, creating dimensions dynamically at runtime is complex.
        // For now, we log a warning and return the overworld as a fallback.
        // Full implementation would require mixins to unfreeze registries.
        BrightbronzeHorizons.LOGGER.warn(
            "Source dimension for biome {} does not exist. Using overworld as fallback. " +
            "Dynamic dimension creation requires additional platform-specific setup.", 
            biomeId
        );

        // Return overworld as fallback
        return server.getLevel(Level.OVERWORLD);
    }

    /**
     * Creates a SingleBiomeChunkGenerator for the specified biome.
     * This can be used when setting up dimensions through data packs or other means.
     * 
     * @param server The Minecraft server
     * @param biomeId The biome's resource location
     * @return A configured SingleBiomeChunkGenerator, or null if the biome doesn't exist
     */
    public static SingleBiomeChunkGenerator createGeneratorForBiome(
            MinecraftServer server, 
            ResourceLocation biomeId) {
        
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            BrightbronzeHorizons.LOGGER.error("Cannot create generator: Overworld not loaded");
            return null;
        }

        // Get the overworld's chunk generator as our parent
        ChunkGenerator overworldGenerator = overworld.getChunkSource().getGenerator();

        // Resolve the biome
        ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeId);
        Holder<Biome> biomeHolder = server.registryAccess()
                .lookupOrThrow(Registries.BIOME)
                .get(biomeKey)
                .orElse(null);

        if (biomeHolder == null) {
            BrightbronzeHorizons.LOGGER.error("Cannot create generator: Unknown biome {}", biomeId);
            return null;
        }

        // Create the single-biome chunk generator
        return new SingleBiomeChunkGenerator(overworldGenerator, biomeHolder);
    }

    /**
     * Checks if a source dimension for the given biome is currently active.
     * 
     * @param biomeId The biome's resource location
     * @return true if the dimension exists and is loaded
     */
    public static boolean isSourceDimensionActive(ResourceLocation biomeId) {
        return activeDimensions.containsKey(biomeId);
    }

    /**
     * Gets the dimension key for a biome's source dimension without creating it.
     * 
     * @param biomeId The biome's resource location
     * @return The dimension key, or null if not created
     */
    public static ResourceKey<Level> getActiveDimensionKey(ResourceLocation biomeId) {
        return activeDimensions.get(biomeId);
    }

    /**
     * Clears all cached dimension references.
     * Called when the server stops to prevent memory leaks.
     */
    public static void clearCache() {
        activeDimensions.clear();
        BrightbronzeHorizons.LOGGER.debug("Cleared source dimension cache");
    }

    /**
     * Gets the count of active source dimensions.
     * 
     * @return The number of source dimensions currently active
     */
    public static int getActiveDimensionCount() {
        return activeDimensions.size();
    }
}
