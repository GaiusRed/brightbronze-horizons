package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import red.gaius.brightbronze.versioned.RegistryHelper;

import java.util.Optional;

/**
 * MC 1.21.10 implementation of RegistryHelper.
 * In 1.21.10, Registry.get(ResourceLocation) returns Optional<Holder.Reference<T>>.
 */
public class RegistryHelperImpl implements RegistryHelper {
    
    @Override
    public Optional<EntityType<?>> getEntityType(ResourceLocation id) {
        return BuiltInRegistries.ENTITY_TYPE.get(id)
                .map(ref -> ref.value());
    }
}
