# Mod Compatibility Requirements: Worldgen Mod Support

**Document Version**: 1.2  
**Created**: 2026-02-19  
**Status**: ✅ Implemented  

---

## Executive Summary

Enable Brightbronze Horizons to coexist with ANY world generation mod while preserving our core void-world mechanic. Players should be able to install worldgen mods alongside Brightbronze Horizons and experience both systems working together—our controlled chunk progression combined with expanded biome variety through the Altered Horizon Anchor.

The Altered Horizon Anchor will automatically detect and provide access to any biome not from the `minecraft:` namespace, requiring zero configuration and supporting future worldgen mods out-of-the-box.

---

## Problem Statement

### Current Behavior

When worldgen mods are installed, they may:
1. Override `data/minecraft/dimension/overworld.json` via datapack
2. Replace the dimension's chunk generator configuration
3. Bypass our `VoidChunkGenerator` world preset
4. Result: Full terrain generates everywhere instead of void + controlled chunks

### Impact

- Core gameplay loop is broken
- Players cannot use Brightbronze Horizons with worldgen mods
- No error messaging—players may not understand why the mod "doesn't work"

---

## Goals

| Priority | Goal |
|----------|------|
| P0 | Brightbronze world preset produces void world regardless of installed worldgen mods |
| P1 | ANY non-`minecraft:` namespaced biome is accessible via Altered Horizon Anchor |
| P2 | Clear user feedback when modded biomes are detected |
| P3 | Graceful handling when no modded biomes are available |

### Non-Goals

- Supporting worldgen mods that fundamentally alter dimension mechanics (e.g., dimension-adding mods)
- Modifying how vanilla biomes work with existing chunk spawners
- Curating or whitelisting specific worldgen mods

---

## Requirements

### R1: Void World Enforcement

**Priority**: P0

The overworld MUST use `VoidChunkGenerator` when a world is created with the Brightbronze Horizons world preset, regardless of any datapacks or mods that attempt to override the dimension configuration.

#### Acceptance Criteria

- [x] New world created with Brightbronze preset has void terrain beyond spawned chunks
- [x] Works with any worldgen mod that overrides dimension configuration
- [x] Existing worlds created without the Brightbronze preset are NOT affected
- [x] Other dimension types (Nether, End, custom) are NOT affected

#### Technical Constraints

- Must activate AFTER datapack loading but BEFORE chunk generation begins
- Must only affect worlds using the Brightbronze world preset
- Should not break if worldgen mods are later removed from a save

---

### R2: Modded Biome Detection

**Priority**: P1

The system MUST detect ALL biomes that are not from the `minecraft:` namespace.

#### Acceptance Criteria

- [x] Enumerate all registered biomes at world load
- [x] Filter biomes where namespace ≠ `minecraft`
- [x] Return empty set if no modded biomes exist (do not error)
- [x] Detection works on both Fabric and NeoForge

#### Key Principle

Detection is **namespace-based**, not mod-based. Any biome with a non-`minecraft:` namespace (e.g., `terralith:*`, `biomesoplenty:*`, `someunknownmod:*`) is considered a modded biome. This ensures automatic support for any current or future worldgen mod without code changes.

---

### R3: Altered Horizon Anchor - Modded Biome Support

**Priority**: P1

The Altered Horizon Anchor MUST spawn chunks using modded biomes when any non-`minecraft:` biomes are available. This behavior mirrors the Coal Anchor pattern—it is a specialized anchor that only functions when its precondition is met.

#### Behavior

| Condition | Altered Horizon Anchor Behavior |
|-----------|--------------------------------|
| Modded biomes available (namespace ≠ `minecraft`) | Spawns chunk with a modded biome |
| No modded biomes available | **Fails gracefully** (does not consume anchor, shows message) |

#### Acceptance Criteria

- [x] Altered Horizon Anchor detects available modded biomes at use time
- [x] If modded biomes exist → selects one and spawns chunk
- [x] If NO modded biomes exist → fails gracefully without consuming the item
- [x] Player receives clear feedback on failure ("No modded biomes available")
- [x] Source dimensions are created correctly for any modded biome
- [x] Terrain generated in source dimensions reflects the modded biome's characteristics
- [x] Chunk copying works identically to vanilla biomes
- [x] If a previously-used mod is removed, graceful fallback occurs (no crash)

#### Biome Selection

The specific method for selecting which modded biome to use (random, weighted, player choice, etc.) is left to implementation discretion, but must align with existing anchor patterns in the mod.

---

### R4: Source Dimension Generation for Modded Biomes

**Priority**: P1

Source dimensions created for modded biomes MUST generate terrain that accurately represents the modded biome.

#### Acceptance Criteria

- [x] Any `<namespace>:<biome_id>` source dimension generates appropriate terrain
- [x] Modded surface builders, features, and structures appear in source dimensions
- [x] Biome-specific mob spawning works in source dimensions
- [x] Works regardless of which mod registered the biome

#### Technical Consideration

Modded biomes may rely on custom noise settings, density functions, or surface rules provided by their parent mod. The source dimension generator must incorporate these when available.

---

### R5: User Feedback

**Priority**: P2

Players should receive clear feedback about modded biome availability.

#### Acceptance Criteria

- [x] Log message at startup indicating count of modded biomes detected
- [x] Clear in-game message when Altered Horizon Anchor fails due to no modded biomes
- [x] No console spam—one-time detection logging only

#### Optional (Nice-to-Have)

- Tooltip on Altered Horizon Anchor indicating number of available modded biomes

---

### R6: Graceful Degradation

**Priority**: P3

The system MUST handle edge cases without crashing.

#### Scenarios to Handle

| Scenario | Expected Behavior |
|----------|-------------------|
| No worldgen mods installed | Altered Horizon Anchor fails gracefully with message |
| Worldgen mod removed after world creation | Chunks already copied remain; new anchor use falls back gracefully |
| Biome ID no longer exists | Log warning, skip biome, do not crash |
| Source dimension creation fails for modded biome | Log error, return null, allow fallback handling |

---

## Out of Scope

The following are explicitly NOT part of this feature:

1. **Mod-specific detection**: We do not check for specific mod IDs; only biome namespaces matter
2. **Automatic biome mapping**: We do not attempt to map modded biomes to vanilla equivalents
3. **Config file for biome whitelisting**: Detection is automatic by namespace
4. **Cross-mod biome mixing**: A biome from mod A will not contain features from mod B
5. **Nether/End modded biomes**: Only Overworld biomes are supported initially
6. **Worldgen mod soft-dependency**: Brightbronze Horizons must work identically with or without worldgen mods installed

---

## Testing Requirements

### Manual Test Cases

Testing will use Terralith and Biomes O' Plenty as representative worldgen mods, but the implementation must not be specific to these mods.

1. **Worldgen mod installed**: Create world → verify void + village → use Altered Horizon Anchor → verify modded biome spawns
2. **Multiple worldgen mods**: Install both test mods → verify biomes from both are available
3. **Mod removal mid-save**: Remove worldgen mod from existing save → verify no crash → verify Altered Horizon Anchor fails gracefully
4. **No worldgen mods**: Fresh install with no worldgen mods → use Altered Horizon Anchor → verify graceful failure with message
5. **Non-Brightbronze world**: Create default world with worldgen mod → verify mod works normally (we don't interfere)
6. **Unknown worldgen mod**: Install a worldgen mod NOT in our test suite → verify its biomes are detected and usable

### Automated Test Considerations

- Unit tests for biome namespace filtering
- Unit tests for graceful failure when no modded biomes available
- Integration tests may require mock biome registrations

---

## Success Metrics

| Metric | Target |
|--------|--------|
| Void world works with any worldgen mod installed | 100% |
| Non-`minecraft:` biomes detected automatically | 100% |
| Graceful failure when no modded biomes available | No crash, clear message |
| No crashes from mod detection/removal | 0 crashes |

---

## Dependencies

- Existing `VoidChunkGenerator` implementation
- Existing `SourceDimensionManager` implementation  
- Existing Altered Horizon Anchor item and mechanics
- Existing biome rule system (`BiomeRuleManager`)

---

## Resolved Questions

1. **Biome selection method**: ✅ Random selection from all available non-`minecraft:` biomes, consistent with existing anchor patterns.
2. **Biome count display**: ⏭️ Deferred. Log message shows count; tooltip enhancement is a future nice-to-have.
3. **Structure handling**: ✅ Yes, modded biomes' structures generate correctly in source dimensions (verified with Terralith villages, terrain features).

---

## Appendix A: Worldgen Mod Architecture Patterns

This section documents common patterns used by worldgen mods for reference. The implementation should NOT be specific to any of these mods.

### Pattern 1: Datapack Dimension Override

Some mods ship `data/minecraft/dimension/overworld.json` that replaces the vanilla dimension configuration. Example: Terralith.

### Pattern 2: Runtime Biome Injection

Some mods use libraries like TerraBlender to inject biomes at runtime without overriding the dimension file. Example: Biomes O' Plenty.

### Pattern 3: Hybrid Approach

Some mods may combine both approaches or use other mechanisms entirely.

**Key insight**: Regardless of how mods register their biomes, all biomes end up in the biome registry with their mod's namespace. Our namespace-based detection will find them all.

---

## Appendix B: Reference Materials

- [ExternalMods.md](ExternalMods.md) - Compatibility testing documentation
- [InitialRequirements.md](InitialRequirements.md) - Original mod PRD (Phase 6A: Void World)

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-02-19 | — | Initial draft |
| 1.1 | 2026-02-19 | — | Made generic for any worldgen mod; Altered Horizon Anchor now fails gracefully when no modded biomes available |
| 1.2 | 2026-02-19 | — | Marked as Implemented. All acceptance criteria verified on Fabric and NeoForge with Terralith 2.5.13 and Biomes O' Plenty. Coal spawner biome detection fix implemented (falls back to recorded biome when VoidChunkGenerator returns plains). |
