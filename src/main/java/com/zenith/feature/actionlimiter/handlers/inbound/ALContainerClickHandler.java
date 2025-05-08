package com.zenith.feature.actionlimiter.handlers.inbound;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundContainerClickPacket;

import static com.zenith.Globals.CONFIG;

public class ALContainerClickHandler implements PacketHandler<ServerboundContainerClickPacket, ServerSession> {
    @Override
    public ServerboundContainerClickPacket apply(final ServerboundContainerClickPacket packet, final ServerSession session) {
        if (!CONFIG.client.extra.actionLimiter.allowInventory) return null;
        return packet;
    }
}
