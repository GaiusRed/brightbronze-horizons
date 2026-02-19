package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import red.gaius.brightbronze.versioned.ItemRegistry;

/**
 * Minecraft 1.21.1 implementation of ItemRegistry.
 * 
 * <p>In 1.21.1, Item.Properties does NOT have setId() - the ID is set
 * during registration, not during property creation.
 */
public class ItemRegistryImpl implements ItemRegistry {
    
    @Override
    public Item.Properties properties(ResourceKey<Item> itemKey) {
        // In 1.21.1, we don't need to set the ID on properties
        return new Item.Properties();
    }
    
    @Override
    public Item.Properties properties(ResourceKey<Item> itemKey, ResourceKey<CreativeModeTab> tab) {
        // In 1.21.1, we don't need to set the ID on properties
        return new Item.Properties()
                .arch$tab(tab);
    }
    
    @Override
    public Item simpleItem(ResourceKey<Item> itemKey, ResourceKey<CreativeModeTab> tab) {
        return new Item(properties(itemKey, tab));
    }
    
    @Override
    public Item blockItem(Block block, ResourceKey<Item> itemKey, ResourceKey<CreativeModeTab> tab) {
        return new BlockItem(block, properties(itemKey, tab));
    }
    
    @Override
    public ResourceKey<Item> key(String modId, String name) {
        return ResourceKey.create(Registries.ITEM,
                ResourceLocation.parse(modId + ":" + name));
    }
}
