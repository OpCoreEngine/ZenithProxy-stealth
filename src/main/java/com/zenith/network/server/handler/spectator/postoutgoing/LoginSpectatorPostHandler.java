package com.zenith.network.server.handler.spectator.postoutgoing;

import com.zenith.Proxy;
import com.zenith.event.proxy.ProxySpectatorLoggedInEvent;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.network.registry.PostOutgoingPacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntryAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import org.jspecify.annotations.NonNull;

import java.util.EnumSet;

import static com.zenith.Globals.*;
import static org.geysermc.mcprotocollib.protocol.data.game.entity.player.GameMode.SPECTATOR;

public class LoginSpectatorPostHandler implements PostOutgoingPacketHandler<ClientboundLoginPacket, ServerSession> {
    @Override
    public void accept(@NonNull ClientboundLoginPacket packet, @NonNull ServerSession session) {
        if (!session.isWhitelistChecked()) {
            // we shouldn't be able to get to this point without whitelist checking, but just in case
            session.disconnect("Login without whitelist check?");
            return;
        }
        session.sendAsync(new ClientboundPlayerInfoUpdatePacket(
            EnumSet.of(PlayerListEntryAction.ADD_PLAYER, PlayerListEntryAction.UPDATE_LISTED, PlayerListEntryAction.UPDATE_GAME_MODE),
            new PlayerListEntry[]{new PlayerListEntry(
                session.getSpectatorFakeProfileCache().getProfile().getId(),
                session.getSpectatorFakeProfileCache().getProfile(),
                true,
                0,
                SPECTATOR,
                null,
                null,
                0,
                null,
                null
            )}
        ));
        EVENT_BUS.postAsync(new ProxySpectatorLoggedInEvent(session));
        SpectatorSync.initSpectator(session, () -> CACHE.getAllDataSpectator(session.getSpectatorPlayerCache()));
        //send cached data
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            if (connection.equals(session)) continue;
            connection.sendAsync(new ClientboundSystemChatPacket(
                ComponentSerializer.minimessage("<green>" + session.getProfileCache().getProfile().getName() + " connected!"), false
            ));
            if (connection.equals(Proxy.getInstance().getCurrentPlayer().get())) {
                connection.sendAsync(new ClientboundSystemChatPacket(
                    ComponentSerializer.minimessage("<blue>Send private messages: \"!m \\<message>\""), false
                ));
            }
        }
        session.setLoggedIn();
        ServerSession currentPlayer = Proxy.getInstance().getCurrentPlayer().get();
        if (currentPlayer != null) currentPlayer.syncTeamMembers();
        SpectatorSync.syncPlayerEquipmentWithSpectatorsFromCache();
        if (CONFIG.server.spectator.playerCamOnJoin) {
            session.setCameraTarget(CACHE.getPlayerCache().getThePlayer());
            session.sendAsync(new ClientboundSetCameraPacket(CACHE.getPlayerCache().getEntityId()));
            var sessions = Proxy.getInstance().getActiveConnections().getArray();
            for (int i = 0; i < sessions.length; i++) {
                var connection = sessions[i];
                connection.sendAsync(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
            }
        }
        // send command help
        session.sendAsyncAlert("<green>Spectating <red>" + CACHE.getProfileCache().getProfile().getName());
        if (CONFIG.inGameCommands.enable) {
            session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<green>Command Prefix : \"" + CONFIG.inGameCommands.prefix + "\""), false));
            session.sendAsync(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>help <gray>- <dark_gray>List Commands"), false));
        }
    }
}
