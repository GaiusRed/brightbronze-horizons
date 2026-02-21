package red.gaius.brightbronze.world.dimension.neoforge;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.mixin.MinecraftServerAccessor;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * NeoForge implementation of dynamic dimension creation.
 * 
 * <p>On NeoForge, we use the same mixin-based approach as Fabric since
 * NeoForge doesn't provide a built-in API for runtime dimension creation.
 * The shared mixin accessor in common module works for both loaders.
 */
public class DimensionHelperImpl {

    /**
     * Creates a dynamic dimension at runtime on NeoForge.
     * 
     * <p>Uses the same approach as Fabric - directly construct ServerLevel
     * and add to server's levels map via mixin accessor.
     */
    public static ServerLevel createDynamicDimension(
            MinecraftServer server,
            ResourceKey<Level> dimensionKey,
            ResourceKey<DimensionType> dimensionTypeKey,
            ChunkGenerator generator) {
        
        try {
            // Check if it already exists
            ServerLevel existing = server.getLevel(dimensionKey);
            if (existing != null) {
                return existing;
            }

            // Get the dimension type holder
            var dimensionTypeRegistry = server.registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);
            var dimensionTypeHolder = dimensionTypeRegistry.get(dimensionTypeKey).orElse(null);
            
            if (dimensionTypeHolder == null) {
                BrightbronzeHorizons.LOGGER.error(
                    "Cannot create dimension {}: dimension type {} not found",
                    dimensionKey.location(), dimensionTypeKey.location()
                );
                return null;
            }

            // Get the overworld as a reference for settings
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) {
                BrightbronzeHorizons.LOGGER.error("Cannot create dimension: Overworld not loaded");
                return null;
            }

            // Create derived level data for this dimension
            WorldData worldData = server.getWorldData();
            DerivedLevelData derivedData = new DerivedLevelData(worldData, worldData.overworldData());

            // Create the level stem (dimension configuration)
            LevelStem levelStem = new LevelStem(dimensionTypeHolder, generator);

            // Get the storage access via mixin
            LevelStorageSource.LevelStorageAccess storageAccess = 
                ((MinecraftServerAccessor) server).getStorageSource();

            // Get the executor via mixin
            Executor executor = ((MinecraftServerAccessor) server).getExecutor();

            // Create the random sequences
            RandomSequences randomSequences = new RandomSequences(worldData.worldGenOptions().seed());

            // Create the new ServerLevel
            // Note: MC 1.21.10 removed the ChunkProgressListener parameter from the constructor
            ServerLevel newLevel = new ServerLevel(
                server,
                executor,
                storageAccess,
                derivedData,
                dimensionKey,
                levelStem,
                false,  // isDebug
                overworld.getSeed(),  // Use same seed as overworld
                List.of(),  // No special spawn settings
                false,  // Don't tick time (we'll sync with overworld)
                randomSequences
            );

            // Add to the server's levels map via mixin
            ((MinecraftServerAccessor) server).getLevels().put(dimensionKey, newLevel);

            BrightbronzeHorizons.LOGGER.info(
                "Created dynamic dimension: {}",
                dimensionKey.location()
            );

            return newLevel;

        } catch (Exception e) {
            BrightbronzeHorizons.LOGGER.error(
                "Failed to create dynamic dimension {}: {}",
                dimensionKey.location(), e.getMessage(), e
            );
            return null;
        }
    }

    /**
     * NeoForge supports dynamic dimension creation.
     */
    public static boolean supportsDynamicDimensions() {
        return true;
    }

    /**
     * Unloads a dynamic dimension on NeoForge.
     */
    public static boolean unloadDynamicDimension(
            MinecraftServer server,
            ResourceKey<Level> dimensionKey) {
        
        try {
            ServerLevel level = server.getLevel(dimensionKey);
            if (level == null) {
                return true; // Already unloaded
            }

            // Don't unload vanilla dimensions
            if (dimensionKey == Level.OVERWORLD || 
                dimensionKey == Level.NETHER || 
                dimensionKey == Level.END) {
                BrightbronzeHorizons.LOGGER.warn(
                    "Cannot unload vanilla dimension: {}", dimensionKey.location()
                );
                return false;
            }

            // Save and close the level
            level.save(null, true, false);
            
            // Remove from the server's levels map via mixin
            ((MinecraftServerAccessor) server).getLevels().remove(dimensionKey);

            BrightbronzeHorizons.LOGGER.info(
                "Unloaded dynamic dimension: {}", dimensionKey.location()
            );

            return true;

        } catch (Exception e) {
            BrightbronzeHorizons.LOGGER.error(
                "Failed to unload dimension {}: {}",
                dimensionKey.location(), e.getMessage(), e
            );
            return false;
        }
    }
}
