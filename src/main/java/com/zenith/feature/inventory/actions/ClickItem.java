package com.zenith.feature.inventory.actions;

import com.zenith.cache.data.inventory.Container;
import com.zenith.mc.item.ItemRegistry;
import com.zenith.mc.item.hashing.ItemStackHasher;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ClickItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.jspecify.annotations.Nullable;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CLIENT_LOG;

@Data
@RequiredArgsConstructor
public class ClickItem implements InventoryAction {
    private final int containerId;
    private final int slotId;
    private final ClickItemAction clickItemAction;
    private static final ContainerActionType containerActionType = ContainerActionType.CLICK_ITEM;

    public ClickItem(final int slotId, final ClickItemAction clickItemAction) {
        this(0, slotId, clickItemAction);
    }

    @Override
    public MinecraftPacket packet() {
        var container = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();
        var mouseStack = CACHE.getPlayerCache().getInventoryCache().getMouseStack();
        ItemStack predictedMouseStack = Container.EMPTY_STACK;
        final ItemStack clickStack = container.getItemStack(slotId);
        if (isStackEmpty(mouseStack) && isStackEmpty(clickStack)) {
            CLIENT_LOG.debug("Both mouse stack and click stack empty: {}", this);
            return null;
        }
        final Int2ObjectMap<@Nullable HashedStack> changedSlots = new Int2ObjectArrayMap<>();

        switch (clickItemAction) {
            case LEFT_CLICK -> {
                // swap the mouse stack with the item in slotId
                predictedMouseStack = clickStack;
                changedSlots.put(slotId, ItemStackHasher.hash(mouseStack));
            }
            case RIGHT_CLICK -> {
                // if mouse stack is empty, pick up half the clickStack
                if (isStackEmpty(mouseStack)) {
                    // round up to the nearest half stack
                    final int halfStackSize = (int) Math.ceil(clickStack.getAmount() / 2.0);
                    predictedMouseStack = new ItemStack(clickStack.getId(), halfStackSize, clickStack.getDataComponents());
                    changedSlots.put(slotId, ItemStackHasher.hash(new ItemStack(clickStack.getId(), clickStack.getAmount() - halfStackSize, clickStack.getDataComponents())));
                } else {
                    if (clickStack == Container.EMPTY_STACK) {
                        // place one item from mouse stack into click stack
                        if (mouseStack.getAmount() == 1) {
                            predictedMouseStack = Container.EMPTY_STACK;
                            changedSlots.put(slotId, ItemStackHasher.hash(new ItemStack(mouseStack.getId(), mouseStack.getAmount(), mouseStack.getDataComponents())));
                        } else {
                            var newMouseStackAmount = mouseStack.getAmount() - 1;
                            predictedMouseStack = newMouseStackAmount == 0
                                ? Container.EMPTY_STACK
                                : new ItemStack(mouseStack.getId(), mouseStack.getAmount() - 1, mouseStack.getDataComponents());
                            changedSlots.put(slotId, ItemStackHasher.hash(new ItemStack(mouseStack.getId(), mouseStack.getAmount() - 1, mouseStack.getDataComponents())));
                        }
                    } else {
                        // if both stacks are the same item, place one item from the mouse stack into clickStack
                        //   if clickStack is full, return null
                        if (mouseStack.getId() == clickStack.getId()) {
                            if (clickStack.getAmount() == ItemRegistry.REGISTRY.get(clickStack.getId()).stackSize()) return null;
                            var newMouseStackAmount = mouseStack.getAmount() - 1;
                            predictedMouseStack = newMouseStackAmount == 0
                                ? Container.EMPTY_STACK
                                : new ItemStack(mouseStack.getId(), mouseStack.getAmount() - 1, mouseStack.getDataComponents());
                            changedSlots.put(slotId, ItemStackHasher.hash(new ItemStack(clickStack.getId(), clickStack.getAmount() + 1, clickStack.getDataComponents())));
                        } else {
                            // if stacks are different, swap them
                            predictedMouseStack = clickStack;
                        changedSlots.put(slotId, ItemStackHasher.hash(mouseStack));
                        }
                    }
                }
            }
        }
        return new ServerboundContainerClickPacket(
            containerId,
            CACHE.getPlayerCache().getActionId().incrementAndGet(),
            slotId,
            containerActionType,
            clickItemAction,
            ItemStackHasher.hash(predictedMouseStack),
            changedSlots
        );
    }

    @Override
    public int containerId() {
        return containerId;
    }
}
