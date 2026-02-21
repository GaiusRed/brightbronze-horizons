package red.gaius.brightbronze.world.compat;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.versioned.Versioned;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Detects and provides access to modded biomes (biomes not from the minecraft: namespace).
 * 
 * <p>This utility enables the Altered Horizon Anchor to automatically support ANY worldgen mod
 * by detecting biomes based on their namespace rather than relying on explicit configuration.
 * 
 * <p>Key principle: Any biome where {@code namespace != "minecraft"} is considered a modded biome.
 * This ensures zero-configuration support for current and future worldgen mods.
 * 
 * <p>Detection results are cached per registry identity to avoid repeated scanning.
 */
public final class ModdedBiomeDetector {

    /** Cached detection result to avoid re-scanning the registry on every call. */
    private static volatile DetectionResult cachedResult = null;

    private ModdedBiomeDetector() {
        // Utility class
    }

    /**
     * Gets all modded biomes (biomes not from the minecraft: namespace).
     * 
     * @param registryAccess The registry access from the server or level
     * @return Unmodifiable list of modded biome holders; empty list if none found
     */
    public static List<Holder<Biome>> getModdedBiomes(RegistryAccess registryAccess) {
        return ensureDetected(registryAccess).moddedBiomes();
    }

    /**
     * Gets the count of available modded biomes.
     * 
     * @param registryAccess The registry access from the server or level
     * @return Number of modded biomes detected; 0 if none
     */
    public static int getModdedBiomeCount(RegistryAccess registryAccess) {
        return ensureDetected(registryAccess).moddedBiomes().size();
    }

    /**
     * Checks if any modded biomes are available.
     * 
     * @param registryAccess The registry access from the server or level
     * @return true if at least one modded biome is available
     */
    public static boolean hasModdedBiomes(RegistryAccess registryAccess) {
        return !ensureDetected(registryAccess).moddedBiomes().isEmpty();
    }

    /**
     * Selects a modded biome by index (for deterministic selection).
     * 
     * @param registryAccess The registry access from the server or level
     * @param index The index to select (should be in range [0, count))
     * @return The selected biome holder, or empty if index is out of range or no modded biomes exist
     */
    public static Optional<Holder<Biome>> selectModdedBiome(RegistryAccess registryAccess, int index) {
        List<Holder<Biome>> biomes = ensureDetected(registryAccess).moddedBiomes();
        if (biomes.isEmpty() || index < 0 || index >= biomes.size()) {
            return Optional.empty();
        }
        return Optional.of(biomes.get(index));
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
     * Clears the cached detection result.
     * Should be called when the server stops or registries are reloaded.
     */
    public static void clearCache() {
        cachedResult = null;
        BrightbronzeHorizons.LOGGER.debug("Cleared modded biome detection cache");
    }

    /**
     * Ensures detection has been performed for the given registry access.
     * Uses caching to avoid repeated registry scans.
     */
    private static DetectionResult ensureDetected(RegistryAccess registryAccess) {
        int registryIdentity = System.identityHashCode(registryAccess);
        DetectionResult current = cachedResult;
        
        if (current != null && current.registryIdentity() == registryIdentity) {
            return current;
        }

        // Perform detection
        DetectionResult result = detectModdedBiomes(registryAccess, registryIdentity);
        cachedResult = result;
        return result;
    }

    /**
     * Scans the biome registry and collects all non-minecraft: namespaced biomes.
     */
    private static DetectionResult detectModdedBiomes(RegistryAccess registryAccess, int registryIdentity) {
        Registry<Biome> biomeRegistry = Versioned.registry().lookupRegistry(registryAccess, Registries.BIOME);
        List<Holder<Biome>> moddedBiomes = new ArrayList<>();

        // Iterate all biome keys in the registry
        for (ResourceLocation biomeId : biomeRegistry.keySet()) {
            // Check if this is a modded biome (not from minecraft namespace)
            if (!biomeId.getNamespace().equals("minecraft")) {
                // Look up the holder reference using versioned helper
                var holderOpt = Versioned.registry().getHolder(biomeRegistry, biomeId);
                if (holderOpt.isPresent()) {
                    moddedBiomes.add(holderOpt.get());
                }
            }
        }

        // Sort for deterministic ordering (by resource location)
        moddedBiomes.sort((a, b) -> {
            ResourceLocation idA = a.unwrapKey().map(k -> k.location()).orElse(null);
            ResourceLocation idB = b.unwrapKey().map(k -> k.location()).orElse(null);
            if (idA == null && idB == null) return 0;
            if (idA == null) return 1;
            if (idB == null) return -1;
            return idA.compareTo(idB);
        });

        if (!moddedBiomes.isEmpty()) {
            BrightbronzeHorizons.LOGGER.debug(
                "Detected {} modded biomes from {} namespace(s)",
                moddedBiomes.size(),
                countUniqueNamespaces(moddedBiomes)
            );
        }

        return new DetectionResult(registryIdentity, Collections.unmodifiableList(moddedBiomes));
    }

    /**
     * Counts unique namespaces among the detected biomes.
     */
    private static long countUniqueNamespaces(List<Holder<Biome>> biomes) {
        return biomes.stream()
            .flatMap(h -> h.unwrapKey().stream())
            .map(key -> key.location().getNamespace())
            .distinct()
            .count();
    }

    /**
     * Cached detection result.
     * 
     * @param registryIdentity Identity hash of the registry access used for detection
     * @param moddedBiomes Unmodifiable list of detected modded biomes
     */
    private record DetectionResult(int registryIdentity, List<Holder<Biome>> moddedBiomes) {
    }
}
