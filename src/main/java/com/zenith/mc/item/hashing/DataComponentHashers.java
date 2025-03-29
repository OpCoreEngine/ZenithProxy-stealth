/*
 * Copyright (c) 2025 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package com.zenith.mc.item.hashing;

import com.google.common.hash.HashCode;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.*;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;

import java.util.*;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
public class DataComponentHashers {
    private static final Map<DataComponentType<?>, MinecraftHasher<?>> hashers = new HashMap<>();

    static {
        register(DataComponentTypes.CUSTOM_DATA, MinecraftHasher.MNBT);
        registerInt(DataComponentTypes.MAX_STACK_SIZE);
        registerInt(DataComponentTypes.MAX_DAMAGE);
        registerInt(DataComponentTypes.DAMAGE);
        registerUnit(DataComponentTypes.UNBREAKABLE);

        // TODO custom name, component
        // TODO item name, component

        register(DataComponentTypes.CUSTOM_NAME, ComponentHasher.COMPONENT);
        register(DataComponentTypes.ITEM_NAME, ComponentHasher.COMPONENT);
        register(DataComponentTypes.ITEM_MODEL, MinecraftHasher.KEY);
        register(DataComponentTypes.LORE, ComponentHasher.COMPONENT.list());
        register(DataComponentTypes.RARITY, MinecraftHasher.RARITY);
        register(DataComponentTypes.ENCHANTMENTS, RegistryHasher.ITEM_ENCHANTMENTS);

        register(DataComponentTypes.CAN_PLACE_ON, RegistryHasher.ADVENTURE_MODE_PREDICATE);
        register(DataComponentTypes.CAN_BREAK, RegistryHasher.ADVENTURE_MODE_PREDICATE); // TODO needs tests
        register(DataComponentTypes.ATTRIBUTE_MODIFIERS, RegistryHasher.ATTRIBUTE_MODIFIER_ENTRY.list().cast(ItemAttributeModifiers::getModifiers)); // TODO needs tests

        registerMap(DataComponentTypes.CUSTOM_MODEL_DATA, builder -> builder
            .optionalList("floats", MinecraftHasher.FLOAT, CustomModelData::floats)
            .optionalList("flags", MinecraftHasher.BOOL, CustomModelData::flags)
            .optionalList("strings", MinecraftHasher.STRING, CustomModelData::strings)
            .optionalList("colors", MinecraftHasher.INT, CustomModelData::colors));

        registerMap(DataComponentTypes.TOOLTIP_DISPLAY, builder -> builder
            .optional("hide_tooltip", MinecraftHasher.BOOL, TooltipDisplay::hideTooltip, false)
            .optionalList("hidden_components", RegistryHasher.DATA_COMPONENT_TYPE, TooltipDisplay::hiddenComponents));

        registerInt(DataComponentTypes.REPAIR_COST);

        register(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, MinecraftHasher.BOOL);
        registerUnit(DataComponentTypes.INTANGIBLE_PROJECTILE);

        registerMap(DataComponentTypes.FOOD, builder -> builder
            .accept("nutrition", MinecraftHasher.INT, FoodProperties::getNutrition)
            .accept("saturation", MinecraftHasher.FLOAT, FoodProperties::getSaturationModifier)
            .optional("can_always_eat", MinecraftHasher.BOOL, FoodProperties::isCanAlwaysEat, false));
        registerMap(DataComponentTypes.CONSUMABLE, builder -> builder
            .optional("consume_seconds", MinecraftHasher.FLOAT, Consumable::consumeSeconds, 1.6F)
            .optional("animation", RegistryHasher.ITEM_USE_ANIMATION, Consumable::animation, Consumable.ItemUseAnimation.EAT)
            .optional("sound", RegistryHasher.SOUND_EVENT, Consumable::sound, BuiltinSound.ENTITY_GENERIC_EAT)
            .optionalList("on_consume_effects", RegistryHasher.CONSUME_EFFECT, Consumable::onConsumeEffects));

        register(DataComponentTypes.USE_REMAINDER, RegistryHasher.ITEM_STACK);

        registerMap(DataComponentTypes.USE_COOLDOWN, builder -> builder
            .accept("seconds", MinecraftHasher.FLOAT, UseCooldown::seconds)
            .optionalNullable("cooldown_group", MinecraftHasher.KEY, UseCooldown::cooldownGroup));
        registerMap(DataComponentTypes.DAMAGE_RESISTANT, builder -> builder
            .accept("types", MinecraftHasher.TAG, Function.identity()));
        registerMap(DataComponentTypes.TOOL, builder -> builder
            .acceptList("rules", RegistryHasher.TOOL_RULE, ToolData::getRules)
            .optional("default_mining_speed", MinecraftHasher.FLOAT, ToolData::getDefaultMiningSpeed, 1.0F)
            .optional("damage_per_block", MinecraftHasher.INT, ToolData::getDamagePerBlock, 1)
            .optional("can_destroy_blocks_in_creative", MinecraftHasher.BOOL, ToolData::isCanDestroyBlocksInCreative, true));
        registerMap(DataComponentTypes.WEAPON, builder -> builder
            .optional("item_damage_per_attack", MinecraftHasher.INT, Weapon::itemDamagePerAttack, 1)
            .optional("disable_blocking_for_seconds", MinecraftHasher.FLOAT, Weapon::disableBlockingForSeconds, 0.0F));
        registerMap(DataComponentTypes.ENCHANTABLE, builder -> builder
            .accept("value", MinecraftHasher.INT, Function.identity()));
        registerMap(DataComponentTypes.EQUIPPABLE, builder -> builder
            .accept("slot", MinecraftHasher.EQUIPMENT_SLOT, Equippable::slot)
            .optional("equip_sound", RegistryHasher.SOUND_EVENT, Equippable::equipSound, BuiltinSound.ITEM_ARMOR_EQUIP_GENERIC)
            .optionalNullable("asset_id", MinecraftHasher.KEY, Equippable::model)
            .optionalNullable("camera_overlay", MinecraftHasher.KEY, Equippable::cameraOverlay)
            .optionalNullable("allowed_entities", RegistryHasher.ENTITY_TYPE.holderSet(), Equippable::allowedEntities)
            .optional("dispensable", MinecraftHasher.BOOL, Equippable::dispensable, true)
            .optional("swappable", MinecraftHasher.BOOL, Equippable::dispensable, true)
            .optional("damage_on_hurt", MinecraftHasher.BOOL, Equippable::damageOnHurt, true)
            .optional("equip_on_interact", MinecraftHasher.BOOL, Equippable::equipOnInteract, false));
        registerMap(DataComponentTypes.REPAIRABLE, builder -> builder
            .accept("items", RegistryHasher.ITEM.holderSet(), Function.identity()));

        registerUnit(DataComponentTypes.GLIDER);
        register(DataComponentTypes.TOOLTIP_STYLE, MinecraftHasher.KEY);

        registerMap(DataComponentTypes.DEATH_PROTECTION, builder -> builder
            .optionalList("death_effects", RegistryHasher.CONSUME_EFFECT, Function.identity()));
        registerMap(DataComponentTypes.BLOCKS_ATTACKS, builder -> builder
            .optional("block_delay_seconds", MinecraftHasher.FLOAT, BlocksAttacks::blockDelaySeconds, 0.0F)
            .optional("disable_cooldown_scale", MinecraftHasher.FLOAT, BlocksAttacks::disableCooldownScale, 1.0F)
            .optional("damage_reductions", RegistryHasher.BLOCKS_ATTACKS_DAMAGE_REDUCTION.list(), BlocksAttacks::damageReductions, List.of(new BlocksAttacks.DamageReduction(90.0F, null, 0.0F, 1.0F)))
            .optional("item_damage", RegistryHasher.BLOCKS_ATTACKS_ITEM_DAMAGE_FUNCTION, BlocksAttacks::itemDamage, new BlocksAttacks.ItemDamageFunction(1.0F, 0.0F, 1.0F))
            .optionalNullable("bypassed_by", MinecraftHasher.TAG, BlocksAttacks::bypassedBy)
            .optionalNullable("block_sound", RegistryHasher.SOUND_EVENT, BlocksAttacks::blockSound)
            .optionalNullable("disabled_sound", RegistryHasher.SOUND_EVENT, BlocksAttacks::disableSound)); // TODO needs tests
        register(DataComponentTypes.STORED_ENCHANTMENTS, RegistryHasher.ITEM_ENCHANTMENTS);

        registerInt(DataComponentTypes.DYED_COLOR);
        registerInt(DataComponentTypes.MAP_COLOR);
        registerInt(DataComponentTypes.MAP_ID);
        register(DataComponentTypes.MAP_DECORATIONS, MinecraftHasher.MNBT);

        register(DataComponentTypes.CHARGED_PROJECTILES, RegistryHasher.ITEM_STACK.list());
        register(DataComponentTypes.BUNDLE_CONTENTS, RegistryHasher.ITEM_STACK.list());

        registerMap(DataComponentTypes.POTION_CONTENTS, builder -> builder
            .optional("potion", RegistryHasher.POTION, PotionContents::getPotionId, -1)
            .optional("custom_color", MinecraftHasher.INT, PotionContents::getCustomColor, -1)
            .optionalList("custom_effects", RegistryHasher.MOB_EFFECT_INSTANCE, PotionContents::getCustomEffects)
            .optionalNullable("custom_name", MinecraftHasher.STRING, PotionContents::getCustomName));

        register(DataComponentTypes.POTION_DURATION_SCALE, MinecraftHasher.FLOAT);
        register(DataComponentTypes.SUSPICIOUS_STEW_EFFECTS, RegistryHasher.SUSPICIOUS_STEW_EFFECT.list());

        registerMap(DataComponentTypes.WRITABLE_BOOK_CONTENT, builder -> builder
            .optionalList("pages", MinecraftHasher.STRING.filterable(), WritableBookContent::getPages));
        registerMap(DataComponentTypes.WRITTEN_BOOK_CONTENT, builder -> builder
            .accept("title", MinecraftHasher.STRING.filterable(), WrittenBookContent::getTitle)
            .accept("author", MinecraftHasher.STRING, WrittenBookContent::getAuthor)
            .accept("generation", MinecraftHasher.INT, WrittenBookContent::getGeneration)
            .optionalList("pages", ComponentHasher.COMPONENT.filterable(), WrittenBookContent::getPages)
            .optional("resolved", MinecraftHasher.BOOL, WrittenBookContent::isResolved, false));

        register(DataComponentTypes.TRIM, RegistryHasher.ARMOR_TRIM);
        register(DataComponentTypes.DEBUG_STICK_STATE, MinecraftHasher.MNBT);
        register(DataComponentTypes.ENTITY_DATA, MinecraftHasher.MNBT);
        register(DataComponentTypes.BUCKET_ENTITY_DATA, MinecraftHasher.MNBT);
        register(DataComponentTypes.BLOCK_ENTITY_DATA, MinecraftHasher.MNBT);

        register(DataComponentTypes.INSTRUMENT, RegistryHasher.INSTRUMENT_COMPONENT);
        register(DataComponentTypes.PROVIDES_TRIM_MATERIAL, RegistryHasher.PROVIDES_TRIM_MATERIAL);

        registerInt(DataComponentTypes.OMINOUS_BOTTLE_AMPLIFIER);

        register(DataComponentTypes.JUKEBOX_PLAYABLE, RegistryHasher.JUKEBOX_PLAYABLE);
        register(DataComponentTypes.PROVIDES_BANNER_PATTERNS, MinecraftHasher.TAG);
        register(DataComponentTypes.RECIPES, MinecraftHasher.NBT_LIST);

        registerMap(DataComponentTypes.LODESTONE_TRACKER, builder -> builder
            .optionalNullable("target", MinecraftHasher.GLOBAL_POS, LodestoneTracker::getPos)
            .optional("tracked", MinecraftHasher.BOOL, LodestoneTracker::isTracked, true));

        register(DataComponentTypes.FIREWORK_EXPLOSION, RegistryHasher.FIREWORK_EXPLOSION);
        registerMap(DataComponentTypes.FIREWORKS, builder -> builder
            .optional("flight_duration", MinecraftHasher.BYTE, fireworks -> (byte) fireworks.getFlightDuration(), (byte) 0)
            .optionalList("explosions", RegistryHasher.FIREWORK_EXPLOSION, Fireworks::getExplosions));

        register(DataComponentTypes.PROFILE, MinecraftHasher.GAME_PROFILE);
        register(DataComponentTypes.NOTE_BLOCK_SOUND, MinecraftHasher.KEY);
        register(DataComponentTypes.BANNER_PATTERNS, RegistryHasher.BANNER_PATTERN_LAYER.list());
        register(DataComponentTypes.BASE_COLOR, MinecraftHasher.DYE_COLOR);
        register(DataComponentTypes.POT_DECORATIONS, RegistryHasher.ITEM.list());
        register(DataComponentTypes.CONTAINER, RegistryHasher.ITEM_CONTAINER_CONTENTS);
        register(DataComponentTypes.BLOCK_STATE, MinecraftHasher.map(MinecraftHasher.STRING, MinecraftHasher.STRING).cast(BlockStateProperties::getProperties));
        register(DataComponentTypes.BEES, RegistryHasher.BEEHIVE_OCCUPANT.list());

        register(DataComponentTypes.LOCK, MinecraftHasher.MNBT);
        register(DataComponentTypes.CONTAINER_LOOT, MinecraftHasher.MNBT);
        register(DataComponentTypes.BREAK_SOUND, RegistryHasher.SOUND_EVENT);

        register(DataComponentTypes.VILLAGER_VARIANT, RegistryHasher.VILLAGER_TYPE);
        register(DataComponentTypes.WOLF_VARIANT, RegistryHasher.WOLF_VARIANT);
        register(DataComponentTypes.WOLF_SOUND_VARIANT, RegistryHasher.WOLF_SOUND_VARIANT);
        register(DataComponentTypes.WOLF_COLLAR, MinecraftHasher.DYE_COLOR);
        register(DataComponentTypes.FOX_VARIANT, RegistryHasher.FOX_VARIANT);
        register(DataComponentTypes.SALMON_SIZE, RegistryHasher.SALMON_VARIANT);
        register(DataComponentTypes.PARROT_VARIANT, RegistryHasher.PARROT_VARIANT);
        register(DataComponentTypes.TROPICAL_FISH_PATTERN, RegistryHasher.TROPICAL_FISH_PATTERN);
        register(DataComponentTypes.TROPICAL_FISH_BASE_COLOR, MinecraftHasher.DYE_COLOR);
        register(DataComponentTypes.TROPICAL_FISH_PATTERN_COLOR, MinecraftHasher.DYE_COLOR);
        register(DataComponentTypes.MOOSHROOM_VARIANT, RegistryHasher.MOOSHROOM_VARIANT);
        register(DataComponentTypes.RABBIT_VARIANT, RegistryHasher.RABBIT_VARIANT);
        register(DataComponentTypes.PIG_VARIANT, RegistryHasher.PIG_VARIANT);
        register(DataComponentTypes.COW_VARIANT, RegistryHasher.COW_VARIANT);
        register(DataComponentTypes.CHICKEN_VARIANT, MinecraftHasher.KEY
            .cast(holder -> "chiken")); // todo: xdd
//            .cast(holder -> holder.getOrCompute(id -> JavaRegistries.CHICKEN_VARIANT.keyFromNetworkId(id)))); // Why, Mojang?
        register(DataComponentTypes.FROG_VARIANT, RegistryHasher.FROG_VARIANT);
        register(DataComponentTypes.HORSE_VARIANT, RegistryHasher.HORSE_VARIANT);
        register(DataComponentTypes.PAINTING_VARIANT, RegistryHasher.PAINTING_VARIANT);
        register(DataComponentTypes.LLAMA_VARIANT, RegistryHasher.LLAMA_VARIANT);
        register(DataComponentTypes.AXOLOTL_VARIANT, RegistryHasher.AXOLOTL_VARIANT);
        register(DataComponentTypes.CAT_VARIANT, RegistryHasher.CAT_VARIANT);
        register(DataComponentTypes.CAT_COLLAR, MinecraftHasher.DYE_COLOR);
        register(DataComponentTypes.SHEEP_COLOR, MinecraftHasher.DYE_COLOR);
        register(DataComponentTypes.SHULKER_COLOR, MinecraftHasher.DYE_COLOR);
    }

    private static void registerUnit(DataComponentType<Unit> component) {
        register(component, MinecraftHasher.UNIT);
    }

    private static void registerInt(IntComponentType component) {
        register(component, MinecraftHasher.INT);
    }

    private static <T> void registerMap(DataComponentType<T> component, MapBuilder<T> builder) {
        register(component, MinecraftHasher.mapBuilder(builder));
    }

    private static <T> void register(DataComponentType<T> component, MinecraftHasher<T> hasher) {
        if (hashers.containsKey(component)) {
            throw new IllegalArgumentException("Tried to register a hasher for a component twice");
        }
        hashers.put(component, hasher);
    }

    public static <T> MinecraftHasher<T> hasherOrEmpty(DataComponentType<T> component) {
        MinecraftHasher<T> hasher = (MinecraftHasher<T>) hashers.get(component);
        if (hasher == null) {
            return MinecraftHasher.UNIT.cast(value -> Unit.INSTANCE);
        }
        return hasher;
    }

    public static <T> HashCode hash(DataComponentType<T> component, T value) {
        MinecraftHasher<T> hasher = (MinecraftHasher<T>) hashers.get(component);
        if (hasher == null) {
            throw new IllegalStateException("Unregistered hasher for component " + component + "!");
        }
        return hasher.hash(value, new MinecraftHashEncoder());
    }

    public static HashedStack hashStack(ItemStack stack) {
        if (stack == null) {
            return null;
        }

        DataComponents patch = stack.getDataComponents();
        if (patch == null) {
            return new HashedStack(stack.getId(), stack.getAmount(), Map.of(), Set.of());
        }
        Map<DataComponentType<?>, DataComponent<?, ?>> components = patch.getDataComponents();
        Map<DataComponentType<?>, Integer> hashedAdditions = new HashMap<>();
        Set<DataComponentType<?>> removals = new HashSet<>();
        for (Map.Entry<DataComponentType<?>, DataComponent<?, ?>> component : components.entrySet()) {
            if (component.getValue().getValue() == null) {
                removals.add(component.getKey());
            } else {
                hashedAdditions.put(component.getKey(), hash((DataComponentType) component.getKey(), component.getValue().getValue()).asInt());
            }
        }
        return new HashedStack(stack.getId(), stack.getAmount(), hashedAdditions, removals);
    }
}
