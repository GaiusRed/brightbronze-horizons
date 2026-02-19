package red.gaius.brightbronze.versioned.mc12110;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import red.gaius.brightbronze.versioned.ToolFactory;

/**
 * Minecraft 1.21.10 implementation of ToolFactory.
 * 
 * <p>In 1.21.10, tools are created via Item.Properties methods (sword(), pickaxe(), etc.)
 * and ToolMaterial is a record class.
 */
public class ToolFactoryImpl implements ToolFactory {
    
    @Override
    public Object createToolMaterial(
            TagKey<Block> incorrectBlocksTag,
            int durability,
            float speed,
            float attackDamageBonus,
            int enchantability,
            TagKey<Item> repairItemsTag) {
        return new ToolMaterial(
                incorrectBlocksTag,
                durability,
                speed,
                attackDamageBonus,
                enchantability,
                repairItemsTag
        );
    }
    
    @Override
    public Item createSword(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab) {
        ToolMaterial toolMaterial = (ToolMaterial) material;
        return new Item(new Item.Properties()
                .setId(key)
                .arch$tab(tab)
                .sword(toolMaterial, attackDamage, attackSpeed));
    }
    
    @Override
    public Item createPickaxe(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab) {
        ToolMaterial toolMaterial = (ToolMaterial) material;
        return new Item(new Item.Properties()
                .setId(key)
                .arch$tab(tab)
                .pickaxe(toolMaterial, attackDamage, attackSpeed));
    }
    
    @Override
    public Item createAxe(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab) {
        ToolMaterial toolMaterial = (ToolMaterial) material;
        return new Item(new Item.Properties()
                .setId(key)
                .arch$tab(tab)
                .axe(toolMaterial, attackDamage, attackSpeed));
    }
    
    @Override
    public Item createShovel(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab) {
        ToolMaterial toolMaterial = (ToolMaterial) material;
        return new Item(new Item.Properties()
                .setId(key)
                .arch$tab(tab)
                .shovel(toolMaterial, attackDamage, attackSpeed));
    }
    
    @Override
    public Item createHoe(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab) {
        ToolMaterial toolMaterial = (ToolMaterial) material;
        return new Item(new Item.Properties()
                .setId(key)
                .arch$tab(tab)
                .hoe(toolMaterial, attackDamage, attackSpeed));
    }
}
