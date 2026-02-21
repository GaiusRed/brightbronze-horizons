# Brightbronze Horizons Local Development

Brightbronze Horizons is a Minecraft mod (multi-loader: Fabric + NeoForge) using Architectury.

## Supported Versions

| Minecraft | Fabric | NeoForge |
|-----------|--------|----------|
| 1.21.10   | ✅     | ✅       |
| 1.21.1    | ✅     | ✅       |

## Prereqs

- Windows + PowerShell
- Java 21 (required: the build is configured for `--release 21`)
  - Verify: `java -version`
  - If you use Scoop: `scoop install openjdk21`

## Run Commands

From the repo root (`c:\MyProjects\brightbronze-horizons`):

### Fabric

```powershell
# Minecraft 1.21.10 (latest)
.\gradlew :fabric-1.21.10:runClient

# Minecraft 1.21.1
.\gradlew :fabric-1.21.1:runClient
```

### NeoForge

```powershell
# Minecraft 1.21.10 (latest)
.\gradlew :neoforge-1.21.10:runClient

# Minecraft 1.21.1
.\gradlew :neoforge-1.21.1:runClient
```

## Build Commands

```powershell
# Build all 4 artifacts (recommended for releases)
.\gradlew buildAll

# Build individual artifacts
.\gradlew :fabric-1.21.10:build
.\gradlew :fabric-1.21.1:build
.\gradlew :neoforge-1.21.10:build
.\gradlew :neoforge-1.21.1:build
```

Release JARs are output to `{subproject}/build/libs/`:
- `brightbronze_horizons-fabric-1.21.10-{version}.jar`
- `brightbronze_horizons-fabric-1.21.1-{version}.jar`
- `brightbronze_horizons-neoforge-1.21.10-{version}.jar`
- `brightbronze_horizons-neoforge-1.21.1-{version}.jar`

## Notes / Troubleshooting

- First run is slow: Gradle/Loom will download Minecraft, mappings, and assets.
- If Gradle hangs on a Loom cache lock like:
  - `Lock for cache='...\.gradle\caches\fabric-loom' is currently held by pid ...`
  - Close other Gradle/IDE tasks that might be running Loom, then run:

```powershell
.\gradlew --stop
```

- Crash reports:
  - `fabric-1.21.10/run/crash-reports/`
  - `fabric-1.21.1/run/crash-reports/`
  - `neoforge-1.21.10/run/crash-reports/`
  - `neoforge-1.21.1/run/crash-reports/`

- Build warnings like `Cannot remap openFresh` are harmless (see `docs/MultiVersionSupport.md` section 8.1)
