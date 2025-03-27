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

import com.google.common.base.Suppliers;
import com.google.common.hash.HashCode;
import com.viaversion.nbt.mini.MNBT;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.zenith.mc.item.Rarity;
import net.kyori.adventure.key.Key;
import org.cloudburstmc.math.vector.Vector3i;
import org.geysermc.mcprotocollib.protocol.data.game.entity.EquipmentSlot;
import org.geysermc.mcprotocollib.protocol.data.game.entity.metadata.GlobalPos;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Consumable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Filterable;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.Unit;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("UnstableApiUsage")
@FunctionalInterface
public interface MinecraftHasher<T> {

    MinecraftHasher<Unit> UNIT = (unit, encoder) -> encoder.emptyMap();

    MinecraftHasher<Byte> BYTE = (b, encoder) -> encoder.number(b);

    MinecraftHasher<Short> SHORT = (s, encoder) -> encoder.number(s);

    MinecraftHasher<Integer> INT = (i, encoder) -> encoder.number(i);

    MinecraftHasher<Long> LONG = (l, encoder) -> encoder.number(l);

    MinecraftHasher<Float> FLOAT = (f, encoder) -> encoder.number(f);

    MinecraftHasher<Double> DOUBLE = (d, encoder) -> encoder.number(d);

    MinecraftHasher<String> STRING = (s, encoder) -> encoder.string(s);

    MinecraftHasher<Boolean> BOOL = (b, encoder) -> encoder.bool(b);

    MinecraftHasher<IntStream> INT_ARRAY = (ints, encoder) -> encoder.intArray(ints.toArray());

    MinecraftHasher<CompoundTag> NBT_MAP = (map, encoder) -> encoder.nbtMap(map);

    MinecraftHasher<ListTag> NBT_LIST = (list, encoder) -> encoder.nbtList(list);

    MinecraftHasher<Vector3i> POS = INT_ARRAY.convert(pos -> IntStream.of(pos.getX(), pos.getY(), pos.getZ()));

    MinecraftHasher<MNBT> MNBT = (mnbt, encoder) -> encoder.mnbt(mnbt);

    MinecraftHasher<String> KEY = STRING.convert((s) -> "minecraft:" + s);

    MinecraftHasher<Key> KYORI_KEY = STRING.convert(Key::asString);

    MinecraftHasher<String> TAG = STRING.convert(key -> "#minecraft:" + key);

    MinecraftHasher<UUID> UUID = INT_ARRAY.convert(uuid -> {
        long mostSignificant = uuid.getMostSignificantBits();
        long leastSignificant = uuid.getLeastSignificantBits();
        return IntStream.of((int) (mostSignificant >> 32), (int) mostSignificant, (int) (leastSignificant >> 32), (int) leastSignificant);
    }); // TODO test

    MinecraftHasher<Integer> RARITY = fromIdEnum(Rarity.values(), Rarity::getName);

    MinecraftHasher<Consumable.ItemUseAnimation> ITEM_USE_ANIMATION = fromEnum();

    MinecraftHasher<EquipmentSlot> EQUIPMENT_SLOT = fromEnum(); // FIXME MCPL enum constants aren't right

    MinecraftHasher<GlobalPos> GLOBAL_POS = mapBuilder(builder -> builder
        .accept("dimension", KYORI_KEY, GlobalPos::getDimension)
        .accept("pos", POS, p -> Vector3i.from(p.getX(), p.getY(), p.getZ())));


    HashCode hash(T value, MinecraftHashEncoder encoder);

    default MinecraftHasher<List<T>> list() {
        return (list, encoder) -> encoder.list(list.stream().map(element -> hash(element, encoder)).toList());
    }

    default MinecraftHasher<Filterable<T>> filterable() {
        return mapBuilder(builder -> builder
            .accept("raw", this, Filterable::getRaw)
            .optionalNullable("filtered", this, Filterable::getOptional));
    }

    default <D> MinecraftHasher<D> dispatch(String typeKey, Function<D, T> typeExtractor, Function<T, MapBuilder<D>> hashDispatch) {
        return mapBuilder(builder -> builder
            .accept(typeKey, this, typeExtractor)
            .accept(hashDispatch, typeExtractor));
    }

    default <C> MinecraftHasher<C> convert(Function<C, T> converter) {
        return (value, encoder) -> hash(converter.apply(value), encoder);
    }

    static <T> MinecraftHasher<T> lazyInitialize(Supplier<MinecraftHasher<T>> hasher) {
        Supplier<MinecraftHasher<T>> memoized = Suppliers.memoize(hasher::get);
        return (value, encoder) -> memoized.get().hash(value, encoder);
    }

    static <T> MinecraftHasher<T> recursive(UnaryOperator<MinecraftHasher<T>> delegate) {
        return new Recursive<>(delegate);
    }

    static <T extends Enum<T>> MinecraftHasher<Integer> fromIdEnum(T[] values, Function<T, String> toName) {
        return STRING.convert(id -> toName.apply(values[id]));
    }

    // TODO: note that this only works correctly if enum constants are named appropriately
    static <T extends Enum<T>> MinecraftHasher<T> fromEnum() {
        return STRING.convert(t -> t.name().toLowerCase());
    }

    static <T> MinecraftHasher<T> mapBuilder(MapBuilder<T> builder) {
        return (value, encoder) -> builder.apply(new MapHasher<>(value, encoder)).build();
    }

    static <K, V> MinecraftHasher<Map<K, V>> map(MinecraftHasher<K> keyHasher, MinecraftHasher<V> valueHasher) {
        return (map, encoder) -> encoder.map(map.entrySet().stream()
            .map(entry -> Map.entry(keyHasher.hash(entry.getKey(), encoder), valueHasher.hash(entry.getValue(), encoder)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    static <T, F, S> MinecraftHasher<T> either(MinecraftHasher<F> firstHasher, Function<T, F> firstExtractor, MinecraftHasher<S> secondHasher, Function<T, S> secondExtractor) {
        return (value, encoder) -> {
            F first = firstExtractor.apply(value);
            if (first != null) {
                return firstHasher.hash(first, encoder);
            }
            return secondHasher.hash(secondExtractor.apply(value), encoder);
        };
    }

    static <T> MinecraftHasher<T> dispatch(Function<T, MinecraftHasher<T>> hashDispatch) {
        return (value, encoder) -> hashDispatch.apply(value).hash(value, encoder);
    }

    class Recursive<T> implements MinecraftHasher<T> {
        private final Supplier<MinecraftHasher<T>> delegate;

        public Recursive(UnaryOperator<MinecraftHasher<T>> delegate) {
            this.delegate = Suppliers.memoize(() -> delegate.apply(this));
        }

        @Override
        public HashCode hash(T value, MinecraftHashEncoder encoder) {
            return delegate.get().hash(value, encoder);
        }
    }
}
