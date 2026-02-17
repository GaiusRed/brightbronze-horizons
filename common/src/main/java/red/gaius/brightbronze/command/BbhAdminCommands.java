package red.gaius.brightbronze.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import red.gaius.brightbronze.world.ChunkSpawnerTier;
import red.gaius.brightbronze.world.PlayableAreaData;
import red.gaius.brightbronze.world.chunk.ChunkExpansionManager;
import red.gaius.brightbronze.world.rules.BiomeRuleManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Phase 12: admin commands for forcing chunk spawns and inspecting tiers.
 */
public final class BbhAdminCommands {

    private BbhAdminCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("bbh:forceSpawn")
                .requires(source -> source.hasPermission(2))
                .then(
                    Commands.argument("biome", ResourceLocationArgument.id())
                        .then(
                            Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> forceSpawn(
                                    ctx.getSource(),
                                    ResourceLocationArgument.getId(ctx, "biome"),
                                    BlockPosArgument.getBlockPos(ctx, "pos"),
                                    null
                                ))
                                .then(
                                    Commands.argument("tier", StringArgumentType.word())
                                        .suggests(BbhAdminCommands::suggestTiers)
                                        .executes(ctx -> forceSpawn(
                                            ctx.getSource(),
                                            ResourceLocationArgument.getId(ctx, "biome"),
                                            BlockPosArgument.getBlockPos(ctx, "pos"),
                                            StringArgumentType.getString(ctx, "tier")
                                        ))
                                )
                        )
                )
        );

        dispatcher.register(
            Commands.literal("bbh:tierInfo")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> tierInfo(ctx.getSource(), null))
                .then(
                    Commands.argument("tier", StringArgumentType.word())
                        .suggests(BbhAdminCommands::suggestTiers)
                        .executes(ctx -> tierInfo(ctx.getSource(), StringArgumentType.getString(ctx, "tier")))
                )
        );
    }

    private static CompletableFuture<Suggestions> suggestTiers(CommandSourceStack source, SuggestionsBuilder builder) {
        List<String> names = new ArrayList<>();
        for (ChunkSpawnerTier tier : ChunkSpawnerTier.values()) {
            names.add(tier.getName());
        }
        return SharedSuggestionProvider.suggest(names, builder);
    }

    private static int forceSpawn(CommandSourceStack source, ResourceLocation biomeId, BlockPos pos, String tierName) {
        var biomeRegistry = source.getServer().registryAccess().lookupOrThrow(Registries.BIOME);
        if (biomeRegistry.get(biomeId).isEmpty()) {
            source.sendFailure(Component.literal("Unknown biome: " + biomeId));
            return 0;
        }

        ChunkSpawnerTier tier = tierName == null ? null : ChunkSpawnerTier.byName(tierName);
        if (tier == null) {
            tier = BiomeRuleManager.getResolvedTier(source.getServer().registryAccess(), biomeId);
        }
        if (tier == null) {
            tier = ChunkSpawnerTier.COPPER;
        }

        ServerLevel overworld = source.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
        if (overworld == null) {
            source.sendFailure(Component.literal("Overworld not loaded."));
            return 0;
        }

        ChunkPos targetChunk = new ChunkPos(pos);

        ServerPlayer player = null;
        UUID playerId = null;
        try {
            player = source.getPlayerOrException();
            playerId = player.getUUID();
        } catch (Exception ignored) {
            // console
        }

        String playerName = source.getTextName();

        ChunkExpansionManager.EnqueueResult result = ChunkExpansionManager.enqueue(
            overworld,
            null,
            tier,
            targetChunk,
            biomeId,
            playerId,
            playerName,
            false,
            false
        );

        if (!result.accepted()) {
            source.sendFailure(result.failureMessage());
            return 0;
        }

        source.sendSuccess(() -> Component.literal(
            "Enqueued forced spawn for chunk (" + targetChunk.x + ", " + targetChunk.z + ") biome " + biomeId + " (tier " + tier.getName() + ")"
        ), false);

        return 1;
    }

    private static int tierInfo(CommandSourceStack source, String tierName) {
        List<ChunkSpawnerTier> tiers = new ArrayList<>();
        if (tierName == null || tierName.isBlank()) {
            for (ChunkSpawnerTier t : ChunkSpawnerTier.values()) {
                tiers.add(t);
            }
        } else {
            ChunkSpawnerTier tier = ChunkSpawnerTier.byName(tierName);
            if (tier == null) {
                source.sendFailure(Component.literal("Unknown tier: " + tierName));
                return 0;
            }
            tiers.add(tier);
        }

        for (ChunkSpawnerTier tier : tiers) {
            if (tier == ChunkSpawnerTier.COAL) {
                source.sendSuccess(() -> Component.literal("Tier " + tier.getName() + ": local-biome expansion (no pool)"), false);
                continue;
            }

            var pool = BiomeRuleManager.getWeightedPool(source.getServer().registryAccess(), tier);
            if (pool.isEmpty()) {
                source.sendSuccess(() -> Component.literal("Tier " + tier.getName() + ": (no biomes)"), false);
                continue;
            }

            List<String> entries = new ArrayList<>();
            for (var entry : pool.entries()) {
                ResourceLocation id = red.gaius.brightbronze.world.BiomePoolManager.getBiomeId(entry.biome());
                if (id != null) {
                    entries.add(id + " x" + Math.max(1, entry.weight()));
                }
            }
            entries.sort(Comparator.comparing(s -> s.toLowerCase(Locale.ROOT)));

            source.sendSuccess(() -> Component.literal(
                "Tier " + tier.getName() + " (" + entries.size() + " biomes): " + String.join(", ", entries)
            ), false);
        }

        return 1;
    }
}
