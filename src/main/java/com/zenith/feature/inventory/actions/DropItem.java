package com.zenith.feature.inventory.actions;

import com.zenith.cache.data.inventory.Container;
import com.zenith.mc.item.hashing.ItemStackHasher;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.geysermc.mcprotocollib.protocol.codec.MinecraftPacket;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.ContainerActionType;
import org.geysermc.mcprotocollib.protocol.data.game.inventory.DropItemAction;
import org.geysermc.mcprotocollib.protocol.data.game.item.HashedStack;
import org.geysermc.mcprotocollib.protocol.data.game.item.ItemStack;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;
import org.jspecify.annotations.Nullable;

import static com.zenith.Globals.CACHE;
import static com.zenith.Globals.CLIENT_LOG;

@Data
@RequiredArgsConstructor
public class DropItem implements InventoryAction {
    private final int containerId;
    private final int slotId;
    private final ContainerActionType actionType = ContainerActionType.DROP_ITEM;
    private final DropItemAction dropItemAction;

    public DropItem(final int slotId, final DropItemAction dropItemAction) {
        this(0, slotId, dropItemAction);
    }

    @Override
    public MinecraftPacket packet() {
        var container = CACHE.getPlayerCache().getInventoryCache().getOpenContainer();
        var mouseStack = CACHE.getPlayerCache().getInventoryCache().getMouseStack();
        if (!isStackEmpty(mouseStack)) {
            CLIENT_LOG.debug("[{}, {}, {}] Can't drop as mouse stack not empty", slotId, actionType, dropItemAction);
            return null; // can't drop if mouse stack is not empty
        }
        final ItemStack clickStack = container.getItemStack(slotId);
        if (isStackEmpty(clickStack)) {
            CLIENT_LOG.debug("[{}, {}, {}] Can't drop empty click stack", slotId, actionType, dropItemAction);
            return null; // can't drop if clickStack is empty
        }
        final Int2ObjectMap<@Nullable HashedStack> changedSlots = new Int2ObjectArrayMap<>();

        switch (dropItemAction) {
            case DROP_FROM_SELECTED -> // drop 1 item from the selected slot
                changedSlots.put(
                    slotId,
                    ItemStackHasher.hash(
                        clickStack.getAmount() == 1
                            ? Container.EMPTY_STACK
                            : new ItemStack(clickStack.getId(), clickStack.getAmount() - 1, clickStack.getDataComponents())));
            case DROP_SELECTED_STACK -> // drop the entire stack from the selected slot
                changedSlots.put(slotId, ItemStackHasher.hash(Container.EMPTY_STACK));
            default -> {
                CLIENT_LOG.debug("[{}, {}, {}] Unhandled drop item action", slotId, actionType, dropItemAction);
                return null;
            }
        }
        return new ServerboundContainerClickPacket(
            containerId,
            CACHE.getPlayerCache().getActionId().incrementAndGet(),
            slotId,
            actionType,
            dropItemAction,
            ItemStackHasher.hash(Container.EMPTY_STACK),
            changedSlots
        );
    }

    @Override
    public int containerId() {
        return containerId;
    }
}
