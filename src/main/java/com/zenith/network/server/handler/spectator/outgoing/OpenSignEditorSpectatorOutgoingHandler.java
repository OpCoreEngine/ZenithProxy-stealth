package com.zenith.network.server.handler.spectator.outgoing;

import com.zenith.api.network.PacketHandler;
import com.zenith.api.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.level.ClientboundOpenSignEditorPacket;

public class OpenSignEditorSpectatorOutgoingHandler implements PacketHandler<ClientboundOpenSignEditorPacket, ServerSession> {
    @Override
    public ClientboundOpenSignEditorPacket apply(final ClientboundOpenSignEditorPacket packet, final ServerSession session) {
        return null;
    }
}
