package red.gaius.brightbronze.world.mob;

import net.minecraft.world.entity.EntityType;

/**
 * A simple spawn rule for scripted one-time mob spawns when a chunk is revealed.
 *
 * <p>Phase 7 starts with a small hardcoded rule set. Phase 8 will migrate this
 * to data-driven configuration.
 */
public record MobSpawnRule(EntityType<?> entityType, int minCount, int maxCount) {
    public MobSpawnRule {
        if (entityType == null) {
            throw new IllegalArgumentException("entityType must not be null");
        }
        if (minCount < 0 || maxCount < 0 || maxCount < minCount) {
            throw new IllegalArgumentException("invalid count range");
        }
    }
}
