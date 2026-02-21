package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import red.gaius.brightbronze.versioned.EntityCopyHelper;

import java.util.Optional;

/**
 * MC 1.21.1 implementation of entity copy helper using Entity.save() and EntityType.loadEntityRecursive().
 */
public class EntityCopyHelperImpl implements EntityCopyHelper {
    
    @Override
    public Optional<CompoundTag> serializeEntity(Entity entity, ServerLevel level) {
        CompoundTag nbt = new CompoundTag();
        if (!entity.saveAsPassenger(nbt)) {
            return Optional.empty(); // Entity doesn't want to be saved
        }
        return Optional.of(nbt);
    }
    
    @Override
    public Optional<Entity> deserializeEntity(CompoundTag nbt, ServerLevel targetLevel) {
        Entity entity = EntityType.loadEntityRecursive(nbt, targetLevel, e -> e);
        return Optional.ofNullable(entity);
    }
    
    @Override
    public void loadBlockEntityData(BlockEntity blockEntity, CompoundTag nbt, ServerLevel level) {
        // In 1.21.1, use the simple load method with registry access
        blockEntity.loadWithComponents(nbt, level.registryAccess());
        blockEntity.setChanged();
    }
}
