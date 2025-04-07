package com.zenith.network.client.handler.incoming.scoreboard;

import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetDisplayObjectivePacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;

public class SetDisplayObjectiveHandler implements ClientEventLoopPacketHandler<ClientboundSetDisplayObjectivePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetDisplayObjectivePacket packet, @NonNull ClientSession session) {
        CACHE.getScoreboardCache().setPositionObjective(packet.getPosition(), packet.getName());
        return true;
    }
}
