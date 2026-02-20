package red.gaius.brightbronze.versioned;

import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Minecraft 1.21.1 implementation of ChunkHelper.
 * Uses setUnsaved(true) since markUnsaved() was only added in 1.21.10.
 */
public class ChunkHelperImpl implements ChunkHelper {

    @Override
    public void markUnsaved(LevelChunk chunk) {
        chunk.setUnsaved(true);
    }
}
