package red.gaius.brightbronze.world.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import red.gaius.brightbronze.BrightbronzeHorizons;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A ChunkGenerator that wraps a parent generator but forces all biomes to a single biome.
 * Used for source dimensions to generate terrain with consistent biome characteristics.
 * 
 * <p>The generator delegates all terrain generation to the parent generator, but uses
 * a {@link FixedBiomeSource} to ensure all chunks report the same biome. This allows
 * us to create multiple dimensions, each with different biome-specific terrain.
 */
public class SingleBiomeChunkGenerator extends ChunkGenerator {

    /**
     * Codec for serialization. The generator is reconstructed from the parent generator
     * and the biome resource key.
     */
    public static final MapCodec<SingleBiomeChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ChunkGenerator.CODEC.fieldOf("parent").forGetter(SingleBiomeChunkGenerator::getParent),
                    ResourceKey.codec(Registries.BIOME).fieldOf("biome").forGetter(SingleBiomeChunkGenerator::getBiomeKey)
            ).apply(instance, SingleBiomeChunkGenerator::new)
    );

    private final ChunkGenerator parent;
    private final ResourceKey<Biome> biomeKey;
    private Holder<Biome> biomeHolder;

    /**
     * Creates a new SingleBiomeChunkGenerator.
     * 
     * @param parent The parent chunk generator to wrap (e.g., overworld generator)
     * @param biomeKey The biome key to use for all chunks
     */
    public SingleBiomeChunkGenerator(ChunkGenerator parent, ResourceKey<Biome> biomeKey) {
        // We use a placeholder biome source initially; it will be replaced when we have registry access
        super(parent.getBiomeSource());
        this.parent = parent;
        this.biomeKey = biomeKey;
    }

    /**
     * Creates a SingleBiomeChunkGenerator with a resolved biome holder.
     * 
     * @param parent The parent chunk generator
     * @param biomeHolder The resolved biome holder
     */
    public SingleBiomeChunkGenerator(ChunkGenerator parent, Holder<Biome> biomeHolder) {
        super(new FixedBiomeSource(biomeHolder));
        this.parent = parent;
        this.biomeKey = biomeHolder.unwrapKey().orElseThrow();
        this.biomeHolder = biomeHolder;
    }

    /**
     * Factory method for creating a SingleBiomeChunkGenerator with registry access.
     * 
     * @param parent The parent chunk generator
     * @param biomeKey The biome key
     * @param biomeRegistry The biome registry for resolving the biome
     * @return A new SingleBiomeChunkGenerator
     */
    public static SingleBiomeChunkGenerator create(
            ChunkGenerator parent, 
            ResourceKey<Biome> biomeKey,
            HolderLookup<Biome> biomeRegistry) {
        Holder<Biome> biomeHolder = biomeRegistry.getOrThrow(biomeKey);
        return new SingleBiomeChunkGenerator(parent, biomeHolder);
    }

    /**
     * @return The parent chunk generator that handles actual terrain generation
     */
    public ChunkGenerator getParent() {
        return parent;
    }

    /**
     * @return The biome key used for all chunks in this dimension
     */
    public ResourceKey<Biome> getBiomeKey() {
        return biomeKey;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState randomState, 
            BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk) {
        parent.applyCarvers(level, seed, randomState, biomeManager, structureManager, chunk);
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, 
            RandomState randomState, ChunkAccess chunk) {
        parent.buildSurface(level, structureManager, randomState, chunk);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        parent.spawnOriginalMobs(level);
    }

    @Override
    public int getGenDepth() {
        return parent.getGenDepth();
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
            StructureManager structureManager, ChunkAccess chunk) {
        return parent.fillFromNoise(blender, randomState, structureManager, chunk);
    }

    @Override
    public int getSeaLevel() {
        return parent.getSeaLevel();
    }

    @Override
    public int getMinY() {
        return parent.getMinY();
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapType, 
            LevelHeightAccessor level, RandomState randomState) {
        return parent.getBaseHeight(x, z, heightmapType, level, randomState);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        return parent.getBaseColumn(x, z, level, randomState);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("SingleBiome: " + biomeKey.location());
        parent.addDebugScreenInfo(info, randomState, pos);
    }
}
