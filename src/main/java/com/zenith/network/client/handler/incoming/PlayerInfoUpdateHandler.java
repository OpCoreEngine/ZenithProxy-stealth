package com.zenith.network.client.handler.incoming;

import com.zenith.Proxy;
import com.zenith.api.event.server.ServerPlayerConnectedEvent;
import com.zenith.api.network.ClientEventLoopPacketHandler;
import com.zenith.api.network.client.ClientSession;
import com.zenith.util.Config;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

import static com.zenith.Globals.*;
import static org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntryAction.*;

public class PlayerInfoUpdateHandler implements ClientEventLoopPacketHandler<ClientboundPlayerInfoUpdatePacket, ClientSession> {
    @Override
    public boolean applyAsync(@NonNull ClientboundPlayerInfoUpdatePacket packet, @NonNull ClientSession session) {
        for (int i = 0; i < packet.getEntries().length; i++) {
            var entry = packet.getEntries()[i];
            if (packet.getActions().contains(ADD_PLAYER)) {
                CACHE.getTabListCache().add(entry);
                EVENT_BUS.postAsync(new ServerPlayerConnectedEvent(entry));
            }
            // skip extra ops if we're only adding a player
            if (packet.getActions().size() <= 1 && packet.getActions().contains(ADD_PLAYER)) continue;
            CACHE.getTabListCache().get(entry.getProfileId()).ifPresent(e -> {
                if (packet.getActions().contains(INITIALIZE_CHAT)) {
                    e.setSessionId(entry.getSessionId());
                    e.setExpiresAt(entry.getExpiresAt());
                    e.setKeySignature(entry.getKeySignature());
                    e.setPublicKey(entry.getPublicKey());
                }
                if (packet.getActions().contains(UPDATE_GAME_MODE))
                    e.setGameMode(entry.getGameMode());
                if (packet.getActions().contains(UPDATE_LISTED))
                    e.setListed(entry.isListed());
                if (packet.getActions().contains(UPDATE_LATENCY)) {
                    e.setLatency(entry.getLatency());
                    if (CONFIG.client.ping.mode == Config.Client.Ping.Mode.TABLIST
                        && !Proxy.getInstance().isOn2b2t()
                        && Objects.equals(e.getProfileId(), CACHE.getPlayerCache().getThePlayer().getUuid())) {
                        session.setPing(e.getLatency());
                    }
                }
                if (packet.getActions().contains(UPDATE_DISPLAY_NAME))
                    e.setDisplayName(entry.getDisplayName());
            });
        }
        return true;
    }
}
