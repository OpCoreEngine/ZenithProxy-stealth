package com.zenith.network.client.handler.incoming.scoreboard;

import com.zenith.api.network.ClientEventLoopPacketHandler;
import com.zenith.api.network.client.ClientSession;
import com.zenith.cache.data.scoreboard.Objective;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetObjectivePacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;

public class SetObjectiveHandler implements ClientEventLoopPacketHandler<ClientboundSetObjectivePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetObjectivePacket packet, @NonNull ClientSession session) {
        switch (packet.getAction()) {
            case ADD -> CACHE.getScoreboardCache().add(packet);
            case REMOVE -> CACHE.getScoreboardCache().remove(packet);
            case UPDATE -> {
                final Objective objective = CACHE.getScoreboardCache().get(packet.getName());
                if (objective == null) {
                    return false;
                }
                objective
                    .setDisplayName(packet.getDisplayName())
                    .setScoreType(packet.getType())
                    .setNumberFormat(packet.getNumberFormat());
            }
        }

        return true;
    }
}
