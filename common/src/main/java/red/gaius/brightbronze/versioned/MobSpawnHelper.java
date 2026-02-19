package red.gaius.brightbronze.versioned;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import org.jetbrains.annotations.Nullable;

/**
 * Abstracts mob spawning across Minecraft versions.
 * 
 * <p>MC 1.21.10 uses {@code EntityType.create(level, EntitySpawnReason.EVENT)}.
 * <p>MC 1.21.1 uses {@code EntityType.create(level)} without spawn reason.
 */
public interface MobSpawnHelper {
    
    /**
     * Creates a new entity for event-based spawning (e.g., chunk mob spawning).
     * 
     * <p>In 1.21.10: Uses {@code EntitySpawnReason.EVENT}
     * <p>In 1.21.1: Uses simple creation without spawn reason
     * 
     * @param type The entity type to create
     * @param level The level to spawn in
     * @return The created entity, or null if creation failed
     */
    @Nullable
    Entity createForEvent(EntityType<?> type, ServerLevel level);
}
