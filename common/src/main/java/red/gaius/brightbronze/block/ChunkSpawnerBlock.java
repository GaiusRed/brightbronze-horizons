package red.gaius.brightbronze.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.config.BrightbronzeConfig;
import red.gaius.brightbronze.world.BiomePoolManager;
import red.gaius.brightbronze.world.ChunkSpawnerTier;
import red.gaius.brightbronze.world.PlayableAreaData;
import red.gaius.brightbronze.world.chunk.ChunkExpansionManager;
import red.gaius.brightbronze.world.compat.ModdedBiomeDetector;
import red.gaius.brightbronze.world.rules.BiomeRuleManager;
import red.gaius.brightbronze.world.rules.BiomeRuleManager.WeightedBiomePool;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base class for all chunk spawner blocks.
 * 
 * <p>Chunk spawners must be placed at the edge of a chunk to indicate the
 * expansion direction. When activated (right-clicked), they spawn a new
 * terrain chunk from the biome pool associated with their tier.
 * 
 * <p>Each tier has different biome pools and mob spawning rules:
 * <ul>
 *   <li>Coal: Spawns the same biome as where the anchor is placed</li>
 *   <li>Copper: Common safe Overworld biomes, mobs spawn at night only</li>
 *   <li>Iron: Rare Overworld biomes (Jungle, Badlands, etc.), mobs spawn at night only</li>
 *   <li>Gold: Nether biomes, mobs always spawn</li>
 *   <li>Emerald: Modded biomes (auto-detected from worldgen mods), mobs always spawn</li>
 *   <li>Diamond: End biomes, mobs always spawn</li>
 * </ul>
 * 
 * <p>Note: This block does not use a BlockEntity because all spawning logic
 * is handled synchronously on right-click. A BlockEntity may be added later
 * for visual effects (particles, glow) or cooldown timers.
 */
public class ChunkSpawnerBlock extends Block {

    private final ChunkSpawnerTier tier;

    public ChunkSpawnerBlock(Properties properties, ChunkSpawnerTier tier) {
        super(properties);
        this.tier = tier;
    }

    /**
     * @return The tier of this chunk spawner
     */
    public ChunkSpawnerTier getTier() {
        return tier;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!BrightbronzeConfig.isTierEnabled(tier)) {
            player.displayClientMessage(
                Component.translatable("message.brightbronze_horizons.spawner.tier_disabled", tier.getName()),
                true
            );
            return InteractionResult.FAIL;
        }

        ServerLevel serverLevel = (ServerLevel) level;
        
        // Check if at chunk edge
        List<Direction> edgeDirections = getChunkEdgeDirections(pos);
        
        if (edgeDirections.isEmpty()) {
            // Not at chunk edge - inform player
            player.displayClientMessage(
                Component.translatable("message.brightbronze_horizons.spawner.not_at_edge"),
                true
            );
            return InteractionResult.FAIL;
        }

        // Select direction (random if at corner)
        Direction expansionDirection;
        if (edgeDirections.size() == 1) {
            expansionDirection = edgeDirections.get(0);
        } else {
            // At a corner - pick deterministic direction (seed + position) for reproducibility
            expansionDirection = pickDeterministicCornerDirection(serverLevel, pos, edgeDirections);
        }

        // Get the target chunk position
        ChunkPos currentChunk = new ChunkPos(pos);
        ChunkPos targetChunk = new ChunkPos(
            currentChunk.x + expansionDirection.getStepX(),
            currentChunk.z + expansionDirection.getStepZ()
        );

        // Phase 10/11: select biome deterministically and enqueue a tick-bounded job.
        PlayableAreaData playableData = PlayableAreaData.get(serverLevel.getServer());
        SpawnAttemptResult selection = selectBiomeForSpawn(serverLevel, playableData, pos);
        if (!selection.success()) {
            player.displayClientMessage(selection.failureMessage(), true);
            return InteractionResult.FAIL;
        }

        ChunkExpansionManager.EnqueueResult enqueueResult = ChunkExpansionManager.enqueue(
            serverLevel,
            pos,
            tier,
            targetChunk,
            selection.biomeId(),
            player.getUUID(),
            player.getName().getString()
        );

        if (!enqueueResult.accepted()) {
            player.displayClientMessage(enqueueResult.failureMessage(), true);
            return InteractionResult.FAIL;
        }

        return InteractionResult.CONSUME;
    }

    /**
     * Determines which chunk edges this block position is on.
     * 
     * @param pos The block position
     * @return List of directions pointing outward from chunk edges (empty if not on edge)
     */
    private List<Direction> getChunkEdgeDirections(BlockPos pos) {
        List<Direction> edges = new ArrayList<>();
        
        // Get position within chunk (0-15)
        int localX = pos.getX() & 15;
        int localZ = pos.getZ() & 15;
        
        // Check each horizontal edge
        if (localX == 0) {
            edges.add(Direction.WEST);
        }
        if (localX == 15) {
            edges.add(Direction.EAST);
        }
        if (localZ == 0) {
            edges.add(Direction.NORTH);
        }
        if (localZ == 15) {
            edges.add(Direction.SOUTH);
        }
        
        return edges;
    }

    private SpawnAttemptResult selectBiomeForSpawn(ServerLevel level, PlayableAreaData playableData, BlockPos spawnerPos) {
        Holder<Biome> placedBiome = level.getBiome(spawnerPos);
        ResourceLocation placedBiomeId = BiomePoolManager.getBiomeId(placedBiome);

        if (tier == ChunkSpawnerTier.COAL) {
            // Coal tier: spawn terrain matching the biome where the spawner is placed.
            // First try the level's biome lookup. If that returns plains (the void world default),
            // fall back to the recorded biome from when the chunk was originally spawned.
            ResourceLocation effectiveBiomeId = placedBiomeId;
            
            // Check if we got the void world default biome (plains) - if so, try to use recorded data
            if (placedBiomeId != null && placedBiomeId.equals(ResourceLocation.withDefaultNamespace("plains"))) {
                ChunkPos spawnerChunk = new ChunkPos(spawnerPos);
                ResourceLocation recordedBiome = playableData.getRecordedBiomeForChunk(spawnerChunk);
                if (recordedBiome != null) {
                    effectiveBiomeId = recordedBiome;
                }
            }
            
            if (effectiveBiomeId == null) {
                return SpawnAttemptResult.failure(Component.translatable("message.brightbronze_horizons.spawner.unknown_biome"));
            }
            return SpawnAttemptResult.success(effectiveBiomeId);
        }

        // EMERALD tier (Altered Horizon Anchor): Use modded biomes from worldgen mods
        if (tier == ChunkSpawnerTier.EMERALD) {
            return selectModdedBiomeForSpawn(level, playableData);
        }

        WeightedBiomePool pool = BiomeRuleManager.getWeightedPool(level.registryAccess(), tier);
        if (pool.isEmpty()) {
            BrightbronzeHorizons.LOGGER.warn("No biomes available for tier {}", tier.getName());
            return SpawnAttemptResult.failure(Component.translatable("message.brightbronze_horizons.spawner.no_biomes"));
        }

        boolean placedIsEligible = placedBiomeId != null && poolContainsBiomeId(pool, placedBiomeId);
        if (placedIsEligible) {
            int roll = playableData.nextDeterministicInt(level.getServer(), 100);
            if (roll < 40) {
                return SpawnAttemptResult.success(placedBiomeId);
            }
        }

        int roll = playableData.nextDeterministicInt(level.getServer(), Math.max(1, pool.totalWeight()));
        Optional<Holder.Reference<Biome>> selectedOpt = pool.selectByWeight(roll);
        if (selectedOpt.isEmpty()) {
            return SpawnAttemptResult.failure(Component.translatable("message.brightbronze_horizons.spawner.no_biomes"));
        }

        Holder<Biome> selected = selectedOpt.get();
        ResourceLocation biomeId = BiomePoolManager.getBiomeId(selected);
        if (biomeId == null) {
            return SpawnAttemptResult.failure(Component.translatable("message.brightbronze_horizons.spawner.unknown_biome"));
        }
        return SpawnAttemptResult.success(biomeId);
    }

    /**
     * Selects a modded biome for the EMERALD tier (Altered Horizon Anchor).
     * 
     * <p>This method auto-detects biomes from worldgen mods (any namespace != "minecraft")
     * and uses deterministic selection from the PlayableAreaData RNG.
     * 
     * @param level The server level
     * @param playableData The playable area data for deterministic RNG
     * @return Success with a modded biome, or failure if no modded biomes are available
     */
    private SpawnAttemptResult selectModdedBiomeForSpawn(ServerLevel level, PlayableAreaData playableData) {
        // Check if any modded biomes are available
        if (!ModdedBiomeDetector.hasModdedBiomes(level.registryAccess())) {
            BrightbronzeHorizons.LOGGER.debug("Altered Horizon Anchor used but no modded biomes available");
            return SpawnAttemptResult.failure(
                Component.translatable("message.brightbronze_horizons.spawner.no_modded_biomes")
            );
        }

        // Use deterministic selection from the playable area's RNG
        int biomeCount = ModdedBiomeDetector.getModdedBiomeCount(level.registryAccess());
        int roll = playableData.nextDeterministicInt(level.getServer(), biomeCount);
        
        Optional<Holder.Reference<Biome>> selectedOpt = ModdedBiomeDetector.selectModdedBiome(
            level.registryAccess(), roll
        );
        
        if (selectedOpt.isEmpty()) {
            // Should not happen if hasModdedBiomes() returned true, but handle gracefully
            return SpawnAttemptResult.failure(
                Component.translatable("message.brightbronze_horizons.spawner.no_modded_biomes")
            );
        }

        Holder.Reference<Biome> selected = selectedOpt.get();
        ResourceLocation biomeId = ModdedBiomeDetector.getBiomeId(selected);
        
        if (biomeId == null) {
            return SpawnAttemptResult.failure(
                Component.translatable("message.brightbronze_horizons.spawner.unknown_biome")
            );
        }

        BrightbronzeHorizons.LOGGER.debug(
            "Altered Horizon Anchor selected modded biome: {} (roll {}/{})",
            biomeId, roll, biomeCount
        );
        
        return SpawnAttemptResult.success(biomeId);
    }

    private static boolean poolContainsBiomeId(WeightedBiomePool pool, ResourceLocation biomeId) {
        for (BiomeRuleManager.WeightedBiomeEntry entry : pool.entries()) {
            ResourceLocation id = BiomePoolManager.getBiomeId(entry.biome());
            if (biomeId.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private record SpawnAttemptResult(boolean success, ResourceLocation biomeId, Component failureMessage) {
        static SpawnAttemptResult success(ResourceLocation biomeId) {
            return new SpawnAttemptResult(true, biomeId, Component.empty());
        }

        static SpawnAttemptResult failure(Component failureMessage) {
            return new SpawnAttemptResult(false, null, failureMessage);
        }
    }

    private static Direction pickDeterministicCornerDirection(ServerLevel level, BlockPos pos, List<Direction> edgeDirections) {
        // Hash seed + position to pick stable direction for this placement.
        long seed = level.getSeed();
        long key = seed ^ pos.asLong() ^ 0x9E3779B97F4A7C15L;
        long mixed = mix64(key);
        int idx = (int) Math.floorMod(mixed, edgeDirections.size());
        return edgeDirections.get(idx);
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
