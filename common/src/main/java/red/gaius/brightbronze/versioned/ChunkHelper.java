package red.gaius.brightbronze.versioned;

import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Provides version-specific chunk helpers.
 * 
 * <p>This interface abstracts chunk methods which have different
 * names between Minecraft versions (markUnsaved() in 1.21.10 vs 
 * setUnsaved(true) in 1.21.1).
 */
public interface ChunkHelper {
    
    /**
     * Marks the chunk as needing to be saved.
     * 
     * @param chunk The chunk to mark
     */
    void markUnsaved(LevelChunk chunk);
}
