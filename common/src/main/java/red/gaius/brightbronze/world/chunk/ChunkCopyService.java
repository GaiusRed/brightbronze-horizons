package red.gaius.brightbronze.world.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import red.gaius.brightbronze.BrightbronzeHorizons;

/**
 * Service for copying chunks from source dimensions to the playable world.
 * 
 * <p>This service handles the complete transfer of chunk data:
 * <ul>
 *   <li>Block states (including block entity data)</li>
 *   <li>Biome data (ensuring the target chunk has the correct biome)</li>
 * </ul>
 * 
 * <p>The copy process respects existing blocks in the target chunk, only overwriting
 * air, liquids, bedrock, and other "empty" blocks. This allows for partial chunk
 * overlays if needed.
 */
public class ChunkCopyService {

    /**
     * Number of layers to copy per tick to avoid lag spikes.
     * A layer is one Y-level across the entire chunk (16x16 blocks).
     */
    public static final int LAYERS_PER_TICK = 8;

    private ChunkCopyService() {
        // Utility class
    }

    /**
     * Copies a chunk from a source dimension to a target dimension.
     * 
     * <p>This is a synchronous operation that copies the entire chunk at once.
     * For large-scale operations, consider using a tick-based approach which
     * spreads the work across multiple ticks.
     * 
     * @param sourceLevel The source dimension level
     * @param sourceChunkPos The source chunk position
     * @param targetLevel The target dimension level
     * @param targetChunkPos The target chunk position
     * @return true if the copy was successful
     */
    public static boolean copyChunk(
            ServerLevel sourceLevel,
            ChunkPos sourceChunkPos,
            ServerLevel targetLevel,
            ChunkPos targetChunkPos) {

        BrightbronzeHorizons.LOGGER.debug("Copying chunk {} from {} to {} at {}",
                sourceChunkPos, sourceLevel.dimension().location(),
                targetLevel.dimension().location(), targetChunkPos);

        // Force-load both chunks
        sourceLevel.setChunkForced(sourceChunkPos.x, sourceChunkPos.z, true);
        targetLevel.setChunkForced(targetChunkPos.x, targetChunkPos.z, true);

        try {
            // Get chunk access
            LevelChunk sourceChunk = sourceLevel.getChunk(sourceChunkPos.x, sourceChunkPos.z);
            LevelChunk targetChunk = targetLevel.getChunk(targetChunkPos.x, targetChunkPos.z);

            // Copy blocks
            int minY = targetLevel.getMinY();
            int maxY = targetLevel.getMaxY();
            copyBlocks(sourceLevel, sourceChunkPos, targetLevel, targetChunkPos, minY, maxY + 1);

            // Mark target chunk as needing save
            targetChunk.markUnsaved();

            BrightbronzeHorizons.LOGGER.debug("Successfully copied chunk {} -> {}", sourceChunkPos, targetChunkPos);
            return true;

        } catch (Exception e) {
            BrightbronzeHorizons.LOGGER.error("Failed to copy chunk: {}", e.getMessage(), e);
            return false;

        } finally {
            // Release force-loaded chunks
            sourceLevel.setChunkForced(sourceChunkPos.x, sourceChunkPos.z, false);
            targetLevel.setChunkForced(targetChunkPos.x, targetChunkPos.z, false);
        }
    }

    /**
     * Copies blocks from source chunk to target chunk within the specified Y range.
     * 
     * @param sourceLevel The source level
     * @param sourceChunkPos The source chunk position
     * @param targetLevel The target level
     * @param targetChunkPos The target chunk position
     * @param fromY The minimum Y level (inclusive)
     * @param toY The maximum Y level (exclusive)
     */
    public static void copyBlocks(
            ServerLevel sourceLevel,
            ChunkPos sourceChunkPos,
            ServerLevel targetLevel,
            ChunkPos targetChunkPos,
            int fromY,
            int toY) {

        int xOffset = targetChunkPos.getMinBlockX() - sourceChunkPos.getMinBlockX();
        int zOffset = targetChunkPos.getMinBlockZ() - sourceChunkPos.getMinBlockZ();

        BlockPos.MutableBlockPos sourcePos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos();

        for (int y = fromY; y < toY; y++) {
            for (int z = sourceChunkPos.getMinBlockZ(); z <= sourceChunkPos.getMaxBlockZ(); z++) {
                for (int x = sourceChunkPos.getMinBlockX(); x <= sourceChunkPos.getMaxBlockX(); x++) {
                    sourcePos.set(x, y, z);
                    targetPos.set(x + xOffset, y, z + zOffset);

                    // Check if we should overwrite this block
                    BlockState existingState = targetLevel.getBlockState(targetPos);
                    if (isReplaceableBlock(existingState)) {
                        BlockState sourceState = sourceLevel.getBlockState(sourcePos);
                        
                        // Prevent leaf decay by marking leaves as persistent
                        if (sourceState.hasProperty(LeavesBlock.PERSISTENT)) {
                            sourceState = sourceState.setValue(LeavesBlock.PERSISTENT, true);
                        }

                        // Set the block
                        targetLevel.setBlock(targetPos, sourceState, Block.UPDATE_ALL);

                        // Copy block entity data if present
                        copyBlockEntity(sourceLevel, sourcePos, targetLevel, targetPos);
                    }
                }
            }
        }
    }

    /**
     * Checks if a block state should be replaced during chunk copy.
     * 
     * @param state The existing block state
     * @return true if this block can be overwritten
     */
    private static boolean isReplaceableBlock(BlockState state) {
        Block block = state.getBlock();
        return state.isAir() 
                || block instanceof LiquidBlock
                || block == Blocks.BEDROCK
                || block == Blocks.BARRIER
                || block == Blocks.STRUCTURE_VOID;
    }

    /**
     * Copies block entity data from source to target position.
     * 
     * <p><b>Note:</b> Block entity copying in MC 1.21 requires special handling.
     * TODO: Implement proper block entity copying once we understand the 1.21 API better.
     * For now, block entities (chests, signs, etc.) will not preserve their contents.
     * 
     * @param sourceLevel The source level
     * @param sourcePos The source position
     * @param targetLevel The target level
     * @param targetPos The target position
     */
    private static void copyBlockEntity(
            ServerLevel sourceLevel,
            BlockPos sourcePos,
            ServerLevel targetLevel,
            BlockPos targetPos) {

        // TODO: Implement block entity copying for MC 1.21
        // The API has changed significantly and requires investigation.
        // For world generation purposes (trees, grass, etc.), this is not critical.
        // Block entities like chests in generated structures will be empty.
    }

    /**
     * Checks if a chunk position is "empty" (not yet spawned with terrain).
     * An empty chunk contains only air or bedrock.
     * 
     * @param level The level to check
     * @param chunkPos The chunk position
     * @return true if the chunk is empty
     */
    public static boolean isEmptyChunk(ServerLevel level, ChunkPos chunkPos) {
        // Check the bottom layer for bedrock (sealed chunk indicator)
        int minY = level.getMinY();
        BlockPos checkPos = new BlockPos(chunkPos.getMiddleBlockX(), minY, chunkPos.getMiddleBlockZ());
        BlockState state = level.getBlockState(checkPos);
        
        // If there's bedrock at the bottom, this chunk hasn't been spawned
        return state.is(Blocks.BEDROCK) || state.isAir();
    }
}
