package red.gaius.brightbronze.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.versioned.Versioned;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

/**
 * Server-level saved data that tracks which chunks are part of the playable area.
 * 
 * <p>This data persists across world saves/loads and tracks:
 * <ul>
 *   <li>All spawned chunks (the "playable area")</li>
 *   <li>Frontier chunks (chunks at the edge that can be expanded)</li>
 *   <li>Whether the starting area has been initialized</li>
 * </ul>
 * 
 * <p>The playable area starts as a 3×3 chunk region and expands when players
 * use chunk spawners. New chunks must be adjacent to existing playable chunks.
 * 
 * <p>MC 1.21 uses Codecs for SavedData serialization.
 */
public class PlayableAreaData extends SavedData {

    private static final String DATA_NAME = "brightbronze_horizons_playable_area";
    
    /** Codec for ChunkPos (stored as int pair) */
    private static final Codec<ChunkPos> CHUNK_POS_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.INT.fieldOf("x").forGetter(pos -> pos.x),
            Codec.INT.fieldOf("z").forGetter(pos -> pos.z)
        ).apply(instance, ChunkPos::new)
    );
    
    /** Codec for PlayableAreaData serialization */
    public static final Codec<PlayableAreaData> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.BOOL.fieldOf("initialized").forGetter(data -> data.initialized),
            CHUNK_POS_CODEC.fieldOf("spawn_chunk").forGetter(data -> data.spawnChunk),
            CHUNK_POS_CODEC.listOf().fieldOf("spawned_chunks").forGetter(data -> new ArrayList<>(data.spawnedChunks)),
            Codec.LONG.optionalFieldOf("rng_state", 0L).forGetter(data -> data.rngState),
            SpawnedChunkMeta.CODEC.listOf().optionalFieldOf("spawned_chunk_meta", List.of()).forGetter(data -> data.spawnedChunkMetaList())
        ).apply(instance, PlayableAreaData::new)
    );
    
    /** Data name for saved data storage */
    public static final String DATA_NAME_VALUE = DATA_NAME;
    
    /** DataFixTypes for data storage */
    public static final DataFixTypes DATA_FIX_TYPES = DataFixTypes.LEVEL;
    
    /** All chunks that are part of the playable area */
    private final Set<ChunkPos> spawnedChunks;

    /** Spawn metadata keyed by chunk position (stored as list in codec for simplicity). */
    private final Map<Long, SpawnedChunkMeta> spawnedChunkMeta;
    
    /** Whether the starting area has been initialized */
    private boolean initialized;
    
    /** The center chunk of the starting 3×3 area (world spawn chunk) */
    private ChunkPos spawnChunk;

    /**
     * Persistent RNG state for reproducible biome selection.
     *
     * <p>We intentionally store our own state (instead of using RandomSource directly)
     * so selection remains deterministic across restarts.
     */
    private long rngState;

    /**
     * Creates a new empty PlayableAreaData.
     * Used for new worlds.
     */
    public PlayableAreaData() {
        this.spawnedChunks = new HashSet<>();
        this.spawnedChunkMeta = new HashMap<>();
        this.initialized = false;
        this.spawnChunk = new ChunkPos(0, 0);
        this.rngState = 0L;
    }
    
    /**
     * Creates PlayableAreaData from loaded data.
     * Used by the Codec during deserialization.
     */
    private PlayableAreaData(boolean initialized, ChunkPos spawnChunk, List<ChunkPos> spawnedChunks, long rngState, List<SpawnedChunkMeta> meta) {
        this.initialized = initialized;
        this.spawnChunk = spawnChunk;
        this.spawnedChunks = new HashSet<>(spawnedChunks);
        this.rngState = rngState;

        this.spawnedChunkMeta = new HashMap<>();
        if (meta != null) {
            for (SpawnedChunkMeta m : meta) {
                if (m != null) {
                    this.spawnedChunkMeta.put(chunkKey(m.chunk()), m);
                }
            }
        }
        
        BrightbronzeHorizons.LOGGER.debug(
            "Loaded PlayableAreaData: {} chunks, initialized={}",
            this.spawnedChunks.size(), this.initialized
        );
    }
    
    /**
     * Saves this data to NBT. Required by SavedData in MC 1.21.1.
     * In MC 1.21.10, serialization is handled by the Codec via SavedDataType.
     * 
     * @param compoundTag The compound tag to save to
     * @param provider The holder lookup provider
     * @return The saved compound tag
     */
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        return CODEC.encodeStart(NbtOps.INSTANCE, this)
            .resultOrPartial(error -> BrightbronzeHorizons.LOGGER.error("Failed to save PlayableAreaData: {}", error))
            .map(tag -> {
                if (tag instanceof CompoundTag ct) {
                    return ct;
                }
                // If encoding produced a different tag type, wrap it
                compoundTag.put("data", tag);
                return compoundTag;
            })
            .orElse(compoundTag);
    }

    /**
     * Gets the PlayableAreaData for the given server.
     * Creates new data if none exists.
     * 
     * @param server The Minecraft server
     * @return The playable area data
     */
    public static PlayableAreaData get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld not loaded");
        }
        
        return Versioned.savedData().getPlayableAreaData(overworld.getDataStorage());
    }

    /**
     * @return Whether the starting area has been initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Marks the starting area as initialized.
     * Should be called after the initial 3×3 area is set up.
     * 
     * @param spawnChunk The center chunk of the starting area
     */
    public void setInitialized(ChunkPos spawnChunk) {
        this.initialized = true;
        this.spawnChunk = spawnChunk;
        setDirty();
    }

    /**
     * Returns a deterministic random int in [0, bound).
     *
     * <p>Uses a SplitMix64-style step and persists the state to disk.
     */
    public int nextDeterministicInt(MinecraftServer server, int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be > 0");
        }

        ensureRngInitialized(server);

        // SplitMix64 increment
        rngState += 0x9E3779B97F4A7C15L;
        long mixed = mix64(rngState);

        setDirty();
        return (int) Math.floorMod(mixed, bound);
    }

    private void ensureRngInitialized(MinecraftServer server) {
        if (rngState != 0L) {
            return;
        }

        long worldSeed = server.getWorldData().worldGenOptions().seed();
        long chunkKey = (((long) spawnChunk.x) << 32) ^ (spawnChunk.z & 0xFFFFFFFFL);

        // Fixed odd constant to namespace our RNG from any future RNG uses.
        rngState = mix64(worldSeed ^ chunkKey ^ 0xD1B54A32D192ED03L);

        // Avoid zero because we use it as "uninitialized".
        if (rngState == 0L) {
            rngState = 0x9E3779B97F4A7C15L;
        }

        setDirty();
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * @return The center chunk of the starting area
     */
    public ChunkPos getSpawnChunk() {
        return spawnChunk;
    }

    /**
     * @return An unmodifiable view of all spawned chunks
     */
    public Set<ChunkPos> getSpawnedChunks() {
        return Collections.unmodifiableSet(spawnedChunks);
    }

    /**
     * @return The number of chunks in the playable area
     */
    public int getChunkCount() {
        return spawnedChunks.size();
    }

    /**
     * Checks if a chunk is part of the playable area.
     * 
     * @param pos The chunk position to check
     * @return true if the chunk is playable
     */
    public boolean isChunkPlayable(ChunkPos pos) {
        return spawnedChunks.contains(pos);
    }

    /**
     * Checks if a chunk is at the frontier (edge of playable area).
     * A frontier chunk is a playable chunk that has at least one
     * non-playable adjacent chunk.
     * 
     * @param pos The chunk position to check
     * @return true if the chunk is at the frontier
     */
    public boolean isFrontierChunk(ChunkPos pos) {
        if (!isChunkPlayable(pos)) {
            return false;
        }
        
        // Check all 4 adjacent chunks (no diagonals)
        return !isChunkPlayable(new ChunkPos(pos.x - 1, pos.z)) ||
               !isChunkPlayable(new ChunkPos(pos.x + 1, pos.z)) ||
               !isChunkPlayable(new ChunkPos(pos.x, pos.z - 1)) ||
               !isChunkPlayable(new ChunkPos(pos.x, pos.z + 1));
    }

    /**
     * Checks if a chunk can be expanded into (is adjacent to playable area).
     * 
     * @param pos The chunk position to check
     * @return true if the chunk can be expanded into
     */
    public boolean canExpandInto(ChunkPos pos) {
        // Can't expand into already playable chunks
        if (isChunkPlayable(pos)) {
            return false;
        }
        
        // Must be adjacent to at least one playable chunk
        return isChunkPlayable(new ChunkPos(pos.x - 1, pos.z)) ||
               isChunkPlayable(new ChunkPos(pos.x + 1, pos.z)) ||
               isChunkPlayable(new ChunkPos(pos.x, pos.z - 1)) ||
               isChunkPlayable(new ChunkPos(pos.x, pos.z + 1));
    }

    /**
     * Adds a chunk to the playable area.
     * 
     * @param pos The chunk position to add
     * @return true if the chunk was added, false if already present
     */
    public boolean addChunk(ChunkPos pos) {
        boolean added = spawnedChunks.add(pos);
        if (added) {
            setDirty();
            BrightbronzeHorizons.LOGGER.debug(
                "Added chunk ({}, {}) to playable area. Total: {}",
                pos.x, pos.z, spawnedChunks.size()
            );
        }
        return added;
    }

    /**
     * Records metadata about a spawned chunk for Phase 10/11 reporting and pruning.
     * 
     * @param pos The chunk position
     * @param biomeId The biome ID used for the chunk
     * @param tierName The tier name of the spawner
     * @param structureTriggered Whether this chunk was spawned due to structure completion
     * @param triggeringChunk The original chunk that triggered structure completion (null if not structure-triggered)
     */
    public void recordSpawnedChunk(ChunkPos pos, @org.jetbrains.annotations.Nullable ResourceLocation biomeId, String tierName, boolean structureTriggered, @org.jetbrains.annotations.Nullable ChunkPos triggeringChunk) {
        if (pos == null || tierName == null) {
            return;
        }
        // Use a placeholder biome if null (can happen for structure-triggered chunks in edge cases)
        ResourceLocation effectiveBiome = biomeId != null ? biomeId : ResourceLocation.withDefaultNamespace("plains");
        spawnedChunkMeta.put(chunkKey(pos), new SpawnedChunkMeta(pos, effectiveBiome, tierName, structureTriggered, triggeringChunk));
        setDirty();
    }

    /**
     * Records metadata about a spawned chunk for Phase 10/11 reporting and pruning.
     * Convenience overload for non-structure-triggered chunks.
     */
    public void recordSpawnedChunk(ChunkPos pos, ResourceLocation biomeId, String tierName) {
        recordSpawnedChunk(pos, biomeId, tierName, false, null);
    }

    /**
     * Gets the recorded biome for a spawned chunk.
     * 
     * @param pos The chunk position
     * @return The biome ID that was used when spawning this chunk, or null if not found
     */
    @org.jetbrains.annotations.Nullable
    public ResourceLocation getRecordedBiomeForChunk(ChunkPos pos) {
        SpawnedChunkMeta meta = spawnedChunkMeta.get(chunkKey(pos));
        return meta != null ? meta.biome() : null;
    }

    public Set<ResourceLocation> getRecordedBiomeIds() {
        Set<ResourceLocation> out = new HashSet<>();
        for (SpawnedChunkMeta meta : spawnedChunkMeta.values()) {
            out.add(meta.biome());
        }
        return out;
    }

    public List<SpawnedChunkMeta> getSpawnedChunkMeta() {
        return spawnedChunkMetaList();
    }

    private List<SpawnedChunkMeta> spawnedChunkMetaList() {
        return new ArrayList<>(spawnedChunkMeta.values());
    }

    private static long chunkKey(ChunkPos pos) {
        return (((long) pos.x) << 32) ^ (pos.z & 0xFFFFFFFFL);
    }

    public record SpawnedChunkMeta(ChunkPos chunk, ResourceLocation biome, String tier, boolean structureTriggered, @org.jetbrains.annotations.Nullable ChunkPos triggeringChunk) {
        static final Codec<SpawnedChunkMeta> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                CHUNK_POS_CODEC.fieldOf("chunk").forGetter(SpawnedChunkMeta::chunk),
                ResourceLocation.CODEC.fieldOf("biome").forGetter(SpawnedChunkMeta::biome),
                Codec.STRING.fieldOf("tier").forGetter(SpawnedChunkMeta::tier),
                Codec.BOOL.optionalFieldOf("structure_triggered", false).forGetter(SpawnedChunkMeta::structureTriggered),
                CHUNK_POS_CODEC.optionalFieldOf("triggering_chunk", null).forGetter(SpawnedChunkMeta::triggeringChunk)
            ).apply(instance, SpawnedChunkMeta::new)
        );
    }

    /**
     * Adds multiple chunks to the playable area (for initial setup).
     * 
     * @param chunks The chunk positions to add
     */
    public void addChunks(Iterable<ChunkPos> chunks) {
        for (ChunkPos pos : chunks) {
            spawnedChunks.add(pos);
        }
        setDirty();
    }

    /**
     * Gets all frontier chunks (chunks at the edge of the playable area).
     * 
     * @return Set of frontier chunk positions
     */
    public Set<ChunkPos> getFrontierChunks() {
        Set<ChunkPos> frontier = new HashSet<>();
        for (ChunkPos pos : spawnedChunks) {
            if (isFrontierChunk(pos)) {
                frontier.add(pos);
            }
        }
        return frontier;
    }

    /**
     * Gets all chunks that can be expanded into (adjacent to playable area but not playable).
     * 
     * @return Set of expandable chunk positions
     */
    public Set<ChunkPos> getExpandableChunks() {
        Set<ChunkPos> expandable = new HashSet<>();
        
        for (ChunkPos pos : spawnedChunks) {
            // Check all 4 adjacent positions
            ChunkPos[] adjacent = {
                new ChunkPos(pos.x - 1, pos.z),
                new ChunkPos(pos.x + 1, pos.z),
                new ChunkPos(pos.x, pos.z - 1),
                new ChunkPos(pos.x, pos.z + 1)
            };
            
            for (ChunkPos adj : adjacent) {
                if (!isChunkPlayable(adj)) {
                    expandable.add(adj);
                }
            }
        }
        
        return expandable;
    }
}
