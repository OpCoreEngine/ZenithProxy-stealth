package com.zenith.event.message;

import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static com.zenith.util.ChatUtil.sanitizeChatMessage;

@Data
@Accessors(fluent = true)
public class DiscordRelayChannelMessageReceivedEvent {
    private final MessageReceivedEvent event;
    @Getter(lazy = true) private final String message = processMessage();

    public String processMessage() {
        return sanitizeChatMessage(event.getMessage().getContentRaw());
    }
}
