package com.zenith.module.impl;

import com.github.rfresh2.EventConsumer;
import com.zenith.event.proxy.DisconnectEvent;
import com.zenith.event.proxy.ProxyClientLoggedInEvent;
import com.zenith.event.proxy.ProxySpectatorLoggedInEvent;
import com.zenith.event.proxy.chat.PublicChatEvent;
import com.zenith.event.proxy.chat.SystemChatEvent;
import com.zenith.event.proxy.chat.WhisperChatEvent;
import com.zenith.module.Module;
import com.zenith.util.CircularFifoQueue;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundSystemChatPacket;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Queue;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Shared.CONFIG;

public class ChatHistory extends Module {
    private Queue<StoredChat> chatHistory = new CircularFifoQueue<>(CONFIG.server.extra.chatHistory.maxCount);

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(PublicChatEvent.class, this::handlePublicChat),
            of(WhisperChatEvent.class, this::handleWhisperChat),
            of(SystemChatEvent.class, this::handleSystemChat),
            of(ProxyClientLoggedInEvent.class, this::handleClientLoggedIn),
            of(ProxySpectatorLoggedInEvent.class, this::handleSpectatorLoggedIn),
            of(DisconnectEvent.class, this::handleDisconnect)
        );
    }

    @Override
    public void onDisable() {
        chatHistory.clear();
    }

    @Override
    public boolean enabledSetting() {
        return CONFIG.server.extra.chatHistory.enable;
    }


    private void handleSystemChat(SystemChatEvent event) {
        chatHistory.add(new StoredChat(event.component(), Instant.now()));
    }

    private void handleWhisperChat(WhisperChatEvent event) {
        chatHistory.add(new StoredChat(event.component(), Instant.now()));
    }

    private void handlePublicChat(PublicChatEvent event) {
        chatHistory.add(new StoredChat(event.component(), Instant.now()));
    }

    private void handleClientLoggedIn(ProxyClientLoggedInEvent event) {
        removeOldChats();
        var session = event.session();
        chatHistory.forEach(chat -> session.sendAsync(new ClientboundSystemChatPacket(chat.message(), false)));
    }

    private void handleSpectatorLoggedIn(ProxySpectatorLoggedInEvent event) {
        if (!CONFIG.server.extra.chatHistory.spectators) return;
        removeOldChats();
        var session = event.session();
        chatHistory.forEach(chat -> session.sendAsync(new ClientboundSystemChatPacket(chat.message(), false)));
    }

    private void handleDisconnect(DisconnectEvent event) {
        chatHistory.clear();
    }

    private void removeOldChats() {
        while (checkChatTime(chatHistory.peek())) chatHistory.poll();
    }

    // true if the chat is older than the configured time, and should be removed
    private boolean checkChatTime(StoredChat storedChat) {
        if (storedChat == null) return false;
        return storedChat.time().isBefore(Instant.now().minus(CONFIG.server.extra.chatHistory.seconds, ChronoUnit.SECONDS));
    }

    public void syncMaxCountFromConfig() {
        final Queue<StoredChat> newChatHistory = new CircularFifoQueue<>(CONFIG.server.extra.chatHistory.maxCount);
        while (chatHistory.peek() != null) {
            newChatHistory.add(chatHistory.poll());
        }
        chatHistory = newChatHistory;
    }

    private record StoredChat(Component message, Instant time) { }

}
