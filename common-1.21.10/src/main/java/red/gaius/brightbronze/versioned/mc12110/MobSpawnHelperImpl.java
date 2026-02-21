package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import red.gaius.brightbronze.versioned.MobSpawnHelper;

/**
 * MC 1.21.10 implementation using EntitySpawnReason.EVENT.
 */
public class MobSpawnHelperImpl implements MobSpawnHelper {
    
    @Override
    @Nullable
    public Entity createForEvent(EntityType<?> type, ServerLevel level) {
        return type.create(level, EntitySpawnReason.EVENT);
    }
}
