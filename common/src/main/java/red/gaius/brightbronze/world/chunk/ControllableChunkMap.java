package red.gaius.brightbronze.world.chunk;

import net.minecraft.world.level.ChunkPos;

/**
 * Interface implemented by ChunkMap via mixin to expose chunk resynchronization capability.
 * 
 * <p>When chunks are modified after initial generation (such as during chunk copying from
 * source dimensions), the changes may not be automatically sent to connected clients.
 * This interface provides a way to force chunk data to be resent to all tracking players.
 * 
 * <p>This is essential for the chunk spawning mechanic where void chunks are filled
 * with terrain data from source dimensions and need to be visible to players.
 */
public interface ControllableChunkMap {

    /**
     * Forces the chunk at the given position to be resent to all players tracking it.
     * 
     * <p>This should be called after modifying chunk data (blocks, biomes, etc.) to ensure
     * clients receive the updated chunk state. Without this, clients may see stale data
     * (e.g., empty void chunks instead of copied terrain).
     * 
     * @param chunkPos The position of the chunk to resync
     */
    void brightbronze$forceResyncChunk(ChunkPos chunkPos);
}
