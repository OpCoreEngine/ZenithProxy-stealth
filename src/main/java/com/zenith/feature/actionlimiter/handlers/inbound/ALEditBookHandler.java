package com.zenith.feature.actionlimiter.handlers.inbound;

import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.inventory.ServerboundEditBookPacket;

import static com.zenith.Globals.CONFIG;

public class ALEditBookHandler implements PacketHandler<ServerboundEditBookPacket, ServerSession> {
    @Override
    public ServerboundEditBookPacket apply(final ServerboundEditBookPacket packet, final ServerSession session) {
        if(!CONFIG.client.extra.actionLimiter.allowBookSigning
            // if title is not null, the book is being signed
            && packet.getTitle() != null) {
            return new ServerboundEditBookPacket(
                packet.getSlot(),
                packet.getPages(),
                null // edits the book but does not sign it
            );
        }
        return packet;
    }
}
