package red.gaius.brightbronze.versioned;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

/**
 * Abstracts entity and block entity serialization/deserialization across Minecraft versions.
 * 
 * <p>MC 1.21.10 uses the new TagValueOutput/TagValueInput APIs.
 * <p>MC 1.21.1 uses Entity.save()/EntityType.loadEntityRecursive() and BlockEntity.load().
 */
public interface EntityCopyHelper {
    
    /**
     * Serializes an entity to NBT for copying purposes.
     * 
     * @param entity The entity to serialize
     * @param level The level the entity is in (for registry access)
     * @return NBT data for the entity, or empty if entity refuses to save
     */
    Optional<CompoundTag> serializeEntity(Entity entity, ServerLevel level);
    
    /**
     * Creates a new entity from NBT data in the target level.
     * 
     * @param nbt The serialized entity data
     * @param targetLevel The level to create the entity in
     * @return The newly created entity, or empty if creation failed
     */
    Optional<Entity> deserializeEntity(CompoundTag nbt, ServerLevel targetLevel);
    
    /**
     * Loads NBT data into an existing block entity.
     * 
     * <p>MC 1.21.10 uses {@code BlockEntity.loadWithComponents(ValueInput)}.
     * <p>MC 1.21.1 uses {@code BlockEntity.load(CompoundTag)}.
     * 
     * @param blockEntity The block entity to load data into
     * @param nbt The NBT data to load
     * @param level The level for registry access
     */
    void loadBlockEntityData(BlockEntity blockEntity, CompoundTag nbt, ServerLevel level);
}
