package com.zenith.module;

import com.github.rfresh2.EventConsumer;
import com.zenith.Proxy;
import com.zenith.network.client.ClientSession;
import lombok.Getter;
import org.geysermc.mcprotocollib.network.packet.Packet;

import java.util.Collections;
import java.util.List;

import static com.zenith.Shared.EVENT_BUS;
import static com.zenith.Shared.MODULE_LOG;

/**
 * Module system base class.
 */
@Getter
public abstract class Module {
    boolean enabled = false;

    public Module() {

    }

    public synchronized void enable() {
        if (!enabled) {
            subscribeEvents();
            enabled = true;
            onEnable();
        }
    }

    public synchronized void disable() {
        if (enabled) {
            enabled = false;
            unsubscribeEvents();
            onDisable();
        }
    }

    public synchronized void setEnabled(boolean enabled) {
        if (enabled) {
            enable();
        } else {
            disable();
        }
    }

    public synchronized void syncEnabledFromConfig() {
        setEnabled(enabledSetting());
    }

    public void onEnable() { }

    public void onDisable() { }

    public void subscribeEvents() {
        EVENT_BUS.subscribe(this, registerEvents().toArray(new EventConsumer[0]));
    }

    public List<EventConsumer<?>> registerEvents() {
        return Collections.emptyList();
    }

    public void unsubscribeEvents() {
        EVENT_BUS.unsubscribe(this);
    }

    public abstract boolean enabledSetting();

    public void sendClientPacketAsync(final Packet packet) {
        ClientSession clientSession = Proxy.getInstance().getClient();
        if (clientSession != null && clientSession.isConnected()) {
            clientSession.sendAsync(packet);
        }
    }

    public void sendClientPacket(final Packet packet) {
        ClientSession clientSession = Proxy.getInstance().getClient();
        if (clientSession != null && clientSession.isConnected()) {
            clientSession.send(packet);
        }
    }

    public void sendClientPacketAwait(final Packet packet) {
        ClientSession clientSession = Proxy.getInstance().getClient();
        if (clientSession != null && clientSession.isConnected()) {
            try {
                clientSession.sendAwait(packet);
            } catch (Exception e) {
                error("Error sending awaited packet: {}", packet.getClass().getSimpleName(), e);
            }
        }
    }

    // preserves packet order
    public void sendClientPacketsAsync(final Packet... packets) {
        ClientSession clientSession = Proxy.getInstance().getClient();
        if (clientSession != null && clientSession.isConnected()) {
            for (Packet packet : packets) {
                clientSession.sendAsync(packet);
            }
        }
    }

    protected final String moduleLogPrefix = "[" + this.getClass().getSimpleName() + "] ";

    public void info(String msg) {
        MODULE_LOG.info(moduleLogPrefix + msg);
    }

    public void info(String msg, Object... args) {
        MODULE_LOG.info(moduleLogPrefix + msg, args);
    }

    public void error(String msg) {
        MODULE_LOG.error(moduleLogPrefix + msg);
    }

    public void error(String msg, Object... args) {
        MODULE_LOG.error(moduleLogPrefix + msg, args);
    }

    public void debug(String msg) {
        MODULE_LOG.debug(moduleLogPrefix + msg);
    }

    public void debug(String msg, Object... args) {
        MODULE_LOG.debug(moduleLogPrefix + msg, args);
    }

    public void warn(String msg) {
        MODULE_LOG.warn(moduleLogPrefix + msg);
    }

    public void warn(String msg, Object... args) {
        MODULE_LOG.warn(moduleLogPrefix + msg, args);
    }

    protected final String moduleAlertPrefix = "<gray>[<aqua>" + this.getClass().getSimpleName() + "<gray>]<reset> ";

    public void inGameAlert(String minedown) {
        var connections = Proxy.getInstance().getActiveConnections().getArray();
        for (int i = 0; i < connections.length; i++) {
            var connection = connections[i];
            connection.sendAsyncAlert(moduleAlertPrefix + minedown);
        }
    }

    public void inGameAlertActivePlayer(String minedown) {
        var connection = Proxy.getInstance().getActivePlayer();
        if (connection == null) return;
        connection.sendAsyncAlert(moduleAlertPrefix + minedown);
    }
}
