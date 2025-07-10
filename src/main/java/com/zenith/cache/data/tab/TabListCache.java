package com.zenith.cache.data.tab;

import com.zenith.cache.CacheResetType;
import com.zenith.cache.CachedData;
import lombok.Data;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.network.tcp.TcpSession;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntry;
import org.geysermc.mcprotocollib.protocol.data.game.PlayerListEntryAction;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundPlayerInfoUpdatePacket;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundTabListPacket;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


@Data
@Accessors(chain = true)
public class TabListCache implements CachedData {
    protected final Map<UUID, PlayerListEntry> tablist = new ConcurrentHashMap<>();
    @NonNull
    protected Component header = Component.text("");
    @NonNull
    protected Component footer = Component.text("");
    protected long lastUpdate = 0L;

    @Override
    public void getPackets(@NonNull Consumer<Packet> consumer, final @NonNull TcpSession session) {
        consumer.accept(new ClientboundTabListPacket(this.getHeader(), this.getFooter()));
        consumer.accept(new ClientboundPlayerInfoUpdatePacket(
            EnumSet.of(PlayerListEntryAction.ADD_PLAYER, PlayerListEntryAction.UPDATE_GAME_MODE, PlayerListEntryAction.UPDATE_LISTED, PlayerListEntryAction.UPDATE_LATENCY, PlayerListEntryAction.UPDATE_DISPLAY_NAME),
            getEntries().toArray(PlayerListEntry[]::new)
        ));
    }

    @Override
    public void reset(CacheResetType type) {
        if (type == CacheResetType.FULL || type == CacheResetType.PROTOCOL_SWITCH) {
            this.tablist.clear();
            this.header = Component.text("");
            this.footer = Component.text("");
            this.lastUpdate = 0L;
        }
    }

    @Override
    public String getSendingMessage() {
        return "Sending " + tablist.size() + " tablist entries";
    }

    public void add(@NonNull PlayerListEntry entry) {
        this.tablist.put(entry.getProfile().getId(), entry);
    }

    public Optional<PlayerListEntry> remove(@NonNull PlayerListEntry entry) {
        return remove(entry.getProfileId());
    }

    public Optional<PlayerListEntry> remove(@NonNull UUID uuid) {
        return Optional.ofNullable(this.tablist.remove(uuid));
    }

    public Optional<PlayerListEntry> get(UUID uuid) {
        return Optional.ofNullable(this.tablist.get(uuid));
    }

    public Optional<PlayerListEntry> getFromName(final String username) {
        return this.tablist.values().stream().filter(v -> v.getName().equals(username)).findFirst();
    }

    public Collection<PlayerListEntry> getEntries() {
        return this.tablist.values();
    }
}
