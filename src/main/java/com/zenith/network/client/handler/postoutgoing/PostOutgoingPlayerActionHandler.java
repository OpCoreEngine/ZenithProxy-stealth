package com.zenith.network.client.handler.postoutgoing;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.player.ServerboundPlayerActionPacket;

import static com.zenith.Globals.CACHE;
import static com.zenith.feature.spectator.SpectatorSync.syncPlayerEquipmentWithSpectatorsFromCache;

public class PostOutgoingPlayerActionHandler implements ClientEventLoopPacketHandler<ServerboundPlayerActionPacket, ClientSession> {
    @Override
    public boolean applyAsync(final ServerboundPlayerActionPacket packet, final ClientSession session) {
        switch (packet.getAction()) {
            case DROP_ITEM -> {
                var heldItemSlot = CACHE.getPlayerCache().getHeldItemSlot();
                var invIndex = heldItemSlot + 36;
                var itemStack = CACHE.getPlayerCache().getPlayerInventory().get(invIndex);
                if (itemStack == null) return true;
                itemStack.setAmount(itemStack.getAmount() - 1);
                if (itemStack.getAmount() <= 0)
                    CACHE.getPlayerCache().getPlayerInventory().set(invIndex, null);
                syncPlayerEquipmentWithSpectatorsFromCache();
            }
            case DROP_ALL_ITEMS -> {
                var heldItemSlot = CACHE.getPlayerCache().getHeldItemSlot();
                var invIndex = heldItemSlot + 36;
                CACHE.getPlayerCache().getPlayerInventory().set(invIndex, null);
                syncPlayerEquipmentWithSpectatorsFromCache();
            }
            case SWAP_ITEM_WITH_OFFHAND -> { // this seems to trigger the server to send an inventory update but let's just do it anyway
                var invIndex = CACHE.getPlayerCache().getHeldItemSlot() + 36;
                var offHandIndex = 45;
                var offHand = CACHE.getPlayerCache().getPlayerInventory().get(offHandIndex);
                var mainHand = CACHE.getPlayerCache().getPlayerInventory().get(invIndex);
                CACHE.getPlayerCache().getPlayerInventory().set(offHandIndex, mainHand);
                CACHE.getPlayerCache().getPlayerInventory().set(invIndex, offHand);
                syncPlayerEquipmentWithSpectatorsFromCache();
            }
        }
        return true;
    }
}
