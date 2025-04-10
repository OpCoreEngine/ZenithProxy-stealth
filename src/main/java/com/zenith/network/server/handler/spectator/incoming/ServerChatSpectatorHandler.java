package com.zenith.network.server.handler.spectator.incoming;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.zenith.Proxy;
import com.zenith.api.event.message.PrivateMessageSendEvent;
import com.zenith.api.network.PacketHandler;
import com.zenith.api.network.server.ServerSession;
import com.zenith.cache.data.entity.Entity;
import com.zenith.feature.spectator.SpectatorEntityRegistry;
import com.zenith.feature.spectator.SpectatorSync;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSetCameraPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundRemoveEntitiesPacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.serverbound.ServerboundChatPacket;

import static com.zenith.Globals.*;

public class ServerChatSpectatorHandler implements PacketHandler<ServerboundChatPacket, ServerSession> {
    @Override
    public ServerboundChatPacket apply(ServerboundChatPacket packet, ServerSession session) {
        if (CONFIG.inGameCommands.enable) {
            EXECUTOR.execute(() -> {
                if (IN_GAME_COMMAND.isCommandPrefixed(packet.getMessage())) {
                    TERMINAL_LOG.info("{} executed spectator command: {}", session.getName(), packet.getMessage());
                    if (CONFIG.server.spectator.fullCommandsEnabled && (!CONFIG.server.spectator.fullCommandsRequireRegularWhitelist || PLAYER_LISTS.getWhitelist().contains(session.getProfileCache().getProfile()))) {
                        final String fullCommandAndArgs = packet.getMessage().substring(CONFIG.inGameCommands.prefix.length()).trim(); // cut off the prefix
                        IN_GAME_COMMAND.handleInGameCommandSpectator(fullCommandAndArgs, session, true);
                    } else {
                        try {
                            handleCommandInput(packet.getMessage(), session);
                        } catch (Exception e) {
                            SERVER_LOG.error("Failed to handle spectator command: {} from: {}", packet.getMessage(), session.getName(), e);
                        }
                    }
                } else {
                    EVENT_BUS.postAsync(new PrivateMessageSendEvent(session.getUUID(), session.getName(), packet.getMessage()));
                }
            });
        }
        return null;
    }

    private void handleCommandInput(final String message, final ServerSession session) {
        final String fullCommandAndArgs = message.substring(CONFIG.inGameCommands.prefix.length()).trim(); // cut off the prefix
        final String command = fullCommandAndArgs.split(" ")[0]; // first word is the command
        switch (command) {
            case "help" -> {
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<blue><bold>Spectator commands:"), false));
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<green>Prefix : \"" + CONFIG.inGameCommands.prefix + "\""), false));
                session.send(new ClientboundSystemChatPacket(Component.text(""), false));
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>help <gray>- <dark_gray>Display help menu"), false));
                if (CONFIG.server.spectator.spectatorPublicChatEnabled)
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>m <gray>- <dark_gray>Send public chats"), false));
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>playercam <gray>- <dark_gray>Set camera to the player"), false));
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>etoggle <gray>- <dark_gray>Hide your entity from yourself"), false));
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>e <gray>- <dark_gray>List spectator entities. Change with \"!e <entity>\""), false));
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>swap <gray>- <dark_gray>Swap from spectator to controlling the player"), false));
            }
            case "m" -> {
                if (CONFIG.server.spectator.spectatorPublicChatEnabled) {
                    String chatMessageContent = fullCommandAndArgs.substring(1).trim();
                    Proxy.getInstance().getClient().send(new ServerboundChatPacket(chatMessageContent));
                } else {
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>Spectator chat disabled"), false));
                }
            }
            case "etoggle" -> {
                session.setShowSelfEntity(!session.isShowSelfEntity());
                if (session.isShowSelfEntity()) {
                    session.send(session.getEntitySpawnPacket());
                    session.send(session.getEntityMetadataPacket());
                } else {
                    session.send(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
                }
                session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<blue>Show self entity toggled " + (session.isShowSelfEntity() ? "on!" : "off!")), false));
            }
            case "e" -> {
                String entityId = fullCommandAndArgs.substring(1).trim();
                boolean spectatorEntitySet = session.setSpectatorEntity(entityId);
                if (spectatorEntitySet) {
                    // respawn entity on all connections
                    var connections = Proxy.getInstance().getActiveConnections().getArray();
                    for (int i = 0; i < connections.length; i++) {
                        var connection = connections[i];
                        connection.send(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
                        if (!connection.equals(session) || session.isShowSelfEntity()) {
                            connection.send(session.getEntitySpawnPacket());
                            connection.send(session.getEntityMetadataPacket());
                            SpectatorSync.updateSpectatorPosition(session);
                        }
                    }
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<blue>Updated entity to: " + entityId), false));
                } else {
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>No entity found with id: " + entityId), false));
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>Valid id's: " + String.join(", ", SpectatorEntityRegistry.getEntityIdentifiers())), false));
                }
            }
            case "playercam" -> {
                final Entity existingTarget = session.getCameraTarget();
                if (existingTarget != null) {
                    session.setCameraTarget(null);
                    session.send(new ClientboundSetCameraPacket(session.getSpectatorSelfEntityId()));
                    SpectatorSync.syncSpectatorPositionToEntity(session, existingTarget);
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<blue>Exited playercam!"), false));
                } else {
                    session.setCameraTarget(CACHE.getPlayerCache().getThePlayer());
                    session.send(new ClientboundSetCameraPacket(CACHE.getPlayerCache().getEntityId()));
                    var connections = Proxy.getInstance().getActiveConnections().getArray();
                    for (int i = 0; i < connections.length; i++) {
                        var connection = connections[i];
                        connection.send(new ClientboundRemoveEntitiesPacket(new int[]{session.getSpectatorEntityId()}));
                    }
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<blue>Entered playercam!"), false));
                }
            }
            case "swap" -> {
                var spectatorProfile = session.getProfileCache().getProfile();
                if (spectatorProfile == null) return;
                if (!PLAYER_LISTS.getWhitelist().contains(spectatorProfile.getId())) {
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>You are not whitelisted!"), false));
                    return;
                }
                if (Proxy.getInstance().getActivePlayer() != null) {
                    session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>Someone is already controlling the player!"), false));
                    return;
                }
                if (CONFIG.server.viaversion.enabled) {
                    if (session.getProtocolVersion().olderThan(ProtocolVersion.v1_20_5)) {
                        session.send(new ClientboundSystemChatPacket(ComponentSerializer.minimessage("<red>Unsupported Client MC Version"), false));
                        return;
                    }
                }
                session.transferToControllingPlayer(CONFIG.server.getProxyAddressForTransfer(), CONFIG.server.getProxyPortForTransfer());
            }
            default -> handleCommandInput("help", session);
        }
    }
}
