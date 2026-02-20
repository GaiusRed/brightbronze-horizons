package red.gaius.brightbronze.world.chunk;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.config.BrightbronzeConfig;
import red.gaius.brightbronze.world.ChunkSpawnerTier;
import red.gaius.brightbronze.world.PlayableAreaData;
import red.gaius.brightbronze.world.dimension.SourceDimensionManager;
import red.gaius.brightbronze.world.mob.ChunkSpawnMobEvent;
import red.gaius.brightbronze.world.rules.BiomeRuleManager;
import red.gaius.brightbronze.world.rules.BlockReplacementRule;
import red.gaius.brightbronze.versioned.Versioned;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Phase 10/11: central manager for chunk expansions.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>De-dupe concurrent requests for the same target chunk</li>
 *   <li>Serialize (or bound) work to avoid server stalls</li>
 *   <li>Run all world operations on the server thread</li>
 * </ul>
 */
public final class ChunkExpansionManager {

    private static final Deque<ExpansionRequest> QUEUE = new ArrayDeque<>();
    private static final Map<Long, ExpansionRequest> IN_FLIGHT_BY_CHUNK = new HashMap<>();

    private static final ResourceLocation FIRST_CHUNK_ADVANCEMENT =
        ResourceLocation.fromNamespaceAndPath(BrightbronzeHorizons.MOD_ID, "first_chunk_spawn");

    @Nullable
    private static ActiveJob activeJob;

    private static boolean tickHookRegistered;

    private ChunkExpansionManager() {
    }

    public static void init() {
        if (tickHookRegistered) {
            return;
        }
        tickHookRegistered = true;

        // Process bounded work each server tick.
        TickEvent.SERVER_POST.register(ChunkExpansionManager::tick);
    }

    public static EnqueueResult enqueue(ServerLevel overworld,
                                       BlockPos spawnerPos,
                                       ChunkSpawnerTier tier,
                                       ChunkPos targetChunk,
                                       ResourceLocation biomeId,
                                       @Nullable UUID playerId,
                                       @Nullable String playerName) {

        return enqueue(overworld, spawnerPos, tier, targetChunk, biomeId, playerId, playerName, true, true, false, null);
    }

    public static EnqueueResult enqueue(ServerLevel overworld,
                                       @Nullable BlockPos spawnerPos,
                                       ChunkSpawnerTier tier,
                                       ChunkPos targetChunk,
                                       ResourceLocation biomeId,
                                       @Nullable UUID playerId,
                                       @Nullable String playerName,
                                       boolean enforceAdjacency,
                                       boolean breakSpawnerOnSuccess) {

        return enqueue(overworld, spawnerPos, tier, targetChunk, biomeId, playerId, playerName, enforceAdjacency, breakSpawnerOnSuccess, false, null);
    }

    public static EnqueueResult enqueue(ServerLevel overworld,
                                       @Nullable BlockPos spawnerPos,
                                       ChunkSpawnerTier tier,
                                       ChunkPos targetChunk,
                                       ResourceLocation biomeId,
                                       @Nullable UUID playerId,
                                       @Nullable String playerName,
                                       boolean enforceAdjacency,
                                       boolean breakSpawnerOnSuccess,
                                       boolean structureTriggered,
                                       @Nullable ChunkPos triggeringChunk) {

        MinecraftServer server = overworld.getServer();

        // Always enqueue from the server thread. The caller should already be on-server,
        // but keep this defensive to avoid races.
        if (!server.isSameThread()) {
            server.execute(() -> enqueue(overworld, spawnerPos, tier, targetChunk, biomeId, playerId, playerName));
            return EnqueueResult.createAccepted();
        }

        PlayableAreaData playableData = PlayableAreaData.get(server);
        if (playableData.isChunkPlayable(targetChunk)) {
            return EnqueueResult.createFailure(Component.translatable("message.brightbronze_horizons.spawner.already_spawned"));
        }
        if (enforceAdjacency && !playableData.canExpandInto(targetChunk)) {
            return EnqueueResult.createFailure(Component.translatable("message.brightbronze_horizons.spawner.not_adjacent"));
        }

        long key = chunkKey(targetChunk);
        if (IN_FLIGHT_BY_CHUNK.containsKey(key)) {
            return EnqueueResult.createFailure(Component.translatable("message.brightbronze_horizons.spawner.in_progress"));
        }

        ExpansionRequest request = new ExpansionRequest(
            overworld.dimension().location(),
            spawnerPos == null ? null : spawnerPos.immutable(),
            breakSpawnerOnSuccess,
            tier,
            targetChunk,
            biomeId,
            playerId,
            playerName,
            structureTriggered,
            triggeringChunk
        );

        // Break the spawner IMMEDIATELY upon accepting the request (not after chunk copies)
        // This gives instant feedback that the action was accepted.
        if (!structureTriggered && breakSpawnerOnSuccess && spawnerPos != null) {
            BlockState state = overworld.getBlockState(spawnerPos);
            if (!state.isAir()) {
                overworld.destroyBlock(spawnerPos, true);
            }
        }

        IN_FLIGHT_BY_CHUNK.put(key, request);
        QUEUE.addLast(request);
        return EnqueueResult.createAccepted();
    }

    private static void tick(MinecraftServer server) {
        if (activeJob == null) {
            ExpansionRequest next = QUEUE.pollFirst();
            if (next == null) {
                return;
            }

            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) {
                finishRequest(next, server, false, Component.literal("Overworld not loaded"), null, ExpansionResult.failure());
                return;
            }

            activeJob = startJob(overworld, next);
            if (activeJob == null) {
                finishRequest(next, server, false, Component.translatable("message.brightbronze_horizons.spawner.copy_failed"), null, ExpansionResult.failure());
            }
        }

        if (activeJob != null) {
            boolean done = activeJob.tick(server);
            if (done) {
                activeJob = null;
            }
        }
    }

    @Nullable
    private static ActiveJob startJob(ServerLevel overworld, ExpansionRequest request) {
        var biomeRegistry = Versioned.registry().lookupRegistry(overworld.registryAccess(), Registries.BIOME);
        Optional<Holder<Biome>> biomeHolderOpt = Versioned.registry().getHolder(biomeRegistry, request.biomeId);
        if (biomeHolderOpt.isEmpty()) {
            return null;
        }

        ServerLevel sourceLevel = SourceDimensionManager.getOrCreateSourceDimension(overworld.getServer(), request.biomeId);
        if (sourceLevel == null) {
            return null;
        }

        // Source coords match target coords per PRD.
        ChunkPos sourceChunkPos = request.targetChunk;

        var replacementRules = BiomeRuleManager.getReplacementRules(overworld.registryAccess(), request.biomeId);
        ChunkCopyService.ChunkCopyJob job = ChunkCopyService.createJob(
            sourceLevel,
            sourceChunkPos,
            overworld,
            request.targetChunk,
            biomeHolderOpt.get(),
            replacementRules
        );
        return new ActiveJob(request, job, sourceLevel);
    }

    /**
     * Result of a completed expansion, including structure completion information.
     */
    public record ExpansionResult(
            boolean success,
            int structureCount,
            int structureChunksSpawned,
            boolean hitStructureLimit,
            boolean hitChunkLimit,
            int skippedExistingChunks,
            String structureNames
    ) {
        public static ExpansionResult failure() {
            return new ExpansionResult(false, 0, 0, false, false, 0, "");
        }

        public static ExpansionResult simpleSuccess() {
            return new ExpansionResult(true, 0, 0, false, false, 0, "");
        }

        /**
         * @return true if structure was only partially materialized
         */
        public boolean isPartial() {
            return skippedExistingChunks > 0 || hitStructureLimit || hitChunkLimit;
        }
    }

    private static void finishRequest(ExpansionRequest request,
                                      MinecraftServer server,
                                      boolean success,
                                      @Nullable Component failure,
                                      @Nullable ResourceLocation biomeId,
                                      ExpansionResult expansionResult) {
        IN_FLIGHT_BY_CHUNK.remove(chunkKey(request.targetChunk));

        if (!success) {
            // Only notify on failure for non-structure-triggered chunks
            if (!request.structureTriggered) {
                notifyPlayer(server, request.playerId, failure != null ? failure : Component.translatable("message.brightbronze_horizons.spawner.copy_failed"));
            }
            return;
        }

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }

        // Update persistent state.
        PlayableAreaData data = PlayableAreaData.get(server);
        data.addChunk(request.targetChunk);
        data.recordSpawnedChunk(
            request.targetChunk,
            biomeId,
            request.tier.getName(),
            request.structureTriggered,
            request.triggeringChunk
        );

        // Phase 7: one-time scripted mob spawns when a chunk is revealed.
        if (BrightbronzeConfig.get().enableChunkSpawnMobs) {
            ChunkSpawnMobEvent.fire(overworld, request.targetChunk, request.tier);
        }

        // Note: Spawner is broken immediately in enqueue(), not here.

        // Phase 12: visual/audio feedback on successful spawn.
        spawnSuccessEffects(overworld, request.targetChunk);

        // Phase 12: optional toast on first chunk spawn for the player (only for non-structure-triggered).
        if (!request.structureTriggered) {
            awardFirstChunkAdvancement(server, request.playerId);
        }

        // PRD: announce success serverwide — but NOT for structure-triggered chunks.
        // Structure completion has its own announcements handled in handleStructureCompletion().
        if (!request.structureTriggered) {
            String who = request.playerName != null ? request.playerName : "Someone";

            // Simple announcement for the primary chunk
            // Structure details are announced separately by handleStructureCompletion()
            Component announcement = Component.translatable(
                "message.brightbronze_horizons.spawner.announce",
                Component.literal(who),
                request.tier.getName(),
                request.targetChunk.x,
                request.targetChunk.z,
                biomeId.toString()
            );

            overworld.getServer().getPlayerList().broadcastSystemMessage(announcement, false);

            // Local confirmation if player is online.
            notifyPlayer(server, request.playerId,
                Component.translatable(
                    "message.brightbronze_horizons.spawner.success",
                    request.tier.getName(),
                    request.targetChunk.x,
                    request.targetChunk.z,
                    biomeId.toString()
                )
            );
        }

        BrightbronzeHorizons.LOGGER.info(
            "Spawned {} tier chunk at ({}, {}) biome {}{} (requested by {})",
            request.tier.getName(), request.targetChunk.x, request.targetChunk.z, biomeId,
            request.structureTriggered ? " [structure-triggered]" : "",
            request.playerName != null ? request.playerName : "unknown"
        );
    }

    private static void notifyPlayer(MinecraftServer server, @Nullable UUID playerId, Component message) {
        if (playerId == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }
        player.displayClientMessage(message, true);
    }

    private static long chunkKey(ChunkPos pos) {
        return (((long) pos.x) << 32) ^ (pos.z & 0xFFFFFFFFL);
    }

    private static void spawnSuccessEffects(ServerLevel level, ChunkPos chunkPos) {
        int x = chunkPos.getMiddleBlockX();
        int z = chunkPos.getMiddleBlockZ();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        double fx = x + 0.5;
        double fy = Math.max(Versioned.level().getMinY(level) + 1, y + 1);
        double fz = z + 0.5;

        level.sendParticles(ParticleTypes.PORTAL, fx, fy, fz, 120, 4.0, 2.5, 4.0, 0.15);
        level.sendParticles(ParticleTypes.CLOUD, fx, fy, fz, 40, 2.5, 0.8, 2.5, 0.01);

        Versioned.sound().playSound(level, fx, fy, fz, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.9F, 1.2F);
    }

    private static void awardFirstChunkAdvancement(MinecraftServer server, @Nullable UUID playerId) {
        if (playerId == null) {
            return;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            return;
        }

        AdvancementHolder advancement = server.getAdvancements().get(FIRST_CHUNK_ADVANCEMENT);
        if (advancement == null) {
            return;
        }

        var progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            return;
        }

        for (String criterion : advancement.value().criteria().keySet()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private record ExpansionRequest(ResourceLocation overworldId,
                                    @Nullable BlockPos spawnerPos,
                                    boolean breakSpawnerOnSuccess,
                                    ChunkSpawnerTier tier,
                                    ChunkPos targetChunk,
                                    ResourceLocation biomeId,
                                    @Nullable UUID playerId,
                                    @Nullable String playerName,
                                    boolean structureTriggered,
                                    @Nullable ChunkPos triggeringChunk) {
    }

    public record EnqueueResult(boolean accepted, @Nullable Component failureMessage) {
        static EnqueueResult createAccepted() {
            return new EnqueueResult(true, null);
        }

        static EnqueueResult createFailure(Component message) {
            return new EnqueueResult(false, message);
        }
    }

    private static final class ActiveJob {
        private final ExpansionRequest request;
        private final ChunkCopyService.ChunkCopyJob job;
        private final ServerLevel sourceLevel;

        private ActiveJob(ExpansionRequest request, ChunkCopyService.ChunkCopyJob job, ServerLevel sourceLevel) {
            this.request = request;
            this.job = job;
            this.sourceLevel = sourceLevel;
        }

        /** @return true when complete (success or failure) */
        public boolean tick(MinecraftServer server) {
            int layersPerTick = Math.max(1, BrightbronzeConfig.get().chunkCopyLayersPerTick);
            ChunkCopyService.ChunkCopyJob.Result result = job.tick(layersPerTick);
            if (!result.done()) {
                return false;
            }

            if (!result.success()) {
                finishRequest(request, server, false, Component.translatable("message.brightbronze_horizons.spawner.copy_failed"), null, ExpansionResult.failure());
                return true;
            }

            // Structure completion: only for non-structure-triggered chunks
            ExpansionResult expansionResult = ExpansionResult.simpleSuccess();
            if (!request.structureTriggered && BrightbronzeConfig.get().enableStructureCompletion) {
                expansionResult = handleStructureCompletion(server);
            }

            finishRequest(request, server, true, null, request.biomeId, expansionResult);
            return true;
        }

        private ExpansionResult handleStructureCompletion(MinecraftServer server) {
            PlayableAreaData playableData = PlayableAreaData.get(server);
            Set<ChunkPos> alreadySpawned = new HashSet<>(playableData.getSpawnedChunks());
            // Also consider chunks currently in flight
            alreadySpawned.addAll(IN_FLIGHT_BY_CHUNK.values().stream()
                    .map(req -> req.targetChunk)
                    .toList());

            StructureCompletionService.StructureCompletionResult structureResult =
                    StructureCompletionService.collectStructureCompletionChunks(
                            sourceLevel,
                            request.targetChunk,
                            alreadySpawned
                    );

            if (!structureResult.hasChunksToSpawn()) {
                return ExpansionResult.simpleSuccess();
            }

            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) {
                return ExpansionResult.simpleSuccess();
            }

            // Announce that structure completion is starting
            String structureNames = structureResult.formattedStructureNames();
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable(
                            "message.brightbronze_horizons.spawner.structure_materializing",
                            structureNames,
                            structureResult.chunksToSpawn().size()
                    ),
                    false
            );

            // Get biome holder and replacement rules for synchronous copying
            var biomeRegistry = Versioned.registry().lookupRegistry(overworld.registryAccess(), Registries.BIOME);
            Optional<Holder<Biome>> biomeHolderOpt = Versioned.registry().getHolder(biomeRegistry, request.biomeId);
            if (biomeHolderOpt.isEmpty()) {
                BrightbronzeHorizons.LOGGER.warn("Cannot complete structures: biome {} not found", request.biomeId);
                return ExpansionResult.simpleSuccess();
            }
            Holder<Biome> biomeHolder = biomeHolderOpt.get();
            List<BlockReplacementRule> replacementRules = BiomeRuleManager.getReplacementRules(overworld.registryAccess(), request.biomeId);

            // Copy ALL structure chunks SYNCHRONOUSLY (not queued)
            // This ensures they all appear at once rather than gradually
            int copiedCount = 0;
            int skippedCount = 0;
            
            for (ChunkPos structureChunk : structureResult.chunksToSpawn()) {
                // Double-check: skip if already playable (race condition guard)
                if (playableData.isChunkPlayable(structureChunk)) {
                    skippedCount++;
                    continue;
                }

                // Synchronous chunk copy
                boolean success = ChunkCopyService.copyChunk(
                        sourceLevel,
                        structureChunk,
                        overworld,
                        structureChunk,
                        biomeHolder,
                        replacementRules
                );

                if (success) {
                    // Register with playable area
                    playableData.addChunk(structureChunk);
                    playableData.recordSpawnedChunk(
                            structureChunk,
                            request.biomeId,
                            request.tier.getName(),
                            true, // structure-triggered
                            request.targetChunk // triggering chunk
                    );
                    
                    // Visual effects for each structure chunk
                    spawnSuccessEffects(overworld, structureChunk);
                    
                    // Mob spawns for structure chunks too
                    if (BrightbronzeConfig.get().enableChunkSpawnMobs) {
                        ChunkSpawnMobEvent.fire(overworld, structureChunk, request.tier);
                    }
                    
                    copiedCount++;
                } else {
                    skippedCount++;
                    BrightbronzeHorizons.LOGGER.warn(
                            "Failed to copy structure chunk ({}, {})",
                            structureChunk.x, structureChunk.z
                    );
                }
            }

            // Add skipped chunks from structure detection (chunks that already existed)
            skippedCount += structureResult.skippedExistingChunks();

            BrightbronzeHorizons.LOGGER.info(
                    "Structure completion from chunk {}: {} ({}) - {} chunks copied, {} skipped",
                    request.targetChunk, structureResult.structureCount(), structureNames,
                    copiedCount, skippedCount
            );

            // Announce structure completion
            boolean isPartial = skippedCount > 0 || structureResult.hitStructureLimit() || structureResult.hitChunkLimit();
            Component completionMessage;
            if (isPartial) {
                completionMessage = Component.translatable(
                        "message.brightbronze_horizons.spawner.structure_partial",
                        structureNames,
                        request.targetChunk.x,
                        request.targetChunk.z,
                        copiedCount
                );
            } else {
                completionMessage = Component.translatable(
                        "message.brightbronze_horizons.spawner.structure_complete",
                        structureNames,
                        request.targetChunk.x,
                        request.targetChunk.z
                );
            }
            server.getPlayerList().broadcastSystemMessage(completionMessage, false);

            return new ExpansionResult(
                    true,
                    structureResult.structureCount(),
                    copiedCount,
                    structureResult.hitStructureLimit(),
                    structureResult.hitChunkLimit(),
                    skippedCount,
                    structureNames
            );
        }
    }
}
