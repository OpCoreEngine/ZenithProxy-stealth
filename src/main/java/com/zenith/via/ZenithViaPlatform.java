package com.zenith.via;

import com.viaversion.vialoader.impl.platform.ViaVersionPlatformImpl;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.configuration.AbstractViaConfig;
import com.zenith.Proxy;
import com.zenith.network.server.ServerSession;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

public class ZenithViaPlatform extends ViaVersionPlatformImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger("ViaVersion");
    public ZenithViaPlatform() {
        super(null);
    }

    @Override
    protected AbstractViaConfig createConfig() {
        var config = new ZenithViaConfig(new File(getDataFolder(), "viaversion.yml"));
        config.reload();
        return config;
    }

    @Override
    public boolean kickPlayer(UserConnection connection, String s) {
        // the UUID does not match the logged in player's UUID
        // viaversion sets it to the UUID we sent in the GameProfile packet, which is the proxy's UUID instead of the connecting player's
        // or for spectators, we send the same UUID for each of them. so im not sure if this will work correctly at all
        var serverConnection = getServerConnection(connection);
        if (serverConnection.isPresent() && !serverConnection.get().isSpectator()) {
            LOGGER.warn("Kicking player {} with reason: {}", serverConnection.get().getLoginProfileUUID(), s);
            serverConnection.get().disconnect(s);
            return true;
        } else {
            LOGGER.warn("Kicking player with reason: {}", s);
            return false; // via will still kick them by closing the tcp connection
        }
    }

    @Override
    public void sendMessage(UserConnection connection, String msg) {
        var serverConnection = getServerConnection(connection);
        if (serverConnection.isPresent()) {
            LOGGER.info("Sending message: {} to player: {}", msg, serverConnection.get().getLoginProfileUUID());
            serverConnection.get().send(new ClientboundSystemChatPacket(Component.text(msg), false));
        } else {
            LOGGER.warn("Failed to send message: {}", msg);
        }
    }

    private Optional<ServerSession> getServerConnection(final UUID viaUuid) {
        if (viaUuid == null) return Optional.empty();
        UserConnection connectedClient = Via.getManager().getConnectionManager().getConnectedClient(viaUuid);
        if (connectedClient == null) return Optional.empty();
        var channel = connectedClient.getChannel();
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            if (connection.getChannel() == channel) {
                return Optional.of(connection);
            }
        }
        return Optional.empty();
    }

    private Optional<ServerSession> getServerConnection(final UserConnection userConnection) {
        var channel = userConnection.getChannel();
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            if (connection.getChannel() == channel) {
                return Optional.of(connection);
            }
        }
        return Optional.empty();
    }
}
