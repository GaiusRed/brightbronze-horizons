package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import red.gaius.brightbronze.versioned.ItemRegistry;

/**
 * Minecraft 1.21.10 implementation of ItemRegistry.
 * 
 * <p>In 1.21.10, Item.Properties requires setId() to be called before construction.
 */
public class ItemRegistryImpl implements ItemRegistry {
    
    @Override
    public Item.Properties properties(ResourceKey<Item> itemKey) {
        return new Item.Properties().setId(itemKey);
    }
    
    @Override
    public Item.Properties properties(ResourceKey<Item> itemKey, ResourceKey<CreativeModeTab> tab) {
        return new Item.Properties()
                .setId(itemKey)
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
                ResourceLocation.fromNamespaceAndPath(modId, name));
    }
}
