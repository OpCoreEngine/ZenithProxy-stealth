package com.zenith.network.server.handler.shared.incoming;

import com.zenith.api.network.PacketHandler;
import com.zenith.api.network.server.ServerSession;
import com.zenith.network.UserAuthTask;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundHelloPacket;
import org.geysermc.mcprotocollib.protocol.packet.login.serverbound.ServerboundHelloPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.EXECUTOR;

public class SHelloHandler implements PacketHandler<ServerboundHelloPacket, ServerSession> {
    @Override
    public ServerboundHelloPacket apply(@NonNull ServerboundHelloPacket packet, @NonNull ServerSession session) {
        session.setUsername(packet.getUsername());
        session.setLoginProfileUUID(packet.getProfileId());
        if (session.isTransferring())
            // TODO: see how viaversion interacts with this sequence
            //  it seems to be legal for clients to not send a response to the cookie request, at which point we stall
            //  in this login sequence forever
            session.getCookieCache().getPackets(session::sendAsync, session);
        else {
            if (CONFIG.server.verifyUsers)
                session.sendAsync(new ClientboundHelloPacket(session.getServerId(), session.getKeyPair().getPublic(), session.getChallenge(), true));
            else
                EXECUTOR.execute(new UserAuthTask(session, null));
        }
        return null;
    }
}
