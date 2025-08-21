package com.zenith.feature.motdspoofer;

import com.zenith.event.server.MotdBuildEvent;
import net.kyori.adventure.text.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.github.rfresh2.EventConsumer.of;
import static com.zenith.Globals.*;

public class MotdSpooferListener {
    private static MotdSpooferListener INSTANCE;
    
    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = new MotdSpooferListener();
        }
    }
    
    private MotdSpooferListener() {
        EVENT_BUS.subscribe(
            this,
            of(MotdBuildEvent.class, this::handleMotdBuild)
        );
        SERVER_LOG.info("MOTD Spoofer listener initialized");
    }
    
    private void handleMotdBuild(MotdBuildEvent event) {
        if (!CONFIG.server.motdSpoofer.enabled) {
            return;
        }
        
        try {
            // Try to fetch MOTD with a short timeout since this is in the ping response path
            CompletableFuture<Component> motdFuture = MotdSpooferApi.fetchMotd();
            Component spoofedMotd = motdFuture.get(100, TimeUnit.MILLISECONDS);
            
            if (spoofedMotd != null) {
                event.setMotd(spoofedMotd);
                SERVER_LOG.debug("Applied spoofed MOTD");
            }
        } catch (Exception e) {
            // Fall back to default MOTD if fetching fails
            SERVER_LOG.debug("Failed to fetch spoofed MOTD, using default", e);
        }
    }
}