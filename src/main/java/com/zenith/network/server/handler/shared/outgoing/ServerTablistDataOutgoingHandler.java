package com.zenith.network.server.handler.shared.outgoing;

import com.zenith.Proxy;
import com.zenith.event.server.CustomTablistFooterBuildEvent;
import com.zenith.network.codec.PacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundTabListPacket;

import static com.zenith.Globals.*;

public class ServerTablistDataOutgoingHandler implements PacketHandler<ClientboundTabListPacket, ServerSession> {
    // todo: allow users to configure the contents of this
    private static final String footerMinimessage = """

        <aqua><bold>ZenithProxy
        <session_profile_name> </bold><gray>[<dark_aqua><session_ping>ms<gray>] <gray>-> <aqua><bold><client_profile_name> </bold><gray>[<dark_aqua><client_ping>ms<gray>]
        <blue>Online: <aqua><bold><online_time></bold> <gray>- <blue>TPS: <aqua><bold><tps>
        """;

    @Override
    public ClientboundTabListPacket apply(ClientboundTabListPacket packet, ServerSession session) {
        CACHE.getTabListCache().setLastUpdate(System.currentTimeMillis());
        return packet; // Directly return the original packet without modification
    }

    public Component insertProxyDataIntoFooter(final Component footer, final ServerSession session) {
        if (!CONFIG.server.injectTablistFooter) return footer;
        try {
            var sessionProfile = session.getProfileCache().getProfile();
            var clientProfile = CACHE.getProfileCache().getProfile();
            var sessionProfileName = sessionProfile == null ? "Unknown" : sessionProfile.getName();
            var clientProfileName = clientProfile == null ? "Unknown" : clientProfile.getName();
            var injectedFooter = ComponentSerializer.minimessage(
                footerMinimessage,
                Placeholder.unparsed("session_profile_name", sessionProfileName),
                Placeholder.unparsed("session_ping", String.valueOf(session.getPing())),
                Placeholder.unparsed("client_profile_name", clientProfileName),
                Placeholder.unparsed("client_ping", String.valueOf(Proxy.getInstance().getClient().getPing())),
                Placeholder.unparsed("online_time", Proxy.getInstance().getOnlineTimeString()),
                Placeholder.unparsed("tps", TPS.getTPS())
            );
            var event = new CustomTablistFooterBuildEvent(injectedFooter);
            EVENT_BUS.post(event);
            return footer.append(event.getFooterComponent());
        } catch (final Exception e) {
            SERVER_LOG.warn("Failed injecting proxy info to tablist footer", e);
            return footer;
        }
    }
}
