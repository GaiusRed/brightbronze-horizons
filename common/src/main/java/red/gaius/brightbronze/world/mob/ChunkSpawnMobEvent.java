package red.gaius.brightbronze.world.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import red.gaius.brightbronze.world.ChunkSpawnerTier;

/**
 * Phase 7 event hook: fired when a new chunk is spawned/revealed by a Chunk Spawner.
 *
 * <p>For now this is a thin wrapper around {@link ChunkMobSpawner}. It exists so future
 * phases can add additional listeners or configuration without rewriting call sites.
 */
public final class ChunkSpawnMobEvent {

    private ChunkSpawnMobEvent() {
    }

    public static void fire(ServerLevel level, ChunkPos chunkPos, ChunkSpawnerTier tier) {
        ChunkMobSpawner.onChunkSpawned(level, chunkPos, tier);
    }
}
