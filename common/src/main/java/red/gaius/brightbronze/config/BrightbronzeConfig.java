package red.gaius.brightbronze.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.world.ChunkSpawnerTier;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Runtime (server/world) config.
 *
 * <p>This is intentionally small and lives under the game's config directory.
 * It is not data-pack reloadable; it is loaded on server start.
 */
public final class BrightbronzeConfig {

    public static final String FILE_NAME = "brightbronze_horizons.json";

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    private static volatile Data active = Data.defaults();

    private BrightbronzeConfig() {
    }

    public static Data get() {
        return active;
    }

    public static String toNetworkJson() {
        try {
            return GSON.toJson(active);
        } catch (Exception e) {
            return null;
        }
    }

    public static void applyNetworkJson(String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            Data loaded = GSON.fromJson(json, Data.class);
            if (loaded == null) {
                throw new JsonParseException("Config payload parsed to null");
            }
            loaded.normalize();
            active = loaded;
            BrightbronzeHorizons.LOGGER.info("Applied synced config from server");
        } catch (Exception e) {
            BrightbronzeHorizons.LOGGER.warn("Failed to apply synced config payload: {}", e.getMessage());
        }
    }

    public static void loadOrCreate(MinecraftServer server) {
        Path configPath = ConfigPaths.getConfigDir().resolve(FILE_NAME);

        if (!Files.exists(configPath)) {
            Data defaults = Data.defaults();
            try {
                Files.createDirectories(configPath.getParent());
                try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                    GSON.toJson(defaults, writer);
                }
            } catch (IOException e) {
                BrightbronzeHorizons.LOGGER.warn("Failed to write default config {}: {}", configPath, e.getMessage());
            }
            active = defaults;
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded == null) {
                throw new JsonParseException("Config file parsed to null");
            }
            loaded.normalize();
            active = loaded;
            BrightbronzeHorizons.LOGGER.info("Loaded config from {}", configPath);
        } catch (Exception e) {
            BrightbronzeHorizons.LOGGER.warn("Failed to read config {} (using defaults): {}", configPath, e.getMessage());
            active = Data.defaults();
        }
    }

    public static boolean isTierEnabled(ChunkSpawnerTier tier) {
        Boolean enabled = active.tiersEnabled.get(tier.name());
        return enabled == null || enabled;
    }

    public static final class Data {
        /** If false, starting area initialization is skipped (world will remain void unless another system populates it). */
        public boolean enableStartingArea = true;

        /** If true, starting area attempts to center on a village (best-effort bounded search). */
        public boolean preferVillageStart = true;

        /** If false, scripted mob spawns on chunk spawn are disabled. */
        public boolean enableChunkSpawnMobs = true;

        /** Phase 11: max vertical layers (Y-levels) copied per tick during chunk spawn to reduce hitching. */
        public int chunkCopyLayersPerTick = 8;

        /** Phase 11: max number of source dimensions to create (0 = unlimited). */
        public int maxSourceDimensions = 0;

        /** Per-tier enable/disable. Keys are tier enum names (e.g. "COPPER"). */
        public Map<String, Boolean> tiersEnabled = new HashMap<>();

        /** Optional future: allow packs to change tier block mappings; unused for now. */
        public Map<String, String> tierBlockOverrides = Map.of();

        /** Optional: config versioning to allow migration later. */
        public int version = 1;

        public static Data defaults() {
            Data data = new Data();
            for (ChunkSpawnerTier tier : ChunkSpawnerTier.values()) {
                data.tiersEnabled.put(tier.name(), true);
            }
            return data;
        }

        private void normalize() {
            if (tiersEnabled == null) {
                tiersEnabled = new HashMap<>();
            }
            for (ChunkSpawnerTier tier : ChunkSpawnerTier.values()) {
                tiersEnabled.putIfAbsent(tier.name(), true);
            }
            if (tierBlockOverrides == null) {
                tierBlockOverrides = Map.of();
            }

            if (chunkCopyLayersPerTick <= 0) {
                chunkCopyLayersPerTick = 8;
            }

            if (maxSourceDimensions < 0) {
                maxSourceDimensions = 0;
            }
        }

        public ResourceLocation getTierBlockOverride(ChunkSpawnerTier tier) {
            if (tierBlockOverrides == null) {
                return null;
            }
            String value = tierBlockOverrides.get(tier.name());
            if (value == null || value.isBlank()) {
                return null;
            }
            try {
                return ResourceLocation.parse(value);
            } catch (Exception e) {
                return null;
            }
        }
    }
}
