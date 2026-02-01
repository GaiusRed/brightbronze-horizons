package red.gaius.brightbronze.world.rules;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import red.gaius.brightbronze.world.ChunkSpawnerTier;
import red.gaius.brightbronze.world.mob.MobSpawnRule;

import java.util.List;
import java.util.Set;

/**
 * Data-driven rule for biome selection and per-biome behaviors.
 */
public record BiomeRule(
    ResourceLocation sourceId,
    int priority,
    TagKey<Biome> biomeTag,
    Set<ResourceLocation> allow,
    Set<ResourceLocation> deny,
    ChunkSpawnerTier tier,
    int weight,
    List<BlockReplacementRule> replacements,
    List<MobSpawnRule> mobSpawns
) {

    public static TagKey<Biome> tagKey(ResourceLocation tagId) {
        return TagKey.create(Registries.BIOME, tagId);
    }
}
