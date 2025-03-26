package com.zenith.mc.item.hashing;

import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.component.DataComponentType;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

// todo: impl
public class ItemStackHasher {

    public HashedStack hash(ItemStack stack) {
        Map<DataComponentType<?>, Integer> addedComponents;
        Set<DataComponentType<?>> removedComponents;
        if (stack.getDataComponents() == null) {
            addedComponents = Collections.emptyMap();
            removedComponents = Collections.emptySet();
        }
//        return new HashedStack(stack.getId(), stack.getAmount(), stack.getAddedComponents(), stack.getRemovedComponents());
        return null;
    }
}
