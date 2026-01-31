# Brightbronze Horizons — Product Requirements Document (PRD)

**Last updated:** 2026-01-31

This document defines the intended player experience and the engineering requirements for Brightbronze Horizons (Fabric + NeoForge, Architectury). It is written for implementers; it focuses on behavior and interfaces, not specific code.

## 1. Vision & Scope

Brightbronze Horizons is a modern spiritual successor to the “Chunk By Chunk” mod concept: a constrained starting area that expands by spawning additional terrain chunks on demand.

Key differentiators from Chunk By Chunk:
- Modern Minecraft + modern multi-loader support (this repo targets Minecraft 1.21.10 with Fabric + NeoForge).
- A revamped item/progression path centered on Brightbronze (amalgam → ingot → tools/armor), inspired by Brightbronze Logistics.
- No reuse of Chunk By Chunk items/recipes: only the high-level idea and certain implementation patterns (e.g., “generation/source dimension” and hidden dimensions) are inspirations.

### 1.1 Inspirations (Non-functional)

- Chunk By Chunk (in `tmp/chunkbychunk`)
	- Strong core idea: expand playable world by copying chunks from a separate “generation/source” dimension.
	- Hidden dimension approach is valuable, but we want to evolve it to be biome-coherent.
- Brightbronze Logistics (in `tmp/brightbronze-logistics`)
	- Reuse the Brightbronze resource line (Brightbronze Amalgam → Brightbronze Ingot → Brightbronze tools/armor) and its approximate position in Minecraft progression.

### 1.2 Goals

- Provide a chunk-expansion gameplay loop that is stable, configurable, and friendly to modpacks.
- Default experience:
	- Start in a 3×3 set of chunks (9 chunks) rather than a single chunk.
	- Start in a Plains biome with a Village present.
- Support a “biome-coherent hidden dimension” model:
	- For every biome, there is a corresponding hidden/source dimension comprised of only that biome.
	- Spawned chunks sourced from a biome look and feel like they belong together.
- Tiered chunk spawning (progression + gating):
	- Coal tier: common Overworld biome chunks.
	- Iron tier: rarer Overworld biomes.
	- Gold tier: Nether biomes.
	- Emerald tier: modded/custom biomes from supported mods.
	- Diamond tier: End biomes.
- Brightbronze-based progression:
	- Brightbronze Ingots are a core material and are also used in chunk spawner recipes.
- Provide a highly data-driven configuration surface so modpack authors can:
	- Assign biomes/tags to tiers.
	- Configure block inclusion/exclusion (e.g., ore control).
	- Configure spawned mobs/conditions.
	- Toggle/adjust natural generation behaviors per-biome or per-tier.

### 1.3 Non-Goals

- Porting or reintroducing any Chunk By Chunk-specific items, crafting recipes, or progression artifacts.
- Providing a full modpack, quest book, or progression guide as part of this repo.
- Implementing bespoke worldgen for every modded biome; instead, focus on a framework that can integrate with other mods via tags/data.

## 2. Player Experience Summary

### 2.1 Core Loop

1. Player starts in a small, curated slice of the world (default: 3×3 chunks) with basic resources and a village nearby.
2. Player crafts Brightbronze and then crafts/uses tiered Chunk Spawners.
3. Each activation spawns exactly one new chunk adjacent to the existing playable area, pulled from a biome-coherent source.
4. Progression unlocks additional biome categories (rarer Overworld → Nether → modded → End), expanding resource availability.

### 2.2 Starting World Defaults

Default new-world behavior (configurable):

- Spawn platform/region: generate and expose a 3×3 chunk area centered on the initial spawn.
- Biome: Plains.
- Structure: a Village should be present within the starting 3×3 area.

Behavioral requirements:

- If a perfect start cannot be found quickly (e.g., seed constraints), the system should degrade gracefully:
	- Prioritize Plains biome first.
	- Then prioritize presence of a Village, ideally centered on the 3×3.
	- If a Village cannot be guaranteed, ensure the start is still safe/playable and does not soft-lock progression.
- The start must be deterministic per-world seed + config to support reproducibility for modpack authors.

## 3. Chunk Spawning System

This section defines the required gameplay and system behavior for the “Chunk Spawner” mechanic (the primary expansion tool).

### 3.1 Chunk Spawner Tiers

Chunk Spawners exist in the following tiers:

- **Coal**: common Overworld biome chunks.
- **Iron**: rarer Overworld biome chunks.
- **Gold**: Nether biome chunks.
- **Emerald**: custom/modded biomes (from supported mods).
- **Diamond**: End biome chunks.

Dimension scope:

- The Overworld is the only dimension that uses the chunk-expansion system.
- The Nether and The End still exist as normal playable dimensions and are generated normally.
- Gold/Diamond tiers can spawn Nether/End biome chunks **into the Overworld** (as pocket terrain), without changing the existence or accessibility of the Nether/End.

Pocket-terrain expectation:

- Spawned Nether/End biome chunks include native blocks and fluids as generated.
- There are no additional safety constraints; fluids may flow naturally across chunk boundaries.

Tier determines:

- Which biome pool is eligible for random selection.
- Whether and how mobs are spawned when a new chunk appears.
- Potentially, additional constraints such as chunk hazards or rarity weighting.

#### 3.1.1 Default Vanilla Biome Pools (Curated)

These defaults are intended to ship as the mod’s baseline data pack rules/tags. Modpacks can override them.

Overworld tiers should target **surface biomes** by default (exclude cave biomes such as Lush Caves / Dripstone Caves / Deep Dark unless a pack explicitly opts in).

**Coal (Common Overworld) — default list:**

- Plains
- Forest
- Birch Forest
- Taiga
- Snowy Plains
- Savanna
- Desert

**Iron (Rare Overworld) — default list:**

- Dark Forest
- Jungle
- Bamboo Jungle
- Sparse Jungle
- Badlands (and variants)
- Swamp
- Mangrove Swamp
- Windswept Hills family (Windswept Hills / Windswept Forest / Windswept Gravelly Hills / Windswept Savanna)
- Meadow
- Cherry Grove
- Grove
- Snowy Slopes
- Jagged Peaks
- Frozen Peaks
- Stony Peaks
- Mushroom Fields

**Gold (Nether) — default list:** all vanilla Nether biomes.

**Diamond (End) — default list:** all vanilla End biomes.

### 3.2 Crafting Recipe Requirements

Chunk Spawner recipe pattern (all tiers):

- Pattern:
	- Row 1: A A A
	- Row 2: A B A
	- Row 3: A C A
- Ingredients:
	- **A** = Brightbronze Ingot
	- **B** = block representing the tier (default mapping below; configurable)
	- **C** = Campfire

Notes:

- The design intends Brightbronze Ingots to be the shared cost across all tiers.
- The “tier block” mapping must be configurable to support modpacks.

Default tier block mapping for **B**:

- Coal: Block of Coal
- Iron: Block of Iron
- Gold: Block of Gold
- Emerald: Block of Emerald
- Diamond: Block of Diamond

### 3.3 Placement & Expansion Rules

Core requirements:

- Using/activating a Chunk Spawner expands the playable area by adding exactly **one** chunk adjacent to the existing frontier.
- Spawned chunks must connect to existing chunks along an edge (no diagonals-only connections).
- The Chunk Spawner block must be placed at the **edge of a chunk** to indicate the intended expansion direction.
	- If placed in a corner position that touches two valid edges, the mod chooses a random valid edge direction.

Selection requirements:

- **Biome selection is random** within the tier’s eligible biome pool.
- **Chunk location is not random:** the source chunk coordinates must match the destination chunk coordinates (see Section 4.3).

Multiplayer/permissions:

- Anyone can spawn chunks.
- No cooldown beyond crafting cost.

Expansion limits:

- There is no hard limit on chunk expansion (no maximum radius/total spawned chunks) by design.

Constraints:

- The system must prevent spawning into protected/invalid positions (e.g., unloaded constraints, dimension boundaries, reserved areas).
- The system must be robust on dedicated servers with multiple players.

Failure behavior:

- If a Chunk Spawner tier has zero eligible biomes (e.g., Emerald tier by default), activation must fail gracefully:
	- Do not create a chunk.
	- Provide clear player feedback.
	- Do not consume the block/item unless explicitly configured to do so.

### 3.4 Mob Spawning Rules for Newly Spawned Chunks

When a chunk is spawned/added via the Chunk Spawner, the mod may also trigger a “surface mob spawn event” on that chunk.

Tier-based rules:

- **Coal** and **Iron** tier:
	- Surface mobs spawn **only if it is night** at the target chunk location.
- **Gold** and **Diamond** tier:
	- Surface mobs spawn **regardless of time**.

Notes:

- The actual mobs and spawn counts are configured (see Configuration section).
- Mob spawning is a **one-time scripted spawn** that occurs when the chunk is created.
- The mob spawning logic must respect game difficulty and relevant gamerules (e.g., `doMobSpawning`).
- The mob spawning logic should be bounded (avoid runaway spawns or performance spikes).

## 4. Biome-Coherent Hidden Dimensions (Source Worlds)

Chunk By Chunk’s “generation dimension + copy chunks out of it” model is a strong foundation. Brightbronze Horizons extends that idea:

### 4.1 One Hidden Dimension per Biome

Requirement:

- For every biome that Brightbronze Horizons can spawn chunks from, the mod must support a corresponding hidden/source dimension that generates as that single biome.

Design intent:

- If a player is expanding off a specific biome, successive chunks sourced from that biome should visually and mechanically match, creating a coherent region.

### 4.2 Source Dimension Lifecycle

Requirements:

- Source dimensions are not intended for normal player habitation.
- The mod must decide whether source dimensions are:
	- Always present, or
	- Lazily created the first time a biome is requested.

Operational constraints:

- World size and disk usage must be controlled.
- Chunk generation/copying must not freeze the server thread for long durations.
- Behavior must be consistent between singleplayer and dedicated servers.

### 4.3 Chunk Sourcing Rules

When spawning a new chunk in the playable world:

- The system selects a biome at random from the eligible pool for the tier.
- The system identifies the corresponding biome’s source dimension.
- The system copies or reconstitutes a chunk from that source dimension into the playable world at the target coordinates.

Coordinate matching requirement:

- The source chunk position must match the destination chunk position (same chunk X/Z coordinates).
- This enables intentional “structure completion” gameplay (e.g., if a structure is partially present in an already-spawned chunk, placing the next chunk correctly can reveal the adjacent portion if the same biome is selected again).

Nether coordinate scale:

- Coordinate matching is literal chunk X/Z equality.
- Do not apply Nether’s typical 8:1 coordinate scaling to chunk spawning/copying.

Quality requirements:

- Spawned terrain should be fully generated (surface, carvers, vegetation, structures as configured) and stable.
- Structures must generate naturally in source dimensions and be copied into the playable world with the chunk. There are no additional structure allow/deny rules.

## 5. Brightbronze Progression (Core Material)

Brightbronze Horizons uses Brightbronze as the foundation for the new progression.

### 5.1 Materials

- **Brightbronze Amalgam** (a crafted intermediate material; named “Brightbronze Amalgam” in Brightbronze Logistics)
- **Brightbronze Ingot** (smelted from the amalgam)
- **Brightbronze Block** (crafted from ingots)
- **Brightbronze Tools + Armor** (standard tool/armor set)

### 5.2 Intended Position in Vanilla Progression

Design intent (from Brightbronze Logistics inspiration):

- Brightbronze Amalgam should be craftable relatively early using common metals.
- Brightbronze tools/armor should feel like a meaningful upgrade that helps the player expand safely and progress into higher tiers.

### 5.3 Recipe Intent (Described, Not Implemented Here)

The PRD defines the *intent*; engineers will implement via standard Minecraft data recipes.

Minimum requirements:

- Brightbronze Amalgam is crafted from a small set of common metal ingredients (e.g., copper + small amounts of iron and gold).
- Brightbronze Amalgam smelts into Brightbronze Ingots.
- Brightbronze tools/armor use conventional vanilla patterns with Brightbronze Ingots.

Integration requirement:

- Brightbronze Ingots are the shared “A” ingredient for all Chunk Spawner tiers.

## 6. Configuration & Data-Driven Extensibility

This mod must be straightforward for modpack creators to tune, especially when adding other biome-providing mods.

### 6.1 Configuration Surfaces

The mod should separate configuration into two layers:

1. **Runtime config (server/world config):**
	 - Small set of toggles and defaults (e.g., starting 3×3 behavior, mob-spawn-on-chunk-spawn toggle, per-tier enable/disable).
	 - Intended to be edited in `config/` and synchronized/validated on dedicated servers.

2. **Data-driven rules (data pack / resource reloadable):**
	 - Biome/tier mapping, block filters, spawn tables, and generation controls.
	 - Intended to be overridden by modpacks without requiring code.

### 6.2 Biome Rule Model (What Needs to Be Expressible)

At minimum, the data-driven rules must allow defining entries that target a set of biomes using biome tags (e.g., a tag reference like “all biomes in X category”). Each entry should be able to define:

- **Biome selector:**
	- A biome tag reference (primary)
	- Optional explicit biome allow/deny lists (secondary)

- **Tier assignment:** coal / iron / gold / emerald / diamond

- **Block post-processing filters (applied to spawned chunks):**
	- **Excludes:** blocks/tags to remove or replace
	- **Includes:** blocks/tags to keep even if excluded by a broader rule
	- Replacement behavior: excluded blocks are replaced with **air**.

- **Mob spawn table for “chunk spawned” events:**
	- Entity type
	- Min/max count
	- Conditions such as night-only, difficulty gates, and per-tier enablement

**Generation controls (target: true gating; fallback: best-effort):**

- Prefer “true include/exclude generation” in the source dimensions for non-structure worldgen content (e.g., ores, vegetation/features, liquids).
- If true gating proves infeasible or too fragile for modded worldgen, the system may fall back to best-effort post-processing/stripping.

### 6.3 Format Options (Pros/Cons)

Option A — **Single JSON list file** (similar to the example you provided):

- Pros: one place to edit; easy to understand.
- Cons: merge conflicts; hard to override parts from multiple mods/packs; large files become unwieldy.

Option B — **Directory of small rule files (recommended):**

- One rule per file (or per biome group) under a stable namespace.
- Pros: easy overrides, easy pack composition, simple diffing.
- Cons: more files; needs clear precedence rules.

Option C — **Tags-first mapping + optional overrides:**

- Use biome tags to assign tiers, and separate files for block/mob behavior.
- Pros: extremely flexible; leverages existing tag workflows.
- Cons: requires pack authors to be comfortable with tags.

### 6.4 Recommendation

Recommend **Option B (directory of rule files)** combined with a small runtime config.

Rationale:

- Modpack authors can add support for a new biome mod by dropping in one additional rule file (and optionally a biome tag file), without touching a monolithic config.
- It composes cleanly when multiple content mods are present.
- It fits Minecraft’s established “data pack override” model.

Precedence requirements:

- Rules must have an explicit ordering mechanism (e.g., numeric priority) to resolve overlaps.
- When multiple rules match a biome, the highest priority rule wins for tier assignment.
- Block filtering and mob spawning use “first-match wins” after applying priority ordering.

### 6.5 Additional Fields Likely Needed (Beyond the Initial Sketch)

To make the system robust for modpacks, expect to require:

- **Weighting / rarity:** when multiple biomes are eligible for a tier, weights control selection.
- **Chunk count & shape:** chunk spawner always creates 1 chunk; any “shape” concerns are about frontier choice rules (if any) and placement constraints.
- **Adjacency constraints:** whether new chunks must share biome with the chunk they attach to.
- **Safety rules:** spawn-protection radius, safe-spawn checks, lava/water replacement toggles.
- **Spawn caps:** prevent excessive mob spawns on repeated chunk generation.

## 7. Compatibility, Multiplayer, and Operational Requirements

### 7.1 Mod Loader Support

- Must behave consistently on Fabric and NeoForge.
- Must work in singleplayer and dedicated servers.
- Any client-only functionality (UI, particles, etc.) must not be required for server correctness.

### 7.2 Modded Biomes (Emerald Tier)

Requirements:

- The system must be able to target modded biomes via biome tags.
- The system must not assume any specific biome IDs beyond vanilla.
- “Supported mods” should be defined as “mods that register biomes using the standard biome registry” (i.e., no bespoke integration required beyond tags), unless we explicitly add compat packs.

Default behavior:

- Emerald tier ships with no eligible biomes by default.
- The Emerald tier Chunk Spawner remains craftable, but will not spawn chunks until a modpack adds rules/tags that make biomes eligible.

Forward-looking requirement:

- Once the core mod is working, we plan to add several first-party compatibility packs/mods for popular biome/worldgen mods.

### 7.3 Performance and Stability

Chunk spawning must not become a lag machine.

Requirements:

- Chunk generation/copying must be bounded and avoid long main-thread stalls.
- The system must prevent runaway source-dimension creation (e.g., one dimension per biome can be large).
- The system must provide server operators with tools to manage disk usage (e.g., pruning unused source chunks and reporting per-biome source-dimension disk usage).

### 7.4 Persistence and Determinism

- Given a fixed world seed + fixed config + fixed data pack, the sequence of biome selections and chunk placements must be reproducible.
- The system must persist any state required to ensure reproducibility across restarts (e.g., RNG state for selection, already-generated chunk IDs).

## Open Questions

None at this time.

---

## Appendix A: Implementation Checklist

> **⚠️ LIVING DOCUMENT**: This appendix is a **checklist** that tracks implementation progress. It must be updated after every phase is completed. Mark items with `[x]` when done.
>
> **Git Workflow**: After completing each phase, the implementer will provide a suggested commit message. The user will then **manually run `git commit`** before proceeding to the next phase. Do not auto-commit.
>
> **🎮 RUNNABLE REQUIREMENT**: Every completed phase **MUST** result in a mod that launches successfully on **both Fabric and NeoForge**. If a phase adds items/blocks, placeholder textures and models must be included. Phases are not considered complete until `.\gradlew :fabric:runClient` and `.\gradlew :neoforge:runClient` both launch without crashes.

---

### Recommended Implementation Order

1. Phase 1 → Core infrastructure (must be first)
2. Phase 2 → Brightbronze items (standalone, establishes patterns)
3. Phase 5 → Tier/biome pool system (needed by spawners)
4. Phase 4 → Source dimensions (core mechanic)
5. Phase 3 → Chunk spawners (ties it together)
6. **Phase 6A → Void world type (CRITICAL - must come before Phase 6)**
7. Phase 6 → Starting area (world initialization)
8. Phase 7 → Mob spawning (enhancement)
9. Phase 8 → Configuration (extensibility)
10. Phase 9 → Block post-processing (data-driven)
11. Phase 10–11 → Server robustness (hardening)
12. Phase 12 → Polish (final)
13. Phase 13 → Testing (ongoing + final)

---

### Phase 1: Core Infrastructure & Registry Setup

**Status:** ✅ COMPLETED

#### 1.1 Project Foundation
- [x] Rename/refactor main mod class — Replace `ExampleMod.java` with `BrightbronzeHorizons.java`
- [x] Create package structure:
  - `red.gaius.brightbronze` — mod root
  - `red.gaius.brightbronze.registry` — deferred registries
  - `red.gaius.brightbronze.fabric` — Fabric loader
  - `red.gaius.brightbronze.neoforge` — NeoForge loader

#### 1.2 Registry Classes
- [x] Create `ModBlocks.java` — DeferredRegister for blocks
- [x] Create `ModItems.java` — DeferredRegister for items
- [x] Create `ModBlockEntities.java` — DeferredRegister for block entities
- [x] Create `ModCreativeTabs.java` — Creative mode tab for mod items
- [x] Create `ModArmorMaterials.java` — Brightbronze armor material registration
- [x] Create `ModDimensions.java` — Dimension type/key registration for source dimensions

#### 1.3 Minimum Viable Assets (required for launch)
- [x] Item models — JSON models for all registered items
- [x] Block models — JSON models + blockstate for all registered blocks
- [x] Placeholder textures — Simple colored textures for all items/blocks
- [x] Language file — Basic `en_us.json` with item/block names
- [x] **Verify Fabric launch** — `.\gradlew :fabric:runClient` starts without crash
- [x] **Verify NeoForge launch** — `.\gradlew :neoforge:runClient` starts without crash

> **Implementation Note (MC 1.21.10):** Blocks and Items require `Properties.setId(ResourceKey)` to be called BEFORE construction. This is a breaking API change from 1.21. Without this, NeoForge throws "Block id not set" / "Item id not set" during registration.

**Suggested commit message:** `feat: Phase 1 — core infrastructure and registry setup`

---

### Phase 2: Brightbronze Material Line

**Status:** ✅ COMPLETED

#### 2.1 Items
- [x] Brightbronze Amalgam — Basic item (crafted intermediate)
- [x] Brightbronze Ingot — Basic item (smelted from amalgam)
- [x] Brightbronze Nugget — Basic item (for conversion)

#### 2.2 Block
- [x] Block of Brightbronze — Storage block with `BlockItem`

#### 2.3 Tools (using 1.21.10 `ToolMaterial` record)
- [x] Create `BRIGHTBRONZE_TOOL_MATERIAL` — Tool material definition (between iron and diamond)
- [x] Brightbronze Sword
- [x] Brightbronze Pickaxe
- [x] Brightbronze Axe
- [x] Brightbronze Shovel
- [x] Brightbronze Hoe

#### 2.4 Armor
- [x] Create `BRIGHTBRONZE` ArmorMaterial — Armor material definition
- [x] Brightbronze Helmet
- [x] Brightbronze Chestplate
- [x] Brightbronze Leggings
- [x] Brightbronze Boots

#### 2.5 Recipes (data pack JSON)
- [x] Brightbronze Amalgam recipe — Copper + Iron + Gold crafting
- [x] Brightbronze Ingot smelting — Amalgam → Ingot (furnace + blast furnace)
- [x] Brightbronze Block crafting — 9 Ingots → Block
- [x] Brightbronze Ingot from Block — Block → 9 Ingots
- [x] Brightbronze Nugget conversions — 9 Nuggets ↔ Ingot
- [x] Tool recipes — Standard patterns with Brightbronze Ingots
- [x] Armor recipes — Standard patterns with Brightbronze Ingots

#### 2.6 Assets
- [x] Textures — Real textures from brightbronze-logistics reference project
- [x] Models — Item/block models (JSON)
- [x] Item model definitions — MC 1.21+ `items/` directory format
- [x] Language file — `en_us.json` translations

> **Implementation Note (MC 1.21.10):** 
> - Recipe format changed in 1.21.2+: ingredients use strings (`"minecraft:item_id"`) not objects (`{"item": "minecraft:item_id"}`).
> - MC 1.21+ requires an `items/` directory with item model definitions that point to models in `models/item/`.

**Suggested commit message:** `feat: Phase 2 — Brightbronze material line (items, tools, armor, recipes)`

---

### Phase 3: Chunk Spawner System

**Status:** ✅ COMPLETED

#### 3.1 Chunk Spawner Blocks
- [x] Create `ChunkSpawnerBlock` base class — Concrete block with tier parameter
- [x] Coal Chunk Spawner — Tier: Coal (common Overworld)
- [x] Iron Chunk Spawner — Tier: Iron (rare Overworld)
- [x] Gold Chunk Spawner — Tier: Gold (Nether biomes)
- [x] Emerald Chunk Spawner — Tier: Emerald (modded biomes)
- [x] Diamond Chunk Spawner — Tier: Diamond (End biomes)

#### 3.2 Chunk Spawner Logic (in ChunkSpawnerBlock)
- [x] Edge detection logic — `getChunkEdgeDirections()` detects if placed at chunk edge
- [x] Direction selection — Random selection when at corner
- [x] Activation handler — `attemptChunkSpawn()` wired to BiomePoolManager, SourceDimensionManager, ChunkCopyService

> **Implementation Note:** BlockEntity was intentionally omitted. All spawning logic is handled synchronously in `ChunkSpawnerBlock.useWithoutItem()` without needing persistent state. A BlockEntity may be added later for visual effects or cooldowns if needed.

#### 3.3 Chunk Spawner Recipes
- [x] Recipe pattern implementation (A A A / A B A / A C A)
- [x] Coal Spawner recipe — B = Block of Coal
- [x] Iron Spawner recipe — B = Block of Iron
- [x] Gold Spawner recipe — B = Block of Gold
- [x] Emerald Spawner recipe — B = Block of Emerald
- [x] Diamond Spawner recipe — B = Block of Diamond

**Suggested commit message:** `feat: Phase 3 — chunk spawner blocks and recipes`

---

### Phase 4: Biome-Coherent Source Dimensions

**Status:** ✅ COMPLETED

#### 4.1 Dimension Infrastructure
- [x] Create `SourceDimensionManager` — Manages per-biome source dimensions
- [x] Create `SingleBiomeChunkGenerator` — Custom chunk generator for single-biome worlds
- [x] Lazy dimension creation — Create source dimensions on-demand when first needed
- [x] Dimension registry integration — Dynamic dimension registration

#### 4.4 Platform-Specific Dynamic Dimension Creation
- [x] Fabric dynamic dimension registration — Use mixin accessor for `MinecraftServer.levels` map
- [x] NeoForge dynamic dimension registration — Use same mixin-based approach as Fabric
- [x] Common abstraction — `DimensionHelper` with `@ExpectPlatform` for cross-loader dimension creation

#### 4.2 Chunk Copying System
- [x] Create `ChunkCopyService` — Orchestrates chunk copying from source to playable
- [x] Block state copying — Copy all block states from source chunk
- [x] Block entity copying — Copy block entities (chests, spawners, etc.) using MC 1.21 `TagValueInput`/`TagValueOutput` API
- [x] Entity copying — Copy mobs and other entities from source chunk to target chunk
- [x] Biome data preservation — Ensure biome data is set correctly in target (via `FixedBiomeSource`)

#### 4.3 Coordinate Matching
- [x] Implement coordinate matching — Source chunk X/Z = destination chunk X/Z
- [x] No Nether scaling — Literal coordinate equality (no 8:1 scaling)

> **Implementation Note (MC 1.21.10):** Block entity and entity serialization uses the new `TagValueInput`/`TagValueOutput` API instead of raw `CompoundTag`. Use `ProblemReporter.DISCARDING` for silent error handling during copy operations.

**Suggested commit message:** `feat: Phase 4 — biome-coherent source dimensions and chunk copying`

---

### Phase 5: Tier & Biome Pool System

**Status:** ✅ COMPLETED

#### 5.1 Tier Enum/Registry
- [x] Create `ChunkSpawnerTier` enum — COAL, IRON, GOLD, EMERALD, DIAMOND
- [x] Tier properties — Biome pool tag key, mob spawn rules (alwaysSpawnMobs)

#### 5.2 Biome Pool Management
- [x] Create `BiomePoolManager` — Loads and manages biome pools per tier
- [x] Biome tags integration — Support biome tags for pool membership via `getTagOrEmpty()`
- [x] Random biome selection — `selectRandomBiome()` from eligible pool

#### 5.3 Default Biome Pool Data
- [x] Coal tier biomes tag — Plains, Forest, Birch Forest, Taiga, Snowy Plains/Taiga, Savanna, Desert, Meadow, Beach, River variants
- [x] Iron tier biomes tag — Dark Forest, Jungle variants, Badlands, Swamp, Mountains, Caves (Lush/Dripstone/Deep Dark)
- [x] Gold tier biomes tag — All 5 vanilla Nether biomes
- [x] Diamond tier biomes tag — All 5 vanilla End biomes
- [x] Emerald tier biomes — Empty by default (for modpacks)

> **Implementation Note (MC 1.21.10):** Use `biomeRegistry.getTagOrEmpty(tagKey)` instead of `biomeRegistry.holders()` for biome tag iteration.

**Suggested commit message:** `feat: Phase 5 — tier enum, biome pool manager, and default biome tags`

---

### Phase 6A: Void World Type (Overworld)

**Status:** 🔄 IN PROGRESS (BLOCKED)

> **CRITICAL**: This phase must be completed before Phase 6. The mod's core mechanic requires the overworld to be a void world where chunks are only populated by the chunk spawning system. Without this, normal terrain generates and the chunk copy system cannot work correctly.

#### 6A.1 Void Chunk Generator
- [x] Create `VoidChunkGenerator` — Custom chunk generator that generates empty/void chunks
- [x] Codec registration — Register codec for serialization/deserialization
- [x] Empty terrain generation — Generate only void air (no blocks, no bedrock)
- [x] Biome handling — Use a placeholder biome (e.g., Plains) for void chunks until terrain is copied
- [x] Structure prevention — Prevent structure generation in void chunks (via empty `applyBiomeDecoration()`)

#### 6A.2 Custom World Preset
- [x] Create `BrightbronzeWorldPreset` — Custom world preset using VoidChunkGenerator for overworld
- [x] World preset registration — Register via `WorldPreset` registry (JSON at `data/brightbronze_horizons/worldgen/world_preset/brightbronze.json`)
- [x] Dimension settings — Configure overworld to use VoidChunkGenerator
- [x] Nether/End unchanged — Keep Nether and End using normal generators

#### 6A.3 World Creation Integration  
- [x] World creation screen — Preset appears in world type selection (via `minecraft:normal` tag)
- [ ] Default selection — Consider making Brightbronze preset the default when mod is installed
- [x] Server support — Ensure preset works for dedicated server world creation

#### 6A.4 Starting Area Visibility (BLOCKING ISSUE)
- [ ] **Client chunk sync** — Copied chunks are not visible to player despite server-side success

> **⚠️ CURRENT BLOCKING ISSUE (2026-02-01):**
> 
> The void world generates correctly (empty, no structures). The `StartingAreaManager` runs during `SERVER_STARTED` and logs report "9 of 9 chunks copied successfully." However, when the player spawns, they see only void — the copied 3×3 plains terrain is not visible.
> 
> **Root cause hypothesis:** The chunk copy runs server-side before the player joins. When the player connects and requests chunks, the client may receive stale/empty chunk data because:
> 1. Chunks are copied during `SERVER_STARTED` but the client hasn't connected yet
> 2. The `setBlock()` calls with `Block.UPDATE_ALL` may not trigger chunk resync to clients that load the chunks later
> 3. The chunks need explicit resync/refresh after copy completes
>
> **Potential solutions to investigate:**
> 1. Force chunk resend to clients after copy (using `ServerChunkCache` or chunk packet APIs)
> 2. Move chunk copy to happen during world creation/spawn chunk generation phase
> 3. Use `ChunkHolder.broadcastChanges()` or similar to notify clients of changes
> 4. Mark chunks as needing full resend rather than relying on block update flags
>
> **See Appendix B** for relevant Fabric client logs from testing session.

> **Implementation Note (Chunk By Chunk Reference):** See `tmp/chunkbychunk/Common/src/main/java/xyz/immortius/chunkbychunk/server/world/SkyChunkGenerator.java` for reference. Key points:
> - Extends or wraps a parent generator for biome information
> - Overrides `fillFromNoise()` to return empty chunks
> - Retains parent generator reference for when chunks need to be "revealed"
> - The approach: overworld is void, source dimensions generate real terrain, chunks are copied from source to overworld

> **Implementation Note (MC 1.21.10):** World presets are registered via `RegistryDataLoader` and require JSON files in `data/<namespace>/worldgen/world_preset/`. The `WorldPreset` codec defines dimension configurations. Platform-specific code may be needed to make the preset visible in the world creation UI.

**Suggested commit message:** `feat: Phase 6A — void chunk generator and custom world preset`

---

### Phase 6: World Initialization & Starting Area

**Status:** 🔄 NEEDS REVISION (depends on Phase 6A)

> **Prerequisites:** Phase 6A must be completed first. The starting area initialization assumes the overworld uses VoidChunkGenerator.

#### 6.1 Starting Area Initialization
- [ ] Revise `StartingAreaManager` — Copy 3×3 chunks from Plains source dimension into void overworld
- [ ] Spawn point placement — Set spawn inside the copied 3×3 area
- [ ] Chunk copy timing — Copy chunks BEFORE player spawns (during world creation, not SERVER_STARTED)
- [ ] Village requirement — Ensure source dimension chunk contains village structures

#### 6.2 Village Placement Strategy
- [ ] Village detection — Scan Plains source dimension for village locations
- [ ] Coordinate selection — Choose starting chunk coords where village exists in source dimension
- [ ] Fallback behavior — If no village found within search radius, use best available location

#### 6.3 Playable Area Tracking
- [x] Create `PlayableAreaData` — Server-level saved data tracking spawned chunks (using MC 1.21 Codec-based SavedData API)
- [x] Frontier detection — Track which chunks are at the edge for expansion
- [x] Adjacency validation — Ensure new chunks connect to existing area

#### 6.4 Chunk Copy Service Revision
- [ ] Remove `isReplaceableBlock` check — Always overwrite (void → terrain is always valid)
- [ ] Client sync — Ensure copied chunks are sent to clients correctly
- [ ] Lighting updates — Properly recalculate lighting after chunk copy

> **Implementation Note (MC 1.21.10):** `SavedData` uses `SavedDataType` with a `Codec` for serialization. The starting area must be initialized during world creation (before spawn chunk generation), not after server start.

**Suggested commit message:** `feat: Phase 6 — starting area initialization with village placement`

---

### Phase 7: Mob Spawning on Chunk Spawn

**Status:** Not Started

#### 7.1 Spawn Event System
- [ ] Create `ChunkSpawnMobEvent` — Triggered when chunk is spawned
- [ ] Time-of-day checks — Coal/Iron tiers spawn mobs only at night
- [ ] Always-spawn tiers — Gold/Diamond spawn regardless of time

#### 7.2 Spawn Configuration
- [ ] Create `MobSpawnRule` data class — Entity type, min/max count, conditions
- [ ] Spawn table loading — Load from data pack
- [ ] Difficulty/gamerule respect — Check `doMobSpawning`, difficulty level

#### 7.3 Spawn Execution
- [ ] Create `ChunkMobSpawner` — Executes one-time mob spawns
- [ ] Surface spawn positions — Find safe spawn locations on chunk surface
- [ ] Spawn caps — Prevent excessive spawns per chunk

**Suggested commit message:** `feat: Phase 7 — mob spawning system for newly spawned chunks`

---

### Phase 8: Configuration System

**Status:** Not Started

#### 8.1 Runtime Config (Server/World)
- [ ] Create `BrightbronzeConfig` — Main config class
- [ ] Starting area toggles — Enable/disable 3×3 start, village requirement
- [ ] Per-tier enable/disable — Toggle individual tiers
- [ ] Mob spawn toggle — Enable/disable chunk-spawn mob events
- [ ] Config sync — Sync config to clients on dedicated servers

#### 8.2 Data-Driven Rules Infrastructure
- [ ] Create `BiomeRule` record/class — Represents a biome rule entry
- [ ] Rule file loader — Load JSON files from `data/<namespace>/brightbronze_horizons/biome_rules/`
- [ ] Priority system — Numeric priority for rule ordering
- [ ] First-match resolution — Highest priority rule wins

#### 8.3 Biome Rule Schema
- [ ] Biome selector — Tag reference + optional allow/deny lists
- [ ] Tier assignment — Which tier this rule assigns
- [ ] Block filters — Exclude/include block tags/IDs
- [ ] Mob spawn table — Per-rule spawn configuration
- [ ] Weighting — Rarity weight for biome selection

#### 8.4 Default Rule Files
- [ ] Coal tier default rules — `data/brightbronze_horizons/biome_rules/coal_tier.json`
- [ ] Iron tier default rules — `data/brightbronze_horizons/biome_rules/iron_tier.json`
- [ ] Gold tier default rules — `data/brightbronze_horizons/biome_rules/gold_tier.json`
- [ ] Diamond tier default rules — `data/brightbronze_horizons/biome_rules/diamond_tier.json`

**Suggested commit message:** `feat: Phase 8 — configuration system and data-driven biome rules`

---

### Phase 9: Block Post-Processing

**Status:** Not Started

#### 9.1 Block Filter System
- [ ] Create `BlockFilter` — Defines exclude/include rules
- [ ] Tag-based filtering — Support block tags (e.g., `#minecraft:ores`)
- [ ] Block ID filtering — Support specific block IDs
- [ ] Replacement behavior — Replace excluded blocks with air

#### 9.2 Post-Processing Pipeline
- [ ] Create `ChunkPostProcessor` — Applies filters to spawned chunks
- [ ] Include override — Include rules override exclude rules
- [ ] Performance optimization — Efficient block iteration

**Suggested commit message:** `feat: Phase 9 — block post-processing filters for spawned chunks`

---

### Phase 10: Multiplayer & Server Support

**Status:** Not Started

#### 10.1 Permissions & Access
- [ ] No restrictions — Anyone can spawn chunks (per PRD)
- [ ] No cooldown — Only crafting cost gates usage

#### 10.2 Server Robustness
- [ ] Thread safety — Ensure chunk operations are thread-safe
- [ ] Chunk loading — Proper chunk loading/unloading during copy
- [ ] Concurrent players — Handle multiple players spawning chunks

#### 10.3 Persistence
- [ ] Deterministic RNG — Seeded RNG for reproducible biome selection
- [ ] State persistence — Save/load RNG state, spawned chunk tracking
- [ ] Restart consistency — Same seed + config = same results

**Suggested commit message:** `feat: Phase 10 — multiplayer support, thread safety, and persistence`

---

### Phase 11: Performance & Disk Management

**Status:** Not Started

#### 11.1 Performance Controls
- [ ] Async chunk generation — Avoid main-thread stalls
- [ ] Bounded operations — Limit work per tick
- [ ] Source dimension caps — Prevent runaway dimension creation

#### 11.2 Disk Management
- [ ] Pruning command — Admin command to prune unused source chunks
- [ ] Usage reporting — Command to report per-biome source dimension sizes

**Suggested commit message:** `feat: Phase 11 — performance controls and disk management tools`

---

### Phase 12: Polish & UX

**Status:** Not Started

#### 12.1 Player Feedback
- [ ] Chunk spawn particles — Visual effect when chunk spawns
- [ ] Chunk spawn sound — Audio feedback
- [ ] Failure messages — Clear chat/actionbar messages on failure
- [ ] Advancement/toast — Optional notification on first chunk spawn

#### 12.2 Debug & Admin Tools
- [ ] Debug command — `/brightbronze debug` for info
- [ ] Force spawn command — Admin command to spawn specific biome chunk
- [ ] Tier info command — List biomes in each tier

**Suggested commit message:** `feat: Phase 12 — polish, UX feedback, and admin commands`

---

### Phase 13: Testing & Validation

**Status:** Not Started

#### 13.1 Manual Testing
- [ ] Test new world creation with 3×3 start
- [ ] Test each tier's chunk spawner
- [ ] Test edge placement detection
- [ ] Test mob spawning rules (day/night)
- [ ] Test dedicated server operation
- [ ] Test config changes take effect

#### 13.2 Edge Cases
- [ ] Test Emerald tier with no biomes (should fail gracefully)
- [ ] Test expansion at world border
- [ ] Test rapid chunk spawning by multiple players
- [ ] Test world load/save with many source dimensions

**Suggested commit message:** `test: Phase 13 — manual testing and edge case validation`

---

### Completion Summary

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Core Infrastructure | ✅ Completed |
| 2 | Brightbronze Materials | ✅ Completed |
| 3 | Chunk Spawner System | ✅ Completed |
| 4 | Source Dimensions | ✅ Completed |
| 5 | Tier & Biome Pools | ✅ Completed |
| 6A | **Void World Type** | 🔄 In Progress |
| 6 | World Initialization | 🔄 Needs Revision |
| 7 | Mob Spawning | ⬜ Not Started |
| 8 | Configuration | ⬜ Not Started |
| 9 | Block Post-Processing | ⬜ Not Started |
| 10 | Multiplayer Support | ⬜ Not Started |
| 11 | Performance & Disk | ⬜ Not Started |
| 12 | Polish & UX | ⬜ Not Started |
| 13 | Testing | ⬜ Not Started |

> **Note:** Platform-specific code is integrated into each phase as needed, not deferred to a separate phase.
> 
> **⚠️ CRITICAL:** Phase 6A (Void World Type) is a prerequisite for the mod to function correctly. The overworld MUST use a void chunk generator; normal terrain generation breaks the chunk spawning mechanic.

---

## Appendix B: Fabric Client Logs (Phase 6A Debugging)

> **Session Date:** 2026-02-01
> 
> **Test Scenario:** New world created with "Brightbronze Horizons" world type. Player spawns in void world. Expected behavior: 3×3 plains terrain visible at spawn. Actual behavior: Pure void, no terrain visible.

[01:28:18] [Server thread/INFO] (brightbronze_horizons) Server started, checking starting area initialization...
[01:28:18] [Server thread/INFO] (brightbronze_horizons) Initializing starting area at chunk (0, 0) near spawn (8, 64, 8)
[01:28:18] [Server thread/INFO] (brightbronze_horizons) Using starting biome: minecraft:plains
[01:28:18] [Server thread/INFO] (brightbronze_horizons) Creating source dimension for biome: minecraft:plains
[01:28:18] [Server thread/INFO] (brightbronze_horizons) Created dynamic dimension: brightbronze_horizons:source/minecraft/plain 
[01:28:18] [Server thread/INFO] (brightbronze_horizons) Successfully created source dimension for biome: minecraft:plains
[01:28:18] [Server thread/INFO] (brightbronze_horizons) Starting area initialized: 9 of 9 chunks copied successfully
[01:28:18] [Server thread/INFO] (brightbronze_horizons) Set spawn point to (8, 64, 8)
[01:28:19] [Server thread/INFO] (Minecraft) Loading 0 chunks for player spawn...
[01:28:19] [Server thread/INFO] (Minecraft) Preparing spawn area: 16%
[01:28:19] [Server thread/INFO] (Minecraft) Time elapsed: 2180 ms
[01:28:19] [Server thread/INFO] (Minecraft) Player175[local:E:68d5ad3d] logged in with entity id 1 at (8.5, -63.0, 8.5)
[01:28:19] [Server thread/INFO] (Minecraft) Player175 joined the game
[01:28:19] [Server thread/INFO] (Minecraft) Changing view distance to 12, from 10
[01:28:19] [Server thread/INFO] (Minecraft) Changing simulation distance to 12, from 0
[01:28:19] [Render thread/INFO] (Minecraft) Resizing Dynamic Transforms UBO, capacity limit of 2 reached during a single frame. New capacity will be 4.
[01:28:19] [Render thread/INFO] (Minecraft) Resizing Dynamic Transforms UBO, capacity limit of 4 reached during a single frame. New capacity will be 8.
[01:28:19] [Render thread/INFO] (Minecraft) Loaded 2 advancements
[01:28:19] [Render thread/INFO] (Minecraft) Resizing Dynamic Transforms UBO, capacity limit of 8 reached during a single frame. New capacity will be 16.
[01:28:25] [Server thread/INFO] (Minecraft) Player175 fell out of the world
[01:28:25] [Render thread/INFO] (Minecraft) [System] [CHAT] Player175 fell out of the world
[01:28:32] [Server thread/INFO] (Minecraft) Player175 fell out of the world
[01:28:32] [Render thread/INFO] (Minecraft) [System] [CHAT] Player175 fell out of the world
[01:28:52] [Server thread/INFO] (Minecraft) Saving and pausing game...
[01:28:52] [Server thread/INFO] (Minecraft) Saving chunks for level 'ServerLevel[New World]'/minecraft:overworld
[01:28:52] [Server thread/INFO] (Minecraft) Saving chunks for level 'ServerLevel[New World]'/minecraft:the_nether
[01:28:52] [Server thread/INFO] (Minecraft) Saving chunks for level 'ServerLevel[New World]'/minecraft:the_end
[01:28:52] [Server thread/INFO] (Minecraft) Saving chunks for level 'ServerLevel[New World]'/brightbronze_horizons:source/minecraft/plains
[01:28:53] [Server thread/INFO] (Minecraft) Player175 lost connection: Disconnected
[01:28:53] [Server thread/INFO] (Minecraft) Player175 left the game