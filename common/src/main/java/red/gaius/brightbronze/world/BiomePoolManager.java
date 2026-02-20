package red.gaius.brightbronze.world;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.versioned.Versioned;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Manages biome pools for each chunk spawner tier.
 * Biome pools are defined via biome tags in the data pack.
 */
public class BiomePoolManager {
    
    private BiomePoolManager() {
        // Utility class
    }

    /**
     * Gets all biomes in the pool for the given tier.
     * 
     * @param registryAccess The registry access (from server or level)
     * @param tier The chunk spawner tier
     * @return List of biome holders in the tier's pool
     */
    public static List<Holder<Biome>> getBiomesForTier(RegistryAccess registryAccess, ChunkSpawnerTier tier) {
        Registry<Biome> biomeRegistry = Versioned.registry().lookupRegistry(registryAccess, Registries.BIOME);
        List<Holder<Biome>> biomes = new ArrayList<>();
        
        // Use getTagOrEmpty to get all biomes with the tier's tag
        biomeRegistry.getTagOrEmpty(tier.getBiomePoolTag()).forEach(biomes::add);
        
        return biomes;
    }

    /**
     * Selects a random biome from the given tier's pool.
     * 
     * @param registryAccess The registry access (from server or level)
     * @param tier The chunk spawner tier
     * @param random The random source
     * @return A randomly selected biome, or empty if the pool is empty
     */
    public static Optional<Holder<Biome>> selectRandomBiome(
            RegistryAccess registryAccess, 
            ChunkSpawnerTier tier, 
            RandomSource random) {
        
        List<Holder<Biome>> biomes = getBiomesForTier(registryAccess, tier);
        
        if (biomes.isEmpty()) {
            BrightbronzeHorizons.LOGGER.warn(
                "Biome pool for tier {} is empty! Check that the biome tag exists: {}",
                tier.getName(),
                tier.getBiomePoolTag().location()
            );
            return Optional.empty();
        }
        
        return Optional.of(biomes.get(random.nextInt(biomes.size())));
    }

    /**
     * Gets the ResourceLocation for a biome holder.
     * 
     * @param biomeHolder The biome holder
     * @return The biome's resource location, or null if unbound
     */
    public static ResourceLocation getBiomeId(Holder<Biome> biomeHolder) {
        return biomeHolder.unwrapKey()
            .map(key -> key.location())
            .orElse(null);
    }

    /**
     * Checks if a tier has any biomes in its pool.
     * 
     * @param registryAccess The registry access
     * @param tier The chunk spawner tier
     * @return true if the tier has at least one biome
     */
    public static boolean hasBiomes(RegistryAccess registryAccess, ChunkSpawnerTier tier) {
        return !getBiomesForTier(registryAccess, tier).isEmpty();
    }

    /**
     * Gets the count of biomes in a tier's pool.
     * 
     * @param registryAccess The registry access
     * @param tier The chunk spawner tier
     * @return The number of biomes in the pool
     */
    public static int getBiomeCount(RegistryAccess registryAccess, ChunkSpawnerTier tier) {
        return getBiomesForTier(registryAccess, tier).size();
    }
}
