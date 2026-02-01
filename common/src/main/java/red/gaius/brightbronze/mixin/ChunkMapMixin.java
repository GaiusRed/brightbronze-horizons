package red.gaius.brightbronze.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import red.gaius.brightbronze.world.chunk.ControllableChunkMap;

import java.util.List;

/**
 * Mixin into ChunkMap to implement ControllableChunkMap interface.
 * 
 * <p>This allows the chunk copy system to force chunks to be resent to all
 * tracking players after their contents have been modified. Without this,
 * clients may see stale chunk data (empty void instead of copied terrain).
 * 
 * <p>MC 1.21.10 uses a new batched chunk sending system via PlayerChunkSender,
 * replacing the old updateChunkTracking approach.
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements ControllableChunkMap {

    @Final
    @Shadow
    ServerLevel level;

    /**
     * Shadow of the getChunkToSend method to get a LevelChunk ready for sending.
     */
    @Shadow
    @Nullable
    public abstract LevelChunk getChunkToSend(long pos);

    /**
     * Shadow of getPlayers to find players tracking a chunk.
     */
    @Shadow
    public abstract List<ServerPlayer> getPlayers(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge);

    /**
     * Forces the chunk to be resent to all players currently tracking it.
     * 
     * <p>Uses MC 1.21.10's PlayerChunkSender API to queue the chunk for resending.
     * This is more efficient than the old immediate-send approach as it batches
     * chunk sends and handles rate limiting automatically.
     */
    @Override
    public void brightbronze$forceResyncChunk(ChunkPos chunkPos) {
        // Get the chunk that's ready to send
        LevelChunk levelChunk = this.getChunkToSend(chunkPos.toLong());
        if (levelChunk != null) {
            // For each player tracking this chunk, mark it pending to send
            for (ServerPlayer player : this.getPlayers(chunkPos, false)) {
                // Use the new PlayerChunkSender API in MC 1.21.10
                player.connection.chunkSender.markChunkPendingToSend(levelChunk);
            }
        }
    }
}
