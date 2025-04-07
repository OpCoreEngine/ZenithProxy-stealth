package com.zenith.feature.extrachat;

import com.zenith.network.registry.PacketHandler;
import com.zenith.network.server.ServerSession;
import com.zenith.util.ComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

import java.util.Objects;

import static com.zenith.Globals.*;
import static java.util.Objects.nonNull;

public class ECSystemChatOutgoingHandler implements PacketHandler<ClientboundSystemChatPacket, ServerSession> {

    @Override
    public ClientboundSystemChatPacket apply(ClientboundSystemChatPacket packet, ServerSession session) {
        try {
            final Component component = packet.getContent();
            final String message = ComponentSerializer.serializePlain(component);
            if (message.startsWith("<")) {
                if (CONFIG.client.extra.chat.hideChat) {
                    return null;
                } else if (PLAYER_LISTS.getIgnoreList()
                    .contains(message.substring(message.indexOf("<") + 1, message.indexOf(">")))) {
                    return null;
                }
            }
            if (CONFIG.client.extra.chat.hideChat && message.startsWith("<")) {
                return null;
            } else if (isWhisper(message)) {
                if (CONFIG.client.extra.chat.hideWhispers) {
                    return null;
                } else if (PLAYER_LISTS.getIgnoreList().contains(message.substring(0, message.indexOf(" ")))) {
                    return null;
                }
            } else if (CONFIG.client.extra.chat.hideDeathMessages && isDeathMessage(component, message)) {
                return null;
            }
        } catch (final Exception e) {
            SERVER_LOG.error("Failed to parse chat message in ExtraChatSystemChatOutgoingHandler: {}",
                             ComponentSerializer.serializePlain(packet.getContent()),
                             e);
        }
        return packet;
    }

    private boolean isWhisper(String message) {
        if (!message.startsWith("<")) {
            String[] split = message.split(" ");
            return split.length > 2 && split[1].startsWith("whispers");
        }
        return false;
    }

    private boolean isDeathMessage(final Component component, final String messageRaw) {
        if (!messageRaw.startsWith("<")) {
            return component.children().stream().anyMatch(child -> nonNull(child.color())
                && Objects.equals(child.color(), TextColor.color(170, 0, 0)));
        }
        return false;
    }
}
