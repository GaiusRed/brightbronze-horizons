package red.gaius.brightbronze.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import red.gaius.brightbronze.BrightbronzeHorizons;

/**
 * Registry for dimension types and dimension keys.
 * 
 * Source dimensions are created dynamically per-biome and are not pre-registered here.
 * This class provides utilities for working with dynamic source dimensions.
 */
public final class ModDimensions {
    
    /**
     * Prefix for all source dimension IDs.
     * Source dimensions are named: brightbronze_horizons:source/<biome_namespace>/<biome_path>
     */
    public static final String SOURCE_DIMENSION_PREFIX = "source/";

    private ModDimensions() {
    }

    /**
     * Creates a dimension key for a biome's source dimension.
     *
     * @param biomeId The biome's resource location (e.g., "minecraft:plains")
     * @return The dimension key for that biome's source dimension
     */
    public static ResourceKey<Level> getSourceDimensionKey(ResourceLocation biomeId) {
        String path = SOURCE_DIMENSION_PREFIX + biomeId.getNamespace() + "/" + biomeId.getPath();
        return ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, path));
    }

    /**
     * Creates a dimension type key for a biome's source dimension.
     *
     * @param biomeId The biome's resource location
     * @return The dimension type key for that biome's source dimension
     */
    public static ResourceKey<DimensionType> getSourceDimensionTypeKey(ResourceLocation biomeId) {
        String path = SOURCE_DIMENSION_PREFIX + biomeId.getNamespace() + "/" + biomeId.getPath();
        return ResourceKey.create(Registries.DIMENSION_TYPE,
                ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, path));
    }

    /**
     * Checks if a dimension key represents a source dimension.
     *
     * @param dimensionKey The dimension key to check
     * @return true if this is a Brightbronze Horizons source dimension
     */
    public static boolean isSourceDimension(ResourceKey<Level> dimensionKey) {
        ResourceLocation location = dimensionKey.location();
        return location.getNamespace().equals(BrightbronzeHorizons.MOD_ID)
                && location.getPath().startsWith(SOURCE_DIMENSION_PREFIX);
    }

    /**
     * Extracts the biome ID from a source dimension key.
     *
     * @param dimensionKey The source dimension key
     * @return The biome's resource location, or null if not a valid source dimension
     */
    public static ResourceLocation getBiomeFromSourceDimension(ResourceKey<Level> dimensionKey) {
        if (!isSourceDimension(dimensionKey)) {
            return null;
        }
        String path = dimensionKey.location().getPath();
        String biomePath = path.substring(SOURCE_DIMENSION_PREFIX.length());
        int separatorIndex = biomePath.indexOf('/');
        if (separatorIndex == -1) {
            return null;
        }
        String namespace = biomePath.substring(0, separatorIndex);
        String biome = biomePath.substring(separatorIndex + 1);
        return ResourceLocation.fromNamespaceAndPath(namespace, biome);
    }

    public static void register() {
        // Dimensions are registered dynamically, nothing to do here for now
        BrightbronzeHorizons.LOGGER.debug("Initialized mod dimensions");
    }
}
