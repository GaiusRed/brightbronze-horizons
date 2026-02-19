package red.gaius.brightbronze.versioned;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.chunk.ChunkGenerator;

/**
 * Provides version-specific world generation helpers.
 * 
 * <p>This interface abstracts APIs that differ between Minecraft versions
 * for world generation, particularly the VoidChunkGenerator which has
 * different abstract method signatures between versions.
 */
public interface WorldGenHelper {
    
    /**
     * Returns the MapCodec for the VoidChunkGenerator.
     * 
     * <p>The VoidChunkGenerator has different abstract method signatures between
     * MC versions (e.g., applyCarvers has an additional parameter in 1.21.1),
     * so the implementation must be version-specific.
     */
    MapCodec<? extends ChunkGenerator> getVoidChunkGeneratorCodec();
    
    /**
     * Checks if the given ChunkGenerator is a VoidChunkGenerator.
     * 
     * <p>Since VoidChunkGenerator implementations are version-specific,
     * this method provides a unified way to check the type.
     */
    boolean isVoidChunkGenerator(ChunkGenerator generator);
}
