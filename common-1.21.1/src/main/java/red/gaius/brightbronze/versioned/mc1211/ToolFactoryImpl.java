package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import red.gaius.brightbronze.versioned.ToolFactory;

import java.util.function.Supplier;

/**
 * Minecraft 1.21.1 implementation of ToolFactory.
 * 
 * <p>In 1.21.1, tools use the Tier interface and specific item classes
 * (SwordItem, PickaxeItem, etc.) rather than Item.Properties methods.
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
        // In 1.21.1, we create a Tier implementation
        return new Tier() {
            @Override
            public int getUses() {
                return durability;
            }
            
            @Override
            public float getSpeed() {
                return speed;
            }
            
            @Override
            public float getAttackDamageBonus() {
                return attackDamageBonus;
            }
            
            @Override
            public TagKey<Block> getIncorrectBlocksForDrops() {
                return incorrectBlocksTag;
            }
            
            @Override
            public int getEnchantmentValue() {
                return enchantability;
            }
            
            @Override
            public Ingredient getRepairIngredient() {
                return Ingredient.of(repairItemsTag);
            }
        };
    }
    
    @Override
    public Item createSword(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab) {
        Tier tier = (Tier) material;
        // In 1.21.1, attack damage for SwordItem is added to the tier's attackDamageBonus
        // and attackSpeed is the actual speed value (negative offset from 4.0)
        return new SwordItem(tier, new Item.Properties()
                .arch$tab(tab)
                .attributes(SwordItem.createAttributes(tier, (int) attackDamage, attackSpeed)));
    }
    
    @Override
    public Item createPickaxe(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab) {
        Tier tier = (Tier) material;
        return new PickaxeItem(tier, new Item.Properties()
                .arch$tab(tab)
                .attributes(PickaxeItem.createAttributes(tier, attackDamage, attackSpeed)));
    }
    
    @Override
    public Item createAxe(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab) {
        Tier tier = (Tier) material;
        return new AxeItem(tier, new Item.Properties()
                .arch$tab(tab)
                .attributes(AxeItem.createAttributes(tier, attackDamage, attackSpeed)));
    }
    
    @Override
    public Item createShovel(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab) {
        Tier tier = (Tier) material;
        return new ShovelItem(tier, new Item.Properties()
                .arch$tab(tab)
                .attributes(ShovelItem.createAttributes(tier, attackDamage, attackSpeed)));
    }
    
    @Override
    public Item createHoe(ResourceKey<Item> key, Object material, float attackDamage, float attackSpeed, ResourceKey<CreativeModeTab> tab) {
        Tier tier = (Tier) material;
        return new HoeItem(tier, new Item.Properties()
                .arch$tab(tab)
                .attributes(HoeItem.createAttributes(tier, attackDamage, attackSpeed)));
    }
}
