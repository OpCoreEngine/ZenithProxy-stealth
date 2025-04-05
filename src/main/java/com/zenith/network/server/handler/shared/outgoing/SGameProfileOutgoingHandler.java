package com.zenith.network.server.handler.shared.outgoing;

import com.zenith.Proxy;
import com.zenith.event.proxy.NonWhitelistedPlayerConnectedEvent;
import com.zenith.feature.api.sessionserver.SessionServerApi;
import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.util.Wait;
import org.geysermc.mcprotocollib.auth.GameProfile;
import org.geysermc.mcprotocollib.protocol.data.ProtocolState;
import org.geysermc.mcprotocollib.protocol.packet.login.clientbound.ClientboundGameProfilePacket;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.UUID;

import static com.zenith.Shared.*;

public class SGameProfileOutgoingHandler implements PacketHandler<ClientboundGameProfilePacket, ServerSession> {
    // can be anything really, just needs to be unique and not taken by a real player seen in-game
    private static final UUID spectatorFakeUUID = UUID.fromString("c9560dfb-a792-4226-ad06-db1b6dc40b95");

    @Override
    public ClientboundGameProfilePacket apply(@NonNull ClientboundGameProfilePacket packet, @NonNull ServerSession session) {
        try {
            // finishLogin will send a second ClientboundGameProfilePacket, just return it as is
            if (session.isWhitelistChecked()) return packet;
            final GameProfile clientGameProfile = session.getProfileCache().getProfile();
            if (clientGameProfile == null) {
                session.disconnect("Failed to Login");
                return null;
            }
            if (PLAYER_LISTS.getBlacklist().contains(clientGameProfile)) {
                session.disconnect(CONFIG.server.extra.whitelist.kickmsg);
                SERVER_LOG.warn("Blacklisted! Username: {} UUID: {} [{}] MC: {} tried to connect!", clientGameProfile.getName(), clientGameProfile.getIdAsString(), session.getMCVersion(), session.getRemoteAddress());
                return null;
            }
            // this has some bearing on authorization
            // can be set by cookie. or forcefully set if they're only on spectator whitelist
            // true: only spectator -> also set by authorization, overrides any cookie state
            // false: only controlling player
            // empty: no preference, whichever is available
            Optional<Boolean> onlySpectator = Optional.empty();
            if (session.isTransferring()) {
                onlySpectator = session.getCookieCache().getSpectatorCookieValue();
                var transferSrc = session.getCookieCache().getZenithTransferSrc();
                transferSrc.ifPresent(s -> SERVER_LOG.info("{} transferring from ZenithProxy instance: {}", clientGameProfile.getName(), s));
                if (CONFIG.server.onlyZenithTransfers && transferSrc.isEmpty()) {
                    // clients can spoof these cookies easily, but the whitelist would stop them anyway
                    SERVER_LOG.info("Blocking transfer from non-ZenithProxy source. Username: {} UUID: {} MC: {} [{}]", clientGameProfile.getName(), clientGameProfile.getIdAsString(), session.getMCVersion(), session.getRemoteAddress());
                    session.disconnect("Transfer Blocked");
                    return null;
                }
            }
            if (CONFIG.server.extra.whitelist.enable && !PLAYER_LISTS.getWhitelist().contains(clientGameProfile)) {
                if (CONFIG.server.spectator.allowSpectator && (!CONFIG.server.spectator.whitelistEnabled || PLAYER_LISTS.getSpectatorWhitelist().contains(clientGameProfile))) {
                    onlySpectator = Optional.of(true);
                } else {
                    session.disconnect(CONFIG.server.extra.whitelist.kickmsg);
                    SERVER_LOG.warn("Username: {} UUID: {} [{}] MC: {} tried to connect!", clientGameProfile.getName(), clientGameProfile.getIdAsString(), session.getMCVersion(), session.getRemoteAddress());
                    EVENT_BUS.post(new NonWhitelistedPlayerConnectedEvent(clientGameProfile, session.getRemoteAddress()));
                    return null;
                }
            }
            SERVER_LOG.info("Username: {} UUID: {} MC: {} [{}] has passed the whitelist check!", clientGameProfile.getName(), clientGameProfile.getIdAsString(), session.getMCVersion(), session.getRemoteAddress());
            session.setWhitelistChecked(true);
            final Optional<Boolean> finalOnlySpectator = onlySpectator;
            EXECUTOR.execute(() -> {
                try {
                    // this method is called asynchronously off the event loop due to blocking calls possibly causing thread starvation
                    finishLogin(session, finalOnlySpectator);
                } catch (final Throwable e) {
                    session.disconnect("Login Failed", e);
                }
            });
            return null;
        } catch (final Throwable e) {
            session.disconnect("Login Failed", e);
            return null;
        }
    }

    private void finishLogin(ServerSession session, final Optional<Boolean> onlySpectator) {
        final GameProfile clientGameProfile = session.getProfileCache().getProfile();
        synchronized (this) {
            if (!Proxy.getInstance().isConnected()) {
                    if (CONFIG.client.extra.autoConnectOnLogin && !onlySpectator.orElse(false)) {
                    try {
                        SERVER_LOG.info("Auto connecting client on player login...");
                        Proxy.getInstance().connect();
                    } catch (final Throwable e) {
                        SERVER_LOG.info("Failed `autoConnectOnLogin` client connect", e);
                        session.disconnect("Failed to connect to server", e);
                        return;
                    }
                    if (!Wait.waitUntil(() -> {
                        var client = Proxy.getInstance().getClient();
                        return client != null
                            && CACHE.getProfileCache().getProfile() != null
                            && (client.isOnline() || client.isInQueue());
                    }, 15)) {
                        SERVER_LOG.info("Timed out waiting for the proxy to login");
                        session.disconnect("Timed out waiting for the proxy to login");
                        return;
                    }
                } else {
                    SERVER_LOG.info("Disconnecting: {} [{}] ({}) : Not connected to server (AutoConnectOnLogin)!", clientGameProfile.getName(), clientGameProfile.getId(), session.getMCVersion());
                    session.disconnect("Not connected to server!");
                    return;
                }
            }
        }
        var client = Proxy.getInstance().getClient();
        if (client == null
            || CACHE.getProfileCache().getProfile() == null
            || !(client.isOnline() || client.isInQueue())) {
            SERVER_LOG.info("Disconnecting: {} [{}] ({}) : Not connected to server!", clientGameProfile.getName(), clientGameProfile.getId(), session.getMCVersion());
            session.disconnect("Not connected to server!");
            return;
        }
        // avoid race condition if player disconnects sometime during our wait
        if (!session.isConnected()) return;
        SERVER_LOG.debug("User UUID: {}\nBot UUID: {}", clientGameProfile.getId().toString(), CACHE.getProfileCache().getProfile().getId().toString());
        if (!onlySpectator.orElse(false) && Proxy.getInstance().getCurrentPlayer().compareAndSet(null, session)) {
            SERVER_LOG.info("Logging in {} [{}] ({}) as controlling player", clientGameProfile.getName(), clientGameProfile.getId().toString(), session.getMCVersion());
            session.getEventLoop().execute(() -> {
                session.send(new ClientboundGameProfilePacket(CACHE.getProfileCache().getProfile(), false));
                session.switchOutboundState(ProtocolState.CONFIGURATION);
            });
            return;
        }
        if (onlySpectator.isPresent() && !onlySpectator.get()) { // the above operation failed and we don't want to be put into spectator
            session.disconnect("Someone is already controlling the player");
            return;
        }
        if (!CONFIG.server.spectator.allowSpectator) {
            session.disconnect("Spectator mode is disabled");
            return;
        }
        SERVER_LOG.info("Logging in {} [{}] ({}) as spectator", clientGameProfile.getName(), clientGameProfile.getId().toString(), session.getMCVersion());
        session.setSpectator(true);
        final GameProfile spectatorFakeProfile = new GameProfile(spectatorFakeUUID, clientGameProfile.getName());
        if (clientGameProfile.getProperty("textures") == null) {
                SessionServerApi.INSTANCE.getProfileAndSkin(clientGameProfile.getId())
                    .ifPresentOrElse(p -> spectatorFakeProfile.setProperties(p.getProperties()),
                                     () -> SERVER_LOG.info("Failed getting spectator skin for {} [{}] ({})", clientGameProfile.getName(), clientGameProfile.getId().toString(), session.getMCVersion()));
        } else {
            spectatorFakeProfile.setProperties(clientGameProfile.getProperties());
        }
        session.getSpectatorFakeProfileCache().setProfile(spectatorFakeProfile);
        session.getEventLoop().execute(() -> {
            session.send(new ClientboundGameProfilePacket(spectatorFakeProfile, false));
            session.switchOutboundState(ProtocolState.CONFIGURATION);
        });
        return;
    }
}
