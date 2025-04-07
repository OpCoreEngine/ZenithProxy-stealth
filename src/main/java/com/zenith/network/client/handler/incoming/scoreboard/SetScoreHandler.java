package com.zenith.network.client.handler.incoming.scoreboard;

import com.zenith.cache.data.scoreboard.Score;
import com.zenith.network.client.ClientSession;
import com.zenith.network.registry.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetScorePacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;

public class SetScoreHandler implements ClientEventLoopPacketHandler<ClientboundSetScorePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetScorePacket packet, @NonNull ClientSession session) {
        var objective = CACHE.getScoreboardCache().get(packet.getObjective());
        if (objective == null) return false;

        objective.getScores().put(packet.getOwner(), new Score(packet));
        return true;
    }
}
