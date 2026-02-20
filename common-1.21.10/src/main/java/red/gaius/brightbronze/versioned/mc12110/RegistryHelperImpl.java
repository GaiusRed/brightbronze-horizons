package red.gaius.brightbronze.versioned.mc12110;

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
 * MC 1.21.10 implementation of RegistryHelper.
 * In 1.21.10, Registry.get(ResourceLocation) returns Optional<Holder.Reference<T>>.
 */
public class RegistryHelperImpl implements RegistryHelper {
    
    @Override
    public Optional<EntityType<?>> getEntityType(ResourceLocation id) {
        return BuiltInRegistries.ENTITY_TYPE.get(id)
                .map(ref -> ref.value());
    }
    
    @Override
    public <T> Registry<T> lookupRegistry(RegistryAccess registryAccess, ResourceKey<? extends Registry<T>> key) {
        return registryAccess.lookupOrThrow(key);
    }
    
    @Override
    public <T> Optional<Holder<T>> getHolder(Registry<T> registry, ResourceLocation id) {
        // In 1.21.10, registry.get returns Optional<Holder.Reference<T>> which extends Holder<T>
        return registry.get(id).map(ref -> ref);
    }
    
    @Override
    public <T> Optional<Holder<T>> getHolder(Registry<T> registry, ResourceKey<T> key) {
        // In 1.21.10, registry.get returns Optional<Holder.Reference<T>> which extends Holder<T>
        return registry.get(key).map(ref -> ref);
    }
    
    @Override
    public <T> Optional<Holder.Reference<T>> getHolderReference(Registry<T> registry, ResourceKey<T> key) {
        // In 1.21.10, registry.get returns Optional<Holder.Reference<T>>
        return registry.get(key);
    }
    
    @Override
    public <T> Holder<T> getHolderOrThrow(Registry<T> registry, ResourceKey<T> key) {
        return registry.getOrThrow(key);
    }
}
