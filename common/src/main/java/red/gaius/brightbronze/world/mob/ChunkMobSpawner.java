package red.gaius.brightbronze.world.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.world.ChunkSpawnerTier;
import red.gaius.brightbronze.world.BiomePoolManager;
import red.gaius.brightbronze.world.rules.BiomeRuleManager;

import java.util.List;

/**
 * Executes one-time scripted mob spawns when a new chunk is spawned/revealed.
 *
 * <p>This respects gamerules and difficulty, and is intentionally bounded.
 */
public final class ChunkMobSpawner {

    private static final int MAX_TOTAL_MOBS_PER_CHUNK = 8;
    private static final int MAX_POSITION_TRIES_PER_MOB = 12;

    private ChunkMobSpawner() {
    }

    public static void onChunkSpawned(ServerLevel level, ChunkPos chunkPos, ChunkSpawnerTier tier) {
        if (!shouldSpawnMobs(level, tier)) {
            return;
        }

        // Phase 8: allow per-biome scripted mob spawns via biome rules.
        var biomeId = BiomePoolManager.getBiomeId(level.getBiome(chunkPos.getMiddleBlockPosition(64)));
        List<MobSpawnRule> rules = biomeId == null ? List.of() : BiomeRuleManager.getMobSpawnRules(level.registryAccess(), biomeId);

        // Back-compat: if no per-biome rules are defined, fall back to tier tables/defaults.
        if (rules.isEmpty()) {
            rules = MobSpawnTableManager.getRulesForTier(tier);
            if (rules.isEmpty()) {
                rules = getDefaultRulesForTier(tier);
            }
        }
        if (rules.isEmpty()) {
            return;
        }

        long seed = level.getSeed() ^ chunkPos.toLong() ^ 0xC6BC279692B5C323L;
        int spawned = 0;

        for (MobSpawnRule rule : rules) {
            if (spawned >= MAX_TOTAL_MOBS_PER_CHUNK) {
                break;
            }

            int desired = nextInt(seed = step(seed), rule.minCount(), rule.maxCount());
            for (int i = 0; i < desired && spawned < MAX_TOTAL_MOBS_PER_CHUNK; i++) {
                if (trySpawnOne(level, chunkPos, rule.entityType(), seed = step(seed))) {
                    spawned++;
                }
            }
        }

        if (spawned > 0) {
            BrightbronzeHorizons.LOGGER.debug(
                "Spawned {} mobs for tier {} in chunk ({}, {})",
                spawned, tier.getName(), chunkPos.x, chunkPos.z
            );
        }
    }

    private static boolean shouldSpawnMobs(ServerLevel level, ChunkSpawnerTier tier) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            return false;
        }

        if (tier.alwaysSpawnsMobs()) {
            return true;
        }

        // Night-only tiers: Coal/Iron
        return isNight(level);
    }

    private static boolean isNight(ServerLevel level) {
        long dayTime = level.getDayTime() % 24000L;
        // Vanilla-ish night window (roughly): 13000..23000
        return dayTime >= 13000L && dayTime <= 23000L;
    }

    private static List<MobSpawnRule> getDefaultRulesForTier(ChunkSpawnerTier tier) {
        return switch (tier) {
            case COPPER, COAL, IRON -> List.of(
                new MobSpawnRule(EntityType.ZOMBIE, 1, 3),
                new MobSpawnRule(EntityType.SKELETON, 0, 2),
                new MobSpawnRule(EntityType.SPIDER, 0, 1)
            );
            case GOLD -> List.of(
                new MobSpawnRule(EntityType.ZOMBIFIED_PIGLIN, 1, 3),
                new MobSpawnRule(EntityType.MAGMA_CUBE, 0, 1)
            );
            case DIAMOND -> List.of(
                new MobSpawnRule(EntityType.ENDERMAN, 1, 2)
            );
            case EMERALD -> List.of(
                // Placeholder defaults; packs will provide real rules later.
                new MobSpawnRule(EntityType.ZOMBIE, 0, 2)
            );
        };
    }

    private static boolean trySpawnOne(ServerLevel level, ChunkPos chunkPos, EntityType<?> type, long seed) {
        for (int attempt = 0; attempt < MAX_POSITION_TRIES_PER_MOB; attempt++) {
            int localX = nextInt(seed = step(seed), 0, 15);
            int localZ = nextInt(seed = step(seed), 0, 15);

            int x = chunkPos.getMinBlockX() + localX;
            int z = chunkPos.getMinBlockZ() + localZ;

            BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, level.getSeaLevel(), z));
            BlockPos spawnPos = surface;

            if (!isSafeSpawnPosition(level, spawnPos)) {
                continue;
            }

            Entity entity = type.create(level, EntitySpawnReason.EVENT);
            if (entity == null) {
                return false;
            }

            entity.setPos(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

            // Let vanilla finalize mob spawn rules where applicable.
            level.addFreshEntity(entity);
            return true;
        }

        return false;
    }

    private static boolean isSafeSpawnPosition(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState below = level.getBlockState(pos.below());

        if (!feet.isAir() || !head.isAir()) {
            return false;
        }
        return below.isSolid();
    }

    /**
     * Returns a random int in [min, max] inclusive.
     */
    private static int nextInt(long seed, int min, int max) {
        if (max < min) {
            return min;
        }
        int bound = (max - min) + 1;
        int value = (int) Math.floorMod(mix64(seed), bound);
        return min + value;
    }

    private static long step(long state) {
        return state + 0x9E3779B97F4A7C15L;
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
