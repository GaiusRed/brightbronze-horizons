package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import red.gaius.brightbronze.versioned.ChunkSyncHelper;

/**
 * Minecraft 1.21.1 implementation of ChunkSyncHelper.
 * 
 * <p>In 1.21.1, chunk resending uses a different approach since 
 * PlayerChunkSender doesn't exist. We use the lower-level approach
 * of sending chunk data packets directly.
 * 
 * <p>Note: In 1.21.1, the player.connection.chunkSender API doesn't exist.
 * Instead we need to use an alternative approach to force chunk resend.
 */
public class ChunkSyncHelperImpl implements ChunkSyncHelper {
    
    @Override
    public void markChunkForResend(ServerPlayer player, LevelChunk chunk) {
        // In 1.21.1, we need to use a different approach
        // The ChunkMap tracks which chunks need to be sent to players
        // We can trigger a resend by marking the chunk's position as needing update
        
        // Get the server level's chunk source and invalidate the player's view of this chunk
        var serverLevel = player.serverLevel();
        var chunkMap = serverLevel.getChunkSource().chunkMap;
        
        // Force the chunk to be resent by calling the appropriate method
        // In 1.21.1, we can use chunkMap.updateChunkTracking() or send packet directly
        var chunkPos = chunk.getPos();
        
        // Send the chunk data packet directly to the player
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(
                chunk,
                serverLevel.getLightEngine(),
                null,  // bitSet for sky light - null means all
                null   // bitSet for block light - null means all
        ));
    }
}
