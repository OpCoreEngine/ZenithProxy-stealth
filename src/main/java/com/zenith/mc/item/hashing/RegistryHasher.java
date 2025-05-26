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
import com.viaversion.nbt.io.MNBTIO;
import com.viaversion.nbt.tag.CompoundTag;
import com.zenith.mc.DynamicRegistry;
import com.zenith.mc.Registry;
import com.zenith.mc.block.BlockRegistry;
import com.zenith.mc.enchantment.EnchantmentRegistry;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.mc.item.hashing.data.ConsumeEffectType;
import com.zenith.mc.item.hashing.data.FireworkExplosionShape;
import com.zenith.mc.item.hashing.data.ItemContainerSlot;
import com.zenith.mc.item.hashing.data.entity.*;
import com.zenith.mc.potion.PotionRegistry;
import net.kyori.adventure.key.Key;
import org.geysermc.mcprotocollib.protocol.data.game.Holder;
import org.geysermc.mcprotocollib.protocol.data.game.entity.Effect;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.AttributeType;
import org.geysermc.mcprotocollib.protocol.data.game.entity.attribute.ModifierOperation;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.PaintingVariant;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.*;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.BuiltinSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.CustomSound;
import org.geysermc.mcprotocollib.protocol.data.game.level.sound.Sound;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.zenith.Globals.CLIENT_LOG;

/**
 * {@link RegistryHasher}s are hashers that hash a network integer ID to a namespaced identifier. {@link RegistryHasher}s can be created using static utility methods in this class, and all registry hashers should be kept in here.
 *
 * <p>The {@link DirectType} parameter is only used for registry hashers that are able to encode {@link Holder}s, and must be left as a {@code ?} if this functionality is not in use. This makes it clear the hasher is not
 * supposed to be able to encode holders.</p>
 *
 * <p>To create a hasher that can encode a {@link Holder}, a direct hasher should be created that hashes a {@link DirectType} (in case of a custom holder), and {@link RegistryHasher#registry(Registry, MinecraftHasher)}
 * should be used to create the registry hasher. {@link RegistryHasher#holder()} can then be used to obtain a hasher that encodes a holder of {@link DirectType}.</p>
 *
 * <p>Along with {@link RegistryHasher}s, this class also contains a bunch of hashers for various Minecraft objects. For organisational purposes, these are grouped in various sections with comments.</p>
 *
 * @param <DirectType> the type this hasher hashes. Only used for registry hashers that can hash holders.
 */
public interface RegistryHasher<DirectType> extends MinecraftHasher<Integer> {

    // Java registries

    RegistryHasher<?> BLOCK = registry(BlockRegistry.REGISTRY);

    RegistryHasher<?> ITEM = registry(ItemRegistry.REGISTRY);

    RegistryHasher<?> ENTITY_TYPE = enumIdRegistry(EntityType.values());

    RegistryHasher<?> ENCHANTMENT = dynamicRegistry(EnchantmentRegistry.REGISTRY);

    RegistryHasher<?> ATTRIBUTE = enumIdRegistry(AttributeType.Builtin.values(), AttributeType::getIdentifier);

    MinecraftHasher<DataComponentType<?>> DATA_COMPONENT_TYPE = KYORI_KEY.cast(DataComponentType::getKey);

    // Mob effects can both be an enum constant or ID in MCPL.
    MinecraftHasher<Effect> EFFECT = enumRegistry();

    RegistryHasher<?> EFFECT_ID = enumIdRegistry(Effect.values());

    RegistryHasher<?> POTION = registry(PotionRegistry.REGISTRY);

    RegistryHasher<?> VILLAGER_TYPE = enumIdRegistry(VillagerVariant.values());

    // Java data-driven registries

    MinecraftHasher<BuiltinSound> BUILTIN_SOUND = KEY.cast(BuiltinSound::getName);

    MinecraftHasher<CustomSound> CUSTOM_SOUND = MinecraftHasher.mapBuilder(builder -> builder
        .accept("sound_id", KEY, CustomSound::getName)
        .optional("range", FLOAT, CustomSound::getRange, 16.0F));

    MinecraftHasher<Sound> SOUND_EVENT = (sound, encoder) -> {
        if (sound instanceof BuiltinSound builtin) {
            return BUILTIN_SOUND.hash(builtin, encoder);
        }
        return CUSTOM_SOUND.hash((CustomSound) sound, encoder);
    };

    MinecraftHasher<InstrumentComponent.Instrument> DIRECT_INSTRUMENT = MinecraftHasher.mapBuilder(builder -> builder
        .accept("sound_event", SOUND_EVENT, InstrumentComponent.Instrument::soundEvent)
        .accept("use_duration", FLOAT, InstrumentComponent.Instrument::useDuration)
        .accept("range", FLOAT, InstrumentComponent.Instrument::range)
        .accept("description", ComponentHasher.COMPONENT, InstrumentComponent.Instrument::description));

    MinecraftHasher<Holder<InstrumentComponent.Instrument>> INSTRUMENT = holderIdOnly(DIRECT_INSTRUMENT);

    MinecraftHasher<ArmorTrim.TrimMaterial> DIRECT_TRIM_MATERIAL = MinecraftHasher.mapBuilder(builder -> builder
        .accept("asset_name", MinecraftHasher.STRING, ArmorTrim.TrimMaterial::assetBase)
        .optional("override_armor_assets", MinecraftHasher.map(KEY, STRING), ArmorTrim.TrimMaterial::assetOverrides, Map.of())
        .accept("description", ComponentHasher.COMPONENT, ArmorTrim.TrimMaterial::description));

    MinecraftHasher<Holder<ArmorTrim.TrimMaterial>> TRIM_MATERIAL = holderIdOnly(DIRECT_TRIM_MATERIAL);

    MinecraftHasher<ArmorTrim.TrimPattern> DIRECT_TRIM_PATTERN = MinecraftHasher.mapBuilder(builder -> builder
        .accept("asset_id", KEY, ArmorTrim.TrimPattern::assetId)
        .accept("description", ComponentHasher.COMPONENT, ArmorTrim.TrimPattern::description)
        .accept("decal", BOOL, ArmorTrim.TrimPattern::decal));

    MinecraftHasher<Holder<ArmorTrim.TrimPattern>> TRIM_PATTERN = holderIdOnly(DIRECT_TRIM_PATTERN);

    MinecraftHasher<JukeboxPlayable.JukeboxSong> DIRECT_JUKEBOX_SONG = MinecraftHasher.mapBuilder(builder -> builder
        .accept("sound_event", SOUND_EVENT, JukeboxPlayable.JukeboxSong::soundEvent)
        .accept("description", ComponentHasher.COMPONENT, JukeboxPlayable.JukeboxSong::description)
        .accept("length_in_seconds", FLOAT, JukeboxPlayable.JukeboxSong::lengthInSeconds)
        .accept("comparator_output", INT, JukeboxPlayable.JukeboxSong::comparatorOutput));

    MinecraftHasher<Holder<JukeboxPlayable.JukeboxSong>> JUKEBOX_SONG = holderIdOnly(DIRECT_JUKEBOX_SONG);

    MinecraftHasher<BannerPatternLayer.BannerPattern> DIRECT_BANNER_PATTERN = MinecraftHasher.mapBuilder(builder -> builder
        .accept("asset_id", KEY, BannerPatternLayer.BannerPattern::getAssetId)
        .accept("translation_key", STRING, BannerPatternLayer.BannerPattern::getTranslationKey));

    MinecraftHasher<Holder<BannerPatternLayer.BannerPattern>> BANNER_PATTERN = holderIdOnly(DIRECT_BANNER_PATTERN);

    RegistryHasher<?> WOLF_VARIANT = enumIdRegistry(WolfVariant.values());

    RegistryHasher<?> WOLF_SOUND_VARIANT = enumIdRegistry(WolfSoundVariants.values());

    RegistryHasher<?> PIG_VARIANT = enumIdRegistry(PigVariants.values());

    RegistryHasher<?> COW_VARIANT = enumIdRegistry(CowVariants.values());

    RegistryHasher<?> FROG_VARIANT = enumIdRegistry(FrogVariants.values());

    MinecraftHasher<PaintingVariant> DIRECT_PAINTING_VARIANT = MinecraftHasher.mapBuilder(builder -> builder
        .accept("width", INT, PaintingVariant::width)
        .accept("height", INT, PaintingVariant::height)
        .accept("asset_id", KEY, PaintingVariant::assetId)
        .optionalNullable("title", ComponentHasher.COMPONENT, PaintingVariant::title)
        .optionalNullable("author", ComponentHasher.COMPONENT, PaintingVariant::author));

    MinecraftHasher<Holder<PaintingVariant>> PAINTING_VARIANT = holderIdOnly(DIRECT_PAINTING_VARIANT);

    RegistryHasher<?> CAT_VARIANT = enumIdRegistry(CatVariants.values());

    // Entity variants
    // These are all not registries on Java, meaning they serialise as just literal strings, not namespaced IDs

    MinecraftHasher<Integer> FOX_VARIANT = MinecraftHasher.fromIdEnum(FoxVariant.values());

    MinecraftHasher<Integer> SALMON_VARIANT = MinecraftHasher.fromIdEnum(SalmonVariant.values());

    MinecraftHasher<Integer> PARROT_VARIANT = MinecraftHasher.fromIdEnum(ParrotVariant.values());

    MinecraftHasher<Integer> TROPICAL_FISH_PATTERN = MinecraftHasher.<TropicalFishPattern>fromEnum().cast(TropicalFishPattern::fromPackedId);

    MinecraftHasher<Integer> MOOSHROOM_VARIANT = MinecraftHasher.fromIdEnum(MooshroomVariant.values());

    MinecraftHasher<Integer> RABBIT_VARIANT = MinecraftHasher.<RabbitVariant>fromEnum().cast(RabbitVariant::fromId);

    MinecraftHasher<Integer> HORSE_VARIANT = MinecraftHasher.fromIdEnum(HorseVariant.values());

    MinecraftHasher<Integer> LLAMA_VARIANT = MinecraftHasher.fromIdEnum(LlamaVariant.values());

    MinecraftHasher<Integer> AXOLOTL_VARIANT = MinecraftHasher.fromIdEnum(AxolotlVariant.values());

    // Widely used Minecraft types

    @SuppressWarnings({"unchecked", "rawtypes"}) // Java generics :(
    MinecraftHasher<DataComponent<?, ?>> DATA_COMPONENT_KEY = MinecraftHasher.either(KYORI_KEY,
        component -> component.getValue() == null ? null : component.getType().getKey(), KYORI_KEY_REMOVAL, component -> component.getType().getKey());

    @SuppressWarnings({"unchecked", "rawtypes"}) // Java generics :(
    MinecraftHasher<DataComponent<?, ?>> DATA_COMPONENT_VALUE = (component, encoder) -> {
        if (component.getValue() == null) {
            return UNIT.hash(Unit.INSTANCE, encoder);
        }
        MinecraftHasher hasher = DataComponentHashers.hasher(component.getType());
        return hasher.hash(component.getValue(), encoder);
    };

    MinecraftHasher<DataComponents> DATA_COMPONENTS = MinecraftHasher.mapSet(DATA_COMPONENT_KEY, DATA_COMPONENT_VALUE).cast(components -> components.getDataComponents().values());

    MinecraftHasher<ItemStack> ITEM_STACK = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", ITEM, ItemStack::getId)
        .accept("count", INT, ItemStack::getAmount)
        .optionalNullable("components", DATA_COMPONENTS, ItemStack::getDataComponents));

    // Encoding of hidden effects is unfortunately not possible
    MapBuilder<MobEffectDetails> MOB_EFFECT_DETAILS = builder -> builder
        .optional("amplifier", BYTE, instance -> (byte) instance.getAmplifier(), (byte) 0)
        .optional("duration", INT, MobEffectDetails::getDuration, 0)
        .optional("ambient", BOOL, MobEffectDetails::isAmbient, false)
        .optional("show_particles", BOOL, MobEffectDetails::isShowParticles, true)
        .accept("show_icon", BOOL, MobEffectDetails::isShowIcon); // Yes, this is not an optional. I checked. Maybe it will be in the future and break everything!

    MinecraftHasher<MobEffectInstance> MOB_EFFECT_INSTANCE = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", RegistryHasher.EFFECT, MobEffectInstance::getEffect)
        .accept(MOB_EFFECT_DETAILS, MobEffectInstance::getDetails));

    MinecraftHasher<ModifierOperation> ATTRIBUTE_MODIFIER_OPERATION = MinecraftHasher.fromEnum(operation -> switch (operation) {
        case ADD -> "add_value";
        case ADD_MULTIPLIED_BASE -> "add_multiplied_base";
        case ADD_MULTIPLIED_TOTAL -> "add_multiplied_total";
    });

    // Component-specific types

    MinecraftHasher<ItemEnchantments> ITEM_ENCHANTMENTS = MinecraftHasher.map(RegistryHasher.ENCHANTMENT, MinecraftHasher.INT).cast(ItemEnchantments::getEnchantments);

    MinecraftHasher<ItemContainerSlot> CONTAINER_SLOT = MinecraftHasher.mapBuilder(builder -> builder
        .accept("slot", INT, ItemContainerSlot::index)
        .accept("item", ITEM_STACK, ItemContainerSlot::item));

    MinecraftHasher<List<ItemStack>> ITEM_CONTAINER_CONTENTS = CONTAINER_SLOT.list().cast(stacks -> {
        List<ItemContainerSlot> slots = new ArrayList<>();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack != null) {
                slots.add(new ItemContainerSlot(i, stacks.get(i)));
            }
        }
        return slots;
    });

    MinecraftHasher<AdventureModePredicate.BlockPredicate> BLOCK_PREDICATE = MinecraftHasher.mapBuilder(builder -> builder
        .optionalNullable("blocks", BLOCK.holderSet(), AdventureModePredicate.BlockPredicate::getBlocks)
        .optionalNullable("nbt", MNBT, AdventureModePredicate.BlockPredicate::getNbt)); // Property and data component matchers are, unfortunately, too complicated to include here

    // Encode as a single element if the list only has one element
    MinecraftHasher<AdventureModePredicate> ADVENTURE_MODE_PREDICATE = MinecraftHasher.either(BLOCK_PREDICATE,
                                                                                              predicate -> predicate.getPredicates().size() == 1 ? predicate.getPredicates().get(0) : null, BLOCK_PREDICATE.list(), AdventureModePredicate::getPredicates);

    MinecraftHasher<ItemAttributeModifiers.Entry> ATTRIBUTE_MODIFIER_ENTRY = MinecraftHasher.mapBuilder(builder -> builder
        .accept("type", RegistryHasher.ATTRIBUTE, ItemAttributeModifiers.Entry::getAttribute)
        .accept("id", KEY, entry -> entry.getModifier().getId())
        .accept("amount", DOUBLE, entry -> entry.getModifier().getAmount())
        .accept("operation", ATTRIBUTE_MODIFIER_OPERATION, entry -> entry.getModifier().getOperation())
        .optional("slot", EQUIPMENT_SLOT_GROUP, ItemAttributeModifiers.Entry::getSlot, ItemAttributeModifiers.EquipmentSlotGroup.ANY));

    MinecraftHasher<Consumable.ItemUseAnimation> ITEM_USE_ANIMATION = MinecraftHasher.fromEnum();

    MinecraftHasher<ConsumeEffectType> CONSUME_EFFECT_TYPE = enumRegistry();

    MinecraftHasher<ConsumeEffect> CONSUME_EFFECT = CONSUME_EFFECT_TYPE.dispatch(ConsumeEffectType::fromEffect, type -> type.getBuilder().cast());

    MinecraftHasher<SuspiciousStewEffect> SUSPICIOUS_STEW_EFFECT = MinecraftHasher.mapBuilder(builder -> builder
        .accept("id", EFFECT_ID, SuspiciousStewEffect::getMobEffectId)
        .optional("duration", INT, SuspiciousStewEffect::getDuration, 160));

    MinecraftHasher<InstrumentComponent> INSTRUMENT_COMPONENT = MinecraftHasher.either(INSTRUMENT, InstrumentComponent::instrumentHolder, KEY, InstrumentComponent::instrumentLocation);

    MinecraftHasher<ToolData.Rule> TOOL_RULE = MinecraftHasher.mapBuilder(builder -> builder
        .accept("blocks", RegistryHasher.BLOCK.holderSet(), ToolData.Rule::getBlocks)
        .optionalNullable("speed", MinecraftHasher.FLOAT, ToolData.Rule::getSpeed)
        .optionalNullable("correct_for_drops", MinecraftHasher.BOOL, ToolData.Rule::getCorrectForDrops));

    RegistryHasher<?> DAMAGE_TYPE = enumIdRegistry(DamageTypes.values());

    MinecraftHasher<BlocksAttacks.DamageReduction> BLOCKS_ATTACKS_DAMAGE_REDUCTION = MinecraftHasher.mapBuilder(builder -> builder
        .optional("horizontal_blocking_angle", FLOAT, BlocksAttacks.DamageReduction::horizontalBlockingAngle, 90.0F)
        .optionalNullable("type", DAMAGE_TYPE.holderSet(), BlocksAttacks.DamageReduction::type)
        .accept("base", FLOAT, BlocksAttacks.DamageReduction::base)
        .accept("factor", FLOAT, BlocksAttacks.DamageReduction::factor));

    MinecraftHasher<BlocksAttacks.ItemDamageFunction> BLOCKS_ATTACKS_ITEM_DAMAGE_FUNCTION = MinecraftHasher.mapBuilder(builder -> builder
        .accept("threshold", FLOAT, BlocksAttacks.ItemDamageFunction::threshold)
        .accept("base", FLOAT, BlocksAttacks.ItemDamageFunction::base)
        .accept("factor", FLOAT, BlocksAttacks.ItemDamageFunction::factor));

    MinecraftHasher<ProvidesTrimMaterial> PROVIDES_TRIM_MATERIAL = MinecraftHasher.either(TRIM_MATERIAL, ProvidesTrimMaterial::materialHolder, KEY, ProvidesTrimMaterial::materialLocation);

    MinecraftHasher<ArmorTrim> ARMOR_TRIM = MinecraftHasher.mapBuilder(builder -> builder
        .accept("material", TRIM_MATERIAL, ArmorTrim::material)
        .accept("pattern", TRIM_PATTERN, ArmorTrim::pattern));

    MinecraftHasher<JukeboxPlayable> JUKEBOX_PLAYABLE = MinecraftHasher.either(JUKEBOX_SONG, JukeboxPlayable::songHolder, KEY, JukeboxPlayable::songLocation);

    MinecraftHasher<BannerPatternLayer> BANNER_PATTERN_LAYER = MinecraftHasher.mapBuilder(builder -> builder
        .accept("pattern", BANNER_PATTERN, BannerPatternLayer::getPattern)
        .accept("color", DYE_COLOR, BannerPatternLayer::getColorId));

    MinecraftHasher<Integer> FIREWORK_EXPLOSION_SHAPE = MinecraftHasher.fromIdEnum(FireworkExplosionShape.values());

    MinecraftHasher<Fireworks.FireworkExplosion> FIREWORK_EXPLOSION = MinecraftHasher.mapBuilder(builder -> builder
        .accept("shape", FIREWORK_EXPLOSION_SHAPE, Fireworks.FireworkExplosion::getShapeId)
        .optionalList("colors", INT, explosion -> IntStream.of(explosion.getColors()).boxed().toList())
        .optionalList("fade_colors", INT, explosion -> IntStream.of(explosion.getFadeColors()).boxed().toList())
        .optional("has_trail", BOOL, Fireworks.FireworkExplosion::isHasTrail, false)
        .optional("has_twinkle", BOOL, Fireworks.FireworkExplosion::isHasTwinkle, false));

    MinecraftHasher<BeehiveOccupant> BEEHIVE_OCCUPANT = MinecraftHasher.mapBuilder(builder -> builder
        .optional("entity_data", MNBT, BeehiveOccupant::getEntityData, MNBTIO.write(new CompoundTag(), false))
        .accept("ticks_in_hive", INT, BeehiveOccupant::getTicksInHive)
        .accept("min_ticks_in_hive", INT, BeehiveOccupant::getMinTicksInHive));


    /**
     * Creates a hasher that encodes registry IDs, and is also able to encode {@link Holder}s by using the {@code directHasher}.
     *
     * @param registry the registry to create a hasher for.
     */
    static RegistryHasher<?> registry(Registry<?> registry) {
        MinecraftHasher<Integer> hasher = KEY.cast(id -> registry.get(id).name());
        return hasher::hash;
    }

    /**
     * Creates a hasher that encodes registry IDs, and is also able to encode {@link Holder}s by using the {@code directHasher}.
     *
     * @param registry the registry to create a hasher for.
     */
    static RegistryHasher<?> dynamicRegistry(DynamicRegistry<?> registry) {
        MinecraftHasher<Integer> hasher = KEY.cast(id -> registry.get(id).name());
        return hasher::hash;
    }

    /**
     * Creates a hasher that using {@link RegistryHasher#registry(Registry)} that is also able to encode {@link Holder}s by using the {@code directHasher}.
     *
     * <p>A hasher that encodes {@link Holder}s can be obtained by using {@link RegistryHasher#holder()}</p>
     *
     * @param registry the registry to create a hasher for.
     * @param directHasher the hasher that encodes a custom object.
     * @param <DirectType> the type of custom objects.
     * @see RegistryHasher#holder()
     */
    // We don't use the registry generic type, because various registries don't use the MCPL type as their type
    static <DirectType> RegistryHasher<DirectType> registry(Registry<?> registry, MinecraftHasher<DirectType> directHasher) {
        return new RegistryHasherWithDirectHasher<>(registry(registry), directHasher);
    }

    /**
     * Creates a hasher that encodes a {@link Holder} of {@link DirectType}. If the holder has an ID, the {@link RegistryHasher} is used to encode it. If the holder is custom,
     * a direct hasher specified in {@link RegistryHasher#registry(Registry, MinecraftHasher)} is used to encode it.
     *
     * <p>This method can only be used if this hasher has a direct hasher attached to it. That is only the case if {@link DirectType} is not {@code ?}. If this hasher doesn't have
     * a direct hasher, a {@link IllegalStateException} will be thrown upon use.</p>
     *
     * @throws IllegalStateException when this hasher does not have a direct hasher attached to it.
     */
    default MinecraftHasher<Holder<DirectType>> holder() {
        if (this instanceof RegistryHasher.RegistryHasherWithDirectHasher<DirectType> withDirect) {
            return withDirect.holderHasher;
        }
        throw new IllegalStateException("Tried to create a holder hasher on a registry hasher that does not have a direct hasher specified");
    }

    static <T> MinecraftHasher<Holder<T>> holderIdOnly(MinecraftHasher<T> direct) {
        return (value, encoder) -> {
            if (value.isId()) {
                CLIENT_LOG.error("Holder type: {} is id: {} but no registry cached", value.getClass().getSimpleName(), value.id());
                return INT.hash(value.id(), encoder); // this is completely incorrect but idk lol
            }
            return direct.hash(value.custom(), encoder);
        };
    }

    /**
     * Creates a hasher that hashes a {@link HolderSet} of the registry. {@link HolderSet}s can encode as a hash-prefixed tag, a single namespaced ID, or a list of namespaced IDs.
     *
     * <p>The hasher throws a {@link IllegalStateException} if the holder set does not have a tag nor a list of IDs. This should never happen.</p>
     */
    default MinecraftHasher<HolderSet> holderSet() {
        return (holder, encoder) -> {
            if (holder.getLocation() != null) {
                return TAG.hash(holder.getLocation(), encoder);
            } else if (holder.getHolders() != null) {
                if (holder.getHolders().length == 1) {
                    return hash(holder.getHolders()[0], encoder);
                }
                return list().hash(Arrays.stream(holder.getHolders()).boxed().toList(), encoder);
            }
            throw new IllegalStateException("HolderSet must have either tag location or holders");
        };
    }

    /**
     * Creates a hasher that uses {@link Enum#name()} (lowercased) to create a key in the {@code minecraft} namespace, and then hashes it.
     *
     * <p>Please be aware that you are using literal enum constants as key paths here, meaning that if there is a typo in a constant, or a constant changes name, things
     * may break. Use cautiously.</p>
     *
     * @param <EnumConstant> the enum.
     */
    static <EnumConstant extends Enum<EnumConstant>> MinecraftHasher<EnumConstant> enumRegistry() {
        return KEY.cast(constant -> constant.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Uses {@link Enum#name()} (lowercased) to create a function that creates a {@link Key} from a {@link EnumConstant}, and uses this as {@code toKey}
     * function in {@link RegistryHasher#enumIdRegistry(Enum[], Function)}.
     *
     * <p>Please be aware that you are using literal enum constants as key paths here, meaning that if there is a typo in a constant, or a constant changes name, things
     * may break. Use cautiously.</p>
     *
     * @param values the array of {@link EnumConstant}s.
     * @param <EnumConstant> the enum.
     * @see RegistryHasher#enumIdRegistry(Enum[], Function)
     */
    static <EnumConstant extends Enum<EnumConstant>> RegistryHasher<?> enumIdRegistry(EnumConstant[] values) {
        return enumIdRegistry(values, constant -> constant.name().toLowerCase(Locale.ROOT));
    }

    /**
     * Creates a hasher that looks up a network ID in the array of {@link EnumConstant}s, and then uses {@code toKey} to turn the constant into a key, which it then hashes.
     *
     * @param values the array of {@link EnumConstant}s.
     * @param toKey the function that turns a {@link EnumConstant} into a {@link Key}.
     * @param <EnumConstant> the enum.
     * @see MinecraftHasher#fromIdEnum(Enum[])
     */
    static <EnumConstant extends Enum<EnumConstant>> RegistryHasher<?> enumIdRegistry(EnumConstant[] values, Function<EnumConstant, String> toKey) {
        MinecraftHasher<Integer> hasher = KEY.cast(i -> toKey.apply(values[i]));
        return hasher::hash;
    }

    class RegistryHasherWithDirectHasher<DirectType> implements RegistryHasher<DirectType> {
        private final MinecraftHasher<Integer> id;
        private final MinecraftHasher<Holder<DirectType>> holderHasher;

        public RegistryHasherWithDirectHasher(MinecraftHasher<Integer> id, MinecraftHasher<DirectType> direct) {
            this.id = id;
            this.holderHasher = (value, encoder) -> {
                if (value.isId()) {
                    return hash(value.id(), encoder);
                }
                return direct.hash(value.custom(), encoder);
            };
        }

        @Override
        public HashCode hash(Integer value, MinecraftHashEncoder encoder) {
            return id.hash(value, encoder);
        }
    }
}
