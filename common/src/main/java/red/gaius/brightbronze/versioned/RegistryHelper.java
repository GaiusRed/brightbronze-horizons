package red.gaius.brightbronze.versioned;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
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
    
    /**
     * Look up a registry by key from RegistryAccess.
     * In 1.21.10: registryAccess.lookupOrThrow(key)
     * In 1.21.1: registryAccess.registryOrThrow(key)
     */
    <T> Registry<T> lookupRegistry(RegistryAccess registryAccess, ResourceKey<? extends Registry<T>> key);
    
    /**
     * Get a Holder from a Registry by ResourceLocation.
     * In 1.21.10: registry.get(id).map(ref -> ref) (returns Optional<Holder.Reference<T>>)
     * In 1.21.1: registry.getHolder(id) (returns Optional<Holder<T>>)
     */
    <T> Optional<Holder<T>> getHolder(Registry<T> registry, ResourceLocation id);
    
    /**
     * Get a Holder from a Registry by ResourceKey.
     * In 1.21.10: registry.get(key).map(ref -> ref) (returns Optional<Holder.Reference<T>>)
     * In 1.21.1: registry.getHolder(key) (returns Optional<Holder<T>>)
     */
    <T> Optional<Holder<T>> getHolder(Registry<T> registry, ResourceKey<T> key);
    
    /**
     * Get a Holder.Reference from a Registry by ResourceKey.
     * This is needed for reflection-based value replacement in frozen registries.
     * In 1.21.10: registry.get(key) (returns Optional<Holder.Reference<T>>)
     * In 1.21.1: registry.getHolder(key) (returns Optional<Holder.Reference<T>>)
     */
    <T> Optional<Holder.Reference<T>> getHolderReference(Registry<T> registry, ResourceKey<T> key);
    
    /**
     * Get a required Holder from a Registry, throwing if not found.
     * In 1.21.10: registry.get(key).orElseThrow()
     * In 1.21.1: registry.getHolderOrThrow(key) or registry.getHolder(key).orElseThrow()
     */
    <T> Holder<T> getHolderOrThrow(Registry<T> registry, ResourceKey<T> key);
}
