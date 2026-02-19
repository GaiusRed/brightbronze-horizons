package red.gaius.brightbronze.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import red.gaius.brightbronze.BrightbronzeHorizons;
import red.gaius.brightbronze.versioned.ArmorFactory.ArmorSlot;
import red.gaius.brightbronze.versioned.Versioned;

import java.util.Map;

/**
 * Registry for armor materials and armor items.
 * 
 * <p>Uses the version abstraction layer to handle differences between MC versions.
 * In MC 1.21.10+, armor is created via Item.Properties#humanoidArmor() method.
 * In MC 1.21.1, armor is created via ArmorItem class.
 */
public final class ModArmorMaterials {

    // ===== Helper method to create ResourceKey =====
    
    private static ResourceKey<Item> key(String name) {
        return Versioned.items().key(BrightbronzeHorizons.MOD_ID, name);
    }

    /**
     * Base durability multiplier for Brightbronze armor.
     * Positioned between Iron (15) and Diamond (33).
     */
    public static final int BASE_DURABILITY = 24;

    /**
     * Armor material handle - the actual type is version-specific.
     * Lazily initialized to ensure Versioned is ready.
     */
    private static Object armorMaterial;

    /**
     * Returns the Brightbronze armor material.
     * Positioned between Iron and Diamond:
     * <ul>
     *   <li>Defense values: Boots=2, Leggings=5, Chestplate=7, Helmet=2 (Iron: 2,5,6,2; Diamond: 3,6,8,3)</li>
     *   <li>Enchantability: 18 (Iron: 9, Diamond: 10)</li>
     *   <li>Toughness: 1.0f (Iron: 0, Diamond: 2.0f)</li>
     *   <li>Knockback Resistance: 0 (same as Iron/Diamond)</li>
     * </ul>
     */
    public static Object getArmorMaterial() {
        if (armorMaterial == null) {
            armorMaterial = Versioned.armor().createArmorMaterial(
                    BrightbronzeHorizons.MOD_ID,
                    "brightbronze",
                    BASE_DURABILITY,
                    Map.of(
                            ArmorSlot.HELMET, 2,
                            ArmorSlot.CHESTPLATE, 7,
                            ArmorSlot.LEGGINGS, 5,
                            ArmorSlot.BOOTS, 2
                    ),
                    18, // enchantability
                    SoundEvents.ARMOR_EQUIP_IRON,
                    1.0f, // toughness
                    0.0f, // knockback resistance
                    ModTags.Items.REPAIRS_BRIGHTBRONZE_ARMOR
            );
        }
        return armorMaterial;
    }

    // ===== Brightbronze Armor Items =====

    public static final RegistrySupplier<Item> BRIGHTBRONZE_HELMET = ModItems.ITEMS.register(
            "brightbronze_helmet",
            () -> Versioned.armor().createHelmet(
                    key("brightbronze_helmet"),
                    getArmorMaterial(),
                    BASE_DURABILITY,
                    CreativeModeTabs.COMBAT));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_CHESTPLATE = ModItems.ITEMS.register(
            "brightbronze_chestplate",
            () -> Versioned.armor().createChestplate(
                    key("brightbronze_chestplate"),
                    getArmorMaterial(),
                    BASE_DURABILITY,
                    CreativeModeTabs.COMBAT));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_LEGGINGS = ModItems.ITEMS.register(
            "brightbronze_leggings",
            () -> Versioned.armor().createLeggings(
                    key("brightbronze_leggings"),
                    getArmorMaterial(),
                    BASE_DURABILITY,
                    CreativeModeTabs.COMBAT));

    public static final RegistrySupplier<Item> BRIGHTBRONZE_BOOTS = ModItems.ITEMS.register(
            "brightbronze_boots",
            () -> Versioned.armor().createBoots(
                    key("brightbronze_boots"),
                    getArmorMaterial(),
                    BASE_DURABILITY,
                    CreativeModeTabs.COMBAT));

    private ModArmorMaterials() {
    }

    public static void register() {
        // ArmorMaterial handling is version-specific
        // The armor items are registered via ModItems.ITEMS
        BrightbronzeHorizons.LOGGER.debug("Initialized mod armor materials");
    }
}
