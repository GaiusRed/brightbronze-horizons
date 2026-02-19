package red.gaius.brightbronze.versioned.mc1211;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import red.gaius.brightbronze.versioned.ArmorFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Minecraft 1.21.1 implementation of ArmorFactory.
 * 
 * <p>In 1.21.1, ArmorMaterial is a record with these parameters:
 * - Map&lt;ArmorItem.Type, Integer&gt; defense
 * - int enchantability
 * - Holder&lt;SoundEvent&gt; equipSound
 * - Supplier&lt;Ingredient&gt; repairIngredient
 * - List&lt;ArmorMaterial.Layer&gt; layers
 * - float toughness
 * - float knockbackResistance
 */
public class ArmorFactoryImpl implements ArmorFactory {
    
    @Override
    public Object createArmorMaterial(
            String modId,
            String name,
            int baseDurability,
            Map<ArmorSlot, Integer> defense,
            int enchantability,
            Holder<SoundEvent> equipSound,
            float toughness,
            float knockbackResistance,
            TagKey<Item> repairItemsTag) {
        
        // Convert our ArmorSlot to MC's ArmorItem.Type for the defense map
        Map<ArmorItem.Type, Integer> defenseMap = new EnumMap<>(ArmorItem.Type.class);
        defense.forEach((slot, value) -> defenseMap.put(toArmorType(slot), value));
        
        // Create repair ingredient supplier
        Supplier<Ingredient> repairIngredient = () -> Ingredient.of(repairItemsTag);
        
        // Create armor layers (for texture rendering)
        ResourceLocation textureLocation = ResourceLocation.parse(modId + ":" + name);
        List<ArmorMaterial.Layer> layers = List.of(
                new ArmorMaterial.Layer(textureLocation)
        );
        
        // In 1.21.1, ArmorMaterial is a record we instantiate directly
        return new ArmorMaterial(
                defenseMap,
                enchantability,
                equipSound,
                repairIngredient,
                layers,
                toughness,
                knockbackResistance
        );
    }
    
    @Override
    public Item createArmorItem(
            ResourceKey<Item> key,
            Object material,
            ArmorSlot slot,
            int baseDurability,
            ResourceKey<CreativeModeTab> tab) {
        
        ArmorMaterial armorMaterial = (ArmorMaterial) material;
        ArmorItem.Type armorType = toArmorType(slot);
        
        // In 1.21.1, ArmorItem expects Holder<ArmorMaterial>
        Holder<ArmorMaterial> armorMaterialHolder = Holder.direct(armorMaterial);
        
        return new ArmorItem(armorMaterialHolder, armorType, new Item.Properties()
                .arch$tab(tab)
                .durability(armorType.getDurability(baseDurability)));
    }
    
    @Override
    public int getDurability(ArmorSlot slot, int baseDurability) {
        return toArmorType(slot).getDurability(baseDurability);
    }
    
    private static ArmorItem.Type toArmorType(ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> ArmorItem.Type.HELMET;
            case CHESTPLATE -> ArmorItem.Type.CHESTPLATE;
            case LEGGINGS -> ArmorItem.Type.LEGGINGS;
            case BOOTS -> ArmorItem.Type.BOOTS;
        };
    }
}
