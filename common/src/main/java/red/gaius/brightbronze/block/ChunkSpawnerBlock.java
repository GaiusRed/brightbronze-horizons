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
 *   <li>Coal: Common Overworld biomes, mobs spawn at night only</li>
 *   <li>Iron: Rare Overworld biomes, mobs spawn at night only</li>
 *   <li>Gold: Nether biomes, mobs always spawn</li>
 *   <li>Emerald: Modded biomes (empty by default), mobs always spawn</li>
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
            if (placedBiomeId == null) {
                return SpawnAttemptResult.failure(Component.translatable("message.brightbronze_horizons.spawner.unknown_biome"));
            }
            return SpawnAttemptResult.success(placedBiomeId);
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
