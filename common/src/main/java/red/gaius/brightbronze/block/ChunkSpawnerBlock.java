package red.gaius.brightbronze.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
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
import red.gaius.brightbronze.world.BiomePoolManager;
import red.gaius.brightbronze.world.ChunkSpawnerTier;
import red.gaius.brightbronze.world.PlayableAreaData;
import red.gaius.brightbronze.world.chunk.ChunkCopyService;
import red.gaius.brightbronze.world.dimension.SourceDimensionManager;

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

        // Attempt to spawn the chunk
        boolean success = attemptChunkSpawn(serverLevel, player, targetChunk);
        
        if (success) {
            // Remove the spawner block (consumed on use)
            level.removeBlock(pos, false);
            
            player.displayClientMessage(
                Component.translatable("message.brightbronze_horizons.spawner.success", 
                    tier.getName(), targetChunk.x, targetChunk.z),
                true
            );
            return InteractionResult.CONSUME;
        } else {
            player.displayClientMessage(
                Component.translatable("message.brightbronze_horizons.spawner.failed"),
                true
            );
            return InteractionResult.FAIL;
        }
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

    /**
     * Attempts to spawn a new chunk at the target position.
     * 
     * @param level The server level
     * @param player The player who activated the spawner
     * @param targetChunk The chunk position to spawn
     * @return true if successful, false otherwise
     */
    private boolean attemptChunkSpawn(ServerLevel level, Player player, ChunkPos targetChunk) {
        BrightbronzeHorizons.LOGGER.info(
            "Chunk spawner activated: tier={}, target=({}, {}), player={}",
            tier.getName(), targetChunk.x, targetChunk.z, player.getName().getString()
        );

        // Step 0: Validate that the target chunk can be expanded into
        PlayableAreaData playableData = PlayableAreaData.get(level.getServer());
        
        if (!playableData.canExpandInto(targetChunk)) {
            if (playableData.isChunkPlayable(targetChunk)) {
                BrightbronzeHorizons.LOGGER.warn(
                    "Target chunk ({}, {}) is already in playable area",
                    targetChunk.x, targetChunk.z
                );
                player.displayClientMessage(
                    Component.translatable("message.brightbronze_horizons.spawner.already_spawned"),
                    true
                );
            } else {
                BrightbronzeHorizons.LOGGER.warn(
                    "Target chunk ({}, {}) is not adjacent to playable area",
                    targetChunk.x, targetChunk.z
                );
                player.displayClientMessage(
                    Component.translatable("message.brightbronze_horizons.spawner.not_adjacent"),
                    true
                );
            }
            return false;
        }

        // Step 1: Select a biome from this tier's pool deterministically
        List<Holder<Biome>> pool = BiomePoolManager.getBiomesForTier(level.registryAccess(), tier);
        if (pool.isEmpty()) {
            BrightbronzeHorizons.LOGGER.warn("No biomes available for tier {}", tier.getName());
            return false;
        }

        int index = playableData.nextDeterministicInt(level.getServer(), pool.size());
        Holder<Biome> biomeHolder = pool.get(index);
        ResourceLocation biomeId = BiomePoolManager.getBiomeId(biomeHolder);
        
        if (biomeId == null) {
            BrightbronzeHorizons.LOGGER.error("Could not get biome ID for selected biome");
            return false;
        }

        BrightbronzeHorizons.LOGGER.info("Selected biome {} for tier {}", biomeId, tier.getName());

        // Step 2: Get or create the source dimension for this biome
        ServerLevel sourceLevel = SourceDimensionManager.getOrCreateSourceDimension(
            level.getServer(),
            biomeId
        );

        // Step 3: Copy the chunk from source to target
        // The source chunk uses the same coordinates as the target (per PRD Section 4.3)
        ChunkPos sourceChunkPos = targetChunk;

        boolean success = ChunkCopyService.copyChunk(
            sourceLevel,
            sourceChunkPos,
            level,
            targetChunk
        );

        if (success) {
            // Register the new chunk in the playable area
            playableData.addChunk(targetChunk);
            
            BrightbronzeHorizons.LOGGER.info(
                "Successfully spawned {} biome chunk at ({}, {})",
                biomeId, targetChunk.x, targetChunk.z
            );
            
            // TODO: Phase 7 - handle mob spawning based on tier rules
            // if (tier.alwaysSpawnsMobs() || isNightTime(level)) { spawnMobs(); }
        }

        return success;
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
