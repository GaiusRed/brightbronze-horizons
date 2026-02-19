package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import red.gaius.brightbronze.versioned.ChunkSyncHelper;

/**
 * Minecraft 1.21.10 implementation of ChunkSyncHelper.
 * 
 * <p>In 1.21.10, chunk resending uses the PlayerChunkSender API via
 * player.connection.chunkSender.markChunkPendingToSend().
 */
public class ChunkSyncHelperImpl implements ChunkSyncHelper {
    
    @Override
    public void markChunkForResend(ServerPlayer player, LevelChunk chunk) {
        // Use the new PlayerChunkSender API in MC 1.21.10
        player.connection.chunkSender.markChunkPendingToSend(chunk);
    }
}
