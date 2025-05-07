package com.zenith.network.client.handler.incoming.scoreboard;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundResetScorePacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;

public class ResetScoreHandler implements ClientEventLoopPacketHandler<ClientboundResetScorePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundResetScorePacket packet, @NonNull ClientSession session) {
        if (packet.getObjective() == null) {
            // Reset from all objectives
            CACHE.getScoreboardCache().removeEntry(packet.getOwner());
        } else {
            var objective = CACHE.getScoreboardCache().get(packet.getObjective());
            if (objective != null) {
                objective.getScores().remove(packet.getOwner());
            }
        }
        return true;
    }
}
