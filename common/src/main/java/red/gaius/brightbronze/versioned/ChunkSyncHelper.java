package red.gaius.brightbronze.versioned;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Version-abstracted chunk synchronization.
 * 
 * <p>Handles the differences in chunk sending between versions:
 * <ul>
 *   <li>1.21.10: Uses PlayerChunkSender.markChunkPendingToSend()</li>
 *   <li>1.21.1: Uses ChunkMap.updateChunkTracking() or packet-based approach</li>
 * </ul>
 */
public interface ChunkSyncHelper {
    
    /**
     * Marks a chunk as needing to be resent to the given player.
     * 
     * <p>This is used after chunk contents have been modified (e.g., copied from
     * a source dimension) to ensure the player sees the updated terrain.
     * 
     * @param player The player to resend the chunk to
     * @param chunk The chunk to resend
     */
    void markChunkForResend(ServerPlayer player, LevelChunk chunk);
}
