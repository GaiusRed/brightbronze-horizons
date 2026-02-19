package red.gaius.brightbronze.versioned;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.Optional;

/**
 * Version-specific helper for registry lookups.
 * In MC 1.21.10, Registry.get(ResourceLocation) returns Optional<Holder.Reference<T>>.
 * In MC 1.21.1, registries use different lookup methods.
 */
public interface RegistryHelper {
    /**
     * Look up an entity type by resource location.
     *
     * @param id the entity type resource location
     * @return Optional containing the entity type if found, empty otherwise
     */
    Optional<EntityType<?>> getEntityType(ResourceLocation id);
}
