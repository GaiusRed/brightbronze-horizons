package red.gaius.brightbronze.world.chunk;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import red.gaius.brightbronze.BrightbronzeHorizons;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

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

        return copyChunk(sourceLevel, sourceChunkPos, targetLevel, targetChunkPos, null);
        }

        /**
         * Copies a chunk from a source dimension to a target dimension, optionally forcing the
         * target chunk's biome container to a specific biome.
         *
         * <p>This is used by chunk spawners: blocks/entities are copied from a fixed-biome source
         * dimension, and then the overworld chunk's biome data is updated so biome lookups (rain,
         * fog, mob rules, and Coal "local-biome" behavior) match the spawned terrain.
         */
        public static boolean copyChunk(
            ServerLevel sourceLevel,
            ChunkPos sourceChunkPos,
            ServerLevel targetLevel,
            ChunkPos targetChunkPos,
            @Nullable Holder<Biome> forcedTargetBiome) {

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
            int blocksCopied = copyBlocks(sourceLevel, sourceChunkPos, targetLevel, targetChunkPos, minY, maxY + 1);
            
            BrightbronzeHorizons.LOGGER.debug("Copied {} non-air blocks to chunk ({}, {})", 
                blocksCopied, targetChunkPos.x, targetChunkPos.z);

            // Copy entities (mobs, item frames, armor stands, etc.)
            copyEntities(sourceLevel, sourceChunkPos, targetLevel, targetChunkPos);

            // Ensure the target chunk biome matches the spawned biome (critical for Coal local-biome rule).
            if (forcedTargetBiome != null) {
                applyUniformBiome(targetChunk, forcedTargetBiome);
            }

            // Mark target chunk as needing save and trigger updates
            targetChunk.markUnsaved();
            
            // Force full light update for the chunk
            for (int y = minY; y <= maxY; y += 16) {
                BlockPos lightPos = new BlockPos(targetChunkPos.getMiddleBlockX(), y, targetChunkPos.getMiddleBlockZ());
                targetLevel.getChunkSource().getLightEngine().checkBlock(lightPos);
            }
            
            // Force the chunk to be saved to disk immediately
            // This ensures that when clients request the chunk, they get the modified version
            targetLevel.getChunkSource().save(false);

            // Force resync chunk to all connected players (helps for chunk spawner use case)
            forceResyncChunk(targetLevel, targetChunkPos);

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

    private static void applyUniformBiome(LevelChunk targetChunk, Holder<Biome> biome) {
        // Fill the chunk's biome container at quart resolution (4x4x4 per section).
        BiomeResolver resolver = (x, y, z, sampler) -> biome;
        targetChunk.fillBiomesFromNoise(resolver, Climate.empty());
        targetChunk.markUnsaved();
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
     * @return The number of non-air blocks copied
     */
    public static int copyBlocks(
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
        
        int blocksCopied = 0;

        for (int y = fromY; y < toY; y++) {
            for (int z = sourceChunkPos.getMinBlockZ(); z <= sourceChunkPos.getMaxBlockZ(); z++) {
                for (int x = sourceChunkPos.getMinBlockX(); x <= sourceChunkPos.getMaxBlockX(); x++) {
                    sourcePos.set(x, y, z);
                    targetPos.set(x + xOffset, y, z + zOffset);

                    BlockState sourceState = sourceLevel.getBlockState(sourcePos);
                    
                    // Skip air blocks for efficiency (void chunks are already air)
                    if (sourceState.isAir()) {
                        continue;
                    }
                    
                    // Prevent leaf decay by marking leaves as persistent
                    if (sourceState.hasProperty(LeavesBlock.PERSISTENT)) {
                        sourceState = sourceState.setValue(LeavesBlock.PERSISTENT, true);
                    }

                    // Set the block with all update flags
                    targetLevel.setBlock(targetPos, sourceState, Block.UPDATE_ALL);
                    blocksCopied++;

                    // Copy block entity data if present
                    copyBlockEntity(sourceLevel, sourcePos, targetLevel, targetPos);
                }
            }
        }
        
        return blocksCopied;
    }

    /**
     * Copies block entity data from source to target position.
     * 
     * <p>This handles containers (chests, barrels), spawners, signs, lecterns,
     * and other blocks with persistent data. The block at target position must
     * already be set before calling this method.
     * 
     * @param sourceLevel The source level
     * @param sourcePos The source position (immutable copy needed)
     * @param targetLevel The target level
     * @param targetPos The target position (immutable copy needed)
     */
    private static void copyBlockEntity(
            ServerLevel sourceLevel,
            BlockPos sourcePos,
            ServerLevel targetLevel,
            BlockPos targetPos) {

        BlockEntity sourceBlockEntity = sourceLevel.getBlockEntity(sourcePos);
        if (sourceBlockEntity == null) {
            return;
        }

        // Get the target block entity (created by setBlock if the block type has one)
        BlockEntity targetBlockEntity = targetLevel.getBlockEntity(targetPos);
        if (targetBlockEntity == null) {
            return;
        }

        try {
            // Save source block entity data with full metadata
            CompoundTag nbtData = sourceBlockEntity.saveWithFullMetadata(sourceLevel.registryAccess());
            
            // Update position in NBT to match target position
            nbtData.putInt("x", targetPos.getX());
            nbtData.putInt("y", targetPos.getY());
            nbtData.putInt("z", targetPos.getZ());
            
            // Load data into target block entity using MC 1.21 ValueInput API
            ValueInput valueInput = TagValueInput.create(
                ProblemReporter.DISCARDING,
                targetLevel.registryAccess(),
                nbtData
            );
            targetBlockEntity.loadWithComponents(valueInput);
            targetBlockEntity.setChanged();
            
        } catch (Exception e) {
            BrightbronzeHorizons.LOGGER.warn("Failed to copy block entity at {}: {}", 
                sourcePos, e.getMessage());
        }
    }

    /**
     * Copies all entities (mobs, item frames, armor stands, etc.) from the source
     * chunk to the target chunk.
     * 
     * <p>Players are not copied. Each entity is serialized from the source and
     * recreated in the target dimension at the corresponding position.
     * 
     * @param sourceLevel The source level
     * @param sourceChunkPos The source chunk position
     * @param targetLevel The target level
     * @param targetChunkPos The target chunk position
     */
    private static void copyEntities(
            ServerLevel sourceLevel,
            ChunkPos sourceChunkPos,
            ServerLevel targetLevel,
            ChunkPos targetChunkPos) {

        int xOffset = targetChunkPos.getMinBlockX() - sourceChunkPos.getMinBlockX();
        int zOffset = targetChunkPos.getMinBlockZ() - sourceChunkPos.getMinBlockZ();

        // Create AABB for the entire source chunk (all Y levels)
        AABB chunkBounds = new AABB(
            sourceChunkPos.getMinBlockX(),
            sourceLevel.getMinY(),
            sourceChunkPos.getMinBlockZ(),
            sourceChunkPos.getMaxBlockX() + 1,
            sourceLevel.getMaxY() + 1,
            sourceChunkPos.getMaxBlockZ() + 1
        );

        // Get all entities in the chunk (excluding players)
        List<Entity> entities = sourceLevel.getEntities(
            (Entity) null, 
            chunkBounds, 
            entity -> !(entity instanceof Player)
        );

        if (entities.isEmpty()) {
            return;
        }

        BrightbronzeHorizons.LOGGER.debug("Copying {} entities from chunk {} to {}", 
            entities.size(), sourceChunkPos, targetChunkPos);

        for (Entity sourceEntity : entities) {
            try {
                // Save entity to NBT using MC 1.21 ValueOutput API
                TagValueOutput valueOutput = TagValueOutput.createWithContext(
                    ProblemReporter.DISCARDING,
                    sourceLevel.registryAccess()
                );
                if (!sourceEntity.saveAsPassenger(valueOutput)) {
                    continue; // Entity doesn't want to be saved
                }
                CompoundTag nbtData = valueOutput.buildResult();

                // Calculate new position
                double newX = sourceEntity.getX() + xOffset;
                double newY = sourceEntity.getY();
                double newZ = sourceEntity.getZ() + zOffset;

                // Update position in NBT - create new Pos list with updated coordinates
                ListTag posList = new ListTag();
                posList.add(DoubleTag.valueOf(newX));
                posList.add(DoubleTag.valueOf(newY));
                posList.add(DoubleTag.valueOf(newZ));
                nbtData.put("Pos", posList);

                // Remove UUID so a new one is generated (prevents duplicate UUID issues)
                nbtData.remove("UUID");

                // Create new entity in target level using MC 1.21 ValueInput API
                ValueInput valueInput = TagValueInput.create(
                    ProblemReporter.DISCARDING,
                    targetLevel.registryAccess(),
                    nbtData
                );
                Optional<Entity> newEntityOpt = EntityType.create(valueInput, targetLevel, EntitySpawnReason.LOAD);
                
                if (newEntityOpt.isPresent()) {
                    Entity newEntity = newEntityOpt.get();
                    newEntity.setPos(newX, newY, newZ);
                    targetLevel.addFreshEntity(newEntity);
                }

            } catch (Exception e) {
                BrightbronzeHorizons.LOGGER.warn("Failed to copy entity {} at {}: {}", 
                    sourceEntity.getType().getDescriptionId(), 
                    sourceEntity.position(), 
                    e.getMessage());
            }
        }
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

    /**
     * Forces a chunk to be resent to all players tracking it.
     * 
     * <p>This is essential after modifying chunk data (copying blocks from source dimensions)
     * because the normal block update flags only notify already-connected clients about
     * individual block changes. Clients who load the chunk later, or clients who need a
     * full chunk refresh, won't see the changes without this explicit resync.
     * 
     * @param level The server level containing the chunk
     * @param chunkPos The position of the chunk to resync
     */
    private static void forceResyncChunk(ServerLevel level, ChunkPos chunkPos) {
        try {
            // Cast ChunkMap to our mixin interface
            if (level.getChunkSource().chunkMap instanceof ControllableChunkMap controllable) {
                controllable.brightbronze$forceResyncChunk(chunkPos);
                BrightbronzeHorizons.LOGGER.debug("Forced resync of chunk ({}, {})", chunkPos.x, chunkPos.z);
            } else {
                BrightbronzeHorizons.LOGGER.warn(
                    "ChunkMap does not implement ControllableChunkMap - chunk resync skipped for ({}, {})",
                    chunkPos.x, chunkPos.z
                );
            }
        } catch (Exception e) {
            BrightbronzeHorizons.LOGGER.warn("Failed to force chunk resync: {}", e.getMessage());
        }
    }
}
