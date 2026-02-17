# Structure-Complete Chunk Spawning — Feature Requirements Document

**Last updated:** 2026-02-18

This document defines the requirements for automatically spawning additional chunks when a structure would otherwise be partially cut off. It extends the core chunk spawning system defined in the main PRD.

## 1. Problem Statement

### 1.1 Current Behavior

When a chunk is spawned (either during starting area initialization or via a Chunk Spawner activation), structures that span multiple chunks are partially revealed. This creates several issues:

- **Visual inconsistency:** Villages have houses cut in half, temples are missing rooms, etc.
- **Gameplay frustration:** Loot chests may be inaccessible, mob spawners cut off, etc.
- **Immersion breaking:** Partial structures look unnatural and unfinished.

### 1.2 Desired Behavior

When a chunk containing part of a structure is spawned, **all chunks that contain any part of that structure** should also be spawned automatically. This ensures:

- Structures always appear complete and functional.
- Players can immediately interact with the full structure.
- The world feels intentional rather than arbitrarily sliced.

### 1.3 Scope

This feature applies to:

- **Starting area initialization** (3×3 chunks + structure completion)
- **Chunk Spawner activations** (single chunk + structure completion)
- **All structure types** (villages, temples, monuments, etc.)

## 2. Technical Requirements

### 2.1 Structure Detection

When a chunk is copied from a source dimension to the playable world:

1. Query the source chunk for **structure starts** (`StructureStart` objects).
2. Query the source chunk for **structure references** (chunks that contain pieces of structures started elsewhere).
3. For each detected structure, retrieve its **bounding box** (`BoundingBox`).

### 2.2 Chunk Identification

For each structure detected in a spawned chunk:

1. Calculate all chunk positions that intersect the structure's bounding box.
2. Filter to only chunks that are **not already in the playable area**.
3. Queue these chunks for spawning from the **same biome's source dimension**.

### 2.3 Cascade Handling

Structure-completion chunks **may themselves contain structures**, which should also be completed. This is handled via controlled cascading:

- When spawning structure-completion chunks, **recursively check for new structures** in those chunks.
- Track all structures discovered across the cascade.
- Stop when **no new structures are found** or the **structure limit is reached**.
- Default limit: **16 structures** per player-triggered spawn.

**Algorithm:**

1. Player triggers chunk spawn (spawner or starting area).
2. Detect structures in that chunk → add to `discoveredStructures` set.
3. Calculate all chunks needed for those structures.
4. For each new chunk, detect structures → add new ones to `discoveredStructures`.
5. Repeat step 3-4 until no new structures found OR `discoveredStructures.size() >= maxStructures`.
6. Spawn all collected chunks.

**Rationale:** Adjacent structures (e.g., a village next to a pillager outpost, or overlapping Ancient City sections) should all complete together. The structure limit (not chunk limit) provides intuitive control — "complete up to 16 structures" is easier to reason about than chunk counts.

**Safety:** The structure limit prevents infinite expansion. Even in pathological cases (dense structure placement), 16 structures is bounded and predictable.

### 2.4 Biome Coherence

Structure-completion chunks must maintain biome coherence:

- All chunks spawned to complete a structure use the **same source dimension** as the triggering chunk.
- This ensures terrain surrounding the structure matches visually.
- Block replacement rules from the triggering biome's rule apply to all structure chunks.

### 2.5 Frontier Updates

All structure-completion chunks must be properly tracked:

- Add each chunk to `PlayableAreaData.spawnedChunks`.
- Update frontier detection so players can expand from structure edges.
- Record metadata including `structureTriggered: true` and the originating chunk position.

## 3. Configuration

### 3.1 Runtime Config Additions

Add to `config/brightbronze_horizons.json`:

| Option | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `enableStructureCompletion` | Boolean | `true` | If true, spawning a chunk automatically spawns all chunks needed to complete any structures in that chunk. If false, structures may be partially cut off (legacy behavior). |
| `maxStructureCompletionStructures` | Integer | `16` | Maximum number of structures that can be completed from a single player-triggered spawn. Cascading stops when this limit is reached. |
| `maxStructureCompletionChunks` | Integer | `256` | Hard safety limit on total chunks spawned for structure completion. Prevents extreme cases where 16 small structures somehow span excessive area. |

### 3.2 Per-Structure Exclusions (Future)

A future enhancement may allow excluding specific structure types from completion logic via data pack rules. This is **not in scope** for the initial implementation.

## 4. Player Experience

### 4.1 Messaging

When structure completion spawns additional chunks:

- **Do not** send individual announcements for each structure-completion chunk.
- **Do** include structure completion info in the original spawn announcement:
  - Example: `"PlayerName spawned a Plains chunk at (5, 3) [COPPER] — completed 1 structure (Village), 4 additional chunks spawned."`
- If structure completion is disabled or hits the cap, inform the player:
  - Example: `"Structure extends beyond completion limit; some parts may be cut off."`

### 4.2 Visual Feedback

- Structure-completion chunks spawn with the same particle/sound effects as normal chunks.
- All chunks (original + structure) appear simultaneously (within the same tick-bounded job sequence).

### 4.3 Cost & Consumption

- Structure completion is **free** — the player pays only for the original Chunk Spawner.
- This is a quality-of-life feature, not an additional resource sink.
- The original spawner still breaks and drops loot per existing behavior.

## 5. Implementation Approach

### 5.1 Integration Points

| Component | Changes Required |
| :--- | :--- |
| `ChunkCopyService` | Add structure detection after chunk copy; return list of structure-completion chunk positions. |
| `ChunkExpansionManager` | After primary chunk completes, enqueue structure-completion chunks. |
| `StartingAreaManager` | After copying each starting chunk, collect structure chunks; batch-copy all. |
| `PlayableAreaData` | Track structure-triggered chunks with metadata. |
| `BrightbronzeConfig` | Add `enableStructureCompletion` and `maxStructureCompletionChunks`. |

### 5.2 Structure Query API

Minecraft provides structure information via:

- `ChunkAccess.getAllStarts()` — Returns `Map<Structure, StructureStart>` for structures that **start** in this chunk.
- `ChunkAccess.getAllReferences()` — Returns `Map<Structure, LongSet>` for structures that **pass through** this chunk.
- `StructureStart.getBoundingBox()` — Returns the full `BoundingBox` of the structure.

### 5.3 Algorithm Outline

```
function collectStructureCompletionChunks(sourceLevel, triggerChunkPos, biomeId):
    if not config.enableStructureCompletion:
        return empty result
    
    discoveredStructures = Set<StructureStart>()
    chunksToSpawn = Set<ChunkPos>()
    chunksToProcess = Queue<ChunkPos>()
    processedChunks = Set<ChunkPos>()
    
    chunksToProcess.add(triggerChunkPos)
    
    while chunksToProcess is not empty:
        chunkPos = chunksToProcess.poll()
        
        if chunkPos in processedChunks:
            continue
        processedChunks.add(chunkPos)
        
        sourceChunk = sourceLevel.getChunk(chunkPos)
        
        // Find structures in this chunk
        for each (structure, start) in sourceChunk.getAllStarts():
            if start.isValid() and start not in discoveredStructures:
                if discoveredStructures.size() >= config.maxStructureCompletionStructures:
                    break  // Structure limit reached
                
                discoveredStructures.add(start)
                boundingBox = start.getBoundingBox()
                structureChunks = getChunksInBoundingBox(boundingBox)
                
                for each structureChunk in structureChunks:
                    if structureChunk not in playableAreaData.spawnedChunks:
                        chunksToSpawn.add(structureChunk)
                        if structureChunk not in processedChunks:
                            chunksToProcess.add(structureChunk)  // Cascade!
        
        // Also check structure references
        for each (structure, chunkRefs) in sourceChunk.getAllReferences():
            for each refChunkPos in chunkRefs:
                refChunk = sourceLevel.getChunk(refChunkPos)
                start = refChunk.getStartForStructure(structure)
                if start != null and start.isValid() and start not in discoveredStructures:
                    if discoveredStructures.size() >= config.maxStructureCompletionStructures:
                        break
                    
                    discoveredStructures.add(start)
                    boundingBox = start.getBoundingBox()
                    structureChunks = getChunksInBoundingBox(boundingBox)
                    
                    for each structureChunk in structureChunks:
                        if structureChunk not in playableAreaData.spawnedChunks:
                            chunksToSpawn.add(structureChunk)
                            if structureChunk not in processedChunks:
                                chunksToProcess.add(structureChunk)  // Cascade!
        
        // Check limits
        if discoveredStructures.size() >= config.maxStructureCompletionStructures:
            break
        if chunksToSpawn.size() >= config.maxStructureCompletionChunks:
            break
    
    // Remove the trigger chunk itself (already being spawned)
    chunksToSpawn.remove(triggerChunkPos)
    
    // Apply hard chunk cap
    if chunksToSpawn.size() > config.maxStructureCompletionChunks:
        chunksToSpawn = chunksToSpawn.take(config.maxStructureCompletionChunks)
        warnPlayer("Structure completion chunk limit reached")
    
    return StructureCompletionResult(
        chunksToSpawn,
        discoveredStructures.size(),
        hitStructureLimit = discoveredStructures.size() >= config.maxStructureCompletionStructures
    )
```

### 5.4 Tick-Bounded Execution

Structure-completion chunks are processed within the existing tick-bounded framework:

1. Primary chunk copy job completes.
2. Structure detection runs (fast — just bounding box queries).
3. Additional chunk copy jobs are enqueued to `ChunkExpansionManager`.
4. Jobs execute over subsequent ticks (respecting `chunkCopyLayersPerTick`).

This avoids server freezes even when completing large structures.

## 6. Edge Cases & Constraints

### 6.1 Large Structures

Some structures are very large:

| Structure | Typical Size | Chunk Span |
| :--- | :--- | :--- |
| Village | ~3–6 chunks | Moderate |
| Woodland Mansion | ~4–6 chunks | Moderate |
| Ocean Monument | ~4 chunks | Moderate |
| Stronghold | Variable, can be large | Large |
| Ancient City | ~6–9 chunks | Large |

The `maxStructureCompletionStructures` config (default: 16) limits structure count, and `maxStructureCompletionChunks` (default: 256) provides a hard chunk cap. Modpacks can adjust these based on their structure landscape.

### 6.2 Structures at Starting Area Edges

If the 3×3 starting area contains a structure that extends beyond those 9 chunks:

- Structure completion spawns the additional chunks needed.
- The starting area effectively becomes larger than 3×3.
- This is intentional — a complete village is better than a partial one.

### 6.3 Overlapping & Adjacent Structures (Cascading)

When structures are near each other:

- Completing one structure's chunks may reveal chunks containing another structure.
- The cascade logic automatically detects and completes the adjacent structure.
- Example: Village next to Pillager Outpost → both complete together.
- The 16-structure limit prevents unbounded expansion in dense areas.

### 6.4 Disabled Structures

If a modpack disables certain structures via data pack:

- Those structures won't generate in source dimensions.
- No special handling needed — the system only sees what exists.

### 6.5 Cross-Biome Structures

Some structures can span chunk boundaries where different biomes meet. In our system:

- Source dimensions are single-biome, so this doesn't apply within a source dimension.
- Structure completion chunks always use the triggering chunk's biome source.
- This maintains visual coherence.

## 7. Testing Requirements

### 7.1 Manual Testing Checklist

- [ ] Create new world; verify starting area villages are complete.
- [ ] Use Copper spawner near a village edge; verify village completes.
- [ ] Use Iron spawner in ocean; verify ocean monuments complete.
- [ ] Use Gold spawner; verify Nether structures (bastions, fortresses) complete.
- [ ] Use Diamond spawner; verify End cities complete.
- [ ] Test with `enableStructureCompletion: false`; verify partial structures appear.
- [ ] Test with `maxStructureCompletionStructures: 2`; verify structure cap is respected.
- [ ] Test with `maxStructureCompletionChunks: 16`; verify chunk cap is respected.
- [ ] Verify structure-completion chunks appear in playable area data.
- [ ] Verify frontier correctly includes structure-completion chunk edges.

### 7.2 Edge Case Testing

- [ ] Spawn chunk at corner where 4 structures meet — verify cascade completes all.
- [ ] Spawn chunk containing Ancient City (large structure).
- [ ] Spawn near village + pillager outpost cluster — verify both complete via cascade.
- [ ] Verify cascade stops at 16 structures.
- [ ] Verify cascade stops at 256 chunks.
- [ ] Test on dedicated server with multiple players.

---

## Appendix A: Implementation Checklist

> **⚠️ LIVING DOCUMENT**: Update this checklist as implementation progresses.

### Phase SC-1: Configuration & Infrastructure

**Status:** ✅ Complete

- [x] Add `enableStructureCompletion` to `BrightbronzeConfig.Data`
- [x] Add `maxStructureCompletionStructures` to `BrightbronzeConfig.Data` (default: 16)
- [x] Add `maxStructureCompletionChunks` to `BrightbronzeConfig.Data` (default: 256)
- [x] Add config defaults and JSON serialization
- [ ] Update config documentation in `ModpackConfiguration.md`

**Suggested commit message:** `feat(structure): Phase SC-1 — configuration for structure completion`

---

### Phase SC-2: Structure Detection Service

**Status:** ✅ Complete

- [x] Create `StructureCompletionService` class in `red.gaius.brightbronze.world.chunk`
- [x] Implement `collectStructureCompletionChunks(ServerLevel sourceLevel, ChunkPos chunkPos, Set<ChunkPos> alreadySpawned)` method
- [x] Query `ChunkAccess.getAllStarts()` for structure starts
- [x] Query `ChunkAccess.getAllReferences()` for structure references
- [x] Resolve structure bounding boxes via `StructureStart.getBoundingBox()`
- [x] Implement `getChunksInBoundingBox(BoundingBox box)` utility
- [x] Implement cascading structure detection with breadth-first search
- [x] Track discovered structures in `Set<StructureStart>` to avoid duplicates
- [x] Stop cascade when `maxStructureCompletionStructures` (16) reached
- [x] Return `StructureCompletionResult` with chunks, structure count, and limit flags

**Suggested commit message:** `feat(structure): Phase SC-2 — structure detection service`

---

### Phase SC-3: Integration with Chunk Copy Pipeline

**Status:** ✅ Complete

- [x] Modify `ChunkExpansionManager.ActiveJob` to call structure detection after primary chunk copy
- [x] Call `StructureCompletionService.collectStructureCompletionChunks()` after primary chunk copy
- [x] Modify `ChunkExpansionManager` to enqueue structure-completion chunks
- [x] Structure-completion chunks marked with `structureTriggered: true` metadata
- [x] Apply `maxStructureCompletionChunks` hard cap in `ChunkExpansionManager`
- [x] Structure-completion chunks do NOT break spawners
- [x] Structure-completion chunks do NOT send individual announcements

**Suggested commit message:** `feat(structure): Phase SC-3 — integrate structure completion with chunk copy`

---

### Phase SC-4: Starting Area Integration

**Status:** ✅ Complete

- [x] Modify `StartingAreaManager.initializeStartingArea()` to collect structure chunks
- [x] After copying each of the 9 starting chunks, run structure detection
- [x] Batch all structure-completion chunk positions (de-duped)
- [x] Copy structure-completion chunks before marking initialization complete
- [x] Ensure all structure chunks are added to `PlayableAreaData`

**Suggested commit message:** `feat(structure): Phase SC-4 — structure completion for starting area`

---

### Phase SC-5: Metadata & Tracking

**Status:** ✅ Complete

- [x] Extend `SpawnedChunkMeta` record with `structureTriggered` boolean field
- [x] Extend `SpawnedChunkMeta` record with optional `triggeringChunk` position
- [x] Update `PlayableAreaData` codec for new fields (with backward compatibility)
- [x] Store metadata when spawning structure-completion chunks
- [x] Ensure frontier detection includes structure-completion chunk edges (inherits from existing `addChunk()` behavior)

**Suggested commit message:** `feat(structure): Phase SC-5 — metadata tracking for structure chunks`

---

### Phase SC-6: Messaging & UX

**Status:** ✅ Complete

- [x] Modify spawn announcement to include structure completion info
- [x] Add message for structure count and additional chunks spawned (`announce_with_structures`)
- [x] Add warning message when `maxStructureCompletionStructures` cap is hit (`structure_limit_reached`)
- [x] Add warning message when `maxStructureCompletionChunks` cap is hit (`structure_chunk_limit_reached`)
- [x] Add translatable strings to `en_us.json`
- [x] Structure-completion chunks use same effects as normal chunks (inherits from existing code path)

**Suggested commit message:** `feat(structure): Phase SC-6 — messaging and UX for structure completion`

---

### Phase SC-7: Testing & Validation

**Status:** ⬜ Not Started

- [ ] Test village completion in starting area
- [ ] Test village completion via Copper spawner
- [ ] Test ocean monument completion via Iron spawner
- [ ] Test bastion/fortress completion via Gold spawner
- [ ] Test End city completion via Diamond spawner
- [ ] Test config toggle `enableStructureCompletion: false`
- [ ] Test structure cap `maxStructureCompletionStructures`
- [ ] Test chunk cap `maxStructureCompletionChunks`
- [ ] Verify no cascade loops
- [ ] Test on Fabric
- [ ] Test on NeoForge

**Suggested commit message:** `test(structure): Phase SC-7 — structure completion testing`

---

### Completion Summary

| Phase | Description | Status |
|-------|-------------|--------|
| SC-1 | Configuration & Infrastructure | ✅ Complete |
| SC-2 | Structure Detection Service | ✅ Complete |
| SC-3 | Chunk Copy Pipeline Integration | ✅ Complete |
| SC-4 | Starting Area Integration | ✅ Complete |
| SC-5 | Metadata & Tracking | ✅ Complete |
| SC-6 | Messaging & UX | ✅ Complete |
| SC-7 | Testing & Validation | ⬜ Not Started |

---

## Appendix B: API Reference

### Relevant Minecraft Classes (1.21.10)

```java
// Structure information in chunks
ChunkAccess.getAllStarts() → Map<Structure, StructureStart>
ChunkAccess.getAllReferences() → Map<Structure, LongSet>
ChunkAccess.getStartForStructure(Structure) → StructureStart

// Structure bounding box
StructureStart.getBoundingBox() → BoundingBox
StructureStart.isValid() → boolean
BoundingBox.minX(), minY(), minZ(), maxX(), maxY(), maxZ()

// Chunk position from block position
new ChunkPos(BlockPos)
new ChunkPos(int chunkX, int chunkZ)
ChunkPos.getMinBlockX(), getMinBlockZ(), getMaxBlockX(), getMaxBlockZ()
```

### Utility: Chunks in Bounding Box

```java
public static Set<ChunkPos> getChunksInBoundingBox(BoundingBox box) {
    Set<ChunkPos> chunks = new HashSet<>();
    int minChunkX = SectionPos.blockToSectionCoord(box.minX());
    int maxChunkX = SectionPos.blockToSectionCoord(box.maxX());
    int minChunkZ = SectionPos.blockToSectionCoord(box.minZ());
    int maxChunkZ = SectionPos.blockToSectionCoord(box.maxZ());
    
    for (int cx = minChunkX; cx <= maxChunkX; cx++) {
        for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
            chunks.add(new ChunkPos(cx, cz));
        }
    }
    return chunks;
}
```
