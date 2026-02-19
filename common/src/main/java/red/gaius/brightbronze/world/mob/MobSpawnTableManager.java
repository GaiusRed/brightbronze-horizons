package red.gaius.brightbronze.world.mob;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.EntityType;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.versioned.Versioned;
import red.gaius.brightbronze.world.ChunkSpawnerTier;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Loads mob spawn tables from data packs.
 *
 * <p>Format: data/&lt;namespace&gt;/mob_spawns/{tier}.json
 */
public final class MobSpawnTableManager {

    private static final String BASE_PATH = "mob_spawns";

    private static volatile Map<ChunkSpawnerTier, List<MobSpawnRule>> rulesByTier = Map.of();

    private MobSpawnTableManager() {
    }

    public static List<MobSpawnRule> getRulesForTier(ChunkSpawnerTier tier) {
        List<MobSpawnRule> rules = rulesByTier.get(tier);
        return rules == null ? List.of() : rules;
    }

    public static void reload(ResourceManager resourceManager) {
        apply(loadTables(resourceManager));
    }

    static Map<ChunkSpawnerTier, List<MobSpawnRule>> loadTables(ResourceManager resourceManager) {
        Map<ChunkSpawnerTier, List<MobSpawnRule>> next = new EnumMap<>(ChunkSpawnerTier.class);

        for (var entry : resourceManager.listResources(BASE_PATH, id -> id.getPath().endsWith(".json")).entrySet()) {
            ResourceLocation id = entry.getKey();
            Resource resource = entry.getValue();

            String tierName = extractTierName(id);
            ChunkSpawnerTier tier = ChunkSpawnerTier.byName(tierName);
            if (tier == null) {
                BrightbronzeHorizons.LOGGER.warn("Ignoring mob spawn table with unknown tier '{}' at {}", tierName, id);
                continue;
            }

            List<MobSpawnRule> parsed = parseRules(id, resource);
            if (!parsed.isEmpty()) {
                next.put(tier, Collections.unmodifiableList(parsed));
            }
        }

        // Ensure all tiers have a value to simplify callers.
        for (ChunkSpawnerTier tier : ChunkSpawnerTier.values()) {
            next.putIfAbsent(tier, List.of());
        }

        return Map.copyOf(next);
    }

    static void apply(Map<ChunkSpawnerTier, List<MobSpawnRule>> loaded) {
        rulesByTier = loaded;

        if (BrightbronzeHorizons.LOGGER.isDebugEnabled()) {
            for (ChunkSpawnerTier tier : ChunkSpawnerTier.values()) {
                BrightbronzeHorizons.LOGGER.debug(
                    "Loaded {} mob spawn rules for tier {}",
                    rulesByTier.get(tier).size(),
                    tier.getName()
                );
            }
        }
    }

    private static String extractTierName(ResourceLocation id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        String file = (slash >= 0) ? path.substring(slash + 1) : path;
        if (file.endsWith(".json")) {
            file = file.substring(0, file.length() - 5);
        }
        return file;
    }

    private static List<MobSpawnRule> parseRules(ResourceLocation id, Resource resource) {
        try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray rulesJson = GsonHelper.getAsJsonArray(root, "rules", new JsonArray());

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

                var typeOpt = Versioned.registry().getEntityType(entityId);
                if (typeOpt.isEmpty()) {
                    BrightbronzeHorizons.LOGGER.warn("Unknown entity type '{}' in {}", entityId, id);
                    continue;
                }

                EntityType<?> type = typeOpt.get();
                int min = GsonHelper.getAsInt(obj, "min", 0);
                int max = GsonHelper.getAsInt(obj, "max", min);

                try {
                    out.add(new MobSpawnRule(type, min, max));
                } catch (IllegalArgumentException e) {
                    BrightbronzeHorizons.LOGGER.warn("Invalid mob spawn rule in {}: {}", id, e.getMessage());
                }
            }

            return out;
        } catch (IOException e) {
            BrightbronzeHorizons.LOGGER.warn("Failed reading mob spawn table {}: {}", id, e.toString());
            return List.of();
        } catch (Exception e) {
            BrightbronzeHorizons.LOGGER.warn("Failed parsing mob spawn table {}: {}", id, e.toString());
            return List.of();
        }
    }
}
