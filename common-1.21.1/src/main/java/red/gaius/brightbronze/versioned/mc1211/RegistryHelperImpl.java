package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
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
    
    @Override
    public <T> Registry<T> lookupRegistry(RegistryAccess registryAccess, ResourceKey<? extends Registry<T>> key) {
        return registryAccess.registryOrThrow(key);
    }
    
    @Override
    public <T> Optional<Holder<T>> getHolder(Registry<T> registry, ResourceLocation id) {
        // In 1.21.1, registry.getHolder returns Optional<Holder<T>>
        return registry.getHolder(id).map(h -> h);
    }
    
    @Override
    public <T> Optional<Holder<T>> getHolder(Registry<T> registry, ResourceKey<T> key) {
        // In 1.21.1, registry.getHolder returns Optional<Holder<T>>
        return registry.getHolder(key).map(h -> h);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<Holder.Reference<T>> getHolderReference(Registry<T> registry, ResourceKey<T> key) {
        // In 1.21.1, registry.getHolder returns Optional<Holder.Reference<T>>
        // The cast is safe because getHolder actually returns Holder.Reference
        return registry.getHolder(key).map(h -> (Holder.Reference<T>) h);
    }
    
    @Override
    public <T> Holder<T> getHolderOrThrow(Registry<T> registry, ResourceKey<T> key) {
        return registry.getHolderOrThrow(key);
    }
}
