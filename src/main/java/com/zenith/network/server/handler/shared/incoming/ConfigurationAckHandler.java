package com.zenith.network.server.handler.shared.incoming;

import com.zenith.api.network.PacketHandler;
import com.zenith.api.network.server.ServerSession;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundConfigurationAcknowledgedPacket;

public class ConfigurationAckHandler implements PacketHandler<ServerboundConfigurationAcknowledgedPacket, ServerSession> {
    @Override
    public ServerboundConfigurationAcknowledgedPacket apply(final ServerboundConfigurationAcknowledgedPacket packet, final ServerSession session) {
        session.switchInboundState(ProtocolState.CONFIGURATION);
        return packet;
    }
}
