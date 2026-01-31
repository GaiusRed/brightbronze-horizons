# Brightbronze Horizons

Minecraft mod (multi-loader: Fabric + NeoForge) using Architectury.

## Prereqs

- Windows + PowerShell
- Java 21 (required: the build is configured for `--release 21`)
  - Verify: `java -version`
  - If you use Scoop: `scoop install openjdk21`

## Quickstart (NeoForge)

From the repo root:

```powershell
cd c:\MyProjects\brightbronze-horizons
.\gradlew :neoforge:runClient
```

## Quickstart (Fabric)

From the repo root:

```powershell
cd c:\MyProjects\brightbronze-horizons
.\gradlew :fabric:runClient
```

## Notes / troubleshooting

- First run is slow: Gradle/Loom will download Minecraft, mappings, and assets.
- If Gradle hangs on a Loom cache lock like:
  - `Lock for cache='...\.gradle\caches\fabric-loom' is currently held by pid ...`
  - Close other Gradle/IDE tasks that might be running Loom, then run:

```powershell
.\gradlew --stop
```

- Crash reports:
  - `fabric\run\crash-reports\`
  - `neoforge\run\crash-reports\`
