package com.zenith.mc.item.hashing;

import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;

import java.util.*;

public class ItemStackHasher {

    public static HashedStack hash(ItemStack stack) {
        Map<DataComponentType<?>, Integer> addedComponents;
        Set<DataComponentType<?>> removedComponents;
        if (stack.getDataComponents() == null) {
            addedComponents = Collections.emptyMap();
            removedComponents = Collections.emptySet();
        } else {
            addedComponents = new HashMap<>();
            removedComponents = new HashSet<>();
            for (var entry: stack.getDataComponents().getDataComponents().entrySet()) {
                if (entry.getValue().getValue() == null) {
                    removedComponents.add(entry.getKey());
                } else {
                    addedComponents.put(entry.getKey(), DataComponentHashers.hash((DataComponentType) entry.getKey(), entry.getValue().getValue()).asInt());
                }
            }
        }
        return new HashedStack(stack.getId(), stack.getAmount(), addedComponents, removedComponents);
    }

    public static boolean matches(HashedStack hashedStack, ItemStack stack) {
        if (hashedStack.id() != stack.getId()) return false;
        if (hashedStack.count() != stack.getAmount()) return false;
        var stackDataComponents = stack.getDataComponents();
        if (stackDataComponents == null) {
            if (!hashedStack.addedComponents().isEmpty()) return false;
            if (!hashedStack.removedComponents().isEmpty()) return false;
            return true;
        }
        return false;
    }
}
