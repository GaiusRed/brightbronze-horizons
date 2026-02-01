package red.gaius.brightbronze.world.dimension;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.registry.ModDimensions;

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
     * with a {@link NoiseBasedChunkGenerator} using overworld noise settings and a fixed biome source.
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

        // Check if platform supports dynamic dimensions
        if (!DimensionHelper.supportsDynamicDimensions()) {
            BrightbronzeHorizons.LOGGER.warn(
                "Platform does not support dynamic dimension creation. Using overworld as fallback for biome {}.",
                biomeId
            );
            return server.getLevel(Level.OVERWORLD);
        }

        // Create the chunk generator for this biome
        ChunkGenerator generator = createGeneratorForBiome(server, biomeId);
        if (generator == null) {
            BrightbronzeHorizons.LOGGER.error(
                "Failed to create chunk generator for biome {}. Using overworld as fallback.",
                biomeId
            );
            return server.getLevel(Level.OVERWORLD);
        }

        // Create the dynamic dimension
        BrightbronzeHorizons.LOGGER.info("Creating source dimension for biome: {}", biomeId);
        
        ServerLevel newLevel = DimensionHelper.createDynamicDimension(
            server,
            dimensionKey,
            BuiltinDimensionTypes.OVERWORLD,  // Use overworld dimension type
            generator
        );

        if (newLevel != null) {
            activeDimensions.put(biomeId, dimensionKey);
            BrightbronzeHorizons.LOGGER.info(
                "Successfully created source dimension for biome: {}",
                biomeId
            );
            return newLevel;
        }

        // Fall back to overworld if creation failed
        BrightbronzeHorizons.LOGGER.warn(
            "Failed to create source dimension for biome {}. Using overworld as fallback.",
            biomeId
        );
        return server.getLevel(Level.OVERWORLD);
    }

    /**
     * Creates a NoiseBasedChunkGenerator for the specified biome.
     * This creates a proper terrain generator (not void!) that forces all chunks to use
     * a single biome.
     * 
     * @param server The Minecraft server
     * @param biomeId The biome's resource location
     * @return A configured ChunkGenerator, or null if setup fails
     */
    public static ChunkGenerator createGeneratorForBiome(
            MinecraftServer server, 
            ResourceLocation biomeId) {
        
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

        // Pick the correct base terrain generator settings for this biome.
        // Without this, End/Nether biomes would still generate overworld-like terrain (e.g., grass).
        ResourceKey<NoiseGeneratorSettings> noiseKey = NoiseGeneratorSettings.OVERWORLD;
        if (biomeHolder.is(BiomeTags.IS_NETHER)) {
            noiseKey = NoiseGeneratorSettings.NETHER;
        } else if (biomeHolder.is(BiomeTags.IS_END)) {
            noiseKey = NoiseGeneratorSettings.END;
        }

        Holder<NoiseGeneratorSettings> noiseSettings = server.registryAccess()
                .lookupOrThrow(Registries.NOISE_SETTINGS)
                .getOrThrow(noiseKey);

        // Create a fixed biome source that always returns our target biome
        FixedBiomeSource biomeSource = new FixedBiomeSource(biomeHolder);

        // Create the noise-based chunk generator with overworld settings
        // This will generate proper terrain (stone, dirt, grass, caves, ores, etc.)
        // but use only our specified biome for surface, features, and mob spawns
        NoiseBasedChunkGenerator generator = new NoiseBasedChunkGenerator(biomeSource, noiseSettings);
        
        BrightbronzeHorizons.LOGGER.debug(
            "Created NoiseBasedChunkGenerator for biome {} with noise settings {}",
            biomeId, noiseKey.location()
        );
        
        return generator;
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
