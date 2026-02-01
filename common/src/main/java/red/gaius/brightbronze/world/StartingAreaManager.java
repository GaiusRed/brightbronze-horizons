package red.gaius.brightbronze.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.world.chunk.ChunkCopyService;
import red.gaius.brightbronze.world.dimension.SourceDimensionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages the initial starting area setup for Brightbronze Horizons worlds.
 * 
 * <p>The starting area is a 3×3 chunk region (9 chunks total) that:
 * <ul>
 *   <li>Is centered on the world spawn point</li>
 *   <li>Prefers Plains biome (configurable)</li>
 *   <li>Attempts to include a Village structure (best effort)</li>
 *   <li>Gracefully degrades if ideal conditions cannot be met</li>
 * </ul>
 * 
 * <p>This manager is called during world initialization to set up the
 * playable area before the player spawns.
 */
public class StartingAreaManager {

    /** The default starting biome (Plains) */
    public static final ResourceLocation DEFAULT_STARTING_BIOME = Biomes.PLAINS.location();
    
    /** Size of the starting area in chunks (3×3 = 9 chunks) */
    public static final int STARTING_AREA_SIZE = 3;
    
    /** Half-size for offset calculations (1 chunk on each side of center) */
    private static final int HALF_SIZE = STARTING_AREA_SIZE / 2;

    /** Initial radius (in chunks) to search for a village near the spawn chunk. */
    private static final int VILLAGE_SEARCH_RADIUS_CHUNKS = 64;

    /** Fallback radius (in chunks) if no village is found in the initial search. */
    private static final int VILLAGE_FALLBACK_SEARCH_RADIUS_CHUNKS = 128;

    /**
     * Initializes the starting area for a new world.
     * 
     * <p>This should be called once during world creation to set up the initial
     * 3×3 playable area. The method:
     * <ol>
     *   <li>Determines the spawn chunk from world spawn position</li>
     *   <li>Attempts to find a good starting location (Plains + Village)</li>
     *   <li>Copies the 3×3 chunk area from the source dimension</li>
     *   <li>Registers all chunks with PlayableAreaData</li>
     * </ol>
     * 
     * @param server The Minecraft server
     * @return true if initialization succeeded
     */
    public static boolean initializeStartingArea(MinecraftServer server) {
        PlayableAreaData playableData = PlayableAreaData.get(server);
        
        // Don't re-initialize if already done
        if (playableData.isInitialized()) {
            BrightbronzeHorizons.LOGGER.debug("Starting area already initialized");
            return true;
        }

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            BrightbronzeHorizons.LOGGER.error("Cannot initialize starting area: Overworld not loaded");
            return false;
        }

        // Get the world spawn position from level data
        // MC 1.21 uses RespawnData for spawn position
        LevelData.RespawnData respawnData = overworld.getLevelData().getRespawnData();
        BlockPos spawnPos = respawnData.pos();
        ChunkPos requestedCenterChunk = new ChunkPos(spawnPos);
        
        BrightbronzeHorizons.LOGGER.info(
            "Initializing starting area at chunk ({}, {}) near spawn ({}, {}, {})",
            requestedCenterChunk.x, requestedCenterChunk.z, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ()
        );

        // Get the starting biome
        ResourceLocation startingBiome = findStartingBiome(overworld, requestedCenterChunk);

        Registry<Biome> biomeRegistry = overworld.registryAccess().lookupOrThrow(Registries.BIOME);
        Optional<Holder.Reference<Biome>> startingBiomeHolderOpt = biomeRegistry.get(startingBiome);
        if (startingBiomeHolderOpt.isEmpty()) {
            BrightbronzeHorizons.LOGGER.error("Cannot initialize starting area: unknown biome {}", startingBiome);
            return false;
        }
        Holder<Biome> startingBiomeHolder = startingBiomeHolderOpt.get();
        
        BrightbronzeHorizons.LOGGER.info("Using starting biome: {}", startingBiome);

        // Get or create the source dimension for this biome
        ServerLevel sourceLevel = SourceDimensionManager.getOrCreateSourceDimension(
            server,
            startingBiome
        );

        // Attempt to anchor the start around a Village (best effort).
        // This does NOT change the "coordinate matching" rule for chunk sourcing:
        // we still copy chunk (x,z) from the Plains source dimension into overworld chunk (x,z).
        // Choosing a different center chunk only decides *which* absolute chunk coordinates
        // we reveal first.
        ChunkPos centerChunk = chooseStartingCenterChunk(overworld, sourceLevel, requestedCenterChunk);

        // Copy the 3×3 chunk area
        List<ChunkPos> startingChunks = getStartingChunks(centerChunk);
        
        boolean allSuccess = true;
        int totalBlocksCopied = 0;
        
        for (ChunkPos chunkPos : startingChunks) {
            boolean success = ChunkCopyService.copyChunk(
                sourceLevel,
                chunkPos,  // Source coords = target coords (per PRD)
                overworld,
                chunkPos,
                startingBiomeHolder
            );
            
            if (success) {
                playableData.addChunk(chunkPos);
            } else {
                BrightbronzeHorizons.LOGGER.warn(
                    "Failed to copy starting chunk ({}, {})",
                    chunkPos.x, chunkPos.z
                );
                allSuccess = false;
            }
        }
        
        // Force save the world to ensure chunk modifications are persisted
        BrightbronzeHorizons.LOGGER.info("Saving modified chunks...");
        overworld.save(null, true, false);
        
        // Force unload and reload all starting chunks to ensure fresh state
        // This is necessary because ChunkHolder caches the original void chunk state
        BrightbronzeHorizons.LOGGER.info("Refreshing chunk cache...");
        for (ChunkPos chunkPos : startingChunks) {
            // Unforce load, let it naturally unload  
            overworld.setChunkForced(chunkPos.x, chunkPos.z, false);
        }
        
        // Force load them again - this will load the saved version from disk
        for (ChunkPos chunkPos : startingChunks) {
            overworld.setChunkForced(chunkPos.x, chunkPos.z, true);
            // Get the chunk to ensure it's loaded
            overworld.getChunk(chunkPos.x, chunkPos.z);
        }

        // Mark as initialized even if some chunks failed
        // (graceful degradation - player can still play)
        playableData.setInitialized(centerChunk);
        
        BrightbronzeHorizons.LOGGER.info(
            "Starting area initialized: {} of {} chunks copied successfully",
            playableData.getChunkCount(), startingChunks.size()
        );

        // Attempt to adjust spawn point to a safe location
        adjustSpawnPoint(overworld, centerChunk);

        return allSuccess;
    }

    /**
     * Chooses the center chunk for the starting 3×3 area.
     *
     * <p>Priority:
     * <ol>
     *   <li>If a village can be located in the source dimension within a bounded radius, center on its chunk.</li>
     *   <li>Otherwise, fall back to the requested spawn-centered chunk.</li>
     * </ol>
     */
    private static ChunkPos chooseStartingCenterChunk(ServerLevel overworld, ServerLevel sourceLevel, ChunkPos requestedCenterChunk) {
        Optional<BlockPos> villagePos = findNearestVillage(sourceLevel, requestedCenterChunk);

        if (villagePos.isPresent()) {
            ChunkPos villageChunk = new ChunkPos(villagePos.get());
            BrightbronzeHorizons.LOGGER.info(
                "Found village for starting area at {} in source dimension; using center chunk ({}, {})",
                villagePos.get(), villageChunk.x, villageChunk.z
            );
            return villageChunk;
        }

        BrightbronzeHorizons.LOGGER.warn(
            "No village found within {} chunks of spawn; using spawn-centered start at chunk ({}, {})",
            VILLAGE_FALLBACK_SEARCH_RADIUS_CHUNKS, requestedCenterChunk.x, requestedCenterChunk.z
        );
        return requestedCenterChunk;
    }

    /**
     * Locates the nearest village in the given source dimension near the provided chunk.
     *
     * <p>This uses the built-in structure location logic (same underlying mechanism as /locate).
     * It is deterministic for a given seed + worldgen settings.
     */
    private static Optional<BlockPos> findNearestVillage(ServerLevel sourceLevel, ChunkPos nearChunk) {
        // Y level is irrelevant for locating structures; use sea level for readability.
        BlockPos origin = new BlockPos(nearChunk.getMiddleBlockX(), sourceLevel.getSeaLevel(), nearChunk.getMiddleBlockZ());

        BlockPos found = sourceLevel.findNearestMapStructure(
            StructureTags.VILLAGE,
            origin,
            VILLAGE_SEARCH_RADIUS_CHUNKS,
            false
        );

        if (found == null) {
            found = sourceLevel.findNearestMapStructure(
                StructureTags.VILLAGE,
                origin,
                VILLAGE_FALLBACK_SEARCH_RADIUS_CHUNKS,
                false
            );
        }

        return Optional.ofNullable(found);
    }

    /**
     * Gets the list of chunk positions for the starting 3×3 area.
     * 
     * @param centerChunk The center chunk
     * @return List of 9 chunk positions
     */
    public static List<ChunkPos> getStartingChunks(ChunkPos centerChunk) {
        List<ChunkPos> chunks = new ArrayList<>(STARTING_AREA_SIZE * STARTING_AREA_SIZE);
        
        for (int dx = -HALF_SIZE; dx <= HALF_SIZE; dx++) {
            for (int dz = -HALF_SIZE; dz <= HALF_SIZE; dz++) {
                chunks.add(new ChunkPos(centerChunk.x + dx, centerChunk.z + dz));
            }
        }
        
        return chunks;
    }

    /**
     * Finds the best starting biome for the given location.
     * 
     * <p>Priority:
     * <ol>
     *   <li>Plains (preferred)</li>
     *   <li>Any non-ocean, non-mountain biome at spawn</li>
     *   <li>Default to Plains regardless</li>
     * </ol>
     * 
     * @param level The server level
     * @param centerChunk The center chunk
     * @return The biome ID to use for starting area
     */
    private static ResourceLocation findStartingBiome(ServerLevel level, ChunkPos centerChunk) {
        // For now, always use Plains for consistent starting experience
        // Future enhancement: could search nearby for Plains if spawn isn't in Plains
        
        // Check what biome is actually at spawn
        BlockPos centerPos = centerChunk.getMiddleBlockPosition(64);
        Holder<Biome> actualBiome = level.getBiome(centerPos);
        
        Optional<ResourceKey<Biome>> biomeKey = actualBiome.unwrapKey();
        if (biomeKey.isPresent()) {
            ResourceLocation actualId = biomeKey.get().location();
            
            // If spawn is already in Plains, use Plains
            if (actualId.equals(Biomes.PLAINS.location())) {
                return DEFAULT_STARTING_BIOME;
            }
            
            // Check if it's a "safe" biome for starting
            // (not ocean, not mountain peaks, not deep dark, etc.)
            if (isSafeStartingBiome(actualBiome)) {
                BrightbronzeHorizons.LOGGER.debug(
                    "Spawn biome {} is safe, but using Plains for consistency",
                    actualId
                );
            }
        }
        
        // Default to Plains for consistent experience
        return DEFAULT_STARTING_BIOME;
    }

    /**
     * Checks if a biome is safe for starting (not dangerous or inaccessible).
     */
    private static boolean isSafeStartingBiome(Holder<Biome> biome) {
        // Unsafe biomes: oceans, deep dark, frozen ocean, etc.
        if (biome.is(BiomeTags.IS_OCEAN)) return false;
        if (biome.is(BiomeTags.IS_DEEP_OCEAN)) return false;
        
        // Check by ID for specific problematic biomes
        Optional<ResourceKey<Biome>> key = biome.unwrapKey();
        if (key.isPresent()) {
            ResourceLocation id = key.get().location();
            String path = id.getPath();
            
            // Avoid these biomes for starting
            if (path.contains("deep_dark")) return false;
            if (path.contains("frozen_ocean")) return false;
            if (path.contains("cold_ocean")) return false;
            if (path.equals("the_void")) return false;
        }
        
        return true;
    }

    /**
     * Adjusts the world spawn point to a safe location within the starting area.
     * 
     * @param level The server level
     * @param centerChunk The center chunk of the starting area
     */
    private static void adjustSpawnPoint(ServerLevel level, ChunkPos centerChunk) {
        // Get the center of the center chunk
        int centerX = centerChunk.getMiddleBlockX();
        int centerZ = centerChunk.getMiddleBlockZ();
        
        // Find a safe Y position
        int safeY = findSafeSpawnY(level, centerX, centerZ);
        
        if (safeY > level.getMinY()) {
            BlockPos newSpawn = new BlockPos(centerX, safeY, centerZ);
            
            // MC 1.21 uses RespawnData for spawn position
            // Get current spawn angle from existing respawn data
            LevelData.RespawnData currentRespawn = level.getLevelData().getRespawnData();
            LevelData.RespawnData newRespawn = LevelData.RespawnData.of(
                Level.OVERWORLD,
                newSpawn,
                currentRespawn.yaw(),
                currentRespawn.pitch()
            );
            
            ServerLevelData levelData = (ServerLevelData) level.getLevelData();
            levelData.setSpawn(newRespawn);
            
            BrightbronzeHorizons.LOGGER.info(
                "Set spawn point to ({}, {}, {})",
                centerX, safeY, centerZ
            );
        }
    }

    /**
     * Finds a safe Y level for spawning at the given XZ coordinates.
     * 
     * @param level The server level
     * @param x X coordinate
     * @param z Z coordinate
     * @return Safe Y level, or minY if none found
     */
    private static int findSafeSpawnY(ServerLevel level, int x, int z) {
        // Start from a reasonable height and work down
        int startY = level.getSeaLevel() + 64;
        
        for (int y = startY; y > level.getMinY(); y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockPos below = pos.below();
            BlockPos above = pos.above();
            
            // Need: solid ground below, air at feet and head level
            if (level.getBlockState(below).isSolid() &&
                level.getBlockState(pos).isAir() &&
                level.getBlockState(above).isAir()) {
                return y;
            }
        }
        
        // Fallback to sea level
        return level.getSeaLevel() + 1;
    }

    /**
     * Checks if the starting area should be initialized.
     * Called during server tick to handle lazy initialization.
     * 
     * @param server The Minecraft server
     * @return true if initialization was triggered or already done
     */
    public static boolean checkAndInitialize(MinecraftServer server) {
        PlayableAreaData playableData = PlayableAreaData.get(server);
        
        if (!playableData.isInitialized()) {
            return initializeStartingArea(server);
        }
        
        return true;
    }
}
