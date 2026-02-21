# Changelog

All notable changes to Brightbronze Horizons will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-02-21

### Added

#### Core Gameplay
- **Void World Preset**: New worlds start as void with only a plains village
- **Brightbronze Horizons** is automatically pre-selected as the default world type

#### Brightbronze Material System
- **Brightbronze Amalgam**: Crafted from Copper Ingot + Iron Nuggets + Gold Nugget
- **Brightbronze Ingot**: Smelted from Brightbronze Amalgam
- **Brightbronze Nugget**: Crafted from Brightbronze Ingot (9 nuggets per ingot)
- **Block of Brightbronze**: Storage block for Brightbronze Ingots

#### Brightbronze Tools & Armor
- Full tool set: Sword, Pickaxe, Axe, Shovel, Hoe
- Full armor set: Helmet, Chestplate, Leggings, Boots
- Tool/armor stats between Iron and Diamond tier

#### Horizon Anchors (Chunk Spawners)
- **Local Horizon Anchor** (Coal): Spawns chunks matching the current biome
- **Overworld Horizon Anchor** (Copper): Spawns common/dry Overworld biomes (plains, forests, deserts)
- **Tidal Horizon Anchor** (Iron): Spawns rare/wet Overworld biomes (jungle, swamp, ocean)
- **Infernal Horizon Anchor** (Gold): Spawns Nether biomes
- **Astral Horizon Anchor** (Diamond): Spawns End biomes
- **Altered Horizon Anchor** (Emerald): Spawns modded biomes from worldgen mods

#### Chunk Spawning Mechanics
- Anchors must be placed at the edge of generated terrain (adjacent to void)
- Successful activation spawns a new chunk and consumes the anchor
- Visual and audio feedback for activation success/failure
- Graceful handling when no eligible biomes are available

#### Worldgen Mod Compatibility
- Automatic detection of modded biomes at runtime
- Compatible with **Terralith** (biome overhaul)
- Compatible with **Biomes O' Plenty** (new biomes)
- Compatible with **TerraBlender** (biome injection library)
- Compatible with **JEI** (recipe viewing)
- VoidWorldEnforcer mixin ensures void world works alongside worldgen mods

#### Data-Driven Configuration
- JSON-based biome rules for anchor tier assignments
- JSON-based mob spawn tables for biome-specific spawning
- Extensible by modpacks via datapacks

### Platform Support
- **Fabric** for Minecraft 1.21.1 and 1.21.10
- **NeoForge** for Minecraft 1.21.1 and 1.21.10

---

*Stabilize the edge of reality. Stitch a world into the void.*
