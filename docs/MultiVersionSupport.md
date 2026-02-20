# Multi-Version Support — Implementation Plan

**Last updated:** 2026-02-21  
**Status:** ✅ Phase 2 Complete — Phase 3 Pending  

This document defines the phased approach for supporting multiple Minecraft versions (1.21.1 and 1.21.10) using a version-specific subproject structure. The goal is to maintain a single codebase with clean separation of version-dependent code.

---

## 1. Problem Statement

### 1.1 Current State

Brightbronze Horizons currently targets **Minecraft 1.21.10** exclusively. The mod uses APIs introduced in 1.21.10 that do not exist in 1.21.1:

| API / Pattern | 1.21.10 | 1.21.1 |
|---------------|---------|--------|
| `Item.Properties.setId()` | Required | Does not exist |
| `ResourceLocation.fromNamespaceAndPath()` | Standard | Use `new ResourceLocation()` |
| `ToolMaterial` record | Record class | `Tier` interface |
| `Item.Properties.sword()`, `.pickaxe()`, etc. | Method-based tools | `SwordItem`, `PickaxeItem` classes |
| `ArmorMaterial` record | Record class | Interface + registry |
| `Item.Properties.humanoidArmor()` | Method-based armor | `ArmorItem` class |
| `EquipmentAsset` / `EquipmentAssets` | Exists | Does not exist |
| `PlayerChunkSender.markChunkPendingToSend()` | New chunk sending API | Use `ChunkMap.updateChunkTracking()` |
| `Registry.get(ResourceKey)` | Returns `Optional<Holder.Reference<T>>` | Returns `T` directly (use `getHolder()`) |
| `Registry.get(ResourceLocation)` | Returns `Optional<Holder.Reference<T>>` | Use `getOptional()` or `getHolder()` |
| `CreateWorldScreen.openFresh()` | `(Minecraft, Runnable, CreateWorldCallback)` | `(Minecraft, Screen)` |
| `Level.getChunk()` | Returns `ChunkAccess` | Returns `LevelChunk` |
| `SoundType` fields | Direct field access | Use getter methods |
| Armor texture paths | `textures/entity/equipment/humanoid/` | `textures/models/armor/` |

### 1.2 Motivation

Supporting 1.21.1 expands compatibility with:

- More modpacks (many still target 1.21.1)
- Players who haven't updated
- Server communities on stable releases

### 1.3 Approach: Version Subprojects

We will use **version-specific subprojects** rather than comment-based preprocessors (Stonecutter). This approach:

- Keeps version-specific code cleanly separated
- Avoids comment clutter in shared code
- Leverages our existing `@ExpectPlatform` pattern for abstraction
- Provides clear build targets per version

---

## 2. Target Architecture

### 2.1 Project Structure (Post-Migration)

```
brightbronze-horizons/
├── build.gradle                    # Root build configuration
├── settings.gradle                 # Subproject definitions
├── gradle.properties               # Shared properties
│
├── common/                         # Version-agnostic shared code
│   ├── build.gradle
│   └── src/main/java/
│       └── red/gaius/brightbronze/
│           ├── BrightbronzeHorizons.java
│           ├── config/
│           ├── world/
│           └── ...                 # Core logic, no MC version-specific APIs
│
├── common-1.21.1/                  # 1.21.1 version implementations
│   ├── build.gradle
│   └── src/main/java/
│       └── red/gaius/brightbronze/
│           └── versioned/
│               └── mc1211/
│                   ├── ItemFactoryImpl.java
│                   ├── ArmorFactoryImpl.java
│                   ├── ToolFactoryImpl.java
│                   └── ChunkSyncImpl.java
│
├── common-1.21.10/                 # 1.21.10 version implementations (current)
│   ├── build.gradle
│   └── src/main/java/
│       └── red/gaius/brightbronze/
│           └── versioned/
│               └── mc12110/
│                   ├── ItemFactoryImpl.java
│                   ├── ArmorFactoryImpl.java
│                   ├── ToolFactoryImpl.java
│                   └── ChunkSyncImpl.java
│
├── fabric-1.21.1/                  # Fabric loader for 1.21.1
│   ├── build.gradle
│   └── src/main/
│       ├── java/
│       └── resources/
│
├── fabric-1.21.10/                 # Fabric loader for 1.21.10
│   ├── build.gradle
│   └── src/main/
│       ├── java/
│       └── resources/
│
├── neoforge-1.21.1/                # NeoForge loader for 1.21.1
│   ├── build.gradle
│   └── src/main/
│       ├── java/
│       └── resources/
│
└── neoforge-1.21.10/               # NeoForge loader for 1.21.10
    ├── build.gradle
    └── src/main/
        ├── java/
        └── resources/
```

### 2.2 Dependency Graph

```
                    ┌─────────────────┐
                    │     common      │
                    │ (version-free)  │
                    └────────┬────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
            ▼                ▼                ▼
   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
   │common-1.21.1│  │common-1.21.10│ │   (other)   │
   │ versioned/  │  │  versioned/  │ │  versions   │
   └──────┬──────┘  └──────┬───────┘ └─────────────┘
          │                │
     ┌────┴────┐      ┌────┴────┐
     ▼         ▼      ▼         ▼
┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
│fabric  │ │neoforge│ │fabric  │ │neoforge│
│1.21.1  │ │1.21.1  │ │1.21.10 │ │1.21.10 │
└────────┘ └────────┘ └────────┘ └────────┘
```

### 2.3 Version Abstraction Pattern

Version-specific code will use factory interfaces in `common/` with implementations in `common-{version}/`:

```java
// common/src/.../versioned/ItemFactory.java
public interface ItemFactory {
    Item.Properties createProperties();
    Item.Properties withCreativeTab(Item.Properties props, ResourceKey<CreativeModeTab> tab);
    // ... other version-divergent operations
}

// common/src/.../versioned/VersionedFactories.java
public class VersionedFactories {
    private static ItemFactory itemFactory;
    
    public static void init(ItemFactory items) {
        itemFactory = items;
    }
    
    public static ItemFactory items() {
        return itemFactory;
    }
}
```

Platform entrypoints initialize the correct factory:

```java
// fabric-1.21.10/.../BrightbronzeHorizonsFabric.java
public void onInitialize() {
    VersionedFactories.init(new ItemFactoryImpl());  // From common-1.21.10
    BrightbronzeHorizons.init();
}
```

---

## 3. Implementation Phases

### Phase 1: Restructure for 1.21.10 ✅ COMPLETE

**Goal:** Refactor current code into the new subproject structure without changing behavior. Verify 1.21.10 still works exactly as before.

**Status:** ✅ Complete - Fabric 1.21.10 builds and runs successfully.

#### 1.1 Create Version Abstraction Layer ✅

- [x] Created `common/src/.../versioned/` package with factory interfaces
- [x] Defined `McVersion` interface aggregating all version helpers
- [x] Defined `ItemRegistry` interface for item/block property creation
- [x] Defined `BlockRegistry` interface for block property creation
- [x] Defined `ToolFactory` interface for tool item creation
- [x] Defined `ArmorFactory` interface for armor item creation
- [x] Defined `ChunkSyncHelper` interface for chunk resync operations
- [x] Created `Versioned` static accessor class

#### 1.2 Extract 1.21.10 Implementations ✅

- [x] Created `common-1.21.10/` subproject
- [x] Implemented `McVersion12110` aggregating all implementations
- [x] Implemented `ItemRegistryImpl` using `.setId()` pattern
- [x] Implemented `BlockRegistryImpl` using `.setId()` pattern
- [x] Implemented `ToolFactoryImpl` using `Item.Properties.sword()` etc.
- [x] Implemented `ArmorFactoryImpl` using `humanoidArmor()` and `EquipmentAsset`
- [x] Implemented `ChunkSyncHelperImpl` using `PlayerChunkSender`

#### 1.3 Refactor Registries to Use Factories ✅

- [x] Updated `ModItems.java` to use `Versioned.items()` and `Versioned.tools()`
- [x] Updated `ModBlocks.java` to use `Versioned.blocks()`
- [x] Updated `ModArmorMaterials.java` to use `Versioned.armor()`
- [x] Updated `ChunkMapMixin.java` to use `Versioned.chunkSync()`

#### 1.4 Update Build Configuration ✅

- [x] Modified `settings.gradle` to include `common-1.21.10`
- [x] Created `common-1.21.10/build.gradle`
- [x] Updated `fabric/build.gradle` to depend on `common-1.21.10`
- [x] Updated `neoforge/build.gradle` to depend on `common-1.21.10`
- [x] Added `Versioned.init(new McVersion12110())` to entrypoints

#### 1.5 Verification Checklist

- [x] `:fabric:build` succeeds
- [x] `:fabric:runClient` — game launches, mod initializes
- [ ] NeoForge build has transformer issue (to investigate)
- [x] Manual test: chunk spawners work as expected
- [x] Manual test: all items/blocks/tools/armor present and functional
- [x] Manual test: worldgen mod compatibility (Terralith) still works

---

### Phase 2: Add 1.21.1 Support ✅ COMPLETE

**Goal:** Implement 1.21.1 versions of all abstracted components. Produce working Fabric and NeoForge builds for 1.21.1.

**Status:** ✅ Both Fabric 1.21.1 and NeoForge 1.21.1 build and run successfully.

#### 2.1 Version Properties ✅

- [x] Create `gradle-1.21.1.properties` with 1.21.1 dependency versions:
  - `minecraft_version = 1.21.1`
  - `fabric_api_version = 0.116.8+1.21.1`
  - `architectury_api_version = 13.0.8`

#### 2.2 Create 1.21.1 Common Implementations ✅

- [x] Create `common-1.21.1/` subproject
- [x] Implement `McVersion1211` aggregating all implementations
- [x] Implement `ItemRegistryImpl` (no `.setId()`)
- [x] Implement `BlockRegistryImpl` (no `.setId()`)
- [x] Implement `ToolFactoryImpl` using `SwordItem`, `PickaxeItem`, etc.
- [x] Implement `ArmorFactoryImpl` using `ArmorItem` class and legacy texture paths
- [x] Implement `ChunkSyncHelperImpl` using `ChunkMap.updateChunkTracking()`
- [x] Implement `RegistryHelperImpl` using `getHolder()` instead of `get()`
- [x] Implement `LevelHelperImpl` for `getChunk()` return type differences
- [x] Implement `SoundHelperImpl` for `SoundType` field access differences

#### 2.3 Mixin Adjustments ✅

- [x] Review `ChunkMapMixin.java` for 1.21.1 compatibility — created version-specific mixin
- [x] Review `VoidWorldEnforcerMixin.java` for 1.21.1 compatibility — fixed `Registry.get()` → `Versioned.registry().getHolderReference()`
- [x] Review `CreateWorldScreenMixin.java` for 1.21.1 compatibility — created version-specific mixin
- [x] Create version-specific mixins where method signatures differ:
  - `common-1.21.1/.../mixin/mc1211/ChunkMapMixin.java` — different field/method names
  - `common-1.21.1/.../mixin/mc1211/CreateWorldScreenMixin.java` — different `openFresh()` signature

#### 2.4 Create Platform Subprojects ✅

- [x] Create `fabric-1.21.1/` from `fabric/` template
- [x] Create `neoforge-1.21.1/` from `neoforge/` template
- [x] Update `fabric.mod.json` version constraints for 1.21.1
- [x] Update `neoforge.mods.toml` version constraints for 1.21.1
- [x] Wire up correct `common-1.21.1/` dependency
- [x] Create version-specific reload listeners for NeoForge 1.21.1 (`PreparableReloadListener` signature differs)

#### 2.5 Resource Compatibility ✅

- [x] Copy armor textures to legacy 1.21.1 paths (`textures/models/armor/`)
- [x] Create item model JSON files for chunk spawners (different model format)
- [x] Verify block textures work correctly

#### 2.6 Verification Checklist ✅

- [x] `.\gradlew :fabric-1.21.1:build` succeeds
- [x] `.\gradlew :neoforge-1.21.1:build` succeeds
- [x] `.\gradlew :fabric-1.21.1:runClient` — game launches, create world works
- [x] `.\gradlew :neoforge-1.21.1:runClient` — game launches, create world works
- [x] Manual test: chunk spawners work as expected
- [x] Manual test: all items/blocks/tools/armor present and functional
- [x] Manual test: worldgen mod compatibility (JEI, TerraBlender, Biomes O' Plenty, Terralith)
- [x] Manual test: Brightbronze Horizons pre-selected as default generator
- [x] Manual test: Altered Horizon Anchor (emerald tier) detects modded biomes

---

### Phase 3: Build & Release Infrastructure

**Goal:** Enable building all versions in a single CI pass and produce properly-named artifacts.

#### 3.1 Unified Build

- [ ] Add Gradle task `buildAll` that builds all 4 artifacts
- [ ] Configure artifact naming: `brightbronze_horizons-{loader}-{mcversion}-{modversion}.jar`
- [ ] Ensure `runClient` tasks are clearly named per version

#### 3.2 CI/CD Updates

- [ ] Update GitHub Actions workflow to build all versions
- [ ] Matrix build: `[fabric-1.21.1, fabric-1.21.10, neoforge-1.21.1, neoforge-1.21.10]`
- [ ] Artifact upload for all successful builds

#### 3.3 Documentation

- [ ] Update `LocalDev.md` with new run commands
- [ ] Update `README.md` with supported versions
- [ ] Add version compatibility notes

---

## 4. API Mapping Reference

Detailed mapping of APIs between versions for implementers.

### 4.1 ResourceLocation

| Operation | 1.21.1 | 1.21.10 |
|-----------|--------|---------|
| From namespace + path | `new ResourceLocation(ns, path)` | `ResourceLocation.fromNamespaceAndPath(ns, path)` |
| Parse string | `new ResourceLocation(string)` | `ResourceLocation.parse(string)` |

### 4.2 Item Properties

| Operation | 1.21.1 | 1.21.10 |
|-----------|--------|---------|
| Create properties | `new Item.Properties()` | `new Item.Properties().setId(key)` |
| Set durability | `.durability(n)` | `.durability(n)` |
| Set max stack | `.stacksTo(n)` | `.stacksTo(n)` |

### 4.3 Tools

| Tool Type | 1.21.1 | 1.21.10 |
|-----------|--------|---------|
| Sword | `new SwordItem(tier, attackDmg, attackSpd, props)` | `new Item(props.sword(material, dmg, spd))` |
| Pickaxe | `new PickaxeItem(tier, attackDmg, attackSpd, props)` | `new Item(props.pickaxe(material, dmg, spd))` |
| Axe | `new AxeItem(tier, attackDmg, attackSpd, props)` | `new Item(props.axe(material, dmg, spd))` |
| Shovel | `new ShovelItem(tier, attackDmg, attackSpd, props)` | `new Item(props.shovel(material, dmg, spd))` |
| Hoe | `new HoeItem(tier, attackDmg, attackSpd, props)` | `new Item(props.hoe(material, dmg, spd))` |

### 4.4 Tool Material

| Aspect | 1.21.1 | 1.21.10 |
|--------|--------|---------|
| Definition | Implement `Tier` interface | Instantiate `ToolMaterial` record |
| Registration | None needed | None needed |

### 4.5 Armor

| Aspect | 1.21.1 | 1.21.10 |
|--------|--------|---------|
| Material definition | Implement `ArmorMaterial` interface, register | Instantiate `ArmorMaterial` record |
| Item creation | `new ArmorItem(material, slot, props)` | `new Item(props.humanoidArmor(material, type))` |
| Equipment asset | Not applicable | `EquipmentAsset` / `EquipmentAssets` |

### 4.6 Chunk Synchronization

| Operation | 1.21.1 | 1.21.10 |
|-----------|--------|---------|
| Force resync to player | `ChunkMap.updateChunkTracking()` or packet-based | `player.connection.chunkSender.markChunkPendingToSend(chunk)` |

### 4.7 Registry Lookups (NEW)

| Operation | 1.21.1 | 1.21.10 |
|-----------|--------|---------|
| Get holder by key | `registry.getHolder(key)` returns `Optional<Holder.Reference<T>>` | `registry.get(key)` returns `Optional<Holder.Reference<T>>` |
| Get holder by location | `registry.getHolder(id)` returns `Optional<Holder.Reference<T>>` | `registry.get(id)` returns `Optional<Holder.Reference<T>>` |
| Get value directly | `registry.get(key)` returns `T` | Not available (use `.get(key).map(Holder::value)`) |
| Get optional value | `registry.getOptional(id)` returns `Optional<T>` | Not available |

### 4.8 Level/Chunk Access (NEW)

| Operation | 1.21.1 | 1.21.10 |
|-----------|--------|---------|
| Get chunk from level | `level.getChunk(x, z)` returns `LevelChunk` | `level.getChunk(x, z)` returns `ChunkAccess` |
| Cast to LevelChunk | Direct return | Cast required: `(LevelChunk) level.getChunk(x, z)` |

### 4.9 SoundType Access (NEW)

| Operation | 1.21.1 | 1.21.10 |
|-----------|--------|---------|
| Get break sound | `soundType.getBreakSound()` | `soundType.breakSound()` |
| Get place sound | `soundType.getPlaceSound()` | `soundType.placeSound()` |
| Get step sound | `soundType.getStepSound()` | `soundType.stepSound()` |

### 4.10 CreateWorldScreen (NEW)

| Operation | 1.21.1 | 1.21.10 |
|-----------|--------|---------|
| Open fresh world screen | `CreateWorldScreen.openFresh(Minecraft, Screen)` | `CreateWorldScreen.openFresh(Minecraft, Runnable, CreateWorldCallback)` |
| Default preset injection | Intercept `Optional.of(WorldPresets.NORMAL)` in constructor call | Intercept `openCreateWorldScreen()` call |

### 4.11 Armor Textures (NEW)

| Aspect | 1.21.1 | 1.21.10 |
|--------|--------|---------|
| Texture path | `textures/models/armor/{material}_layer_1.png` | `textures/entity/equipment/humanoid/{material}.png` |
| Layer 2 path | `textures/models/armor/{material}_layer_2.png` | `textures/entity/equipment/humanoid_leggings/{material}.png` |
| Registration | Automatic by naming convention | Via `EquipmentAsset` / `EquipmentAssets` |

---

## 5. Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Mixin targets differ significantly | Medium | High | Test each mixin against both versions early; version-specific mixin classes if needed |
| Architectury API version incompatibility | Low | Medium | Pin Architectury versions; test registry abstractions |
| Hidden API differences in world gen | Medium | Medium | Thorough testing of chunk copy, structure detection |
| Build complexity slows development | Medium | Low | Clear task naming; IDE run configurations |

---

## 6. Success Criteria

Phase 1 is complete when:
- [x] Project builds without warnings for 1.21.10
- [x] All existing functionality works identically to pre-refactor
- [x] No version-specific code remains in `common/`

Phase 2 is complete when:
- [x] Project builds for both 1.21.1 and 1.21.10 (Fabric)
- [x] Project builds for both 1.21.1 and 1.21.10 (NeoForge)
- [x] Core gameplay loop works on both versions
- [x] All items, blocks, and mechanics function correctly
- [x] Worldgen mod compatibility verified (JEI, TerraBlender, BOP, Terralith)

Phase 3 is complete when:
- [ ] Single command builds all artifacts
- [ ] CI produces all 4 JARs per commit
- [ ] Documentation covers all supported versions

---

## 7. Version Helper Interfaces

The `common/src/.../versioned/` package contains these abstraction interfaces:

| Interface | Purpose | Key Methods |
|-----------|---------|-------------|
| `McVersion` | Aggregates all version helpers | `createResourceLocation()`, `parseResourceLocation()` |
| `ItemRegistry` | Item property creation | `createItemProperties()`, `createBlockItemProperties()` |
| `BlockRegistry` | Block property creation | `createBlockProperties()` |
| `ToolFactory` | Tool item creation | `createSword()`, `createPickaxe()`, etc. |
| `ArmorFactory` | Armor item creation | `createHelmet()`, `createChestplate()`, etc. |
| `ChunkSyncHelper` | Chunk resync operations | `resyncChunkToPlayer()` |
| `RegistryHelper` | Registry lookups | `getHolder()`, `getHolderReference()`, `lookupRegistry()` |
| `LevelHelper` | Level/chunk access | `getLevelChunk()` |
| `SoundHelper` | SoundType field access | `getBreakSound()`, `getPlaceSound()` |

Access via: `Versioned.mc()`, `Versioned.items()`, `Versioned.blocks()`, `Versioned.tools()`, `Versioned.armor()`, `Versioned.chunkSync()`, `Versioned.registry()`, `Versioned.level()`, `Versioned.sound()`

---

## 8. Known Issues & Workarounds

### 8.1 Architectury Transformer Warnings

During build, warnings like "Cannot remap openFresh" appear. These are harmless — they indicate methods that exist in only one MC version. The version-specific implementations handle these correctly.

### 8.2 Holder.Reference vs Holder

In 1.21.1, `Registry.getHolder()` returns `Optional<Holder.Reference<T>>`, but many APIs expect just `Holder<T>`. Solution: Use `Versioned.registry().getHolder()` which returns `Optional<Holder<T>>` for general use, or `getHolderReference()` when reflection-based registry modification is needed.

### 8.3 NeoForge 1.21.1 Differences

NeoForge 1.21.1 has several API differences from 1.21.10:

| Aspect | 1.21.1 | 1.21.10 |
|--------|--------|---------|
| Reload event | `AddReloadListenerEvent` | `AddServerReloadListenersEvent` |
| PreparableReloadListener | `reload(PreparationBarrier, ResourceManager, ProfilerFiller, ProfilerFiller, Executor, Executor)` | `reload(SharedState, Executor, PreparationBarrier, Executor)` |
| Registry access | `registryOrThrow()` + `.getHolder()` | `lookupOrThrow()` + `.get()` |

**Split package workaround:** NeoForge's dev environment uses Java modules which don't allow split packages. Version-specific implementations must use unique packages:
- `common/...world/mob/MobSpawnTableReloadListener` — 1.21.10 (uses `SharedState`)
- `common-1.21.1/...world/mob1211/MobSpawnTableReloadListener` — 1.21.1 (uses old signature)

### 8.4 Package Structure for NeoForge Compatibility

Due to Java module restrictions in NeoForge dev environment, version-specific classes that would normally override `common` classes must be placed in separate packages:

```
common/
└── red/gaius/brightbronze/
    └── versioned/        # Interfaces ONLY (McVersion, ChunkHelper, etc.)
    └── world/mob/        # 1.21.10 MobSpawnTableReloadListener

common-1.21.1/
└── red/gaius/brightbronze/
    └── versioned/
        └── mc1211/       # All 1.21.1 implementations (*Impl classes)
    └── world/
        └── mob1211/      # 1.21.1 MobSpawnTableReloadListener
        └── rules1211/    # 1.21.1 BiomeRuleReloadListener
```

This ensures no package overlap between `common` and `common-1.21.1` modules.
