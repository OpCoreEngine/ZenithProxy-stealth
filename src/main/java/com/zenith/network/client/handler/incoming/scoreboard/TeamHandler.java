package com.zenith.network.client.handler.incoming.scoreboard;

import com.zenith.network.client.ClientSession;
import com.zenith.network.codec.ClientEventLoopPacketHandler;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.scoreboard.ClientboundSetPlayerTeamPacket;
import org.jspecify.annotations.NonNull;

import java.util.Collections;

import static com.zenith.Globals.CACHE;

public class TeamHandler implements ClientEventLoopPacketHandler<ClientboundSetPlayerTeamPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundSetPlayerTeamPacket packet, @NonNull ClientSession session) {
        switch (packet.getAction()) {
            case CREATE -> CACHE.getTeamCache().add(packet);
            case REMOVE -> CACHE.getTeamCache().remove(packet);
            case UPDATE -> {
                var team = CACHE.getTeamCache().get(packet);
                if (team == null) return false;
                team.setDisplayName(packet.getDisplayName())
                    .setPrefix(packet.getPrefix())
                    .setSuffix(packet.getSuffix())
                    .setFriendlyFire(packet.isFriendlyFire())
                    .setSeeFriendlyInvisibles(packet.isSeeFriendlyInvisibles())
                    .setNameTagVisibility(packet.getNameTagVisibility())
                    .setCollisionRule(packet.getCollisionRule())
                    .setColor(packet.getColor());
            }
            case ADD_PLAYER -> {
                var team = CACHE.getTeamCache().get(packet);
                if (team == null) return false;
                Collections.addAll(team.getPlayers(), packet.getPlayers());
            }
            case REMOVE_PLAYER -> {
                var team = CACHE.getTeamCache().get(packet);
                if (team == null) return false;
                var players = team.getPlayers();
                for (String p : packet.getPlayers()) players.remove(p);
            }
        }
        return true;
    }
}
