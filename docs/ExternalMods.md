# External Mods for Compatibility Testing

This document describes how to inject and remove external mods to test compatibility with Brightbronze Horizons.

## Overview

When developing a Minecraft mod, it's important to verify compatibility with popular community mods. This project uses Architectury with separate run directories for each loader (Fabric and NeoForge), each with their own `mods` folder for testing external mods.

### Mod Directories

| Loader   | Mods Directory                  |
|----------|--------------------------------|
| Fabric   | `fabric/run/mods/`             |
| NeoForge | `neoforge/run/mods/`           |

## Adding External Mods

1. Download the mod `.jar` file from a trusted source (CurseForge, Modrinth, or official GitHub releases)
2. Ensure you download the correct version for:
   - **Minecraft version**: `1.21.10`
   - **Mod loader**: Fabric OR NeoForge (they are NOT interchangeable)
3. Place the `.jar` file in the appropriate `run/mods/` directory
4. Run the client with `.\gradlew :fabric:runClient` or `.\gradlew :neoforge:runClient`

## Removing External Mods

1. Stop the running Minecraft client
2. Delete the `.jar` file from the `run/mods/` directory
3. Restart the client

## Tested External Mods

### JEI (Just Enough Items)

**Description**: JEI is a popular item and recipe viewing mod that provides a searchable item list and recipe lookup.

**Official Links**:
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/jei
- Modrinth: https://modrinth.com/mod/jei
- GitHub: https://github.com/mezz/JustEnoughItems

**Download URLs** (for Minecraft 1.21.10):

| Loader   | Download Source |
|----------|-----------------|
| Fabric   | [Modrinth - JEI Fabric](https://modrinth.com/mod/jei/versions?g=1.21.10&l=fabric) |
| NeoForge | [Modrinth - JEI NeoForge](https://modrinth.com/mod/jei/versions?g=1.21.10&l=neoforge) |

> **⚠️ Important**: JEI versions are specific to Minecraft versions. For Minecraft 1.21.10, you must use JEI version 26.x (not 19.x which is for 1.21.1). Using the wrong version will cause mixin injection failures.

**Installation**:

```powershell
# For Fabric (from repo root)
Invoke-WebRequest -Uri "https://cdn.modrinth.com/data/u6dRKJwZ/versions/Y0JKqg8L/jei-1.21.10-fabric-26.2.0.30.jar" -OutFile "fabric/run/mods/jei-1.21.10-fabric-26.2.0.30.jar"

# For NeoForge (from repo root)
Invoke-WebRequest -Uri "https://cdn.modrinth.com/data/u6dRKJwZ/versions/jIs3DXUN/jei-1.21.10-neoforge-26.2.0.30.jar" -OutFile "neoforge/run/mods/jei-1.21.10-neoforge-26.2.0.30.jar"
```

**Removal**:

```powershell
# Remove from Fabric
Remove-Item "fabric/run/mods/jei-*.jar"

# Remove from NeoForge
Remove-Item "neoforge/run/mods/jei-*.jar"
```

**Current Installed Version**: `26.2.0.30` (for Minecraft 1.21.10)

**Testing Checklist** (verified 2026-02-19):
- [x] Game loads without crashes
- [x] JEI item list appears on the right side of inventory
- [x] Brightbronze Horizons items appear in JEI
- [x] Recipe lookups work for mod items
- [x] No log errors related to mod interaction

---

### Terralith

> **✅ COMPATIBLE**: Terralith is fully compatible with Brightbronze Horizons.

**Description**: Terralith adds over 95 brand new biomes with realistic and fantasy themes, custom terrain (canyons, floating islands, deep ocean trenches), and immersive structures. Uses only vanilla blocks for maximum compatibility.

**Official Links**:
- Modrinth: https://modrinth.com/datapack/terralith
- GitHub: https://github.com/Stardust-Labs-MC/Terralith
- Wiki: https://stardustlabs.miraheze.org/wiki/Terralith

#### Compatibility Details

Terralith works by **overriding the vanilla `minecraft:overworld` dimension** via a datapack (at `data/minecraft/dimension/overworld.json`). This datapack includes a complete `multi_noise` biome source with all Terralith biomes and custom terrain generation.

**How we handle it**: Brightbronze Horizons uses a mixin (`VoidWorldEnforcerMixin`) that detects when a worldgen mod has overridden the chunk generator and replaces it back with our `VoidChunkGenerator` before levels are created. This ensures the void world works correctly while still allowing Terralith biomes to be accessed via the **Altered Horizon Anchor**.

**Download URLs** (for Minecraft 1.21.10):

| Loader   | Download Source |
|----------|-----------------|
| Fabric   | [Modrinth - Terralith Fabric](https://modrinth.com/datapack/terralith/versions?g=1.21.10&l=fabric) |
| NeoForge | [Modrinth - Terralith NeoForge](https://modrinth.com/datapack/terralith/versions?g=1.21.10&l=neoforge) |

**Installation**:

```powershell
# For Fabric (from repo root)
Invoke-WebRequest -Uri "https://cdn.modrinth.com/data/8oi3bsk5/versions/lO4Bdlgx/Terralith_1.21.x_v2.5.13.jar" -OutFile "fabric/run/mods/Terralith_1.21.x_v2.5.13.jar"

# For NeoForge (from repo root)
Invoke-WebRequest -Uri "https://cdn.modrinth.com/data/8oi3bsk5/versions/lO4Bdlgx/Terralith_1.21.x_v2.5.13.jar" -OutFile "neoforge/run/mods/Terralith_1.21.x_v2.5.13.jar"
```

**Removal**:

```powershell
# Remove from Fabric
Remove-Item "fabric/run/mods/Terralith*.jar"

# Remove from NeoForge
Remove-Item "neoforge/run/mods/Terralith*.jar"
```

**Current Installed Version**: `2.5.13` (for Minecraft 1.21.x)

**Testing Checklist** (verified 2026-02-19 on Fabric and NeoForge):
- [x] Game loads without crashes
- [x] Brightbronze Horizons void world works correctly
- [x] Village spawns in void world as expected
- [x] Altered Horizon Anchor detects Terralith biomes
- [x] Altered Horizon Anchor spawns Terralith terrain correctly
- [x] Coal spawner detects correct Terralith biome (not plains)
- [x] No log errors related to mod interaction

---

### Biomes O' Plenty

> **✅ COMPATIBLE**: Biomes O' Plenty is fully compatible with Brightbronze Horizons.

**Description**: Biomes O' Plenty adds 50+ unique biomes to the Overworld, Nether, and End, along with new plants, flowers, trees, and building blocks. A classic and popular biome mod with vanilla-esque aesthetic.

**Official Links**:
- Modrinth: https://modrinth.com/mod/biomes-o-plenty
- CurseForge: https://www.curseforge.com/minecraft/mc-mods/biomes-o-plenty
- GitHub: https://github.com/Glitchfiend/BiomesOPlenty

**Download URLs** (for Minecraft 1.21.10):

| Loader   | Download Source |
|----------|-----------------|
| Fabric   | [Modrinth - BOP Fabric](https://modrinth.com/mod/biomes-o-plenty/versions?g=1.21.10&l=fabric) |
| NeoForge | [Modrinth - BOP NeoForge](https://modrinth.com/mod/biomes-o-plenty/versions?g=1.21.10&l=neoforge) |

> **⚠️ Important**: Biomes O' Plenty requires two dependencies: **TerraBlender** and **GlitchCore**. All three mods must be installed together.

#### Compatibility Details

Biomes O' Plenty uses **TerraBlender** to inject its biomes into world generation at runtime. Our `VoidWorldEnforcerMixin` handles the generator override from TerraBlender the same way it handles Terralith, ensuring the void world works correctly while BOP biomes remain accessible via the **Altered Horizon Anchor**.

**Installation**:

```powershell
# For Fabric (from repo root) - Install all 3 required mods
Invoke-WebRequest -Uri "https://cdn.modrinth.com/data/kkmrDlKT/versions/kzbTmNaX/TerraBlender-fabric-1.21.10-21.10.0.0.jar" -OutFile "fabric/run/mods/TerraBlender-fabric-1.21.10-21.10.0.0.jar"
Invoke-WebRequest -Uri "https://cdn.modrinth.com/data/s3dmwKy5/versions/f5GdOk9W/GlitchCore-fabric-1.21.10-21.10.0.4.jar" -OutFile "fabric/run/mods/GlitchCore-fabric-1.21.10-21.10.0.4.jar"
Invoke-WebRequest -Uri "https://cdn.modrinth.com/data/HXF82T3G/versions/pzUNs4s7/BiomesOPlenty-fabric-1.21.10-21.10.0.4.jar" -OutFile "fabric/run/mods/BiomesOPlenty-fabric-1.21.10-21.10.0.4.jar"

# For NeoForge (from repo root) - Install all 3 required mods
Invoke-WebRequest -Uri "https://cdn.modrinth.com/data/kkmrDlKT/versions/OXAwdlqt/TerraBlender-neoforge-1.21.10-21.10.0.0.jar" -OutFile "neoforge/run/mods/TerraBlender-neoforge-1.21.10-21.10.0.0.jar"
Invoke-WebRequest -Uri "https://cdn.modrinth.com/data/s3dmwKy5/versions/PuK3AwMh/GlitchCore-neoforge-1.21.10-21.10.0.4.jar" -OutFile "neoforge/run/mods/GlitchCore-neoforge-1.21.10-21.10.0.4.jar"
Invoke-WebRequest -Uri "https://cdn.modrinth.com/data/HXF82T3G/versions/GA1U3T4h/BiomesOPlenty-neoforge-1.21.10-21.10.0.4.jar" -OutFile "neoforge/run/mods/BiomesOPlenty-neoforge-1.21.10-21.10.0.4.jar"
```

**Removal**:

```powershell
# Remove from Fabric (removes BOP and its dependencies)
Remove-Item "fabric/run/mods/TerraBlender*.jar", "fabric/run/mods/GlitchCore*.jar", "fabric/run/mods/BiomesOPlenty*.jar"

# Remove from NeoForge (removes BOP and its dependencies)
Remove-Item "neoforge/run/mods/TerraBlender*.jar", "neoforge/run/mods/GlitchCore*.jar", "neoforge/run/mods/BiomesOPlenty*.jar"
```

**Current Installed Version**: `21.10.0.4` (for Minecraft 1.21.10)
- TerraBlender: `21.10.0.0`
- GlitchCore: `21.10.0.4`

**Testing Checklist** (verified 2026-02-19 on NeoForge):
- [x] Game loads without crashes
- [x] Brightbronze Horizons void world works correctly
- [x] BOP blocks and items appear in JEI
- [x] Altered Horizon Anchor detects BOP biomes (e.g., `biomesoplenty:wasteland`)
- [x] Altered Horizon Anchor spawns BOP terrain correctly
- [x] No log errors related to mod interaction

---

## Adding New Mods to This Document

When testing a new external mod, add an entry with:

1. **Mod Name** and brief description
2. **Official Links** (CurseForge/Modrinth/GitHub)
3. **Download URLs** for each supported loader
4. **Installation notes** if any special steps are required
5. **Testing Checklist** for compatibility verification

## Troubleshooting

### Mod fails to load
- Verify the mod version matches Minecraft `1.21.10`
- Verify the mod is for the correct loader (Fabric vs NeoForge)
- Check `run/logs/latest.log` for error messages

### Crash on startup
- Check crash reports in `run/crash-reports/`
- Try removing the external mod to confirm it's the cause
- Check if the mod requires additional dependencies

### Mods not appearing in-game
- Ensure the `.jar` file is directly in `run/mods/` (not in a subfolder)
- Restart the client completely (don't just reload)

## Gitignore Note

The `run/mods/` directories are typically gitignored to avoid committing large binary files. Each developer should download external mods locally as needed.
