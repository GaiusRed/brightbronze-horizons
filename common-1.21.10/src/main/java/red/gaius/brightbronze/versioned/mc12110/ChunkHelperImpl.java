package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.world.level.chunk.LevelChunk;
import red.gaius.brightbronze.versioned.ChunkHelper;

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
