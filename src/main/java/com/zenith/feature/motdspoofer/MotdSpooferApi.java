package com.zenith.feature.motdspoofer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static com.zenith.Globals.CONFIG;
import static com.zenith.Globals.SERVER_LOG;

public class MotdSpooferApi {
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    
    private static volatile CachedMotdData cachedData = null;
    private static volatile long lastFetchTime = 0;
    private static final long CACHE_DURATION_MS = 300000; // 5 minutes cache
    
    public static class CachedMotdData {
        public final Component motd;
        public final String iconBase64;
        public final int onlinePlayers;
        public final int maxPlayers;
        
        public CachedMotdData(Component motd, String iconBase64, int onlinePlayers, int maxPlayers) {
            this.motd = motd;
            this.iconBase64 = iconBase64;
            this.onlinePlayers = onlinePlayers;
            this.maxPlayers = maxPlayers;
        }
    }
    
    public static CompletableFuture<Component> fetchMotd() {
        if (!CONFIG.server.motdSpoofer.enabled) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Check cache
        if (cachedData != null && (System.currentTimeMillis() - lastFetchTime) < CACHE_DURATION_MS) {
            return CompletableFuture.completedFuture(cachedData.motd);
        }
        
        String serverAddress = CONFIG.server.motdSpoofer.serverPort != 25565 
            ? CONFIG.server.motdSpoofer.serverIp + ":" + CONFIG.server.motdSpoofer.serverPort
            : CONFIG.server.motdSpoofer.serverIp;
            
        String apiUrl = "https://api.mcsrvstat.us/3/" + serverAddress;
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("User-Agent", "ZenithProxy/1.0")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        SERVER_LOG.warn("Failed to fetch MOTD from API. Status code: {}", response.statusCode());
                        return null;
                    }
                    
                    try {
                        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                        
                        if (!json.has("online") || !json.get("online").getAsBoolean()) {
                            SERVER_LOG.debug("Target server {} is offline", serverAddress);
                            return null;
                        }
                        
                        if (!json.has("motd")) {
                            return null;
                        }
                        
                        JsonObject motdObj = json.getAsJsonObject("motd");
                        if (motdObj.has("raw") && motdObj.get("raw").isJsonArray()) {
                            var rawArray = motdObj.getAsJsonArray("raw");
                            if (rawArray.size() > 0) {
                                StringBuilder motdBuilder = new StringBuilder();
                                for (int i = 0; i < rawArray.size(); i++) {
                                    if (i > 0) motdBuilder.append("\n");
                                    motdBuilder.append(rawArray.get(i).getAsString());
                                }
                                
                                // Parse the MOTD with legacy color codes (§)
                                Component motdComponent = LegacyComponentSerializer.legacySection()
                                        .deserialize(motdBuilder.toString());
                                
                                // Get player counts
                                int onlinePlayers = 0;
                                int maxPlayers = 100;
                                if (json.has("players")) {
                                    JsonObject players = json.getAsJsonObject("players");
                                    onlinePlayers = players.has("online") ? players.get("online").getAsInt() : 0;
                                    maxPlayers = players.has("max") ? players.get("max").getAsInt() : 100;
                                }
                                
                                // Cache the result
                                String iconBase64 = json.has("icon") ? json.get("icon").getAsString() : null;
                                cachedData = new CachedMotdData(motdComponent, iconBase64, onlinePlayers, maxPlayers);
                                lastFetchTime = System.currentTimeMillis();
                                
                                SERVER_LOG.debug("Successfully fetched and cached MOTD from {}", serverAddress);
                                return motdComponent;
                            }
                        }
                        
                        return null;
                    } catch (Exception e) {
                        SERVER_LOG.error("Error parsing MOTD response", e);
                        return null;
                    }
                })
                .exceptionally(e -> {
                    SERVER_LOG.error("Error fetching MOTD from API", e);
                    return null;
                });
    }
    
    public static String getCachedIcon() {
        return cachedData != null ? cachedData.iconBase64 : null;
    }
    
    public static CachedMotdData getCachedData() {
        return cachedData;
    }
    
    public static void clearCache() {
        cachedData = null;
        lastFetchTime = 0;
    }
}