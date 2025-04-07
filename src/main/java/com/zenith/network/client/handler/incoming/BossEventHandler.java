package com.zenith.network.client.handler.incoming;

import com.zenith.api.network.ClientEventLoopPacketHandler;
import com.zenith.api.network.client.ClientSession;
import org.geysermc.mcprotocollib.protocol.data.game.BossBarAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundBossEventPacket;
import org.jspecify.annotations.NonNull;

import static com.zenith.Globals.CACHE;

public class BossEventHandler implements ClientEventLoopPacketHandler<ClientboundBossEventPacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundBossEventPacket packet, @NonNull ClientSession session) {
        if (packet.getAction() == BossBarAction.ADD) {
            CACHE.getBossBarCache().add(packet);
            return true;
        }
        var bossBar = CACHE.getBossBarCache().get(packet);
        if (bossBar == null) return false;
        switch (packet.getAction()) {
            case REMOVE -> CACHE.getBossBarCache().remove(packet);
            case UPDATE_HEALTH -> bossBar.setHealth(packet.getHealth());
            case UPDATE_TITLE -> bossBar.setTitle(packet.getTitle());
            case UPDATE_STYLE -> bossBar.setColor(packet.getColor()).setDivision(packet.getDivision());
            case UPDATE_FLAGS -> bossBar.setDarkenSky(packet.isDarkenSky()).setPlayEndMusic(packet.isPlayEndMusic());
        }
        return true;
    }
}
