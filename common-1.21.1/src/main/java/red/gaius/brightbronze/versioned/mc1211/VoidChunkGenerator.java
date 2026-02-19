package red.gaius.brightbronze.versioned.mc1211;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A ChunkGenerator that generates completely empty (void) chunks.
 * This is the MC 1.21.1 version with the additional GenerationStep.Carving parameter.
 */
public class VoidChunkGenerator extends ChunkGenerator {

    public static final MapCodec<VoidChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Biome.CODEC.fieldOf("biome").forGetter(VoidChunkGenerator::getBiomeHolder)
            ).apply(instance, VoidChunkGenerator::new)
    );

    private final Holder<Biome> biomeHolder;

    public VoidChunkGenerator(Holder<Biome> biomeHolder) {
        super(new FixedBiomeSource(biomeHolder));
        this.biomeHolder = biomeHolder;
    }

    public Holder<Biome> getBiomeHolder() {
        return biomeHolder;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /**
     * MC 1.21.1 version with GenerationStep.Carving parameter.
     */
    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState,
            BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk,
            GenerationStep.Carving carving) {
        // No carvers in void world
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
            RandomState randomState, ChunkAccess chunk) {
        // No surface building in void world
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // No mob spawning in void world
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        // No biome decoration or structures in void world
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
            StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getSeaLevel() {
        return 63;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapType,
            LevelHeightAccessor level, RandomState randomState) {
        return getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        return new NoiseColumn(getMinY(), new BlockState[0]);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Void Generator (Brightbronze Horizons)");
        biomeHolder.unwrapKey().ifPresent(key -> 
            info.add("Biome: " + key.location())
        );
    }
}
