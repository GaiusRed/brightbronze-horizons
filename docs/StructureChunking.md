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
- Stop when **no new structures are found**, the **structure limit is reached**, or **cascade depth exceeded**.
- Default limits: **16 structures**, **5 cascade depth** per player-triggered spawn.
- Blacklisted structures (e.g., mineshafts) are skipped entirely and don't contribute to cascade.

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
| `maxStructureCascadeDepth` | Integer | `5` | Maximum BFS depth for cascade detection. Depth 0 = only trigger chunk's structures. Higher values allow discovering structures in structure-completion chunks. |
| `structureCompletionBlacklist` | String[] | `["minecraft:mineshaft", "minecraft:mineshaft_mesa"]` | Structure types to exclude from completion. Blacklisted structures are ignored entirely (no completion, no cascade). |

### 3.2 Structure Blacklist

Some structures are problematic for automatic completion:

- **Mineshafts** span dozens of chunks and frequently cascade into neighboring mineshafts, causing massive chunk explosions.
- The default blacklist excludes mineshafts, which are thematically meant to be explored gradually anyway.

Modpacks can customize the blacklist to exclude other problematic structures:

```json
"structureCompletionBlacklist": [
  "minecraft:mineshaft",
  "minecraft:mineshaft_mesa",
  "modname:sprawling_dungeon"
]
```

## 4. Player Experience

### 4.1 Order of Operations

When a player activates a Horizon Anchor:

1. **Validate** — Check if spawn is valid (adjacent, not already spawned, etc.)
2. **Break Anchor** — Immediately destroy the anchor block (confirms action accepted)
3. **Copy Primary Chunk** — Async/tick-bounded chunk copy begins
4. **Detect Structures** — After primary chunk completes, scan for structures
5. **If Structures Found:**
   - Announce: `"Structure detected: Village. Materializing 4 chunks..."`
   - Copy ALL structure chunks **synchronously** (appear at once, not gradually)
   - Announce: `"Village has materialized at (5, 3)."`
6. **If No Structures** — Standard announcement only

### 4.2 Messaging

Structure completion messaging:

- **Materializing notice:** `"Structure detected: Village. Materializing 4 chunks..."` — Sent before structure chunks are copied.
- **Complete notice:** `"Village has materialized at (5, 3)."` — Sent after all structure chunks are copied.
- **Partial notice:** `"Village partially materialized at (5, 3) - 3 chunks spawned (some areas already explored or limit reached)."` — Sent if some chunks couldn't be spawned.

Individual structure-completion chunks do **not** get their own announcements.

### 4.3 Conflict Handling

If structure completion would spawn chunks that already exist:

- **Skip existing chunks** — Never overwrite already-spawned chunks.
- **Track as partial** — Record that the structure was partially materialized.
- **Announce partial** — Let players know some parts weren't spawned.

Same handling applies when hitting the `maxStructureCompletionChunks` limit.

### 4.4 Visual Feedback

- Structure-completion chunks spawn with the same particle/sound effects as normal chunks.
- All structure chunks appear **simultaneously** (synchronous copy, not queued).
- This prevents the surreal "distant chunks first" effect from gradual loading.

### 4.5 Cost & Consumption

- Structure completion is **free** — the player pays only for the original Chunk Spawner.
- This is a quality-of-life feature, not an additional resource sink.
- The original spawner breaks **immediately** when activated (not after chunks complete).

## 5. Implementation Approach

### 5.1 Integration Points

| Component | Changes Required |
| :--- | :--- |
| `StructureCompletionService` | New service: detects structures in chunks, calculates completion chunks with cascading, returns structure names. |
| `ChunkExpansionManager` | Break spawner immediately on enqueue; after primary chunk, copy structure chunks synchronously; send materializing/complete messages. |
| `StartingAreaManager` | After copying each starting chunk, collect structure chunks; batch-copy all synchronously. |
| `PlayableAreaData` | Track structure-triggered chunks with metadata (`structureTriggered`, `triggeringChunk`). |
| `BrightbronzeConfig` | Add `enableStructureCompletion`, `maxStructureCompletionStructures`, `maxStructureCompletionChunks`. |

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

### 5.4 Synchronous Structure Completion

When a structure is detected, all associated chunks are copied **synchronously** (not queued):

1. Spawner breaks immediately (player gets instant feedback).
2. "Materializing" message is sent to all players.
3. Structure detection runs (fast — just bounding box queries).
4. All structure-completion chunks are copied **synchronously** via direct `ChunkCopyService.copyChunk()` calls.
5. "Complete" or "Partial" message is sent based on result.

This ensures structures appear all at once rather than gradually loading in, providing a better player experience. While this may cause a brief pause for very large structures, the `maxStructureCompletionChunks` cap (default: 256) bounds the worst-case impact.

> **Design Note:** The previous tick-bounded approach caused structures to load chunk-by-chunk, with distant chunks appearing first and gradually filling in toward the player. This was disorienting. The synchronous approach makes the entire structure "pop in" at once, which feels more intentional and magical.

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
- [ ] **NEW:** Verify mineshafts are NOT completed (blacklist working).
- [ ] **NEW:** Test custom blacklist entries.
- [ ] **NEW:** Test cascade depth limit with `maxStructureCascadeDepth: 1`.

### 7.2 Edge Case Testing

- [ ] Spawn chunk at corner where 4 structures meet — verify cascade completes all.
- [ ] Spawn chunk containing Ancient City (large structure).
- [ ] Spawn near village + pillager outpost cluster — verify both complete via cascade.
- [ ] Verify cascade stops at 16 structures.
- [ ] Verify cascade stops at 256 chunks.
- [ ] **NEW:** Verify cascade stops at depth limit.
- [ ] **NEW:** Verify blacklisted structures don't cascade.
- [ ] Test on dedicated server with multiple players.

---

## Appendix A: Implementation Checklist

> **⚠️ LIVING DOCUMENT**: Update this checklist as implementation progresses.

### Phase SC-1: Configuration & Infrastructure

**Status:** ✅ Complete

- [x] Add `enableStructureCompletion` to `BrightbronzeConfig.Data`
- [x] Add `maxStructureCompletionStructures` to `BrightbronzeConfig.Data` (default: 16)
- [x] Add `maxStructureCompletionChunks` to `BrightbronzeConfig.Data` (default: 256)
- [x] Add `maxStructureCascadeDepth` to `BrightbronzeConfig.Data` (default: 5)
- [x] Add `structureCompletionBlacklist` to `BrightbronzeConfig.Data` (default: mineshaft, mineshaft_mesa)
- [x] Add `getStructureCompletionBlacklistSet()` helper for efficient lookup
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
- [x] Return `StructureCompletionResult` with chunks, structure count, structure names, and limit flags
- [x] Extract human-readable structure names via `getStructureDisplayName()` (e.g., "Village", "Pillager Outpost")
- [x] Track `skippedExistingChunks` count for partial materialization detection
- [x] **NEW:** Implement `isStructureBlacklisted()` to skip blacklisted structures
- [x] **NEW:** Track cascade depth per chunk; stop cascade at `maxStructureCascadeDepth`
- [x] **NEW:** Jigsaw structure type displays as "Unknown Structure" (generic type used by many structures)

**Suggested commit message:** `feat(structure): Phase SC-2 — structure detection service`

---

### Phase SC-3: Integration with Chunk Copy Pipeline

**Status:** ✅ Complete

- [x] Modify `ChunkExpansionManager.ActiveJob` to call structure detection after primary chunk copy
- [x] Call `StructureCompletionService.collectStructureCompletionChunks()` after primary chunk copy
- [x] ~~Modify `ChunkExpansionManager` to enqueue structure-completion chunks~~ → **Changed:** Copy structure chunks **synchronously** (all at once)
- [x] Structure-completion chunks marked with `structureTriggered: true` metadata
- [x] Apply `maxStructureCompletionChunks` hard cap in `ChunkExpansionManager`
- [x] Structure-completion chunks do NOT break spawners
- [x] Structure-completion chunks do NOT send individual announcements
- [x] **NEW:** Break spawner **immediately** in `enqueue()` (not after chunk copy completes)
- [x] **NEW:** Structure chunks copied synchronously via `ChunkCopyService.copyChunk()` (appear all at once)
- [x] **NEW:** Skip chunks that already exist (never overwrite) and track as partial

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
- [x] **NEW:** `ExpansionResult` extended with `skippedExistingChunks` and `structureNames` fields

**Suggested commit message:** `feat(structure): Phase SC-5 — metadata tracking for structure chunks`

---

### Phase SC-6: Messaging & UX

**Status:** ✅ Complete

- [x] ~~Modify spawn announcement to include structure completion info~~ → **Changed:** Separate announcement flow
- [x] ~~Add message `announce_with_structures`~~ → **Removed:** Replaced with materializing/complete flow
- [x] ~~Add warning messages for limits~~ → **Changed:** Handled via partial materialization message
- [x] Add translatable strings to `en_us.json`
- [x] Structure-completion chunks use same effects as normal chunks (inherits from existing code path)
- [x] **NEW:** "Materializing" message: `"Structure detected: Village. Materializing 4 chunks..."`
- [x] **NEW:** "Complete" message: `"Village has materialized at (5, 3)."`
- [x] **NEW:** "Partial" message: `"Village partially materialized at (5, 3) - 3 chunks spawned (some areas already explored or limit reached)."`

**Translation keys added:**
- `message.brightbronze_horizons.spawner.structure_materializing`
- `message.brightbronze_horizons.spawner.structure_complete`
- `message.brightbronze_horizons.spawner.structure_partial`

**Suggested commit message:** `feat(structure): Phase SC-6 — messaging and UX for structure completion`

---

### Phase SC-7: Testing & Validation

**Status:** 🔄 In Progress

- [x] Test village completion in starting area *(confirmed working)*
- [ ] Test village completion via Copper spawner
- [ ] Test ocean monument completion via Iron spawner
- [ ] Test bastion/fortress completion via Gold spawner
- [ ] Test End city completion via Diamond spawner
- [ ] Test config toggle `enableStructureCompletion: false`
- [ ] Test structure cap `maxStructureCompletionStructures`
- [ ] Test chunk cap `maxStructureCompletionChunks`
- [ ] Verify no cascade loops
- [ ] Test spawner breaks immediately (before chunk copy)
- [ ] Test structure chunks appear all at once (synchronous)
- [ ] Test partial materialization when chunks already exist
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
| SC-7 | Testing & Validation | 🔄 In Progress |

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
