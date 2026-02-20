package red.gaius.brightbronze.versioned;

import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Minecraft 1.21.10 implementation of ChunkHelper.
 * Uses the markUnsaved() method introduced in 1.21.10.
 */
public class ChunkHelperImpl implements ChunkHelper {

    @Override
    public void markUnsaved(LevelChunk chunk) {
        chunk.markUnsaved();
    }
}
