package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import red.gaius.brightbronze.versioned.MobSpawnHelper;

/**
 * MC 1.21.1 implementation using simple EntityType.create() without spawn reason.
 */
public class MobSpawnHelperImpl implements MobSpawnHelper {
    
    @Override
    @Nullable
    public Entity createForEvent(EntityType<?> type, ServerLevel level) {
        return type.create(level);
    }
}
