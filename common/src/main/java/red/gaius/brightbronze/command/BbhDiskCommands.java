package red.gaius.brightbronze.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.storage.LevelResource;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.registry.ModDimensions;
import red.gaius.brightbronze.world.PlayableAreaData;
import red.gaius.brightbronze.world.dimension.SourceDimensionManager;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Phase 11: disk management and usage reporting for source dimensions.
 */
public final class BbhDiskCommands {

    private BbhDiskCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bbh:sourceUsage")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> sourceUsage(ctx.getSource()))
        );

        dispatcher.register(
            Commands.literal("bbh:pruneSources")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> pruneSources(ctx.getSource()))
        );
    }

    private static int sourceUsage(CommandSourceStack source) {
        MinecraftServer server = source.getServer();

        Set<ResourceLocation> biomeIds = new HashSet<>();
        biomeIds.addAll(PlayableAreaData.get(server).getRecordedBiomeIds());
        biomeIds.addAll(SourceDimensionManager.getActiveBiomeIds());

        if (biomeIds.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No source biomes recorded yet."), false);
            return 1;
        }

        long totalBytes = 0L;
        for (ResourceLocation biomeId : biomeIds) {
            Path dimDir = getSourceDimensionDir(server, biomeId);
            long bytes = directorySizeBytes(dimDir);
            totalBytes += bytes;

            source.sendSuccess(
                () -> Component.literal(biomeId + ": " + humanBytes(bytes)),
                false
            );
        }

        long finalTotalBytes = totalBytes;
        source.sendSuccess(() -> Component.literal("Total: " + humanBytes(finalTotalBytes)), false);
        return 1;
    }

    private static int pruneSources(CommandSourceStack source) {
        MinecraftServer server = source.getServer();

        // Compute used region files per biome from persisted spawned-chunk metadata.
        Map<ResourceLocation, Set<String>> keepByBiome = new HashMap<>();
        for (var meta : PlayableAreaData.get(server).getSpawnedChunkMeta()) {
            ResourceLocation biomeId = meta.biome();
            ChunkPos pos = meta.chunk();
            int rx = Math.floorDiv(pos.x, 32);
            int rz = Math.floorDiv(pos.z, 32);
            String file = "r." + rx + "." + rz + ".mca";
            keepByBiome.computeIfAbsent(biomeId, ignored -> new HashSet<>()).add(file);
        }

        if (keepByBiome.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No spawned-chunk metadata found; nothing to prune."), false);
            return 1;
        }

        int deleted = 0;
        int kept = 0;
        int missing = 0;

        for (var entry : keepByBiome.entrySet()) {
            ResourceLocation biomeId = entry.getKey();
            Set<String> keepFiles = entry.getValue();

            Path regionDir = getSourceDimensionDir(server, biomeId).resolve("region");
            if (!Files.isDirectory(regionDir)) {
                missing++;
                continue;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(regionDir, "r.*.*.mca")) {
                for (Path file : stream) {
                    String name = file.getFileName().toString();
                    if (keepFiles.contains(name)) {
                        kept++;
                        continue;
                    }

                    try {
                        Files.deleteIfExists(file);
                        deleted++;
                    } catch (IOException e) {
                        BrightbronzeHorizons.LOGGER.warn("Failed to delete {}: {}", file, e.getMessage());
                    }
                }
            } catch (IOException e) {
                BrightbronzeHorizons.LOGGER.warn("Failed to scan region dir {}: {}", regionDir, e.getMessage());
            }
        }

        int finalDeleted = deleted;
        int finalKept = kept;
        int finalMissing = missing;
        source.sendSuccess(
            () -> Component.literal("Prune complete. Deleted " + finalDeleted + " region files; kept " + finalKept + ". Missing region dirs: " + finalMissing),
            true
        );

        return 1;
    }

    private static Path getSourceDimensionDir(MinecraftServer server, ResourceLocation biomeId) {
        // Vanilla stores custom dimensions under <world>/dimensions/<namespace>/<path>/
        // Our source dimension id is brightbronze_horizons:source/<biome_namespace>/<biome_path>
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path dimensionsDir = worldRoot.resolve("dimensions");

        ResourceLocation dimId = ModDimensions.getSourceDimensionKey(biomeId).location();
        return dimensionsDir
            .resolve(dimId.getNamespace())
            .resolve(dimId.getPath());
    }

    private static long directorySizeBytes(Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return 0L;
        }

        try (var stream = Files.walk(dir)) {
            return stream
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0L;
                    }
                })
                .sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    private static String humanBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KiB", "MiB", "GiB", "TiB"};
        int unitIndex = -1;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", value, units[unitIndex]);
    }
}
