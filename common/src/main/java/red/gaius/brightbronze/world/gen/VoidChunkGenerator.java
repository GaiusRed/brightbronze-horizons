package red.gaius.brightbronze.world.gen;

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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A ChunkGenerator that generates completely empty (void) chunks.
 * Used for the Brightbronze Horizons overworld - terrain is copied from source dimensions
 * rather than generated procedurally.
 * 
 * <p>This generator produces empty chunks with a fixed biome (plains by default).
 * The actual terrain is populated by copying chunks from source dimensions via
 * chunk spawner items and the chunk copying system.
 */
public class VoidChunkGenerator extends ChunkGenerator {

    /**
     * Codec for serialization/deserialization.
     * Uses Biome holder directly for proper registry resolution.
     */
    public static final MapCodec<VoidChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Biome.CODEC.fieldOf("biome").forGetter(VoidChunkGenerator::getBiomeHolder)
            ).apply(instance, VoidChunkGenerator::new)
    );

    private final Holder<Biome> biomeHolder;

    /**
     * Creates a VoidChunkGenerator with the specified biome holder.
     * 
     * @param biomeHolder The biome holder to use for all void chunks
     */
    public VoidChunkGenerator(Holder<Biome> biomeHolder) {
        super(new FixedBiomeSource(biomeHolder));
        this.biomeHolder = biomeHolder;
    }

    /**
     * @return The biome holder used for void chunks
     */
    public Holder<Biome> getBiomeHolder() {
        return biomeHolder;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    /**
     * Does nothing - void chunks have no carvers.
     */
    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState,
            BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
        // No carvers in void world
    }

    /**
     * Does nothing - void chunks have no surface.
     */
    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager,
            RandomState randomState, ChunkAccess chunk) {
        // No surface building in void world
    }

    /**
     * Does nothing - void chunks have no mobs to spawn.
     */
    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        // No mob spawning in void world
    }

    /**
     * Does nothing - void world has no biome decorations or structures.
     * This is the key method that prevents structures like trial chambers from generating.
     */
    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager) {
        // No biome decoration or structures in void world
    }

    /**
     * @return The generation depth (384 blocks, same as overworld: -64 to 320)
     */
    @Override
    public int getGenDepth() {
        return 384; // Standard overworld height
    }

    /**
     * Returns an immediately completed future with the unmodified chunk.
     * This is the key method that makes this a void generator - we don't fill
     * the chunk with any blocks.
     */
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
            StructureManager structureManager, ChunkAccess chunk) {
        // Return the chunk immediately without any modifications - it stays empty
        return CompletableFuture.completedFuture(chunk);
    }

    /**
     * @return The sea level (63, same as overworld)
     */
    @Override
    public int getSeaLevel() {
        return 63; // Standard overworld sea level
    }

    /**
     * @return The minimum Y level (-64, same as overworld)
     */
    @Override
    public int getMinY() {
        return -64; // Standard overworld minimum
    }

    /**
     * Returns the base height at the given position.
     * For void generation, always returns minimum Y since there's no terrain.
     */
    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapType,
            LevelHeightAccessor level, RandomState randomState) {
        return getMinY(); // No terrain, so minimum height
    }

    /**
     * Returns an empty noise column since void chunks have no blocks.
     */
    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        return new NoiseColumn(getMinY(), new BlockState[0]);
    }

    /**
     * Adds debug info to the F3 screen.
     */
    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Void Generator (Brightbronze Horizons)");
        biomeHolder.unwrapKey().ifPresent(key -> 
            info.add("Biome: " + key.location())
        );
    }
}
