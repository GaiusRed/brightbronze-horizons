package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import red.gaius.brightbronze.versioned.RegistryHelper;

import java.util.Optional;

/**
 * MC 1.21.1 implementation of RegistryHelper.
 * In 1.21.1, Registry.getOptional(ResourceLocation) returns Optional<T> directly.
 */
public class RegistryHelperImpl implements RegistryHelper {
    
    @Override
    @SuppressWarnings("unchecked")
    public Optional<EntityType<?>> getEntityType(ResourceLocation id) {
        return (Optional<EntityType<?>>) (Optional<?>) BuiltInRegistries.ENTITY_TYPE.getOptional(id);
    }
}
