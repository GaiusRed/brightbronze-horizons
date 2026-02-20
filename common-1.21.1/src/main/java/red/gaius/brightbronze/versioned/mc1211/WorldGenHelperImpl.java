package red.gaius.brightbronze.versioned.mc1211;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import red.gaius.brightbronze.versioned.WorldGenHelper;

/**
 * MC 1.21.1 implementation of WorldGenHelper.
 */
public class WorldGenHelperImpl implements WorldGenHelper {
    
    @Override
    public MapCodec<? extends ChunkGenerator> getVoidChunkGeneratorCodec() {
        return VoidChunkGenerator.CODEC;
    }
    
    @Override
    public boolean isVoidChunkGenerator(ChunkGenerator generator) {
        return generator instanceof VoidChunkGenerator;
    }
    
    @Override
    public ChunkGenerator createVoidChunkGenerator(Holder<Biome> biome) {
        return new VoidChunkGenerator(biome);
    }
}
