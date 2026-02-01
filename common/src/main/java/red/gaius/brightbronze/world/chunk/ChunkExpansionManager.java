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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

        MinecraftServer server = overworld.getServer();

        // Always enqueue from the server thread. The caller should already be on-server,
        // but keep this defensive to avoid races.
        if (!server.isSameThread()) {
            server.execute(() -> enqueue(overworld, spawnerPos, tier, targetChunk, biomeId, playerId, playerName));
            return EnqueueResult.createAccepted();
        }

        PlayableAreaData playableData = PlayableAreaData.get(server);
        if (!playableData.canExpandInto(targetChunk)) {
            if (playableData.isChunkPlayable(targetChunk)) {
                return EnqueueResult.createFailure(Component.translatable("message.brightbronze_horizons.spawner.already_spawned"));
            }
            return EnqueueResult.createFailure(Component.translatable("message.brightbronze_horizons.spawner.not_adjacent"));
        }

        long key = chunkKey(targetChunk);
        if (IN_FLIGHT_BY_CHUNK.containsKey(key)) {
            return EnqueueResult.createFailure(Component.translatable("message.brightbronze_horizons.spawner.in_progress"));
        }

        ExpansionRequest request = new ExpansionRequest(
            overworld.dimension().location(),
            spawnerPos.immutable(),
            tier,
            targetChunk,
            biomeId,
            playerId,
            playerName
        );

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
                finishRequest(next, server, false, Component.literal("Overworld not loaded"), null);
                return;
            }

            activeJob = startJob(overworld, next);
            if (activeJob == null) {
                finishRequest(next, server, false, Component.translatable("message.brightbronze_horizons.spawner.copy_failed"), null);
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
        var biomeRegistry = overworld.registryAccess().lookupOrThrow(Registries.BIOME);
        Optional<Holder.Reference<Biome>> biomeHolderOpt = biomeRegistry.get(request.biomeId);
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
        return new ActiveJob(request, job);
    }

    private static void finishRequest(ExpansionRequest request,
                                      MinecraftServer server,
                                      boolean success,
                                      @Nullable Component failure,
                                      @Nullable ResourceLocation biomeId) {
        IN_FLIGHT_BY_CHUNK.remove(chunkKey(request.targetChunk));

        if (!success) {
            notifyPlayer(server, request.playerId, failure != null ? failure : Component.translatable("message.brightbronze_horizons.spawner.copy_failed"));
            return;
        }

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }

        // Update persistent state.
        PlayableAreaData data = PlayableAreaData.get(server);
        data.addChunk(request.targetChunk);
        data.recordSpawnedChunk(request.targetChunk, biomeId, request.tier.getName());

        // Phase 7: one-time scripted mob spawns when a chunk is revealed.
        if (BrightbronzeConfig.get().enableChunkSpawnMobs) {
            ChunkSpawnMobEvent.fire(overworld, request.targetChunk, request.tier);
        }

        // Break the spawner only on success (PRD).
        BlockState state = overworld.getBlockState(request.spawnerPos);
        if (!state.isAir()) {
            overworld.destroyBlock(request.spawnerPos, true);
        }

        // PRD: announce success serverwide.
        String who = request.playerName != null ? request.playerName : "Someone";
        overworld.getServer().getPlayerList().broadcastSystemMessage(
            Component.translatable(
                "message.brightbronze_horizons.spawner.announce",
                Component.literal(who),
                request.tier.getName(),
                request.targetChunk.x,
                request.targetChunk.z,
                biomeId.toString()
            ),
            false
        );

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

        BrightbronzeHorizons.LOGGER.info(
            "Spawned {} tier chunk at ({}, {}) biome {} (requested by {})",
            request.tier.getName(), request.targetChunk.x, request.targetChunk.z, biomeId,
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

    private record ExpansionRequest(ResourceLocation overworldId,
                                    BlockPos spawnerPos,
                                    ChunkSpawnerTier tier,
                                    ChunkPos targetChunk,
                                    ResourceLocation biomeId,
                                    @Nullable UUID playerId,
                                    @Nullable String playerName) {
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

        private ActiveJob(ExpansionRequest request, ChunkCopyService.ChunkCopyJob job) {
            this.request = request;
            this.job = job;
        }

        /** @return true when complete (success or failure) */
        public boolean tick(MinecraftServer server) {
            int layersPerTick = Math.max(1, BrightbronzeConfig.get().chunkCopyLayersPerTick);
            ChunkCopyService.ChunkCopyJob.Result result = job.tick(layersPerTick);
            if (!result.done()) {
                return false;
            }

            if (!result.success()) {
                finishRequest(request, server, false, Component.translatable("message.brightbronze_horizons.spawner.copy_failed"), null);
                return true;
            }

            finishRequest(request, server, true, null, request.biomeId);
            return true;
        }
    }
}
