package red.gaius.brightbronze.world;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import red.gaius.brightbronze.BrightbronzeHorizons;

/**
 * Defines the tiers for Chunk Spawners.
 * Each tier is associated with a specific biome pool and spawn behavior.
 */
public enum ChunkSpawnerTier {
    /**
     * Copper tier - Common, non-watery Overworld biomes (curated default pool).
     * Mobs spawn only at night.
     */
    COPPER("copper", false),

    /**
     * Coal tier - Common Overworld biomes (Plains, Forest, Taiga, etc.)
     * Mobs spawn only at night.
     */
    COAL("coal", false),
    
    /**
     * Iron tier - Rare Overworld biomes (Dark Forest, Jungle, Badlands, etc.)
     * Mobs spawn only at night.
     */
    IRON("iron", false),
    
    /**
     * Gold tier - All Nether biomes.
     * Mobs spawn regardless of time.
     */
    GOLD("gold", true),
    
    /**
     * Emerald tier - Empty by default, intended for modpack customization.
     * Mobs spawn regardless of time.
     */
    EMERALD("emerald", true),
    
    /**
     * Diamond tier - All End biomes.
     * Mobs spawn regardless of time.
     */
    DIAMOND("diamond", true);

    private final String name;
    private final boolean alwaysSpawnMobs;
    private final TagKey<Biome> biomePoolTag;

    ChunkSpawnerTier(String name, boolean alwaysSpawnMobs) {
        this.name = name;
        this.alwaysSpawnMobs = alwaysSpawnMobs;
        this.biomePoolTag = TagKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "tier/" + name)
        );
    }

    /**
     * @return The lowercase name of this tier (e.g., "coal", "iron")
     */
    public String getName() {
        return name;
    }

    /**
     * @return Whether mobs should spawn regardless of time of day.
     *         Coal and Iron tiers spawn mobs only at night.
     *         Gold, Emerald, and Diamond tiers always spawn mobs.
     */
    public boolean alwaysSpawnsMobs() {
        return alwaysSpawnMobs;
    }

    /**
     * @return The biome tag key for this tier's biome pool.
     *         Located at data/brightbronze_horizons/tags/worldgen/biome/tier/{name}.json
     */
    public TagKey<Biome> getBiomePoolTag() {
        return biomePoolTag;
    }

    /**
     * Get a tier by its name.
     * @param name The lowercase tier name
     * @return The tier, or null if not found
     */
    public static ChunkSpawnerTier byName(String name) {
        if (name == null) {
            return null;
        }

        String normalized = name.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        for (ChunkSpawnerTier tier : values()) {
            // Accept both the datapack-friendly lowercase id (tier.getName()) and the enum constant name (tier.name()).
            if (tier.name.equalsIgnoreCase(normalized) || tier.name().equalsIgnoreCase(normalized)) {
                return tier;
            }
        }
        return null;
    }
}
