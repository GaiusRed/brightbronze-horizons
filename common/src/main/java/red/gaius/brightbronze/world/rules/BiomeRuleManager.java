package red.gaius.brightbronze.world.rules;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.versioned.Versioned;
import red.gaius.brightbronze.world.BiomePoolManager;
import red.gaius.brightbronze.world.ChunkSpawnerTier;
import red.gaius.brightbronze.world.mob.MobSpawnRule;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads biome rules from data packs and provides resolved per-biome behaviors.
 *
 * <p>Files: data/<namespace>/biome_rules/*.json
 */
public final class BiomeRuleManager {

    private static final String BASE_PATH = "biome_rules";

    private static volatile List<BiomeRule> loadedRules = List.of();
    private static volatile int rulesGeneration = 0;

    private static volatile ResolvedCache resolvedCache = new ResolvedCache(0, -1, Map.of(), Map.of());

    private BiomeRuleManager() {
    }

    public static void reload(ResourceManager resourceManager) {
        apply(loadRules(resourceManager));
    }

    static List<BiomeRule> loadRules(ResourceManager resourceManager) {
        List<BiomeRule> rules = new ArrayList<>();

        for (var entry : resourceManager.listResources(BASE_PATH, id -> id.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation id = entry.getKey();
            Resource resource = entry.getValue();

            BiomeRule parsed = parseRule(id, resource);
            if (parsed != null) {
                rules.add(parsed);
            }
        }

        rules.sort(Comparator
            .comparingInt(BiomeRule::priority).reversed()
            .thenComparing(r -> r.sourceId().toString()));

        return Collections.unmodifiableList(rules);
    }

    static void apply(List<BiomeRule> rules) {
        loadedRules = rules;
        rulesGeneration++;
        resolvedCache = new ResolvedCache(rulesGeneration, -1, Map.of(), Map.of());

        BrightbronzeHorizons.LOGGER.info("Loaded {} biome rule files", rules.size());
    }

    public static WeightedBiomePool getWeightedPool(RegistryAccess registryAccess, ChunkSpawnerTier tier) {
        ensureResolved(registryAccess);
        WeightedBiomePool pool = resolvedCache.poolsByTier.get(tier);
        if (pool == null || pool.isEmpty()) {
            return WeightedBiomePool.empty();
        }
        return pool;
    }

    @Nullable
    public static ChunkSpawnerTier getResolvedTier(RegistryAccess registryAccess, ResourceLocation biomeId) {
        ensureResolved(registryAccess);
        ResolvedBiome resolved = resolvedCache.byBiomeId.get(biomeId);
        return resolved == null ? null : resolved.tier;
    }

    public static List<BlockReplacementRule> getReplacementRules(RegistryAccess registryAccess, ResourceLocation biomeId) {
        ensureResolved(registryAccess);
        ResolvedBiome resolved = resolvedCache.byBiomeId.get(biomeId);
        return resolved == null ? List.of() : resolved.replacements;
    }

    public static List<MobSpawnRule> getMobSpawnRules(RegistryAccess registryAccess, ResourceLocation biomeId) {
        ensureResolved(registryAccess);
        ResolvedBiome resolved = resolvedCache.byBiomeId.get(biomeId);
        return resolved == null ? List.of() : resolved.mobSpawns;
    }

    private static void ensureResolved(RegistryAccess registryAccess) {
        int identity = System.identityHashCode(registryAccess);
        ResolvedCache cache = resolvedCache;
        if (cache.generation == rulesGeneration && cache.registryIdentity == identity) {
            return;
        }

        Registry<Biome> biomeRegistry = Versioned.registry().lookupRegistry(registryAccess, Registries.BIOME);

        // Registry iteration APIs differ across MC versions. Instead of iterating every biome in
        // the registry, build a candidate set from:
        // - tier biome tags (back-compat)
        // - rule biome tags
        // - rule allow lists
        Map<ResourceLocation, Holder<Biome>> candidates = new HashMap<>();

        for (ChunkSpawnerTier tier : ChunkSpawnerTier.values()) {
            if (tier == ChunkSpawnerTier.COAL) {
                continue;
            }
            biomeRegistry.getTagOrEmpty(tier.getBiomePoolTag()).forEach(holder -> {
                ResourceLocation id2 = BiomePoolManager.getBiomeId(holder);
                if (id2 != null) {
                    candidates.putIfAbsent(id2, holder);
                }
            });
        }

        for (BiomeRule rule : loadedRules) {
            if (rule.biomeTag() != null) {
                biomeRegistry.getTagOrEmpty(rule.biomeTag()).forEach(holder -> {
                    ResourceLocation id2 = BiomePoolManager.getBiomeId(holder);
                    if (id2 != null) {
                        candidates.putIfAbsent(id2, holder);
                    }
                });
            }

            for (ResourceLocation allowId : rule.allow()) {
                var opt = Versioned.registry().getHolder(biomeRegistry, allowId);
                opt.ifPresent(holder -> candidates.putIfAbsent(allowId, holder));
            }
        }

        Map<ResourceLocation, ResolvedBiome> byBiome = new HashMap<>();
        Map<ChunkSpawnerTier, WeightedBiomePool> pools = new EnumMap<>(ChunkSpawnerTier.class);
        for (ChunkSpawnerTier tier : ChunkSpawnerTier.values()) {
            pools.put(tier, new WeightedBiomePool(new ArrayList<>()));
        }

        // Resolve per-biome rule stacking.
        for (var entry : candidates.entrySet()) {
            ResourceLocation biomeId = entry.getKey();
            Holder<Biome> holder = entry.getValue();

            List<BiomeRule> matching = new ArrayList<>();
            for (BiomeRule rule : loadedRules) {
                if (matches(rule, holder, biomeId)) {
                    matching.add(rule);
                }
            }

            ChunkSpawnerTier assignedTier = null;
            int weight = 1;
            List<BlockReplacementRule> replacements = List.of();
            List<MobSpawnRule> mobSpawns = List.of();

            if (!matching.isEmpty()) {
                // Rules already loaded in priority order; ensure stable.
                matching.sort(Comparator
                    .comparingInt(BiomeRule::priority).reversed()
                    .thenComparing(r -> r.sourceId().toString()));

                BiomeRule first = matching.get(0);
                assignedTier = first.tier();
                weight = Math.max(1, first.weight());

                // First-match wins at rule ordering level: concatenate in priority order.
                List<BlockReplacementRule> repl = new ArrayList<>();
                List<MobSpawnRule> mobs = new ArrayList<>();
                for (BiomeRule r : matching) {
                    repl.addAll(r.replacements());
                    mobs.addAll(r.mobSpawns());
                }
                replacements = Collections.unmodifiableList(repl);
                mobSpawns = Collections.unmodifiableList(mobs);
            } else {
                // Back-compat: fall back to tier biome tags if no rule matches.
                for (ChunkSpawnerTier tier : ChunkSpawnerTier.values()) {
                    if (tier == ChunkSpawnerTier.COAL) {
                        continue;
                    }
                    if (holder.is(tier.getBiomePoolTag())) {
                        assignedTier = tier;
                        break;
                    }
                }
            }

            if (assignedTier != null) {
                byBiome.put(biomeId, new ResolvedBiome(assignedTier, weight, replacements, mobSpawns));
            }
        }

        // Build tier pools from resolved biome mapping (stable ordering for determinism).
        List<ResourceLocation> orderedBiomeIds = new ArrayList<>(byBiome.keySet());
        orderedBiomeIds.sort(Comparator.comparing(ResourceLocation::toString));
        for (ResourceLocation biomeId : orderedBiomeIds) {
            ResolvedBiome resolved = byBiome.get(biomeId);

            if (resolved.tier == ChunkSpawnerTier.COAL) {
                continue; // Coal is local-biome; no pool selection.
            }

            Holder<Biome> holder = candidates.get(biomeId);
            if (holder != null) {
                pools.get(resolved.tier).entries.add(new WeightedBiomeEntry(holder, resolved.weight));
            }
        }

        // Freeze pools.
        Map<ChunkSpawnerTier, WeightedBiomePool> frozenPools = new EnumMap<>(ChunkSpawnerTier.class);
        for (ChunkSpawnerTier tier : ChunkSpawnerTier.values()) {
            frozenPools.put(tier, pools.get(tier).freeze());
        }

        resolvedCache = new ResolvedCache(rulesGeneration, identity, Map.copyOf(byBiome), Map.copyOf(frozenPools));
    }

    private static boolean matches(BiomeRule rule, Holder<Biome> biome, ResourceLocation biomeId) {
        if (rule.biomeTag() != null && !biome.is(rule.biomeTag())) {
            return false;
        }
        if (!rule.allow().isEmpty() && !rule.allow().contains(biomeId)) {
            return false;
        }
        return !rule.deny().contains(biomeId);
    }

    private static BiomeRule parseRule(ResourceLocation id, Resource resource) {
        try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            int priority = GsonHelper.getAsInt(root, "priority", 0);
            String tierStr = GsonHelper.getAsString(root, "tier", "");
            ChunkSpawnerTier tier = ChunkSpawnerTier.byName(tierStr);
            if (tier == null) {
                BrightbronzeHorizons.LOGGER.warn("Ignoring biome rule with unknown tier '{}' at {}", tierStr, id);
                return null;
            }

            TagKeyOrNull tag = parseBiomeTag(root);

            Set<ResourceLocation> allow = parseIdSet(root, "allow");
            Set<ResourceLocation> deny = parseIdSet(root, "deny");

            int weight = Math.max(1, GsonHelper.getAsInt(root, "weight", 1));

            List<BlockReplacementRule> replacements = parseReplacements(root, id);
            List<MobSpawnRule> mobSpawns = parseMobSpawns(root, id);

            return new BiomeRule(
                id,
                priority,
                tag == null ? null : BiomeRule.tagKey(tag.id),
                allow,
                deny,
                tier,
                weight,
                replacements,
                mobSpawns
            );
        } catch (IOException e) {
            BrightbronzeHorizons.LOGGER.warn("Failed reading biome rule {}: {}", id, e.toString());
            return null;
        } catch (Exception e) {
            BrightbronzeHorizons.LOGGER.warn("Failed parsing biome rule {}: {}", id, e.toString());
            return null;
        }
    }

    private static TagKeyOrNull parseBiomeTag(JsonObject root) {
        String tagStr = GsonHelper.getAsString(root, "biome_tag", "");
        if (tagStr.isBlank()) {
            return null;
        }
        if (tagStr.startsWith("#")) {
            tagStr = tagStr.substring(1);
        }
        try {
            return new TagKeyOrNull(ResourceLocation.parse(tagStr));
        } catch (Exception e) {
            return null;
        }
    }

    private static Set<ResourceLocation> parseIdSet(JsonObject root, String key) {
        JsonArray array = GsonHelper.getAsJsonArray(root, key, null);
        if (array == null) {
            return Set.of();
        }
        Set<ResourceLocation> ids = new HashSet<>();
        for (JsonElement el : array) {
            if (!el.isJsonPrimitive()) {
                continue;
            }
            String s = el.getAsString();
            try {
                ids.add(ResourceLocation.parse(s));
            } catch (Exception ignored) {
            }
        }
        return ids.isEmpty() ? Set.of() : Collections.unmodifiableSet(ids);
    }

    private static List<BlockReplacementRule> parseReplacements(JsonObject root, ResourceLocation id) {
        JsonArray array = GsonHelper.getAsJsonArray(root, "replacements", null);
        if (array == null) {
            return List.of();
        }

        List<BlockReplacementRule> rules = new ArrayList<>();
        for (JsonElement el : array) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject obj = el.getAsJsonObject();
            String matchStr = GsonHelper.getAsString(obj, "match", "");
            String replaceStr = GsonHelper.getAsString(obj, "replace_with", "");
            if (matchStr.isBlank() || replaceStr.isBlank()) {
                continue;
            }

            ResourceLocation replacement;
            try {
                replacement = ResourceLocation.parse(replaceStr);
            } catch (Exception e) {
                BrightbronzeHorizons.LOGGER.warn("Invalid replacement id '{}' in {}", replaceStr, id);
                continue;
            }

            if (matchStr.startsWith("#")) {
                try {
                    ResourceLocation tagId = ResourceLocation.parse(matchStr.substring(1));
                    rules.add(BlockReplacementRule.matchTag(tagId, replacement));
                } catch (Exception e) {
                    BrightbronzeHorizons.LOGGER.warn("Invalid match tag '{}' in {}", matchStr, id);
                }
            } else {
                try {
                    ResourceLocation matchId = ResourceLocation.parse(matchStr);
                    rules.add(BlockReplacementRule.matchBlock(matchId, replacement));
                } catch (Exception e) {
                    BrightbronzeHorizons.LOGGER.warn("Invalid match id '{}' in {}", matchStr, id);
                }
            }
        }

        return rules.isEmpty() ? List.of() : Collections.unmodifiableList(rules);
    }

    private static List<MobSpawnRule> parseMobSpawns(JsonObject root, ResourceLocation id) {
        JsonArray rulesJson = GsonHelper.getAsJsonArray(root, "mob_spawns", null);
        if (rulesJson == null) {
            return List.of();
        }

        List<MobSpawnRule> out = new ArrayList<>();
        for (JsonElement element : rulesJson) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject obj = element.getAsJsonObject();
            String entityStr = GsonHelper.getAsString(obj, "entity", "");
            if (entityStr.isEmpty()) {
                continue;
            }

            ResourceLocation entityId;
            try {
                entityId = ResourceLocation.parse(entityStr);
            } catch (Exception e) {
                BrightbronzeHorizons.LOGGER.warn("Invalid entity id '{}' in {}", entityStr, id);
                continue;
            }

            var typeRefOpt = BuiltInRegistries.ENTITY_TYPE.get(entityId);
            if (typeRefOpt.isEmpty()) {
                BrightbronzeHorizons.LOGGER.warn("Unknown entity type '{}' in {}", entityId, id);
                continue;
            }

            EntityType<?> type = typeRefOpt.get().value();
            int min = GsonHelper.getAsInt(obj, "min", 0);
            int max = GsonHelper.getAsInt(obj, "max", min);

            try {
                out.add(new MobSpawnRule(type, min, max));
            } catch (IllegalArgumentException e) {
                BrightbronzeHorizons.LOGGER.warn("Invalid mob spawn rule in {}: {}", id, e.getMessage());
            }
        }

        return out.isEmpty() ? List.of() : Collections.unmodifiableList(out);
    }

    private record TagKeyOrNull(ResourceLocation id) {
    }

    private record ResolvedBiome(ChunkSpawnerTier tier, int weight, List<BlockReplacementRule> replacements, List<MobSpawnRule> mobSpawns) {
    }

    private record ResolvedCache(int generation, int registryIdentity, Map<ResourceLocation, ResolvedBiome> byBiomeId, Map<ChunkSpawnerTier, WeightedBiomePool> poolsByTier) {
    }

    public record WeightedBiomeEntry(Holder<Biome> biome, int weight) {
    }

    public static final class WeightedBiomePool {
        private final List<WeightedBiomeEntry> entries;
        private final int totalWeight;

        private WeightedBiomePool(List<WeightedBiomeEntry> entries) {
            this.entries = entries;
            int sum = 0;
            for (WeightedBiomeEntry e : entries) {
                sum += Math.max(1, e.weight());
            }
            this.totalWeight = sum;
        }

        private WeightedBiomePool freeze() {
            return new WeightedBiomePool(Collections.unmodifiableList(entries));
        }

        public static WeightedBiomePool empty() {
            return new WeightedBiomePool(List.of());
        }

        public boolean isEmpty() {
            return entries.isEmpty() || totalWeight <= 0;
        }

        public int totalWeight() {
            return totalWeight;
        }

        public List<WeightedBiomeEntry> entries() {
            return entries;
        }

        public Optional<Holder<Biome>> selectByWeight(int roll) {
            if (isEmpty()) {
                return Optional.empty();
            }

            int r = Math.floorMod(roll, totalWeight);
            int acc = 0;
            for (WeightedBiomeEntry e : entries) {
                acc += Math.max(1, e.weight());
                if (r < acc) {
                    return Optional.of(e.biome());
                }
            }
            return Optional.of(entries.get(entries.size() - 1).biome());
        }
    }
}
