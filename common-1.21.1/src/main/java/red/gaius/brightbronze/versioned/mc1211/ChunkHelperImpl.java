package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.world.level.chunk.LevelChunk;
import red.gaius.brightbronze.versioned.ChunkHelper;

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
