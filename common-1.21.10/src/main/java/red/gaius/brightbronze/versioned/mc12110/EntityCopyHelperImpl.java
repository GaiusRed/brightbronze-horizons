package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import red.gaius.brightbronze.versioned.EntityCopyHelper;

import java.util.Optional;

/**
 * MC 1.21.10 implementation of entity copy helper using TagValueOutput/TagValueInput APIs.
 */
public class EntityCopyHelperImpl implements EntityCopyHelper {
    
    @Override
    public Optional<CompoundTag> serializeEntity(Entity entity, ServerLevel level) {
        TagValueOutput valueOutput = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING,
                level.registryAccess()
        );
        if (!entity.saveAsPassenger(valueOutput)) {
            return Optional.empty(); // Entity doesn't want to be saved
        }
        return Optional.of(valueOutput.buildResult());
    }
    
    @Override
    public Optional<Entity> deserializeEntity(CompoundTag nbt, ServerLevel targetLevel) {
        ValueInput valueInput = TagValueInput.create(
                ProblemReporter.DISCARDING,
                targetLevel.registryAccess(),
                nbt
        );
        return EntityType.create(valueInput, targetLevel, EntitySpawnReason.LOAD);
    }
    
    @Override
    public void loadBlockEntityData(BlockEntity blockEntity, CompoundTag nbt, ServerLevel level) {
        ValueInput valueInput = TagValueInput.create(
                ProblemReporter.DISCARDING,
                level.registryAccess(),
                nbt
        );
        blockEntity.loadWithComponents(valueInput);
        blockEntity.setChanged();
    }
}
