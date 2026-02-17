package red.gaius.brightbronze.world.chunk;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.config.BrightbronzeConfig;
import red.gaius.brightbronze.world.PlayableAreaData;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Service for detecting structures in chunks and calculating all chunks needed
 * to complete those structures.
 * 
 * <p>This implements cascading structure detection: when a chunk is spawned,
 * any structures in that chunk are fully completed by spawning all chunks
 * they span. Those additional chunks are also scanned for structures, which
 * are also completed, up to a configurable limit.
 * 
 * <p>The cascade is controlled by two limits:
 * <ul>
 *   <li>{@code maxStructureCompletionStructures} - max structures to complete (default: 16)</li>
 *   <li>{@code maxStructureCompletionChunks} - hard cap on total chunks (default: 256)</li>
 * </ul>
 */
public final class StructureCompletionService {

    private StructureCompletionService() {
        // Utility class
    }

    /**
     * Result of structure completion detection.
     * 
     * @param chunksToSpawn Set of chunk positions that need to be spawned to complete structures
     * @param structureCount Number of structures discovered
     * @param hitStructureLimit Whether the structure limit was reached
     * @param hitChunkLimit Whether the chunk limit was reached
     */
    public record StructureCompletionResult(
            Set<ChunkPos> chunksToSpawn,
            int structureCount,
            boolean hitStructureLimit,
            boolean hitChunkLimit
    ) {
        public static StructureCompletionResult empty() {
            return new StructureCompletionResult(Set.of(), 0, false, false);
        }

        public boolean hasChunksToSpawn() {
            return !chunksToSpawn.isEmpty();
        }
    }

    /**
     * Collects all chunks needed to complete structures starting from a trigger chunk.
     * 
     * <p>This method implements cascading detection:
     * <ol>
     *   <li>Scan the trigger chunk for structures</li>
     *   <li>For each structure, calculate all chunks it spans</li>
     *   <li>For each new chunk, scan for additional structures (cascade)</li>
     *   <li>Repeat until no new structures found or limits reached</li>
     * </ol>
     * 
     * @param sourceLevel The source dimension level to scan
     * @param triggerChunkPos The chunk that triggered structure completion
     * @param alreadySpawnedChunks Chunks already in the playable area (will be excluded)
     * @return Result containing chunks to spawn and metadata
     */
    public static StructureCompletionResult collectStructureCompletionChunks(
            ServerLevel sourceLevel,
            ChunkPos triggerChunkPos,
            Set<ChunkPos> alreadySpawnedChunks) {

        BrightbronzeConfig.Data config = BrightbronzeConfig.get();

        if (!config.enableStructureCompletion) {
            return StructureCompletionResult.empty();
        }

        int maxStructures = config.maxStructureCompletionStructures;
        int maxChunks = config.maxStructureCompletionChunks;

        // Track discovered structures by their identity (StructureStart reference)
        Set<StructureStart> discoveredStructures = new HashSet<>();
        Set<ChunkPos> chunksToSpawn = new HashSet<>();
        Queue<ChunkPos> chunksToProcess = new ArrayDeque<>();
        Set<ChunkPos> processedChunks = new HashSet<>();

        // Start with the trigger chunk
        chunksToProcess.add(triggerChunkPos);

        boolean hitStructureLimit = false;
        boolean hitChunkLimit = false;

        while (!chunksToProcess.isEmpty()) {
            ChunkPos chunkPos = chunksToProcess.poll();

            if (processedChunks.contains(chunkPos)) {
                continue;
            }
            processedChunks.add(chunkPos);

            // Get the chunk from source dimension
            ChunkAccess sourceChunk = getChunkSafely(sourceLevel, chunkPos);
            if (sourceChunk == null) {
                continue;
            }

            // Find structure starts in this chunk
            Map<Structure, StructureStart> starts = sourceChunk.getAllStarts();
            for (StructureStart start : starts.values()) {
                if (!start.isValid()) {
                    continue;
                }

                if (discoveredStructures.contains(start)) {
                    continue;
                }

                if (discoveredStructures.size() >= maxStructures) {
                    hitStructureLimit = true;
                    break;
                }

                discoveredStructures.add(start);
                BoundingBox boundingBox = start.getBoundingBox();
                Set<ChunkPos> structureChunks = getChunksInBoundingBox(boundingBox);

                for (ChunkPos structureChunk : structureChunks) {
                    if (!alreadySpawnedChunks.contains(structureChunk)) {
                        chunksToSpawn.add(structureChunk);

                        // Cascade: add unprocessed chunks for structure scanning
                        if (!processedChunks.contains(structureChunk)) {
                            chunksToProcess.add(structureChunk);
                        }
                    }
                }

                if (chunksToSpawn.size() >= maxChunks) {
                    hitChunkLimit = true;
                    break;
                }
            }

            if (hitStructureLimit || hitChunkLimit) {
                break;
            }

            // Also check structure references (structures that pass through but don't start here)
            Map<Structure, it.unimi.dsi.fastutil.longs.LongSet> references = sourceChunk.getAllReferences();
            for (Map.Entry<Structure, it.unimi.dsi.fastutil.longs.LongSet> entry : references.entrySet()) {
                Structure structure = entry.getKey();
                it.unimi.dsi.fastutil.longs.LongSet refChunkLongs = entry.getValue();

                for (long refChunkLong : refChunkLongs) {
                    ChunkPos refChunkPos = new ChunkPos(refChunkLong);
                    ChunkAccess refChunk = getChunkSafely(sourceLevel, refChunkPos);
                    if (refChunk == null) {
                        continue;
                    }

                    StructureStart start = refChunk.getStartForStructure(structure);
                    if (start == null || !start.isValid()) {
                        continue;
                    }

                    if (discoveredStructures.contains(start)) {
                        continue;
                    }

                    if (discoveredStructures.size() >= maxStructures) {
                        hitStructureLimit = true;
                        break;
                    }

                    discoveredStructures.add(start);
                    BoundingBox boundingBox = start.getBoundingBox();
                    Set<ChunkPos> structureChunks = getChunksInBoundingBox(boundingBox);

                    for (ChunkPos structureChunk : structureChunks) {
                        if (!alreadySpawnedChunks.contains(structureChunk)) {
                            chunksToSpawn.add(structureChunk);

                            // Cascade
                            if (!processedChunks.contains(structureChunk)) {
                                chunksToProcess.add(structureChunk);
                            }
                        }
                    }

                    if (chunksToSpawn.size() >= maxChunks) {
                        hitChunkLimit = true;
                        break;
                    }
                }

                if (hitStructureLimit || hitChunkLimit) {
                    break;
                }
            }

            if (hitStructureLimit || hitChunkLimit) {
                break;
            }
        }

        // Remove the trigger chunk itself (it's already being spawned by the caller)
        chunksToSpawn.remove(triggerChunkPos);

        // Apply hard chunk cap if we somehow exceeded it
        if (chunksToSpawn.size() > maxChunks) {
            Set<ChunkPos> capped = new HashSet<>();
            int count = 0;
            for (ChunkPos pos : chunksToSpawn) {
                if (count >= maxChunks) {
                    break;
                }
                capped.add(pos);
                count++;
            }
            chunksToSpawn = capped;
            hitChunkLimit = true;
        }

        if (!chunksToSpawn.isEmpty()) {
            BrightbronzeHorizons.LOGGER.debug(
                    "Structure completion from chunk {}: {} structures found, {} additional chunks to spawn",
                    triggerChunkPos, discoveredStructures.size(), chunksToSpawn.size()
            );
        }

        return new StructureCompletionResult(
                chunksToSpawn,
                discoveredStructures.size(),
                hitStructureLimit,
                hitChunkLimit
        );
    }

    /**
     * Calculates all chunk positions that intersect a bounding box.
     * 
     * @param box The bounding box
     * @return Set of chunk positions
     */
    public static Set<ChunkPos> getChunksInBoundingBox(BoundingBox box) {
        Set<ChunkPos> chunks = new HashSet<>();

        int minChunkX = SectionPos.blockToSectionCoord(box.minX());
        int maxChunkX = SectionPos.blockToSectionCoord(box.maxX());
        int minChunkZ = SectionPos.blockToSectionCoord(box.minZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(box.maxZ());

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                chunks.add(new ChunkPos(cx, cz));
            }
        }

        return chunks;
    }

    /**
     * Safely gets a chunk from the source level, generating it if needed.
     * Returns null if chunk cannot be obtained.
     */
    private static ChunkAccess getChunkSafely(ServerLevel level, ChunkPos pos) {
        try {
            // Use FULL status to ensure structures are generated
            return level.getChunk(pos.x, pos.z, ChunkStatus.FULL, true);
        } catch (Exception e) {
            BrightbronzeHorizons.LOGGER.warn(
                    "Failed to get chunk {} for structure detection: {}",
                    pos, e.getMessage()
            );
            return null;
        }
    }
}
